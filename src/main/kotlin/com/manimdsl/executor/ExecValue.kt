package com.manimdsl.executor

import com.manimdsl.frontend.ErrorType
import com.manimdsl.linearrepresentation.EmptyMObject
import com.manimdsl.linearrepresentation.MObject
import java.util.*

// Wrapper classes for values of variables while executing code
sealed class ExecValue {
    abstract var manimObject: MObject
    abstract val value: Any

    /** Extend when more types are added these are assuming semantic checks have passed **/

    /** '+' **/
    operator fun plus(other: ExecValue): ExecValue = when (this) {
        is DoubleValue -> if (other is DoubleValue) DoubleValue(this.value + other.value) else throwTypeError()
        else -> throwTypeError()
    }

    /** '-' **/
    operator fun minus(other: ExecValue): ExecValue = when (this) {
        is DoubleValue -> if (other is DoubleValue) DoubleValue(this.value - other.value) else throwTypeError()
        else -> throwTypeError()
    }

    /** '*' **/
    operator fun times(other: ExecValue): ExecValue = when (this) {
        is DoubleValue -> if (other is DoubleValue) DoubleValue(this.value * other.value) else throwTypeError()
        else -> throwTypeError()
    }

    /** '!' **/
    operator fun not(): Boolean = when (this) {
        is BoolValue -> !this.value
        else -> throw UnsupportedOperationException("Wrong type")
    }

    /** '==','!=', '<', '<=', '>', '>='  **/
    operator fun compareTo(other: Any): Int = when (this) {
        is DoubleValue -> if (other is DoubleValue) this.value.compareTo(other.value) else throwTypeError()
        is BoolValue -> if (other is BoolValue) this.value.compareTo(other.value) else throwTypeError()
        else -> throw throwTypeError()
    }

    private fun throwTypeError(): Nothing = throw UnsupportedOperationException("Unsupported type")

    abstract fun clone(): ExecValue
}


data class DoubleValue(override val value: Double, override var manimObject: MObject = EmptyMObject) : ExecValue() {
    override fun equals(other: Any?): Boolean = other is DoubleValue && this.value == other.value

    override fun hashCode(): Int {
        return value.hashCode()
    }

    override fun clone(): ExecValue {
        return DoubleValue(value, manimObject)
    }
}

data class BoolValue(override val value: Boolean, override var manimObject: MObject = EmptyMObject) : ExecValue() {
    override fun equals(other: Any?): Boolean = other is BoolValue && this.value == other.value
    override fun hashCode(): Int {
        var result = value.hashCode()
        result = 31 * result + manimObject.hashCode()
        return result
    }

    override fun clone(): ExecValue {
        return BoolValue(value, manimObject)
    }
}

data class StackValue(override var manimObject: MObject, val stack: Stack<ExecValue>) : ExecValue() {
    override val value: Stack<ExecValue> = stack

    override fun clone(): ExecValue {
        return StackValue(manimObject, stack)
    }

    override fun toString(): String {
        return "Stack"
    }
}

object EmptyValue : ExecValue() {
    override var manimObject: MObject = EmptyMObject
    override val value: Any = ErrorType

    override fun clone(): ExecValue {
        return this
    }
}

// For use to terminate a void function with a return of no expression.
object VoidValue : ExecValue() {
    override var manimObject: MObject = EmptyMObject
    override val value: Any = ErrorType

    override fun clone(): ExecValue {
        return this
    }
}

// Used to propagate runtime error up scope
data class RuntimeError(override val value: String, override var manimObject: MObject = EmptyMObject, val lineNumber: Int) : ExecValue() {
    override fun clone(): ExecValue {
        return RuntimeError(value, manimObject, lineNumber)
    }
}