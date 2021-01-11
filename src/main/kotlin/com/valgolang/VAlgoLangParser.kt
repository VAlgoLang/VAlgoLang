package com.valgolang

import antlr.VAlgoLangLexer
import antlr.VAlgoLangParser
import com.valgolang.errorhandling.ErrorHandler
import com.valgolang.errorhandling.syntaxerror.SyntaxErrorListener
import com.valgolang.errorhandling.syntaxerror.SyntaxErrorStrategy
import com.valgolang.frontend.VAlgoLangParserVisitor
import com.valgolang.frontend.ProgramNode
import com.valgolang.frontend.StatementNode
import com.valgolang.frontend.SymbolTableVisitor
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
 * VAlgoLangASTGenerator
 *
 * @property input: file to be parsed as an input stream of bytes
 * @constructor Create empty VAlgoLangASTGenerator
 */
class VAlgoLangASTGenerator(private val input: InputStream) {

    /**
     * Parse file to build ANTLR parse tree and find any syntax errors
     *
     * @return pair of exit status [ExitStatus] and parse tree [VAlgoLangParser.ProgramContext]
     */
    fun parseFile(): Pair<ExitStatus, VAlgoLangParser.ProgramContext> {
        val input = CharStreams.fromStream(input)

        /** Lexical analysis **/
        val lexer = VAlgoLangLexer(input)
        lexer.removeErrorListeners()

        /** Syntax analysis **/
        val tokens = CommonTokenStream(lexer)
        val parser = VAlgoLangParser(tokens)

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
    fun convertToAst(program: VAlgoLangParser.ProgramContext): ParserResult {
        val visitor = VAlgoLangParserVisitor()
        val ast = visitor.visitProgram(program)
        return ParserResult(ErrorHandler.checkErrorsAndWarnings(), ast, visitor.symbolTable, visitor.lineNumberNodeMap)
    }
}

data class ParserResult(val exitStatus: ExitStatus, val abstractSyntaxTree: ProgramNode, val symbolTableVisitor: SymbolTableVisitor, val lineNodeMap: MutableMap<Int, StatementNode>)
