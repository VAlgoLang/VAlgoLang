package com.manimdsl.linearrepresentation

import com.manimdsl.frontend.NoType
import com.manimdsl.frontend.SymbolTable
import com.manimdsl.shapes.Shape

class VariableNameGenerator(private val symbolTable: SymbolTable) {
    private val prefixCounter: MutableMap<String, Int> = mutableMapOf()

    fun generateShapeName(shape: Shape): String = generateNameFromPrefix(shape.pythonVariablePrefix)

    fun generateNameFromPrefix(prefix: String): String {
        var count = prefixCounter.getOrDefault(prefix, 0)
        while (symbolTable.getTypeOf("$prefix$count") !is NoType) {
            count++
        }
        prefixCounter[prefix] = count + 1
        return if (count > 0) "$prefix$count" else prefix
    }
}