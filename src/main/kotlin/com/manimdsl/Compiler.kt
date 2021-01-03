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

/**
 * Compile a ManimDSL program, producing python file and/or animation video depending on command-line arguments specified
 *
 * @param filename: Path to input file
 * @param outputVideoFile: Name of output python and/or video files
 * @param generatePython: Whether to generate python and manim file
 * @param onlyGenerateManim: Whether to *only* generate python and manim file
 * @param manimOptions: Options to pass to manim (video quality, whether to show video location in file system, whether to open video once finished)
 * @param stylesheetPath: Path to stylesheet
 * @param boundaries: Whether to print out boundaries of shapes
 */
private fun compile(
    filename: String,
    outputVideoFile: String,
    generatePython: Boolean,
    onlyGenerateManim: Boolean,
    manimOptions: List<String>,
    stylesheetPath: String?,
    boundaries: Boolean
) {
    val file = File(filename)
    /** Check if file path is valid **/
    if (!file.isFile) {
        println("Please enter a valid file name: $filename not found")
        exitProcess(1)
    }

    /** Check if stylesheet path is valid **/
    if (stylesheetPath != null && !File(stylesheetPath).isFile) {
        println("Please enter a valid stylesheet file: $stylesheetPath not found")
        exitProcess(1)
    }

    println("Compiling...")

    /** Parse file to get ANTLR parse tree **/
    val parser = ManimDSLParser(file.inputStream())
    val (syntaxErrorStatus, program) = parser.parseFile()

    /** Throw syntax errors and exit if any exist **/
    if (syntaxErrorStatus != ExitStatus.EXIT_SUCCESS) {
        exitProcess(syntaxErrorStatus.code)
    }

    /** Visit ANTLR parse tree to generate AST **/
    val (semanticErrorStatus, abstractSyntaxTree, symbolTable, lineNodeMap) = parser.convertToAst(program)

    /** Throw semantic errors and exit if any exist **/
    if (semanticErrorStatus != ExitStatus.EXIT_SUCCESS) {
        exitProcess(semanticErrorStatus.code)
    }

    val stylesheet = Stylesheet(stylesheetPath, symbolTable)

    /** Run virtual machine and execute AST to generate linear representation **/
    val (runtimeErrorStatus, manimInstructions) = VirtualMachine(
        abstractSyntaxTree,
        symbolTable,
        lineNodeMap,
        file.readLines(),
        stylesheet,
        boundaries
    ).runProgram()

    if (boundaries) {
        exitProcess(runtimeErrorStatus.code)
    }

    /** Throw runtime errors and exit if any exist **/
    if (runtimeErrorStatus != ExitStatus.EXIT_SUCCESS) {
        exitProcess(runtimeErrorStatus.code)
    }

    /** Code generation into python and manim **/
    val writer = ManimProjectWriter(ManimWriter(manimInstructions).build())

    /** Create python file to be executed **/
    val outputFile = if (generatePython) {
        val pythonOutputFile = outputVideoFile.removeSuffix(".mp4") + ".py"
        println("Writing file to $pythonOutputFile")
        val output = writer.createPythonFile(if (generatePython) pythonOutputFile else null)
        println("File written successfully!")
        output
    } else {
        writer.createPythonFile()
    }

    /** Run manim on python file to produce MP4 video **/
    if (!onlyGenerateManim) {
        println("Generating animation...")
        val exitCode = writer.generateAnimation(outputFile, manimOptions, outputVideoFile)

        if (exitCode != 0) {
            println("Animation could not be generated")
            exitProcess(1)
        }

        println("Animation saved to $outputVideoFile")
    }
}

/**
 * Manim animation quality
 *
 * @constructor Create empty Animation quality
 */
enum class AnimationQuality {
    LOW,
    MEDIUM,
    HIGH;

    override fun toString(): String {
        return this.name.toLowerCase()
    }
}


/**
 * Manim DSL command line arguments
 * */
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
