package com.manimdsl

import com.manimdsl.frontend.*
import com.manimdsl.linearrepresentation.Object
import java.util.*

// Wrapper classes for values of variables while executing code
sealed class ExecValue

data class DoubleValue(val value: Double) : ExecValue()

data class StackValue(val stack: Stack<Pair<Double, Object>>) : ExecValue()

object EmptyValue : ExecValue()

class ASTExecutor(private val program: ProgramNode) {

    // Map of all the variables and there current values
    private val variables = mutableMapOf<String, ExecValue>()

    // Keep track of next instruction to execute
    private var programCounter = 0

    private fun executeExpression(node: ExpressionNode): ExecValue {
        return when (node) {
            is IdentifierNode -> variables[node.identifier]!!
            is NumberNode -> DoubleValue(node.double)
            is MethodCallNode -> executeMethodCall(node)
            is ConstructorNode -> executeConstructor(node)
            is AddExpression -> executeBinaryOp(node) { x, y -> x + y }
            is SubtractExpression -> executeBinaryOp(node) { x, y -> x - y }
            is MultiplyExpression -> executeBinaryOp(node) { x, y -> x * y }
            is PlusExpression -> executeUnaryOp(node) { x -> x }
            is MinusExpression -> executeUnaryOp(node) { x -> -x }
        }
    }

    private fun executeMethodCall(node: MethodCallNode): ExecValue {
        return when (val ds = variables[node.instanceIdentifier]) {
            is StackValue -> {
                return when (node.dataStructureMethod) {
                    is StackType.PushMethod -> {
                        node.dataStructureMethod.animateMethod(
                            listOf((executeExpression(node.arguments[0]) as DoubleValue).toString())
                                    DoubleValue (ds.stack.push((executeExpression(node.arguments[0]) as DoubleValue).value).)
                    }
                    is StackType.PopMethod -> DoubleValue(ds.stack.pop().first)
                    else -> EmptyValue
                }
            }
            else -> EmptyValue
        }
    }

    private fun executeConstructor(node: ConstructorNode): ExecValue {
        return when (node.type) {
            is StackType -> StackValue(Stack())
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
    fun executeNextStatement(): Pair<Boolean, MutableMap<String, ExecValue>> {
        val node = program.statements[programCounter]
        programCounter++
        when (node) {
            is DeclarationNode -> variables[node.identifier] = executeExpression(node.expression)
            is AssignmentNode -> variables[node.identifier] = executeExpression(node.expression)
            is ExpressionNode -> executeExpression(node)
            // Comment or sleep command so skip to next statement
            else -> return executeNextStatement()
        }
        return Pair(program.statements.size == programCounter, variables)
    }

    fun executeStatement(statementNode: StatementNode) {
        when (statementNode) {
            is DeclarationNode -> variables[statementNode.identifier] = executeExpression(statementNode.expression)
            is AssignmentNode -> variables[statementNode.identifier] = executeExpression(statementNode.expression)
            is ExpressionNode -> executeExpression(statementNode)
            // Comment or sleep command so skip to next statement
        }
    }

    fun getValue(identifier: String): ExecValue {
        return this.variables.getOrDefault(identifier, EmptyValue)
    }
}