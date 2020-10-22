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

        val codeBlock = listOf("let y = new Stack;", "y.push(2);", "y.push(3);", "y.pop();")
        val testIdent = Rectangle("testIdent", "2")
        val testIdent1 = Rectangle("testIdent1", "3")
        val stackIS = InitStructure(Coord(2.0, -1.0), Alignment.HORIZONTAL, "empty", "y")

        val stackIR = listOf(
            CodeBlock(codeBlock, "code_block", "code_text", "pointer"),
            MoveToLine(1, "pointer", "code_block"),
            stackIS,
            NewMObject(testIdent, "code_text"),
            MoveToLine(2, "pointer", "code_block"),
            MoveObject(testIdent, stackIS.shape, ObjectSide.ABOVE),
            MoveToLine(3, "pointer", "code_block"),
            NewMObject(testIdent1, "code_text"),
            MoveObject(testIdent1, testIdent, ObjectSide.ABOVE),
            MoveToLine(4, "pointer", "code_block"),
            MoveObject(testIdent1, testIdent, ObjectSide.ABOVE, 20, true),
        )

        val writer = ManimProjectWriter(ManimWriter(stackIR).build())

        val expected = File("src/test/testFiles/python/stack.py").readLines()
        val generated = File(writer.createPythonFile()).readLines()

        assertEquals(
            expected.filter { it.trim() != "" }.joinToString("\n"),
            generated.filter { it.trim() != "" }.joinToString("\n")
        )
    }
}