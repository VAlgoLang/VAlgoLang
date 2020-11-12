package com.manimdsl.linearrepresentation

import com.manimdsl.runtime.ExecValue
import com.manimdsl.shapes.Shape
import com.manimdsl.shapes.StyleableShape
import com.manimdsl.stylesheet.AnimationProperties
import com.manimdsl.stylesheet.StyleSheetValidator
import com.manimdsl.stylesheet.StylesheetProperty

interface ManimInstr {
    fun toPython(): List<String>
}

interface ManimInstrWithRuntime : ManimInstr {
    val hide: Boolean
    val runtime: Double?
    fun playIfNotHidden(code: String) = if (hide) code else "self.play(*$code)"
    fun getRuntimeString(): String = if (runtime != null) ", run_time=$runtime" else ""
}

enum class Alignment(val angle: String) {
    HORIZONTAL("0"), VERTICAL("TAU/4")
}

/** Animation Functions **/

data class Sleep(val length: Double = 1.0) : ManimInstr {
    override fun toPython(): List<String> {
        return listOf("self.wait($length)")
    }
}

data class MoveToLine(
    val lineNumber: Int,
    val pointerName: String,
    val codeBlockName: String,
    val codeTextVariable: String
) : ManimInstr {
    override fun toPython(): List<String> {
        return listOf(
            "self.move_arrow_to_line($lineNumber, $pointerName, $codeBlockName, $codeTextVariable)"
        )
    }
}

data class MoveObject(
    val shape: Shape,
    val moveToShape: Shape,
    val objectSide: ObjectSide,
    val offset: Int = 0,
    val fadeOut: Boolean = false
) :
    ManimInstr {
    override fun toPython(): List<String> {
        val instructions =
            mutableListOf("self.move_relative_to_obj($shape, $moveToShape, ${objectSide.addOffset(offset)})")
        if (fadeOut) {
            instructions.add("self.play(FadeOut($shape))")
        }
        return instructions
    }
}

data class StackPushObject(
        val shape: Shape,
        val dataStructureIdentifier: String,
        override val hide: Boolean,
        val isPushPop: Boolean = false,
        val creationStyle: String? = null,
        override val runtime: Double? = null
) : ManimInstrWithRuntime {

    override fun toPython(): List<String> {
        val creationString = if (isPushPop || creationStyle == null) "" else ", creation_style=\"$creationStyle\""
        val methodName = if (isPushPop) "push_existing" else "push"
        val code = if (hide) {
            "[animation${getRuntimeString()} for animation in $dataStructureIdentifier.$methodName(${shape.ident}$creationString)]"
        } else {
            "[self.play(*animation${getRuntimeString()}) for animation in $dataStructureIdentifier.$methodName(${shape.ident}$creationString)]"
        }
        return listOf(
                code,
            "$dataStructureIdentifier.add($shape)"
        )
    }
}

data class StackPopObject(
        val shape: Shape,
        val dataStructureIdentifier: String,
        val insideMethodCall: Boolean,
        override val runtime: Double? = null, override val hide: Boolean
) : ManimInstrWithRuntime {

    override fun toPython(): List<String> {
        return listOf(
                "[${playIfNotHidden("animation${getRuntimeString()}")} for animation in $dataStructureIdentifier.pop(${shape.ident}, fade_out=${(!insideMethodCall).toString()
                        .capitalize()})]"
            )

    }
}

data class ArrayElemAssignObject(
    val arrayIdent: String,
    val index: Int,
    val newElemValue: ExecValue,
    val animatedStyle: AnimationProperties?,
    val secondIndex: Int? = null,
    override val hide: Boolean,
    override val runtime: Double?
) : ManimInstrWithRuntime {
    override fun toPython(): List<String> {
        val animationString = if (animatedStyle?.textColor != null) ", color=${animatedStyle.textColor.toUpperCase()}" else ""
        val assignIndex2D = if (secondIndex == null) "" else ".rows[$secondIndex]"
        return if (!hide) {
            listOf(
            "self.play($arrayIdent$assignIndex2D.array_elements[$index].replace_text(\"${newElemValue.value}\"$animationString)${getRuntimeString()})"
            )
        } else {
            listOf("$arrayIdent$assignIndex2D.array_elements[$index].replace_text(\"${newElemValue.value}\"$animationString)")
        }
    }
}

