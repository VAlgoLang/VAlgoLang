package com.manimdsl.shapes

interface Shape {

    val text: String
    val classPath: String
    val className: String
    val pythonVariablePrefix: String
}

data class Rectangle(override val text: String) : Shape {
    override val classPath: String = "pythonLib/rectangle.py"
    override val className: String = "Rectangle_block"
    override val pythonVariablePrefix: String = "rectangle"
}
