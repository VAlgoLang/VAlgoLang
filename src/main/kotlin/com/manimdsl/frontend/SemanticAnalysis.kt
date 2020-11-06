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
            is VoidNode -> VoidType
            is FunctionCallNode -> currentSymbolTable.getTypeOf(expression.functionIdentifier)
            is ArrayElemNode -> getArrayElemType(expression, currentSymbolTable)
            is BinaryTreeElemNode -> getBinaryTreeNodeType(expression, currentSymbolTable)
            is NullNode -> NullType
            is CharNode -> CharType
        }

    private fun getBinaryTreeNodeType(expression: BinaryTreeElemNode, currentSymbolTable: SymbolTableVisitor): Type {
        val type = currentSymbolTable.getTypeOf(expression.identifier)
        return if (type is BinaryTreeType) {
            if (expression.accessChain.isNotEmpty()) {
                val lastValue = expression.accessChain.last()
                if (lastValue is BinaryTreeType.Value) lastValue.returnType else BinaryTreeType(lastValue.returnType)
            } else {
                type
            }
        } else {
            ErrorType
        }
    }

    private fun getArrayElemType(expression: ArrayElemNode, currentSymbolTable: SymbolTableVisitor): Type {
        // To extend to multiple dimensions perform below recursively
        val arrayType = currentSymbolTable.getTypeOf(expression.identifier)
        return if (arrayType is ArrayType) {
            arrayType.internalType
        } else {
            ErrorType
        }
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
                if ((expr1Type is NumberType || expr1Type is CharType) && (expr2Type is NumberType || expr2Type is CharType)) NumberType else ErrorType
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
        if(rhsType is NullType && lhsType !is NullableDataStructure && lhsType !is NullType) {
            nonNullableAssignedToNull(rhsType.toString(), lhsType.toString(), ctx)
        } else if (rhsType != NullType && lhsType != ErrorType && rhsType != ErrorType && lhsType != rhsType) {
            declareAssignError(text, rhsType, lhsType, ctx)
        }
    }


    fun undeclaredIdentifierCheck(currentSymbolTable: SymbolTableVisitor, identifier: String, ctx: ParserRuleContext) {
        if (currentSymbolTable.getTypeOf(identifier) == ErrorType) {
            undeclaredAssignError(identifier, ctx)
        }
    }

    fun notDataStructureCheck(currentSymbolTable: SymbolTableVisitor, identifier: String, ctx: ParserRuleContext) {
        if (currentSymbolTable.getTypeOf(identifier) !is DataStructureType && currentSymbolTable.getTypeOf(identifier) !is NullType) {
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

    private fun invalidNumberOfArgumentsCheck(
        dataStructureType: DataStructureType,
        method: DataStructureMethod,
        numArgs: Int,
        ctx: ParserRuleContext
    ) {
        val correctNumberOfArgs = numArgs >= method.argumentTypes.filter { it.second }.size && numArgs <= method.argumentTypes.size
        if (method != ErrorMethod && !correctNumberOfArgs) {
            numOfArgsInMethodCallError(
                dataStructureType.toString(),
                dataStructureType.getMethodNameByMethod(method),
                numArgs,
                method.argumentTypes.size,
                ctx
            )
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
        ctx: ParserRuleContext
    ) {
        if (!dataStructureMethod.varargs) {
            invalidNumberOfArgumentsCheck(dataStructureType, dataStructureMethod, argumentTypes.size, ctx)
        }

        val argumentCtx = when (ctx) {
            is ManimParser.MethodCallContext -> ctx.arg_list()
            is ManimParser.DataStructureConstructorContext -> ctx.arg_list()
            else -> null
        } ?: return

        val expectedTypes = dataStructureMethod.argumentTypes
        if (dataStructureMethod != ErrorMethod &&
            (dataStructureMethod.varargs || expectedTypes.size == argumentTypes.size)
        ) {

            argumentTypes.forEachIndexed { index, type ->
                // Sets expected type. When varargs is enabled then set to last if index greater than size of given types
                val expectedType = when {
                    index in expectedTypes.indices -> {
                        expectedTypes[index].first
                    }
                    dataStructureMethod.varargs -> {
                        expectedTypes.last().first
                    }
                    else -> {
                        ErrorType
                    }
                }
                if (type != expectedType && type is PrimitiveType) {
                    val argCtx = argumentCtx.getRuleContext(ManimParser.ExprContext::class.java, index)
                    val argName = argumentCtx.getChild(index).text
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
        if (inferType(currentSymbolTable, opExpr) is ErrorType) {
            when (opExpr) {
                is BinaryExpression -> {
                    incompatibleOperatorTypeError(
                        operator,
                        inferType(currentSymbolTable, opExpr.expr1),
                        inferType(currentSymbolTable, opExpr.expr2),
                        ctx
                    )
                }
                is UnaryExpression -> {
                    incompatibleOperatorTypeError(
                        operator,
                        inferType(currentSymbolTable, opExpr.expr),
                        ctx = ctx
                    )
                }
            }
        }

    }

    fun checkExpressionTypeWithExpectedType(
        expression: ExpressionNode,
        expected: Type,
        currentSymbolTable: SymbolTableVisitor,
        ctx: ParserRuleContext
    ) {
        val actual = inferType(currentSymbolTable, expression)
        if (actual != expected) {
            unexpectedExpressionTypeError(expected, actual, ctx)
        }
    }

    fun globalReturnCheck(inFunction: Boolean, ctx: ManimParser.ReturnStatementContext) {
        if (!inFunction) {
            globalReturnError(ctx)
        }
    }

    fun breakOrContinueOutsideLoopCheck(action: String, inLoop: Boolean, ctx: ParserRuleContext) {
        if (!inLoop) {
            breakOrContinueOutsideLoopError(action, ctx)
        }
    }

    fun incompatibleReturnTypesCheck(
        currentSymbolTable: SymbolTableVisitor,
        functionReturnType: Type,
        expression: ExpressionNode,
        ctx: ManimParser.ReturnStatementContext
    ) {
        val type = inferType(currentSymbolTable, expression)
        if (type != functionReturnType) {
            returnTypeError(type.toString(), functionReturnType.toString(), ctx)
        }
    }

    fun invalidNumberOfArgumentsForFunctionsCheck(
        identifier: String,
        currentSymbolTable: SymbolTableVisitor,
        numArgs: Int,
        ctx: ManimParser.FunctionCallContext
    ) {
        val functionData = currentSymbolTable.getData(identifier)
        if (functionData is FunctionData) {
            val expected = functionData.parameters.size
            if (numArgs != expected) {
                numOfArgsInFunctionCallError(identifier, numArgs, expected, ctx)
            }
        }
    }

    fun incompatibleArgumentTypesForFunctionsCheck(
        identifier: String,
        currentSymbolTable: SymbolTableVisitor,
        argTypes: List<Type>,
        ctx: ManimParser.FunctionCallContext
    ) {
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

    fun missingReturnCheck(
        identifier: String,
        statements: List<StatementNode>,
        type: Type,
        ctx: ManimParser.FunctionContext
    ) {
        if (!checkStatementsHaveReturn(statements)) {
            missingReturnError(identifier, type.toString(), ctx)
        }
    }

    private fun checkStatementsHaveReturn(statements: List<StatementNode>): Boolean {
        return statements.any {
            when (it) {
                is ReturnNode -> true
                is IfStatementNode -> checkStatementsHaveReturn(it.statements)
                        && it.elifs.all { elif -> checkStatementsHaveReturn(elif.statements) }
                        && checkStatementsHaveReturn(it.elseBlock.statements)
                is WhileStatementNode -> checkStatementsHaveReturn(it.statements)
                else -> false
            }
        }
    }

    fun voidTypeDeclarationCheck(rhsType: Type, identifier: String, ctx: ParserRuleContext) {
        if (rhsType is VoidType) {
            voidTypeDeclarationError(identifier, ctx)
        }
    }

    fun undeclaredFunctionCheck(
        currentSymbolTable: SymbolTableVisitor,
        identifier: String,
        inFunction: Boolean,
        argTypes: List<Type>,
        ctx: ParserRuleContext
    ) {
        if (currentSymbolTable.getTypeOf(identifier) == ErrorType) {
            if (!inFunction) {
                undeclaredAssignError(identifier, ctx)
            } else {
                val params = argTypes.mapIndexed { index, type ->
                    ParameterNode("param$index", type)
                }
                val currentScope = currentSymbolTable.getCurrentScopeID()
                currentSymbolTable.goToScope(0)
                currentSymbolTable.addVariable(
                    identifier,
                    FunctionData(inferred = true, firstTime = true, parameters = params, type = VoidType)
                )
                currentSymbolTable.goToScope(currentScope)
            }
        }
    }

    fun redeclaredFunctionCheck(
        currentSymbolTable: SymbolTableVisitor,
        identifier: String,
        returnType: Type,
        parameters: List<ParameterNode>,
        ctx: ParserRuleContext
    ) {
        if (currentSymbolTable.getTypeOf(identifier) != ErrorType) {
            val functionData = currentSymbolTable.getData(identifier) as FunctionData
            if (functionData.inferred) {
                if (returnType != functionData.type) {
                    incompatibleFunctionType(identifier, returnType.toString(), functionData.type.toString(), ctx)
                }
                if (functionData.parameters.size != parameters.size) {
                    incompatibleParameterCount(identifier, parameters.size, functionData.parameters.size, ctx)
                } else {
                    functionData.parameters.forEachIndexed { index, parameter ->
                        val declaredParameter = parameters[index]
                        if (parameter.type != declaredParameter.type) {
                            incompatibleParameterType(
                                declaredParameter.identifier,
                                declaredParameter.type.toString(),
                                parameter.type.toString(),
                                ctx
                            )
                        }
                    }
                }
            } else {
                redeclarationError(identifier, currentSymbolTable.getTypeOf(identifier), ctx)
            }
        }
    }

    fun incompatibleMultipleFunctionCall(
        identifier: String,
        functionData: FunctionData,
        lhsType: Type,
        ctx: ParserRuleContext
    ) {
        if (functionData.inferred && !functionData.firstTime && functionData.type != lhsType) {
            incompatibleTypeFromMultipleFunctionCall(identifier, ctx)
        }
    }

    fun tooManyInferredFunctionsCheck(currentSymbolTable: SymbolTableVisitor, ctx: ParserRuleContext) {
        val functions = currentSymbolTable.getFunctions()
        functions.forEach { (identifier, data) ->
            val functionData = data as FunctionData
            if (functionData.inferred) {
                undeclaredAssignError(identifier, ctx)
            }
        }
    }

    fun allExpressionsAreSameTypeCheck(
        expected: Type,
        expressions: List<ExpressionNode>,
        currentSymbolTable: SymbolTableVisitor,
        ctx: ParserRuleContext
    ) {
        if (expressions.any { inferType(currentSymbolTable, it) != expected }) {
            inconsistentTypeError(expected, ctx)
        }
    }

    fun datastructureConstructorCheck(
        dataStructureType: DataStructureType,
        initialValue: List<ExpressionNode>,
        argumentTypes: List<Type>,
        ctx: ManimParser.DataStructureConstructorContext
    ) {
        val constructor = dataStructureType.getConstructor()
        if (initialValue.isEmpty() && argumentTypes.size < constructor.minRequiredArgsWithoutInitialValue) {
            missingConstructorArgumentsError(
                dataStructureType,
                constructor.minRequiredArgsWithoutInitialValue,
                argumentTypes.size,
                ctx
            )
        }
        incompatibleArgumentTypesCheck(dataStructureType, argumentTypes, constructor, ctx)
    }

    fun unableToInferTypeCheck(rhsType: Type, ctx: ParserRuleContext) {
        if (rhsType is NullType) {
            unableToInferType(rhsType.toString(), ctx)
        }
    }

    fun invalidArrayElemAssignment(identifier: String, type: Type, ctx: ManimParser.Assignment_lhsContext) {
        if (type !is ArrayType) {
            incorrectLHSForDataStructureElem(identifier, "Array", type, ctx)
        }
    }
}