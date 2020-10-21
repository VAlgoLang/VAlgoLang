package com.manimdsl.executor

import com.manimdsl.frontend.ErrorType
import com.manimdsl.linearrepresentation.EmptyMObject
import com.manimdsl.linearrepresentation.MObject
import com.sun.jdi.BooleanValue
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
        is BooleanValue -> !this.value()
        else -> throw UnsupportedOperationException("Wrong type")
    }

    /** '==','!=', '<', '<=', '>', '>='  **/
    operator fun compareTo(other: Any): Int = when (this) {
        is DoubleValue -> if (other is DoubleValue) this.value.compareTo(other.value) else throwTypeError()
        is BoolValue -> if (other is BoolValue) this.value.compareTo(other.value) else throwTypeError()
        else -> throw throwTypeError()
    }

    private fun throwTypeError(): Nothing = throw UnsupportedOperationException("Unsupported type")
}


data class DoubleValue(override val value: Double, override var manimObject: MObject = EmptyMObject) : ExecValue() {
    override fun equals(other: Any?): Boolean = other is DoubleValue && this.value == other.value
    override fun hashCode(): Int {
        var result = value.hashCode()
        result = 31 * result + manimObject.hashCode()
        return result
    }
}

data class BoolValue(override val value: Boolean, override var manimObject: MObject = EmptyMObject) : ExecValue() {
    override fun equals(other: Any?): Boolean = other is BoolValue && this.value == other.value
    override fun hashCode(): Int {
        var result = value.hashCode()
        result = 31 * result + manimObject.hashCode()
        return result
    }
}

data class StackValue(override var manimObject: MObject, val stack: Stack<ExecValue>) : ExecValue() {
    override val value: Stack<ExecValue> = stack
}

object EmptyValue : ExecValue() {
    override var manimObject: MObject = EmptyMObject
    override val value: Any = ErrorType
}


