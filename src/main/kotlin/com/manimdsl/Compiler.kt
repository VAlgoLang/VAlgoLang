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

    ManimDSLParser(file.inputStream()).parseFile()
}

fun main(args: Array<String>) {

    if (args.isEmpty()) {
        // No argument passed in
        println("Please enter a file name")
        return
    }

    compile(args.first())

}

