package com.manimdsl.linearrepresentation.datastructures.list

import com.manimdsl.linearrepresentation.*
import com.manimdsl.runtime.ExecValue

data class ListPrepend(
    val arrayIdent: String,
    val newArrayIdent: String,
    val text: String,
    val values: Array<ExecValue>,
    val color: String?,
    val textColor: String?,
    var creationString: String?,
    val showLabel: Boolean? = null,
    var boundaries: List<Pair<Double, Double>> = emptyList(),

    override val runtime: Double,
    override val render: Boolean,
    override val uid: String
) :
    ManimInstr(), ManimInstrWithBoundary {
    val style = PythonStyle()

    init {
        if (creationString == null) creationString = "FadeIn"
        color?.let { style.addStyleAttribute(Color(it)) }
        textColor?.let { style.addStyleAttribute(TextColor(it)) }
    }


    override fun toPython(): List<String> {
        val arrayTitle = if (showLabel == null || showLabel) text else ""
        return listOf(
            "$newArrayIdent = Array([${values.joinToString(", ") { "\"${it.value}\"" }}], \"$arrayTitle\", [${
            boundaries.joinToString(", ")}]$style).build()",
            "self.play(ReplacementTransform($arrayIdent.all, $newArrayIdent.all))",
            "$arrayIdent = $newArrayIdent"
        )
    }

    override fun setNewBoundary(corners: List<Pair<Double, Double>>, newMaxSize: Int) {
        boundaries = corners
    }
}

data class ListAppend(
    val arrayIdent: String,
    val newElemValue: ExecValue,
    override val runtime: Double,
    override val render: Boolean
) :
    ManimInstr() {
    override fun toPython(): List<String> {
        return listOf("self.play_animation(*$arrayIdent.append(\"$newElemValue\")${getRuntimeString()})")
    }
}
