package com.manimdsl.shapes

sealed class Shape {
    abstract val text: String
    abstract val classPath: String
    abstract val className: String
    abstract val pythonVariablePrefix: String
    val style = Style()

    open fun getConstructor(): String {
        return "${className}(\"${text}\"$style).build()"
    }
}

data class Rectangle(
    override val text: String,
    val color: String? = null,
    val textColor: String? = null,
    val textWeight: String? = null,
    val font: String? = null
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

data class CodeBlockShape(
    val lines: List<String>,
    val textColor: String? = null,
    val textWeight: String? = null,
    val font: String? = null
) : Shape() {
    override val classPath: String = "python/code_block.py"
    override val className: String = "Code_block"
    override val pythonVariablePrefix: String = "code_block"
    override val text: String = "[\"${lines.joinToString("\",\"")}\"]"

    override fun getConstructor(): String {
        return "${className}($text$style)"
    }

    init {
        textColor?.let { style.addStyleAttribute(TextColor(textColor)) }
        textWeight?.let { style.addStyleAttribute(TextWeight(textWeight)) }
        font?.let { style.addStyleAttribute(Font(font)) }
    }
}
