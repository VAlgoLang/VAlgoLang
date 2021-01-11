package com.valgolang

import com.valgolang.frontend.*
import junit.framework.TestCase.assertEquals
import org.junit.jupiter.api.Test

class ValidASTTests {

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
            DeclarationNode(3, IdentifierNode(3, "y"), ConstructorNode(3, StackType(NumberType), emptyList(), EmptyInitialiserNode))
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
            DeclarationNode(1, IdentifierNode(1, "y"), ConstructorNode(1, StackType(NumberType), emptyList(), EmptyInitialiserNode)),
            MethodCallNode(2, "y", StackType.PushMethod(argumentTypes = listOf(NumberType to true)), listOf(NumberNode(2, 1.0)))
        )
        val reference = ProgramNode(listOf(), statements)

        val actual = buildAST(methodProgram)
        assertEquals(reference, actual)
    }

    // Assumes syntactically correct program
    private fun buildAST(program: String): ASTNode {
        val parser = VAlgoLangASTGenerator(program.byteInputStream())
        return parser.convertToAst(parser.parseFile().second).abstractSyntaxTree
    }
}
