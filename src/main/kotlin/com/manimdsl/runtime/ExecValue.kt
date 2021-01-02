package com.manimdsl.runtime

import com.manimdsl.frontend.ErrorType
import com.manimdsl.linearrepresentation.EmptyMObject
import com.manimdsl.linearrepresentation.MObject
import kotlin.math.roundToInt

/**
 * Abstract Execution Value Class
 *
 * @property manimObject: Manim Object corresponded to by the Execution Value.
 * @property value: Current value in Kotlin.
 * @property name: String name of value.
 *
 */

abstract class ExecValue {
    abstract var manimObject: MObject
    abstract val value: Any
    abstract val name: String

    /**
     * @return [value] to string formatted for use in interpolation
     *
     */
    open fun toInterpolatedString(): String = this.toString()

    /**
     * '+'
     *
     * @param other
     * @return [value] plus [other]
     * @exception UnsupportedOperationException
     *
     */

    operator fun plus(other: ExecValue): ExecValue = when (this) {
        is DoubleAlias -> {
            if (other is StringValue) {
                StringValue(other.toInterpolatedString() + this.toInterpolatedString())
            } else {
                DoubleValue((other as DoubleAlias).toDouble() + this.toDouble())
            }
        }
        is StringValue -> StringValue(this.toInterpolatedString() + other.toInterpolatedString())
        else -> throwTypeError()
    }

    /**
     * '-'
     *
     * @param other
     * @return [value] minus [other]
     * @exception UnsupportedOperationException
     *
     */

    operator fun minus(other: ExecValue): ExecValue = when (this) {
        is DoubleAlias -> DoubleValue(this.toDouble() - (other as DoubleAlias).toDouble())
        else -> throwTypeError()
    }

    /**
     * '/'
     *
     * @param other
     * @return [value] divided by [other]
     * @exception UnsupportedOperationException
     *
     */

    operator fun div(other: ExecValue): ExecValue = when (this) {
        is DoubleValue -> if (other is DoubleValue) DoubleValue(
            (this.value / other.value).roundToInt().toDouble()
        ) else throwTypeError()
        else -> throwTypeError()
    }

    /**
     * '*'
     *
     * @param other
     * @return [value] times [other]
     * @exception UnsupportedOperationException
     *
     */

    operator fun times(other: ExecValue): ExecValue = when (this) {
        is DoubleAlias -> DoubleValue((other as DoubleAlias).toDouble() * this.toDouble())
        else -> throwTypeError()
    }

    /**
     * '!'
     *
     * @return not [value]
     * @exception UnsupportedOperationException
     *
     */

    operator fun not(): Boolean = when (this) {
        is BoolValue -> !this.value
        else -> throwTypeError()
    }

    /** '==','!=', '<', '<=', '>', '>='  **/
    operator fun compareTo(other: Any): Int = when (this) {
        is DoubleAlias -> this.toDouble().compareTo((other as DoubleAlias).toDouble())
        is BoolValue -> this.value.compareTo((other as BoolValue).value)
        is StringValue -> this.value.compareTo((other as StringValue).value)
        else -> throwTypeError()
    }

    private fun throwTypeError(): Nothing = throw UnsupportedOperationException("Unsupported type")

    abstract fun clone(): ExecValue
}

/**
 * Abstract primitive execution value (non data structures)
 *
 */

sealed class PrimitiveValue : ExecValue()

sealed class DoubleAlias : PrimitiveValue() {
    abstract fun toDouble(): Double
}

/**
 * Double Execution Value Class
 *
 * @property manimObject: Manim Object corresponded to by the DoubleValue.
 * @property value: Current double value.
 * @constructor: Creates a new DoubleValue.
 *
 */

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

/**
 * Char Execution Value Class
 *
 * @property manimObject: Manim Object corresponded to by the CharValue.
 * @property value: Current char value.
 * @constructor: Creates a new CharValue.
 *
 */

data class CharValue(override val value: Char, override var manimObject: MObject = EmptyMObject) : DoubleAlias() {

    override val name: String = "Char"

    override fun toInterpolatedString(): String = value.toString()

    override fun equals(other: Any?): Boolean = other is CharValue && this.value == other.value
    override fun toDouble(): Double = value.toDouble()

    override fun hashCode(): Int {
        return value.hashCode()
    }

    override fun toString(): String {
        return "\\'${value}\\'"
    }

    override fun clone(): ExecValue {
        return CharValue(value, manimObject)
    }
}

/**
 * Bool Execution Value Class
 *
 * @property manimObject: Manim Object corresponded to by the BoolValue.
 * @property value: Current boolean value.
 * @constructor: Creates a new BoolValue.
 *
 */

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

/**
 * String Execution Value Class
 *
 * @property manimObject: Manim Object corresponded to by the BoolValue.
 * @property value: Current string value.
 * @constructor: Creates a new StringValue.
 *
 */

data class StringValue(override val value: String, override var manimObject: MObject = EmptyMObject) : PrimitiveValue() {

    override val name: String = "Bool"

    /**
     * @return [value] to string formatted for use in interpolation
     *
     */
    override fun toInterpolatedString(): String = value

    override fun equals(other: Any?): Boolean = other is StringValue && this.value == other.value
    override fun hashCode(): Int {
        var result = value.hashCode()
        result = 31 * result + manimObject.hashCode()
        return result
    }

    override fun toString(): String {
        return "\"$value\""
    }

    override fun clone(): ExecValue {
        return StringValue(value, manimObject)
    }
}

/**
 * Empty Execution Value Class
 *
 * @constructor: Creates a new EmptyValue.
 *
 */

object EmptyValue : ExecValue() {
    override var manimObject: MObject = EmptyMObject
    override val value: Any = ErrorType

    override fun clone(): ExecValue {
        return this
    }

    override val name: String = "Empty"
}

/**
 * Void Execution Value Class. Terminates void function.
 *
 * @constructor: Creates a new VoidValue.
 *
 */

object VoidValue : ExecValue() {
    override var manimObject: MObject = EmptyMObject
    override val value: Any = ErrorType

    override fun clone(): ExecValue {
        return this
    }

    override val name: String = "Void"
}

/**
 * Runtime Error Execution Value Class
 *
 * @property manimObject: Manim Object corresponded to by the BoolValue.
 * @property value: Description of error.
 * @property lineNumber: Line number at error.
 * @constructor: Creates a new Runtime Error for safe handling. Error communicated to user.
 *
 */

data class RuntimeError(
    override val value: String,
    override var manimObject: MObject = EmptyMObject,
    val lineNumber: Int
) : ExecValue() {
    override fun clone(): ExecValue {
        return RuntimeError(value, manimObject, lineNumber)
    }

    override val name: String = "RuntimeError"
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
