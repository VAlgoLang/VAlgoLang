package com.valgolang.stylesheet

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import com.valgolang.frontend.SymbolTableVisitor
import com.valgolang.runtime.ExecValue
import java.io.File
import java.lang.reflect.Type
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.system.exitProcess

/**
 * Abstract stylesheet property
 *
 * @constructor Create empty Stylesheet property
 */
sealed class StylesheetProperty {
    abstract val borderColor: String?
    abstract val textColor: String?

    /**
     * Handles color value to be passed to Python
     *
     * @param color
     * @return [color] in a form that can be used by Manim, either as a hex string enclosed in quotation marks or
     * one of the upper-case pre-defined color constants defined in Manim
     */
    fun handleColourValue(color: String?): String? {
        if (color == null) return null
        return if (color.matches(Regex("#[a-fA-F0-9]{6}"))) {
            // hex value
            "\"${color}\""
        } else {
            // predefined constant
            color.toUpperCase()
        }
    }
}

/**
 * Animation properties for when a data structure is being animated
 * (for example, during push/pop operations on a stack)
 *
 * @property borderColor
 * @property textColor
 * @property pointer
 * @property highlight
 * @property animationStyle
 * @property animationTime
 * @property render
 * @constructor Create empty Animation properties
 */
open class AnimationProperties(
    override val borderColor: String? = null,
    override val textColor: String? = null,
    open val pointer: Boolean? = null,
    open val highlight: String? = "YELLOW",
    open var animationStyle: String? = null,
    open var animationTime: Double? = null,
    open val render: Boolean? = true,
) : StylesheetProperty()

/**
 * Default animation properties
 * (to fall back on if any have not been defined by the user)
 *
 * @property textColor
 * @property pointer
 * @property highlight
 * @property animationStyle
 * @property animationTime
 * @property render
 * @constructor Create empty Default animation properties
 */
data class DefaultAnimationProperties(
    override val textColor: String? = "YELLOW",
    override val pointer: Boolean = true,
    override val highlight: String? = "YELLOW",
    override var animationStyle: String? = "FadeToColor",
    override var animationTime: Double? = null,
    override val render: Boolean? = true,
) : AnimationProperties()

/**
 * Style properties for when a data structure is being created or is "at rest" on screen
 *
 * @property borderColor
 * @property textColor
 * @property showLabel
 * @property creationStyle
 * @property creationTime
 * @property animate
 * @property duration
 * @constructor Create empty Style properties
 */
open class StyleProperties(
    override var borderColor: String? = null,
    override var textColor: String? = null,
    val showLabel: Boolean? = null,
    var creationStyle: String? = null,
    var creationTime: Double? = null,
    val animate: AnimationProperties? = null,
    val duration: Int? = null
) : StylesheetProperty()

/**
 * Default style properties
 * (to fall back on if any have not been defined by the user)
 *
 * @property textColor
 * @property borderColor
 * @constructor Create empty Default style properties
 */
data class DefaultStyleProperties(
    override var textColor: String? = "YELLOW",
    override var borderColor: String? = "BLUE"
) : StyleProperties()

/**
 * Position properties
 * (to assign which area on screen an object can occupy in the final animation)
 *
 * @property x: x-coordinate of bottom-left corner
 * @property y: y-coordinate of bottom-left corner
 * @property width: width of assigned area
 * @property height: height of assigned area
 * @constructor Create empty Position properties
 */
data class PositionProperties(
    val x: Double,
    val y: Double,
    val width: Double,
    val height: Double,
) {
    /**
     * Calculate Manim coordinates of assigned area based on properties in constructor
     *
     * @return list of 4 coordinate pairs in the following order:
     * upper-left, upper-right, bottom-left and bottom-right
     * (UL, UR, LL, LR)
     */
    fun calculateManimCoord(): List<Pair<Double, Double>> {
        return listOf(Pair(x, y + height), Pair(x + width, y + height), Pair(x, y), Pair(x + width, y))
    }
}

/**
 * Stylesheet generated by parsing JSON file
 *
 * @property codeTracking: whether to step into or step over code
 * @property hideCode: whether to hide the code and variable blocks
 * @property hideVariables: whether to hide the variable block
 * @property syntaxHighlightingOn: whether syntax highlighting in the code block should be switched on
 * @property syntaxHighlightingStyle: which Pygments style should be applied to the code in the code block
 * @property displayNewLinesInCode: whether new lines \n should be displayed in the code block
 * @property tabSpacing: how many tabs should be used to indent code in the code block
 * @property subtitles: style properties for subtitles
 * @property variables: style properties for variables in program
 * @property dataStructures: style properties for data structures in program
 * @property positions: positions of each data structure as well as code, variable and subtitle blocks in final animation
 * @constructor Create empty Stylesheet
 */
