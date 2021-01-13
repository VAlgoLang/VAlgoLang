package com.valgolang.frontend.ast

/**
 * Expression node
 *
 * Represents expressions in VAlgoLang
 *
 * @property lineNumber
 * @constructor Create empty Expression node
 */
abstract class ExpressionNode(override val lineNumber: Int) : CodeNode(lineNumber)

/**
 * Assign lhs
 *
 * Represents node that can be on the lhs of an assignment '='
 *
 * @constructor Create empty Assign lhs
 */
interface AssignLHS {
    val identifier: String
}

object EmptyLHS : AssignLHS {
    override val identifier: String = ""
}

/**
 * Identifier node
 *
 * Identifies a variable
 *
 * @property lineNumber
 * @property identifier
 * @constructor Create empty Identifier node
 */
data class IdentifierNode(override val lineNumber: Int, override val identifier: String) :
    ExpressionNode(lineNumber),
    AssignLHS {
    override fun toString(): String {
        return identifier
    }
}

/**
 * Number node
 *
 * Represents a number
 *
 * @property lineNumber
 * @property double
 * @constructor Create empty Number node
 */
data class NumberNode(override val lineNumber: Int, val double: Double) : ExpressionNode(lineNumber)

/**
 * Bool node
 *
 * Represents a boolean value
 *
 * @property lineNumber
 * @property value
 * @constructor Create empty Bool node
 */
data class BoolNode(override val lineNumber: Int, val value: Boolean) : ExpressionNode(lineNumber)

/**
 * Char node
 *
 * Represents a character value
 *
 * @property lineNumber
 * @property value
 * @constructor Create empty Char node
 */
data class CharNode(override val lineNumber: Int, val value: Char) : ExpressionNode(lineNumber)

/**
 * String node
 *
 * Represents a string value
 *
 * @property lineNumber
 * @property value
 * @constructor Create empty String node
 */
data class StringNode(override val lineNumber: Int, val value: String) : ExpressionNode(lineNumber)

/**
 * Void node
 *
 * Represents a void value e.g. from a void function
 *
 * @property lineNumber
 * @constructor Create empty Void node
 */
data class VoidNode(override val lineNumber: Int) : ExpressionNode(lineNumber)

/**
 * Null node
 *
 * @property lineNumber
 * @constructor Create empty Null node
 */
data class NullNode(override val lineNumber: Int) : ExpressionNode(lineNumber)

/**
 * Initialiser node
 *
 *
 * @constructor Create empty Initialiser node
 */
abstract class InitialiserNode : ASTNode()

object EmptyInitialiserNode : InitialiserNode()

/**
 * Function call node
 *
 * @property lineNumber
 * @property functionIdentifier
 * @property arguments
 * @constructor Create empty Function call node
 */
data class FunctionCallNode(
    override val lineNumber: Int,
    val functionIdentifier: String,
    val arguments: List<ExpressionNode>
) : ExpressionNode(lineNumber)

/**
 * Cast expression node
 *
 * @property lineNumber
 * @property targetType
 * @property expr
 * @constructor Create empty Cast expression node
 */
data class CastExpressionNode(
    override val lineNumber: Int,
    val targetType: PrimitiveType,
    val expr: ExpressionNode
) : ExpressionNode(lineNumber)

/**
 * Comparable types
 *
 * @constructor Create empty Comparable types
 */
interface ComparableTypes {
    val compatibleTypes: Set<Type>
}

/**
 * Binary expression
 *
 * @property lineNumber
 * @property expr1
 * @property expr2
 * @constructor Create empty Binary expression
 */
sealed class BinaryExpression(
    override val lineNumber: Int,
    open val expr1: ExpressionNode,
    open val expr2: ExpressionNode
) : ExpressionNode(lineNumber)

/**
 * Add expression
 *
 * @property lineNumber
 * @property expr1
 * @property expr2
 * @constructor Create empty Add expression
 */
data class AddExpression(
    override val lineNumber: Int,
    override val expr1: ExpressionNode,
    override val expr2: ExpressionNode
) : BinaryExpression(lineNumber, expr1, expr2), ComparableTypes {
    override val compatibleTypes: Set<Type> = setOf(CharType, NumberType, StringType)
}

/**
 * Subtract expression
 *
 * @property lineNumber
 * @property expr1
 * @property expr2
 * @constructor Create empty Subtract expression
 */
data class SubtractExpression(
    override val lineNumber: Int,
    override val expr1: ExpressionNode,
    override val expr2: ExpressionNode
) : BinaryExpression(lineNumber, expr1, expr2), ComparableTypes {
    override val compatibleTypes: Set<Type> = setOf(CharType, NumberType)
}

