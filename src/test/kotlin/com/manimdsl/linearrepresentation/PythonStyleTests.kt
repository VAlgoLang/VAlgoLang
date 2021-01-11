package com.manimdsl.linearrepresentation

import org.junit.Assert.assertEquals
import org.junit.jupiter.api.Test

class PythonStyleTests {

    @Test
    fun stylesAreMappedToPythonCorrectly() {

        val color = "white"
        val textColor = "black"
        val rectangle = Rectangle("x", "rectangle", "stack1", color = color, textColor = textColor)

        val expected =
            "x = RectangleBlock(\"rectangle\", stack1, color=${color.toUpperCase()}, text_color=${textColor.toUpperCase()})"
        assertEquals(expected, rectangle.toPython()[1])
    }

    @Test
    fun hexadecimalColorsAreMappedCorrectly() {

        val color = "#ffffff"
        val textColor = "#ffffff"
        val rectangle = Rectangle("x", "rectangle", "stack1", color = color, textColor = textColor)

        val expected =
            "x = RectangleBlock(\"rectangle\", stack1, color=\"$color\", text_color=\"$textColor\")"
        assertEquals(expected, rectangle.toPython()[1])
    }

    @Test
    fun invalidTextWeightsAreReturnedToDefault() {

        val rectangle = Rectangle("x", "rectangle", "stack1")

        val expected =
            "x = RectangleBlock(\"rectangle\", stack1)"
        assertEquals(expected, rectangle.toPython()[1])
    }
}
