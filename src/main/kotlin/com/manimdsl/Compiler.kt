package com.manimdsl

import com.manimdsl.linearrepresentation.ManimInstr
import java.io.File
import kotlin.system.exitProcess

private fun compile(filename: String) {
    val file = File(filename)
    if (!file.isFile) {
        // File argument was not valid
        println("Please enter a valid file className: ${file.name} not found")
        exitProcess(1)
    }

    println("Compiling...")
    val parser = ManimDSLParser(file.inputStream())
    val (syntaxErrorStatus, program) = parser.parseFile()

    // Error handling
    if (syntaxErrorStatus != ExitStatus.EXIT_SUCCESS) {
        exitProcess(syntaxErrorStatus.code)
    }

    val (semanticErrorStatus, abstractSyntaxTree, symbolTable) = parser.convertToAst(program)
    // Error handling
    if (semanticErrorStatus != ExitStatus.EXIT_SUCCESS) {
        exitProcess(semanticErrorStatus.code)
    }

    val executor = ASTExecutor(abstractSyntaxTree, symbolTable, file.readLines())

    var state: Pair<Boolean, List<ManimInstr>>

    do {
         state = executor.executeNextStatement()
    } while (!state.first)

    val writer = ManimProjectWriter(ManimWriter(state.second).build())

    writer.createPythonFile("test.py")
    writer.generateAnimation("test.py")

    println("Animation Complete!")
}

fun main(args: Array<String>) {

    if (args.isEmpty()) {
        // No argument passed in
        println("Please enter a file name")
        return
    }

    compile(args.first())
}

