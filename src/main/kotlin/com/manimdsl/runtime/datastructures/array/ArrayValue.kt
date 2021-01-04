package com.manimdsl.runtime.datastructures.array

import com.manimdsl.linearrepresentation.MObject
import com.manimdsl.runtime.ExecValue
import com.manimdsl.stylesheet.AnimationProperties
import com.manimdsl.stylesheet.StyleProperties

/**
 * 1D Array Execution Value Class
 *
 * @property manimObject: Manim Object corresponded to by the BoolValue.
 * @property value: Current execution state represented by array of execution values.
 * @property style: Static styling to apply to the array on rendering.
 * @property animatedStyle: Dynamic styling to apply to the array on rendering.
 * @constructor: Creates a new Array Execution Value.
 *
 */

data class ArrayValue(override var manimObject: MObject, var array: Array<ExecValue>, var style: StyleProperties = StyleProperties(), var animatedStyle: AnimationProperties? = null) : ExecValue() {
    override val value: Array<ExecValue> = array
    override val name: String = "Array"
    override fun clone(): ExecValue {
        return ArrayValue(manimObject, array, style, animatedStyle)
    }

    override fun toString(): String {
        return array.joinToString(", ", "[", "]")
    }
}

/**
 * 2D Array Execution Value Class
 *
 * @property manimObject: Manim Object corresponded to by the BoolValue.
 * @property value: Current execution state represented by 2D array of execution values.
 * @property style: Static styling to apply to the array on rendering.
 * @property animatedStyle: Dynamic styling to apply to the array on rendering.
 * @constructor: Creates a new Array2DValue Execution Value.
 *
 */

data class Array2DValue(override var manimObject: MObject, var array: Array<Array<ExecValue>>, var style: StyleProperties = StyleProperties(), var animatedStyle: AnimationProperties? = null) : ExecValue() {
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
