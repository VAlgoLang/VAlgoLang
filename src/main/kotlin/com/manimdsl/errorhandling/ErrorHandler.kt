package com.manimdsl.errorhandling

import com.manimdsl.ExitStatus

/* Error handler object which stores errors */
object ErrorHandler {
    private val syntaxErrors = arrayListOf<String>()

    private val semanticErrors = arrayListOf<String>()

    private val warnings = arrayListOf<String>()

    fun addSyntaxError(errorEvent: String, linePos: String) {
        syntaxErrors.add("Syntax error at $linePos: $errorEvent")
    }

    fun addSemanticError(errorEvent: String, linePos: String) {
        semanticErrors.add("Semantic error at $linePos: $errorEvent")
    }

    fun addRuntimeError(errorEvent: String, lineNumber: Int) {
        println("Error detected during program execution. Animation could not be generated")
        println("Exit code: ${ExitStatus.RUNTIME_ERROR.code}")
        println("Your program failed at line $lineNumber: $errorEvent")
    }

    fun addTooManyDatastructuresError() {
        println("Too many data structures attempted to be created. Animation could not be generated")
        println("Exit code: ${ExitStatus.RUNTIME_ERROR.code}")
    }

    fun addWarning(warningEvent: String) {
        warnings.add("Warning: $warningEvent")
    }

    fun checkErrorsAndWarnings(): ExitStatus {
        if (syntaxErrors.isNotEmpty()) {
            println(
                "Errors detected during compilation \n" +
                    "Exit code: ${ExitStatus.SYNTAX_ERROR.code}"
            )
            syntaxErrors.forEach { println(it) }
            syntaxErrors.clear()
            return ExitStatus.SYNTAX_ERROR
        }

        if (semanticErrors.isNotEmpty()) {
            println(
                "Errors detected during compilation \n" +
                    "Exit code: ${ExitStatus.SEMANTIC_ERROR.code}"
            )
            semanticErrors.forEach { println(it) }
            semanticErrors.clear()
            return ExitStatus.SEMANTIC_ERROR
        }

        checkWarnings()

        return ExitStatus.EXIT_SUCCESS
    }

    fun checkWarnings() {
        warnings.forEach { println(it) }
        warnings.clear()
    }

    fun printPathError(path: String, linePos: String = "") {
        println(
            "Errors detected during compilation \n" +
                "Exit code: ${ExitStatus.PATH_ERROR.code}"
        )

        if (linePos == "") {
            println("Path Error: File doesn't exist at $path")
        } else {
            println("Path Error at $linePos: Import doesn't exist at $path")
        }
    }
}
