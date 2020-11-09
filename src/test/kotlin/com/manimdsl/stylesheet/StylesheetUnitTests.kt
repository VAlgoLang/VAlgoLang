package com.manimdsl.stylesheet

import com.manimdsl.frontend.IdentifierData
import com.manimdsl.frontend.NumberType
import com.manimdsl.frontend.StackType
import com.manimdsl.frontend.SymbolTableVisitor
import com.manimdsl.linearrepresentation.EmptyMObject
import com.manimdsl.runtime.StackValue
import org.hamcrest.CoreMatchers.`is`
import org.junit.Assert.assertThat
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.*

class StylesheetUnitTests {
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
        assertThat(stack1Style.creationStyle, `is`("DrawBorderThenFill"))
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
        assertThat(stack1Style.animate!!.animationStyle, `is`("CircleIndicate"))
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
        assertThat(stack1Style.creationStyle, `is`("GrowFromCenter"))

        val stack1AnimationStyle = stack1Stylesheet.getAnimatedStyle("stack1", StackValue(EmptyMObject, Stack()))!!
        assertThat(stack1AnimationStyle.borderColor, `is`("BLUE"))
        assertThat(stack1AnimationStyle.textColor, `is`("RED"))
        assertThat(stack1AnimationStyle.animationStyle, `is`("ApplyWave"))
    }

    @Test
    fun defaultCodeTrackingIsStepInto() {
        val stylesheet = Stylesheet(null, SymbolTableVisitor())
        assertTrue(stylesheet.getStepIntoIsDefault())
    }



}