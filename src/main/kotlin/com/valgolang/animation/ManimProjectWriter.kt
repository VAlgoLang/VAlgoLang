package com.valgolang.animation

import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

/**
 * Writer that produces the output animation video and/or the python file
 *
 * @property pythonCode: string containing all the python code that generates the animation
 * @constructor Creates a new Manim project writer
 */
class ManimProjectWriter(private val pythonCode: String) {

    /**
     * Creates and writes the python code generated to a python file if input fileName is provided,
     * otherwise creates and writes to a temporary file
     *
     * @param fileName
     * @return name of output file or the path to the temporary file
     */
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

    /**
     * Generates animation using the python file created beforehand
     *
     * @param fileName: name of python file to be executed
     * @param options: CLI options for generating manim animation, such as quality
     * @param outputFile: name of output mp4 file
     * @return exit code from generating animation and writing it to output mp4
     */
    fun generateAnimation(fileName: String, options: List<String>, outputFile: String): Int {
        Files.createDirectories(Paths.get(outputFile.split("/").dropLast(1).joinToString("")))
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
