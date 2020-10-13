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
    val (exitStatus, program) = parser.parseFile()

    // Error handling
    if (exitStatus != ExitStatus.EXIT_SUCCESS) {
        exitProcess(exitStatus.code)
    }

    val (abstractSyntaxTree, symbolTable) = parser.convertToAst(program)
    println(abstractSyntaxTree)
    println(symbolTable)

    val executor = ASTExecutor(abstractSyntaxTree, symbolTable, file.readLines())

    var state: Pair<Boolean, List<ManimInstr>>
    do {
         state = executor.executeNextStatement()
        // TODO: Replace with conversion to IR
        println(state)
    } while (!state.first)
    val writer = ManimProjectWriter(ManimWriter(state.second).build())
    writer.createPythonFile("test23.py")
    writer.generateAnimation("test23.py")
    println("done")
}

fun main(args: Array<String>) {

    if (args.isEmpty()) {
        // No argument passed in
        println("Please enter a file name")
        return
    }

    compile(args.first())
}

