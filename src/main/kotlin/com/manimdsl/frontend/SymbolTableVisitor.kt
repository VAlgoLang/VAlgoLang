package com.manimdsl.frontend

data class IdentifierData(val type: Type)

sealed class SymbolTable(open val id: Int) {
    protected val table: MutableMap<String, IdentifierData> = mutableMapOf()

    abstract fun getTypeOf(identifier: String): Type

    fun addVariable(identifier: String, type: Type) {
        table[identifier] = IdentifierData(type)
    }
}

/* Visitor for symbol table used when creating and traversing AST */
class SymbolTableVisitor {
    private val scopes = mutableListOf<SymbolTable>(GlobalScopeSymbolTable())
    private var currentScope: SymbolTable = scopes[0]

    fun getTypeOf(identifier: String): Type = currentScope.getTypeOf(identifier)

    fun addVariableToCurrentScope(identifier: String, type: Type) = currentScope.addVariable(identifier, type)

    fun enterScope(): Int {
        val newScope = SymbolTableNode(currentScope, scopes.size)
        scopes.add(newScope)
        currentScope = newScope
        // Returning index of this new symbol table (used in AST)
        return newScope.id
    }

    fun leaveScope() {
        currentScope.let { if (it is SymbolTableNode) currentScope = it.parent }
    }

    fun goToScope(scopeID: Int) {
        if (scopeID in scopes.indices) {
            currentScope = scopes[scopeID]
        }
    }

    fun getCurrentScopeID(): Int = currentScope.id
}

open class SymbolTableNode(val parent: SymbolTable, override val id: Int) : SymbolTable(id) {

    override fun getTypeOf(identifier: String): Type {
        return table[identifier]?.type ?: parent.getTypeOf(identifier)
    }
}

class GlobalScopeSymbolTable : SymbolTable(id = 0) {
    override fun getTypeOf(identifier: String): Type {
        return table[identifier]?.type ?: ErrorType
    }
}

