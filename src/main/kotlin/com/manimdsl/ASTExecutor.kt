package com.manimdsl

import com.manimdsl.executor.*
import com.manimdsl.frontend.*
import com.manimdsl.linearrepresentation.*
import com.manimdsl.shapes.Rectangle
import com.manimdsl.stylesheet.Stylesheet
import java.util.*

class VirtualMachine(
    private val program: ProgramNode,
    private val symbolTableVisitor: SymbolTableVisitor,
    private val statements: Map<Int, ASTNode>,
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

    fun runProgram(): List<ManimInstr> {
        linearRepresentation.add(CodeBlock(displayCode, codeBlockVariable, codeTextVariable, pointerVariable))
        val variables = mutableMapOf<String, ExecValue>()
        Frame(program.statements.first().lineNumber, fileLines.size, variables).runFrame()
        return linearRepresentation
    }

    private inner class Frame(
        private var pc: Int,
        private var finalLine: Int,
        private var variables: MutableMap<String, ExecValue>
    ) {

        // instantiate new Frame and execute on scoping changes e.g. recursion

        fun runFrame(): ExecValue {

            while (pc <= finalLine) {

                if (statements.containsKey(pc)) {
                    val statement = statements[pc]

                    if (statement is CodeNode) {
                        moveToLine()
                    }

                    val value = executeStatement(statement)
                    if (value !is EmptyValue) return value

                }
                fetchNextStatement()
            }

            return EmptyValue
        }

        private fun executeStatement(statement: ASTNode?): ExecValue {
            when (statement) {
                is ReturnNode -> return executeExpression(statement.expression)
                is FunctionNode -> {
                    // just go onto next line, this is just a label
                }
                is SleepNode -> executeSleep(statement)
                is AssignmentNode -> executeAssignment(statement)
                is DeclarationNode -> executeAssignment(statement)
                is MethodCallNode -> executeMethodCall(statement, false)
                is FunctionCallNode -> executeFunctionCall(statement)
                is IfStatementNode -> return executeIfStatement(statement)
                is ElseNode -> return EmptyValue
            }

            return EmptyValue
        }

        private fun executeSleep(statement: SleepNode) {
            linearRepresentation.add(Sleep((executeExpression(statement.sleepTime) as DoubleValue).value))
        }

        private fun moveToLine(line: Int = pc, updatePc: Boolean = false) {
            if (updatePc) pc = line
            linearRepresentation.add(MoveToLine(displayLine[line - 1], pointerVariable, codeBlockVariable))
        }

        private fun executeFunctionCall(statement: FunctionCallNode): ExecValue {
            // create new stack frame with argument variables
            val executedArguments = statement.arguments.map { executeExpression(it) }
            val argumentNames =
                (symbolTableVisitor.getData(statement.functionIdentifier) as FunctionData).parameters.map { it.identifier }
            val argumentVariables = (argumentNames zip executedArguments).toMap().toMutableMap()
            val functionNode = program.functions.find { it.identifier == statement.functionIdentifier }!!
            val finalStatementLine = functionNode.statements.last().lineNumber
            // program counter will forward in loop, we have popped out of stack
            val returnValue = Frame(functionNode.lineNumber, finalStatementLine, argumentVariables).runFrame()
            // to visualise popping back to assignment we can move pointer to the prior statement again
            moveToLine()
            return returnValue
        }


        private fun fetchNextStatement() {
            ++pc
        }

        private fun executeAssignment(node: DeclarationOrAssignment) {
            variables[node.identifier] = executeExpression(node.expression, identifier = node.identifier)
        }

        private fun executeExpression(
            node: ExpressionNode,
            insideMethodCall: Boolean = false,
            identifier: String = ""
        ) = when (node) {
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
                            val topOfStack = if (ds.stack.empty()) ds.manimObject else ds.stack.peek().manimObject

                            val hasOldMObject = value.manimObject !is EmptyMObject
                            val oldMObject = value.manimObject
                            val style = stylesheet.getStyle(node.instanceIdentifier, ds)
                            val newObjectStyle = stylesheet.getAnimatedStyle(node.instanceIdentifier, ds) ?: style
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
                                    RestyleObject(rectangle.shape, stylesheet.getStyle(node.instanceIdentifier, ds))
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
                    val style = stylesheet.getStyle(identifier, stackValue)
                    val numStack = variables.values.filterIsInstance(StackValue::class.java).lastOrNull()
                    val (instructions, newObject) = if (numStack == null) {
                        val stackInit = InitStructure(
                            node.type,
                            Coord(2.0, -1.0),
                            Alignment.HORIZONTAL,
                            variableNameGenerator.generateNameFromPrefix("empty"),
                            identifier,
                            color = style.borderColor,
                            textColor = style.textColor,
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
                            color = style.borderColor,
                            textColor = style.textColor,
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
            return op(executeExpression(node.expr))
        }

        private fun executeBinaryOp(
            node: BinaryExpression,
            op: (first: ExecValue, seconds: ExecValue) -> ExecValue
        ): ExecValue {
            return op(
                executeExpression(node.expr1),
                executeExpression(node.expr2)
            )
        }

        private fun executeIfStatement(ifStatementNode: IfStatementNode): ExecValue {
            var conditionValue = executeExpression(ifStatementNode.condition) as BoolValue
            //If
            if (conditionValue.value) {
                return executeStatementBlock(ifStatementNode)
            }

            // Elif
            for (elif in ifStatementNode.elifs) {
                moveToLine(elif.lineNumber)
                // Add statement to code
                conditionValue = executeExpression(elif.condition) as BoolValue
                if (conditionValue.value) {
                    return executeStatementBlock(elif)
                }
            }

            // Else
            moveToLine(ifStatementNode.elseBlock.lineNumber)
            return executeStatementBlock(ifStatementNode.elseBlock)

        }

        private fun executeStatementBlock(statementBlock: StatementBlock): ExecValue {
            if (statementBlock.statements.isEmpty()) return EmptyValue
            var execValue: ExecValue = EmptyValue
            statementBlock.statements.forEach {
                moveToLine(it.lineNumber)
                execValue = executeStatement(it)
                if (it is ReturnNode) {
                    return execValue;
                }
            }
            return execValue
        }


    }

}
