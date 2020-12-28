package com.manimdsl.runtime.datastructures.array

import com.manimdsl.linearrepresentation.MObject
import com.manimdsl.runtime.ExecValue
import com.manimdsl.stylesheet.AnimationProperties
import com.manimdsl.stylesheet.StyleProperties

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

data class Array2DValue(override var manimObject: MObject, val array: Array<Array<ExecValue>>, var style: StyleProperties = StyleProperties(), var animatedStyle: AnimationProperties? = null) : ExecValue() {
    override val value: Array<Array<ExecValue>> = array
    override val name: String = "Array"
    override fun clone(): ExecValue {
        return Array2DValue(manimObject, array, style, animatedStyle)
    }

    override fun toString(): String {
        return array.joinToString(
            ", ", "[", "]",
            transform = {
                it.joinToString(", ", "[", "]")
            }
        )
    }
}
