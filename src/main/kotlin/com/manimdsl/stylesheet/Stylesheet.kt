package com.manimdsl.stylesheet

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import java.io.File
import java.lang.reflect.Type
import kotlin.system.exitProcess

sealed class StylesheetProperty(open val borderColor: String = "WHITE", open val textColor: String = "WHITE")

data class AnimationProperties(override val borderColor: String, override val textColor: String) : StylesheetProperty()

data class BasicStylesheet(
    override val borderColor: String,
    override val textColor: String,
    val animate: AnimationProperties? = null
) : StylesheetProperty()

fun parseStylesheet(stylesheetPath: String): Map<String, BasicStylesheet> {
    val gson = Gson()
    val type: Type = object : TypeToken<Map<String, BasicStylesheet>>() {}.type
    return try {
        gson.fromJson(File(stylesheetPath).readText(), type)
    } catch (e: JsonSyntaxException) {
        print("Invalid JSON stylesheet: ")
        if(e.message.let { it != null && it.startsWith("duplicate key") }) {
            println(e.message)
        } else {
            println("Could not parse JSON")
        }
        exitProcess(1)
    }
}