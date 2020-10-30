package com.manimdsl.stylesheet

import com.manimdsl.errorhandling.ErrorHandler
import com.manimdsl.errorhandling.warnings.undeclaredVariableStyleWarning
import com.manimdsl.frontend.SymbolTableVisitor

object StyleSheetValidator {
    private val dataStructureStrings = setOf("Stack", "Array")
    private val validCodeTracking = setOf("stepOver", "stepInto")

    fun validateStyleSheet(styleSheet: StyleSheetFromJSON, symbolTable: SymbolTableVisitor) {
        // Check variables
        styleSheet.variables.keys.forEach {
            if (!(symbolTable.getVariableNames().contains(it))) {
                undeclaredVariableStyleWarning(it)
            }
        }

        // Check data structures styles
        styleSheet.dataStructures.keys.forEach {
            if (!(dataStructureStrings.contains(it))) {
                undeclaredVariableStyleWarning(it)
            }
        }

        // Check code tracking
        if(!validCodeTracking.contains(styleSheet.codeTracking)) {
            // throw warning
            undeclaredVariableStyleWarning("hi")
        }

        ErrorHandler.checkWarnings()
    }

}