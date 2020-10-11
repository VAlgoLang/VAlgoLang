package com.manimdsl.errorHandling

import org.antlr.v4.runtime.*

class SyntaxErrorStrategy : DefaultErrorStrategy() {

    override fun reportFailedPredicate(recognizer: Parser?, e: FailedPredicateException?) {
        val ruleName = recognizer!!.ruleNames[recognizer.context.ruleIndex]
        val offendingToken = recognizer.context.text
        val offendingTokenChar = recognizer.context.getStart().charPositionInLine
        val offendingTokenLine = recognizer.context.getStart().line
        val msg = "rule " + ruleName + " " + e!!.message
        recognizer.notifyErrorListeners(
            e.offendingToken,
            msg + "TOKEN${offendingToken}LINE${offendingTokenLine}CHAR${offendingTokenChar}",
            e
        )
    }

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

    override fun reportUnwantedToken(recognizer: Parser?) {
        if (!inErrorRecoveryMode(recognizer)) {
            beginErrorCondition(recognizer)
            val t = recognizer!!.currentToken
            val tokenName = getTokenErrorDisplay(t)
            val msg = "extraneous input $tokenName"
            recognizer.notifyErrorListeners(t, msg, null as RecognitionException?)
        }
    }

    override fun reportMissingToken(recognizer: Parser?) {
        if (!inErrorRecoveryMode(recognizer)) {
            beginErrorCondition(recognizer)
            val t = recognizer!!.currentToken
            val expecting = getExpectedTokens(recognizer)
            val expectedToken = expecting.toString(recognizer.vocabulary).trim('\'')
            val actualToken = getTokenErrorDisplay(t).trim('\'')
            var msg = "missing '$expectedToken' at ''$actualToken'"
            if (expectedToken.toLowerCase().contains(actualToken.toLowerCase())
                || actualToken.toLowerCase().contains(expectedToken.toLowerCase())
            ) {
                msg = "potential spelling error, did you mean: '$expectedToken'"
            }
            recognizer.notifyErrorListeners(t, msg, null as RecognitionException?)
        }
    }

    override fun reportInputMismatch(recognizer: Parser, e: InputMismatchException) {
        val expecting =
            if (e.expectedTokens.size() > 1) "" else " expecting " + e.expectedTokens.toString(recognizer.vocabulary)
        val msg = "mismatched input " + getTokenErrorDisplay(e.offendingToken) + expecting
        recognizer.notifyErrorListeners(e.offendingToken, msg, e)
    }

}