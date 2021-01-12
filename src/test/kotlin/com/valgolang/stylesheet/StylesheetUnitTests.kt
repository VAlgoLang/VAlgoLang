package com.valgolang.stylesheet

import com.valgolang.frontend.IdentifierData
import com.valgolang.frontend.NumberType
import com.valgolang.frontend.SymbolTableVisitor
import com.valgolang.frontend.datastructures.stack.StackType
import com.valgolang.linearrepresentation.EmptyMObject
import com.valgolang.runtime.datastructures.stack.StackValue
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
        val stylesheet = Stylesheet(
            "$stylesheetPath/stackTypeStylesheet.json",
            symbolTable
        )
        val stack1Style = stylesheet.getStyle("stack1", StackValue(EmptyMObject, Stack()))
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

        val stack1AnimationStyle =
            stack1Stylesheet.getAnimatedStyle("stack1", StackValue(EmptyMObject, Stack()))
        assertThat(stack1AnimationStyle.borderColor, `is`("RED"))
        assertThat(stack1AnimationStyle.textColor, `is`("RED"))
        assertThat(stack1AnimationStyle.animationStyle, `is`("ApplyWave"))
    }

    @Test
    fun defaultCodeTrackingIsStepInto() {
        val stylesheet = Stylesheet(null, SymbolTableVisitor())
        assertTrue(stylesheet.getStepIntoIsDefault())
    }
}
