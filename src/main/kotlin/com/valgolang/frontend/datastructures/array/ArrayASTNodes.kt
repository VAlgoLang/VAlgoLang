package com.valgolang.frontend.datastructures.array

import com.valgolang.frontend.ast.AssignLHS
import com.valgolang.frontend.ast.ExpressionNode
import com.valgolang.frontend.ast.InitialiserNode
import com.valgolang.frontend.ast.Type
import com.valgolang.frontend.datastructures.DataStructureMethod

/**
 * Array2d initialiser node
 *
 * @property nestedExpressions
 * @constructor Create empty Array2d initialiser node
 */
data class Array2DInitialiserNode(
    val nestedExpressions: List<List<ExpressionNode>>
) : InitialiserNode()

/**
 * Array elem node
 *
 * Array element such as identifier[index]
 *
 * @property lineNumber
 * @property identifier
 * @property indices
 * @property internalType
 * @constructor Create empty Array elem node
 */
data class ArrayElemNode(
    override val lineNumber: Int,
    override val identifier: String,
    val indices: List<ExpressionNode>,
    val internalType: Type
) : ExpressionNode(lineNumber), AssignLHS {
    override fun toString(): String {
        return "$identifier[$indices]"
    }
}

/**
 * Internal array method call node
 *
 * Used in 2d arrays for accessing internal sub arrays
 *
 * @property lineNumber
 * @property index
 * @property instanceIdentifier
 * @property dataStructureMethod
 * @property arguments
 * @constructor Create empty Internal array method call node
 */
data class InternalArrayMethodCallNode(
    override val lineNumber: Int,
    val index: ExpressionNode,
    val instanceIdentifier: String,
    val dataStructureMethod: DataStructureMethod,
    val arguments: List<ExpressionNode>
) : ExpressionNode(lineNumber)
