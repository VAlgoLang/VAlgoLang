package com.manimdsl.shapes

import com.manimdsl.linearrepresentation.Alignment

sealed class Shape {
    abstract val ident: String
    abstract val text: String
    abstract val classPath: String
    abstract val className: String
    abstract val pythonVariablePrefix: String
    val style = Style()

    open fun getConstructor(): String {
        return "$ident = ${className}(\"${text}\"$style)"
    }

    override fun toString(): String {
        return "$ident.all"
    }
}

sealed class ShapeWithText : Shape() {
    // Python shape classes which has shape and text members to manipulate color

    fun getTextMObject(): String {
        return "$ident.text"
    }

    fun getShapeMObject(): String {
        return "$ident.shape"
    }
}

class Rectangle(
    override val ident: String,
    override val text: String,
    color: String? = null,
    textColor: String? = null,
    textWeight: String? = null,
    font: String? = null,
) : ShapeWithText() {
    override val classPath: String = "python/rectangle.py"
    override val className: String = "Rectangle_block"
    override val pythonVariablePrefix: String = "rectangle"

    init {
        color?.let { style.addStyleAttribute(Color(it)) }
        textColor?.let { style.addStyleAttribute(TextColor(it)) }
        textWeight?.let { style.addStyleAttribute(TextWeight(it)) }
        font?.let { style.addStyleAttribute(Font(it)) }
    }
}

class CodeBlockShape(
    override val ident: String,
    lines: List<String>,
    textColor: String? = null,
    textWeight: String? = null,
    font: String? = null
) : Shape() {
    override val classPath: String = "python/code_block.py"
    override val className: String = "Code_block"
    override val pythonVariablePrefix: String = "code_block"
    override val text: String = "[\"${lines.joinToString("\",\"")}\"]"

    init {
        textColor?.let { style.addStyleAttribute(TextColor(it)) }
        textWeight?.let { style.addStyleAttribute(TextWeight(it)) }
        font?.let { style.addStyleAttribute(Font(it)) }
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
    textWeight: String? = null,
    font: String? = null
) : ShapeWithText() {
    override val classPath: String = "python/init_structure.py"
    override val className: String = "Init_structure"
    override val pythonVariablePrefix: String = ""

    init {
        color?.let { style.addStyleAttribute(Color(it)) }
        textColor?.let { style.addStyleAttribute(TextColor(it)) }
        textWeight?.let { style.addStyleAttribute(TextWeight(it)) }
        font?.let { style.addStyleAttribute(Font(it)) }
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
