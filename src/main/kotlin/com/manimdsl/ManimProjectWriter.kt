package com.manimdsl

import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

class ManimProjectWriter(private val pythonCode: String) {

    fun createPythonFile(fileName: String? = null): String {
        return if (fileName !== null) {
            Files.createDirectories(Paths.get(fileName.split("/").dropLast(1).joinToString("")))
            File(fileName).writeText(pythonCode)
            fileName
        } else {
            val tempFile = createTempFile(suffix = ".py")
            tempFile.writeText(pythonCode)
            tempFile.path
        }
    }

    fun generateAnimation(fileName: String, options: List<String>, outputFile: String): Int {
        val uid = UUID.randomUUID().toString()
        val commandOptions = options.joinToString(" ")
        val manimExitCode = ProcessBuilder("manim $fileName Main $commandOptions --media_dir $uid --video_output_dir $uid".split(" "))
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start().waitFor()
        val copyExitCode = ProcessBuilder("cp -f $uid/Main.mp4 $outputFile".split(" "))
            .start().waitFor()
        val removeTempExitCode = ProcessBuilder("rm -rf $uid".split(" "))
            .start().waitFor()
        return copyExitCode + manimExitCode + removeTempExitCode
    }

}