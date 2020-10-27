package com.manimdsl.linearrepresentation

import com.manimdsl.animation.ManimProjectWriter
import com.manimdsl.animation.ManimWriter
import com.manimdsl.frontend.NumberType
import com.manimdsl.frontend.StackType
import com.manimdsl.shapes.Rectangle
import org.junit.Assert.assertEquals
import org.junit.jupiter.api.Test
import java.io.File


class TestLinearRepresentation {

    @Test
    fun mockStackLinearRepresentation() {
        val codeBlock = listOf("let y = new Stack<number>;", "y.push(2);", "y.push(3);", "y.pop();")
        val rectangle = Rectangle("rectangle", "2.0", "stack")
        val rectangle1 = Rectangle("rectangle1", "3.0", "stack")
        val stackIS = InitManimStack(
            StackType(NumberType),
            Coord(2.0, -1.0),
            Alignment.HORIZONTAL,
            "stack",
            "y",
            boundary = emptyList()
        )

        stackIS.setNewBoundary(listOf(Pair(5, 4), Pair(7, 4), Pair(5, -4), Pair(7, -4)), 5)

        val stackIR = listOf(
            CodeBlock(codeBlock, "code_block", "code_text", "pointer"),
            MoveToLine(1, "pointer", "code_block", "code_text"),
            stackIS,
            MoveToLine(2, "pointer", "code_block"),
            NewMObject(rectangle, "code_text"),
            StackPushObject(rectangle, "stack"),
            MoveToLine(3, "pointer", "code_block"),
            NewMObject(rectangle1, "code_text"),
            StackPushObject(rectangle1, "stack"),
            MoveToLine(4, "pointer", "code_block"),
            StackPopObject(rectangle1, "stack", false)
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