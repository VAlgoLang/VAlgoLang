package com.manimdsl.stylesheet

import com.manimdsl.ManimDSLParser
import com.manimdsl.animation.ManimProjectWriter
import com.manimdsl.animation.ManimWriter
import com.manimdsl.linearrepresentation.EmptyMObject
import com.manimdsl.runtime.StackValue
import com.manimdsl.runtime.VirtualMachine
import org.hamcrest.CoreMatchers.`is`
import org.junit.Assert.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.util.*

class StylesheetIntegrationTests {
    private val stylesheetPath = "src/test/testFiles/stylesheet"
    private val validTestFilePath = "src/test/testFiles/valid"

    private val standardOut = System.out
    private val outputStreamCaptor = ByteArrayOutputStream()

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

    @Test
    fun localVariableAssignedCorrectStyle() {
        val stylesheet =
            runCompiler("stackFunction.manimdsl", "variableOverrideStylesheet.json")
        val stack1Style = stylesheet.getStyle("stack1", StackValue(EmptyMObject, Stack()))
        assertThat(stack1Style.borderColor, `is`("ORANGE"))
        assertThat(stack1Style.textColor, `is`("BLUE"))
    }

    @Test
    fun incorrectCreationStyleDoesNotGetPassedToPython() {
        runCompiler("stackFunction.manimdsl", "incorrectCreationStyle.json", true)
        // Pattern to recognise Manim error but not our warning
        assertFalse(outputStreamCaptor.toString().contains(Regex(": RandomWrongTransform")))
    }

    @Test
    fun incorrectAnimationStyleDoesNotGetPassedToPython() {
        runCompiler("stackFunction.manimdsl", "incorrectAnimationStyle.json", true)
        // Pattern to recognise Manim error but not our warning
        assertFalse(outputStreamCaptor.toString().contains(Regex(": RandomWrongTransform")))
    }


    private fun runCompiler(fileName: String, stylesheetName: String, generateAnimation: Boolean = false): Stylesheet {
        val inputFile = File("$validTestFilePath/$fileName")
        val parser = ManimDSLParser(inputFile.inputStream())
        val program = parser.parseFile().second
        val parserResult = parser.convertToAst(program)
        val stylesheet = Stylesheet(
            "$stylesheetPath/$stylesheetName",
            parserResult.symbolTableVisitor
        )
        val manimInstructions = VirtualMachine(
            parserResult.abstractSyntaxTree,
            parserResult.symbolTableVisitor,
            parserResult.lineNodeMap,
            inputFile.readLines(),
            stylesheet
        ).runProgram().second
        if (generateAnimation) {
            val writer = ManimProjectWriter(ManimWriter(manimInstructions).build())
            val pythonOutput = writer.createPythonFile()
            writer.generateAnimation(pythonOutput, listOf("-l"), "out.mp4")
        }
        return stylesheet
    }


}