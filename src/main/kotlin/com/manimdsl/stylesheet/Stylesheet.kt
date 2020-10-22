package com.manimdsl.stylesheet

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import java.io.File
import java.lang.reflect.Type
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.system.exitProcess

sealed class StylesheetProperty(open val borderColor: String? = null, open val textColor: String? = null)

data class AnimationProperties(override val borderColor: String? = null, override val textColor: String? = null) : StylesheetProperty()

data class BasicStylesheet(
        val borderColor: String? = null,
        val textColor: String? = null,
        val font: String? = null
//        val animate: AnimationProperties? = null
)

fun parseStylesheet(stylesheetPath: String): Map<String, BasicStylesheet> {
    val gson = Gson()
    val type: Type = object : TypeToken<Map<String, BasicStylesheet>>() {}.type
    return try {
        gson.fromJson(File(stylesheetPath).readText(), type)
    } catch (e: JsonSyntaxException) {
        print("Invalid JSON stylesheet: ")
        if (e.message.let { it != null && it.startsWith("duplicate key") }) {
            println(e.message)
        } else {
            println("Could not parse JSON")
        }
        exitProcess(1)
    }
}

// Given a stylesheet map, variable name and type, return appropriate styling
fun getStyle(stylesheetMap: Map<String, BasicStylesheet>, identifier: String, type: com.manimdsl.frontend.Type): BasicStylesheet {
    val dataStructureStyle = stylesheetMap.getOrDefault(type.toString(), BasicStylesheet())
    val style = stylesheetMap.getOrDefault(identifier, dataStructureStyle)
    return style merge dataStructureStyle
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