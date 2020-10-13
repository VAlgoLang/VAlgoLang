package com.manimdsl.linearrepresentation

import com.manimdsl.ManimProjectWriter
import com.manimdsl.ManimWriter
import com.manimdsl.shapes.Rectangle
import org.junit.Assert.assertEquals
import org.junit.jupiter.api.Test
import java.io.File


class TestLinearRepresentation {

    @Test
    fun mockStackLinearRepresentation() {

        val codeBlock = listOf("let y = new Stack;", "y.push(2);", "y.push(3);", "y.pop();");

        val stackIR = listOf(
            CodeBlock(codeBlock, "code_block", "code_text", "pointer"),
            InitStructure(2, -1, Alignment.HORIZONTAL, "y", "empty"),
            NewObject(Rectangle("2"), "testIdent"),
            MoveToLine(2, "pointer", "code_block"),
            MoveObject("testIdent", "empty", ObjectSide.ABOVE),
            MoveToLine(3, "pointer", "code_block"),
            NewObject(Rectangle("3"), "testIdent1"),
            MoveObject("testIdent1", "testIdent", ObjectSide.ABOVE),
            Sleep(2.0),
            MoveToLine(4, "pointer", "code_block"),
            MoveObject("testIdent1", "testIdent", ObjectSide.ABOVE, 20, true),
            Sleep(2.0)
        )

        val writer = ManimProjectWriter(ManimWriter(stackIR).build())

        val expected = File("src/test/testFiles/python/stack.py").readText()
        val generated = File(writer.createPythonFile()).readText()

        assertEquals(expected, generated)
    }
}