package com.valgolang.stylesheet

import com.valgolang.VAlgoLangASTGenerator
import com.valgolang.linearrepresentation.EmptyMObject
import com.valgolang.runtime.VirtualMachine
import com.valgolang.runtime.datastructures.stack.StackValue
import org.hamcrest.CoreMatchers.`is`
import org.junit.Assert.assertThat
import org.junit.jupiter.api.Test
import java.io.File
import java.util.*

class StylesheetIntegrationTests {
    private val stylesheetPath = "src/test/testFiles/stylesheet"
    private val validTestFilePath = "src/test/testFiles/valid"

    @Test
    fun localVariableAssignedCorrectStyle() {
        val stylesheet =
            runCompiler("stackFunction.val", "variableOverrideStylesheet.json")
        val stack1Style = stylesheet.getStyle("stack1", StackValue(EmptyMObject, Stack()))
        assertThat(stack1Style.borderColor, `is`("ORANGE"))
        assertThat(stack1Style.textColor, `is`("BLUE"))
    }

    private fun runCompiler(fileName: String, stylesheetName: String): Stylesheet {
        val inputFile = File("$validTestFilePath/$fileName")
        val parser = VAlgoLangASTGenerator(inputFile.inputStream())
        val program = parser.parseFile().second
        val parserResult = parser.convertToAst(program)
        val stylesheet = Stylesheet(
            "$stylesheetPath/$stylesheetName",
            parserResult.symbolTableVisitor
        )
        VirtualMachine(
            parserResult.abstractSyntaxTree,
            parserResult.symbolTableVisitor,
            parserResult.lineNodeMap,
            inputFile.readLines(),
            stylesheet
        ).runProgram().second
        return stylesheet
    }
}
