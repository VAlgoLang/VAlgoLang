package com.manimdsl.shapes

sealed class Shape {
    abstract val text: String
    abstract val classPath: String
    abstract val className: String
    abstract val pythonVariablePrefix: String

    abstract fun getStyle(): Set<StyleAttribute>

    fun getConstructor(): String {
        val style = getStyle()
        return "${className}(\"${text}\"" +
                "${if (style.isNotEmpty()) ", ${style.joinToString(", ")}" else ""}).build()"
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

    override fun getStyle(): Set<StyleAttribute> {
        val style = mutableSetOf<StyleAttribute>()
        color?.let { style.add(Color(color)) }
        textColor?.let { style.add(TextColor(textColor)) }
        textWeight?.let { style.add(TextWeight(textWeight)) }
        font?.let { style.add(Font(font)) }
        return style
    }
}