data class StylesheetFromJSON(
    val codeTracking: String = "stepInto",
    val hideCode: Boolean = false,
    val hideVariables: Boolean = false,
    val syntaxHighlightingOn: Boolean = true,
    var syntaxHighlightingStyle: String = "inkpot",
    val displayNewLinesInCode: Boolean = true,
    val tabSpacing: Int = 2,
    val subtitles: StyleProperties = StyleProperties(),
    val variables: Map<String, StyleProperties> = emptyMap(),
    val dataStructures: Map<String, StyleProperties> = emptyMap(),
    val positions: Map<String, PositionProperties> = emptyMap()
)

class Stylesheet(private val stylesheetPath: String?, private val symbolTableVisitor: SymbolTableVisitor) {
    private val stylesheet: StylesheetFromJSON

    /**
     * Uses Gson to read JSON file into a StylesheetFromJSON
     */
    init {
        stylesheet = if (stylesheetPath != null) {
            val gson = Gson()
            val type: Type = object : TypeToken<StylesheetFromJSON>() {}.type
            try {
                val parsedStylesheet: StylesheetFromJSON = gson.fromJson(File(stylesheetPath).readText(), type)
                StylesheetValidator.validateStyleSheet(parsedStylesheet, symbolTableVisitor)
                parsedStylesheet
            } catch (e: JsonSyntaxException) {
                print("Invalid JSON stylesheet: ")
                if (e.message.let {
                    it != null && (
                        it.startsWith("duplicate key") || it.startsWith("Missing field") || it.startsWith(
                                "Cannot"
                            )
                        )
                }
                ) {
                    println(e.message)
                } else {
                    println("Could not parse JSON")
                }
                exitProcess(1)
            }
        } else {
            StylesheetFromJSON()
        }
    }

    /**
     * Get style, falling back on default style properties where necessary
     *
     * @param identifier
     * @param value
     * @return style properties for [identifier]
     */
    fun getStyle(identifier: String, value: ExecValue): StyleProperties {

        val dataStructureStyle =
            stylesheet.dataStructures.getOrDefault(value.name, StyleProperties())
        val style = stylesheet.variables.getOrDefault(identifier, dataStructureStyle)

        val styleProperties = style merge dataStructureStyle
        return styleProperties merge DefaultStyleProperties()
    }

    /**
     * Get animated style, falling back on default animation properties where necessary
     *
     * @param identifier
     * @param value
     * @return animation properties for [identifier]
     */
    fun getAnimatedStyle(identifier: String, value: ExecValue): AnimationProperties {
        val dataStructureStyle =
            stylesheet.dataStructures.getOrDefault(value.name, StyleProperties()) merge StyleProperties(
                borderColor = "BLUE",
                textColor = "WHITE",
                animate = AnimationProperties(),
            )
        val style = stylesheet.variables.getOrDefault(identifier, dataStructureStyle)
        val animationStyle = (
            style.animate
                ?: AnimationProperties()
            ) merge (
            dataStructureStyle.animate
                ?: AnimationProperties()
            )

        return (animationStyle merge DefaultAnimationProperties())
    }

    /** Methods for accessing style attributes **/

    fun getSubtitleStyle(): StyleProperties = stylesheet.subtitles

    fun userDefinedPositions(): Boolean = stylesheet.positions.isNotEmpty()

    fun getPositions(): Map<String, PositionProperties> = stylesheet.positions

    fun getPosition(identifier: String): PositionProperties? = stylesheet.positions[identifier]

    fun getStepIntoIsDefault(): Boolean = stylesheet.codeTracking == "stepInto"

    fun getHideCode(): Boolean = stylesheet.hideCode

    fun getHideVariables(): Boolean = stylesheet.hideVariables

    fun getSyntaxHighlighting(): Boolean = stylesheet.syntaxHighlightingOn

    fun getSyntaxHighlightingStyle(): String = stylesheet.syntaxHighlightingStyle

    fun getDisplayNewLinesInCode(): Boolean = stylesheet.displayNewLinesInCode

    fun getTabSpacing(): Int = stylesheet.tabSpacing

    fun renderDataStructure(identifier: String) =
        !stylesheet.positions.containsKey(identifier) || stylesheet.positions[identifier]!!.height != 0.0 || stylesheet.positions[identifier]!!.width != 0.0
}

/**
 * Infix function that merges two instances of a generic class [T]
 *
 * @param other
 * @return merged form of [this] and [other], where the properties of [this] take precedence over [other]
 *
 * Credit to https://stackoverflow.com/questions/44566607/combining-merging-data-classes-in-kotlin/44570679#44570679
 */
inline infix fun <reified T : Any> T.merge(other: T): T {
    val propertiesByName = T::class.declaredMemberProperties.associateBy { it.name }
    val primaryConstructor = T::class.primaryConstructor
        ?: throw IllegalArgumentException("merge type must have a primary constructor")
    val args = primaryConstructor.parameters.associateWith { parameter ->
        val property = propertiesByName[parameter.name]
            ?: throw IllegalStateException("no declared member property found with name '${parameter.name}'")
        (property.get(this) ?: property.get(other))
    }
    return primaryConstructor.callBy(args)
}
