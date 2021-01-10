package com.manimdsl.errorhandling.warnings

import com.manimdsl.errorhandling.ErrorHandler.addWarning

/**
 * Warning for providing style for an undeclared variable
 *
 * @param identifier: identifier for undeclared variable
 */
fun undeclaredVariableStyleWarning(identifier: String) {
    addWarning("Created style for variable $identifier that has not been declared")
}

/**
 * Warning for providing invalid value for a stylesheet attribute
 *
 * @param attribute: stylesheet attribute
 * @param expecting: set of valid expected values
 * @param invalid: provided value
 */
fun invalidStyleAttributeWarning(attribute: String, expecting: Collection<String>, invalid: String) {
    addWarning(
        "Invalid value: '$invalid' for attribute '$attribute' expecting: ${
        expecting.joinToString(
            "', '",
            prefix = "'",
            postfix = "'"
        )
        }"
    )
}
