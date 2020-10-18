package com.manimdsl.frontend

data class IdentifierData(val type: Type)

class SymbolTableVisitor {
    private val scopes = mutableListOf<SymbolTable>(GlobalScopeSymbolTable())
    private var currentScope: SymbolTable = scopes[0]

    fun getTypeOf(identifier: String): Type {
        return currentScope.table[identifier]?.type ?: (currentScope.parent?.table?.get(identifier)?.type ?: ErrorType)
    }

    fun addVariable(identifier: String, type: Type) {
        currentScope.table[identifier] = IdentifierData(type)
    }

    fun enterScope(): Int {
        val newScope = SymbolTable(currentScope, scopes.size)
        scopes.add(newScope)
        currentScope = newScope
        // Returning index of this new symbol table (used in AST)
        return newScope.id
    }

    fun leaveScope() {
        currentScope = currentScope.parent?: scopes[0]
    }

    fun goToScope(scopeID: Int) {
        if (scopeID in scopes.indices) {
            currentScope = scopes[scopeID]
        }
    }

    fun getCurrentScopeID(): Int = currentScope.id
}

class GlobalScopeSymbolTable : SymbolTable(null, 0)

open class SymbolTable(val parent: SymbolTable?, val id: Int) {
    val table: MutableMap<String, IdentifierData> = mutableMapOf()
}
