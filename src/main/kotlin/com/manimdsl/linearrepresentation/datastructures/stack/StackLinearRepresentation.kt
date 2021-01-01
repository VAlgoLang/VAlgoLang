package com.manimdsl.linearrepresentation.datastructures.stack

import com.manimdsl.linearrepresentation.ManimInstr

data class StackPushObject(
    val ident: String,
    val dataStructureIdentifier: String,
    val isPushPop: Boolean = false,
    val creationStyle: String? = null,
    override val runtime: Double,
    override val render: Boolean,
) : ManimInstr() {

    override fun toPython(): List<String> {
        val creationString = if (isPushPop || creationStyle == null) "" else ", creation_style=\"$creationStyle\""
        val methodName = if (isPushPop) "push_existing" else "push"
        val instruction = getInstructionString("animation", true)
        return listOf(
            "[$instruction for animation in $dataStructureIdentifier.$methodName(${ident}$creationString)]",
            "$dataStructureIdentifier.add($ident.all)"
        )
    }
}

data class StackPopObject(
    val ident: String,
    val dataStructureIdentifier: String,
    val insideMethodCall: Boolean,
    override val runtime: Double,
    override val render: Boolean,
) : ManimInstr() {

    override fun toPython(): List<String> {
        val instruction = getInstructionString("animation", true)
        return listOf(
            "[$instruction for animation in $dataStructureIdentifier.pop($ident, fade_out=${
            (!insideMethodCall).toString()
                .capitalize()
            })]"
        )
    }
}
