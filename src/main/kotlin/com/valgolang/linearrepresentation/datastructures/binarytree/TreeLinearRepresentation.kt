package com.valgolang.linearrepresentation.datastructures.binarytree

import com.valgolang.linearrepresentation.ManimInstr
import com.valgolang.runtime.PrimitiveValue
import com.valgolang.runtime.datastructures.binarytree.BinaryTreeNodeValue
import com.valgolang.runtime.datastructures.binarytree.BinaryTreeValue
import com.valgolang.stylesheet.StylesheetProperty

/**
 * Tree append object
 *
 * @property parentNodeValue
 * @property childNodeValue
 * @property treeValue
 * @property left
 * @property runtime
 * @property render
 * @constructor Create empty Tree append object
 */
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
            "# Appends \"${childNodeValue.manimObject.ident}\" to the ${if (left) "left" else "right"} of \"${parentNodeValue.manimObject.ident}\"",
            "[$instruction for animation in ${treeValue.manimObject.ident}.check_if_child_will_cross_boundary(${parentNodeValue.manimObject.ident}, ${childNodeValue.manimObject.ident},${
            left.toString()
                .capitalize()
            })]",
            "[$instruction for animation in ${treeValue.manimObject.ident}.$methodName(${parentNodeValue.manimObject.ident}, ${childNodeValue.manimObject.ident})]",
        )
    }
}

/**
 * Node append object
 *
 * @property parentNodeValue
 * @property childNodeValue
 * @property left
 * @property runtime
 * @property render
 * @constructor Create empty Node append object
 */
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
            "# Appends \"${childNodeValue.manimObject.ident}\" to the ${if (left) "left" else "right"} of \"${parentNodeValue.manimObject.ident}\"",
            "${parentNodeValue.manimObject.ident}.$methodName(${childNodeValue.manimObject.ident}, 1)",
        )
    }
}

/**
 * Tree node restyle
 *
 * @property nodeIdent
 * @property styleProperties
 * @property highlightColor
 * @property animationString
 * @property runtime
 * @property render
 * @constructor Create empty Tree node restyle
 */
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
                "# Restyles \"$nodeIdent\" for indication",
                "self.play_animation(*${instructions}${getRuntimeString()})"
            )
        }
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
            "# Edits value of \"${treeValue.manimObject.ident}\" to \"${value}\"",
            getInstructionString(
                "${treeValue.manimObject.ident}.$methodName(${nodeValue.manimObject.ident}, \"${value}\")",
                true
            ),
        )
    }
}

/**
 * Tree delete object
 *
 * @property parentNodeValue
 * @property treeValue
 * @property left
 * @property runtime
 * @property render
 * @constructor Create empty Tree delete object
 */
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
            "# Removes ${if (left) "left" else "right"} of \"${parentNodeValue.manimObject.ident}\"",
            getInstructionString(
                "${treeValue.manimObject.ident}.$methodName(${parentNodeValue.manimObject.ident})",
                true
            ),
        )
    }
}
