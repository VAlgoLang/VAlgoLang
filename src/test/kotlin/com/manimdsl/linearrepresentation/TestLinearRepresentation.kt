package com.manimdsl.linearrepresentation

import com.manimdsl.*
import org.junit.jupiter.api.Test

class TestLinearRepresentation {

    @Test
    fun mockStackLinearRepresntation() {

        val codeBlock = listOf("let y = new Stack;", "y.push(2);", "y.pop();");

        val stackIR = listOf(
            CodeBlock(codeBlock, "code_block", "code_text", "pointer"),
            InitStructure(2, -1, Alignment.HORIZONTAL, "y"),
            NewObject(Rectangle("2"), "testIdent"),
            MoveToLine(2, "pointer", "code_block"),
            MoveObject("testIdent", "y", ObjectSide.ABOVE),
            MoveToLine(3, "pointer", "code_block"),
            MoveObject("testIdent", "y", ObjectSide.ABOVE, 5),
            Sleep(2.0)
        )

        val writer = ManimProjectWriter(ManimWriter(stackIR).build())
        writer.createPythonFile(false, "test.py")
        writer.generateAnimation("test.py")

//        println(ManimWriter(stackIR).build())
//        assertEquals(ManimDSLParser(inputFile.inputStream()).parseFile().first, ExitStatus.EXIT_SUCCESS)
    }

}