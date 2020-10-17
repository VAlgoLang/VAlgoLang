package com.manimdsl.frontend

data class IdentifierData(val type: Type)

class SymbolTable {
    private val scopes = mutableListOf<ScopedSymbolTable>(GlobalScopeSymbolTable())
    private var currentScope: ScopedSymbolTable = scopes[0]

    fun getTypeOf(identifier: String): Type = currentScope.getTypeOf(identifier)

    fun addVariable(identifier: String, type: Type) {
        currentScope.addVariable(identifier, type)
    }

    fun enterScope(): Int {
        val newScope = ScopedSymbolTable(currentScope)
        scopes.add(newScope)
        currentScope = newScope
        // Returning index of this new symbol table (used in AST)
        return scopes.size - 1
    }

    fun leaveScope() {
        currentScope = currentScope.parent?: scopes[0]
    }

    fun goToScope(scopeID: Int) {
        if (scopeID in scopes.indices) {
            currentScope = scopes[scopeID]
        }
    }

    fun getCurrentScopeID(): Int = scopes.indexOf(currentScope)
}

class GlobalScopeSymbolTable: ScopedSymbolTable(null)

open class ScopedSymbolTable(val parent: ScopedSymbolTable?) {

    // we've already done semantic analysis
    fun addVariable(identifier: String, type: Type) {
        table[identifier] = IdentifierData(type)
    }

    fun getTypeOf(identifier: String): Type {
        return table[identifier]?.type ?: (parent?.getTypeOf(identifier) ?: NoType)
    }

    private val table: MutableMap<String, IdentifierData> = mutableMapOf()
}


