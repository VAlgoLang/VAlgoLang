package com.valgolang.frontend

import com.valgolang.frontend.ast.ErrorType
import com.valgolang.frontend.ast.ParameterNode
import com.valgolang.frontend.ast.Type

/**
 * Symbol table data
 *
 * Data to be stored in the symbol table
 *
 * @constructor Create empty Symbol table data
 */
interface SymbolTableData {
    val type: Type
}

/**
 * Identifier data
 *
 * Stores type information on identifier representing a variable
 *
 * @property type
 * @constructor Create empty Identifier data
 */
open class IdentifierData(override val type: Type) : SymbolTableData

object ErrorIdentifierData : IdentifierData(ErrorType)

/**
 * Function data
 *
 * Stores information on parameter types and return type of functions
 *
 * @property inferred
 * @property firstTime
 * @property parameters
 * @property type
 * @constructor Create empty Function data
 */
data class FunctionData(
    val inferred: Boolean,
    var firstTime: Boolean,
    val parameters: List<ParameterNode>,
    override var type: Type,
) : SymbolTableData

/**
 * Symbol table visitor
 *
 * Visits symbol table tree (tree of scopes). The ability to jump to different scopes is also available
 *
 * @constructor Create empty Symbol table visitor
 *//* Visitor for symbol table used when creating and traversing AST */
class SymbolTableVisitor {
    private val scopes = mutableListOf<SymbolTable>(GlobalScopeSymbolTable())
    private var currentScope: SymbolTable = scopes[0]
    private val variableNames = hashSetOf<String>()

    /**
     * Get type of
     *
     * Returns type of identifier. Checks current scope and recursively traverses up the tree till the global scope.
     *
     * @param identifier
     * @return
     */
    fun getTypeOf(identifier: String): Type = currentScope[identifier].type

    /**
     * Get data
     *
     * Gets the data for the identifier. Checks current scope and recursively traverses up the tree till the global scope.
     *
     * @param identifier
     * @return
     */
    fun getData(identifier: String): SymbolTableData = currentScope[identifier]

    /**
     * Get functions
     *
     * Gets all defined functions currently visible in the current scope. Searches at current scope and recursively searches upwards to global scope.
     *
     * @return
     */
    fun getFunctions(): Map<String, SymbolTableData> {
        return currentScope.getFunctions()
    }

    /**
     * Add variable
     *
     * Adds a variable to the symbol table
     *
     * @param identifier
     * @param data
     */
    fun addVariable(identifier: String, data: SymbolTableData) {
        currentScope[identifier] = data
        if (data is IdentifierData) variableNames.add(identifier)
    }

    /**
     * Enter scope
     *
     * Enters a new scope. This is usually called when a new code block is entered such as an if statement or function body.
     * This will add a new child scope to the current scope as a child and updates the current scope to the newly added child scope.
     * Additionally this appends the scope onto the end of the scopes list which is also assigned an Id representing its position in
     * that list.
     *
     * @return
     */
    fun enterScope(): Int {
        val newScope = SymbolTableNode(currentScope, scopes.size)
        scopes.add(newScope)
        currentScope = newScope
        // Returning index of this new symbol table (used in AST)
        return newScope.id
    }

    /**
     * Leave scope
     *
     * Usually called when the end of a block of code is reached e.g. at the end of a if statement or function body.
     * This sets the current scope to its parent scope.
     *
     */
    fun leaveScope() {
        currentScope.let { if (it is SymbolTableNode) currentScope = it.parent }
    }

    /**
     * Go to scope
     *
     * Sets current scope to that at the index scopeID in the scopes list.
     *
     * @param scopeID
     */
    fun goToScope(scopeID: Int) {
        if (scopeID in scopes.indices) {
            currentScope = scopes[scopeID]
        }
    }

    /**
     * Get current scopeID
     *
     * Returns id of the current scope in scopes list.
     *
     * @return
     */
    fun getCurrentScopeID(): Int = currentScope.id

    /**
     * Get variable names
     *
     * Returns all variable names present in symbol table.
     *
     * @return
     */
    fun getVariableNames(): Set<String> = variableNames
}

/**
 * Symbol table
 *
 * A class representing the symbol table.
 *
 * @property id
 * @constructor Create empty Symbol table
 */
sealed class SymbolTable(open val id: Int) {
    protected val table: MutableMap<String, SymbolTableData> = mutableMapOf()

    /**
     * Get functions
     *
     * Gets functions in symbol table
     *
     * @return
     */
    fun getFunctions(): Map<String, SymbolTableData> {
        return table.filterValues { it is FunctionData }
    }

    /**
     * Get
     *
     * Returns information on the identifier given if present.
     *
     * @param identifier
     * @return
     */
    abstract operator fun get(identifier: String): SymbolTableData

    /**
     * Set
     *
     * Sets the information of the given identifier to data.
     *
     * @param identifier
     * @param data
     */
    operator fun set(identifier: String, data: SymbolTableData) {
        table[identifier] = data
    }
}

/**
 * Symbol table node
 *
 * Represents a single scope or node in the tree of symbol tables structure.
 *
 * @property parent
 * @property id
 * @constructor Create empty Symbol table node
 */
open class SymbolTableNode(val parent: SymbolTable, override val id: Int) : SymbolTable(id) {
    override operator fun get(identifier: String): SymbolTableData {
        return table[identifier] ?: parent[identifier]
    }
}

/**
 * Global scope symbol table
 *
 * Root level symbol table representing the the global scope. This does not have a parent scope.
 *
 * @constructor Create empty Global scope symbol table
 */
class GlobalScopeSymbolTable : SymbolTable(id = 0) {
    override operator fun get(identifier: String): SymbolTableData {
        return table[identifier] ?: ErrorIdentifierData
    }
}
