package com.manimdsl

import java.io.File

class ManimProjectWriter(private val pythonCode: String) {

    fun createPythonFile(createTempFile: Boolean, fileName: String): String {
        return if(createTempFile) {
            val tempFile = createTempFile(pythonCode)
            tempFile.writeText(pythonCode)
            tempFile.absolutePath
        } else {
            File(fileName).writeText(pythonCode)
            fileName
        }
    }

    fun generateAnimation(fileName: String): Int {
        val process = ProcessBuilder("manim $fileName Main -pl".split(" ")).start()

        return process.waitFor()
    }

}