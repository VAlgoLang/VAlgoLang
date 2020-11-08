package com.manimdsl.stylesheet

import com.manimdsl.errorhandling.ErrorHandler
import com.manimdsl.errorhandling.warnings.animationStyleDoesNotAcceptColorWarning
import com.manimdsl.errorhandling.warnings.invalidStyleAttribute
import com.manimdsl.errorhandling.warnings.undeclaredVariableStyleWarning
import com.manimdsl.frontend.SymbolTableVisitor

object StyleSheetValidator {
    private val dataStructureStrings = setOf("Stack", "Array")
    private val validCodeTracking = setOf("stepOver", "stepInto")
    private val validCreationStrings =
        setOf("FadeIn", "FadeInFromLarge", "Write", "GrowFromCenter", "ShowCreation", "DrawBorderThenFill")

    // Map of valid animations to whether they accept a color
    private val validAnimationStrings =
        mapOf(
            "FadeToColor" to true,
            "Indicate" to true,
            "ApplyWave" to false,
            "WiggleOutThenIn" to false,
            "CircleIndicate" to true,
            "TurnInsideOut" to false
        )


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
        styles.forEach { style ->
            style.creationStyle?.let {
                if (!validCreationStrings.contains(it)) {
                    invalidStyleAttribute("creationStyle", validCreationStrings, it)
                    // Ignores creation style if invalid and defaults to FadeIn so that Manim program compiles
                    style.creationStyle = "FadeIn"
                }
            }
        }
    }

    private fun checkValidAnimationStyles(styles: Collection<StyleProperties>) {
        styles.forEach { style ->
            style.animate?.let { animationProperties ->
                animationProperties.animationString?.let { animationString ->
                    if (validAnimationStrings.contains(animationString)) {
                        if (!validAnimationStrings.getOrDefault(
                                animationString,
                                false
                            ) && animationProperties.textColor != null
                        ) {
                            val validAnimationStylesWithColor = validAnimationStrings.filter { it.value }.keys
                            animationStyleDoesNotAcceptColorWarning(animationString, validAnimationStylesWithColor)
                        }
                    } else {
                        invalidStyleAttribute("animationStyle", validAnimationStrings.keys, animationString)
                        // Ignores animation style if invalid and defaults to FadeToColor so that Manim program compiles
                        style.animate.animationString = "FadeToColor"
                    }
                }
            }
        }
    }

}