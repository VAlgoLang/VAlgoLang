package com.manimdsl.runtime

import com.manimdsl.frontend.ErrorType
import com.manimdsl.linearrepresentation.EmptyMObject
import com.manimdsl.linearrepresentation.MObject
import com.manimdsl.stylesheet.AnimationProperties
import com.manimdsl.stylesheet.StyleProperties
import java.util.*
import kotlin.math.roundToInt

// Wrapper classes for values of variables while executing code
sealed class ExecValue {
    abstract var manimObject: MObject
    abstract val value: Any
    abstract val name: String
    /** Extend when more types are added these are assuming semantic checks have passed **/

    /** '+' **/
    operator fun plus(other: ExecValue): ExecValue = when (this) {
        is DoubleAlias -> DoubleValue((other as DoubleAlias).toDouble() + this.toDouble())
        else -> throw UnsupportedOperationException("Not implemented yet")
    }

    /** '-' **/
    operator fun minus(other: ExecValue): ExecValue = when (this) {
        is DoubleAlias -> DoubleValue(this.toDouble() - (other as DoubleAlias).toDouble())
        else -> throw UnsupportedOperationException("Not implemented yet")
    }

    /** '/' **/
    operator fun div(other: ExecValue): ExecValue = when (this) {
        is DoubleValue -> if (other is DoubleValue) DoubleValue((this.value / other.value).roundToInt().toDouble()) else throwTypeError()
        else -> throwTypeError()
    }

    /** '*' **/
    operator fun times(other: ExecValue): ExecValue = when (this) {
        is DoubleAlias -> DoubleValue((other as DoubleAlias).toDouble() * this.toDouble())
        else -> throw UnsupportedOperationException("Not implemented yet")
    }

    /** '!' **/
    operator fun not(): Boolean = when (this) {
        is BoolValue -> !this.value
        else -> throw UnsupportedOperationException("Not implemented yet")
    }

    /** '==','!=', '<', '<=', '>', '>='  **/
    operator fun compareTo(other: Any): Int = when (this) {
        is DoubleAlias -> this.toDouble().compareTo((other as DoubleAlias).toDouble())
        is BoolValue -> this.value.compareTo((other as BoolValue).value)
        else -> throw UnsupportedOperationException("Not implemented yet")
    }

    private fun throwTypeError(): Nothing = throw UnsupportedOperationException("Unsupported type")

    abstract fun clone(): ExecValue
}

sealed class PrimitiveValue : ExecValue()

sealed class DoubleAlias : PrimitiveValue() {
    abstract fun toDouble(): Double
}

data class DoubleValue(override val value: Double, override var manimObject: MObject = EmptyMObject) : DoubleAlias() {

    override val name: String = "Double"

    override fun equals(other: Any?): Boolean = other is DoubleValue && this.value == other.value
    override fun toDouble(): Double = value

    override fun hashCode(): Int {
        return value.hashCode()
    }

    override fun toString(): String {
        return value.toString()
    }

    override fun clone(): ExecValue {
        return DoubleValue(value, manimObject)
    }
}

data class CharValue(override val value: Char, override var manimObject: MObject = EmptyMObject) : DoubleAlias() {

    override val name: String = "Char"

    override fun equals(other: Any?): Boolean = other is CharValue && this.value == other.value
    override fun toDouble(): Double = value.toDouble()

    override fun hashCode(): Int {
        return value.hashCode()
    }

    override fun toString(): String {
        return "\'${value}\'"
    }

    override fun clone(): ExecValue {
        return CharValue(value, manimObject)
    }
}

data class BoolValue(override val value: Boolean, override var manimObject: MObject = EmptyMObject) : PrimitiveValue() {

    override val name: String = "Bool"

    override fun equals(other: Any?): Boolean = other is BoolValue && this.value == other.value
    override fun hashCode(): Int {
        var result = value.hashCode()
        result = 31 * result + manimObject.hashCode()
        return result
    }

    override fun toString(): String {
        return value.toString()
    }

    override fun clone(): ExecValue {
        return BoolValue(value, manimObject)
    }
}

data class StackValue(override var manimObject: MObject, val stack: Stack<ExecValue>, var style: StyleProperties = StyleProperties(), var animatedStyle: AnimationProperties? = null) : ExecValue() {
    override val value: Stack<ExecValue> = stack
    override val name: String = "Stack"
    override fun clone(): ExecValue {
        return StackValue(manimObject, stack, style, animatedStyle)
    }

    override fun toString(): String {
        return stack.joinToString(" -> ", "[", "]")
    }
}

data class ArrayValue(override var manimObject: MObject, val array: Array<ExecValue>, var style: StyleProperties = StyleProperties(), var animatedStyle: AnimationProperties? = null) : ExecValue() {
    override val value: Array<ExecValue> = array
    override val name: String = "Array"
    override fun clone(): ExecValue {
        return ArrayValue(manimObject, array, style, animatedStyle)
    }

    override fun toString(): String {
        return array.joinToString(", ", "[", "]")
    }
}

sealed class ITreeNodeValue : ExecValue()

object NullValue : ITreeNodeValue() {

    override val name: String = "Null"
    override var manimObject: MObject = EmptyMObject
    override val value: Int = 0

    override fun clone(): ExecValue {
        return this
    }

    override fun toString(): String {
        return "()"
    }
}

fun nodeCount(node: ITreeNodeValue): Int {
    return if (node is NullValue) {
        0
    } else {
        1 + nodeCount((node as BinaryTreeNodeValue).left) + nodeCount(node.right)
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

data class Array2DValue(override var manimObject: MObject, val array: Array<Array<ExecValue>>, var style: StyleProperties = StyleProperties(), var animatedStyle: AnimationProperties? = null) : ExecValue() {
    override val value: Array<Array<ExecValue>> = array
    override val name: String = "Array"
    override fun clone(): ExecValue {
        return Array2DValue(manimObject, array, style, animatedStyle)
    }

    override fun toString(): String {
        return array.joinToString(", ", "[", "]", transform = {
            it.joinToString(", ", "[", "]")
        })
    }
}

object EmptyValue : ExecValue() {
    override var manimObject: MObject = EmptyMObject
    override val value: Any = ErrorType

    override fun clone(): ExecValue {
        return this
    }

    override val name: String = "Empty"
}

// For use to terminate a void function with a return of no expression.
object VoidValue : ExecValue() {
    override var manimObject: MObject = EmptyMObject
    override val value: Any = ErrorType

    override fun clone(): ExecValue {
        return this
    }

    override val name: String = "Void"
}

object BreakValue : ExecValue() {
    override var manimObject: MObject = EmptyMObject
    override val value: Any = ErrorType

    override fun clone(): ExecValue {
        return this
    }

    override val name: String = "Break"
}

object ContinueValue : ExecValue() {
    override var manimObject: MObject = EmptyMObject
    override val value: Any = ErrorType

    override fun clone(): ExecValue {
        return this
    }

    override val name: String = "Continue"
}

// Used to propagate runtime error up scope
data class RuntimeError(override val value: String, override var manimObject: MObject = EmptyMObject, val lineNumber: Int) : ExecValue() {
    override fun clone(): ExecValue {
        return RuntimeError(value, manimObject, lineNumber)
    }

    override val name: String = "RuntimeError"
}
