package com.manimdsl

import com.manimdsl.frontend.*
import com.manimdsl.linearrepresentation.*
import java.util.*

// Wrapper classes for values of variables while executing code
sealed class ExecValue

data class DoubleValue(val value: Double) : ExecValue()

data class StackValue(val initObject: Object, val stack: Stack<Pair<Double, Object>>) : ExecValue()

object EmptyValue : ExecValue()

class ASTExecutor(private val program: ProgramNode, private val symbolTable: SymbolTable, val fileLines: List<String>) {

    private val linearRepresentation = mutableListOf<ManimInstr>()
    private val variableNameGenerator = VariableNameGenerator(symbolTable)

    private val codeBlockVariable: String
    private val codeTextVariable: String
    private val pointerVariable: String

    init {
        pointerVariable = variableNameGenerator.generateNameFromPrefix("pointer")
        codeBlockVariable = variableNameGenerator.generateNameFromPrefix("code_block")
        codeTextVariable = variableNameGenerator.generateNameFromPrefix("code_text")
    }

    // Map of all the variables and there current values
    private val variables = mutableMapOf<String, ExecValue>()

    // Keep track of next instruction to execute
    private var programCounter = 0

    private val finalDSLCode = mutableListOf<String>()

    private fun executeExpression(node: ExpressionNode): ExecValue {
        return when (node) {
            is IdentifierNode -> variables[node.identifier]!!
            is NumberNode -> DoubleValue(node.double)
            is MethodCallNode -> executeMethodCall(node)
            is AddExpression -> executeBinaryOp(node) { x, y -> x + y }
            is SubtractExpression -> executeBinaryOp(node) { x, y -> x - y }
            is MultiplyExpression -> executeBinaryOp(node) { x, y -> x * y }
            is PlusExpression -> executeUnaryOp(node) { x -> x }
            is MinusExpression -> executeUnaryOp(node) { x -> -x }
            else -> EmptyValue
        }
    }

    private fun executeMethodCall(node: MethodCallNode): ExecValue {
        return when (val ds = variables[node.instanceIdentifier]) {
            is StackValue -> {
                return when (node.dataStructureMethod) {
                    is StackType.PushMethod -> {
                        val doubleValue = executeExpression(node.arguments[0]) as DoubleValue
                        val secondObject = if (ds.stack.empty()) ds.initObject else ds.stack.peek().second
                        val (instructions, newObject) = node.dataStructureMethod.animateMethod(listOf(doubleValue.value.toString()), mapOf("top" to secondObject, "generator" to variableNameGenerator))
                        linearRepresentation.addAll(instructions)
                        ds.stack.push(Pair(doubleValue.value, newObject!!))
                        doubleValue
                    }
                    is StackType.PopMethod -> {
                        val poppedValue = ds.stack.pop()
                        val secondObject = if (ds.stack.empty()) ds.initObject else ds.stack.peek().second
                        val (instructions, _) = node.dataStructureMethod.animateMethod(
                            emptyList(),
                            mapOf(
                                "top" to poppedValue.second,
                                "second" to secondObject
                            )
                        )
                        linearRepresentation.addAll(instructions)
                        DoubleValue(poppedValue.first)
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
                val (instructions, newObject) = node.type.init(variableNameGenerator.generateNameFromPrefix("empty"), 2, -1, identifier)
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

    // Returns whether the program is complete and the state of all the variables after executing the statement
    fun executeNextStatement(): Pair<Boolean, List<ManimInstr>> {
        val node = program.statements[programCounter] as StatementNode
        programCounter++

        if (node is CodeNode){
            finalDSLCode.add(fileLines[node.lineNumber - 1])
            linearRepresentation.add(MoveToLine(finalDSLCode.size, pointerVariable, codeBlockVariable))
        }

        when (node) {
            is DeclarationOrAssignment -> visitAssignmentOrDeclaration(node)
            is ExpressionNode -> executeExpression(node)
            is SleepNode -> linearRepresentation.add(Sleep((executeExpression(node.sleepTime) as DoubleValue).value))
        }

        val endOfProgram = program.statements.size == programCounter
        if(endOfProgram) {
            linearRepresentation.add(0, CodeBlock(finalDSLCode, codeBlockVariable, codeTextVariable, pointerVariable))
        }

        return Pair(endOfProgram, linearRepresentation)
    }

    private fun visitAssignmentOrDeclaration(node: DeclarationOrAssignment) {
        variables[node.identifier] =
            when (node.expression) {
                is ConstructorNode -> executeConstructor(node.expression as ConstructorNode, node.identifier)
                else -> executeExpression(node.expression)
            }
    }

    fun getValue(identifier: String): ExecValue {
        return this.variables.getOrDefault(identifier, EmptyValue)
    }
}