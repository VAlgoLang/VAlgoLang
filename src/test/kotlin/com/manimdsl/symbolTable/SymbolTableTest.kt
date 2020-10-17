package com.manimdsl.symbolTable

import com.manimdsl.frontend.NoType
import com.manimdsl.frontend.NumberType
import com.manimdsl.frontend.StackType
import com.manimdsl.frontend.SymbolTable
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class SymbolTableTest {
    private val symbolTable = SymbolTable()

    @Test
    fun declaredVariableReturnsCorrectTypeAtGlobalScope() {
        symbolTable.addVariable("x", NumberType)
        assertEquals(NumberType, symbolTable.getTypeOf("x"))
    }

    @Test
    fun undeclaredVariableReturnsNoTypeAtGlobalScope() {
        assertEquals(NoType, symbolTable.getTypeOf("x"))
    }

    @Test
    fun undeclaredVariableReturnsNoTypeAtChildScope() {
        symbolTable.enterScope()
        assertEquals(NoType, symbolTable.getTypeOf("x"))
    }

    @Test
    fun enteringScopeCreatesNewScope() {
        assertEquals(1, symbolTable.enterScope())
        assertEquals(2, symbolTable.enterScope())
        symbolTable.leaveScope()
        symbolTable.leaveScope()
        assertEquals(3, symbolTable.enterScope())
    }

    @Test
    fun variableDeclaredInScopeIsOnlyAccessibleInThatScope() {
        symbolTable.enterScope()
        symbolTable.addVariable("x", NumberType)
        assertEquals(NumberType, symbolTable.getTypeOf("x"))
        symbolTable.leaveScope()
        assertEquals(NoType, symbolTable.getTypeOf("x"))
    }

    @Test
    fun variableDeclaredInParentScopeIsAccessibleInChildScope() {
        symbolTable.addVariable("x", NumberType)
        symbolTable.enterScope()
        assertEquals(NumberType, symbolTable.getTypeOf("x"))
    }

    @Test
    fun variableDeclaredInUnrelatedScopesIsUnaccessible() {
        symbolTable.enterScope()
        symbolTable.addVariable("x", NumberType)
        symbolTable.leaveScope()

        symbolTable.enterScope()
        assertEquals(NoType, symbolTable.getTypeOf("x"))
    }

    @Test
    fun goToScopeCanJumpBetweenScopesCorrectly() {
        val firstScope = symbolTable.enterScope()
        symbolTable.addVariable("x", NumberType)
        symbolTable.leaveScope()

        val secondScope = symbolTable.enterScope()
        symbolTable.addVariable("x", StackType())
        symbolTable.leaveScope()

        assertEquals(NoType, symbolTable.getTypeOf("x"))

        symbolTable.goToScope(firstScope)
        assertEquals(NumberType, symbolTable.getTypeOf("x"))

        symbolTable.goToScope(secondScope)
        assertEquals(StackType(), symbolTable.getTypeOf("x"))
    }

    @Test
    fun currentScopeIsUpdatedCorrectlyOnEnterAndLeave() {
        // Check current is global scope ID
        val globalScopeID = 0
        assertEquals(globalScopeID, symbolTable.getCurrentScopeID())

        val firstScope = symbolTable.enterScope()
        assertEquals(firstScope, symbolTable.getCurrentScopeID())
        symbolTable.leaveScope()

        assertEquals(globalScopeID, symbolTable.getCurrentScopeID())

        val secondScope = symbolTable.enterScope()
        assertEquals(secondScope, symbolTable.getCurrentScopeID())

        val thirdScope = symbolTable.enterScope()
        assertEquals(thirdScope, symbolTable.getCurrentScopeID())

        symbolTable.leaveScope()
        assertEquals(secondScope, symbolTable.getCurrentScopeID())

        symbolTable.leaveScope()
        assertEquals(globalScopeID, symbolTable.getCurrentScopeID())
    }
}