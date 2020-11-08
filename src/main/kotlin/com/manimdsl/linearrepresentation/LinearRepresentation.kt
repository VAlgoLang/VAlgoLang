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
    val isPushPop: Boolean = false,
    val creationStyle: String? = null
) : ManimInstr {

    override fun toPython(): List<String> {
        val creationString = if (isPushPop || creationStyle == null) "" else ", creation_style=\"$creationStyle\""
        val methodName = if (isPushPop) "push_existing" else "push"

        return listOf(
            "[self.play(*animation) for animation in $dataStructureIdentifier.$methodName(${shape.ident}$creationString)]",
            "$dataStructureIdentifier.add($shape)"
        )
    }
}

data class StackPopObject(
    val shape: Shape,
    val dataStructureIdentifier: String,
    val insideMethodCall: Boolean
) : ManimInstr {

    override fun toPython(): List<String> {
        return listOf(
            "[self.play(*animation) for animation in $dataStructureIdentifier.pop(${shape.ident}, fade_out=${(!insideMethodCall).toString()
                .capitalize()})]"
        )
    }
}

data class ArrayElemAssignObject(
    val arrayIdent: String,
    val index: Int,
    val newElemValue: ExecValue,
    val animatedStyle: AnimationProperties?
) : ManimInstr {
    override fun toPython(): List<String> {
        val animationString = if (animatedStyle?.textColor != null) ", color=${animatedStyle.textColor}" else ""
        return listOf("self.play($arrayIdent.array_elements[$index].replace_text(\"${newElemValue.value}\"$animationString))")
    }
}

data class ArrayShortSwap(val arrayIdent: String, val indices: Pair<Int, Int>) : ManimInstr {
    override fun toPython(): List<String> {
        return listOf("self.play(*$arrayIdent.swap_mobjects(${indices.first}, ${indices.second}))")
    }
}

data class ArrayLongSwap(
    val arrayIdent: String,
    val indices: Pair<Int, Int>,
    val elem1: String,
    val elem2: String,
    val animations: String
) : ManimInstr {
    override fun toPython(): List<String> {
        return listOf(
            "$elem1, $elem2, $animations = $arrayIdent.clone_and_swap(${indices.first}, ${indices.second})",
            "[self.play(*animation) for animation in $animations]",
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
    val animationString: String? = null
) : ManimInstr {
    override fun toPython(): List<String> {
        val instructions = mutableListOf<String>()
        val animationString = animationString ?: "FadeToColor"
        val animationStringTakesColorAsParameter =
            StyleSheetValidator.validAnimationStrings.getOrDefault(animationString, true)

        styleProperties.borderColor?.let {
            for (i in indices) {
                instructions.add(
                    "FadeToColor($arrayIdent.array_elements[$i].shape, ${styleProperties.handleColourValue(
                        it
                    )})"
                )
            }
        }

        for (i in indices) {
            if (pointer == null || pointer) {
                instructions.add(
                    "FadeIn($arrayIdent.array_elements[$i].pointer.next_to($arrayIdent.array_elements[$i].shape, TOP, 0.01)." +
                            "set_color(${styleProperties.handleColourValue(styleProperties.borderColor ?: "WHITE")}))"
                )
            } else {
                instructions.add("self.fade_out_if_needed($arrayIdent.array_elements[$i].pointer)")
            }
        }

        styleProperties.textColor?.let {
            for (i in indices) {
                if (!animationStringTakesColorAsParameter) {
                    instructions.add(
                        "FadeToColor($arrayIdent.array_elements[$i].text, " +
                                "color=${styleProperties.handleColourValue(it)})"
                    )
                }
                instructions.add(
                    "$animationString($arrayIdent.array_elements[$i].text, " +
                            "color=${styleProperties.handleColourValue(it)})"
                )

            }
        }

        return if (instructions.isEmpty()) {
            emptyList()
        } else {
            listOf(
                "self.play(*[animation for animation in [${instructions.joinToString(", ")}] if animation], run_time=1.5)"
            )
        }
    }
}

data class RestyleObject(
    val shape: Shape,
    val newStyle: StylesheetProperty,
) : ManimInstr {
    override fun toPython(): List<String> {
        return if (shape is StyleableShape) {
            shape.restyle(newStyle)
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




