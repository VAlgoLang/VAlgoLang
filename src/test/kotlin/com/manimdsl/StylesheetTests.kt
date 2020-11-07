package com.manimdsl

import com.manimdsl.frontend.IdentifierData
import com.manimdsl.frontend.NumberType
import com.manimdsl.frontend.StackType
import com.manimdsl.frontend.SymbolTableVisitor
import com.manimdsl.linearrepresentation.EmptyMObject
import com.manimdsl.runtime.StackValue
import com.manimdsl.stylesheet.Stylesheet
import org.hamcrest.CoreMatchers.`is`
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThat
import org.junit.jupiter.api.Test
import java.io.File
import java.util.*

class StylesheetTests {
    private val stylesheetPath = "src/test/testFiles/stylesheet"

    @Test
    fun variableStyleOverridesDataStructureStyle() {
        val symbolTable = SymbolTableVisitor()
        symbolTable.addVariable("stack1", IdentifierData(StackType(NumberType)))
        val stack1Style = Stylesheet(
            "$stylesheetPath/variableOverrideStylesheet.json",
            symbolTable
        ).getStyle("stack1", StackValue(EmptyMObject, Stack()))
        assertThat(stack1Style.borderColor, `is`("ORANGE"))
        assertThat(stack1Style.textColor, `is`("BLUE"))
    }

    @Test
    fun dataStructureStyleIsPassedToVariableWhenNoOtherStyleExists() {
        val symbolTable = SymbolTableVisitor()
        symbolTable.addVariable("stack1", IdentifierData(StackType(NumberType)))
        val stack1Style = Stylesheet(
            "$stylesheetPath/stackTypeStylesheet.json",
            symbolTable
        ).getStyle("stack1", StackValue(EmptyMObject, Stack()))
        assertThat(stack1Style.borderColor, `is`("YELLOW"))
        assertThat(stack1Style.textColor, `is`("GREEN"))
    }

    @Test
    fun dataStructureStyleMergesWithVariableStyle() {
        val symbolTable = SymbolTableVisitor()
        symbolTable.addVariable("stack1", IdentifierData(StackType(NumberType)))
        val stack1Stylesheet =
            Stylesheet("$stylesheetPath/mixedStylesheet.json", symbolTable)
        val stack1Style = stack1Stylesheet.getStyle("stack1", StackValue(EmptyMObject, Stack()))
        assertThat(stack1Style.borderColor, `is`("YELLOW"))
        assertThat(stack1Style.textColor, `is`("BLUE"))

        val stack1AnimationStyle = stack1Stylesheet.getAnimatedStyle("stack1", StackValue(EmptyMObject, Stack()))!!
        assertThat(stack1AnimationStyle.borderColor, `is`("BLUE"))
        assertThat(stack1AnimationStyle.textColor, `is`("RED"))
    }

    @Test
    fun localVariableAssignedCorrectStyle() {
        val stylesheet = getStylesheetAfterRunningVirtualMachine("stackFunction.manimdsl", "variableOverrideStylesheet.json")
        val stack1Style = stylesheet.getStyle("stack1", StackValue(EmptyMObject, Stack()))
        assertThat(stack1Style.borderColor, `is`("ORANGE"))
        assertThat(stack1Style.textColor, `is`("BLUE"))
    }

    @Test
    fun defaultCodeTrackingIsStepInto() {
        val stylesheet = Stylesheet(null, SymbolTableVisitor())
        assertEquals(true, stylesheet.getStepIntoIsDefault())
    }

    private fun getStylesheetAfterRunningVirtualMachine(fileName: String, stylesheetName: String): Stylesheet {
        val inputFile = File("src/test/testFiles/valid/$fileName")
        val parser = ManimDSLParser(inputFile.inputStream())
        val program = parser.parseFile().second
        val parserResult = parser.convertToAst(program)
        val stylesheet = Stylesheet(
            "$stylesheetPath/$stylesheetName",
            parserResult.symbolTableVisitor
        )
//        VirtualMachine(parserResult.abstractSyntaxTree, parserResult.symbolTableVisitor, parserResult.lineNodeMap, inputFile.readLines(), stylesheet).runProgram()
        return stylesheet
    }

}