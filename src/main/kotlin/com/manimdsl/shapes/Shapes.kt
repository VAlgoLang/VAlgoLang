package com.manimdsl.shapes

import java.util.*

interface Shape {

    val text: String
    val classPath: String
    val className: String

    fun generateVariableName(): String
}

data class Rectangle(override val text: String) : Shape {
    override val classPath: String = "pythonLib/rectangle.py"
    override val className: String = "Rectangle_block"

    override fun generateVariableName(): String {
        return UUID.randomUUID().toString()
    }
}
