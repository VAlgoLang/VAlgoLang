package com.manimdsl.stylesheet

import com.manimdsl.executor.StackValue
import com.manimdsl.frontend.IdentifierData
import com.manimdsl.frontend.NumberType
import com.manimdsl.frontend.StackType
import com.manimdsl.frontend.SymbolTableVisitor
import com.manimdsl.linearrepresentation.EmptyMObject
import org.hamcrest.CoreMatchers.`is`
import org.junit.Assert.assertThat
import org.junit.Test
import java.util.*

class StylesheetTests {
    private val stylesheetPath = "src/test/testFiles/stylesheet"

    @Test
    fun variableStyleOverridesDataStructureStyle() {
        val symbolTable = SymbolTableVisitor()
        symbolTable.addVariable("stack1", IdentifierData(StackType(NumberType)))
        val stack1Style = Stylesheet("$stylesheetPath/variableOverrideStylesheet.json", symbolTable).getStyle("stack1", StackValue(EmptyMObject, Stack()))
        assertThat(stack1Style.borderColor, `is`("ORANGE"))
        assertThat(stack1Style.textColor, `is`("BLUE"))
    }

    @Test
    fun dataStructureStyleIsPassedToVariableWhenNoOtherStyleExists() {
        val symbolTable = SymbolTableVisitor()
        symbolTable.addVariable("stack1", IdentifierData(StackType(NumberType)))
        val stack1Style = Stylesheet("$stylesheetPath/stackTypeStylesheet.json", symbolTable).getStyle("stack1", StackValue(EmptyMObject, Stack()))
        assertThat(stack1Style.borderColor, `is`("YELLOW"))
        assertThat(stack1Style.textColor, `is`("GREEN"))
    }

    @Test
    fun dataStructureStyleMergesWithVariableStyle() {
        val symbolTable = SymbolTableVisitor()
        symbolTable.addVariable("stack1", IdentifierData(StackType(NumberType)))
        val stack1Stylesheet = Stylesheet("$stylesheetPath/mixedStylesheet.json", symbolTable)
        val stack1Style = stack1Stylesheet.getStyle("stack1", StackValue(EmptyMObject, Stack()))
        assertThat(stack1Style.borderColor, `is`("YELLOW"))
        assertThat(stack1Style.textColor, `is`("BLUE"))

        val stack1AnimationStyle = stack1Stylesheet.getAnimatedStyle("stack1", StackValue(EmptyMObject, Stack()))!!
        assertThat(stack1AnimationStyle.borderColor, `is`("BLUE"))
        assertThat(stack1AnimationStyle.textColor, `is`("RED"))
    }

}