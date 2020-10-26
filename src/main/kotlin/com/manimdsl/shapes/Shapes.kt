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

        return listOf("self.play(${instructions.joinToString(", ")})")
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

class VariableBlockShape(
    override val ident: String,
    variables: List<String>,
    variable_frame : String,
    textColor: String? = null,
) : Shape() {
    override val classPath: String = "python/variable_block.py"
    override val className: String = "Variable_block"
    override val pythonVariablePrefix: String = "variable_block"
    override val text: String = "[\"${variables.joinToString("\",\"")}\"], variable_frame"

    init {
        textColor?.let { style.addStyleAttribute(TextColor(it)) }
    }

    override fun getConstructor(): String {
        return "$ident = ${className}($text$style)"
    }
}

class InitStructureShape(
    override val ident: String,
    override val text: String,
    private val alignment: Alignment,
    color: String? = null,
    textColor: String? = null,
) : ShapeWithText() {
    override val classPath: String = "python/init_structure.py"
    override val className: String = "Init_structure"
    override val pythonVariablePrefix: String = ""

    init {
        color?.let { style.addStyleAttribute(Color(it)) }
        textColor?.let { style.addStyleAttribute(TextColor(it)) }
    }

    override fun getConstructor(): String {
        return "$ident = ${className}(\"$text\", ${alignment.angle}$style)"
    }
}

object NullShape : Shape() {
    override val ident: String = ""
    override val text: String = ""
    override val classPath: String = ""
    override val className: String = ""
    override val pythonVariablePrefix: String = ""

}
