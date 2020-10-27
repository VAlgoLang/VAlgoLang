package com.manimdsl.runtime

import com.manimdsl.ExitStatus
import com.manimdsl.errorhandling.ErrorHandler.addRuntimeError
import com.manimdsl.executor.*
import com.manimdsl.frontend.*
import com.manimdsl.linearrepresentation.*
import com.manimdsl.shapes.Rectangle
import com.manimdsl.stylesheet.Stylesheet
import java.util.*

class VirtualMachine(
    private val program: ProgramNode,
    private val symbolTableVisitor: SymbolTableVisitor,
    private val statements: Map<Int, StatementNode>,
    private val fileLines: List<String>,
    private val stylesheet: Stylesheet
) {

    private val linearRepresentation = mutableListOf<ManimInstr>()
    private val variableNameGenerator = VariableNameGenerator(symbolTableVisitor)
    private val codeBlockVariable: String = variableNameGenerator.generateNameFromPrefix("code_block")
    private val codeTextVariable: String = variableNameGenerator.generateNameFromPrefix("code_text")
    private val pointerVariable: String = variableNameGenerator.generateNameFromPrefix("pointer")
    private val displayLine: MutableList<Int> = mutableListOf()
    private val displayCode: MutableList<String> = mutableListOf()
    private val acceptableNonStatements = setOf("}", "{", "")
    private val ALLOCATED_STACKS = 1000

    init {
        fileLines.indices.forEach {
            if (acceptableNonStatements.any { x -> fileLines[it].contains(x) } || statements[it + 1] is CodeNode) {
                displayCode.add(fileLines[it])
                displayLine.add(1 + (displayLine.lastOrNull() ?: 0))
            } else {
                displayLine.add(displayLine.lastOrNull() ?: 0)
            }
        }
    }

    fun runProgram(): Pair<ExitStatus, MutableList<ManimInstr>> {
        linearRepresentation.add(CodeBlock(displayCode, codeBlockVariable, codeTextVariable, pointerVariable))
        val variables = mutableMapOf<String, ExecValue>()
        val result = Frame(program.statements.first().lineNumber, fileLines.size, variables).runFrame()
        return if (result is RuntimeError) {
            addRuntimeError(result.value, result.lineNumber)
            Pair(ExitStatus.RUNTIME_ERROR, linearRepresentation)
        } else {
            Pair(ExitStatus.EXIT_SUCCESS, linearRepresentation)
        }
    }

    private inner class Frame(
            private var pc: Int,
            private var finalLine: Int,
            private var variables: MutableMap<String, ExecValue>,
            val depth: Int = 1
    ) {

        // instantiate new Frame and execute on scoping changes e.g. recursion

        fun runFrame(): ExecValue {
            if (depth > ALLOCATED_STACKS) {
                return RuntimeError(value = "Stack Overflow Error. Program failed to terminate.", lineNumber = pc)
            }

            while (pc <= finalLine) {

                if (statements.containsKey(pc)) {
                    val statement = statements[pc]!!

                    if (statement is CodeNode) {
                        moveToLine()
                    }

                    val value = executeStatement(statement)
                    if (statement is ReturnNode || value is RuntimeError) return value

                }
                fetchNextStatement()
            }

            return EmptyValue
        }

        private fun executeStatement(statement: StatementNode): ExecValue = when (statement) {
            is ReturnNode -> executeExpression(statement.expression)
            is FunctionNode -> {
                // just go onto next line, this is just a label
                EmptyValue
            }
            is SleepNode -> executeSleep(statement)
            is AssignmentNode -> executeAssignment(statement)
            is DeclarationNode -> executeAssignment(statement)
            is MethodCallNode -> executeMethodCall(statement, false)
            is FunctionCallNode -> executeFunctionCall(statement)
            is IfStatementNode -> executeIfStatement(statement)
            else -> EmptyValue
        }

        private fun executeSleep(statement: SleepNode): ExecValue {
            linearRepresentation.add(Sleep((executeExpression(statement.sleepTime) as DoubleValue).value))
            return EmptyValue
        }

        private fun addSleep(length: Double) {
            linearRepresentation.add(Sleep(length))
        }

        private fun moveToLine(line: Int = pc, updatePc: Boolean = false) {
            if (updatePc) pc = line
            linearRepresentation.add(MoveToLine(displayLine[line - 1], pointerVariable, codeBlockVariable, codeTextVariable))
        }

        private fun executeFunctionCall(statement: FunctionCallNode): ExecValue {
            // create new stack frame with argument variables
            val executedArguments = mutableListOf<ExecValue>()
            statement.arguments.forEach {
                val executed = executeExpression(it)
                if (executed is RuntimeError)
                    return executed
                else
                    executedArguments.add(executed)
            }
            val argumentNames =
                (symbolTableVisitor.getData(statement.functionIdentifier) as FunctionData).parameters.map { it.identifier }
            val argumentVariables = (argumentNames zip executedArguments).toMap().toMutableMap()
            val functionNode = program.functions.find { it.identifier == statement.functionIdentifier }!!
            val finalStatementLine = functionNode.statements.last().lineNumber
            // program counter will forward in loop, we have popped out of stack
            val returnValue = Frame(functionNode.lineNumber, finalStatementLine, argumentVariables, depth+1).runFrame()
            // to visualise popping back to assignment we can move pointer to the prior statement again
            moveToLine()
            return returnValue
        }


        private fun fetchNextStatement() {
            ++pc
        }

        private fun executeAssignment(node: DeclarationOrAssignment): ExecValue {
            val assignedValue = executeExpression(node.expression, identifier = node.identifier)
            variables[node.identifier] = assignedValue
            return if (assignedValue is RuntimeError) {
                assignedValue
            } else {
                EmptyValue
            }
        }

        private fun executeExpression(node: ExpressionNode, insideMethodCall: Boolean = false, identifier: String = ""): ExecValue = when (node) {
            is IdentifierNode -> variables[node.identifier]!!
            is NumberNode -> DoubleValue(node.double)
            is MethodCallNode -> executeMethodCall(node, insideMethodCall)
            is AddExpression -> executeBinaryOp(node) { x, y -> x + y }
            is SubtractExpression -> executeBinaryOp(node) { x, y -> x - y }
            is MultiplyExpression -> executeBinaryOp(node) { x, y -> x * y }
            is PlusExpression -> executeUnaryOp(node) { x -> x }
            is MinusExpression -> executeUnaryOp(node) { x -> DoubleValue(-(x as DoubleValue).value) }
            is BoolNode -> BoolValue(node.value)
            is AndExpression -> executeBinaryOp(node) { x, y -> BoolValue((x as BoolValue).value && (y as BoolValue).value) }
            is OrExpression -> executeBinaryOp(node) { x, y -> BoolValue((x as BoolValue).value || (y as BoolValue).value) }
            is EqExpression -> executeBinaryOp(node) { x, y -> BoolValue(x == y) }
            is NeqExpression -> executeBinaryOp(node) { x, y -> BoolValue(x != y) }
            is GtExpression -> executeBinaryOp(node) { x, y -> BoolValue(x > y) }
            is LtExpression -> executeBinaryOp(node) { x, y -> BoolValue(x < y) }
            is GeExpression -> executeBinaryOp(node) { x, y -> BoolValue(x >= y) }
            is LeExpression -> executeBinaryOp(node) { x, y -> BoolValue(x <= y) }
            is NotExpression -> executeUnaryOp(node) { x -> BoolValue(!x) }
            is ConstructorNode -> executeConstructor(node, identifier)
            is FunctionCallNode -> executeFunctionCall(node)
            is VoidNode -> VoidValue
        }

        private fun executeMethodCall(node: MethodCallNode, insideMethodCall: Boolean): ExecValue {
            return when (val ds = variables[node.instanceIdentifier]) {
                is StackValue -> {
                    return when (node.dataStructureMethod) {
                        is StackType.PushMethod -> {
                            val value = executeExpression(node.arguments[0], true)
                            if (value is RuntimeError) {
                                return value
                            }
                            val topOfStack = if (ds.stack.empty()) ds.manimObject else ds.stack.peek().manimObject

                            val hasOldMObject = value.manimObject !is EmptyMObject
                            val oldMObject = value.manimObject
                            val newObjectStyle = ds.style.animate ?: ds.style
                            val rectangle = if (hasOldMObject) oldMObject else NewMObject(
                                Rectangle(
                                    variableNameGenerator.generateNameFromPrefix("rectangle"),
                                    value.value.toString(),
                                    color = newObjectStyle.borderColor,
                                    textColor = newObjectStyle.textColor
                                ),
                                codeTextVariable
                            )

                            val instructions =
                                mutableListOf<ManimInstr>(
                                    MoveObject(
                                        rectangle.shape,
                                        topOfStack.shape,
                                        ObjectSide.ABOVE
                                    ),
                                    RestyleObject(rectangle.shape, ds.style)
                                )
                            if (!hasOldMObject) {
                                instructions.add(0, rectangle)
                            }

                            linearRepresentation.addAll(instructions)
                            value.manimObject = rectangle
                            ds.stack.push(value)
                            EmptyValue
                        }
                        is StackType.PopMethod -> {
                            if (ds.stack.empty()) {
                                return RuntimeError(value = "Attempted to pop from empty stack ${node.instanceIdentifier}", lineNumber = pc)
                            }
                            val poppedValue = ds.stack.pop()
                            val newTopOfStack = if (ds.stack.empty()) ds.manimObject else ds.stack.peek().manimObject

                            val topOfStack = poppedValue.manimObject
                            val instructions = mutableListOf<ManimInstr>(
                                    MoveObject(
                                            topOfStack.shape,
                                            newTopOfStack.shape,
                                            ObjectSide.ABOVE,
                                            20,
                                            !insideMethodCall
                                    ),
                            )
                            val newStyle = stylesheet.getAnimatedStyle(node.instanceIdentifier, ds)
                            if (newStyle != null) instructions.add(0, RestyleObject(topOfStack.shape, newStyle))

                            linearRepresentation.addAll(instructions)
                            return poppedValue
                        }
                        is StackType.IsEmptyMethod -> {
                            return BoolValue(ds.stack.isEmpty())
                        }
                        is StackType.SizeMethod -> {
                            return DoubleValue(ds.stack.size.toDouble())
                        }
                        is StackType.PeekMethod -> {
                            if (ds.stack.empty()) {
                                return RuntimeError(value = "Attempted to peek empty stack", lineNumber = pc)
                            }
                            val clonedPeekValue = ds.stack.peek().clone()
                            clonedPeekValue.manimObject = EmptyMObject
                            return clonedPeekValue
                        }
                        else -> EmptyValue
                    }
                }
                else -> EmptyValue
            }
        }

        private fun executeConstructor(node: ConstructorNode, identifier: String): ExecValue {
            return when (node.type) {
                is StackType -> {
                    val stackValue = StackValue(EmptyMObject, Stack())
                    stackValue.style = stylesheet.getStyle(identifier, stackValue)
                    val numStack = variables.values.filterIsInstance(StackValue::class.java).lastOrNull()
                    val (instructions, newObject) = if (numStack == null) {
                        val stackInit = InitStructure(
                            node.type,
                            Coord(2.0, -1.0),
                            Alignment.HORIZONTAL,
                            variableNameGenerator.generateNameFromPrefix("empty"),
                            identifier,
                            color = stackValue.style.borderColor,
                            textColor = stackValue.style.textColor,
                        )
                        // Add to stack of objects to keep track of identifier
                        Pair(listOf(stackInit), stackInit)
                    } else {
                        val stackInit = InitStructure(
                            node.type,
                            RelativeToMoveIdent,
                            Alignment.HORIZONTAL,
                            variableNameGenerator.generateNameFromPrefix("empty"),
                            identifier,
                            numStack.manimObject.shape,
                            color = stackValue.style.borderColor,
                            textColor = stackValue.style.textColor,
                        )
                        Pair(listOf(stackInit), stackInit)
                    }
                    linearRepresentation.addAll(instructions)
                    stackValue.manimObject = newObject
                    stackValue
                }
            }
        }

        private fun executeUnaryOp(node: UnaryExpression, op: (first: ExecValue) -> ExecValue): ExecValue {
            val subExpression = executeExpression(node.expr)
            return if (subExpression is RuntimeError) {
                subExpression
            } else {
                op(subExpression)
            }
        }

        private fun executeBinaryOp(
            node: BinaryExpression,
            op: (first: ExecValue, seconds: ExecValue) -> ExecValue
        ): ExecValue {

            val leftExpression = executeExpression(node.expr1)
            if (leftExpression is RuntimeError) {
                return leftExpression
            }
            val rightExpression = executeExpression(node.expr2)

            if (rightExpression is RuntimeError) {
                return rightExpression
            }
            return op(
                    leftExpression,
                    rightExpression
            )
        }

        private fun executeIfStatement(ifStatementNode: IfStatementNode): ExecValue {
            addSleep(0.5)
            var conditionValue = executeExpression(ifStatementNode.condition) as BoolValue
            // Set pc to end of if statement as branching is handled here
            pc = ifStatementNode.endLineNumber

            //If
            if (conditionValue.value) {
                return executeStatementBlock(ifStatementNode.statements)
            }

            // Elif
            for (elif in ifStatementNode.elifs) {
                moveToLine(elif.lineNumber)
                addSleep(0.5)
                // Add statement to code
                conditionValue = executeExpression(elif.condition) as BoolValue
                if (conditionValue.value) {
                    return executeStatementBlock(elif.statements)
                }
            }

            // Else
            moveToLine(ifStatementNode.elseBlock.lineNumber)
            addSleep(0.5)
            return executeStatementBlock(ifStatementNode.elseBlock.statements)

        }

        private fun executeStatementBlock(statements: List<StatementNode>): ExecValue {
            if (statements.isEmpty()) return EmptyValue
            var execValue: ExecValue = EmptyValue
            statements.forEach {
                moveToLine(it.lineNumber)
                execValue = executeStatement(it)
                if (execValue !is EmptyValue) {
                    return execValue
                }
            }
            return execValue
        }


    }

}
