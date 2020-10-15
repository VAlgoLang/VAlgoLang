package com.manimdsl.linearrepresentation

import com.manimdsl.shapes.Shape

/** Objects **/

interface MObject : ManimInstr {
    val ident: String
}

data class Coord(val x: Double, val y: Double) {
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

data class CodeBlock(
    val lines: List<String>,
    override val ident: String,
    val codeTextName: String,
    val pointerName: String
) : MObject {

    override fun toPython(): List<String> {
        return listOf(
            "$ident = Code_block([\"${lines.joinToString("\",\"")}\"])",
            "$codeTextName = $ident.build()",
            "self.place_at($codeTextName, -1, 0)",
            "self.play(FadeIn($codeTextName))",
            "$pointerName = ArrowTip(color=YELLOW).scale(0.7).flip(TOP)",
        )
    }
}

data class InitStructure(
    val x: Int, val y: Int, val alignment: Alignment, override val ident: String,
    val text: String, val moveIdent: String? = null
) : MObject {
    override fun toPython(): List<String> {
        return listOf(
            "$ident = Init_structure(\"${text}\", ${alignment.angle}).build()",
            "$ident.to_edge(np.array([$x, $y, 0]))",
            "self.play(ShowCreation($ident))"
        )
    }
}

data class InitStructureRelative(
    val alignment: Alignment, override val ident: String, val text: String,
    val moveIdent: String? = null
) : MObject {
    override fun toPython(): List<String> {
        return listOf(
            "$ident = Init_structure(\"${text}\", ${alignment.angle}).build()",
            "self.place_relative_to_obj($ident, $moveIdent, ${ObjectSide.LEFT.addOffset(0)})",
            "self.play(ShowCreation($ident))"
        )
    }
}

data class NewMObject(val shape: Shape, override val ident: String, val codeBlockVariable: String) : MObject {
    override fun toPython(): List<String> {
        return listOf(
            "$ident = ${shape.className}(\"${shape.text}\").build()",
            "self.place_relative_to_obj($ident, $codeBlockVariable, ${ObjectSide.RIGHT.addOffset(0)})",
            "self.play(FadeIn($ident))"
        )
    }
}

object EmptyMObject : MObject {
    override val ident: String = "null"
    override fun toPython(): List<String> = emptyList()
}