data class ArrayReplaceRow(val arrayIdent: String, val index: Int, val newArray: Array<ExecValue>, override val runtime: Double? = null, override val hide: Boolean) :
        ManimInstrWithRuntime {
    override fun toPython(): List<String> {
        return if (hide) {
            listOf("$arrayIdent.replace_row($index, [${newArray.joinToString(separator = ",")}])${getRuntimeString()}")
        } else {
            listOf("self.play(*$arrayIdent.replace_row($index, [${newArray.joinToString(separator = ",")}])${getRuntimeString()})")
        }
    }
}

data class Array2DSwap(val arrayIdent: String, val indices: List<Int>, override val runtime: Double? = null, override val hide: Boolean) :
        ManimInstrWithRuntime {
    override fun toPython(): List<String> {
        return listOf("[${playIfNotHidden("animations${getRuntimeString()}")} for animations in array.swap_mobjects(${indices.joinToString(separator = ",")})]")
    }
}


data class ArrayShortSwap(val arrayIdent: String, val indices: Pair<Int, Int>, override val runtime: Double? = null, override val hide: Boolean) :
    ManimInstrWithRuntime {
    override fun toPython(): List<String> {
        return listOf("self.play(*$arrayIdent.swap_mobjects(${indices.first}, ${indices.second})${getRuntimeString()})")
    }
}

data class ArrayLongSwap(
        val arrayIdent: String,
        val indices: Pair<Int, Int>,
        val elem1: String,
        val elem2: String,
        val animations: String,
        override val runtime: Double? = null,
        override val hide: Boolean
) : ManimInstrWithRuntime {
    override fun toPython(): List<String> {
        return listOf(
            "$elem1, $elem2, $animations = $arrayIdent.clone_and_swap(${indices.first}, ${indices.second})",
            "[${playIfNotHidden("animation")} for animation in $animations]",
            "$arrayIdent.array_elements[${indices.first}].text = $elem2",
            "$arrayIdent.array_elements[${indices.second}].text = $elem1"
        )
    }
}

data class ArrayElemRestyle(
        val arrayIdent: String,
        val indices: List<Int>,
        val styleProperties: StylesheetProperty,
        val pointer: Boolean? = false,
        val animationString: String? = null,
        override val runtime: Double? = null,
        val secondIndices: List<Int>? = null,
        override val hide: Boolean
) : ManimInstrWithRuntime {

    private fun get2DAccess(index: Int): String {
        styleProperties
        return if (secondIndices == null) "" else ".rows[${secondIndices[index]}]"
    }


    override fun toPython(): List<String> {
        val instructions = mutableListOf<String>()
        val animationString = animationString ?: "FadeToColor"



        val animationStringTakesColorAsParameter =
            StyleSheetValidator.validAnimationStrings.getOrDefault(animationString, true)

        styleProperties.borderColor?.let {
            indices.forEachIndexed { index, i ->
                instructions.add(
                    "FadeToColor($arrayIdent${get2DAccess(index)}.array_elements[$i].shape, ${styleProperties.handleColourValue(
                        it
                    )})"
                )

            }
        }

        indices.forEachIndexed { index, i ->
            if (pointer == null || pointer) {
                instructions.add(
                    "FadeIn($arrayIdent${get2DAccess(index)}.array_elements[$i].pointer.next_to($arrayIdent${get2DAccess(index)}.array_elements[$i].shape, TOP, 0.01)." +
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

        return if (instructions.isEmpty() || hide) {
            emptyList()
        } else {
            listOf(
                "self.play(*[animation for animation in [${instructions.joinToString(", ")}] if animation]${getRuntimeString()})"
            )
        }
    }
}

data class RestyleObject(
        val shape: Shape,
        val newStyle: StylesheetProperty,
        override val runtime: Double?, override val hide: Boolean = false
) : ManimInstrWithRuntime {
    override fun toPython(): List<String> {
        return if (shape is StyleableShape) {
            shape.restyle(newStyle, getRuntimeString())
        } else emptyList()
    }
}

data class UpdateVariableState(
    val variables: List<String>,
    val ident: String,
    val textColor: String? = null
) : ManimInstr {
    override fun toPython(): List<String> =
        if (variables.isNotEmpty()) {
            listOf("self.play(*${ident}.update_variable(${variables.map { "\"${it}\"" }}))")
        } else {
            emptyList()
        }

}




