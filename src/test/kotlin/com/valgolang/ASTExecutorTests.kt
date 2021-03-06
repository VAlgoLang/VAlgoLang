package com.valgolang

import com.valgolang.linearrepresentation.*
import com.valgolang.runtime.VirtualMachine
import com.valgolang.stylesheet.Stylesheet
import junit.framework.TestCase.assertEquals
import org.junit.jupiter.api.Test

class ASTExecutorTests {
    val defaultVariableBlockBoundaries = listOf(
        Pair(-7.0, 3.9999999999999996),
        Pair(-2.0, 3.9999999999999996),
        Pair(-7.0, 1.333333333333333),
        Pair(-2.0, 1.333333333333333)
    )
    val defaultCodeBlockBoundaries =
        listOf(Pair(-7.0, 1.333333333333333), Pair(-2.0, 1.333333333333333), Pair(-7.0, -4.0), Pair(-2.0, -4.0))

    @Test
    fun checkBasicFunction() {
        val program =
            "fun f(x: number): number{\n" +
                "    return x * 3;\n" +
                "}\n" +
                "let ans = f(3);\n"

        val (_, abstractSyntaxTree, symbolTable, lineNodeMap) = buildAST(program)

        val expected = listOf(
            VariableBlock(
                variables = listOf(),
                ident = "variable_block",
                variableGroupName = "variable_vg",
                textColor = null,
                boundaries = defaultVariableBlockBoundaries
            ),
            CodeBlock(
                lines = listOf(
                    listOf("fun f(x: number): number{"),
                    listOf("    return x * 3;"),
                    listOf("}"),
                    listOf("let ans = f(3);"),
                    listOf(" ")
                ),
                ident = "code_block",
                codeTextName = "code_text",
                pointerName = "pointer", runtime = 1.0,
                boundaries = defaultCodeBlockBoundaries
            ),
            UpdateVariableState(variables = emptyList(), ident = "variable_block", textColor = null, runtime = 1.0),
            MoveToLine(
                lineNumber = 4,
                pointerName = "pointer",
                codeBlockName = "code_block",
                codeTextVariable = "code_text", runtime = 1.0
            ),
            UpdateVariableState(
                variables = listOf("x = 3.0"),
                ident = "variable_block",
                textColor = null,
                runtime = 1.0
            ),
            MoveToLine(
                lineNumber = 1,
                pointerName = "pointer",
                codeBlockName = "code_block",
                codeTextVariable = "code_text", runtime = 1.0
            ),
            MoveToLine(
                lineNumber = 2,
                pointerName = "pointer",
                codeBlockName = "code_block",
                codeTextVariable = "code_text", runtime = 1.0
            ),
            MoveToLine(
                lineNumber = 4,
                pointerName = "pointer",
                codeBlockName = "code_block",
                codeTextVariable = "code_text", runtime = 1.0
            ),
            UpdateVariableState(
                variables = listOf("ans = 9.0"),
                ident = "variable_block",
                textColor = null,
                runtime = 1.0
            ),
            Sleep(length = 1.0, runtime = 1.0)
        )
        val (_, actual) = VirtualMachine(
            abstractSyntaxTree,
            symbolTable,
            lineNodeMap,
            program.split("\n"),
            Stylesheet(null, symbolTable)
        ).runProgram()
        assertEquals(expected.toString(), actual.toString())
    }

