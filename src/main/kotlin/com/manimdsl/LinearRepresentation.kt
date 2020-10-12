package com.manimdsl

import java.util.*

interface IR {
    fun toPython(): List<String>
}

enum class ObjectSide(var coord: Pair<Double, Double>) {
    ABOVE(Pair(0.0, 0.25)),
    BELOW(Pair(0.0, 0.25)),
    LEFT(Pair(0.0, 0.25)),
    RIGHT(Pair(0.0, 0.25));

    fun addOffset(offset: Int): ObjectSide {
        val newCoord = if(this == ABOVE) {
            Pair(this.coord.first, this.coord.second + offset)
        } else {
            this.coord
        }
        this.coord = newCoord

        return this
    }

    override fun toString(): String {
        return "${this.coord.first}, ${this.coord.second}"
    }
}

enum class Alignment {
    HORIZONTAL, VERTICAL
}

interface Object : IR {
    val ident: String
}

data class CodeBlock(
    val lines: List<String>,
    override val ident: String,
    val codeTextName: String,
    val pointerName: String
) : Object {

    override fun toPython(): List<String> {
        return listOf(
            "$ident = Code_block([\"${lines.joinToString("\",\"")}\"])",
            "$codeTextName = $ident.build()",
            "self.place_at($codeTextName, -1, 0)",
            "self.play(FadeIn($codeTextName))",
            "$pointerName = ArrowTip(color=YELLOW).scale(0.7).flip(TOP)",
            "self.move_arrow_to_line(1, $pointerName, $ident)"
        )
    }
}

data class MoveToLine(val lineNumber: Int, val pointerName: String, val codeBlockName: String) : IR {
    override fun toPython(): List<String> {
        return listOf("self.move_arrow_to_line($lineNumber, $pointerName, $codeBlockName)")
    }
}

data class InitStructure(val x: Int, val y: Int, val alignment: Alignment, override val ident: String) : Object {
    override fun toPython(): List<String> {
        return listOf(
            "$ident = Line()",
            "${ident}.to_edge(np.array([$x,$y, 0]))",
            "self.play(ShowCreation($ident))"
        )
    }
}

data class NewObject(val shape: Shape, override val ident: String = shape.generateVariableName()) : Object {
    override fun toPython(): List<String> {
        return listOf("$ident = ${shape.className}(\"${shape.text}\").build()")
    }
}

data class MoveObject(val ident: String, val moveToIdent: String, val objectSide: ObjectSide, val offset: Int = 0) :
    IR {
    override fun toPython(): List<String> {
        return listOf("self.move_relative_to_obj($ident, $moveToIdent, ${objectSide.addOffset(offset)})")
    }
}

data class Rectangle(override val text: String) : Shape {


    override val classPath: String = "pythonLib/rectangle.py"
    override val className: String = "Rectangle_block"

    override fun generateVariableName(): String {
        return UUID.randomUUID().toString()
    }


}

interface Shape {

    val text: String
    val classPath: String
    val className: String

    fun generateVariableName(): String
}