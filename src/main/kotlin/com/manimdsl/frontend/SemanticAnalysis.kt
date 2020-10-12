package com.manimdsl.frontend

class SemanticAnalysis {
    fun failIfRedeclaredVariable(currentSymbolTable: SymbolTableNode, identifier: String): Boolean {
        return currentSymbolTable.getTypeOf(identifier) != NoType
    }


    private fun getExpressionType(expression: ExpressionNode, currentSymbolTable: SymbolTableNode): Type = when (expression) {
        is IdentifierNode -> currentSymbolTable.getTypeOf(expression.identifier)
        is NumberNode -> NumberType
        is MethodCallNode -> currentSymbolTable.getTypeOf(expression.instanceIdentifier)
        is ConstructorNode -> expression.type
        is BinaryExpression -> if (getExpressionType(expression.expr1, currentSymbolTable) is NumberType && getExpressionType(expression.expr2, currentSymbolTable) is NumberType) {
            NumberType
        } else {
            NoType
        }
        is UnaryExpression -> getExpressionType(expression.expr, currentSymbolTable)
    }

    fun inferType(currentSymbolTable: SymbolTableNode, expression: ExpressionNode): Type {
        return getExpressionType(expression, currentSymbolTable)
    }

    fun failIfIncompatibleTypes(lhsType: Type, rhsType: Type): Boolean {
        return lhsType != rhsType
    }

    fun undeclaredAssignment(currentSymbolTable: SymbolTableNode, identifier: String): Boolean {
        return currentSymbolTable.getTypeOf(identifier) == NoType
    }

    fun failIfNotDataStructure(currentSymbolTable: SymbolTableNode, identifier: String): Boolean {
        return currentSymbolTable.getTypeOf(identifier) !is DataStructureType
    }

    // Assume it is a data structure
    fun notValidMethodNameForDataStructure(currentSymbolTable: SymbolTableNode, identifier: String, method: String): Boolean {
        return when (currentSymbolTable.getTypeOf(identifier)) {
            is StackType -> DataStructure.STACK.containsMethod(method)
            else -> false
        }
    }

    fun invalidNumberOfArguments(dataStructureType: DataStructureType, method: String, size: Int): Boolean {
        return when (dataStructureType) {
            is StackType -> DataStructure.STACK.hasValidNumberOfArguments(method, size)
        }
    }


}