package com.manimdsl

import com.manimdsl.linearrepresentation.*

class ManimWriter(private val linearRepresentation: List<ManimInstr>) {

    fun build(): String {
        var pythonCode = initialPythonSetup()

        val constructCodeBlock = mutableListOf<String>()

        val shapeClassPaths = mutableSetOf<String>()
        var executed = false
        linearRepresentation.forEach {
            when (it) {
                is NewMObject -> {
                    shapeClassPaths.add(it.shape.classPath)
                }
                is CodeBlock -> {
                    shapeClassPaths.add("python/code_block.py")
                }
                is InitStructure -> {
                    shapeClassPaths.add("python/init_structure.py")
                }
            }
            if (it is MoveToLine && !executed) {
                constructCodeBlock.add(printWithIndent(2, listOf("# Moves the current line pointer to line ${it.lineNumber}")))
                executed = true
            }
            constructCodeBlock.add(printWithIndent(2, it.toPython()))
        }

        pythonCode += constructCodeBlock.joinToString("\n") + "\n"

        pythonCode += printWithIndent(1, addUtilityFunctions()) + "\n"

        pythonCode += printWithIndent(
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
             
            class Main(Scene):
                def construct(self):

        """.trimIndent()
    }

    private fun printWithIndent(identSize: Int, lines: List<String>): String {
        return lines.map { line -> "${"    ".repeat(identSize)}${line}" }.joinToString("\n")
    }


}