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
        if(color == null) return null
        return if (color.matches(Regex("#[a-fA-F0-9]{6}"))) {
            // hex value
            "\"${color}\""
        } else {
            // predefined constant
            color.toUpperCase()
        }
    }

}

class AnimationProperties(borderColor: String? = null, textColor: String? = null) :
    StylesheetProperty() {
    override val borderColor: String? = handleColourValue(borderColor)
    override val textColor: String? = handleColourValue(textColor)
    val pointer: Boolean? = null
}

class StyleProperties(
    borderColor: String? = null,
    textColor: String? = null,
    val animate: AnimationProperties? = null
) : StylesheetProperty() {
    override var borderColor: String? = handleColourValue(borderColor)
    override var textColor: String? = handleColourValue(textColor)
}

data class StyleSheetFromJSON(
    val codeTracking: String = "stepInto",
    val hideCode: Boolean = false,
    val variables: Map<String, StyleProperties> = emptyMap(),
    val dataStructures: Map<String, StyleProperties> = emptyMap()
)

class Stylesheet(private val stylesheetPath: String?, private val symbolTableVisitor: SymbolTableVisitor) {
    private val stylesheet: StyleSheetFromJSON

    init {
        stylesheet = if (stylesheetPath != null) {
            val gson = Gson()
            val type: Type = object : TypeToken<StyleSheetFromJSON>() {}.type
            try {
                val parsedStylesheet: StyleSheetFromJSON = gson.fromJson(File(stylesheetPath).readText(), type)
                StyleSheetValidator.validateStyleSheet(parsedStylesheet, symbolTableVisitor)
                parsedStylesheet
            } catch (e: JsonSyntaxException) {
                print("Invalid JSON stylesheet: ")
                if (e.message.let { it != null && it.startsWith("duplicate key") }) {
                    println(e.message)
                } else {
                    println("Could not parse JSON")
                }
                exitProcess(1)
            }
        } else {
            StyleSheetFromJSON()
        }
    }

    fun getStyle(identifier: String, value: ExecValue): StyleProperties {
        val dataStructureStyle =
            stylesheet.dataStructures.getOrDefault(value.toString(), StyleProperties())
        val style = stylesheet.variables.getOrDefault(identifier, dataStructureStyle)

        val newStyle = style merge dataStructureStyle
        val animatedStyle = getAnimatedStyle(identifier, value)
        if (animatedStyle != null) {
            if (animatedStyle.borderColor != null && newStyle.borderColor == null) {
                newStyle.borderColor = "WHITE"
            }
            if (animatedStyle.textColor != null && newStyle.textColor == null) {
                newStyle.textColor = "WHITE"
            }
        }
        return newStyle
    }

    fun getAnimatedStyle(identifier: String, value: ExecValue): AnimationProperties? {
        val dataStructureStyle =
            stylesheet.dataStructures.getOrDefault(value.toString(), StyleProperties())
        val style = stylesheet.variables.getOrDefault(identifier, dataStructureStyle)

        val animationStyle = (style.animate ?: AnimationProperties()) merge (dataStructureStyle.animate ?: AnimationProperties())
        // Return null if there is no style to make sure null checks work throughout executor
        return if (animationStyle == AnimationProperties()) null else animationStyle
    }

    fun getStepIntoIsDefault(): Boolean {
        return stylesheet.codeTracking == "stepInto"
    }

    fun getHideCode(): Boolean {
        return stylesheet.hideCode
    }
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