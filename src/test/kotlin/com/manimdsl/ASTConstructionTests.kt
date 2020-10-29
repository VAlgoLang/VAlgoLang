package com.manimdsl

import com.manimdsl.frontend.*
import junit.framework.TestCase.assertEquals

import org.junit.jupiter.api.Test


class ASTConstructionTests {

    @Test
    fun variableDeclaration() {
        val declarationProgram = "let x: number = 1;"
        val reference = ProgramNode(listOf(), listOf(DeclarationNode(1, IdentifierNode(1, "x"), NumberNode(1, 1.0))))
        val actual = buildAST(declarationProgram)
        assertEquals(reference, actual)
    }

    @Test
    fun sleepCommand() {
        val declarationProgram = "sleep(1.5);"
        val reference = ProgramNode(listOf(), listOf(SleepNode(1, NumberNode(1, 1.5))))
        val actual = buildAST(declarationProgram)
        assertEquals(reference, actual)
    }

    @Test
    fun multiLineProgram() {
        val multiLineProgram = "let x: number = 1.5;\n" +
                "# code comment\n" +
                "let y: Stack<number> = Stack<number>();\n"
        val statements = listOf(
            DeclarationNode(1, IdentifierNode(1, "x"), NumberNode(1, 1.5)),
            DeclarationNode(3, IdentifierNode(3, "y"), ConstructorNode(3, StackType(NumberType), emptyList(), emptyList()))
        )
        val reference = ProgramNode(listOf(), statements)
        val actual = buildAST(multiLineProgram)
        assertEquals(reference, actual)
    }

    @Test
    fun methodCallProgram() {
        val methodProgram = "let y: Stack<number> = Stack<number>();\n" +
                "y.push(1);\n"
        val statements = listOf(
            DeclarationNode(1, IdentifierNode(1, "y"), ConstructorNode(1, StackType(NumberType), emptyList(), emptyList())),
            MethodCallNode(
                2,
                "y",
                StackType.PushMethod(returnType = ErrorType, argumentTypes = listOf(NumberType to true)),
                listOf(NumberNode(2, 1.0))
            )
        )
        val reference = ProgramNode(listOf(), statements)
        val actual = buildAST(methodProgram)
        assertEquals(reference, actual)
    }

    @Test
    fun functionDeclarationProgram() {
        val functionDeclarationProgram = "fun func(number x): number {\n" +
            "\tlet z: number = 10;\n" +
            "return z;\n" +
            "}\n" +
            "let z: number = 5;"
        val functionStatements = listOf(
            DeclarationNode(2, IdentifierNode(2, "z"), NumberNode(2, 10.0)),
            ReturnNode(3, IdentifierNode(3, "z"))
        )
        val functions = listOf(
            FunctionNode(
                1,
                1,
                "func",
                listOf(ParameterNode("x", NumberType)),
                functionStatements
            )
        )
        val statements = listOf(DeclarationNode(5, IdentifierNode(5, "z"), NumberNode(5, 5.0)))
        val reference = ProgramNode(functions, statements)
        val actual = buildAST(functionDeclarationProgram)
        assertEquals(reference, actual)
    }

    @Test
    fun functionCallProgram() {
        val functionCallProgram = "fun func(x: number, y: number): number {\n" +
                "\tlet z: number = x + y;\n" +
                "return z;\n" +
                "}\n" +
                "let z: number = func(1,2);\n" +
                "func(3,4);"
        val functionStatements = listOf(
            DeclarationNode(2, IdentifierNode(2, "z"), AddExpression(2, IdentifierNode(2, "x"), IdentifierNode(2, "y"))),
            ReturnNode(3, IdentifierNode(3, "z"))
        )
        val functions = listOf(
                FunctionNode(
                    1,
                    1,
                    "func",
                    listOf(ParameterNode("x", NumberType), ParameterNode("y", NumberType)),
                    functionStatements
                )
        )
        val statements = listOf(
            DeclarationNode(
                5,
                IdentifierNode(5, "z"),
                FunctionCallNode(5, "func", listOf(NumberNode(5, 1.0), NumberNode(5, 2.0)))),
            FunctionCallNode(6, "func", listOf(NumberNode(6, 3.0), NumberNode(6, 4.0)))
        )
        val reference = ProgramNode(functions, statements)
        val actual = buildAST(functionCallProgram)
        assertEquals(reference, actual)
    }

