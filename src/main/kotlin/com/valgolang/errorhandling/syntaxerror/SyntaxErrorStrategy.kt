package com.valgolang.errorhandling.syntaxerror

import org.antlr.v4.runtime.*

/**
 * Syntax error strategy
 *
 * Custom syntax error strategy to override default ANTLR syntax errors.
 *
 * @constructor Create empty Syntax error strategy
 */
class SyntaxErrorStrategy : DefaultErrorStrategy() {

    /**
     * Report failed predicate
     *
     * General error thrown by ANTLR when a parser rule has not matched fully. Has been overridden to send
     * additional information to be used to underline offending token in SyntaxErrorListener.
     *
     * @param recognizer: Parser recognizer
     * @param e: Error
     */
    override fun reportFailedPredicate(recognizer: Parser?, e: FailedPredicateException?) {
        val ruleName = recognizer!!.ruleNames[recognizer.context.ruleIndex]
        val offendingToken = recognizer.context.text
        val offendingTokenChar = recognizer.context.getStart().charPositionInLine
        val offendingTokenLine = recognizer.context.getStart().line
        val msg = "rule " + ruleName + " " + e!!.message
        recognizer.notifyErrorListeners(
            e.offendingToken,
            msg + "TOKEN${offendingToken}LINE${offendingTokenLine}CHAR$offendingTokenChar",
            e
        )
    }

    /**
     * Report no viable alternative
     *
     * No viable alternative error in parser. Enriches message with string value rather than token.
     *
     * @param recognizer: Parser recognizer
     * @param e: Error
     */
    override fun reportNoViableAlternative(recognizer: Parser?, e: NoViableAltException?) {
        val tokens = recognizer!!.inputStream
        val input: String
        input = if (tokens != null) {
            if (e!!.startToken.type == -1) {
                "<EOF>"
            } else {
                e.startToken.text + " " + e.offendingToken.text
            }
        } else {
            "<unknown input>"
        }

        val msg = "missing token at input ${this.escapeWSAndQuote(input)}"
        recognizer.notifyErrorListeners(e!!.offendingToken, msg, e)
    }

    /**
     * Report unwanted token
     *
     * Extraneous input error.
     *
     * @param recognizer: Parser recognizer
     */
    override fun reportUnwantedToken(recognizer: Parser?) {
        if (!inErrorRecoveryMode(recognizer)) {
            beginErrorCondition(recognizer)
            val t = recognizer!!.currentToken
            val tokenName = getTokenErrorDisplay(t)
            val msg = "extraneous input $tokenName"
            recognizer.notifyErrorListeners(t, msg, null as RecognitionException?)
        }
    }

    /**
     * Report missing token
     *
     * Attempts to offer suggestions on general missing token errors
     *
     * @param recognizer: Parser recognizer
     */
    override fun reportMissingToken(recognizer: Parser?) {
        if (!inErrorRecoveryMode(recognizer)) {
            beginErrorCondition(recognizer)
            val t = recognizer!!.currentToken
            val expecting = getExpectedTokens(recognizer)
            val expectedToken = expecting.toString(recognizer.vocabulary).trim('\'')
            val actualToken = getTokenErrorDisplay(t).trim('\'')
            var msg = "missing '$expectedToken' at ''$actualToken'"
            if (expectedToken.toLowerCase().contains(actualToken.toLowerCase()) ||
                actualToken.toLowerCase().contains(expectedToken.toLowerCase())
            ) {
                msg = "potential spelling error, did you mean: '$expectedToken'"
            }
            recognizer.notifyErrorListeners(t, msg, null as RecognitionException?)
        }
    }

    /**
     * Report input mismatch
     *
     * Shows expected tokens on input mismatch
     *
     * @param recognizer: Parser recognizer
     * @param e: Error
     */
    override fun reportInputMismatch(recognizer: Parser, e: InputMismatchException) {
        val expecting = " expecting " + e.expectedTokens.toString(recognizer.vocabulary)
        val msg = "mismatched input " + getTokenErrorDisplay(e.offendingToken) + expecting
        recognizer.notifyErrorListeners(e.offendingToken, msg, e)
    }
}