    @Test
    fun checkStepInBlock() {
        val program =
            "fun f(x: number): number{\n" +
                "    return x * 3;\n" +
                "}\n" +
                "@stepInto { \n" +
                "let ans = f(3);\n" +
                "}\n"

        val (_, abstractSyntaxTree, symbolTable, lineNodeMap) = buildAST(program)

        val expected = listOf(
            VariableBlock(
                listOf(),
                ident = "variable_block",
                variableGroupName = "variable_vg",
                textColor = null,
                runtime = 1.0,
                boundaries = defaultVariableBlockBoundaries
            ),
            CodeBlock(
                lines = listOf(
                    listOf("fun f(x: number): number{"),
                    listOf("    return x * 3;"),
                    listOf("}"),
                    listOf("let ans = f(3);"),
                    listOf(" ")
                ),
                ident = "code_block",
                codeTextName = "code_text",
                pointerName = "pointer", runtime = 1.0,
                boundaries = defaultCodeBlockBoundaries
            ),
            UpdateVariableState(variables = listOf(), ident = "variable_block", textColor = null, runtime = 1.0),
            MoveToLine(
                lineNumber = 4,
                pointerName = "pointer",
                codeBlockName = "code_block",
                codeTextVariable = "code_text",
                runtime = 1.0
            ),
            UpdateVariableState(
                variables = listOf("x = 3.0"),
                ident = "variable_block",
                textColor = null,
                runtime = 1.0
            ),
            MoveToLine(
                lineNumber = 1,
                pointerName = "pointer",
                codeBlockName = "code_block",
                codeTextVariable = "code_text",
                runtime = 1.0
            ),
            MoveToLine(
                lineNumber = 2,
                pointerName = "pointer",
                codeBlockName = "code_block",
                codeTextVariable = "code_text",
                runtime = 1.0
            ),
            MoveToLine(
                lineNumber = 4,
                pointerName = "pointer",
                codeBlockName = "code_block",
                codeTextVariable = "code_text",
                runtime = 1.0
            ),
            UpdateVariableState(
                variables = listOf("ans = 9.0"),
                ident = "variable_block",
                textColor = null,
                runtime = 1.0
            ),
            Sleep(1.0, runtime = 1.0)
        )
        val (_, actual) = VirtualMachine(
            abstractSyntaxTree,
            symbolTable,
            lineNodeMap,
            program.split("\n"),
            Stylesheet(null, symbolTable)
        ).runProgram()

        assertEquals(expected.toString(), actual.toString())
    }

    @Test
    fun checkStepOverBlock() {
        val program =
            "fun f(x: number): number{\n" +
                "    return x * 3;\n" +
                "}\n" +
                "@stepOver { \n" +
                "let ans = f(3);\n" +
                "}\n"

        val (_, abstractSyntaxTree, symbolTable, lineNodeMap) = buildAST(program)

        val expected = listOf(
            VariableBlock(
                listOf(),
                ident = "variable_block",
                variableGroupName = "variable_vg",
                textColor = null,
                runtime = 1.0,
                boundaries = defaultVariableBlockBoundaries
            ),
            CodeBlock(
                lines = listOf(
                    listOf("fun f(x: number): number{"),
                    listOf("    return x * 3;"),
                    listOf("}"),
                    listOf("let ans = f(3);"),
                    listOf(" ")
                ),
                ident = "code_block",
                codeTextName = "code_text",
                pointerName = "pointer", runtime = 1.0,
                boundaries = defaultCodeBlockBoundaries
            ),
            UpdateVariableState(variables = listOf(), ident = "variable_block", textColor = null, runtime = 1.0),
            MoveToLine(
                lineNumber = 4,
                pointerName = "pointer",
                codeBlockName = "code_block",
                codeTextVariable = "code_text", runtime = 1.0
            ),
            UpdateVariableState(
                variables = listOf("ans = 9.0"),
                ident = "variable_block",
                textColor = null,
                runtime = 1.0
            ),
            Sleep(1.0, runtime = 1.0)
        )
        val (_, actual) = VirtualMachine(
            abstractSyntaxTree,
            symbolTable,
            lineNodeMap,
            program.split("\n"),
            Stylesheet(null, symbolTable)
        ).runProgram()

        assertEquals(expected.toString(), actual.toString())
    }

