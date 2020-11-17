package com.manimdsl.runtime.utility

import com.manimdsl.stylesheet.PositionProperties

private val WRAP_LINE_LENGTH = 50

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

fun wrapLine(line : String): Int{
    val list = line.split(" ")
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