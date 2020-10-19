package com.manimdsl.shapes

import com.manimdsl.linearrepresentation.Alignment

sealed class Shape {
    abstract val text: String
    abstract val classPath: String
    abstract val className: String
    abstract val pythonVariablePrefix: String
    val style = Style()

    override fun toString(): String {
        return "${className}(\"${text}\"$style).build()"
    }
}

class Rectangle(
    override val text: String,
    private val color: String? = null,
    private val textColor: String? = null,
    private val textWeight: String? = null,
    private val font: String? = null
) : Shape() {
    override val classPath: String = "python/rectangle.py"
    override val className: String = "Rectangle_block"
    override val pythonVariablePrefix: String = "rectangle"

    init {
        color?.let { style.addStyleAttribute(Color(color)) }
        textColor?.let { style.addStyleAttribute(TextColor(textColor)) }
        textWeight?.let { style.addStyleAttribute(TextWeight(textWeight)) }
        font?.let { style.addStyleAttribute(Font(font)) }
    }
}

class CodeBlockShape(
    lines: List<String>,
    private val textColor: String? = null,
    private val textWeight: String? = null,
    private val font: String? = null
) : Shape() {
    override val classPath: String = "python/code_block.py"
    override val className: String = "Code_block"
    override val pythonVariablePrefix: String = "code_block"
    override val text: String = "[\"${lines.joinToString("\",\"")}\"]"

    init {
        textColor?.let { style.addStyleAttribute(TextColor(textColor)) }
        textWeight?.let { style.addStyleAttribute(TextWeight(textWeight)) }
        font?.let { style.addStyleAttribute(Font(font)) }
    }

    override fun toString(): String {
        return "${className}($text$style)"
    }
}

class InitStructureShape(
    override val text: String,
    private val alignment: Alignment,
    private val color: String? = null,
    private val textColor: String? = null,
    private val textWeight: String? = null,
    private val font: String? = null
) : Shape() {
    override val classPath: String = "python/init_structure.py"
    override val className: String = "Init_structure"
    override val pythonVariablePrefix: String = ""

    init {
        color?.let { style.addStyleAttribute(Color(color)) }
        textColor?.let { style.addStyleAttribute(TextColor(textColor)) }
        textWeight?.let { style.addStyleAttribute(TextWeight(textWeight)) }
        font?.let { style.addStyleAttribute(Font(font)) }
    }

    override fun toString(): String {
        return "${className}(\"$text\", ${alignment.angle}$style).build()"
    }
}
