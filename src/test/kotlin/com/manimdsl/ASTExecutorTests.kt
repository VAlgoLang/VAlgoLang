package com.manimdsl

import com.manimdsl.frontend.ProgramNode
import junit.framework.TestCase.assertEquals

import org.junit.jupiter.api.Test
import java.util.*


class ASTExecutorTests {

    @Test
    fun checkBasicProgramStates() {
        val program = "let x: number = 1.5;\n" +
                "# code comment\n" +
                "let y: Stack = new Stack;\n"

        val abstractSyntaxTree = buildAST(program)

        val executor = ASTExecutor(abstractSyntaxTree)

        val states = listOf(
            Pair(false, mapOf("x" to DoubleValue(1.5))),
            Pair(true, mapOf("x" to DoubleValue(1.5), "y" to StackValue(Stack())))
        )

        checkExecutionStates(executor, states)

    }

    @Test
    fun checkStatePopPushProgramStates() {
        val program = "let x: number = 1.5;\n" +
                "# code comment\n" +
                "let y: Stack = new Stack;\n" +
                "y.push(10);\n" +
                "let z = y.pop();\n"

        val abstractSyntaxTree = buildAST(program)

        val executor = ASTExecutor(abstractSyntaxTree)

        val stack = Stack<Double>()
        stack.push(10.0)

        val states = mutableListOf(
            Pair(false, mapOf("x" to DoubleValue(1.5))),
            Pair(false, mapOf("x" to DoubleValue(1.5), "y" to StackValue(Stack()))),
            Pair(false, mapOf("x" to DoubleValue(1.5), "y" to StackValue(stack))),
            Pair(true, mapOf("x" to DoubleValue(1.5), "z" to DoubleValue(10.0), "y" to StackValue(Stack())))
        )

        checkExecutionStates(executor, states)

    }

    private fun checkExecutionStates(
        executor: ASTExecutor,
        states: List<Pair<Boolean, Map<String, ExecValue>>>
    ) {
        var stateCounter = 0
        do {
            val state = executor.executeNextStatement()
            assertEquals(state, states[stateCounter])
            stateCounter++
        } while (!state.first)
    }

    // Assumes syntactically correct program
    private fun buildAST(program: String): ProgramNode {
        val parser = ManimDSLParser(program.byteInputStream())
        return parser.convertToAst(parser.parseFile().second).second
    }
}