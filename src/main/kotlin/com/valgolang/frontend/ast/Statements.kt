package com.valgolang.frontend.ast

// All statements making up program
abstract class StatementNode(open val lineNumber: Int) : ASTNode()

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

sealed class LoopNode(override val lineNumber: Int) : StatementBlock(lineNumber) {
    abstract val endLineNumber: Int
}

data class WhileStatementNode(
    override val lineNumber: Int,
    override val endLineNumber: Int,
    override val scope: Int,
    val condition: ExpressionNode,
    override val statements: List<StatementNode>
) : LoopNode(lineNumber)

data class ForStatementNode(
    override val lineNumber: Int,
    override val endLineNumber: Int,
    override val scope: Int,
    val beginStatement: DeclarationNode,
    val endCondition: ExpressionNode,
    val updateCounter: AssignmentNode,
    override val statements: List<StatementNode>,
) : LoopNode(lineNumber)

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

sealed class LoopStatementNode(override val lineNumber: Int) : CodeNode(lineNumber)
data class BreakNode(override val lineNumber: Int, val loopEndLineNumber: Int) : LoopStatementNode(lineNumber)
data class ContinueNode(override val lineNumber: Int, val loopStartLineNumber: Int) : LoopStatementNode(lineNumber)
