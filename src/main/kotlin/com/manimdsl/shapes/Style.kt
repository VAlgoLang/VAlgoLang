package com.manimdsl.shapes

sealed class StyleAttribute {
    abstract val name: String
    abstract val value: String

    override fun toString(): String = "$name=$value"
}

sealed class ColorAttribute : StyleAttribute() {
    private fun handleColourValue(): String {
        return if (value.matches(Regex("#[a-fA-F0-9]{6}"))) {
            // hex value
            "\"${value}\""
        } else {
            // predefined constant
            value.toUpperCase()
        }
    }

    override fun toString(): String {
        return "$name=${handleColourValue()}"
    }
}

class Color(override val value: String) : ColorAttribute() {
    override val name: String = "color"
}

class TextColor(override val value: String) : ColorAttribute() {
    override val name: String = "text_color"
}

class TextWeight(override val value: String) : StyleAttribute() {
    override val name: String = "text_weight"
    private val default = "NORMAL"
    private val validWeights = setOf(default, "BOLD")

    override fun toString(): String {
        val upperCaseValue = value.toUpperCase()
        return if (validWeights.contains(upperCaseValue)) {
            "$name=$upperCaseValue"
        } else {
            "$name=$default"
        }
    }
}

class Font(override val value: String) : StyleAttribute() {
    override val name: String = "font"

    override fun toString(): String = "$name=\"$value\""
}
