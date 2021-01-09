package com.manimdsl.linearrepresentation

import com.manimdsl.stylesheet.StylesheetProperty

/**
 * Manim instruction abstract class
 * Implemented by every linear representation instruction
 *
 * @constructor Create empty Manim instr
 */
abstract class ManimInstr {
    /** [Boolean] to express whether instruction should be rendered in animation **/
    open val render: Boolean = false

    /** Runtime of instruction in animation **/
    abstract val runtime: Double

    /**
     * Function for converting linear representation instruction to its corresponding Python instructions
     *
     * @return [List] of corresponding instructions as [String]s
     */
    abstract fun toPython(): List<String>

    /**
     * Get instruction string to be played using Manim
     *
     * @param instruction
     * @param spread: whether instruction needs to be unpacked
     * @return instruction that can be played to visualise on Manim Scene
     */
    fun getInstructionString(instruction: String, spread: Boolean): String = if (render) {
        "self.play_animation(${if (spread) "*" else ""}${instruction}${getRuntimeString()})"
    } else {
        instruction
    }

    /**
     * Get runtime string using [runtime] provided
     *
     * @return runtime string
     */
    fun getRuntimeString(): String = ", run_time=$runtime"
}

/** Animation Functions **/

data class Sleep(val length: Double = 1.0, override val runtime: Double) : ManimInstr() {
    override fun toPython(): List<String> {
        return listOf("self.wait($length)")
    }
}

/**
 * Move arrow to line [lineNumber] of code block
 *
 * @property lineNumber
 * @property pointerName
 * @property codeBlockName
 * @property codeTextVariable
 * @property runtime
 * @constructor Create empty Move to line
 */
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

/**
 * Restyle [rectangle] with [newStyle]
 *
 * @property rectangle
 * @property newStyle
 * @property runtime
 * @property render
 * @constructor Create empty Restyle rectangle
 */
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

/**
 * Update subtitle with [text] as new text
 *
 * @property subtitleBlock
 * @property text
 * @property runtime
 * @constructor Create empty Update subtitle
 */
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

/**
 * Update variable block with [variables]
 *
 * @property variables
 * @property ident
 * @property textColor
 * @property runtime
 * @constructor Create empty Update variable state
 */
data class UpdateVariableState(
    val variables: List<String>,
    val ident: String,
    val textColor: String? = null,
    override val runtime: Double
) : ManimInstr() {
    override fun toPython(): List<String> =
        listOf("self.play_animation(*$ident.update_variable(${variables.map { "\'${it}\'" }})${getRuntimeString()})")
}

/**
 * Clean up local data structures
 *
 * @property dataStructures
 * @property runtime
 * @constructor Create empty Clean up local data structures
 */
data class CleanUpLocalDataStructures(
    val dataStructures: Set<String>,
    override val runtime: Double
) : ManimInstr() {
    override fun toPython(): List<String> {
        val instr = "self.play(${dataStructures.joinToString(", ") { "*$it.clean_up()" }}${getRuntimeString()})"
        return listOf(instr)
    }
}
