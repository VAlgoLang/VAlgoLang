package com.manimdsl.linearrepresentation.datastructures.array

import com.manimdsl.frontend.DataStructureType
import com.manimdsl.linearrepresentation.Color
import com.manimdsl.linearrepresentation.DataStructureMObject
import com.manimdsl.linearrepresentation.TextColor
import com.manimdsl.runtime.ExecValue

data class ArrayStructure(
    override val type: DataStructureType,
    override val ident: String,
    override val render: Boolean,
    override var text: String,
    val values: Array<ExecValue>,
    val color: String? = null,
    val textColor: String? = null,
    var creationString: String? = null,
    override val runtime: Double = 1.0,
    val showLabel: Boolean? = null,
    var maxSize: Int = -1,
    private var boundaries: List<Pair<Double, Double>> = emptyList(),
    override val uid: String
) : DataStructureMObject(type, ident, uid, text, boundaries) {
    override val classPath: String = "python/array.py"
    override val className: String = "Array"
    override val pythonVariablePrefix: String = ""

    init {
        if (creationString == null) creationString = "FadeIn"
        color?.let { style.addStyleAttribute(Color(it)) }
        textColor?.let { style.addStyleAttribute(TextColor(it)) }
    }

    override fun getConstructor(): String {
        val arrayTitle = if (showLabel == null || showLabel) text else ""
        return "$ident = $className([${values.joinToString(",") { "\"${it}\"" }}], \"$arrayTitle\", [${
        boundaries.joinToString(
            ","
        )
        }]$style).build()"
    }

    override fun toPython(): List<String> {
        return listOf(
            "# Constructing new $type \"$text\"",
            getConstructor(),
            if (render && (showLabel == null || showLabel)) "self.play($creationString($ident.title)${getRuntimeString()})" else "",
            if (values.isNotEmpty()) getInstructionString(
                "[$creationString(array_elem.all${getRuntimeString()}) for array_elem in $ident.array_elements]",
                true
            ) else ""
        )
    }

    override fun setNewBoundary(corners: List<Pair<Double, Double>>, newMaxSize: Int) {
        maxSize = newMaxSize
        boundaries = corners
    }
}

data class Array2DStructure(
    override val type: DataStructureType,
    override val ident: String,
    override val render: Boolean,
    override var text: String,
    val values: Array<Array<ExecValue>>,
    val color: String? = null,
    val textColor: String? = null,
    var creationString: String? = null,
    override val runtime: Double = 1.0,
    val showLabel: Boolean? = null,
    var maxSize: Int = -1,
    private var boundaries: List<Pair<Double, Double>> = emptyList(),
    override val uid: String
) : DataStructureMObject(type, ident, uid, text, boundaries) {
    override val classPath: String = "python/array.py"
    override val className: String = "Array2D"
    override val pythonVariablePrefix: String = ""

    init {
        if (creationString == null) creationString = "FadeIn"
        color?.let { style.addStyleAttribute(Color(it)) }
        textColor?.let { style.addStyleAttribute(TextColor(it)) }
    }

    override fun toPython(): List<String> {
        return listOf(
            "# Constructing new $type \"$text\"",
            getConstructor(),
            if (render && (showLabel == null || showLabel)) "self.play($creationString($ident.title))" else "",
            getInstructionString("$ident.build(\"$creationString\")", true)
        )
    }

    override fun setNewBoundary(corners: List<Pair<Double, Double>>, newMaxSize: Int) {
        maxSize = newMaxSize
        boundaries = corners
    }

    override fun getConstructor(): String {
        val arrayTitle = if (showLabel == null || showLabel) text else ""
        return "$ident = $className([${
        values.map { array -> "[ ${array.map { "\"${it}\"" }.joinToString(",")}]" }.joinToString(",")
        }], \"$arrayTitle\", [${boundaries.joinToString(",")}]$style)"
    }
}
