package com.manimdsl.frontend

open class IdentifierData(open val type: Type)

object ErrorIdentifierData : IdentifierData(ErrorType)

data class FunctionData(val parameters: List<ParameterNode>?, override val type: Type) : IdentifierData(type)

/* Visitor for symbol table used when creating and traversing AST */
class SymbolTableVisitor {
    private val scopes = mutableListOf<SymbolTable>(GlobalScopeSymbolTable())
    private var currentScope: SymbolTable = scopes[0]

    fun getTypeOf(identifier: String): Type = currentScope[identifier].type

    fun addVariable(identifier: String, data: IdentifierData) {
        currentScope[identifier] = data
    }

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

sealed class SymbolTable(open val id: Int) {
    protected val table: MutableMap<String, IdentifierData> = mutableMapOf()

    abstract operator fun get(identifier: String): IdentifierData

    operator fun set(identifier: String, data: IdentifierData) {
        table[identifier] = data
    }
}

open class SymbolTableNode(val parent: SymbolTable, override val id: Int) : SymbolTable(id) {
    override operator fun get(identifier: String): IdentifierData {
        return table[identifier] ?: parent[identifier]
    }
}

class GlobalScopeSymbolTable : SymbolTable(id = 0) {
    override operator fun get(identifier: String): IdentifierData {
        return table[identifier] ?: ErrorIdentifierData
    }
}

