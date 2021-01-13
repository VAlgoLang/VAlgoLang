package com.valgolang.frontend.datastructures.binarytree

import com.valgolang.frontend.ast.AssignLHS
import com.valgolang.frontend.ast.ExpressionNode
import com.valgolang.frontend.datastructures.DataStructureMethod

/**
 * Binary tree node access
 *
 * Represents access to a node in a binary tree
 *
 * @constructor
 *
 * @param lineNumber
 */
sealed class BinaryTreeNodeAccess(lineNumber: Int) : ExpressionNode(lineNumber), AssignLHS

/**
 * Binary tree node elem access node
 *
 * Represents access to a nodes in a binary tree e.g. node.left.right.value
 *
 * @property lineNumber
 * @property identifier
 * @property accessChain
 * @constructor Create empty Binary tree node elem access node
 */
data class BinaryTreeNodeElemAccessNode(
    override val lineNumber: Int,
    override var identifier: String,
    val accessChain: List<DataStructureMethod>,
) : BinaryTreeNodeAccess(lineNumber) {
    override fun toString(): String {
        return "$identifier.${accessChain.joinToString(".")}"
    }
}

/**
 * Binary tree root access node
 *
 * Represents access to the root of a binary tree wrapper type class
 *
 * @property lineNumber
 * @property identifier
 * @property elemAccessNode
 * @constructor Create empty Binary tree root access node
 */
data class BinaryTreeRootAccessNode(
    override val lineNumber: Int,
    override val identifier: String,
    val elemAccessNode: BinaryTreeNodeElemAccessNode
) : BinaryTreeNodeAccess(lineNumber) {
    override fun toString(): String {
        return "$identifier.root.${elemAccessNode.accessChain.joinToString(".")}"
    }
}
