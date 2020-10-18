package com.manimdsl.frontend

data class IdentifierData(val type: Type)

interface SymbolTable {
    fun getTypeOf(identifier: String): Type
}

// Visitor
class SymbolTableVisitor {
    private val scopes = mutableListOf<SymbolTableNode>(GlobalScopeSymbolTable())
    private var currentScope: SymbolTableNode = scopes[0]

    fun getTypeOf(identifier: String): Type = currentScope.getTypeOf(identifier)

    fun addVariable(identifier: String, type: Type) {
        currentScope.table[identifier] = IdentifierData(type)
    }

    fun enterScope(): Int {
        val newScope = SymbolTableNode(currentScope, scopes.size)
        scopes.add(newScope)
        currentScope = newScope
        // Returning index of this new symbol table (used in AST)
        return newScope.id
    }

    fun leaveScope() {
        val currentScopeParent = currentScope.parent
        if (currentScopeParent is SymbolTableNode) {
            currentScope = currentScopeParent
        }
    }

    fun goToScope(scopeID: Int) {
        if (scopeID in scopes.indices) {
            currentScope = scopes[scopeID]
        }
    }

    fun getCurrentScopeID(): Int = currentScope.id
}


object SymbolTableRoot : SymbolTable {
    override fun getTypeOf(identifier: String): Type {
        return ErrorType
    }
}

open class SymbolTableNode(val parent: SymbolTable, val id: Int): SymbolTable {
    val table: MutableMap<String, IdentifierData> = mutableMapOf()

    override fun getTypeOf(identifier: String): Type {
        return table[identifier]?.type ?: parent.getTypeOf(identifier)
    }
}

class GlobalScopeSymbolTable: SymbolTableNode(SymbolTableRoot, 0)

