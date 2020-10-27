package com.manimdsl.animation

import com.manimdsl.linearrepresentation.*

class ManimWriter(private val linearRepresentation: List<ManimInstr>) {

    fun build(): String {
        var pythonCode = initialPythonSetup()

        val constructCodeBlock = mutableListOf<String>()

        val shapeClassPaths = mutableSetOf<String>()
        var executed = false
        linearRepresentation.forEach {
            when (it) {
                is DataStructureMObject -> {
                    shapeClassPaths.addAll(listOf("python/data_structure.py", it.shape.classPath))
                }
                is MObject -> {
                    shapeClassPaths.add(it.shape.classPath)
                }
                is VariableBlock -> {
                    shapeClassPaths.add(it.shape.classPath)
                }
            }
            if (it is MoveToLine && !executed) {
                constructCodeBlock.add(printWithIndent(2, listOf("# Moves the current line pointer to line ${it.lineNumber}")))
                executed = true
            }
            constructCodeBlock.add(printWithIndent(2, it.toPython()))
        }
        pythonCode += constructCodeBlock.joinToString("\n") + "\n"

        pythonCode += "\n" + printWithIndent(1, addUtilityFunctions())

        pythonCode += "\n" + printWithIndent(
            0,
            shapeClassPaths.map { getResourceAsText(it) })

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
            from manimlib.imports import *
            from abc import ABC, abstractmethod
             
            class Main(Scene):
                code_start = 0
                code_end = 5
                def construct(self):

        """.trimIndent()
    }

    private fun printWithIndent(identSize: Int, lines: List<String>): String {
        return lines.map { line -> "${"    ".repeat(identSize)}${line}" }.joinToString("\n")
    }


}