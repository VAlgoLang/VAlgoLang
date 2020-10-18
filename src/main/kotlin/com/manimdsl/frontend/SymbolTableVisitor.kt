package com.manimdsl.frontend

data class IdentifierData(val type: Type)

interface SymbolTable {
    fun getTypeOf(identifier: String): Type
}

/* Visitor for symbol table used when creating and traversing AST */
class SymbolTableVisitor {
    private val scopes = mutableListOf<SymbolTableNode>(GlobalScopeSymbolTable())
    private var currentScope: SymbolTableNode = scopes[0]

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

/* Represents one level higher than the global scope
*  Reaching this in getTypeOf function means a variable has never been declared */
object SymbolTableRoot : SymbolTable {
    override fun getTypeOf(identifier: String): Type {
        return ErrorType
    }
}

open class SymbolTableNode(val parent: SymbolTable, val id: Int): SymbolTable {
    private val table: MutableMap<String, IdentifierData> = mutableMapOf()

    fun addVariable(identifier: String, type: Type) {
        table[identifier] = IdentifierData(type)
    }

    override fun getTypeOf(identifier: String): Type {
        return table[identifier]?.type ?: parent.getTypeOf(identifier)
    }
}

class GlobalScopeSymbolTable: SymbolTableNode(SymbolTableRoot, 0)

