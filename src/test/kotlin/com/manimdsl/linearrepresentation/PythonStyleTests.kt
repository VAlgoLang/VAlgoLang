package com.manimdsl.linearrepresentation

import com.manimdsl.shapes.Rectangle
import org.junit.Assert.assertEquals
import org.junit.jupiter.api.Test


class PythonStyleTests {

    @Test
    fun stylesAreMappedToPythonCorrectly() {

        val color = "white"
        val textColor = "black"
        val textWeight = "normal"
        val font = "TestFont"
        val rectangle = Rectangle("x", "rectangle", color, textColor, textWeight, font)

        val mobject = NewMObject(rectangle, "codeBlock")

        val expected =
            "x = Rectangle_block(\"rectangle\", color=${color.toUpperCase()}, text_color=${textColor.toUpperCase()}, text_weight=${textWeight.toUpperCase()}, font=\"$font\")"
        assertEquals(expected, mobject.toPython()[0])
    }

    @Test
    fun hexadecimalColorsAreMappedCorrectly() {

        val color = "#ffffff"
        val textColor = "#ffffff"
        val rectangle = Rectangle("x", "rectangle", color, textColor)

        val mobject = NewMObject(rectangle, "codeBlock")

        val expected =
            "x = Rectangle_block(\"rectangle\", color=\"$color\", text_color=\"$textColor\")"
        assertEquals(expected, mobject.toPython()[0])
    }

    @Test
    fun invalidTextWeightsAreReturnedToDefault() {

        val textWeight = "invalid"
        val rectangle = Rectangle("x", "rectangle", textWeight = textWeight)

        val mobject = NewMObject(rectangle, "codeBlock")

        val expected =
            "x = Rectangle_block(\"rectangle\", text_weight=NORMAL)"
        assertEquals(expected, mobject.toPython()[0])
    }
}