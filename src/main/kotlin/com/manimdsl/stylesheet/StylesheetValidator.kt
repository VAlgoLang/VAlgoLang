package com.manimdsl.stylesheet

import com.google.gson.JsonSyntaxException
import com.manimdsl.errorhandling.ErrorHandler
import com.manimdsl.errorhandling.warnings.invalidStyleAttributeWarning
import com.manimdsl.errorhandling.warnings.undeclaredVariableStyleWarning
import com.manimdsl.frontend.SymbolTableVisitor

/**
 * Stylesheet validator that checks whether inputs to stylesheet JSON are valid and throws errors/warnings otherwise
 *
 * @constructor Create empty Stylesheet validator
 */
object StylesheetValidator {
    /** Sets of acceptable inputs **/
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

    /** Map of valid animations to whether they accept a color as a parameter **/
    val validAnimationStrings =
        mapOf(
            "FadeToColor" to true,
            "Indicate" to true,
            "ApplyWave" to false,
            "WiggleOutThenIn" to false,
            "CircleIndicate" to true,
            "TurnInsideOut" to false
        )

    /** Default constants **/
    private val DEFAULT_PYGMENT_STYLE = "inkpot"
    private val DEFAULT_CREATION_STYLE = "FadeIn"
    private val DEFAULT_ANIMATION_STYLE = "FadeToColor"

    /** Minimum height constraint constants **/
    private val MIN_CODE_BLOCK_HEIGHT = 1
    private val MIN_VARIABLE_BLOCK_HEIGHT = 2

    /**
     * Validate style sheet
     *
     * @param stylesheet
     * @param symbolTable
     */
    fun validateStyleSheet(stylesheet: StylesheetFromJSON, symbolTable: SymbolTableVisitor) {
        /** Check whether variables in stylesheet have been declared in program **/
        stylesheet.variables.keys.forEach {
            if (!(symbolTable.getVariableNames().contains(it))) {
                undeclaredVariableStyleWarning(it)
            }
        }

        /** Check whether data structures in stylesheet are valid **/
        stylesheet.dataStructures.keys.forEach {
            if (!(dataStructureStrings.contains(it))) {
                invalidStyleAttributeWarning("dataStructures", dataStructureStrings, it)
            }
        }

        /** Check whether code tracking string is valid **/
        if (!validCodeTracking.contains(stylesheet.codeTracking)) {
            // throw warning
            invalidStyleAttributeWarning("codeTracking", validCodeTracking, stylesheet.codeTracking)
        }

        /** Check syntax highlighting style is valid **/
        if (!validPygmentsStyles.contains(stylesheet.syntaxHighlightingStyle)) {
            invalidStyleAttributeWarning("syntaxHighlightingStyle", validPygmentsStyles, stylesheet.syntaxHighlightingStyle)
            stylesheet.syntaxHighlightingStyle = DEFAULT_PYGMENT_STYLE
        }

        /** Check creation style strings **/
        checkValidCreationStyles(stylesheet.variables.values)
        checkValidCreationStyles(stylesheet.dataStructures.values)

        /** Check animation style strings **/
        checkValidAnimationStyles(stylesheet.variables.values)
        checkValidAnimationStyles(stylesheet.dataStructures.values)

        ErrorHandler.checkWarnings()

        /** JSON exceptions **/
        checkValidPositions(stylesheet.positions.values)
        checkMinHeightOfCodeAndVariableBlocks(stylesheet.positions)
    }

    /**
     * Check whether creation styles defined by creationStyle attribute are valid
     * Default to a valid style if supplied with invalid style in stylesheet
     *
     * @param styles
     */
    private fun checkValidCreationStyles(styles: Collection<StyleProperties>) {
        styles.forEach { style ->
            style.creationStyle?.let {
                if (!validCreationStrings.contains(it)) {
                    invalidStyleAttributeWarning("creationStyle", validCreationStrings, it)
                    style.creationStyle = DEFAULT_CREATION_STYLE
                }
            }
        }
    }

    /**
     * Check whether animation styles defined by animationStyle attribute are valid
     * Default to a valid style if supplied with invalid style in stylesheet
     *
     * @param styles
     */
    private fun checkValidAnimationStyles(styles: Collection<StyleProperties>) {
        styles.forEach { style ->
            style.animate?.let { animationProperties ->
                animationProperties.animationStyle?.let { animationString ->
                    if (!validAnimationStrings.contains(animationString)) {
                        invalidStyleAttributeWarning("animationStyle", validAnimationStrings.keys, animationString)
                        style.animate.animationStyle = DEFAULT_ANIMATION_STYLE
                    }
                }
            }
        }
    }

    /**
     * Check positions of objects have been defined in a valid way and throw JSON exception otherwise
     *
     * @param positions
     */
    private fun checkValidPositions(positions: Collection<PositionProperties>) {
        positions.forEach { position ->
            if (position.width != position.height && (position.width == 0.0 || position.height == 0.0)) {
                throw JsonSyntaxException("Missing field entry in position definition")
            }
        }
    }

    /**
     * Check supplied heights of code and variable blocks are greater than the minimums specified
     * Throw JSON exception otherwise to prevent text inside blocks from overlapping with other objects on screen
     *
     * @param positions
     */
    private fun checkMinHeightOfCodeAndVariableBlocks(positions: Map<String, PositionProperties>) {
        val codeBlockPosition = positions["_code"]
        val variableBlockPosition = positions["_variables"]
        if (codeBlockPosition != null && codeBlockPosition.height < MIN_CODE_BLOCK_HEIGHT) {
            throw JsonSyntaxException("Cannot create code block with height smaller than 1")
        }
        if (variableBlockPosition != null && variableBlockPosition.height < MIN_VARIABLE_BLOCK_HEIGHT) {
            throw JsonSyntaxException("Cannot create variable block with height smaller than 2")
        }
    }
}
