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
    private val codeBlockVariable: String
    private val codeTextVariable: String
    private val pointerVariable: String
    private val realLine: MutableList<Int> = mutableListOf()
    private val displayCode: MutableList<String> = mutableListOf()
    init {
        pointerVariable = variableNameGenerator.generateNameFromPrefix("pointer")
        codeBlockVariable = variableNameGenerator.generateNameFromPrefix("code_block")
        codeTextVariable = variableNameGenerator.generateNameFromPrefix("code_text")
        fileLines.indices.forEach {
            if (it == 0) {
                if (statements.containsKey(it+1) && statements[it+1] is CodeNode) {
                    displayCode.add(fileLines[it])
                    realLine.add(1)
                } else {
                    realLine.add(0)
                }
            } else {
                realLine.add(realLine.last() + if (fileLines[it] == "}" || fileLines[it] == "{"  || (statements.containsKey(it+1) && statements[it+1] is CodeNode)) {
                    displayCode.add(fileLines[it])
                    1
                } else {
                    0
                })
            }
        }F
    }

    fun runProgram(): List<ManimInstr> {
        val variables = mutableMapOf<String, ExecValue>()
        val startLine = program.statements.first().lineNumber
        Frame(startLine, fileLines.size, variables).execute()
        linearRepresentation.add(0, CodeBlock(displayCode, codeBlockVariable, codeTextVariable, pointerVariable))
        return linearRepresentation
    }

    private inner class Frame(private var pc: Int, private var finalLine: Int, private var variables: MutableMap<String, ExecValue>) {

        // will be used for scoping changes e.g. recursion

        fun execute(): ExecValue {

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
            linearRepresentation.add(MoveToLine(realLine[pc-1], pointerVariable, codeBlockVariable))
        }

        private fun executeFunctionCall(statement: FunctionCallNode): ExecValue {
            // create new stack frame with argument variables
            val executedArguments = statement.arguments.map { executeExpression(it) }
            val parameterNodes = (symbolTableVisitor.getData(statement.functionIdentifier) as FunctionData).parameters
            val argumentVariables = (parameterNodes.map { it.identifier } zip executedArguments).toMap().toMutableMap()
            val functionNode = program.functions.find { it.identifier == statement.functionIdentifier }!!
            val finalStatementLine = functionNode.statements.last().lineNumber
            val functionFrame = Frame(functionNode.lineNumber, finalStatementLine, argumentVariables)
            // program counter will forward in loop, we have popped out of stack
            return functionFrame.execute()
        }


        private fun fetchNextStatement() {
            ++pc
        }

        private fun executeAssignment(node: DeclarationOrAssignment) {
            val rhsValue = when (node.expression) {
                is ConstructorNode -> executeConstructor(node.expression as ConstructorNode, node.identifier)
                else -> executeExpression(node.expression)
            }
            variables[node.identifier] = rhsValue

            // to visualise popping back to assignment we can move line to the prior assignment statement again
            if (node.expression is FunctionCallNode) {
                moveToLine()
            }
        }




        private fun executeExpression(node: ExpressionNode, insideMethodCall: Boolean = false): ExecValue {
            return when (node) {
                is IdentifierNode -> variables[node.identifier]!!
                is NumberNode -> DoubleValue(node.double)
                is MethodCallNode -> executeMethodCall(node, insideMethodCall)
                is AddExpression -> executeBinaryOp(node) { x, y -> x + y }
                is SubtractExpression -> executeBinaryOp(node) { x, y -> x - y }
                is MultiplyExpression -> executeBinaryOp(node) { x, y -> x * y }
                is PlusExpression -> executeUnaryOp(node) { x -> x }
                is MinusExpression -> executeUnaryOp(node) { x -> -x }
                is ConstructorNode -> TODO()
                is FunctionCallNode -> executeFunctionCall(node)
            }
        }

        private fun executeMethodCall(node: MethodCallNode, insideMethodCall: Boolean): ExecValue {
            return when (val ds = variables[node.instanceIdentifier]) {
                is StackValue -> {
                    return when (node.dataStructureMethod) {
                        is StackType.PushMethod -> {
                            val doubleValue = executeExpression(node.arguments[0], true) as DoubleValue
                            val secondObject = if (ds.stack.empty()) ds.initObject else ds.stack.peek().second

                            val hasOldShape = doubleValue.manimObject != null

                            val rectangleShape = Rectangle(doubleValue.value.toString())
                            val oldShape = doubleValue.manimObject ?: EmptyMObject
                            val rectangle = if (hasOldShape) oldShape else NewMObject(
                                    rectangleShape,
                                    variableNameGenerator.generateShapeName(rectangleShape),
                                    codeTextVariable
                            )

                            val instructions =
                                    mutableListOf<ManimInstr>(MoveObject(rectangle.ident, secondObject.ident, ObjectSide.ABOVE))
                            if (!hasOldShape) {
                                instructions.add(0, rectangle)
                            }

                            linearRepresentation.addAll(instructions)
                            ds.stack.push(Pair(doubleValue.value, rectangle))
                            doubleValue
                        }
                        is StackType.PopMethod -> {
                            val poppedValue = ds.stack.pop()
                            val secondObject = if (ds.stack.empty()) ds.initObject else ds.stack.peek().second

                            val topOfStack = poppedValue.second
                            val instructions = listOf(
                                    MoveObject(
                                            topOfStack.ident,
                                            secondObject.ident,
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

        private fun executeConstructor(node: ConstructorNode, identifier: String): ExecValue = when (node.type) {
            is StackType -> {
                val numStack = variables.values.filterIsInstance(StackValue::class.java).lastOrNull()
                val (instructions, newObject) = if (numStack == null) {
                    val stackInit = InitStructure(
                            2,
                            -1,
                            Alignment.HORIZONTAL,
                            variableNameGenerator.generateNameFromPrefix("empty"),
                            identifier
                    )
                    // Add to stack of objects to keep track of identifier
                    Pair(listOf(stackInit), stackInit)
                } else {
                    val stackInit = InitStructureRelative(
                            Alignment.HORIZONTAL,
                            variableNameGenerator.generateNameFromPrefix("empty"),
                            identifier,
                            numStack.initObject.ident
                    )
                    Pair(listOf(stackInit), stackInit)
                }
                linearRepresentation.addAll(instructions)
                StackValue(newObject, Stack())
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
