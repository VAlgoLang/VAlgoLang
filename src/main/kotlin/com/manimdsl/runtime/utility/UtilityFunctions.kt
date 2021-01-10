package com.manimdsl.runtime.utility

import com.manimdsl.frontend.*
import com.manimdsl.linearrepresentation.DataStructureMObject
import com.manimdsl.runtime.*
import com.manimdsl.stylesheet.PositionProperties

private const val WRAP_LINE_LENGTH = 50

/**
 * Wrap code lines to prevent overly long lines during rendering.
 *
 * @param code The code being formatted as an array of lines.
 * @return 2D list of strings with each inner list corresponding to one original broken up line.
 * Flattened output corresponds to original unwrapped code.
 *
 */

fun wrapCode(code: MutableList<String>): MutableList<MutableList<String>> {
    val wrappedCode = mutableListOf<MutableList<String>>()
    for (line in code) {
        val wrappedLine = mutableListOf<String>()
        var index = 0
        var prevIndex = 0
        while (index < line.length) {
            index = wrapLine(line.substring(index, line.length)) + prevIndex
            wrappedLine.add(line.substring(prevIndex, index))
            prevIndex = index
        }
        wrappedCode.add(wrappedLine)
    }
    return wrappedCode
}

/**
 * Wrap individual code line.
 *
 * @param line
 * @return Length of the line of code that can be safely rendered.
 *
 */

fun wrapLine(line: String): Int {
    val list = line.split(" ", ",")
    var counter = 0
    var prevCounter = 0
    for (word in list) {
        counter += word.length + 1
        if (counter >= WRAP_LINE_LENGTH) {
            return prevCounter
        }
        prevCounter = counter
    }
    return line.length
}

/**
 * Gets boundaries as list of coordinates based on position properties.
 *
 * @param position The position properties from the stylesheet.
 * @return List of top left to bottom right coordinates based on [position].
 *
 */

fun getBoundaries(position: PositionProperties?): List<Pair<Double, Double>> {
    val boundaries = mutableListOf<Pair<Double, Double>>()
    if (position != null) {
        val left = position.x
        val right = left + position.width
        val bottom = position.y
        val top = bottom + position.height
        boundaries.addAll(listOf(Pair(left, top), Pair(right, top), Pair(left, bottom), Pair(right, bottom)))
    }
    return boundaries
}

/**
 * Formats [text] with \n insertions so each line is less than [max_length].
 *
 * @param text
 * @param max_length
 * @return Formatted [text]
 *
 */

fun wrapString(text: String, max_length: Int = WRAP_LINE_LENGTH): String {
    val sb = StringBuilder(text)
    for (index in max_length until text.length step max_length)
        sb.insert(index, "\\n")
    return sb.toString()
}

/**
 * Utility to construct some basic expression nodes back from corresponding execution values.
 *
 * @param value
 * @param lineNumber
 * @return ExpressionNode.
 *
 */

fun makeExpressionNode(value: ExecValue, lineNumber: Int): ExpressionNode {
    return when (value) {
        is CharValue -> CharNode(lineNumber, value.value)
        is DoubleValue -> NumberNode(lineNumber, value.value)
        is BoolValue -> BoolNode(lineNumber, value.value)
        is StringValue -> StringNode(lineNumber, value.value)
        else -> VoidNode(lineNumber)
    }
}

fun convertToIdent(dataStructureVariable: MutableSet<String>, variables: MutableMap<String, ExecValue>): MutableSet<String> {
    val idents = dataStructureVariable.map {
        if (it.contains('.')) {
            it.substringAfter('.')
        } else {
            it
        }
    }.map {
        (variables[it]!!.manimObject as DataStructureMObject).ident
    }
    dataStructureVariable.forEach {
        variables[it] = EmptyValue
    }
    return idents.toMutableSet()
}
