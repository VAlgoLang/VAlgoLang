package com.manimdsl.stylesheet

import com.manimdsl.errorhandling.ErrorHandler
import com.manimdsl.errorhandling.warnings.invalidStyleAttribute
import com.manimdsl.errorhandling.warnings.undeclaredVariableStyleWarning
import com.manimdsl.frontend.SymbolTableVisitor

object StyleSheetValidator {
    private val dataStructureStrings = setOf("Stack", "Array")
    private val validCodeTracking = setOf("stepOver", "stepInto")
    private val validCreationStrings =
        setOf("FadeIn", "FadeInFromLarge", "Write", "GrowFromCenter", "ShowCreation", "DrawBorderThenFill")
    private val validAnimationStrings =
        setOf("Indicate", "ApplyWave", "WiggleOutThenIn", "CircleIndicate", "TurnInsideOut")

    fun validateStyleSheet(stylesheet: StylesheetFromJSON, symbolTable: SymbolTableVisitor) {
        // Check variables
        stylesheet.variables.keys.forEach {
            if (!(symbolTable.getVariableNames().contains(it))) {
                undeclaredVariableStyleWarning(it)
            }
        }

        // Check data structures styles
        stylesheet.dataStructures.keys.forEach {
            if (!(dataStructureStrings.contains(it))) {
                invalidStyleAttribute("dataStructures", dataStructureStrings, it)
            }
        }


        // Check code tracking
        if (!validCodeTracking.contains(stylesheet.codeTracking)) {
            // throw warning
            invalidStyleAttribute("codeTracking", validCodeTracking, stylesheet.codeTracking)
        }

        // Check creation style strings
        checkValidCreationStyles(stylesheet.variables.values)
        checkValidCreationStyles(stylesheet.dataStructures.values)

        // Check animation style strings
        checkValidAnimationStyles(stylesheet.variables.values)
        checkValidAnimationStyles(stylesheet.dataStructures.values)

        ErrorHandler.checkWarnings()
    }

    private fun checkValidCreationStyles(styles: Collection<StyleProperties>) {
        styles.map { style ->
            style.creationStyle?.let {
                if (!validCreationStrings.contains(it)) {
                    invalidStyleAttribute("creationStyle", validCreationStrings, it)
                    style.creationStyle = "FadeIn"
                }
            }
        }
    }

    private fun checkValidAnimationStyles(styles: Collection<StyleProperties>) {
        styles.map { style ->
            style.animate?.let { animationProperties ->
                animationProperties.animationStyle?.let {
                    if (!validAnimationStrings.contains(it)) {
                        invalidStyleAttribute("animationStyle", validAnimationStrings, it)
                    }
                }
            }
        }
    }

}