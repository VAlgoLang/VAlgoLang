package com.manimdsl.linearrepresentation

import com.manimdsl.frontend.DataStructureType
import com.manimdsl.shapes.CodeBlockShape
import com.manimdsl.shapes.InitManimStackShape
import com.manimdsl.shapes.NullShape
import com.manimdsl.shapes.Shape

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
        return listOf(
            "# Building code visualisation pane",
            shape.getConstructor(),
            "$codeTextName = $ident.build()",
            "self.place_at($codeTextName, -1, 0)",
            "self.play(FadeIn($codeTextName))",
            "# Constructing current line pointer",
            "$pointerName = ArrowTip(color=YELLOW).scale(0.7).flip(TOP)",
        )
    }
}

data class InitManimStack(
    val type: DataStructureType,
    val position: Position,
    val alignment: Alignment,
    val ident: String,
    val text: String,
    val boundaryCoordinates: List<Pair<Int, Int>>,
    val moveToShape: Shape? = null,
    val color: String? = null,
    val textColor: String? = null,
    var boundary: List<Pair<Int, Int>> = emptyList(),
    var maxSize: Int = -1
) : MObject {
    override val shape: Shape = InitManimStackShape(ident, text, boundaryCoordinates, alignment, color, textColor)

    override fun toPython(): List<String> {
        val python =
            mutableListOf("# Constructing new ${type} \"${text}\"", shape.getConstructor())
        python.add("self.play($ident.create_init(\"$text\"))")
        return python
    }
}

data class NewMObject(override val shape: Shape, val codeBlockVariable: String) : MObject {
    override fun toPython(): List<String> {
        return listOf(
            "# Constructs a new ${shape.className} with value ${shape.text}",
            shape.getConstructor(),
        )
    }
}

object EmptyMObject : MObject {
    override val shape: Shape = NullShape
    override fun toPython(): List<String> = emptyList()
}