package com.valgolang.animation

import com.manimdsl.linearrepresentation.DataStructureMObject
import com.manimdsl.linearrepresentation.MObject
import com.manimdsl.linearrepresentation.ManimInstr
import com.manimdsl.linearrepresentation.ShapeWithBoundary
import com.manimdsl.linearrepresentation.datastructures.binarytree.NodeStructure

/**
 * Manim writer that generates the Python code written using the manim library
 *
 * @property linearRepresentation: list of all the instructions to be converted to Python code
 * @constructor Creates a new Manim writer
 */
class ManimWriter(private val linearRepresentation: List<ManimInstr>) {

    /**
     * Converts linear representation to Python code written in the format compatible with manim.
     * Also copies in the utility functions and prebuilt Python libraries that are used by the linear representation.
     *
     * @return string containing all the well-formatted Python code
     */
    fun build(): String {
        var pythonCode = initialPythonSetup()

        val constructCodeBlock = mutableListOf<String>()

        val shapeClassPaths = mutableSetOf<String>()
        linearRepresentation.forEach {
            when (it) {
                is NodeStructure -> {
                    shapeClassPaths.addAll(listOf("python/data_structure.py", "python/rectangle.py", it.classPath))
                }
                is DataStructureMObject -> {
                    shapeClassPaths.addAll(listOf("python/data_structure.py", "python/rectangle.py", it.classPath))
                }
                is MObject -> {
                    if (it is ShapeWithBoundary) {
                        shapeClassPaths.add(it.classPath)
                    }
                }
            }
            constructCodeBlock.add(printWithIndent(2, it.toPython()))
        }
        pythonCode += constructCodeBlock.joinToString("\n") + "\n"

        pythonCode += "\n" + printWithIndent(1, addUtilityFunctions())

        pythonCode += "\n" + printWithIndent(
            0,
            shapeClassPaths.map { "\n" + getResourceAsText(it) }
        )

        return pythonCode
    }

    private fun addUtilityFunctions(): List<String> {
        return getResourceAsText("python/util.py").split("\n")
    }

    private fun getResourceAsText(path: String): String {
        return ClassLoader.getSystemResource(path).readText()
    }

    private fun initialPythonSetup(): String {
        return """
            import tempfile
            from abc import ABC, abstractmethod
            from manimlib.imports import *
             
            class Main(Scene):
                code_start = 0
                code_end = 10
                line_spacing = 0.1
                time_objects = []

                def construct(self):

        """.trimIndent()
    }

    private fun printWithIndent(identSize: Int, lines: List<String>): String {
        return lines.map { line -> "${"    ".repeat(identSize)}$line" }.joinToString("\n")
    }
}
