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

data class Color(override val value: String) : ColorAttribute() {
    override val name: String = "color"
}

data class TextColor(override val value: String) : ColorAttribute() {
    override val name: String = "text_color"
}

data class TextWeight(override val value: String) : StyleAttribute() {
    override val name: String = "text_weight"
}

data class Font(override val value: String) : StyleAttribute() {
    override val name: String = "font"

    override fun toString(): String = "$name=\"$value\""
}