    @Test
    fun checkCastingExpressions() {
        val program =
            """
                let x = 'a';
                let y = toNumber(x);
                let z = toChar(y);
                let a = toNumber(toChar(toNumber('a')));
                let shouldBeTrue = x == z;
                let w = "123";
                let shouldAlsoBeTrue = toNumber(w) == 123;
            """.trimIndent()

        val (_, abstractSyntaxTree, symbolTable, lineNodeMap) = buildAST(program)

        val expected = listOf(
            VariableBlock(
                listOf(),
                ident = "variable_block",
                variableGroupName = "variable_vg",
                textColor = null,
                runtime = 1.0,
                boundaries = defaultVariableBlockBoundaries
            ),
            CodeBlock(
                lines = listOf(
                    listOf("let x = \\'a\\';"),
                    listOf("let y = toNumber(x);"),
                    listOf("let z = toChar(y);"),
                    listOf("let a = toNumber(toChar(toNumber(\\'a\\')));"),
                    listOf("let shouldBeTrue = x == z;"),
                    listOf("let w = \\\"123\\\";"),
                    listOf("let shouldAlsoBeTrue = toNumber(w) == 123;")
                ),
                ident = "code_block",
                codeTextName = "code_text",
                pointerName = "pointer", runtime = 1.0,
                boundaries = defaultCodeBlockBoundaries
            ),
            UpdateVariableState(variables = listOf(), ident = "variable_block", textColor = null, runtime = 1.0),
            MoveToLine(
                lineNumber = 1,
                pointerName = "pointer",
                codeBlockName = "code_block",
                codeTextVariable = "code_text", runtime = 1.0
            ),
            UpdateVariableState(
                variables = listOf("x = \\'a\\'"),
                ident = "variable_block",
                textColor = null,
                runtime = 1.0
            ),
            MoveToLine(
                lineNumber = 2,
                pointerName = "pointer",
                codeBlockName = "code_block",
                codeTextVariable = "code_text", runtime = 1.0
            ),
            UpdateVariableState(
                variables = listOf("x = \\'a\\', y = 97.0"),
                ident = "variable_block",
                textColor = null,
                runtime = 1.0
            ),
            MoveToLine(
                lineNumber = 3,
                pointerName = "pointer",
                codeBlockName = "code_block",
                codeTextVariable = "code_text", runtime = 1.0
            ),
            UpdateVariableState(
                variables = listOf("x = \\'a\\', y = 97.0, z = \\'a\\'"),
                ident = "variable_block",
                textColor = null, runtime = 1.0
            ),
            MoveToLine(
                lineNumber = 4,
                pointerName = "pointer",
                codeBlockName = "code_block",
                codeTextVariable = "code_text", runtime = 1.0
            ),
            UpdateVariableState(
                variables = listOf("x = \\'a\\', y = 97.0, z = \\'a\\', a = 97.0"),
                ident = "variable_block",
                textColor = null, runtime = 1.0
            ),
            MoveToLine(
                lineNumber = 5,
                pointerName = "pointer",
                codeBlockName = "code_block",
                codeTextVariable = "code_text", runtime = 1.0
            ),
            UpdateVariableState(
                variables = listOf("shouldBeTrue = true, y = 97.0, z = \\'a\\', a = 97.0"),
                ident = "variable_block",
                textColor = null, runtime = 1.0
            ),
            MoveToLine(
                lineNumber = 6,
                pointerName = "pointer",
                codeBlockName = "code_block",
                codeTextVariable = "code_text", runtime = 1.0
            ),
            UpdateVariableState(
                variables = listOf("shouldBeTrue = true, w = \\\"123\\\", z = \\'a\\', a = 97.0"),
                ident = "variable_block",
                textColor = null, runtime = 1.0
            ),
            MoveToLine(
                lineNumber = 7,
                pointerName = "pointer",
                codeBlockName = "code_block",
                codeTextVariable = "code_text", runtime = 1.0
            ),
            UpdateVariableState(
                variables = listOf("shouldBeTrue = true, w = \\\"123\\\", shouldAlsoBeTrue = true, a = 97.0"),
                ident = "variable_block",
                textColor = null, runtime = 1.0
            ),
            Sleep(1.0, runtime = 1.0)
        )
        val (_, actual) = VirtualMachine(
            abstractSyntaxTree,
            symbolTable,
            lineNodeMap,
            program.split("\n"),
            Stylesheet(null, symbolTable)
        ).runProgram()

        assertEquals(expected.toString(), actual.toString())
    }

