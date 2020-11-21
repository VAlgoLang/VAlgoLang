package com.manimdsl.stylesheet

import com.google.gson.JsonSyntaxException
import com.manimdsl.errorhandling.ErrorHandler
import com.manimdsl.errorhandling.warnings.invalidStyleAttribute
import com.manimdsl.errorhandling.warnings.undeclaredVariableStyleWarning
import com.manimdsl.frontend.SymbolTableVisitor

object StyleSheetValidator {
    private val dataStructureStrings = setOf("Stack", "Array", "Tree")
    private val validCodeTracking = setOf("stepOver", "stepInto")
    private val validCreationStrings =
        setOf("FadeIn", "FadeInFromLarge", "Write", "GrowFromCenter", "ShowCreation", "DrawBorderThenFill")
    private val validPygmentsStyles = setOf(
        "inkpot",
        "solarized-dark",
        "paraiso-dark",
        "vim",
        "fruity",
        "native",
        "monokai"
    )

    private val DEFAULT_PYGMENT_STYLE = "inkpot"

    // Map of valid animations to whether they accept a color as a parameter
    val validAnimationStrings =
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

        // Check syntax highlighting style is correct
        if (!validPygmentsStyles.contains(stylesheet.syntaxHighlightingStyle)) {
            invalidStyleAttribute("syntaxHighlightingStyle", validPygmentsStyles, stylesheet.syntaxHighlightingStyle)
            stylesheet.syntaxHighlightingStyle = DEFAULT_PYGMENT_STYLE
        }

        // Check creation style strings
        checkValidCreationStyles(stylesheet.variables.values)
        checkValidCreationStyles(stylesheet.dataStructures.values)

        // Check animation style strings
        checkValidAnimationStyles(stylesheet.variables.values)
        checkValidAnimationStyles(stylesheet.dataStructures.values)

        ErrorHandler.checkWarnings()

        checkValidPositions(stylesheet.positions.values)
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
                animationProperties.animationStyle?.let { animationString ->
                    if (!validAnimationStrings.contains(animationString)) {
                        invalidStyleAttribute("animationStyle", validAnimationStrings.keys, animationString)
                        // Ignores animation style if invalid and defaults to FadeToColor so that Manim program compiles
                        style.animate.animationStyle = "FadeToColor"
                    }
                }
            }
        }
    }

    private fun checkValidPositions(positions: Collection<PositionProperties>) {
        positions.forEach { position ->
            if (position.width == 0.0 || position.height == 0.0) {
                throw JsonSyntaxException("Missing field entry in position definition")
            }
        }
    }
}
