package com.valgolang.runtime.datastructures.stack

import com.valgolang.linearrepresentation.MObject
import com.valgolang.runtime.ExecValue
import com.valgolang.stylesheet.AnimationProperties
import com.valgolang.stylesheet.StyleProperties
import java.util.*

/**
 * Stack Execution Value
 *
 * @property manimObject: Manim Object corresponded to by the StackValye.
 * @property stack: Kotlin stack of execution values.
 * @property style: Static style properties to apply.
 * @property animatedStyle: Dynamic style properties to apply.
 * @constructor: Creates a new Stack Execution Value.
 *
 */

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
