package com.manimdsl

import junit.framework.TestCase.assertEquals

import org.junit.jupiter.api.Test


class ValidASTTests {

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
        val declarationProgram = "let x: number = 1.5;\n" +
                "# code comment\n" +
                "let y: Stack = new Stack;\n" +
                "comment(\"Now we push onto a stack\");\n" +
                "y.push(1+6);"
        val statements = listOf(DeclarationNode(1, "x", NumberNode(1, 1.5)),
                DeclarationNode(3, "y", ConstructorNode(3, StackType, listOf())),
                CommentNode("\"Now we push onto a stack\""),
                MethodCallNode(5, "y", "push", listOf(AddExpression(5, NumberNode(5, 1.0), NumberNode(5, 6.0)))))
        val reference = ProgramNode(statements)

        val actual = buildAST(declarationProgram)
        assertEquals(reference, actual)
    }

    // Assumes syntactically correct program
    private fun buildAST(program: String): ASTNode {
        return ManimDSLParser(program.byteInputStream()).parseFile()
    }
}