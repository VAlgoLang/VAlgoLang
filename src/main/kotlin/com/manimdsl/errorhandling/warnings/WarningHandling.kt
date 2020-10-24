package com.manimdsl.errorhandling.warnings

import com.manimdsl.errorhandling.ErrorHandler.addWarning
import com.manimdsl.frontend.Type

fun undeclaredVariableStyleWarning(identifier: String) {
    addWarning("Created style for variable $identifier that has not been declared")
}

fun primitiveTypeStyleWarning(identifier: String, type: Type) {
    addWarning("Created style for variable $identifier of type $type that is not a data structure")
}
