package com.manimdsl.shapes

class PythonStyle {
    private val pythonStyleAttributes: MutableSet<PythonStyleAttribute> = mutableSetOf()

    fun addStyleAttribute(pythonStyleAttribute: PythonStyleAttribute) {
        pythonStyleAttributes.add(pythonStyleAttribute)
    }

    override fun toString(): String {
        return if (pythonStyleAttributes.isNotEmpty()) {
            ", ${pythonStyleAttributes.joinToString(", ")}"
        } else ""
    }
}

sealed class PythonStyleAttribute {
    abstract val name: String
    abstract val value: String

    override fun toString(): String = "$name=$value"
}

sealed class ColorAttributePython : PythonStyleAttribute() {
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

class Color(override val value: String) : ColorAttributePython() {
    override val name: String = "color"
}

class TextColor(override val value: String) : ColorAttributePython() {
    override val name: String = "text_color"
}
