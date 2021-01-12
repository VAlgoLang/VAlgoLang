package com.valgolang.frontend.ast

import com.valgolang.frontend.datastructures.DataStructureMethod
import com.valgolang.frontend.datastructures.DataStructureType

// Expressions
sealed class ExpressionNode(override val lineNumber: Int) : CodeNode(lineNumber)

interface AssignLHS {
    val identifier: String
}

object EmptyLHS : AssignLHS {
    override val identifier: String = ""
}

data class IdentifierNode(override val lineNumber: Int, override val identifier: String) :
    ExpressionNode(lineNumber),
    AssignLHS {
    override fun toString(): String {
        return identifier
    }
}

data class ArrayElemNode(
    override val lineNumber: Int,
    override val identifier: String,
    val indices: List<ExpressionNode>,
    val internalType: Type
) :
    ExpressionNode(lineNumber), AssignLHS {
    override fun toString(): String {
        return "$identifier[$indices]"
    }
}

sealed class BinaryTreeNodeAccess(lineNumber: Int) : ExpressionNode(lineNumber), AssignLHS

data class BinaryTreeNodeElemAccessNode(
    override val lineNumber: Int,
    override var identifier: String,
    val accessChain: List<DataStructureMethod>,
) : BinaryTreeNodeAccess(lineNumber) {
    override fun toString(): String {
        return "$identifier.${accessChain.joinToString(".")}"
    }
}

data class BinaryTreeRootAccessNode(
    override val lineNumber: Int,
    override val identifier: String,
    val elemAccessNode: BinaryTreeNodeElemAccessNode
) : BinaryTreeNodeAccess(lineNumber) {
    override fun toString(): String {
        return "$identifier.root.${elemAccessNode.accessChain.joinToString(".")}"
    }
}

data class NumberNode(override val lineNumber: Int, val double: Double) : ExpressionNode(lineNumber)
data class BoolNode(override val lineNumber: Int, val value: Boolean) : ExpressionNode(lineNumber)
data class CharNode(override val lineNumber: Int, val value: Char) : ExpressionNode(lineNumber)
data class StringNode(override val lineNumber: Int, val value: String) : ExpressionNode(lineNumber)
data class VoidNode(override val lineNumber: Int) : ExpressionNode(lineNumber)

data class MethodCallNode(
    override val lineNumber: Int,
    val instanceIdentifier: String,
    val dataStructureMethod: DataStructureMethod,
    val arguments: List<ExpressionNode>,
    override val identifier: String = ""
) : ExpressionNode(lineNumber), AssignLHS

data class InternalArrayMethodCallNode(
    override val lineNumber: Int,
    val index: ExpressionNode,
    val instanceIdentifier: String,
    val dataStructureMethod: DataStructureMethod,
    val arguments: List<ExpressionNode>
) : ExpressionNode(lineNumber)

data class ConstructorNode(
    override val lineNumber: Int,
    val type: DataStructureType,
    val arguments: List<ExpressionNode>,
    val initialiser: InitialiserNode
) : ExpressionNode(lineNumber)

data class NullNode(override val lineNumber: Int) : ExpressionNode(lineNumber)

sealed class InitialiserNode : ASTNode()

object EmptyInitialiserNode : InitialiserNode()

data class DataStructureInitialiserNode(
    val expressions: List<ExpressionNode>
) : InitialiserNode()

data class Array2DInitialiserNode(
    val nestedExpressions: List<List<ExpressionNode>>
) : InitialiserNode()

data class FunctionCallNode(
    override val lineNumber: Int,
    val functionIdentifier: String,
    val arguments: List<ExpressionNode>
) : ExpressionNode(lineNumber)

data class CastExpressionNode(
    override val lineNumber: Int,
    val targetType: PrimitiveType,
    val expr: ExpressionNode
) : ExpressionNode(lineNumber)

// Method calls
sealed class MethodClassNode

interface ComparableTypes {
    val compatibleTypes: Set<Type>
}

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
) : BinaryExpression(lineNumber, expr1, expr2), ComparableTypes {
    override val compatibleTypes: Set<Type> = setOf(CharType, NumberType, StringType)
}

data class SubtractExpression(
    override val lineNumber: Int,
    override val expr1: ExpressionNode,
    override val expr2: ExpressionNode
) : BinaryExpression(lineNumber, expr1, expr2), ComparableTypes {
    override val compatibleTypes: Set<Type> = setOf(CharType, NumberType)
}

data class MultiplyExpression(
    override val lineNumber: Int,
    override val expr1: ExpressionNode,
    override val expr2: ExpressionNode
) : BinaryExpression(lineNumber, expr1, expr2), ComparableTypes {
    override val compatibleTypes: Set<Type> = setOf(CharType, NumberType)
}

data class DivideExpression(
    override val lineNumber: Int,
    override val expr1: ExpressionNode,
    override val expr2: ExpressionNode
) : BinaryExpression(lineNumber, expr1, expr2), ComparableTypes {
    override val compatibleTypes: Set<Type> = setOf(CharType, NumberType)
}

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
