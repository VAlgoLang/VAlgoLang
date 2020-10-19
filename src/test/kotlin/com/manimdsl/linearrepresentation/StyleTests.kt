package com.manimdsl.linearrepresentation

import com.manimdsl.shapes.Rectangle
import org.junit.Assert.assertEquals
import org.junit.jupiter.api.Test


class StyleTests {

    @Test
    fun stylesAreMappedToPythonCorrectly() {

        val color = "white"
        val textColor = "black"
        val textWeight = "normal"
        val font = "TestFont"
        val rectangle = Rectangle("rectangle", color, textColor, textWeight, font)

        val mobject = NewMObject(rectangle, "x", "codeBlock")

        val expected =
            "x = Rectangle_block(\"rectangle\", color=${color.toUpperCase()}, text_color=${textColor.toUpperCase()}, text_weight=${textWeight.toUpperCase()}, font=\"$font\").build()"
        assertEquals(expected, mobject.toPython()[0])
    }

    @Test
    fun hexadecimalColorsAreMappedCorrectly() {

        val color = "#ffffff"
        val textColor = "#ffffff"
        val rectangle = Rectangle("rectangle", color, textColor)

        val mobject = NewMObject(rectangle, "x", "codeBlock")

        val expected =
            "x = Rectangle_block(\"rectangle\", color=\"$color\", text_color=\"$textColor\").build()"
        assertEquals(expected, mobject.toPython()[0])
    }

    @Test
    fun invalidTextWeightsAreReturnedToDefault() {

        val textWeight = "invalid"
        val rectangle = Rectangle("rectangle", textWeight = textWeight)

        val mobject = NewMObject(rectangle, "x", "codeBlock")

        val expected =
            "x = Rectangle_block(\"rectangle\", text_weight=NORMAL).build()"
        assertEquals(expected, mobject.toPython()[0])
    }
}