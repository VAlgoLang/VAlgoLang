package com.manimdsl.shapes

import com.manimdsl.linearrepresentation.Alignment
import com.manimdsl.stylesheet.StylesheetProperty

sealed class Shape {
    abstract val ident: String
    abstract val text: String
    abstract val classPath: String
    abstract val className: String
    abstract val pythonVariablePrefix: String
    val style = PythonStyle()

    open fun getConstructor(): String {
        return "$ident = ${className}(\"${text}\"$style)"
    }

    override fun toString(): String {
        return "$ident.all"
    }
}

sealed class ShapeWithText : Shape()

interface StyleableShape {
    fun restyle(styleProperties: StylesheetProperty): List<String>
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

    override fun restyle(styleProperties: StylesheetProperty): List<String> {
        val instructions = mutableListOf<String>()

        styleProperties.borderColor?.let { instructions.add("FadeToColor($ident.shape, ${it})") }
        styleProperties.textColor?.let { instructions.add("FadeToColor($ident.text, ${it})") }

        return if (instructions.isEmpty()) {
            emptyList()
        } else {
            listOf("self.play(${instructions.joinToString(", ")})")
        }
    }

    override fun getConstructor(): String {
        return "$ident = ${className}(\"${text}\", ${dataStructureIdentifier}.empty$style)"
    }



}

class CodeBlockShape(
    override val ident: String,
    lines: List<String>,
    textColor: String? = null,
) : Shape() {
    override val classPath: String = "python/code_block.py"
    override val className: String = "Code_block"
    override val pythonVariablePrefix: String = "code_block"
    override val text: String = "[\"${lines.joinToString("\",\"")}\"]"

    init {
        textColor?.let { style.addStyleAttribute(TextColor(it)) }
    }

    override fun getConstructor(): String {
        return "$ident = ${className}($text$style)"
    }
}

class InitManimStackShape(
    override val ident: String,
    override val text: String,
    private val coordinates: List<Pair<Int, Int>>,
    private val alignment: Alignment,
    val color: String? = null,
    val textColor: String? = null,
) : ShapeWithText() {
    override val classPath: String = "python/stack.py"
    override val className: String = "Stack"
    override val pythonVariablePrefix: String = ""

    init {
        color?.let { style.addStyleAttribute(Color(it)) }
        textColor?.let { style.addStyleAttribute(TextColor(it)) }
    }

    override fun getConstructor(): String {
        val coordinatesString = coordinates.joinToString(", ") { "[${it.first}, ${it.second}, 0]" }
        return "$ident = ${className}(${coordinatesString}, DOWN${style})"
    }
}

object NullShape : Shape() {
    override val ident: String = ""
    override val text: String = ""
    override val classPath: String = ""
    override val className: String = ""
    override val pythonVariablePrefix: String = ""

}
