package com.valgolang.semanticanalysis

import com.valgolang.ExitStatus
import com.valgolang.VAlgoLangASTGenerator
import com.valgolang.runtime.VirtualMachine
import com.valgolang.stylesheet.Stylesheet
import org.junit.Assert.assertEquals
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.File
import java.util.stream.Stream
import kotlin.streams.asStream

class ValidRuntimeTests {
    companion object {
        @JvmStatic
        fun data(): Stream<Arguments> {
            return File("src/test/testFiles/valid/").walk().filter { it.isFile }.map { Arguments.of(it.path) }
                .asStream()
        }

        @JvmStatic
        @BeforeAll
        internal fun initialiseBinFolder() {
            File("src/test/testFiles/bin").mkdir()
        }

        @AfterAll
        @JvmStatic
        internal fun afterAll() {
            File("src/test/testFiles/bin").deleteRecursively()
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    fun testFileExecutesProperlyAtRuntime(fileName: String) {
        val inputFile = File(fileName)
        val parser = VAlgoLangASTGenerator(inputFile.inputStream())
        val (_, program) = parser.parseFile()
        val (_, abstractSyntaxTree, symbolTable, lineNodeMap) = parser.convertToAst(program)
        val (exitStatus, _) = VirtualMachine(abstractSyntaxTree, symbolTable, lineNodeMap, inputFile.readLines(), Stylesheet(null, symbolTable)).runProgram()
        assertEquals(ExitStatus.EXIT_SUCCESS, exitStatus)
    }
}
