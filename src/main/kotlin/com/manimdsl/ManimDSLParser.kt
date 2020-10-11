package com.manimdsl

import antlr.ManimLexer
import antlr.ManimParser
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.atn.PredictionMode
import java.io.InputStream
import kotlin.system.exitProcess

val SYNTAX_ERROR = 1;

class ManimDSLParser(private val input: InputStream) {

    // Build ANTLR Parse Tree and if Syntax Errors found, throw them and exit
    fun parseFile(): ProgramNode {
        val input = CharStreams.fromStream(input)
        val lexer = ManimLexer(input)
        lexer.removeErrorListeners()
        val tokens = CommonTokenStream(lexer)
        val parser = ManimParser(tokens)
        // Speeds up parser with no backtracking as our grammar is quite simple
        parser.interpreter.predictionMode = PredictionMode.SLL
        parser.removeErrorListeners()
//        parser.addErrorListener(SyntaxErrorListener())
        val program = parser.program()
        if (parser.numberOfSyntaxErrors > 0) {
            exitProcess(SYNTAX_ERROR)
        }
        return convertToAst(program)
    }

    private fun convertToAst(program: ManimParser.ProgramContext): ProgramNode {
        val visitor = ManimParserVisitor()
        return visitor.visitProgram(program)
    }

}
