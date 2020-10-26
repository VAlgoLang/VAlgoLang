package com.manimdsl.warnings

import com.manimdsl.ManimDSLParser
import com.manimdsl.stylesheet.Stylesheet
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream

class WarningTests {
    private val standardOut = System.out
    private val outputStreamCaptor = ByteArrayOutputStream()

    // To capture standard out e.g. use of println
    @Before
    internal fun setUp() {
        System.setOut(PrintStream(outputStreamCaptor))
    }

    // Reset system out to stdout
    @After
    internal fun tearDown() {
        System.setOut(standardOut)
    }

    @Test
    fun creatingStyleForUndeclaredGlobalVariableThrowsWarning() {
        runErrorAndWarningHandling("example.manimdsl", "mixedStylesheet.json")
        Assert.assertTrue(
            outputStreamCaptor.toString().contains(Regex("Created style for variable .* that has not been declared"))
        )
    }

    @Test
    fun creatingStyleForUndeclaredVariableInAllScopesThrowsWarning() {
        runErrorAndWarningHandling("multiplyByTwo.manimdsl", "mixedStylesheet.json")
        Assert.assertTrue(
            outputStreamCaptor.toString().contains(Regex("Created style for variable .* that has not been declared"))
        )
    }


    private fun runErrorAndWarningHandling(fileName: String, stylesheetName: String) {
        val inputFile = File("src/test/testFiles/valid/$fileName")
        val parser = ManimDSLParser(inputFile.inputStream())
        val program = parser.parseFile().second
        val symbolTable = parser.convertToAst(program).symbolTableVisitor
        Stylesheet("src/test/testFiles/stylesheet/$stylesheetName", symbolTable)
    }
}