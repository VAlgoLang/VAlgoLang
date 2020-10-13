package com.manimdsl.frontend

import antlr.ManimParser
import com.manimdsl.errorhandling.semanticerror.*
import org.antlr.v4.runtime.ParserRuleContext

class SemanticAnalysis {
    private val dataStructureHandler = DataStructureHandler()

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

    fun failIfIncompatibleTypes(lhsType: Type, rhsType: Type, text: String, ctx: ParserRuleContext) {
        if (lhsType != rhsType){
            declareAssignError(text, rhsType, lhsType, ctx)
        }
    }

    fun undeclaredIdentifier(currentSymbolTable: SymbolTableNode, identifier: String, ctx: ParserRuleContext) {
        if (currentSymbolTable.getTypeOf(identifier) == NoType){
            undeclaredAssignError(identifier, ctx)
        }
    }

    fun failIfNotDataStructure(currentSymbolTable: SymbolTableNode, identifier: String, ctx: ParserRuleContext) {
        if (currentSymbolTable.getTypeOf(identifier) !is DataStructureType) {
            nonDataStructureMethodError(identifier, ctx)
        }
    }

    // Assume it is a data structure
    fun notValidMethodNameForDataStructure(currentSymbolTable: SymbolTableNode, identifier: String, method: String, ctx: ParserRuleContext) {
        val dataStructureType = currentSymbolTable.getTypeOf(identifier)
        if (dataStructureType is DataStructureType) {
            if (dataStructureHandler.convertStringToMethod(method, dataStructureType) == ErrorMethod) {
                unsupportedMethodError(dataStructureType.toString(), method, ctx)
            }
        }
    }

    fun invalidNumberOfArguments(dataStructureType: DataStructureType, method: String, numArgs: Int, ctx: ParserRuleContext) {
        if (!dataStructureHandler.convertStringToMethod(method, dataStructureType).hasValidNumberOfArguments(numArgs)){
            numOfArgsInMethodCallError(dataStructureType.toString(), method, numArgs, ctx)
        }
    }

    fun checkArgTypes(argTypes: List<Type>, method: String, dataStructureType: DataStructureType, ctx: ManimParser.MethodCallContext) {
        argTypes.forEachIndexed { index, type ->
            if (type !is PrimitiveType) {
                val argCtx = ctx.arg_list().getRuleContext(ManimParser.ExprContext::class.java, index)
                val argName = ctx.arg_list().getChild(index).text
                typeOfArgsInMethodCallError(dataStructureType.toString(), method, type.toString(), argName, argCtx)
            }
        }
    }

    fun failIfDataStructureIncompatibleTypes(
        argTypes: List<Type>,
        typeInsideStructure: Type,
        dataStructureType: DataStructureType,
        ctx: ManimParser.MethodCallContext
    ) {
        //TODO with updated version
//        argTypes.forEachIndexed { index, type ->
//            if (type !is typeInsideStructure) {
//                val argCtx = ctx.arg_list().getRuleContext(ManimParser.ExprContext::class.java, index)
//                val argName = ctx.arg_list().getChild(index).text
//                typeOfArgsInMethodCallError(type.toString(), argName, argCtx)
//            }
//        }
    }


}