package com.manimdsl.frontend

import antlr.ManimParser
import com.manimdsl.errorhandling.semanticerror.*
import org.antlr.v4.runtime.ParserRuleContext

class SemanticAnalysis {

    private fun getExpressionType(expression: ExpressionNode, currentSymbolTable: SymbolTableVisitor): Type =
        when (expression) {
            is IdentifierNode -> currentSymbolTable.getTypeOf(expression.identifier)
            is NumberNode -> NumberType
            is MethodCallNode -> expression.dataStructureMethod.returnType
            is ConstructorNode -> expression.type
            is BinaryExpression -> getBinaryExpressionType(expression, currentSymbolTable)
            is UnaryExpression -> getUnaryExpressionType(expression, currentSymbolTable)
            is BoolNode -> BoolType
        }

    private fun getUnaryExpressionType(expression: UnaryExpression, currentSymbolTable: SymbolTableVisitor): Type {
        val exprType = getExpressionType(expression.expr, currentSymbolTable)

        return when (expression) {
            is PlusExpression, is MinusExpression -> if (exprType is NumberType) NumberType else ErrorType
            is NotExpression -> if (exprType is BoolType) BoolType else ErrorType
        }
    }

    private fun getBinaryExpressionType(expression: BinaryExpression, currentSymbolTable: SymbolTableVisitor): Type {
        val expr1Type = getExpressionType(expression.expr1, currentSymbolTable)
        val expr2Type = getExpressionType(expression.expr2, currentSymbolTable)

        return when (expression) {
            is AddExpression, is SubtractExpression, is MultiplyExpression -> {
                if (expr1Type is NumberType && expr2Type is NumberType) NumberType else ErrorType
            }
            is AndExpression, is OrExpression -> {
                if (expr1Type is BoolType && expr2Type is BoolType) BoolType else ErrorType
            }
            is EqExpression, is NeqExpression, is GtExpression,
            is LtExpression, is GeExpression, is LeExpression -> {
                if (expr1Type == expr2Type) BoolType else ErrorType
            }
        }
    }

    fun inferType(currentSymbolTable: SymbolTableVisitor, expression: ExpressionNode): Type {
        return getExpressionType(expression, currentSymbolTable)
    }


    fun redeclaredVariableCheck(currentSymbolTable: SymbolTableVisitor, identifier: String, ctx: ParserRuleContext) {
        if (currentSymbolTable.getTypeOf(identifier) != ErrorType) {
            redeclarationError(identifier, currentSymbolTable.getTypeOf(identifier), ctx)
        }
    }

    fun incompatibleTypesCheck(lhsType: Type, rhsType: Type, text: String, ctx: ParserRuleContext) {
        if (lhsType != ErrorType && rhsType != ErrorType && lhsType != rhsType) {
            declareAssignError(text, rhsType, lhsType, ctx)
        }
    }

    fun undeclaredIdentifierCheck(currentSymbolTable: SymbolTableVisitor, identifier: String, ctx: ParserRuleContext) {
        if (currentSymbolTable.getTypeOf(identifier) == ErrorType) {
            undeclaredAssignError(identifier, ctx)
        }
    }

    fun notDataStructureCheck(currentSymbolTable: SymbolTableVisitor, identifier: String, ctx: ParserRuleContext) {
        if (currentSymbolTable.getTypeOf(identifier) !is DataStructureType) {
            nonDataStructureMethodError(identifier, ctx)
        }
    }

    fun notValidMethodNameForDataStructureCheck(
            currentSymbolTable: SymbolTableVisitor,
            identifier: String,
            method: String,
            ctx: ParserRuleContext
    ) {
        val dataStructureType = currentSymbolTable.getTypeOf(identifier)
        if (dataStructureType is DataStructureType && !dataStructureType.containsMethod(method)) {
            unsupportedMethodError(dataStructureType.toString(), method, ctx)
        }
    }

    fun invalidNumberOfArgumentsCheck(
        dataStructureType: DataStructureType,
        methodName: String,
        numArgs: Int,
        ctx: ParserRuleContext
    ) {
        val method = dataStructureType.getMethodByName(methodName)
        if (method != ErrorMethod && method.argumentTypes.size != numArgs) {
            numOfArgsInMethodCallError(dataStructureType.toString(), methodName, numArgs, ctx)
        }
    }

    fun primitiveArgTypesCheck(
        argTypes: List<Type>,
        methodName: String,
        dataStructureType: DataStructureType,
        ctx: ManimParser.MethodCallContext
    ) {
        argTypes.forEachIndexed { index, type ->
            if (type !is PrimitiveType) {
                val argCtx = ctx.arg_list().getRuleContext(ManimParser.ExprContext::class.java, index)
                val argName = ctx.arg_list().getChild(index).text
                typeOfArgsInMethodCallError(dataStructureType.toString(), methodName, type.toString(), argName, argCtx)
            }
        }
    }

    fun incompatibleArgumentTypesCheck(
        dataStructureType: DataStructureType,
        argumentTypes: List<Type>,
        dataStructureMethod: DataStructureMethod,
        ctx: ManimParser.MethodCallContext
    ) {
        if (dataStructureMethod != ErrorMethod && dataStructureMethod.argumentTypes.size == argumentTypes.size) {
            argumentTypes.forEachIndexed { index, type ->
                if (type != dataStructureMethod.argumentTypes[index] && type is PrimitiveType) {
                    val argCtx = ctx.arg_list().getRuleContext(ManimParser.ExprContext::class.java, index)
                    val argName = ctx.arg_list().getChild(index).text
                    typeOfArgsInMethodCallError(
                        dataStructureType.toString(),
                        dataStructureMethod.toString(),
                        type.toString(),
                        argName,
                        argCtx
                    )
                }
            }
        }
    }

    fun incompatibleOperatorTypeCheck(
        operator: String,
        opExpr: ExpressionNode,
        currentSymbolTable: SymbolTableVisitor,
        ctx: ManimParser.ExprContext
    ) {
        when (opExpr) {
            is BinaryExpression -> {
                if (inferType(currentSymbolTable, opExpr) is ErrorType) {
                    incompatibleOperatorTypeError(
                        operator,
                        inferType(currentSymbolTable, opExpr.expr1),
                        inferType(currentSymbolTable, opExpr.expr2),
                        ctx
                    )
                }
            }
            is UnaryExpression -> {
                if (inferType(currentSymbolTable, opExpr) is ErrorType) {
                    incompatibleOperatorTypeError(
                        operator,
                        inferType(currentSymbolTable, opExpr.expr),
                        ctx = ctx
                    )
                }
            }
        }

    }

}