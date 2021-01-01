package com.manimdsl.linearrepresentation

import com.manimdsl.animation.ManimProjectWriter
import com.manimdsl.animation.ManimWriter
import com.manimdsl.frontend.NumberType
import com.manimdsl.frontend.StackType
import org.junit.Assert.assertEquals
import org.junit.jupiter.api.Test
import java.io.File

class TestLinearRepresentation {
    val defaultCodeBlockBoundaries =
        listOf(Pair(-7.0, 1.333333333333333), Pair(-2.0, 1.333333333333333), Pair(-7.0, -4.0), Pair(-2.0, -4.0))

    @Test
    fun mockStackLinearRepresentation() {
        val rectangle = Rectangle("rectangle", "2.0", "stack")
        val rectangle1 = Rectangle("rectangle1", "3.0", "stack")
        val stackIS = InitManimStack(
            StackType(NumberType),
            "stack",
            Coord(2.0, -1.0),
            Alignment.HORIZONTAL,
            "y",
            boundaries = emptyList(),
            uid = "y",
            render = true
        )

        stackIS.setNewBoundary(listOf(Pair(5.0, 4.0), Pair(7.0, 4.0), Pair(5.0, -4.0), Pair(7.0, -4.0)), 5)

        val codeLines = listOf(listOf("let y = new Stack<number>();"), listOf("y.push(2);"), listOf("y.push(3);"), listOf("y.pop();"))
        val codeBlock = CodeBlock(codeLines, "code_block", "code_text", "pointer", runtime = 1.0)
        codeBlock.setNewBoundary(defaultCodeBlockBoundaries, -1)

        val stackIR = listOf(
            codeBlock,
            MoveToLine(1, "pointer", "code_block", "code_text", runtime = 1.0),
            stackIS,
            MoveToLine(2, "pointer", "code_block", "code_text", runtime = 1.0),
            rectangle,
            StackPushObject(rectangle.ident, "stack", runtime = 1.0, render = true),
            MoveToLine(3, "pointer", "code_block", "code_text", runtime = 1.0),
            rectangle1,
            StackPushObject(rectangle1.ident, "stack", runtime = 1.0, render = true),
            MoveToLine(4, "pointer", "code_block", "code_text", runtime = 1.0),
            StackPopObject(rectangle1.ident, "stack", false, runtime = 1.0, render = true)
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
