package com.manimdsl.linearrepresentation

import com.manimdsl.runtime.ExecValue
import com.manimdsl.runtime.PrimitiveValue
import com.manimdsl.runtime.datastructures.binarytree.BinaryTreeNodeValue
import com.manimdsl.runtime.datastructures.binarytree.BinaryTreeValue
import com.manimdsl.shapes.Shape
import com.manimdsl.shapes.StyleableShape
import com.manimdsl.stylesheet.AnimationProperties
import com.manimdsl.stylesheet.StyleSheetValidator
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

data class MoveObject(
    val shape: Shape,
    val moveToShape: Shape,
    val objectSide: ObjectSide,
    val offset: Int = 0,
    val fadeOut: Boolean = false,
    override val runtime: Double
) :
    ManimInstr() {
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
    override val runtime: Double,
    override val render: Boolean,
) : ManimInstr() {

    override fun toPython(): List<String> {
        val creationString = if (isPushPop || creationStyle == null) "" else ", creation_style=\"$creationStyle\""
        val methodName = if (isPushPop) "push_existing" else "push"
        val instruction = getInstructionString("animation", true)
        return listOf(
            "[$instruction for animation in $dataStructureIdentifier.$methodName(${shape.ident}$creationString)]",
            "$dataStructureIdentifier.add($shape)"
        )
    }
}

data class TreeDeleteObject(
    val parentNodeValue: BinaryTreeNodeValue,
    val treeValue: BinaryTreeValue,
    val left: Boolean,
    override val runtime: Double,
    override val render: Boolean,
) : ManimInstr() {

    override fun toPython(): List<String> {
        val methodName = if (left) "delete_left" else "delete_right"
        return listOf(
            getInstructionString(
                "${treeValue.manimObject.shape.ident}.$methodName(${parentNodeValue.manimObject.shape.ident})",
                true
            ),
        )
    }
}

data class TreeEditValue(
    val nodeValue: BinaryTreeNodeValue,
    val value: PrimitiveValue,
    val treeValue: BinaryTreeValue,
    override val runtime: Double,
    override val render: Boolean
) : ManimInstr() {

    override fun toPython(): List<String> {
        val methodName = "edit_node_value"
        return listOf(
            getInstructionString(
                "${treeValue.manimObject.shape.ident}.$methodName(${nodeValue.manimObject.shape.ident}, \"${value}\")",
                true
            ),
        )
    }
}

data class TreeAppendObject(
    val parentNodeValue: BinaryTreeNodeValue,
    val childNodeValue: BinaryTreeNodeValue,
    val treeValue: BinaryTreeValue,
    val left: Boolean,
    override val runtime: Double,
    override val render: Boolean
) : ManimInstr() {

    override fun toPython(): List<String> {
        val methodName = if (left) "set_left" else "set_right"
        val instruction = getInstructionString("animation", false)
        return listOf(
            "[$instruction for animation in ${treeValue.manimObject.shape.ident}.check_if_child_will_cross_boundary(${parentNodeValue.manimObject.shape.ident}, ${childNodeValue.manimObject.shape.ident},${left.toString()
                .capitalize()})]",
            "[$instruction for animation in ${treeValue.manimObject.shape.ident}.$methodName(${parentNodeValue.manimObject.shape.ident}, ${childNodeValue.manimObject.shape.ident})]",
        )
    }
}

data class NodeFocusObject(
    val parentNodeValue: BinaryTreeNodeValue,
    override val runtime: Double,
    override val render: Boolean,
) : ManimInstr() {

    override fun toPython(): List<String> {
        return if (render) {
            listOf(
                "self.play(*${parentNodeValue.manimObject.shape.ident}eeee.highlight(${parentNodeValue.manimObject.shape.ident}.highlight_color)${getRuntimeString()})",
            )
        } else {
            emptyList()
        }
    }
}

data class NodeUnfocusObject(
    val parentNodeValue: BinaryTreeNodeValue,
    override val runtime: Double,
    override val render: Boolean
) : ManimInstr() {

    override fun toPython(): List<String> {
        return if (render) {
            listOf("self.play_animation(*${parentNodeValue.manimObject.shape.ident}.unhighlight()${getRuntimeString()})")
        } else {
            emptyList()
        }
    }
}

data class NodeAppendObject(
    val parentNodeValue: BinaryTreeNodeValue,
    val childNodeValue: BinaryTreeNodeValue,
    val left: Boolean,
    override val runtime: Double,
    override val render: Boolean,
) : ManimInstr() {

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
    override val runtime: Double,
    override val render: Boolean,
) : ManimInstr() {

    override fun toPython(): List<String> {
        val instruction = getInstructionString("animation", true)
        return listOf(
            "[$instruction for animation in $dataStructureIdentifier.pop(${shape.ident}, fade_out=${(!insideMethodCall).toString()
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
    override val runtime: Double,
    override val render: Boolean
) : ManimInstr() {
    override fun toPython(): List<String> {
        val animationString =
            if (animatedStyle?.textColor != null) ", color=${animatedStyle.handleColourValue(animatedStyle.textColor)}" else ""
        val assignIndex2D = if (secondIndex == null) "" else ".rows[$secondIndex]"
        return listOf(
            getInstructionString(
                "$arrayIdent$assignIndex2D.array_elements[$index].replace_text(\"${newElemValue.value}\"$animationString)",
                false
            )
        )
    }
}

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
                getInstructionString("[animation for animation in [${instructions.joinToString(", ")}] if animation]", true)
            )
        }
    }
}

data class TreeNodeRestyle(
    val nodeIdent: String,
    val styleProperties: StylesheetProperty,
    val highlightColor: String? = null,
    val animationString: String? = null,
    override val runtime: Double,
    override val render: Boolean,
) : ManimInstr() {
    override fun toPython(): List<String> {

        val instructions = if (!render) {
            ""
        } else if (highlightColor != null) {
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
    val subtitleBlock: SubtitleBlock,
    val text: String,
    override val runtime: Double
) : ManimInstr() {
    override fun toPython(): List<String> {
        val instr = mutableListOf("self.play_animation(${subtitleBlock.ident}.clear())")
        if (!text.isBlank()) {
            instr.add("self.play_animation(${subtitleBlock.ident}.display($text, self.get_time() + ${subtitleBlock.duration}))")
        }

        return instr
    }
}

data class RestyleObject(
    val shape: Shape,
    val newStyle: StylesheetProperty,
    override val runtime: Double,
    override val render: Boolean,
) : ManimInstr() {
    override fun toPython(): List<String> {
        return if (shape is StyleableShape && render) {
            shape.restyle(newStyle, getRuntimeString())
        } else emptyList()
    }
}

data class UpdateVariableState(
    val variables: List<String>,
    val ident: String,
    val textColor: String? = null,
    override val runtime: Double
) : ManimInstr() {
    override fun toPython(): List<String> =
        listOf("self.play_animation(*$ident.update_variable(${variables.map { "\"${it}\"" }})${getRuntimeString()})")
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
