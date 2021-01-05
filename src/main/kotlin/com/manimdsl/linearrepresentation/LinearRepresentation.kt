package com.manimdsl.linearrepresentation

import com.manimdsl.stylesheet.StylesheetProperty

abstract class ManimInstr {
    open val render: Boolean = false
    abstract val runtime: Double

    abstract fun toPython(): List<String>

    fun getInstructionString(instruction: String, spread: Boolean): String = if (render) {
        "self.play_animation(${if (spread) "*" else ""}${instruction}${getRuntimeString()})"
    } else {
        instruction
    }

    fun getRuntimeString(): String = ", run_time=$runtime"
}

enum class Alignment(val angle: String) {
    HORIZONTAL("0"), VERTICAL("TAU/4")
}

/** Animation Functions **/

data class Sleep(val length: Double = 1.0, override val runtime: Double) : ManimInstr() {
    override fun toPython(): List<String> {
        return listOf("self.wait($length)")
    }
}

data class MoveToLine(
    val lineNumber: Int,
    val pointerName: String,
    val codeBlockName: String,
    val codeTextVariable: String,
    override val runtime: Double
) : ManimInstr() {
    override fun toPython(): List<String> {
        return listOf(
            "self.move_arrow_to_line($lineNumber, $pointerName, $codeBlockName, $codeTextVariable)"
        )
    }
}

data class RestyleRectangle(
    val rectangle: Rectangle,
    val newStyle: StylesheetProperty,
    override val runtime: Double,
    override val render: Boolean,
) : ManimInstr() {
    override fun toPython(): List<String> {
        return if (render) {
            rectangle.restyle(newStyle, getRuntimeString())
        } else emptyList()
    }
}

/** Utility **/

data class UpdateSubtitle(
    val subtitleBlock: SubtitleBlock,
    val text: String,
    override val runtime: Double
) : ManimInstr() {
    override fun toPython(): List<String> {
        val instr = mutableListOf("self.play_animation(${subtitleBlock.ident}.clear())")
        if (!text.isBlank()) {
            instr.add("self.play_animation(${subtitleBlock.ident}.display('$text', self.get_time() + ${subtitleBlock.duration}))")
        }

        return instr
    }
}

data class UpdateVariableState(
    val variables: List<String>,
    val ident: String,
    val textColor: String? = null,
    override val runtime: Double
) : ManimInstr() {
    override fun toPython(): List<String> =
        listOf("self.play_animation(*$ident.update_variable(${variables.map { "\'${it}\'" }})${getRuntimeString()})")
}

data class CleanUpLocalDataStructures(
    val dataStructures: Set<String>,
    override val runtime: Double
) : ManimInstr() {
    override fun toPython(): List<String> {
        val instr = "self.play(${dataStructures.joinToString(", ") { "*$it.clean_up()" }}${getRuntimeString()})"
        return listOf(instr)
    }
}
