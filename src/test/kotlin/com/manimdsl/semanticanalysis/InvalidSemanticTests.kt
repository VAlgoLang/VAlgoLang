package com.manimdsl.semanticanalysis

import com.manimdsl.ExitStatus
import com.manimdsl.ManimDSLParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.jupiter.api.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.util.stream.Stream
import kotlin.streams.asStream


class InvalidSemanticTests {
    private val standardOut = System.out
    private val outputStreamCaptor = ByteArrayOutputStream()

    companion object {
        private const val semanticErrorFilePath = "src/test/testFiles/invalid/semanticErrors"

        @JvmStatic
        fun data(): Stream<Arguments> {
            return File(semanticErrorFilePath).walk().filter { it.isFile }
                    .map { Arguments.of(it.path) }.asStream()
        }

        @JvmStatic
        @BeforeAll
        internal fun initialiseBinFolder() {
            File("src/test/testFiles/bin").mkdir()
        }


        @JvmStatic
        @AfterAll
        internal fun afterAll() {
            File("src/test/testFiles/bin").deleteRecursively()
        }
    }

    // To capture standard out e.g. use of println
    @BeforeEach
    internal fun setUp() {
        System.setOut(PrintStream(outputStreamCaptor))
    }

    // Reset system out to stdout
    @AfterEach
    internal fun tearDown() {
        System.setOut(standardOut)
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    fun invalidSemanticErrorTests(fileName: String) {
        val inputFile = File(fileName)
        val parser = ManimDSLParser(inputFile.inputStream())
        val program = parser.parseFile().second
        val semanticErrorStatus = parser.convertToAst(program).first
        assertEquals(semanticErrorStatus, ExitStatus.SEMANTIC_ERROR)
    }

    @Test
    fun redeclaredVariable() {
        runSyntaxAndSemanticAnalysis("redeclaredVariable.manimdsl")
        assertTrue(
                outputStreamCaptor.toString().contains(Regex(".* of type .* is already declared"))
        )
    }

    @Test
    fun incompatibleTypesOnDeclaration() {
        runSyntaxAndSemanticAnalysis("incompatibleExplicitCast.manimdsl")
        assertTrue(
                outputStreamCaptor.toString().contains(Regex("Cannot assign expression of type .* to .*, which is of type .*"))
        )
    }

    @Test
    fun incompatibleTypesOnAssignment() {
        runSyntaxAndSemanticAnalysis("incompatibleTypesAssignment.manimdsl")
        assertTrue(
                outputStreamCaptor.toString().contains(Regex("Cannot assign expression of type .* to .*, which is of type .*"))
        )
    }

    @Test
    fun undeclaredVariableUsage() {
        runSyntaxAndSemanticAnalysis("undeclaredVariableUse.manimdsl")
        assertTrue(
                outputStreamCaptor.toString().contains(Regex(".* has not been declared"))
        )
    }

    @Test
    fun primitiveTypeMethodCall() {
        runSyntaxAndSemanticAnalysis("methodCallOnPrimitiveType.manimdsl")
        assertTrue(
                outputStreamCaptor.toString().contains(Regex(".* is not a data structure"))
        )
    }

    @Test
    fun incorrectMethodNameForDataStructure() {
        runSyntaxAndSemanticAnalysis("incorrectMethodName.manimdsl")
        assertTrue(
                outputStreamCaptor.toString().contains(Regex(".* does not support .* method"))
        )
    }

    @Test
    fun incorrectArgumentTypesForDataStructureMethod() {
        runSyntaxAndSemanticAnalysis("incorrectArgTypeForPush.manimdsl")
        assertTrue(
                outputStreamCaptor.toString().contains(Regex(".* method on .* does not accept argument .* of type .*"))
        )
    }

    private fun runSyntaxAndSemanticAnalysis(fileName: String) {
        val inputFile = File("$semanticErrorFilePath/$fileName")
        val parser = ManimDSLParser(inputFile.inputStream())
        val program = parser.parseFile().second
        parser.convertToAst(program)
    }


}