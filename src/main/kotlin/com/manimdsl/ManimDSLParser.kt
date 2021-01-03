package com.manimdsl

import antlr.ManimLexer
import antlr.ManimParser
import com.manimdsl.errorhandling.ErrorHandler
import com.manimdsl.errorhandling.syntaxerror.SyntaxErrorListener
import com.manimdsl.errorhandling.syntaxerror.SyntaxErrorStrategy
import com.manimdsl.frontend.ManimParserVisitor
import com.manimdsl.frontend.ProgramNode
import com.manimdsl.frontend.StatementNode
import com.manimdsl.frontend.SymbolTableVisitor
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.atn.PredictionMode
import java.io.InputStream

/**
 * Exit status codes
 *
 * @property code
 * @constructor Create empty Exit status
 */
enum class ExitStatus(val code: Int) {
    EXIT_SUCCESS(0),
    SYNTAX_ERROR(100),
    SEMANTIC_ERROR(200),
    RUNTIME_ERROR(300),
    PATH_ERROR(101)
}

/**
 * Manim DSL parser
 *
 * @property input: file to be parsed as an input stream of bytes
 * @constructor Create empty Manim DSL parser
 */
class ManimDSLParser(private val input: InputStream) {

    /**
     * Parse file to build ANTLR parse tree and find any syntax errors
     *
     * @return pair of exit status and parse tree
     */
    fun parseFile(): Pair<ExitStatus, ManimParser.ProgramContext> {
        val input = CharStreams.fromStream(input)

        /** Lexical analysis **/
        val lexer = ManimLexer(input)
        lexer.removeErrorListeners()

        /** Syntax analysis **/
        val tokens = CommonTokenStream(lexer)
        val parser = ManimParser(tokens)

        /** Speeds up parser with no backtracking as DSL grammar is quite simple **/
        parser.interpreter.predictionMode = PredictionMode.SLL
        parser.errorHandler = SyntaxErrorStrategy()
        parser.removeErrorListeners()
        parser.addErrorListener(SyntaxErrorListener())
        val program = parser.program()
        return Pair(ErrorHandler.checkErrorsAndWarnings(), program)
    }

    /**
     * Convert ANTLR parse tree to AST
     *
     * @param program: ANTLR parse tree
     * @return a [ParserResult] containing exit code, AST, symbol table and line number node map
     */
    fun convertToAst(program: ManimParser.ProgramContext): ParserResult {
        val visitor = ManimParserVisitor()
        val ast = visitor.visitProgram(program)
        return ParserResult(ErrorHandler.checkErrorsAndWarnings(), ast, visitor.symbolTable, visitor.lineNumberNodeMap)
    }
}

data class ParserResult(val exitStatus: ExitStatus, val abstractSyntaxTree: ProgramNode, val symbolTableVisitor: SymbolTableVisitor, val lineNodeMap: MutableMap<Int, StatementNode>)
