package com.manimdsl.linearrepresentation.datastructures.list

import com.manimdsl.linearrepresentation.ManimInstr
import com.manimdsl.linearrepresentation.ManimInstrWithBoundary
import com.manimdsl.runtime.ExecValue

data class ListPrepend(
    val arrayIdent: String,
    val newArrayIdent: String,
    val text: String,
    val values: Array<ExecValue>,
    val showLabel: Boolean? = null,
    var boundaries: List<Pair<Double, Double>> = emptyList(),
    override val runtime: Double,
    override val render: Boolean,
    override val uid: String
) :
    ManimInstr(), ManimInstrWithBoundary {

    override fun toPython(): List<String> {
        val arrayTitle = if (showLabel == null || showLabel) text else ""
        /**
         * array1 = Array([ "6.0", "1.0", "2.0", "3.0", "4.0"], "x", [(-2.0, 0.0), (7.0, 0.0), (-2.0, -2.0), (7.0, -2.0)] color=BLUE, text_color=YELLOW).build()
         * self.play(ReplacementTransform(array.all, array1.all))
         */
        return listOf(
            "$newArrayIdent = Array([${values.joinToString(", ") { "\"${it.value}\"" }}], \"$arrayTitle\", [${
            boundaries.joinToString(
                ", "
            )
            }]).build()",
            "self.play(ReplacementTransform($arrayIdent.all, $newArrayIdent.all))"
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