/**
 * Multiply expression
 *
 * @property lineNumber
 * @property expr1
 * @property expr2
 * @constructor Create empty Multiply expression
 */
data class MultiplyExpression(
    override val lineNumber: Int,
    override val expr1: ExpressionNode,
    override val expr2: ExpressionNode
) : BinaryExpression(lineNumber, expr1, expr2), ComparableTypes {
    override val compatibleTypes: Set<Type> = setOf(CharType, NumberType)
}

/**
 * Divide expression
 *
 * @property lineNumber
 * @property expr1
 * @property expr2
 * @constructor Create empty Divide expression
 */
data class DivideExpression(
    override val lineNumber: Int,
    override val expr1: ExpressionNode,
    override val expr2: ExpressionNode
) : BinaryExpression(lineNumber, expr1, expr2), ComparableTypes {
    override val compatibleTypes: Set<Type> = setOf(CharType, NumberType)
}

/**
 * And expression
 *
 * @property lineNumber
 * @property expr1
 * @property expr2
 * @constructor Create empty And expression
 */
data class AndExpression(
    override val lineNumber: Int,
    override val expr1: ExpressionNode,
    override val expr2: ExpressionNode
) : BinaryExpression(lineNumber, expr1, expr2)

/**
 * Or expression
 *
 * @property lineNumber
 * @property expr1
 * @property expr2
 * @constructor Create empty Or expression
 */
data class OrExpression(
    override val lineNumber: Int,
    override val expr1: ExpressionNode,
    override val expr2: ExpressionNode
) : BinaryExpression(lineNumber, expr1, expr2)

/**
 * Eq expression
 *
 * @property lineNumber
 * @property expr1
 * @property expr2
 * @constructor Create empty Eq expression
 */
data class EqExpression(
    override val lineNumber: Int,
    override val expr1: ExpressionNode,
    override val expr2: ExpressionNode
) : BinaryExpression(lineNumber, expr1, expr2)

/**
 * Neq expression
 *
 * @property lineNumber
 * @property expr1
 * @property expr2
 * @constructor Create empty Neq expression
 */
data class NeqExpression(
    override val lineNumber: Int,
    override val expr1: ExpressionNode,
    override val expr2: ExpressionNode
) : BinaryExpression(lineNumber, expr1, expr2)

/**
 * Gt expression
 *
 * @property lineNumber
 * @property expr1
 * @property expr2
 * @constructor Create empty Gt expression
 */
data class GtExpression(
    override val lineNumber: Int,
    override val expr1: ExpressionNode,
    override val expr2: ExpressionNode
) : BinaryExpression(lineNumber, expr1, expr2)

/**
 * Lt expression
 *
 * @property lineNumber
 * @property expr1
 * @property expr2
 * @constructor Create empty Lt expression
 */
data class LtExpression(
    override val lineNumber: Int,
    override val expr1: ExpressionNode,
    override val expr2: ExpressionNode
) : BinaryExpression(lineNumber, expr1, expr2)

/**
 * Ge expression
 *
 * @property lineNumber
 * @property expr1
 * @property expr2
 * @constructor Create empty Ge expression
 */
data class GeExpression(
    override val lineNumber: Int,
    override val expr1: ExpressionNode,
    override val expr2: ExpressionNode
) : BinaryExpression(lineNumber, expr1, expr2)

/**
 * Le expression
 *
 * @property lineNumber
 * @property expr1
 * @property expr2
 * @constructor Create empty Le expression
 */
data class LeExpression(
    override val lineNumber: Int,
    override val expr1: ExpressionNode,
    override val expr2: ExpressionNode
) : BinaryExpression(lineNumber, expr1, expr2)

/**
 * Unary expression
 *
 * @property lineNumber
 * @property expr
 * @constructor Create empty Unary expression
 */ // Unary Expressions
sealed class UnaryExpression(override val lineNumber: Int, open val expr: ExpressionNode) : ExpressionNode(lineNumber)

/**
 * Plus expression
 *
 * @property lineNumber
 * @property expr
 * @constructor Create empty Plus expression
 */
data class PlusExpression(override val lineNumber: Int, override val expr: ExpressionNode) :
    UnaryExpression(lineNumber, expr)

/**
 * Minus expression
 *
 * @property lineNumber
 * @property expr
 * @constructor Create empty Minus expression
 */
data class MinusExpression(override val lineNumber: Int, override val expr: ExpressionNode) :
    UnaryExpression(lineNumber, expr)

/**
 * Not expression
 *
 * @property lineNumber
 * @property expr
 * @constructor Create empty Not expression
 */
data class NotExpression(override val lineNumber: Int, override val expr: ExpressionNode) :
    UnaryExpression(lineNumber, expr)
