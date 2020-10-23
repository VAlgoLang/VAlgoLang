package com.manimdsl.stylesheet

import com.manimdsl.frontend.IdentifierData
import com.manimdsl.frontend.NumberType
import com.manimdsl.frontend.StackType
import com.manimdsl.frontend.SymbolTableVisitor
import org.hamcrest.CoreMatchers.`is`
import org.junit.Assert.assertThat
import org.junit.Test

class StylesheetTests {
    private val stylesheetPath = "src/test/testFiles/stylesheet/testStylesheet.json"

    @Test
    fun parsingCorrectStylesheetGivesExpectedStyleProperties() {
        val symbolTable = SymbolTableVisitor()
        symbolTable.addVariable("stack1", IdentifierData(StackType(NumberType)))
        val testStylesheet = Stylesheet(stylesheetPath, symbolTable)
        val stack1Style = testStylesheet.getStyle("stack1")
        assertThat(stack1Style.borderColor, `is`("ORANGE"))
        assertThat(stack1Style.textColor, `is`("BLUE"))
    }


}