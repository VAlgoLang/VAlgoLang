package com.manimdsl.errorhandling.syntaxerror

import com.manimdsl.errorhandling.ErrorHandler

fun overflowError(info: SyntaxErrorListener.OverflowInfo, underlinedError: String) {
    ErrorHandler.addSyntaxError("${info.type} overflow\n$underlinedError", "${info.line}:${info.char}")
}

fun extraneousInputError(token: String, line: Int, char: Int, underlinedError: String) {
    ErrorHandler.addSyntaxError("extraneous input '$token'\n$underlinedError", "$line:$char")
}

fun otherError(msg: String, token: String, line: Int, char: Int, underlinedError: String) {
    println(token)
    ErrorHandler.addSyntaxError("$msg\n$underlinedError", "$line:$char")
}
