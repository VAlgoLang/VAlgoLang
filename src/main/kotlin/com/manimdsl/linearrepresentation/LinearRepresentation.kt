package comcreat.manimdsl.linearrepresentation

import com.manimdsl.linearrepresentation.ObjectSide
import com.manimdsl.runtime.BinaryTreeNodeValue
import com.manimdsl.runtime.BinaryTreeValue
import com.manimdsl.runtime.ExecValue
import com.manimdsl.runtime.PrimitiveValue
import com.manimdsl.shapes.Shape
import com.manimdsl.shapes.StyleableShape
import com.manimdsl.stylesheet.AnimationProperties
import com.manimdsl.stylesheet.StyleSheetValidator
import com.manimdsl.stylesheet.StylesheetProperty

interface ManimInstr {
    val runtime: Double
    fun toPython(): List<String>
    fun getRuntimeString(): String = ", run_time=$runtime"
}

enum class Alignment(val angle: String) {
    HORIZONTAL("0"), VERTICAL("TAU/4")
}

/** Animation Functions **/

data class Sleep(val length: Double = 1.0, override val runtime: Double) : ManimInstr {
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
    val fadeOut: Boolean = false,
    override val runtime: Double
) :
    ManimInstr {
    override fun toPython(): List<String> {
        val instructions =
            mutableListOf("self.move_relative_to_obj($shape, $moveToShape, ${objectSide.addOffset(offset)})")
        if (fadeOut) {
            instructions.add("self.play_animation(FadeOut($shape)${getRuntimeString()})")
        }
        return instructions
    }
}

data class StackPushObject(
    val shape: Shape,
    val dataStructureIdentifier: String,
    val isPushPop: Boolean = false,
    val creationStyle: String? = null,
    override val runtime: Double
) : ManimInstr {

    override fun toPython(): List<String> {
        val creationString = if (isPushPop || creationStyle == null) "" else ", creation_style=\"$creationStyle\""
        val methodName = if (isPushPop) "push_existing" else "push"

        return listOf(
            "[self.play(*animation${getRuntimeString()}) for animation in $dataStructureIdentifier.$methodName(${shape.ident}$creationString)]",
            "$dataStructureIdentifier.add($shape)"
        )
    }
}

data class TreeDeleteObject(
    val parentNodeValue: BinaryTreeNodeValue,
    val treeValue: BinaryTreeValue,
    val left: Boolean,
    override val runtime: Double
) : ManimInstr {

    override fun toPython(): List<String> {
        val methodName = if (left) "delete_left" else "delete_right"
        return listOf(
                "self.play_animation(*${treeValue.manimObject.shape.ident}.$methodName(${parentNodeValue.manimObject.shape.ident})${getRuntimeString()})",

                )
    }
}

data class TreeEditValue(
    val nodeValue: BinaryTreeNodeValue,
    val value: PrimitiveValue,
    val treeValue: BinaryTreeValue,
    override val runtime: Double
) : ManimInstr {

    override fun toPython(): List<String> {
        val methodName = "edit_node_value"
        return listOf(
                "self.play_animation(*${treeValue.manimObject.shape.ident}.$methodName(${nodeValue.manimObject.shape.ident}, \"${value}\")${getRuntimeString()})",
        )
    }
}

data class TreeAppendObject(
    val parentNodeValue: BinaryTreeNodeValue,
    val childNodeValue: BinaryTreeNodeValue,
    val treeValue: BinaryTreeValue,
    val left: Boolean,
    override val runtime: Double
) : ManimInstr {

    override fun toPython(): List<String> {
        val methodName = if (left) "set_left" else "set_right"
        return listOf(
            "[self.play(animation${getRuntimeString()}) for animation in ${treeValue.manimObject.shape.ident}.check_if_child_will_cross_boundary(${parentNodeValue.manimObject.shape.ident}, ${childNodeValue.manimObject.shape.ident},${left.toString().capitalize()})]",
            "[self.play(animation${getRuntimeString()}) for animation in ${treeValue.manimObject.shape.ident}.$methodName(${parentNodeValue.manimObject.shape.ident}, ${childNodeValue.manimObject.shape.ident})]",
        )
    }
}

data class NodeFocusObject(
    val parentNodeValue: BinaryTreeNodeValue,
    override val runtime: Double,
) : ManimInstr {

    override fun toPython(): List<String> {
        return listOf(
                "self.play_animation(*${parentNodeValue.manimObject.shape.ident}eeee.highlight(${parentNodeValue.manimObject.shape.ident}.highlight_color)${getRuntimeString()})",
        )
    }
}

data class NodeUnfocusObject(
    val parentNodeValue: BinaryTreeNodeValue,
    override val runtime: Double,
) : ManimInstr {

    override fun toPython(): List<String> {
        return listOf(
                "self.play_animation(*${parentNodeValue.manimObject.shape.ident}.unhighlight()${getRuntimeString()})",
        )
    }
}

