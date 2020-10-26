package com.manimdsl
import com.manimdsl.linearrepresentation.CodeBlock
import com.manimdsl.linearrepresentation.MoveToLine
import com.manimdsl.linearrepresentation.VariableBlock
import com.manimdsl.linearrepresentation.Sleep
import com.manimdsl.runtime.VirtualMachine
import com.manimdsl.stylesheet.Stylesheet
import junit.framework.TestCase.assertEquals
import org.junit.jupiter.api.Test


class ASTExecutorTests {


    @Test
    fun checkBasicFunction() {
        val program =
                "fun f(x: number): number{\n" +
                        "    return x * 3;\n" +
                "}\n" +
                "let ans = f(3);\n"

        val (_, abstractSyntaxTree, symbolTable, lineNodeMap) = buildAST(program)

        val expected = listOf(
            PartitionBlock(
                    scaleLeft = "1/3",
                    scaleRight = "2/3"
            ),
            VariableBlock(
                    variables = listOf("x = 1"),
                    ident = "variable_block",
                    variableGroupName = "variable_vg",
                    textColor = null
            ),
            CodeBlock(
                lines = listOf("fun f(x: number): number{", "    return x * 3;", "}", "let ans = f(3);", ""),
                ident = "code_block",
                codeTextName = "code_text",
                pointerName = "pointer"
            ),
            MoveToLine(lineNumber = 4, pointerName = "pointer", codeBlockName = "code_block"),
            Sleep(length=0.5)
        )
        val (_, actual) = VirtualMachine(
            abstractSyntaxTree,
            symbolTable,
            lineNodeMap,
            program.split("\n"),
            Stylesheet(null, symbolTable)
        ).runProgram()
        assertEquals(expected, actual)
    }

    @Test
    fun checkStepInBlock() {
        val program =
            "fun f(x: number): number{\n" +
                    "    return x * 3;\n" +
                    "}\n" +
                    "stepinto { \n" +
                    "let ans = f(3);\n" +
                    "}\n"

        val (_, abstractSyntaxTree, symbolTable, lineNodeMap) = buildAST(program)

        val expected = listOf(
            CodeBlock(
                lines = listOf("fun f(x: number): number{", "    return x * 3;", "}", "let ans = f(3);", ""),
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
        val (_, actual) = VirtualMachine(
            abstractSyntaxTree,
            symbolTable,
            lineNodeMap,
            program.split("\n"),
            Stylesheet(null, symbolTable)
        ).runProgram()

        assertEquals(expected, actual)

    }

    // Assumes syntactically correct program
    private fun buildAST(program: String): ParserResult {
        val parser = ManimDSLParser(program.byteInputStream())
        return parser.convertToAst(parser.parseFile().second)
    }
}