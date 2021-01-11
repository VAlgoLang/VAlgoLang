package com.valgolang.errorhandling.syntaxerror

import org.antlr.runtime.UnwantedTokenException
import org.antlr.v4.runtime.*

/**
 * Syntax error listener
 *
 * Custom Syntax error listener that throws custom messages.
 *
 * @constructor Create empty Syntax error listener
 */
class SyntaxErrorListener : BaseErrorListener() {

    /** Map to convert tokens to readable format.
     * Please update when grammar updates **/
    private val readableTokenMap = mapOf(
        "IDENT" to "identifier",
        "NUMBER_TYPE" to "number",
    )

    /**
     * Syntax error
     *
     * Syntax error to throw.
     *
     * @param recognizer: Token recognizer
     * @param offendingSymbol: Offending symbol
     * @param line: Error line
     * @param charPositionInLine: Error char position in error line
     * @param msg: Error message
     * @param e: Exception
     */
    override fun syntaxError(
        recognizer: Recognizer<*, *>,
        offendingSymbol: Any,
        line: Int,
        charPositionInLine: Int,
        msg: String,
        e: RecognitionException?
    ) {

        val token = offendingSymbol as Token
        val underlinedError = underlineError(recognizer, offendingSymbol, line, charPositionInLine)

        when (e) {
            is UnwantedTokenException -> {
                extraneousInputError(token.text, line, charPositionInLine, underlinedError)
            }
            else -> {
                otherError(makeReadable(msg), token.text, line, charPositionInLine, underlinedError)
            }
        }
    }

    /**
     * Underline error
     *
     * Underlines whereabouts in input file the error has occured.
     *
     * @param recognizer: Token recognizer
     * @param offendingSymbol: Offending symbol
     * @param line: Error line
     * @param charPositionInLine: Error char position in error line
     * @return
     */
    private fun underlineError(
        recognizer: Recognizer<*, *>,
        offendingSymbol: Token,
        line: Int,
        charPositionInLine: Int
    ): String {
        var errorLine = getErrorLine(recognizer, line, charPositionInLine)
        val start = offendingSymbol.startIndex
        val end = offendingSymbol.stopIndex
        if (start in 0..end) {
            for (i in start..end) {
                errorLine += "^"
            }
        } else if (end == 0) {
            errorLine += "^"
        }
        return errorLine
    }

    /**
     * Get error line
     *
     * Gets the line of code error had occurred on from input file.
     *
     * @param recognizer: Token recognizer
     * @param line: Error line
     * @param charPositionInLine: Error char position in error line
     * @return
     */
    private fun getErrorLine(recognizer: Recognizer<*, *>, line: Int, charPositionInLine: Int): String {
        val tokens = recognizer.inputStream as CommonTokenStream
        val input = tokens.tokenSource.inputStream.toString()
        val lines = input.split("\n")
        var errorLine = lines[line - 1] + "\n"
        for (i in 1..charPositionInLine) {
            errorLine += " "
        }
        return errorLine
    }

    /**
     * Make readable
     *
     * Replaces tokens with more meaningful equivalents
     *
     * @param message: Error message
     * @return
     */
    private fun makeReadable(message: String): String {
        var readableMessage = message
        readableTokenMap.forEach { (token, readable) ->
            readableMessage = message.replace(token, readable, false)
        }
        return readableMessage
    }
}
