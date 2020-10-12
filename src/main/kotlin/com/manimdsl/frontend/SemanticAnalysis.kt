package com.manimdsl.frontend

class SemanticAnalysis {
    fun failIfRedeclaredVariable(currentSymbolTable: SymbolTableNode, identifier: String): Boolean {
        return currentSymbolTable.getTypeOf(identifier) != NoType
    }

    fun inferTypeDeclaration(currentSymbolTable: SymbolTableNode, identifier: String, expression: ExpressionNode): Type {
        // DO SEMANTIC LOGGING HERE!
        return getExpressionType(expression, currentSymbolTable)
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

}