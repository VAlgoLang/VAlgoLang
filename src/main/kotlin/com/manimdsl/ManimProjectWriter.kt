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

    fun generateAnimation(fileName: String, preview: List<String>): Int {
        val commandOptions = preview.joinToString("")
        return ProcessBuilder("manim $fileName Main -$commandOptions".split(" "))
            .start().waitFor()
    }

}