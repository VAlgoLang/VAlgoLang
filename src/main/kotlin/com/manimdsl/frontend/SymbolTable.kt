package com.manimdsl.frontend

data class IdentifierData(val type: Type)

interface SymbolTable {
    fun getTypeOf(identifier: String): Type
}

object SymbolTableRoot: SymbolTable {
    override fun getTypeOf(identifier: String): Type {
        return NoType
    }
}

class SymbolTableNode: SymbolTable {

    // we've already done semantic analysis
    fun addDeclaration(identifier: String, type: Type) {
        table[identifier] = IdentifierData(type)
    }

    override fun getTypeOf(identifier: String): Type {
        return table[identifier]?.type?:parent.getTypeOf(identifier)
    }

    private val table: MutableMap<String, IdentifierData> = mutableMapOf()
    private val parent: SymbolTable = SymbolTableRoot
}


