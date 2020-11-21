package com.manimdsl.linearrepresentation

import com.manimdsl.frontend.ErrorType
import com.manimdsl.frontend.SymbolTableVisitor
import com.manimdsl.shapes.Shape

interface NameGenerator {
    fun generateShapeName(shape: Shape): String

    fun generateNameFromPrefix(prefix: String): String
}

class VariableNameGenerator(private val symbolTableVisitor: SymbolTableVisitor) : NameGenerator {
    private val prefixCounter: MutableMap<String, Int> = mutableMapOf()

    override fun generateShapeName(shape: Shape): String = generateNameFromPrefix(shape.pythonVariablePrefix)

    override fun generateNameFromPrefix(prefix: String): String {
        var count = prefixCounter.getOrDefault(prefix, 0)
        while (symbolTableVisitor.getTypeOf("$prefix$count") !is ErrorType) {
            count++
        }
        prefixCounter[prefix] = count + 1
        return if (count > 0) "$prefix$count" else prefix
    }
}

object DummyNameGenerator : NameGenerator {
    override fun generateShapeName(shape: Shape): String {
        return "dummy"
    }

    override fun generateNameFromPrefix(prefix: String): String {
        return "dummy"
    }
}
