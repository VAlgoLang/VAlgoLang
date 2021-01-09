package com.manimdsl.linearrepresentation.datastructures.array

import com.manimdsl.linearrepresentation.ManimInstr
import com.manimdsl.runtime.ExecValue
import com.manimdsl.stylesheet.AnimationProperties
import com.manimdsl.stylesheet.StylesheetProperty
import com.manimdsl.stylesheet.StylesheetValidator

/**
 * Array element assignment object
 *
 * @property arrayIdent
 * @property index
 * @property newElemValue
 * @property animatedStyle
 * @property secondIndex
 * @property runtime
 * @property render
 * @constructor Create empty Array elem assign object
 */
data class ArrayElemAssignObject(
    val arrayIdent: String,
    val index: Int,
    val newElemValue: ExecValue,
    val animatedStyle: AnimationProperties?,
    val secondIndex: Int? = null,
    override val runtime: Double,
    override val render: Boolean
) : ManimInstr() {
    override fun toPython(): List<String> {
        val animationString =
            if (animatedStyle?.textColor != null) ", color=${animatedStyle.handleColourValue(animatedStyle.textColor)}" else ""
        val assignIndex2D = if (secondIndex == null) "" else ".rows[$secondIndex]"
        return listOf(
            getInstructionString(
                "$arrayIdent$assignIndex2D.update_element($index, \"${newElemValue.value}\"$animationString)",
                false
            )
        )
    }
}

/**
 * Array row replacement
 *
 * @property arrayIdent
 * @property index
 * @property newArray
 * @property runtime
 * @property render
 * @constructor Create empty Array replace row
 */
data class ArrayReplaceRow(
    val arrayIdent: String,
    val index: Int,
    val newArray: Array<ExecValue>,
    override val runtime: Double,
    override val render: Boolean
) :
    ManimInstr() {
    override fun toPython(): List<String> {
        return listOf(
            getInstructionString(
                "$arrayIdent.replace_row($index, [${newArray.joinToString(separator = ",")}])",
                true
            )
        )
    }
}

/**
 * 2D array swap
 *
 * @property arrayIdent
 * @property indices
 * @property runtime
 * @property render
 * @constructor Create empty Array2d swap
 */
data class Array2DSwap(
    val arrayIdent: String,
    val indices: List<Int>,
    override val runtime: Double,
    override val render: Boolean
) :
    ManimInstr() {
    override fun toPython(): List<String> {
        val instruction = getInstructionString("animations", true)
        return listOf("[$instruction for animations in array.swap_mobjects(${indices.joinToString(separator = ",")})]")
    }
}

/**
 * 1D array short swap
 *
 * @property arrayIdent
 * @property indices
 * @property runtime
 * @property render
 * @constructor Create empty Array short swap
 */
data class ArrayShortSwap(
    val arrayIdent: String,
    val indices: Pair<Int, Int>,
    override val runtime: Double,
    override val render: Boolean
) :
    ManimInstr() {
    override fun toPython(): List<String> {
        return listOf(getInstructionString("$arrayIdent.swap_mobjects(${indices.first}, ${indices.second})", true))
    }
}

/**
 * 1D array long swap
 *
 * @property arrayIdent
 * @property indices
 * @property elem1
 * @property elem2
 * @property animations
 * @property runtime
 * @property render
 * @constructor Create empty Array long swap
 */
data class ArrayLongSwap(
    val arrayIdent: String,
    val indices: Pair<Int, Int>,
    val elem1: String,
    val elem2: String,
    val animations: String,
    override val runtime: Double,
    override val render: Boolean
) : ManimInstr() {
    override fun toPython(): List<String> {
        val instruction = getInstructionString("animation", true)
        return listOf(
            "$elem1, $elem2, $animations = $arrayIdent.clone_and_swap(${indices.first}, ${indices.second})",
            "[$instruction for animation in $animations]",
            "$arrayIdent.array_elements[${indices.first}].text = $elem2",
            "$arrayIdent.array_elements[${indices.second}].text = $elem1"
        )
    }
}

/**
 * Array element restyle
 *
 * @property arrayIdent
 * @property indices
 * @property styleProperties
 * @property pointer
 * @property animationString
 * @property runtime
 * @property render
 * @property secondIndices
 * @constructor Create empty Array elem restyle
 */
data class ArrayElemRestyle(
    val arrayIdent: String,
    val indices: List<Int>,
    val styleProperties: StylesheetProperty,
    val pointer: Boolean? = false,
    val animationString: String? = null,
    override val runtime: Double,
    override val render: Boolean,
    val secondIndices: List<Int>? = null
) : ManimInstr() {

    private fun get2DAccess(index: Int): String {
        return if (secondIndices == null) "" else ".rows[${secondIndices[index]}]"
    }

    override fun toPython(): List<String> {
        val instructions = mutableListOf<String>()
        val animationString = animationString ?: "FadeToColor"

        val animationStringTakesColorAsParameter =
            StylesheetValidator.validAnimationStrings.getOrDefault(animationString, true)

        styleProperties.borderColor?.let {
            indices.forEachIndexed { index, i ->
                instructions.add(
                    "FadeToColor($arrayIdent${get2DAccess(index)}.array_elements[$i].shape, ${
                    styleProperties.handleColourValue(
                        it
                    )
                    })"
                )
            }
        }

        indices.forEachIndexed { index, i ->
            if (pointer == null || pointer) {
                instructions.add(
                    "FadeIn($arrayIdent${get2DAccess(index)}.array_elements[$i].pointer.next_to($arrayIdent${
                    get2DAccess(
                        index
                    )
                    }.array_elements[$i].shape, TOP, 0.01)." +
                        "set_color(${styleProperties.handleColourValue(styleProperties.borderColor ?: "WHITE")}))"
                )
            } else {
                instructions.add("self.fade_out_if_needed($arrayIdent${get2DAccess(index)}.array_elements[$i].pointer)")
            }
        }

        styleProperties.textColor?.let {
            indices.forEachIndexed { index, i ->
                if (!animationStringTakesColorAsParameter) {
                    instructions.add(
                        "FadeToColor($arrayIdent${get2DAccess(index)}.array_elements[$i].text, " +
                            "color=${styleProperties.handleColourValue(it)})"
                    )
                }
                instructions.add(
                    "$animationString($arrayIdent${get2DAccess(index)}.array_elements[$i].text, " +
                        "color=${styleProperties.handleColourValue(it)})"
                )
            }
        }

        return if (instructions.isEmpty()) {
            emptyList()
        } else {
            listOf(
                getInstructionString(
                    "[animation for animation in [${instructions.joinToString(", ")}] if animation]",
                    true
                )
            )
        }
    }
}
