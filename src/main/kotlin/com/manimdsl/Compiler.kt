package com.manimdsl

import com.manimdsl.animation.ManimProjectWriter
import com.manimdsl.animation.ManimWriter
import com.manimdsl.runtime.VirtualMachine
import com.manimdsl.stylesheet.Stylesheet
import picocli.CommandLine
import picocli.CommandLine.*
import java.io.File
import java.util.concurrent.Callable
import kotlin.system.exitProcess

private fun compile(filename: String, outputVideoFile: String, generatePython: Boolean, onlyGenerateManim: Boolean, manimOptions: List<String>, stylesheetPath: String?, boundaries: Boolean) {
    val file = File(filename)
    if (!file.isFile) {
        // File argument was not valid
        println("Please enter a valid file name: $filename not found")
        exitProcess(1)
    }

    if (stylesheetPath != null && !File(stylesheetPath).isFile) {
        // Stylesheet argument was not valid
        println("Please enter a valid stylesheet file: $stylesheetPath not found")
        exitProcess(1)
    }

    println("Compiling...")
    val parser = ManimDSLParser(file.inputStream())
    val (syntaxErrorStatus, program) = parser.parseFile()

    // Error handling
    if (syntaxErrorStatus != ExitStatus.EXIT_SUCCESS) {
        exitProcess(syntaxErrorStatus.code)
    }

    val (semanticErrorStatus, abstractSyntaxTree, symbolTable, lineNodeMap) = parser.convertToAst(program)
    // Error handling
    if (semanticErrorStatus != ExitStatus.EXIT_SUCCESS) {
        exitProcess(semanticErrorStatus.code)
    }
    val stylesheet = Stylesheet(stylesheetPath, symbolTable)

    val (runtimeErrorStatus, manimInstructions) = VirtualMachine(abstractSyntaxTree, symbolTable, lineNodeMap, file.readLines(), stylesheet, boundaries).runProgram()

    if (boundaries) {
        exitProcess(runtimeErrorStatus.code)
    }

    if (runtimeErrorStatus != ExitStatus.EXIT_SUCCESS) {
        exitProcess(runtimeErrorStatus.code)
    }

    val writer = ManimProjectWriter(ManimWriter(manimInstructions).build())

    val outputFile = if (generatePython) {
        val pythonOutputFile = outputVideoFile.removeSuffix(".mp4") + ".py"
        println("Writing file to $pythonOutputFile")
        val output = writer.createPythonFile(if (generatePython) pythonOutputFile else null)
        println("File written successfully!")
        output
    } else {
        writer.createPythonFile()
    }

    if (!onlyGenerateManim) {
        println("Generating animation...")
        val exitCode = writer.generateAnimation(outputFile, manimOptions, outputVideoFile)

        if (exitCode != 0) {
            println("Animation could not be generated")
            exitProcess(1)
        }

        println("Animation Complete!")
    }
}

enum class AnimationQuality {
    LOW,
    MEDIUM,
    HIGH;

    override fun toString(): String {
        return this.name.toLowerCase()
    }
}

@Command(
    name = "manimdsl",
    mixinStandardHelpOptions = true,
    version = ["manimdsl 1.0"],
    description = ["ManimDSL compiler to produce manim animations."]
)
class DSLCommandLineArguments : Callable<Int> {

    private val manimArguments = mutableListOf<String>()

    @Parameters(index = "0", description = ["The manimdsl file to compile and animate."])
    lateinit var file: String

    @Option(names = ["-o", "--output"], description = ["The animated mp4 file location (default: \${DEFAULT-VALUE})."])
    var output: String = "out.mp4"

    @Option(names = ["-s", "--stylesheet"], description = ["The JSON stylesheet associated with your code"])
    var stylesheet: String? = null

    @Option(names = ["-p", "--python"], description = ["Output generated python & manim code (optional)."])
    var python: Boolean = false

    @Option(names = ["-m", "--manim"], description = ["Only output generated python & manim code (optional)."])
    var manim: Boolean = false

    @Option(names = ["-b", "--boundaries"], description = ["Print out boundaries of shapes"], hidden = true)
    var boundaries: Boolean = false

    @Option(
        names = ["-q", "--quality"],
        defaultValue = "low",
        description = ["Quality of animation. [\${COMPLETION-CANDIDATES}] (default: \${DEFAULT-VALUE})."]
    )
    fun quality(quality: AnimationQuality = AnimationQuality.LOW) {
        when (quality) {
            AnimationQuality.LOW -> manimArguments.add("-l")
            AnimationQuality.HIGH -> manimArguments.add("--high_quality")
            AnimationQuality.MEDIUM -> manimArguments.add("-m")
        }
    }

    @Option(names = ["-f", "--open_file"], description = ["Show the output file in file manager (optional)."])
    fun open_file(showFile: Boolean = false) {
        if (showFile) manimArguments.add("-f")
    }

    @Option(names = ["--preview"], description = ["Automatically open the saved file once its done (optional)."])
    fun preview(open_file: Boolean = false) {
        if (open_file) manimArguments.add("-p")
    }

    override fun call(): Int {
        compile(file, output, python, manim, manimArguments, stylesheet, boundaries)
        return 0
    }
}

fun main(args: Array<String>): Unit = exitProcess(CommandLine(DSLCommandLineArguments()).execute(*args))
