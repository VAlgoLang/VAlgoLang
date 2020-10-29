package com.manimdsl
import com.manimdsl.linearrepresentation.CodeBlock
import com.manimdsl.linearrepresentation.MoveToLine
import com.manimdsl.linearrepresentation.Sleep
import com.manimdsl.runtime.VirtualMachine
import com.manimdsl.stylesheet.Stylesheet
import junit.framework.TestCase.assertEquals
import org.junit.jupiter.api.Test


class ASTExecutorTests {


    @Test
    fun checkBasicFunction() {
        val program =
                "fun f(number x): number{\n" +
                "    return x * 3;\n" +
                "}\n" +
                "let ans = f(3);\n"

        val (_, abstractSyntaxTree, symbolTable, lineNodeMap) = buildAST(program)

        val expected = listOf(
            CodeBlock(
                lines = listOf("fun f(number x): number{", "    return x * 3;", "}", "let ans = f(3);", ""),
                ident = "code_block",
                codeTextName = "code_text",
                pointerName = "pointer"
            ),
            MoveToLine(lineNumber = 4, pointerName = "pointer", codeBlockName = "code_block"),
            MoveToLine(lineNumber = 1, pointerName = "pointer", codeBlockName = "code_block"),
            MoveToLine(lineNumber = 2, pointerName = "pointer", codeBlockName = "code_block"),
            MoveToLine(lineNumber = 4, pointerName = "pointer", codeBlockName = "code_block"),
            Sleep(0.5)
        )
        val (_, actual) = VirtualMachine(abstractSyntaxTree, symbolTable, lineNodeMap, program.split("\n"), Stylesheet(null, symbolTable)).runProgram()

        assertEquals(expected, actual)

    }


    // Assumes syntactically correct program
    private fun buildAST(program: String): ParserResult {
        val parser = ManimDSLParser(program.byteInputStream())
        return parser.convertToAst(parser.parseFile().second)
    }
}