package com.manimdsl.errorhandling

import com.manimdsl.ExitStatus

/* Error handler object which stores errors */
object ErrorHandler {
    private val syntaxErrors = arrayListOf<String>()

    private val semanticErrors = arrayListOf<String>()

    fun addSyntaxError(errorEvent: String, linePos: String) {
        syntaxErrors.add("Syntax error at $linePos: $errorEvent")
    }

    fun addSemanticError(errorEvent: String, linePos: String) {
        semanticErrors.add("Semantic error at $linePos: $errorEvent")
    }

    fun checkErrors(): ExitStatus {
        if (syntaxErrors.isNotEmpty()) {
            println(
                "Errors detected during compilation \n" +
                        "Exit code: ${ExitStatus.SYNTAX_ERROR.code}"
            )
            syntaxErrors.map { println(it) }
            syntaxErrors.clear()
            return ExitStatus.SYNTAX_ERROR
        }

        if (semanticErrors.isNotEmpty()) {
            println(
                "Errors detected during compilation \n" +
                        "Exit code: ${ExitStatus.SEMANTIC_ERROR.code}"
            )
            semanticErrors.map { println(it) }
            semanticErrors.clear()
            return ExitStatus.SEMANTIC_ERROR
        }

        return ExitStatus.EXIT_SUCCESS
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

