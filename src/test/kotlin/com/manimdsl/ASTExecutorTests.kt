package com.manimdsl

import com.manimdsl.frontend.ProgramNode
import com.manimdsl.frontend.SymbolTableVisitor
import com.manimdsl.linearrepresentation.*
import junit.framework.TestCase.assertEquals
import org.junit.jupiter.api.Test


class ASTExecutorTests {

    @Test
    fun checkBasicProgramStates() {
        val program = "let x: number = 1.5;\n" +
                "# code comment\n" +
                "let y: Stack<number> = new Stack<number>;\n"

        val (_, abstractSyntaxTree, symbolTable) = buildAST(program)

        val executor = ASTExecutor(abstractSyntaxTree, symbolTable, program.split("\n"))

        val states = listOf(
            Pair(false, listOf(MoveToLine(1, pointerName = "pointer", codeBlockName = "code_block"))),
            Pair(
                true,
                listOf(
                    CodeBlock(
                        lines = listOf("let x: number = 1.5;", "let y: Stack<number> = new Stack<number>;"),
                        ident = "code_block",
                        codeTextName = "code_text",
                        pointerName = "pointer"
                    ),
                    MoveToLine(lineNumber = 1, pointerName = "pointer", codeBlockName = "code_block"),
                    MoveToLine(lineNumber = 2, pointerName = "pointer", codeBlockName = "code_block"),
                    InitStructure(Coord(2.0, -1.0), alignment = Alignment.HORIZONTAL, ident = "empty", text = "y")
                )
            )
        )

        checkExecutionStates(executor, states)

    }


    private fun checkExecutionStates(
        executor: ASTExecutor,
        states: List<Pair<Boolean, List<ManimInstr>>>
    ) {
        var stateCounter = 0
        do {
            val state = executor.executeNextStatement()
            assertEquals(state, states[stateCounter])
            stateCounter++
        } while (!state.first)
    }

    // Assumes syntactically correct program
    private fun buildAST(program: String): Triple<ExitStatus, ProgramNode, SymbolTableVisitor> {
        val parser = ManimDSLParser(program.byteInputStream())
        return parser.convertToAst(parser.parseFile().second)
    }
}