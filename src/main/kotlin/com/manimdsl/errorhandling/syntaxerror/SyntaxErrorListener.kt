package com.manimdsl.errorhandling.syntaxerror

import org.antlr.runtime.UnwantedTokenException
import org.antlr.v4.runtime.*

class SyntaxErrorListener : BaseErrorListener() {

    override fun syntaxError(
        recognizer: Recognizer<*, *>, offendingSymbol: Any,
        line: Int, charPositionInLine: Int, msg: String,
        e: RecognitionException?
    ) {

        val token = offendingSymbol as Token
        var underlinedError = underlineError(recognizer, offendingSymbol, line, charPositionInLine)

        when (e) {
            is FailedPredicateException -> {
                val overflowInfo = getOverflowInfo(msg)
                underlinedError =
                    underlineCharOverflow(recognizer, overflowInfo.token, overflowInfo.line, overflowInfo.char)
                overflowError(overflowInfo, underlinedError)
            }
            is UnwantedTokenException -> {
                extraneousInputError(token.text, line, charPositionInLine, underlinedError)
            }
            else -> {
                otherError(makeReadable(msg), token.text, line, charPositionInLine, underlinedError)
            }
        }
    }

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

    private fun underlineCharOverflow(
        recognizer: Recognizer<*, *>,
        offendingSymbol: String,
        line: Int,
        charPositionInLine: Int
    ): String {
        var errorLine = getErrorLine(recognizer, line, charPositionInLine)
        for (i in offendingSymbol.indices) {
            errorLine += if (!isASCII(offendingSymbol[i])) {
                "^"
            } else {
                " "
            }
        }
        return errorLine
    }

    private fun isASCII(c: Char): Boolean {
        return c >= 0.toChar() && c < 128.toChar()
    }

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

    private fun makeReadable(message: String): String {
        return message.replace("IDENT", "identifier", false)
            .replace("UNSIGNED", "integer", false)
            .replace("STR_LITER", "string", false)
            .replace("CHAR_LITER", "character", false)
    }

    data class OverflowInfo(val type: String, val token: String, val line: Int, val char: Int)

    private fun getOverflowInfo(errorInfo: String): OverflowInfo {
        val type = if (errorInfo.contains("char")) "character" else "integer"
        val extraInfo = errorInfo.split("TOKEN")[1].split("LINE")
        val text = extraInfo[0]
        val tokenLineInfo = extraInfo[1].split("CHAR")
        val line = Integer.parseInt(tokenLineInfo[0])
        val char = Integer.parseInt(tokenLineInfo[1])
        return OverflowInfo(type, text, line, char)
    }
}



