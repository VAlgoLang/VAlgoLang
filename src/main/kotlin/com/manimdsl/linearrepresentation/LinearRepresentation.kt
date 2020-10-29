package com.manimdsl.linearrepresentation

import com.manimdsl.shapes.Shape
import com.manimdsl.shapes.StyleableShape
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

data class MoveToLine(val lineNumber: Int, val pointerName: String, val codeBlockName: String, val codeTextVariable: String) : ManimInstr {
    override fun toPython(): List<String> {
        return listOf(
                "self.move_arrow_to_line($lineNumber, $pointerName, $codeBlockName, $codeTextVariable)")
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

data class RestyleObject(
    val shape: Shape,
    val newStyle: StylesheetProperty,
): ManimInstr {
    override fun toPython(): List<String> {
        return if (shape is StyleableShape) {
            shape.restyle(newStyle)
        } else emptyList()
    }
}

data class UpdateVariableState(
        val variables: List<String>,
        val ident: String,
        val textColor: String? = null) : ManimInstr {
    override fun toPython(): List<String> =
        if(variables.isNotEmpty()) {
            listOf("self.play(*${ident}.update_variable(${variables}))")
        } else {
            emptyList()
        }

}