    @Test
    fun checkStringInterpolation() {
        val program = """
            let x = 4;
            let interpolate = "x is " + x;
        """.trimIndent()

        val (_, abstractSyntaxTree, symbolTable, lineNodeMap) = buildAST(program)

        val expected = listOf(
            VariableBlock(
                listOf(),
                ident = "variable_block",
                variableGroupName = "variable_vg",
                textColor = null,
                runtime = 1.0,
                boundaries = defaultVariableBlockBoundaries
            ),
            CodeBlock(
                lines = listOf(
                    listOf("let x = 4;"),
                    listOf("let interpolate = \\\"x is \\\" + x;"),
                ),
                ident = "code_block",
                codeTextName = "code_text",
                pointerName = "pointer", runtime = 1.0,
                boundaries = defaultCodeBlockBoundaries
            ),
            UpdateVariableState(variables = listOf(), ident = "variable_block", textColor = null, runtime = 1.0),
            MoveToLine(
                lineNumber = 1,
                pointerName = "pointer",
                codeBlockName = "code_block",
                codeTextVariable = "code_text", runtime = 1.0
            ),
            UpdateVariableState(
                variables = listOf("x = 4.0"),
                ident = "variable_block",
                textColor = null,
                runtime = 1.0
            ),
            MoveToLine(
                lineNumber = 2,
                pointerName = "pointer",
                codeBlockName = "code_block",
                codeTextVariable = "code_text", runtime = 1.0
            ),
            UpdateVariableState(
                variables = listOf("x = 4.0", "interpolate = \\\"x is 4.0\\\""),
                ident = "variable_block",
                textColor = null,
                runtime = 1.0
            ),
            Sleep(1.0, runtime = 1.0)
        )
        val (_, actual) = VirtualMachine(
            abstractSyntaxTree,
            symbolTable,
            lineNodeMap,
            program.split("\n"),
            Stylesheet(null, symbolTable)
        ).runProgram()

        assertEquals(expected.toString(), actual.toString())
    }

    @Test
    fun checkStringArrayAccess() {
        val program = """
            let x = "abcdefgh";
            let firstChar = x[0];
            let thirdChar = x[2];
        """.trimIndent()

        val (_, abstractSyntaxTree, symbolTable, lineNodeMap) = buildAST(program)

        val expected = listOf(
            VariableBlock(
                listOf(),
                ident = "variable_block",
                variableGroupName = "variable_vg",
                textColor = null,
                runtime = 1.0,
                boundaries = defaultVariableBlockBoundaries
            ),
            CodeBlock(
                lines = listOf(
                    listOf("let x = \\\"abcdefgh\\\";"),
                    listOf("let firstChar = x[0];"),
                    listOf("let thirdChar = x[2];"),
                ),
                ident = "code_block",
                codeTextName = "code_text",
                pointerName = "pointer", runtime = 1.0,
                boundaries = defaultCodeBlockBoundaries
            ),
            UpdateVariableState(variables = listOf(), ident = "variable_block", textColor = null, runtime = 1.0),
            MoveToLine(
                lineNumber = 1,
                pointerName = "pointer",
                codeBlockName = "code_block",
                codeTextVariable = "code_text", runtime = 1.0
            ),
            UpdateVariableState(
                variables = listOf("x = \\\"abcdefgh\\\""),
                ident = "variable_block",
                textColor = null,
                runtime = 1.0
            ),
            MoveToLine(
                lineNumber = 2,
                pointerName = "pointer",
                codeBlockName = "code_block",
                codeTextVariable = "code_text", runtime = 1.0
            ),
            UpdateVariableState(
                variables = listOf("x = \\\"abcdefgh\\\"", "firstChar = \\\'a\\\'"),
                ident = "variable_block",
                textColor = null,
                runtime = 1.0
            ),
            MoveToLine(
                lineNumber = 3,
                pointerName = "pointer",
                codeBlockName = "code_block",
                codeTextVariable = "code_text", runtime = 1.0
            ),
            UpdateVariableState(
                variables = listOf("x = \\\"abcdefgh\\\"", "firstChar = \\\'a\\\'", "thirdChar = \\\'c\\\'"),
                ident = "variable_block",
                textColor = null,
                runtime = 1.0
            ),
            Sleep(1.0, runtime = 1.0)
        )
        val (_, actual) = VirtualMachine(
            abstractSyntaxTree,
            symbolTable,
            lineNodeMap,
            program.split("\n"),
            Stylesheet(null, symbolTable)
        ).runProgram()

        assertEquals(expected.toString(), actual.toString())
    }

    // Assumes syntactically correct program
    private fun buildAST(program: String): ParserResult {
        val parser = VAlgoLangASTGenerator(program.byteInputStream())
        return parser.convertToAst(parser.parseFile().second)
    }
}
