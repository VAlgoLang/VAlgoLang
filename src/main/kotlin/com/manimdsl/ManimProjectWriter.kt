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

    fun generateAnimation(fileName: String, options: List<String>, outputFile: String): Int {
        val commandOptions = options.joinToString("")
        val manimExitCode = ProcessBuilder("manim $fileName Main -$commandOptions --video_output_dir tmp".split(" "))
            .start().waitFor()
        val copyExitCode = ProcessBuilder("cp -f tmp/Main.mp4 $outputFile".split(" "))
            .start().waitFor()
        val removeTempExitCode = ProcessBuilder("rm -rf tmp".split(" "))
            .start().waitFor()
        return copyExitCode + manimExitCode + removeTempExitCode
    }

}