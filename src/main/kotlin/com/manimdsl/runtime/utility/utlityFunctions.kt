package com.manimdsl.runtime.utility
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