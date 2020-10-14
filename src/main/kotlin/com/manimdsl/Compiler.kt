package com.manimdsl

import com.manimdsl.linearrepresentation.ManimInstr
import picocli.CommandLine
import picocli.CommandLine.*
import java.io.File
import java.util.concurrent.Callable
import kotlin.system.exitProcess

private fun compile(filename: String, output: String?, manimOptions: List<String>) {
    val file = File(filename)
    if (!file.isFile) {
        // File argument was not valid
        println("Please enter a valid file className: ${file.name} not found")
        exitProcess(1)
    }

    println("Compiling...")
    val parser = ManimDSLParser(file.inputStream())
    val (exitStatus, program) = parser.parseFile()

    // Error handling
    if (exitStatus != ExitStatus.EXIT_SUCCESS) {
        exitProcess(exitStatus.code)
    }

    val (abstractSyntaxTree, symbolTable) = parser.convertToAst(program)

    val executor = ASTExecutor(abstractSyntaxTree, symbolTable, file.readLines())

    var state: Pair<Boolean, List<ManimInstr>>
    do {
         state = executor.executeNextStatement()
    } while (!state.first)

    val writer = ManimProjectWriter(ManimWriter(state.second).build())

    if(output !== null) println("Writing file to $output")
    val outputFile = writer.createPythonFile(output)

    println("Generating animation...")
    val exitCode = writer.generateAnimation(outputFile, manimOptions)

    if(exitCode != 0) {
        println("Animation could not be generated")
        exitProcess(1)
    }

    println("Animation Complete!")
}

enum class AnimationQuality {
    LOW,
    HIGH;

    override fun toString(): String {
        return this.name.toLowerCase()
    }
}

@Command(name = "manimdsl", mixinStandardHelpOptions = true, version = ["manimdsl 1.0"], description = ["ManimDSL compiler to produce manim animations"])
class DSLCommandLineArguments : Callable<Int> {

    private val manimArguments = mutableListOf<String>()

    @Parameters(index = "0", description = ["The manimdsl file to compile and animate"])
    lateinit var file: String

    @Option(names = ["-o", "--output"], description = ["Output generated python & manim code (optional)"])
    var output: String? = null

    @Option(names = ["-p", "--preview"], defaultValue = "false", description = ["Preview animation after generation (default: \${DEFAULT-VALUE})"])
    fun preview(preview: Boolean = false) {
        if(preview) manimArguments.add("p")
    }

    @Option(names = ["-q", "--quality"], defaultValue = "low", description = ["Quality of animation. [\${COMPLETION-CANDIDATES}] (default: \${DEFAULT-VALUE})"])
    fun quality(quality: AnimationQuality = AnimationQuality.LOW) {
        if (quality == AnimationQuality.LOW) manimArguments.add("l")
    }

    override fun call(): Int {
        compile(file, output, manimArguments)
        return 0
    }
}

fun main(args: Array<String>) : Unit = exitProcess(CommandLine(DSLCommandLineArguments()).execute(*args))
