package com.valgolang.linearrepresentation

/**
 * Python style for storing style attributes
 *
 * @constructor Create empty Python style
 */
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

/**
 * Python style attribute
 *
 * @constructor Create empty Python style attribute
 */
sealed class PythonStyleAttribute {
    abstract val name: String
    abstract val value: String

    override fun toString(): String = "$name=$value"
}

/**
 * Python color attribute
 *
 * @constructor Create empty Color attribute python
 */
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

/**
 * Color attribute
 *
 * @property value
 * @constructor Create empty Color
 */
class Color(override val value: String) : ColorAttributePython() {
    override val name: String = "color"
}

/**
 * Text color attribute
 *
 * @property value
 * @constructor Create empty Text color
 */
class TextColor(override val value: String) : ColorAttributePython() {
    override val name: String = "text_color"
}
