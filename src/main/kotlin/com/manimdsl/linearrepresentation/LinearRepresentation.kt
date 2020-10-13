package com.manimdsl.linearrepresentation

interface ManimInstr {
    fun toPython(): List<String>
}

enum class Alignment(val angle: Int) {
    HORIZONTAL(0), VERTICAL(90)
}

/** Animation Functions **/

data class Sleep(val length: Double = 1.0) : ManimInstr {
    override fun toPython(): List<String> {
        return listOf("self.wait($length)")
    }
}

data class MoveToLine(val lineNumber: Int, val pointerName: String, val codeBlockName: String) : ManimInstr {
    override fun toPython(): List<String> {
        return listOf("self.move_arrow_to_line($lineNumber, $pointerName, $codeBlockName)")
    }
}

data class MoveObject(val ident: String, val moveToIdent: String, val objectSide: ObjectSide, val offset: Int = 0, val fadeOut: Boolean = false) :
    ManimInstr {
    override fun toPython(): List<String> {
        val instructions = mutableListOf("self.move_relative_to_obj($ident, $moveToIdent, ${objectSide.addOffset(offset)})")
        if (fadeOut) {
            instructions.add("self.play(FadeOut($ident))")
        }
        return instructions
    }
}






