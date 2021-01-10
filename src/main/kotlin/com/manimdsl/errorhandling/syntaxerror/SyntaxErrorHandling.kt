package com.manimdsl.errorhandling.syntaxerror

import com.manimdsl.errorhandling.ErrorHandler

/**
 * Extraneous input error
 *
 * Error message for extraneous input
 *
 * @param token: Offending token
 * @param line: Error line
 * @param char: Error character
 * @param underlinedError: Underline
 */
fun extraneousInputError(token: String, line: Int, char: Int, underlinedError: String) {
    ErrorHandler.addSyntaxError("extraneous input '$token'\n$underlinedError", "$line:$char")
}

/**
 * Other error
 *
 * General error message
 *
 * @param msg: Error message
 * @param token: Offending token
 * @param line: Error line
 * @param char: Error character
 * @param underlinedError: Underline
 */
fun otherError(msg: String, token: String, line: Int, char: Int, underlinedError: String) {
    println(token)
    ErrorHandler.addSyntaxError("$msg\n$underlinedError", "$line:$char")
}
