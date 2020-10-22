package com.manimdsl

import com.manimdsl.frontend.*
import junit.framework.TestCase.assertEquals

import org.junit.jupiter.api.Test


class ASTConstructionTests {

    @Test
    fun variableDeclaration() {
        val declarationProgram = "let x: number = 1;"
        val reference = ProgramNode(listOf(DeclarationNode(1, "x", NumberNode(1, 1.0))))
        val actual = buildAST(declarationProgram)
        assertEquals(reference, actual)
    }

    @Test
    fun sleepCommand() {
        val declarationProgram = "sleep(1.5);"
        val reference = ProgramNode(listOf(SleepNode(NumberNode(1, 1.5))))
        val actual = buildAST(declarationProgram)
        assertEquals(reference, actual)
    }

    @Test
    fun multiLineProgram() {
        val multiLineProgram = "let x: number = 1.5;\n" +
                "# code comment\n" +
                "let y: Stack<number> = new Stack<number>;\n"
        val statements = listOf(
            DeclarationNode(1, "x", NumberNode(1, 1.5)),
            DeclarationNode(3, "y", ConstructorNode(3, StackType(NumberType), listOf()))
        )
        val reference = ProgramNode(statements)
        val actual = buildAST(multiLineProgram)
        assertEquals(reference, actual)
    }

    @Test
    fun methodCallProgram() {
        val methodProgram = "let y: Stack<number> = new Stack<number>;\n" +
                "y.push(1);\n"
        val statements = listOf(
            DeclarationNode(1, "y", ConstructorNode(1, StackType(NumberType), listOf())),
            MethodCallNode(
                2,
                "y",
                StackType.PushMethod(returnType = ErrorType, argumentTypes = listOf(NumberType)),
                listOf(NumberNode(2, 1.0))
            )
        )
        val reference = ProgramNode(statements)
        val actual = buildAST(methodProgram)
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
            DeclarationNode(1, "x", NumberNode(1, 3.0)),
            IfStatement(
                lineNumber = 2,
                ifCondition = EqExpression(2, IdentifierNode(2, "x"), NumberNode(2, 2.0)),
                ifStatement = listOf(AssignmentNode(3, "x", NumberNode(3, 2.0))),
                elifs = listOf(
                    Elif(
                        condition = EqExpression(4, IdentifierNode(4, "x"), NumberNode(4, 1.0)),
                        statements = listOf(AssignmentNode(5, "x", NumberNode(5, 4.0)))
                    ),
                    Elif(
                        condition = EqExpression(6, IdentifierNode(6, "x"), NumberNode(6, 0.0)),
                        statements = listOf(AssignmentNode(7, "x", NumberNode(7, 3.0)))
                    )
                ),
                elseStatement = listOf(
                    AssignmentNode(9, "x", NumberNode(9, 1.0))
                )
            )
        )
        val reference = ProgramNode(statements)
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
            DeclarationNode(1, "x", NumberNode(1, 3.0)),
            IfStatement(
                lineNumber = 2,
                ifCondition = EqExpression(2, IdentifierNode(2, "x"), NumberNode(2, 2.0)),
                ifStatement = listOf(AssignmentNode(3, "x", NumberNode(3, 2.0))),
                elifs = emptyList(),
                elseStatement = listOf(
                    AssignmentNode(5, "x", NumberNode(5, 1.0))
                )
            )
        )
        val reference = ProgramNode(statements)
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
            DeclarationNode(1, "x", NumberNode(1, 3.0)),
            IfStatement(
                lineNumber = 2,
                ifCondition = EqExpression(2, IdentifierNode(2, "x"), NumberNode(2, 2.0)),
                ifStatement = listOf(AssignmentNode(3, "x", NumberNode(3, 2.0))),
                elifs = emptyList(),
                elseStatement = emptyList()
            )
        )
        val reference = ProgramNode(statements)
        val actual = buildAST(methodProgram)
        assertEquals(reference, actual)
    }

    // Assumes syntactically correct program
    private fun buildAST(program: String): ASTNode {
        val parser = ManimDSLParser(program.byteInputStream())
        val (_, ast, _) = parser.convertToAst(parser.parseFile().second)
        return ast
    }
}