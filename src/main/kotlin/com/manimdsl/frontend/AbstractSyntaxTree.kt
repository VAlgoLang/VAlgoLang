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
sealed class NoRenderAnimationNode(override val lineNumber: Int) : AnimationNode(lineNumber)

data class SleepNode(override val lineNumber: Int, val sleepTime: ExpressionNode) : NoRenderAnimationNode(lineNumber)

// Comments (not discarded so they can be rendered for educational purposes)
data class CommentNode(override val lineNumber: Int, val content: String) : AnimationNode(lineNumber)

// Step over to step over code and avoid additional animations
data class StepIntoNode(override val lineNumber: Int, val endLineNumber: Int, val statements: List<StatementNode>) :
    NoRenderAnimationNode(lineNumber)

// Step over to step over code and avoid additional animations
data class StartStepIntoNode(override val lineNumber: Int) : NoRenderAnimationNode(lineNumber)

// Step over to step over code and avoid additional animations
data class StopStepIntoNode(override val lineNumber: Int) : NoRenderAnimationNode(lineNumber)


// Code Specific Nodes holding line number
sealed class CodeNode(override val lineNumber: Int) : StatementNode(lineNumber)

// Used to more easily deal with blocks
data class ConsecutiveStatementNode(val stat1: StatementNode, val stat2: StatementNode) :
    StatementNode(stat1.lineNumber)

interface DeclarationOrAssignment {
    val lineNumber: Int
    val identifier: AssignLHS
    val expression: ExpressionNode
}

data class DeclarationNode(
    override val lineNumber: Int,
    override val identifier: AssignLHS,
    override val expression: ExpressionNode
) : CodeNode(lineNumber), DeclarationOrAssignment

data class AssignmentNode(
    override val lineNumber: Int,
    override val identifier: AssignLHS,
    override val expression: ExpressionNode
) : CodeNode(lineNumber), DeclarationOrAssignment

data class ReturnNode(
    override val lineNumber: Int,
    val expression: ExpressionNode
) : CodeNode(lineNumber)

sealed class StatementBlock(
    override val lineNumber: Int
) : CodeNode(lineNumber) {
    abstract val statements: List<StatementNode>
    abstract val scope: Int
}

data class IfStatementNode(
    override val lineNumber: Int,
    val endLineNumber: Int,
    override val scope: Int,
    val condition: ExpressionNode,
    override val statements: List<StatementNode>,
    val elifs: List<ElifNode> = emptyList(),
    val elseBlock: ElseNode
) : StatementBlock(lineNumber)

data class ElifNode(
    override val lineNumber: Int,
    override val scope: Int,
    val condition: ExpressionNode,
    override val statements: List<StatementNode>
) : StatementBlock(lineNumber)

data class ElseNode(
    override val lineNumber: Int,
    override val scope: Int,
    override val statements: List<StatementNode>
) : StatementBlock(lineNumber)

interface AssignLHS {
    val identifier: String
}

object EmptyLHS: AssignLHS {
    override val identifier: String = ""
}

// Expressions
sealed class ExpressionNode(override val lineNumber: Int) : CodeNode(lineNumber)
data class IdentifierNode(override val lineNumber: Int, override val identifier: String) : ExpressionNode(lineNumber), AssignLHS
data class ArrayElemNode(override val lineNumber: Int, override val identifier: String, val index: ExpressionNode) :
    ExpressionNode(lineNumber), AssignLHS

data class NumberNode(override val lineNumber: Int, val double: Double) : ExpressionNode(lineNumber)
data class BoolNode(override val lineNumber: Int, val value: Boolean) : ExpressionNode(lineNumber)
data class VoidNode(override val lineNumber: Int) : ExpressionNode(lineNumber)
data class MethodCallNode(
    override val lineNumber: Int,
    val instanceIdentifier: String,
    val dataStructureMethod: DataStructureMethod,
    val arguments: List<ExpressionNode>
) : ExpressionNode(lineNumber)

data class ConstructorNode(
    override val lineNumber: Int,
    val type: DataStructureType,
    val arguments: List<ExpressionNode>,
    val initialValue: List<ExpressionNode>
) : ExpressionNode(lineNumber)

data class DataStructureInitialiserNode(
    val expressions: List<ExpressionNode>
) : ASTNode()

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
