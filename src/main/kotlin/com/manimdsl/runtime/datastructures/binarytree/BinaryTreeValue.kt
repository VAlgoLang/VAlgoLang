package com.manimdsl.runtime.datastructures.binarytree

import com.manimdsl.linearrepresentation.EmptyMObject
import com.manimdsl.linearrepresentation.MObject
import com.manimdsl.runtime.ExecValue
import com.manimdsl.runtime.PrimitiveValue
import com.manimdsl.stylesheet.AnimationProperties
import com.manimdsl.stylesheet.StyleProperties

sealed class ITreeNodeValue : ExecValue() {
    abstract fun nodeCount(): Int
}

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

// value is element in node
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
    fun attachTree(tree: BinaryTreeValue, prefix: String = "${tree.manimObject.shape.ident}.root") {
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

// value is root
data class BinaryTreeValue(override var manimObject: MObject, override var value: BinaryTreeNodeValue, var style: StyleProperties = StyleProperties(), var animatedStyle: AnimationProperties? = null) : ExecValue() {

    override val name: String = "Tree"
    override fun clone(): ExecValue {
        return BinaryTreeValue(manimObject, value, style, animatedStyle)
    }

    override fun toString(): String {
        return "$value"
    }
}
