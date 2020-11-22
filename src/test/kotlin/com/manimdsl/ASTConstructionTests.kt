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
            DeclarationNode(
                3,
                IdentifierNode(3, "y"),
                ConstructorNode(3, StackType(NumberType), emptyList(), EmptyInitialiserNode)
            )
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
            DeclarationNode(
                1,
                IdentifierNode(1, "y"),
                ConstructorNode(1, StackType(NumberType), emptyList(), EmptyInitialiserNode)
            ),
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
        val functionDeclarationProgram = "fun func(x : number): number {\n" +
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
            DeclarationNode(
                2,
                IdentifierNode(2, "z"),
                AddExpression(2, IdentifierNode(2, "x"), IdentifierNode(2, "y"))
            ),
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
                FunctionCallNode(5, "func", listOf(NumberNode(5, 1.0), NumberNode(5, 2.0)))
            ),
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

    @Test
    fun whileLoopProgram() {
        val methodProgram = "let x = 0;\n" +
                "while(x < 2) {\n" +
                "    x = x + 1;\n" +
                "}"
        val statements = listOf(
            DeclarationNode(1, IdentifierNode(1, "x"), NumberNode(1, 0.0)),
            WhileStatementNode(
                lineNumber = 2,
                endLineNumber = 4,
                scope = 1,
                condition = LtExpression(2, IdentifierNode(2, "x"), NumberNode(2, 2.0)),
                statements = listOf(
                    AssignmentNode(
                        3,
                        IdentifierNode(3, "x"),
                        AddExpression(3, IdentifierNode(3, "x"), NumberNode(3, 1.0))
                    )
                )
            )
        )
        val reference = ProgramNode(listOf(), statements)
        val actual = buildAST(methodProgram)
        assertEquals(reference, actual)
    }

    @Test
    fun whileLoopWithBreakProgram() {
        val methodProgram = "let x = 0;\n" +
                "while(true) {\n" +
                "    x = x + 1;\n" +
                "    if (x == 2) {\n" +
                "       break;\n" +
                "    }\n" +
                "}"
        val statements = listOf(
            DeclarationNode(1, IdentifierNode(1, "x"), NumberNode(1, 0.0)),
            WhileStatementNode(
                lineNumber = 2,
                endLineNumber = 7,
                scope = 1,
                condition = BoolNode(2, true),
                statements = listOf(
                    AssignmentNode(
                        3,
                        IdentifierNode(3, "x"),
                        AddExpression(3, IdentifierNode(3, "x"), NumberNode(3, 1.0))
                    ),
                    IfStatementNode(
                        lineNumber = 4,
                        endLineNumber = 6,
                        scope = 2,
                        condition = EqExpression(4, IdentifierNode(4, "x"), NumberNode(4, 2.0)),
                        statements = listOf(BreakNode(5, 7)),
                        elifs = emptyList(),
                        elseBlock = ElseNode(6, 0, emptyList())
                    )
                )
            )
        )
        val reference = ProgramNode(listOf(), statements)
        val actual = buildAST(methodProgram)
        assertEquals(reference, actual)
    }

    @Test
    fun whileLoopWithContinueProgram() {
        val methodProgram = "let x = 0;\n" +
                "while(x < 2) {\n" +
                "    x = x + 1;\n" +
                "    if (x == 1) {\n" +
                "       x = 3;\n" +
                "       continue;\n" +
                "    }\n" +
                "}"
        val statements = listOf(
            DeclarationNode(1, IdentifierNode(1, "x"), NumberNode(1, 0.0)),
            WhileStatementNode(
                lineNumber = 2,
                endLineNumber = 8,
                scope = 1,
                condition = LtExpression(2, IdentifierNode(2, "x"), NumberNode(2, 2.0)),
                statements = listOf(
                    AssignmentNode(
                        3,
                        IdentifierNode(3, "x"),
                        AddExpression(3, IdentifierNode(3, "x"), NumberNode(3, 1.0))
                    ),
                    IfStatementNode(
                        lineNumber = 4,
                        endLineNumber = 7,
                        scope = 2,
                        condition = EqExpression(4, IdentifierNode(4, "x"), NumberNode(4, 1.0)),
                        statements = listOf(
                            AssignmentNode(5, IdentifierNode(5, "x"), NumberNode(5, 3.0)),
                            ContinueNode(6, 2)
                        ),
                        elifs = emptyList(),
                        elseBlock = ElseNode(7, 0, emptyList())
                    )
                )
            )
        )
        val reference = ProgramNode(listOf(), statements)
        val actual = buildAST(methodProgram)
        assertEquals(reference, actual)
    }

    @Test
    fun forLoopProgram() {
        val methodProgram = "let x = 0;\n" +
                "for i in range(3) {\n" +
                "    x = x + 1;\n" +
                "}"
        val statements = listOf(
            DeclarationNode(1, IdentifierNode(1, "x"), NumberNode(1, 0.0)),
            ForStatementNode(
                lineNumber = 2,
                endLineNumber = 4,
                scope = 1,
                beginStatement = DeclarationNode(2, IdentifierNode(2, "i"), NumberNode(2, 0.0)),
                endCondition = NumberNode(2, 3.0),
                updateCounter = AssignmentNode(
                    2,
                    IdentifierNode(2, "i"),
                    AddExpression(2, IdentifierNode(3, "i"), NumberNode(2, 1.0))
                ),
                statements = listOf(
                    AssignmentNode(
                        3,
                        IdentifierNode(3, "x"),
                        AddExpression(3, IdentifierNode(3, "x"), NumberNode(3, 1.0))
                    )
                )
            )
        )
        val reference = ProgramNode(listOf(), statements)
        val actual = buildAST(methodProgram)
        assertEquals(reference.toString(), actual.toString())
    }

    @Test
    fun nestedForLoopWithBreak() {
        val methodProgram = "let x = 0;\n" +
                "for i in range(3) {\n" +
                "    for j in range(i, 5) {\n" +
                "        if (j > 2) {\n" +
                "            break;\n" +
                "        }\n" +
                "        x = i * j;\n" +
                "    }\n" +
                "}"
        val statements = listOf(
            DeclarationNode(1, IdentifierNode(1, "x"), NumberNode(1, 0.0)),
            ForStatementNode(
                lineNumber = 2,
                endLineNumber = 9,
                scope = 1,
                beginStatement = DeclarationNode(2, IdentifierNode(2, "i"), NumberNode(2, 0.0)),
                endCondition = NumberNode(2, 3.0),
                updateCounter = AssignmentNode(
                    2,
                    IdentifierNode(2, "i"),
                    AddExpression(2, IdentifierNode(3, "i"), NumberNode(2, 1.0))
                ),
                statements = listOf(
                    ForStatementNode(
                        lineNumber = 3,
                        endLineNumber = 8,
                        scope = 2,
                        beginStatement = DeclarationNode(3, IdentifierNode(3, "j"), IdentifierNode(3, "i")),
                        endCondition = NumberNode(3, 5.0),
                        updateCounter = AssignmentNode(
                            3,
                            IdentifierNode(3, "j"),
                            AddExpression(3, IdentifierNode(3, "j"), NumberNode(3, 1.0))
                        ),
                        statements = listOf(
                            IfStatementNode(
                                lineNumber = 4,
                                endLineNumber = 6,
                                scope = 3,
                                condition = GtExpression(4, IdentifierNode(4, "j"), NumberNode(4, 2.0)),
                                statements = listOf(BreakNode(5, 8)),
                                elifs = emptyList(),
                                elseBlock = ElseNode(6, 0, emptyList())
                            ),
                            AssignmentNode(
                                7,
                                IdentifierNode(7, "x"),
                                MultiplyExpression(7, IdentifierNode(7, "i"), IdentifierNode(7, "j"))
                            )
                        )
                    )
                )
            )
        )
        val reference = ProgramNode(listOf(), statements)
        val actual = buildAST(methodProgram)
        assertEquals(reference.toString(), actual.toString())
    }

    @Test
    fun subtitleAnnotations() {
        val methodProgram = """
            let x = 4;
            @subtitle("x is 4", x == 4)
            @subtitleOnce("x is not 4", x != 4)
            @subtitleOnce("x is not 4")
            @subtitle("x is 4")
        """.trimIndent()

        val statements = listOf<StatementNode>(
            DeclarationNode(1, IdentifierNode(1, "x"), NumberNode(1, 4.0)),
            SubtitleAnnotationNode(
                2,
                condition = EqExpression(2, IdentifierNode(2, "x"), NumberNode(2, 4.0)),
                "x is 4",
                showOnce = false
            ),
            SubtitleAnnotationNode(
                3,
                condition = NeqExpression(3, IdentifierNode(3, "x"), NumberNode(3, 4.0)),
                "x is not 4",
                showOnce = true
            ),
            SubtitleAnnotationNode(
                4,
                condition = BoolNode(4, true),
                "x is not 4",
                showOnce = true
            ),
            SubtitleAnnotationNode(
                5,
                condition = BoolNode(5, true),
                "x is 4",
                showOnce = false
            )
        )

        val reference = ProgramNode(listOf(), statements)
        val actual = buildAST(methodProgram)
        assertEquals(reference.toString(), actual.toString())
    }

    // Assumes syntactically correct program
    private fun buildAST(program: String): ASTNode {
        val parser = ManimDSLParser(program.byteInputStream())
        val (_, ast, _, _) = parser.convertToAst(parser.parseFile().second)
        return ast
    }
}
