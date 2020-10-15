package com.manimdsl

import com.manimdsl.linearrepresentation.CodeBlock
import com.manimdsl.linearrepresentation.InitStructure
import com.manimdsl.linearrepresentation.ManimInstr
import com.manimdsl.linearrepresentation.NewMObject
import java.io.File

class ManimWriter(private val linearRepresentation: List<ManimInstr>) {

    fun build(): String {
        var pythonCode = initialPythonSetup()

        val constructCodeBlock = mutableListOf<String>()

        val shapeClassPaths = mutableSetOf<String>()

        linearRepresentation.forEach {
            when (it) {
                is NewMObject -> {
                    shapeClassPaths.add(it.shape.classPath)
                }
                is CodeBlock -> {
                    shapeClassPaths.add("pythonLib/code_block.py")
                }
                is InitStructure -> {
                    shapeClassPaths.add("pythonLib/init_structure.py")
                }
            }
            constructCodeBlock.add(printWithIndent(2, it.toPython()))
        }

        pythonCode += constructCodeBlock.joinToString("\n") + "\n"

        pythonCode += printWithIndent(1, addUtilityFunctions()) + "\n"

        pythonCode += printWithIndent(0, shapeClassPaths.map { File(it).readText() })

        return pythonCode
    }

    private fun addUtilityFunctions(): List<String> {
        return File("pythonLib/util.py").readLines()
    }

    private fun initialPythonSetup(): String {
        return """
            from manimlib.imports import *
             
            class Main(Scene):
                def construct(self):

        """.trimIndent()
    }

    fun printWithIndent(identSize: Int, lines: List<String>): String {
        return lines.map { line ->  "${"    ".repeat(identSize)}${line}" }.joinToString("\n")
    }






}