package com.manimdsl

import com.manimdsl.linearrepresentation.*
import org.junit.jupiter.api.Test


class ASTExecutorTests {

    @Test
    fun checkBasicProgramStates() {
        val program = "let x: number = 1.5;\n" +
                "# code comment\n" +
                "let y: Stack<number> = new Stack<number>;\n"

        val (_, abstractSyntaxTree, symbolTable, statements) = buildAST(program)

        val executor = VirtualMachine(abstractSyntaxTree, symbolTable, statements, program.split("\n"))

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
                    InitStructure(x = 2, y = -1, alignment = Alignment.HORIZONTAL, ident = "empty", text = "y")
                )
            )
        )

        checkExecutionStates(executor, states)

    }


    private fun checkExecutionStates(
        executor: VirtualMachine,
        states: List<Pair<Boolean, List<ManimInstr>>>
    ) {
//        var stateCounter = 0
//        do {
//            val state = executor.runProgram()
//            assertEquals(state, states[stateCounter])
//            stateCounter++
//        } while (!state.first)
        return
        TODO("Will complete once new VM completed")
    }

    // Assumes syntactically correct program
    private fun buildAST(program: String): ParserResult  {
        val parser = ManimDSLParser(program.byteInputStream())
        return parser.convertToAst(parser.parseFile().second)
    }
}