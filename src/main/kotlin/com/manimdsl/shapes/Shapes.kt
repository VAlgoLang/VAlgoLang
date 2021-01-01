package com.manimdsl.shapes

import com.manimdsl.linearrepresentation.Alignment
import com.manimdsl.runtime.ExecValue
import com.manimdsl.runtime.datastructures.binarytree.BinaryTreeNodeValue
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

class InitManimStackShape(
    override val ident: String,
    override val text: String,
    private val boundary: List<Pair<Double, Double>>,
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
        val coordinatesString = boundary.joinToString(", ") { "[${it.first}, ${it.second}, 0]" }
        return "$ident = $className($coordinatesString, DOWN$style)"
    }
}

class InitTreeShape(
    override val ident: String,
    override val text: String,
    private val root: BinaryTreeNodeValue,
    private val boundaries: List<Pair<Double, Double>>,
    val color: String? = null,
    val textColor: String? = null,
) : ShapeWithText() {
    override val classPath: String = "python/binary_tree.py"
    override val className: String = "Tree"
    override val pythonVariablePrefix: String = ""

    init {
        color?.let { style.addStyleAttribute(Color(it)) }
        textColor?.let { style.addStyleAttribute(TextColor(it)) }
    }

    override fun getConstructor(): String {
        val coordinatesString = boundaries.joinToString(", ") { "[${it.first}, ${it.second}, 0]" }
        return "$ident = $className($coordinatesString, ${root.manimObject.shape.ident}, $text)"
    }
}

class ArrayShape(
    override val ident: String,
    private val values: Array<ExecValue>,
    override val text: String,
    private val boundaries: List<Pair<Double, Double>>,
    color: String? = null,
    textColor: String? = null,
    private val showLabel: Boolean? = null
) : ShapeWithText() {
    override val classPath: String = "python/array.py"
    override val className: String = "Array"
    override val pythonVariablePrefix: String = ""

    init {
        color?.let { style.addStyleAttribute(Color(it)) }
        textColor?.let { style.addStyleAttribute(TextColor(it)) }
    }

    override fun getConstructor(): String {
        val arrayTitle = if (showLabel == null || showLabel) text else ""
        return "$ident = $className([${values.joinToString(",") { "\"${it.value}\"" }}], \"$arrayTitle\", [${boundaries.joinToString(
            ","
        )}]$style).build()"
    }
}

class Array2DShape(
    override val ident: String,
    private val values: Array<Array<ExecValue>>,
    override val text: String,
    private val boundaries: List<Pair<Double, Double>>,
    color: String? = null,
    textColor: String? = null,
    private val showLabel: Boolean? = null
) : ShapeWithText() {
    override val classPath: String = "python/array.py"
    override val className: String = "Array2D"
    override val pythonVariablePrefix: String = ""

    init {
        color?.let { style.addStyleAttribute(Color(it)) }
        textColor?.let { style.addStyleAttribute(TextColor(it)) }
    }

    override fun getConstructor(): String {
        val arrayTitle = if (showLabel == null || showLabel) text else ""
        return "$ident = $className([${values.map { array -> "[ ${array.map { "\"${it.value}\"" }.joinToString(",")}]" }.joinToString(",")}], \"$arrayTitle\", [${boundaries.joinToString(",")}]$style)"
    }
}

object NullShape : Shape() {
    override val ident: String = ""
    override val text: String = ""
    override val classPath: String = ""
    override val className: String = ""
    override val pythonVariablePrefix: String = ""
}
