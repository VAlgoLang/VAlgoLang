package com.manimdsl.linearrepresentation

import com.manimdsl.shapes.Shape

/** Objects **/

interface Object : ManimInstr {
    val ident: String
}

enum class ObjectSide(var coord: Pair<Double, Double>) {
    ABOVE(Pair(0.0, 0.25)),
    BELOW(Pair(-0.25, 0.0)),
    LEFT(Pair(-0.25, 0.0)),
    RIGHT(Pair(0.025, 0.0));

    fun addOffset(offset: Int): ObjectSide {
        val newCoord = if (this == ABOVE) {
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

data class InitStructure(val x: Int, val y: Int, val alignment: Alignment, override val ident: String,
                         val variableName: String) : Object {
    override fun toPython(): List<String> {
        return listOf(
                "$variableName = Init_structure(\"${ident}\", ${alignment.angle}).build()",
                "$variableName.to_edge(np.array([$x, $y, 0]))",
                "self.play(ShowCreation($variableName))"
        )
    }
}

data class NewObject(val shape: Shape, override val ident: String = shape.generateVariableName()) : Object {
    override fun toPython(): List<String> {
        return listOf("$ident = ${shape.className}(\"${shape.text}\").build()")
    }
}