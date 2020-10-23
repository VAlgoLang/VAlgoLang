package com.manimdsl.frontend

open class ASTNode

data class ProgramNode(
        val functions: List<FunctionNode>,
        val statements: List<StatementNode>
) : ASTNode()

data class FunctionNode(
        override val lineNumber: Int,
        val scope: Int,
        val identifier: String,
        val parameters: List<ParameterNode>,
        val statements: List<StatementNode>
) : CodeNode(lineNumber)

data class ParameterListNode(val parameters: List<ParameterNode>) : ASTNode()
data class ParameterNode(val identifier: String, val type: Type) : ASTNode()

// All statements making up program
sealed class StatementNode(open val lineNumber: Int) : ASTNode()

// Animation Command Specific type for easy detection
sealed class AnimationNode(override val lineNumber: Int) : StatementNode(lineNumber)
data class SleepNode(override val lineNumber: Int, val sleepTime: ExpressionNode) : AnimationNode(lineNumber)

// Comments (not discarded so they can be rendered for educational purposes)
data class CommentNode(override val lineNumber: Int, val content: String) : AnimationNode(lineNumber)

// Code Specific Nodes holding line number
sealed class CodeNode(override val lineNumber: Int) : StatementNode(lineNumber)

interface DeclarationOrAssignment {
    val lineNumber: Int
    val identifier: String
    val expression: ExpressionNode
}

data class DeclarationNode(
    override val lineNumber: Int,
    override val identifier: String,
    override val expression: ExpressionNode
) : CodeNode(lineNumber), DeclarationOrAssignment

data class AssignmentNode(
    override val lineNumber: Int,
    override val identifier: String,
    override val expression: ExpressionNode
) : CodeNode(lineNumber), DeclarationOrAssignment

data class ReturnNode(
    override val lineNumber: Int,
    val expression: ExpressionNode
) : CodeNode(lineNumber)

// Expressions
sealed class ExpressionNode(override val lineNumber: Int) : CodeNode(lineNumber)
data class IdentifierNode(override val lineNumber: Int, val identifier: String) : ExpressionNode(lineNumber)
data class NumberNode(override val lineNumber: Int, val double: Double) : ExpressionNode(lineNumber)
data class MethodCallNode(
    override val lineNumber: Int,
    val instanceIdentifier: String,
    val dataStructureMethod: DataStructureMethod,
    val arguments: List<ExpressionNode>
) : ExpressionNode(lineNumber)

data class ConstructorNode(
    override val lineNumber: Int,
    val type: DataStructureType,
    val arguments: List<ExpressionNode>
) : ExpressionNode(lineNumber)

data class FunctionCallNode(
    override val lineNumber: Int,
    val functionIdentifier: String,
    val arguments: List<ExpressionNode>
) : ExpressionNode(lineNumber)

// Method calls
sealed class MethodClassNode

// Binary Expressions
sealed class BinaryExpression(
    override val lineNumber: Int,
    open val expr1: ExpressionNode,
    open val expr2: ExpressionNode
) : ExpressionNode(lineNumber)

data class AddExpression(
    override val lineNumber: Int,
    override val expr1: ExpressionNode,
    override val expr2: ExpressionNode
) : BinaryExpression(lineNumber, expr1, expr2)

data class SubtractExpression(
    override val lineNumber: Int,
    override val expr1: ExpressionNode,
    override val expr2: ExpressionNode
) : BinaryExpression(lineNumber, expr1, expr2)

data class MultiplyExpression(
    override val lineNumber: Int,
    override val expr1: ExpressionNode,
    override val expr2: ExpressionNode
) : BinaryExpression(lineNumber, expr1, expr2)

// Unary Expressions
sealed class UnaryExpression(override val lineNumber: Int, open val expr: ExpressionNode) : ExpressionNode(lineNumber)
data class PlusExpression(override val lineNumber: Int, override val expr: ExpressionNode) :
    UnaryExpression(lineNumber, expr)

data class MinusExpression(override val lineNumber: Int, override val expr: ExpressionNode) :
    UnaryExpression(lineNumber, expr)
