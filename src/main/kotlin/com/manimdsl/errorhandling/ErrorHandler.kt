package com.manimdsl.errorhandling

import com.manimdsl.ExitStatus

/**
 * Error handler object which stores errors
 *
 * @constructor Create empty Error handler
 */
object ErrorHandler {
    private val syntaxErrors = arrayListOf<String>()

    private val semanticErrors = arrayListOf<String>()

    private val warnings = arrayListOf<String>()

    /**
     * Add syntax error
     *
     * @param errorEvent
     * @param linePos
     */
    fun addSyntaxError(errorEvent: String, linePos: String) {
        syntaxErrors.add("Syntax error at $linePos: $errorEvent")
    }

    /**
     * Add semantic error
     *
     * @param errorEvent
     * @param linePos
     */
    fun addSemanticError(errorEvent: String, linePos: String) {
        semanticErrors.add("Semantic error at $linePos: $errorEvent")
    }

    /**
     * Add runtime error
     *
     * @param errorEvent
     * @param lineNumber
     */
    fun addRuntimeError(errorEvent: String, lineNumber: Int) {
        println("Error detected during program execution. Animation could not be generated")
        println("Exit code: ${ExitStatus.RUNTIME_ERROR.code}")
        println("Your program failed at line $lineNumber: $errorEvent")
    }

    /**
     * Add too many datastructures error
     *
     */
    fun addTooManyDatastructuresError() {
        println("Too many data structures attempted to be created. Animation could not be generated")
        println("Exit code: ${ExitStatus.RUNTIME_ERROR.code}")
    }

    /**
     * Add warning
     *
     * @param warningEvent
     */
    fun addWarning(warningEvent: String) {
        warnings.add("Warning: $warningEvent")
    }

    /**
     * Check errors and warnings
     *
     * @return exit status [ExitStatus]
     */
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

    /**
     * Check warnings
     *
     */
    fun checkWarnings() {
        warnings.forEach { println(it) }
        warnings.clear()
    }
}
