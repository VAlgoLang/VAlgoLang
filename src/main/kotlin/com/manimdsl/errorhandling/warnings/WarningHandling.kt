package com.manimdsl.errorhandling.warnings

import com.manimdsl.errorhandling.ErrorHandler.addWarning

fun undeclaredVariableStyleWarning(identifier: String) {
    addWarning("Created style for variable $identifier that has not been declared")
}
