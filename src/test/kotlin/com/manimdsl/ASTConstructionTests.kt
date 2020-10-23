package com.manimdsl

import com.manimdsl.frontend.*
import junit.framework.TestCase.assertEquals

import org.junit.jupiter.api.Test


class ASTConstructionTests {

    @Test
    fun variableDeclaration() {
        val declarationProgram = "let x: number = 1;"
        val reference = ProgramNode(listOf(), listOf(DeclarationNode(1, "x", NumberNode(1, 1.0))))
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
                "let y: Stack<number> = new Stack<number>;\n"
        val statements = listOf(
            DeclarationNode(1, "x", NumberNode(1, 1.5)),
            DeclarationNode(3, "y", ConstructorNode(3, StackType(NumberType), listOf()))
        )
        val reference = ProgramNode(listOf(), statements)
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
            DeclarationNode(2, "z", NumberNode(2, 10.0)),
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
        val statements = listOf(DeclarationNode(5, "z", NumberNode(5, 5.0)))
        val reference = ProgramNode(functions, statements)
        val actual = buildAST(functionDeclarationProgram)
        assertEquals(reference, actual)
    }

    @Test
    fun functionCallProgram() {
        val functionCallProgram = "fun func(number x, number y): number {\n" +
                "\tlet z: number = x + y;\n" +
                "return z;\n" +
                "}\n" +
                "let z: number = func(1,2);\n" +
                "func(3,4);"
        val functionStatements = listOf(
                DeclarationNode(2, "z", AddExpression(2, IdentifierNode(2, "x"), IdentifierNode(2, "y"))),
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
                "z",
                FunctionCallNode(5, "func", listOf(NumberNode(5, 1.0), NumberNode(5, 2.0)))),
            FunctionCallNode(6, "func", listOf(NumberNode(6, 3.0), NumberNode(6, 4.0)))
        )
        val reference = ProgramNode(functions, statements)
        val actual = buildAST(functionCallProgram)
        assertEquals(reference, actual)
    }


    // Assumes syntactically correct program
    private fun buildAST(program: String): ASTNode {
        val parser = ManimDSLParser(program.byteInputStream())
        val (_, ast, _, _) = parser.convertToAst(parser.parseFile().second)
        return ast
    }
}