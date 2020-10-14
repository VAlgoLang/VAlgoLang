package com.manimdsl

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

    val executor = ASTExecutor(abstractSyntaxTree)

    do {
        val state = executor.executeNextStatement()
        // TODO: Replace with conversion to IR
        println(state)
    } while (!state.first)

}

fun main(args: Array<String>) {

    if (args.isEmpty()) {
        // No argument passed in
        println("Please enter a file name")
        return
    }

    compile(args.first())

}

