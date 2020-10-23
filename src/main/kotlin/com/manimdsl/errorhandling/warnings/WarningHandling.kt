package com.manimdsl.errorhandling.warnings

import com.manimdsl.errorhandling.ErrorHandler.addWarning

class WarningHandling {
    fun undeclaredVariableStyle() {
        addWarning("Created style for a variable that has not been declared")
    }
}