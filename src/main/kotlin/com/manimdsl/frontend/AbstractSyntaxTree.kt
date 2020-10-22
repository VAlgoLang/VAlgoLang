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

// All statements making up program
sealed class StatementNode : ASTNode()

// Animation Command Specific type for easy detection
sealed class AnimationNode : StatementNode()
data class SleepNode(val sleepTime: ExpressionNode) : AnimationNode()

// Comments (not discarded so they can be rendered for educational purposes)
data class CommentNode(val content: String) : AnimationNode()

// Used to more easily deal with blocks
data class ConsecutiveStatementNode(val stat1: StatementNode, val stat2: StatementNode) : StatementNode()

// Code Specific Nodes holding line number - todo: replace with composition
sealed class CodeNode(open val lineNumber: Int) : StatementNode()

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

data class IfStatement(
    override val lineNumber: Int,
    val ifScope: Int,
    val ifCondition: ExpressionNode,
    val ifStatement: List<StatementNode>,
    val elifs: List<Elif> = emptyList(),
    val elseScope: Int = 0,
    val elseStatement: List<StatementNode> = emptyList()
) : CodeNode(lineNumber)

data class Elif(
    val scope: Int,
    val condition: ExpressionNode,
    val statements: List<StatementNode>
) : StatementNode()

// Expressions
sealed class ExpressionNode(override val lineNumber: Int) : CodeNode(lineNumber)
data class IdentifierNode(override val lineNumber: Int, val identifier: String) : ExpressionNode(lineNumber)
data class NumberNode(override val lineNumber: Int, val double: Double) : ExpressionNode(lineNumber)
data class BoolNode(override val lineNumber: Int, val value: Boolean) : ExpressionNode(lineNumber)
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

data class AndExpression(
    override val lineNumber: Int,
    override val expr1: ExpressionNode,
    override val expr2: ExpressionNode
) : BinaryExpression(lineNumber, expr1, expr2)

data class OrExpression(
    override val lineNumber: Int,
    override val expr1: ExpressionNode,
    override val expr2: ExpressionNode
) : BinaryExpression(lineNumber, expr1, expr2)

data class EqExpression(
    override val lineNumber: Int,
    override val expr1: ExpressionNode,
    override val expr2: ExpressionNode
) : BinaryExpression(lineNumber, expr1, expr2)

data class NeqExpression(
    override val lineNumber: Int,
    override val expr1: ExpressionNode,
    override val expr2: ExpressionNode
) : BinaryExpression(lineNumber, expr1, expr2)

data class GtExpression(
    override val lineNumber: Int,
    override val expr1: ExpressionNode,
    override val expr2: ExpressionNode
) : BinaryExpression(lineNumber, expr1, expr2)

data class LtExpression(
    override val lineNumber: Int,
    override val expr1: ExpressionNode,
    override val expr2: ExpressionNode
) : BinaryExpression(lineNumber, expr1, expr2)

data class GeExpression(
    override val lineNumber: Int,
    override val expr1: ExpressionNode,
    override val expr2: ExpressionNode
) : BinaryExpression(lineNumber, expr1, expr2)

data class LeExpression(
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

data class NotExpression(override val lineNumber: Int, override val expr: ExpressionNode) :
    UnaryExpression(lineNumber, expr)
