package com.manimdsl.frontend

interface SymbolTableData {
    val type: Type
}

open class IdentifierData(override val type: Type) : SymbolTableData

object ErrorIdentifierData : IdentifierData(ErrorType)

data class FunctionData(
    val inferred: Boolean,
    var firstTime: Boolean,
    val parameters: List<ParameterNode>,
    override var type: Type,
) : SymbolTableData

data class ArrayData(
    val dimensions: List<Int>,
    val elementType: Type,
    override val type: Type
) : SymbolTableData

/* Visitor for symbol table used when creating and traversing AST */
class SymbolTableVisitor {
    private val scopes = mutableListOf<SymbolTable>(GlobalScopeSymbolTable())
    private var currentScope: SymbolTable = scopes[0]
    private val variableNames = hashSetOf<String>()

    fun getTypeOf(identifier: String): Type = currentScope[identifier].type

    fun getData(identifier: String): SymbolTableData = currentScope[identifier]

    fun getFunctions(): Map<String, SymbolTableData> {
        return currentScope.getFunctions()
    }

    fun addVariable(identifier: String, data: SymbolTableData) {
        currentScope[identifier] = data
        if (data is IdentifierData) variableNames.add(identifier)
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

    fun getVariableNames(): Set<String> = variableNames
}

sealed class SymbolTable(open val id: Int) {
    protected val table: MutableMap<String, SymbolTableData> = mutableMapOf()

    fun getFunctions(): Map<String, SymbolTableData> {
        return table.filterValues { it is FunctionData }
    }

    abstract operator fun get(identifier: String): SymbolTableData

    operator fun set(identifier: String, data: SymbolTableData) {
        table[identifier] = data
    }
}

open class SymbolTableNode(val parent: SymbolTable, override val id: Int) : SymbolTable(id) {
    override operator fun get(identifier: String): SymbolTableData {
        return table[identifier] ?: parent[identifier]
    }
}

class GlobalScopeSymbolTable : SymbolTable(id = 0) {
    override operator fun get(identifier: String): SymbolTableData {
        return table[identifier] ?: ErrorIdentifierData
    }
}

