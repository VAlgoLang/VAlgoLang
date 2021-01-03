package com.manimdsl.runtime.datastructures.binarytree

import com.manimdsl.linearrepresentation.EmptyMObject
import com.manimdsl.linearrepresentation.MObject
import com.manimdsl.runtime.ExecValue
import com.manimdsl.runtime.PrimitiveValue
import com.manimdsl.stylesheet.AnimationProperties
import com.manimdsl.stylesheet.StyleProperties

/**
 * Binary Tree Node Execution Value interface. Represents the type of ExecValues that can be nodes of a binary tree.
 * @constructor: Creates a new Binary Tree Node Execution Value.
 *
 */

sealed class ITreeNodeValue : ExecValue() {
    abstract fun nodeCount(): Int
}

/**
 * Binary Tree Node Execution Value
 *
 * @property left: Left ITreeNodeValue child.
 * @property right: Right ITreeNodeValue child.
 * @property manimObject: Manim Object corresponded to by the BinaryTreeNodeValue.
 * @property value: Current primitive value held by node.
 * @property binaryTreeValue: Tree node is attached to. Null if not present (i.e. not rendered).
 * @property pathFromRoot: String path from root. Constructed automatically in most cases.
 * @property depth: Depth of node in tree.
 * @constructor: Creates a new Binary Tree Node Execution Value.
 *
 */

data class BinaryTreeNodeValue(
    var left: ITreeNodeValue = NullValue,
    var right: ITreeNodeValue = NullValue,
    override val value: PrimitiveValue,
    override var manimObject: MObject = EmptyMObject,
    var binaryTreeValue: BinaryTreeValue? = null,
    var pathFromRoot: String = "",
    var depth: Int
) : ITreeNodeValue() {
    override fun clone(): ExecValue {
        return BinaryTreeNodeValue(left, right, value, manimObject, depth = depth)
    }

    override fun nodeCount(): Int = 1 + left.nodeCount() + right.nodeCount()

    override val name: String = "Tree"
    fun attachTree(tree: BinaryTreeValue, prefix: String = "${tree.manimObject.ident}.root") {
        binaryTreeValue = tree
        pathFromRoot = prefix
        if (left is BinaryTreeNodeValue) {
            (left as BinaryTreeNodeValue).attachTree(tree, "$prefix.left")
        }
        if (right is BinaryTreeNodeValue) {
            (right as BinaryTreeNodeValue).attachTree(tree, "$prefix.right")
        }
    }

    override fun toString(): String {
        val stringBuilder = StringBuilder(value.toString())
        if (left is NullValue) {
            stringBuilder.append(" $left")
        } else {
            stringBuilder.append(" (${left.value}...)")
        }
        if (right is NullValue) {
            stringBuilder.append(" $right")
        } else {
            stringBuilder.append(" (${right.value}...)")
        }

        return stringBuilder.toString()
    }
}

/**
 * Binary Tree Execution Value. Differs from node value as this represents the renderable tree.
 *
 * @property manimObject: Manim Object corresponded to by the BinaryTreeValue.
 * @property value: Current root of renderable tree.
 * @property binaryTreeValue: Tree node is attached to. Null if not present (i.e. not rendered).
 * @property style: Static style properties to apply.
 * @property animatedStyle: Dynamic style properties to apply.
 * @constructor: Creates a new Binary Tree Execution Value.
 *
 */

data class BinaryTreeValue(override var manimObject: MObject, override var value: BinaryTreeNodeValue, var style: StyleProperties = StyleProperties(), var animatedStyle: AnimationProperties? = null) : ExecValue() {

    override val name: String = "Tree"
    override fun clone(): ExecValue {
        return BinaryTreeValue(manimObject, value, style, animatedStyle)
    }

    override fun toString(): String {
        return "$value"
    }
}

/**
 * Null Execution Value singleton.
 *
 */

object NullValue : ITreeNodeValue() {
    override val name: String = "Null"
    override fun nodeCount(): Int = 0

    override var manimObject: MObject = EmptyMObject
    override val value: Int = 0

    override fun clone(): ExecValue {
        return this
    }

    override fun toString(): String {
        return "()"
    }
}