    @Test
    fun ifStatementProgram() {
        val methodProgram = "let x = 3;\n" +
                "if(x == 2) {\n" +
                "    x = 2;\n" +
                "} else if (x == 1) {\n" +
                "    x = 4;\n" +
                "} else if (x == 0) {\n" +
                "    x = 3;\n" +
                "} else {\n" +
                "    x = 1;\n" +
                "}"
        val statements = listOf(
            DeclarationNode(1, IdentifierNode(1, "x"), NumberNode(1, 3.0)),
            IfStatementNode(
                lineNumber = 2,
                endLineNumber = 10,
                scope = 1,
                condition = EqExpression(2, IdentifierNode(2, "x"), NumberNode(2, 2.0)),
                statements = listOf(AssignmentNode(3, IdentifierNode(3, "x"), NumberNode(3, 2.0))),
                elifs = listOf(
                    ElifNode(
                        4,
                        scope = 2,
                        condition = EqExpression(4, IdentifierNode(4, "x"), NumberNode(4, 1.0)),
                        statements = listOf(AssignmentNode(5, IdentifierNode(5, "x"), NumberNode(5, 4.0)))
                    ),
                    ElifNode(
                        6,
                        scope = 3,
                        condition = EqExpression(6, IdentifierNode(6, "x"), NumberNode(6, 0.0)),
                        statements = listOf(AssignmentNode(7, IdentifierNode(7, "x"), NumberNode(7, 3.0)))
                    )
                ),
                elseBlock = ElseNode(
                    lineNumber = 8,
                    scope = 4,
                    statements = listOf(
                        AssignmentNode(9, IdentifierNode(9, "x"), NumberNode(9, 1.0))
                    )
                )

            )
        )
        val reference = ProgramNode(listOf(), statements)
        val actual = buildAST(methodProgram)
        assertEquals(reference, actual)
    }

    @Test
    fun ifStatementWithoutElifProgram() {
        val methodProgram = "let x = 3;\n" +
                "if(x == 2) {\n" +
                "    x = 2;\n" +
                "} else {\n" +
                "    x = 1;\n" +
                "}"
        val statements = listOf(
            DeclarationNode(1, IdentifierNode(1, "x"), NumberNode(1, 3.0)),
            IfStatementNode(
                lineNumber = 2,
                endLineNumber = 6,
                scope = 1,
                condition = EqExpression(2, IdentifierNode(2, "x"), NumberNode(2, 2.0)),
                statements = listOf(AssignmentNode(3, IdentifierNode(3, "x"), NumberNode(3, 2.0))),
                elifs = emptyList(),
                elseBlock = ElseNode(
                    lineNumber = 4,
                    scope = 2,
                    statements = listOf(
                        AssignmentNode(5, IdentifierNode(5, "x"), NumberNode(5, 1.0))
                    )
                )
            )
        )
        val reference = ProgramNode(listOf(), statements)
        val actual = buildAST(methodProgram)
        assertEquals(reference, actual)
    }

    @Test
    fun ifStatementJustIfProgram() {
        val methodProgram = "let x = 3;\n" +
                "if(x == 2) {\n" +
                "    x = 2;\n" +
                "}"
        val statements = listOf(
            DeclarationNode(1, IdentifierNode(1, "x"), NumberNode(1, 3.0)),
            IfStatementNode(
                lineNumber = 2,
                endLineNumber = 4,
                scope = 1,
                condition = EqExpression(2, IdentifierNode(2, "x"), NumberNode(2, 2.0)),
                statements = listOf(AssignmentNode(3, IdentifierNode(3, "x"), NumberNode(3, 2.0))),
                elifs = emptyList(),
                elseBlock = ElseNode(4, 0, emptyList())
            )
        )
        val reference = ProgramNode(listOf(), statements)
        val actual = buildAST(methodProgram)
        assertEquals(reference, actual)
    }

    // Assumes syntactically correct program
    private fun buildAST(program: String): ASTNode {
        val parser = ManimDSLParser(program.byteInputStream())
        val (_, ast, _, _) = parser.convertToAst(parser.parseFile().second)
        return ast
    }
}