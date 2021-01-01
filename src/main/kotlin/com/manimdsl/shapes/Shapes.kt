package com.manimdsl.shapes

import com.manimdsl.stylesheet.StylesheetProperty

sealed class Shape {
    abstract val ident: String
    abstract val text: String
    abstract val classPath: String
    abstract val className: String
    abstract val pythonVariablePrefix: String
    val style = PythonStyle()

    open fun getConstructor(): String {
        return "$ident = $className(\"${text}\"$style)"
    }

    override fun toString(): String {
        return "$ident.all"
    }
}

sealed class ShapeWithText : Shape()

interface StyleableShape {
    fun restyle(styleProperties: StylesheetProperty, runtimeString: String): List<String>
}

class Rectangle(
    override val ident: String,
    override val text: String,
    private val dataStructureIdentifier: String,
    color: String? = null,
    textColor: String? = null,
) : ShapeWithText(), StyleableShape {
    override val classPath: String = "python/rectangle.py"
    override val className: String = "Rectangle_block"
    override val pythonVariablePrefix: String = "rectangle"

    init {
        color?.let { style.addStyleAttribute(Color(it)) }
        textColor?.let { style.addStyleAttribute(TextColor(it)) }
    }

    override fun restyle(styleProperties: StylesheetProperty, runtimeString: String): List<String> {
        val instructions = mutableListOf<String>()

        styleProperties.borderColor?.let {
            instructions.add(
                "FadeToColor($ident.shape, ${styleProperties.handleColourValue(
                    it
                )})"
            )
        }
        styleProperties.textColor?.let {
            instructions.add(
                "FadeToColor($ident.text, ${styleProperties.handleColourValue(
                    it
                )})"
            )
        }

        return if (instructions.isEmpty()) {
            emptyList()
        } else {
            listOf("self.play_animation(${instructions.joinToString(", ")}$runtimeString)")
        }
    }

    override fun getConstructor(): String {
        return "$ident = $className(\"${text}\", ${dataStructureIdentifier}$style)"
    }
}