data class NodeAppendObject(
    val parentNodeValue: BinaryTreeNodeValue,
    val childNodeValue: BinaryTreeNodeValue,
    val left: Boolean,
    override val runtime: Double,
) : ManimInstr {

    override fun toPython(): List<String> {
        val methodName = if (left) "set_left" else "set_right"
        return listOf(
            "${parentNodeValue.manimObject.shape.ident}.$methodName(${childNodeValue.manimObject.shape.ident}, 1)",
        )
    }
}

data class StackPopObject(
    val shape: Shape,
    val dataStructureIdentifier: String,
    val insideMethodCall: Boolean,
    override val runtime: Double
) : ManimInstr {

    override fun toPython(): List<String> {
        return listOf(
            "[self.play(*animation${getRuntimeString()}) for animation in $dataStructureIdentifier.pop(${shape.ident}, fade_out=${(!insideMethodCall).toString()
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
    override val runtime: Double
) : ManimInstr {
    override fun toPython(): List<String> {
        val animationString = if (animatedStyle?.textColor != null) ", color=${animatedStyle.handleColourValue(animatedStyle.textColor)}" else ""
        val assignIndex2D = if (secondIndex == null) "" else ".rows[$secondIndex]"
        return listOf("self.play_animation($arrayIdent$assignIndex2D.array_elements[$index].replace_text(\"${newElemValue.value}\"$animationString)${getRuntimeString()})")
    }
}

data class ArrayReplaceRow(val arrayIdent: String, val index: Int, val newArray: Array<ExecValue>, override val runtime: Double) :
    ManimInstr {
    override fun toPython(): List<String> {
        return listOf("self.play_animation(*$arrayIdent.replace_row($index, [${newArray.joinToString(separator = ",")}])${getRuntimeString()})")
    }
}

data class Array2DSwap(val arrayIdent: String, val indices: List<Int>, override val runtime: Double) :
    ManimInstr {
    override fun toPython(): List<String> {
        return listOf("[self.play_animation(*animations${getRuntimeString()}) for animations in array.swap_mobjects(${indices.joinToString(separator = ",")})]")
    }
}

data class ArrayShortSwap(val arrayIdent: String, val indices: Pair<Int, Int>, override val runtime: Double) :
    ManimInstr {
    override fun toPython(): List<String> {
        return listOf("self.play_animation(*$arrayIdent.swap_mobjects(${indices.first}, ${indices.second})${getRuntimeString()})")
    }
}

data class ArrayLongSwap(
    val arrayIdent: String,
    val indices: Pair<Int, Int>,
    val elem1: String,
    val elem2: String,
    val animations: String,
    override val runtime: Double
) : ManimInstr {
    override fun toPython(): List<String> {
        return listOf(
                "$elem1, $elem2, $animations = $arrayIdent.clone_and_swap(${indices.first}, ${indices.second})",
                "[self.play_animation(*animation) for animation in $animations]",
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
    override val runtime: Double,
    val secondIndices: List<Int>? = null
) : ManimInstr {

    private fun get2DAccess(index: Int): String {
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

        return if (instructions.isEmpty()) {
            emptyList()
        } else {
            listOf(
                    "self.play_animation(*[animation for animation in [${instructions.joinToString(", ")}] if animation]${getRuntimeString()})"
            )
        }
    }
}

data class TreeNodeRestyle(
    val nodeIdent: String,
    val styleProperties: StylesheetProperty,
    val highlightColor: String? = null,
    val animationString: String? = null,
    override val runtime: Double
) : ManimInstr {
    override fun toPython(): List<String> {

        val instructions = if (highlightColor != null) {
            "$nodeIdent.highlight(${styleProperties.handleColourValue(highlightColor)})"
        } else {
            "$nodeIdent.unhighlight()"
        }

        return if (instructions.isEmpty()) {
            emptyList()
        } else {
            listOf(
                    "self.play_animation(*${instructions}${getRuntimeString()})"
            )
        }
    }
}

data class UpdateSubtitle(
    val shape: Shape,
    val text: String,
    override val runtime: Double
) : ManimInstr {
    override fun toPython(): List<String> {
        val instr = mutableListOf("self.play_animation(${shape.ident}.clear())")

        if (!text.isBlank()) {
            instr.add("self.play_animation(${shape.ident}.display($text))")
        }

        return instr
    }
}

data class RestyleObject(
    val shape: Shape,
    val newStyle: StylesheetProperty,
    override val runtime: Double
) : ManimInstr {
    override fun toPython(): List<String> {
        return if (shape is StyleableShape) {
            shape.restyle(newStyle, getRuntimeString())
        } else emptyList()
    }
}

data class UpdateVariableState(
    val variables: List<String>,
    val ident: String,
    val textColor: String? = null,
    override val runtime: Double
) : ManimInstr {
    override fun toPython(): List<String> =
            listOf("self.play_animation(*$ident.update_variable(${variables.map { "\"${it}\"" }})${getRuntimeString()})")
}
