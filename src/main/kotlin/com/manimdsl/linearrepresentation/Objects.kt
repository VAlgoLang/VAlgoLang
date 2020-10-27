package com.manimdsl.linearrepresentation

import com.manimdsl.frontend.DataStructureType
import com.manimdsl.shapes.*

/** Objects **/

interface MObject : ManimInstr {
    val shape: Shape
}

/** Positioning **/
interface Position
object RelativeToMoveIdent : Position

data class Coord(val x: Double, val y: Double) : Position {
    override fun toString(): String {
        return "$x, $y"
    }
}

enum class ObjectSide(var coord: Coord) {
    ABOVE(Coord(0.0, 0.25)),
    BELOW(Coord(0.0, -0.25)),
    LEFT(Coord(-0.25, 0.0)),
    RIGHT(Coord(0.25, 0.0));

    fun addOffset(offset: Int): Coord {
        if (this == ABOVE) {
            return Coord(this.coord.x, this.coord.y + offset)
        }
        return coord
    }

    override fun toString(): String {
        return coord.toString()
    }
}

/** MObjects **/
data class CodeBlock(
    val lines: List<String>,
    val ident: String,
    val codeTextName: String,
    val pointerName: String,
    val textColor: String? = null,
) : MObject {
    override val shape: Shape = CodeBlockShape(ident, lines, textColor)

    override fun toPython(): List<String> {
        val list = mutableListOf("# Building code visualisation pane")
        list.addAll(shape.getConstructor())
        list.addAll(listOf("$codeTextName = $ident.build()",
        "$codeTextName.move_to(code_frame)",
        "self.code_end = len(code_lines) if self.code_end > len(code_lines) else self.code_end",
        "$codeTextName.scale(min(code_height / $codeTextName.get_height(), lhs_width / $codeTextName.get_width()))",
        "self.play(FadeIn($codeTextName[self.code_start:self.code_end]))",
        "# Constructing current line pointer",
        "$pointerName = ArrowTip(color=YELLOW).scale(0.7).flip(TOP)"))
        return list
    }
}

data class PartitionBlock(
        val scaleLeft: String,
        val scaleRight: String,
) : MObject {
    override val shape: Shape = NullShape
    override fun toPython(): List<String> {
        return listOf(
                "# Building partition of scene",
                "width = FRAME_WIDTH - 2 * SMALL_BUFF",
                "height = FRAME_HEIGHT - 2 * SMALL_BUFF",
                "lhs_width = width * $scaleLeft",
                "rhs_width = width * $scaleRight",
                "variable_height = (height - SMALL_BUFF) * $scaleLeft",
                "code_height = (height - SMALL_BUFF) * $scaleRight",
                "variable_frame = Rectangle(height=variable_height, width=lhs_width, color=YELLOW)",
                "variable_frame.to_corner(UL, buff=SMALL_BUFF)",
                "code_frame = Rectangle(height=code_height, width=lhs_width, color=GREEN)",
                "code_frame.next_to(variable_frame, DOWN, buff=SMALL_BUFF)",
                "self.play(FadeIn(variable_frame), FadeIn(code_frame)) \n"
        )
    }
}

data class VariableBlock(
    val variables: List<String>,
    val ident: String,
    val variableGroupName: String,
    val variableFrame : String,
    val textColor: String? = null,
) : MObject {
    override val shape: Shape = VariableBlockShape(ident, variables, variableFrame, textColor)

    override fun toPython(): List<String> {
        val list = mutableListOf("# Building variable visualisation pane")
        list.addAll(shape.getConstructor())
        list.addAll(listOf("$variableGroupName = $ident.build()",
            "$variableGroupName.move_to($variableFrame)",
            "self.play(FadeIn($variableGroupName))"))
        return list
    }
}

data class InitStructure(
    val type: DataStructureType,
    val position: Position,
    val alignment: Alignment,
    val ident: String,
    val text: String,
    val moveToShape: Shape? = null,
    val color: String? = null,
    val textColor: String? = null,
) : MObject {
    override val shape: Shape = InitStructureShape(ident, text, alignment, color, textColor)

    override fun toPython(): List<String> {
        val python =
            mutableListOf("# Constructing new ${type} \"${text}\"")
        python.addAll(shape.getConstructor())
        python.add(
            when (position) {
                is Coord -> "$shape.to_edge(np.array([${position.x}, ${position.y}, 0]))"
                else -> "self.place_relative_to_obj($shape, $moveToShape, ${ObjectSide.LEFT.addOffset(0)})"
            }
        )
        python.add("self.play(ShowCreation($shape))")
        return python
    }
}

data class NewMObject(override val shape: Shape, val codeBlockVariable: String) : MObject {
    override fun toPython(): List<String> {
        val list = mutableListOf("# Constructs a new ${shape.className} with value ${shape.text}")
        list.addAll(shape.getConstructor())
        list.addAll(listOf("self.place_relative_to_obj($shape, $codeBlockVariable, ${ObjectSide.RIGHT.addOffset(0)})",
            "self.play(FadeIn($shape))"
        ))
        return list
    }
}

object EmptyMObject : MObject {
    override val shape: Shape = NullShape
    override fun toPython(): List<String> = emptyList()
}