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
            is BinaryExpression -> if (getExpressionType(
                    expression.expr1,
                    currentSymbolTable
                ) is NumberType && getExpressionType(expression.expr2, currentSymbolTable) is NumberType
            ) {
                NumberType
            } else {
                ErrorType
            }
            is UnaryExpression -> getExpressionType(expression.expr, currentSymbolTable)
            is FunctionCallNode -> currentSymbolTable.getTypeOf(expression.functionIdentifier)
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

    fun globalReturnCheck(inFunction: Boolean, ctx: ManimParser.ReturnStatementContext) {
        if (!inFunction) {
            globalReturnError(ctx)
        }
    }

    fun incompatibleReturnTypesCheck(currentSymbolTable: SymbolTableVisitor, functionReturnType: Type, expression: ExpressionNode, ctx: ManimParser.ReturnStatementContext) {
        val type = inferType(currentSymbolTable, expression)
        if (type != functionReturnType) {
            returnTypeError(type.toString(), functionReturnType.toString(), ctx)
        }
    }

    fun invalidNumberOfArgumentsForFunctionsCheck(identifier: String, currentSymbolTable: SymbolTableVisitor, numArgs: Int, ctx: ManimParser.FunctionCallContext) {
        val functionData = currentSymbolTable.getData(identifier)
        if (functionData is FunctionData) {
            val expected = functionData.parameters.size
            if (numArgs != expected) {
                numOfArgsInFunctionCallError(identifier, numArgs, expected, ctx)
            }
        }
    }

    fun incompatibleArgumentTypesForFunctionsCheck(identifier: String, currentSymbolTable: SymbolTableVisitor, argTypes: List<Type>, ctx: ManimParser.FunctionCallContext) {
        val functionData = currentSymbolTable.getData(identifier)
        if (functionData is FunctionData) {
            val parameters = functionData.parameters
            argTypes.forEachIndexed { index, type ->
                if (type != parameters[index].type) {
                    val argCtx = ctx.arg_list().getRuleContext(ManimParser.ExprContext::class.java, index)
                    val argName = ctx.arg_list().getChild(index).text
                    typeOfArgsInFunctionCallError(
                        identifier,
                        type.toString(),
                        argName,
                        parameters[index].type.toString(),
                        argCtx
                    )
                }
            }
        }
    }

    fun missingReturnCheck(identifier: String, statements: List<StatementNode>, type: Type, ctx: ManimParser.FunctionContext) {
        if(!statements.any { it is ReturnNode }) {
            missingReturnError(identifier, type.toString(), ctx)
        }
    }

    fun voidTypeDeclarationCheck(rhsType: Type, identifier: String, ctx: ParserRuleContext) {
        if (rhsType is VoidType) {
            voidTypeDeclarationError(identifier, ctx)
        }
    }

}