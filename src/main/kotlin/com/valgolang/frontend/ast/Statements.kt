package com.valgolang.frontend.ast

/**
 * Statement node
 *
 * Represents a statement that constitutes the program
 *
 * @property lineNumber
 * @constructor Create empty Statement node
 */
abstract class StatementNode(open val lineNumber: Int) : ASTNode()

/**
 * Consecutive statement node
 *
 * Recursive statement node which wraps two consecutive statements
 *
 * @property stat1
 * @property stat2
 * @constructor Create empty Consecutive statement node
 */ // Used to more easily deal with blocks
data class ConsecutiveStatementNode(val stat1: StatementNode, val stat2: StatementNode) :
    StatementNode(stat1.lineNumber)

/**
 * Declaration or assignment
 *
 * @constructor Create empty Declaration or assignment
 */
interface DeclarationOrAssignment {
    val lineNumber: Int
    val identifier: AssignLHS
    val expression: ExpressionNode
}

/**
 * Declaration node
 *
 * @property lineNumber
 * @property identifier
 * @property expression
 * @constructor Create empty Declaration node
 */
data class DeclarationNode(
    override val lineNumber: Int,
    override val identifier: AssignLHS,
    override val expression: ExpressionNode
) : CodeNode(lineNumber), DeclarationOrAssignment

/**
 * Assignment node
 *
 * @property lineNumber
 * @property identifier
 * @property expression
 * @constructor Create empty Assignment node
 */
data class AssignmentNode(
    override val lineNumber: Int,
    override val identifier: AssignLHS,
    override val expression: ExpressionNode
) : CodeNode(lineNumber), DeclarationOrAssignment

/**
 * Return node
 *
 * @property lineNumber
 * @property expression
 * @constructor Create empty Return node
 */
data class ReturnNode(
    override val lineNumber: Int,
    val expression: ExpressionNode
) : CodeNode(lineNumber)

/**
 * Statement block
 *
 * @property lineNumber
 * @constructor Create empty Statement block
 */
sealed class StatementBlock(
    override val lineNumber: Int
) : CodeNode(lineNumber) {
    abstract val statements: List<StatementNode>
    abstract val scope: Int
}

/**
 * Loop node
 *
 * @property lineNumber
 * @constructor Create empty Loop node
 */
sealed class LoopNode(override val lineNumber: Int) : StatementBlock(lineNumber) {
    abstract val endLineNumber: Int
}

/**
 * While statement node
 *
 * @property lineNumber
 * @property endLineNumber
 * @property scope
 * @property condition
 * @property statements
 * @constructor Create empty While statement node
 */
data class WhileStatementNode(
    override val lineNumber: Int,
    override val endLineNumber: Int,
    override val scope: Int,
    val condition: ExpressionNode,
    override val statements: List<StatementNode>
) : LoopNode(lineNumber)

/**
 * For statement node
 *
 * @property lineNumber
 * @property endLineNumber
 * @property scope
 * @property beginStatement
 * @property endCondition
 * @property updateCounter
 * @property statements
 * @constructor Create empty For statement node
 */
data class ForStatementNode(
    override val lineNumber: Int,
    override val endLineNumber: Int,
    override val scope: Int,
    val beginStatement: DeclarationNode,
    val endCondition: ExpressionNode,
    val updateCounter: AssignmentNode,
    override val statements: List<StatementNode>,
) : LoopNode(lineNumber)

/**
 * If statement node
 *
 * @property lineNumber
 * @property endLineNumber
 * @property scope
 * @property condition
 * @property statements
 * @property elifs
 * @property elseBlock
 * @constructor Create empty If statement node
 */
data class IfStatementNode(
    override val lineNumber: Int,
    val endLineNumber: Int,
    override val scope: Int,
    val condition: ExpressionNode,
    override val statements: List<StatementNode>,
    val elifs: List<ElifNode> = emptyList(),
    val elseBlock: ElseNode
) : StatementBlock(lineNumber)

/**
 * Elif node
 *
 * @property lineNumber
 * @property scope
 * @property condition
 * @property statements
 * @constructor Create empty Elif node
 */
data class ElifNode(
    override val lineNumber: Int,
    override val scope: Int,
    val condition: ExpressionNode,
    override val statements: List<StatementNode>
) : StatementBlock(lineNumber)

/**
 * Else node
 *
 * @property lineNumber
 * @property scope
 * @property statements
 * @constructor Create empty Else node
 */
data class ElseNode(
    override val lineNumber: Int,
    override val scope: Int,
    override val statements: List<StatementNode>
) : StatementBlock(lineNumber)

/**
 * Loop statement node
 *
 * @property lineNumber
 * @constructor Create empty Loop statement node
 */
sealed class LoopStatementNode(override val lineNumber: Int) : CodeNode(lineNumber)

/**
 * Break node
 *
 * @property lineNumber
 * @property loopEndLineNumber
 * @constructor Create empty Break node
 */
data class BreakNode(override val lineNumber: Int, val loopEndLineNumber: Int) : LoopStatementNode(lineNumber)

/**
 * Continue node
 *
 * @property lineNumber
 * @property loopStartLineNumber
 * @constructor Create empty Continue node
 */
data class ContinueNode(override val lineNumber: Int, val loopStartLineNumber: Int) : LoopStatementNode(lineNumber)
