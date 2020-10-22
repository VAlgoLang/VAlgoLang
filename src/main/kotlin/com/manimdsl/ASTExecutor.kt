package com.manimdsl

import com.manimdsl.frontend.*
import com.manimdsl.linearrepresentation.*
import com.manimdsl.shapes.Rectangle
import java.util.*

// Wrapper classes for values of variables while executing code
sealed class ExecValue

data class DoubleValue(val value: Double, val manimObject: MObject? = null) : ExecValue()

data class StackValue(val initObject: MObject, val stack: Stack<Pair<Double, MObject>>) : ExecValue()

object EmptyValue : ExecValue()

class VirtualMachine(private val program: ProgramNode, private val symbolTableVisitor: SymbolTableVisitor, private val statements: Map<Int, ASTNode>, private val fileLines: List<String>) {

    private val linearRepresentation = mutableListOf<ManimInstr>()
    private val variableNameGenerator = VariableNameGenerator(symbolTableVisitor)
    private val codeBlockVariable: String = variableNameGenerator.generateNameFromPrefix("code_block")
    private val codeTextVariable: String = variableNameGenerator.generateNameFromPrefix("code_text")
    private val pointerVariable: String = variableNameGenerator.generateNameFromPrefix("pointer")
    private val displayLine: MutableList<Int> = mutableListOf()
    private val displayCode: MutableList<String> = mutableListOf()

    init {
        fileLines.indices.forEach {
            val presentableLine = fileLines[it] == "}" || fileLines[it] == "{" || fileLines[it] == "" || (statements.containsKey(it + 1) && statements[it + 1] is CodeNode)
            if (presentableLine) {
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

    private inner class Frame(private var pc: Int, private var finalLine: Int, private var variables: MutableMap<String, ExecValue>) {

        // instantiate new Frame and execute on scoping changes e.g. recursion

        fun runFrame(): ExecValue {

            while (pc <= finalLine) {

                if (statements.containsKey(pc)) {
                    val statement = statements[pc]

                    if (statement is CodeNode) {
                        moveToLine()
                    }

                    when (statement) {
                        is ReturnNode -> {
                            return executeExpression(statement.expression)
                        }
                        is FunctionNode -> {
                            // just go onto next line, this is just a label
                        }
                        is AssignmentNode -> executeAssignment(statement)
                        is DeclarationNode -> executeAssignment(statement)
                        is MethodCallNode -> executeMethodCall(statement, false)
                        is FunctionCallNode -> executeFunctionCall(statement)
                    }
                }
                fetchNextStatement()
            }

            return EmptyValue
        }

        private fun moveToLine() {
            linearRepresentation.add(MoveToLine(displayLine[pc-1], pointerVariable, codeBlockVariable))
        }

        private fun executeFunctionCall(statement: FunctionCallNode): ExecValue {
            // create new stack frame with argument variables
            val executedArguments = statement.arguments.map { executeExpression(it) }
            val argumentNames = (symbolTableVisitor.getData(statement.functionIdentifier) as FunctionData).parameters.map { it.identifier }
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

        private fun executeExpression(node: ExpressionNode, insideMethodCall: Boolean = false, identifier: String = "") = when (node) {
            is IdentifierNode -> variables[node.identifier]!!
            is NumberNode -> DoubleValue(node.double)
            is MethodCallNode -> executeMethodCall(node, insideMethodCall)
            is AddExpression -> executeBinaryOp(node) { x, y -> x + y }
            is SubtractExpression -> executeBinaryOp(node) { x, y -> x - y }
            is MultiplyExpression -> executeBinaryOp(node) { x, y -> x * y }
            is PlusExpression -> executeUnaryOp(node) { x -> x }
            is MinusExpression -> executeUnaryOp(node) { x -> -x }
            is ConstructorNode -> executeConstructor(node, identifier)
            is FunctionCallNode -> executeFunctionCall(node)
        }

        private fun executeMethodCall(node: MethodCallNode, insideMethodCall: Boolean): ExecValue {
            return when (val ds = variables[node.instanceIdentifier]) {
                is StackValue -> {
                    return when (node.dataStructureMethod) {
                        is StackType.PushMethod -> {
                            val doubleValue = executeExpression(node.arguments[0], true) as DoubleValue
                            val topOfStack = if (ds.stack.empty()) ds.initObject else ds.stack.peek().second

                            val hasOldMObject = doubleValue.manimObject != null
                            val oldMObject = doubleValue.manimObject ?: EmptyMObject
                            val rectangle = if (hasOldMObject) oldMObject else NewMObject(
                                    Rectangle(
                                            variableNameGenerator.generateNameFromPrefix("rectangle"),
                                            doubleValue.value.toString()
                                    ),
                                    codeTextVariable
                            )

                            val instructions =
                                    mutableListOf<ManimInstr>(MoveObject(rectangle.shape, topOfStack.shape, ObjectSide.ABOVE))
                            if (!hasOldMObject) {
                                instructions.add(0, rectangle)
                            }

                            linearRepresentation.addAll(instructions)
                            ds.stack.push(Pair(doubleValue.value, rectangle))
                            doubleValue
                        }
                        is StackType.PopMethod -> {
                            val poppedValue = ds.stack.pop()
                            val newTopOfStack = if (ds.stack.empty()) ds.initObject else ds.stack.peek().second

                            val topOfStack = poppedValue.second
                            val instructions = listOf(
                                    MoveObject(
                                            topOfStack.shape,
                                            newTopOfStack.shape,
                                            ObjectSide.ABOVE,
                                            20,
                                            !insideMethodCall
                                    ),
                            )

                            linearRepresentation.addAll(instructions)
                            DoubleValue(poppedValue.first, poppedValue.second)
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
                    val numStack = variables.values.filterIsInstance(StackValue::class.java).lastOrNull()
                    val (instructions, newObject) = if (numStack == null) {
                        val stackInit = InitStructure(
                                Coord(2.0, -1.0),
                                Alignment.HORIZONTAL,
                                variableNameGenerator.generateNameFromPrefix("empty"),
                                identifier
                        )
                        // Add to stack of objects to keep track of identifier
                        Pair(listOf(stackInit), stackInit)
                    } else {
                        val stackInit = InitStructure(
                                RelativeToMoveIdent,
                                Alignment.HORIZONTAL,
                                variableNameGenerator.generateNameFromPrefix("empty"),
                                identifier,
                                numStack.initObject.shape
                        )
                        Pair(listOf(stackInit), stackInit)
                    }
                    linearRepresentation.addAll(instructions)
                    StackValue(newObject, Stack())
                }
            }
        }

        private fun executeUnaryOp(node: UnaryExpression, op: (first: Double) -> Double): ExecValue {
            return DoubleValue(op((executeExpression(node.expr) as DoubleValue).value))
        }

        private fun executeBinaryOp(node: BinaryExpression, op: (first: Double, seconds: Double) -> Double): ExecValue {
            return DoubleValue(
                    op(
                            (executeExpression(node.expr1) as DoubleValue).value,
                            (executeExpression(node.expr2) as DoubleValue).value
                    )
            )
        }




    }

}
