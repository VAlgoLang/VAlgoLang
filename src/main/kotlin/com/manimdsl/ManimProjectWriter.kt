package com.manimdsl

import java.io.File

class ManimProjectWriter(private val pythonCode: String) {

    fun createPythonFile(fileName: String? = null): String {
        return if (fileName !== null) {
            File(fileName).writeText(pythonCode)
            fileName
        } else {
            val tempFile = createTempFile(suffix = ".py")
            tempFile.writeText(pythonCode)
            tempFile.path
        }
    }

    fun generateAnimation(fileName: String): Int {
        println(fileName)
        val process = ProcessBuilder("manim $fileName Main -pl".split(" ")).start()

        return process.waitFor()
    }

}