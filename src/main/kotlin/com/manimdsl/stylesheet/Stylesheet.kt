package com.manimdsl.stylesheet

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import com.manimdsl.frontend.SymbolTableVisitor
import com.manimdsl.runtime.ExecValue
import java.io.File
import java.lang.reflect.Type
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.system.exitProcess

sealed class StylesheetProperty {
    abstract val borderColor: String?
    abstract val textColor: String?

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

open class AnimationProperties(
    override val borderColor: String? = null,
    override val textColor: String? = null,
    open val pointer: Boolean? = null,
    open val highlight: String? = "YELLOW",
    open var animationStyle: String? = null,
    open var animationTime: Double? = null,
    open val render: Boolean? = true,
) : StylesheetProperty()

data class DefaultAnimationProperties(
    override val borderColor: String? = "RED",
    override val textColor: String? = "YELLOW",
    override val pointer: Boolean = true,
    override val highlight: String? = "YELLOW",
    override var animationStyle: String? = "FadeToColor",
    override var animationTime: Double? = 1.0,
    override val render: Boolean? = true,
) : AnimationProperties()

data class StyleProperties(
    override var borderColor: String? = null,
    override var textColor: String? = null,
    val showLabel: Boolean? = null,
    var creationStyle: String? = null,
    var creationTime: Double? = null,
    val animate: AnimationProperties? = null,
    val render: Boolean? = true,
) : StylesheetProperty()

data class PositionProperties(
    val x: Double,
    val y: Double,
    val width: Double,
    val height: Double,
)

data class StylesheetFromJSON(
    val codeTracking: String = "stepInto",
    val hideCode: Boolean = false,
    val syntaxHighlightingOn: Boolean = true,
    var syntaxHighlightingStyle: String = "inkpot",
    val displayNewLinesInCode: Boolean = true,
    val tabSpacing: Int = 2,
    val variables: Map<String, StyleProperties> = emptyMap(),
    val dataStructures: Map<String, StyleProperties> = emptyMap(),
    val positions: Map<String, PositionProperties> = emptyMap()
)

class Stylesheet(private val stylesheetPath: String?, private val symbolTableVisitor: SymbolTableVisitor) {
    private val stylesheet: StylesheetFromJSON

    init {
        stylesheet = if (stylesheetPath != null) {
            val gson = Gson()
            val type: Type = object : TypeToken<StylesheetFromJSON>() {}.type
            try {
                val parsedStylesheet: StylesheetFromJSON = gson.fromJson(File(stylesheetPath).readText(), type)
                StyleSheetValidator.validateStyleSheet(parsedStylesheet, symbolTableVisitor)
                parsedStylesheet
            } catch (e: JsonSyntaxException) {
                print("Invalid JSON stylesheet: ")
                if (e.message.let { it != null && (it.startsWith("duplicate key") || it.startsWith("Missing field")) }) {
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

    fun getStyle(identifier: String, value: ExecValue): StyleProperties {


        val dataStructureStyle =
            stylesheet.dataStructures.getOrDefault(value.name, StyleProperties())
        val style = stylesheet.variables.getOrDefault(identifier, dataStructureStyle)

        val styleProperties = style merge dataStructureStyle
        return styleProperties
    }

    fun getAnimatedStyle(identifier: String, value: ExecValue): AnimationProperties? {
        val dataStructureStyle =
            stylesheet.dataStructures.getOrDefault(value.toString(), StyleProperties()) merge StyleProperties(
                borderColor = "BLUE",
                textColor = "WHITE",
                animate = DefaultAnimationProperties(),
                render = true
            )
        val style = stylesheet.variables.getOrDefault(identifier, dataStructureStyle)
        val animationStyle = (
            style.animate
                ?: AnimationProperties()
            ) merge (
            dataStructureStyle.animate
                ?: AnimationProperties()
            )

        // Returns null if there is no style to make sure null checks work throughout executor
        return (animationStyle merge DefaultAnimationProperties())
    }

    fun userDefinedPositions(): Boolean = stylesheet.positions.isNotEmpty()

    fun getPositions(): Map<String, PositionProperties> = stylesheet.positions

    fun getPosition(identifier: String): PositionProperties? = stylesheet.positions[identifier]

    fun getStepIntoIsDefault(): Boolean = stylesheet.codeTracking == "stepInto"

    fun getHideCode(): Boolean = stylesheet.hideCode

    fun getSyntaxHighlighting(): Boolean = stylesheet.syntaxHighlightingOn

    fun getSyntaxHighlightingStyle(): String = stylesheet.syntaxHighlightingStyle

    fun getDisplayNewLinesInCode(): Boolean = stylesheet.displayNewLinesInCode

    fun getTabSpacing(): Int = stylesheet.tabSpacing
}

// Credit to https://stackoverflow.com/questions/44566607/combining-merging-data-classes-in-kotlin/44570679#44570679
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
