package com.manimdsl.syntaxanalysis

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


class InvalidSyntaxTests {
    private val standardOut = System.out
    private val outputStreamCaptor = ByteArrayOutputStream()

    companion object {
        private const val syntaxErrorFilePath = "src/test/testFiles/invalid/syntaxErrors"

        @JvmStatic
        fun data(): Stream<Arguments> {
            return File(syntaxErrorFilePath).walk().filter { it.isFile }
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

    @ParameterizedTest(name = "{1}")
    @MethodSource("data")
    fun invalidSyntaxErrorTests(fileName: String) {
        val inputFile = File(fileName)
        assertEquals(ExitStatus.SYNTAX_ERROR, ManimDSLParser(inputFile.inputStream()).parseFile().first)
    }

    @Test
    fun mismatchedInput() {
        val inputFile = File("$syntaxErrorFilePath/mismatchedInput.manimdsl")
        ManimDSLParser(inputFile.inputStream()).parseFile()
        assertTrue(
            outputStreamCaptor.toString().contains(Regex("Syntax error at \\d*:\\d*: mismatched input .* expecting .*"))
        )
    }

    @Test
    fun missingToken() {
        val inputFile = File("$syntaxErrorFilePath/missingToken.manimdsl")
        ManimDSLParser(inputFile.inputStream()).parseFile()
        assertTrue(outputStreamCaptor.toString().contains(Regex("Syntax error at \\d*:\\d*: missing .* at .*")))
    }

    @Test
    fun invalidTypeParameter() {
        val inputFile = File("$syntaxErrorFilePath/invalidTypeParameter.manimdsl")
        ManimDSLParser(inputFile.inputStream()).parseFile()
        assertTrue(
            outputStreamCaptor.toString().contains(Regex("Syntax error at \\d*:\\d*: mismatched input .* expecting .*"))
        )
    }

}