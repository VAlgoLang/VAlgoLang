package com.manimdsl

import antlr.ManimLexer
import antlr.ManimParser
import com.manimdsl.frontend.ProgramNode
import com.manimdsl.errorhandling.ErrorHandler
import com.manimdsl.errorhandling.syntaxerror.SyntaxErrorListener
import com.manimdsl.errorhandling.syntaxerror.SyntaxErrorStrategy
import com.manimdsl.frontend.SymbolTable
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.atn.PredictionMode
import java.io.InputStream

val SYNTAX_ERROR = 1;

/* Exit status codes */
enum class ExitStatus(val code: Int) {
    EXIT_SUCCESS(0),
    SYNTAX_ERROR(100),
    SEMANTIC_ERROR(200),
    PATH_ERROR(101)
}

class ManimDSLParser(private val input: InputStream) {

    // Build ANTLR Parse Tree and if Syntax Errors found, throw them and exit
    fun parseFile(): Pair<ExitStatus, ManimParser.ProgramContext> {
        val input = CharStreams.fromStream(input)
        // Lexical analysis
        val lexer = ManimLexer(input)
        lexer.removeErrorListeners()

        // Syntax analysis
        val tokens = CommonTokenStream(lexer)
        val parser = ManimParser(tokens)
        // Speeds up parser with no backtracking as our grammar is quite simple
        parser.interpreter.predictionMode = PredictionMode.SLL
        parser.errorHandler = SyntaxErrorStrategy()
        parser.removeErrorListeners()
        parser.addErrorListener(SyntaxErrorListener())
//        parser.addErrorListener(SyntaxErrorListener())
        val program = parser.program()
        return Pair(ErrorHandler.checkErrors(), program)
    }

    fun convertToAst(program: ManimParser.ProgramContext): Triple<ExitStatus, ProgramNode, SymbolTable> {
        val visitor = ManimParserVisitor()
        val ast = visitor.visitProgram(program)
        return Triple(ErrorHandler.checkErrors(), ast, visitor.currentSymbolTable)
    }
}
