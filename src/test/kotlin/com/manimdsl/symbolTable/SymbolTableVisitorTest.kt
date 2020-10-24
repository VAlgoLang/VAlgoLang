package com.manimdsl.symbolTable

import com.manimdsl.frontend.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SymbolTableVisitorTest {
    private val symbolTable = SymbolTableVisitor()

    @Test
    fun declaredVariableReturnsCorrectTypeAtGlobalScope() {
        symbolTable.addVariable("x", IdentifierData(NumberType))
        assertEquals(NumberType, symbolTable.getTypeOf("x"))
    }

    @Test
    fun undeclaredVariableReturnsNoTypeAtGlobalScope() {
        assertEquals(ErrorType, symbolTable.getTypeOf("x"))
    }

    @Test
    fun undeclaredVariableReturnsNoTypeAtChildScope() {
        symbolTable.enterScope()
        assertEquals(ErrorType, symbolTable.getTypeOf("x"))
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
        symbolTable.addVariable("x", IdentifierData(NumberType))
        assertEquals(NumberType, symbolTable.getTypeOf("x"))
        symbolTable.leaveScope()
        assertEquals(ErrorType, symbolTable.getTypeOf("x"))
    }

    @Test
    fun variableDeclaredInParentScopeIsAccessibleInChildScope() {
        symbolTable.addVariable("x", IdentifierData(NumberType))
        symbolTable.enterScope()
        symbolTable.enterScope()
        assertEquals(NumberType, symbolTable.getTypeOf("x"))
    }

    @Test
    fun variableDeclaredInUnrelatedScopesIsUnaccessible() {
        symbolTable.enterScope()
        symbolTable.addVariable("x", IdentifierData(NumberType))
        symbolTable.leaveScope()

        symbolTable.enterScope()
        assertEquals(ErrorType, symbolTable.getTypeOf("x"))
    }

    @Test
    fun goToScopeCanJumpBetweenScopesCorrectly() {
        val firstScope = symbolTable.enterScope()
        symbolTable.addVariable("x", IdentifierData(NumberType))
        symbolTable.leaveScope()

        val secondScope = symbolTable.enterScope()
        symbolTable.addVariable("x", IdentifierData(StackType(NumberType)))
        symbolTable.leaveScope()

        assertEquals(ErrorType, symbolTable.getTypeOf("x"))

        symbolTable.goToScope(firstScope)
        assertEquals(NumberType, symbolTable.getTypeOf("x"))

        symbolTable.goToScope(secondScope)
        assertEquals(StackType(NumberType), symbolTable.getTypeOf("x"))
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