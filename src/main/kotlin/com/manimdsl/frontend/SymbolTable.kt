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
    fun addVariable(identifier: String, type: Type) {
        table[identifier] = IdentifierData(type)
    }

    fun replaceDataStructure(identifier: String, type: DataStructureType, internalType: Type) {
        val newType = when (type) {
            is StackType -> StackType(internalType)
        }
        table[identifier] = IdentifierData(newType)
    }

    override fun getTypeOf(identifier: String): Type {
        return table[identifier]?.type?:parent.getTypeOf(identifier)
    }

    private val table: MutableMap<String, IdentifierData> = mutableMapOf()
    private val parent: SymbolTable = SymbolTableRoot
}


