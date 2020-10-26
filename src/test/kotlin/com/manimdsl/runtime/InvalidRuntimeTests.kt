package com.manimdsl.semanticanalysis

import com.manimdsl.ExitStatus
import com.manimdsl.ManimDSLParser
import com.manimdsl.runtime.VirtualMachine
import com.manimdsl.stylesheet.Stylesheet
import org.junit.Assert.assertEquals
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.util.stream.Stream
import kotlin.streams.asStream


class InvalidRuntimeTests {
    private val standardOut = System.out
    private val outputStreamCaptor = ByteArrayOutputStream()

    companion object {
        private const val semanticErrorFilePath = "src/test/testFiles/invalid/runtimeErrors"

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
    fun invalidRuntimeTests(fileName: String) {
        val inputFile = File(fileName)
        val parser = ManimDSLParser(inputFile.inputStream())
        val (_, program) = parser.parseFile()
        val (_, abstractSyntaxTree, symbolTable, lineNodeMap) = parser.convertToAst(program)
        val (exitStatus, _) = VirtualMachine(abstractSyntaxTree, symbolTable, lineNodeMap, inputFile.readLines(), Stylesheet(null, symbolTable)).runProgram()
        assertEquals(ExitStatus.RUNTIME_ERROR, exitStatus)
    }

}