package com.manimdsl.runtime

import com.manimdsl.frontend.ErrorType
import com.manimdsl.linearrepresentation.EmptyMObject
import com.manimdsl.linearrepresentation.MObject
import kotlin.math.roundToInt

// Wrapper classes for values of variables while executing code
abstract class ExecValue {
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
