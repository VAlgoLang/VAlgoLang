package com.valgolang.frontend

import antlr.VAlgoLangParser
import com.valgolang.errorhandling.semanticerror.*
import com.valgolang.frontend.ast.*
import com.valgolang.frontend.datastructures.*
import com.valgolang.frontend.datastructures.array.Array2DInitialiserNode
import com.valgolang.frontend.datastructures.array.ArrayElemNode
import com.valgolang.frontend.datastructures.array.ArrayType
import com.valgolang.frontend.datastructures.array.InternalArrayMethodCallNode
import com.valgolang.frontend.datastructures.binarytree.BinaryTreeNodeElemAccessNode
import com.valgolang.frontend.datastructures.binarytree.BinaryTreeNodeType
import com.valgolang.frontend.datastructures.binarytree.BinaryTreeRootAccessNode
import com.valgolang.frontend.datastructures.binarytree.BinaryTreeType
import org.antlr.v4.runtime.ParserRuleContext

/**
 * Semantic analysis
 *
 * Class which contains all functions used to aid semantic analysis. Any errors are thrown and held in the error handler.
 *
 * @constructor Create empty Semantic analysis
 */
class SemanticAnalysis {

    /** Utility Functions **/

    private fun getExpressionType(expression: ExpressionNode, currentSymbolTable: SymbolTableVisitor): Type =
        when (expression) {
            is IdentifierNode -> currentSymbolTable.getTypeOf(expression.identifier)
            is NumberNode -> NumberType
            is VoidNode -> VoidType
            is BoolNode -> BoolType
            is NullNode -> NullType
            is CharNode -> CharType
            is StringNode -> StringType
            is MethodCallNode -> expression.dataStructureMethod.returnType
            is ConstructorNode -> expression.type
            is BinaryExpression -> getBinaryExpressionType(expression, currentSymbolTable)
            is UnaryExpression -> getUnaryExpressionType(expression, currentSymbolTable)
            is FunctionCallNode -> currentSymbolTable.getTypeOf(expression.functionIdentifier)
            is ArrayElemNode -> getArrayElemType(expression, currentSymbolTable)
            is BinaryTreeNodeElemAccessNode -> getBinaryTreeNodeType(expression, currentSymbolTable)
            is CastExpressionNode -> expression.targetType
            is InternalArrayMethodCallNode -> expression.dataStructureMethod.returnType
            is BinaryTreeRootAccessNode -> {
                val type = currentSymbolTable.getTypeOf(expression.identifier)
                if (type is BinaryTreeType) {
                    type.internalType
                } else {
                    ErrorType
                }
            }
            else -> throw NotImplementedError("Expression Type not implemented")
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
            is AddExpression, is SubtractExpression, is MultiplyExpression, is DivideExpression -> {
                if (expr1Type is StringType || expr2Type is StringType) {
                    // String interpolation can be compatible with any type and takes highest priority
                    StringType
                } else {
                    val validTypes = (expression as ComparableTypes).compatibleTypes
                    if (validTypes.contains(expr1Type) && validTypes.contains(expr2Type)) NumberType else ErrorType
                }
            }
            is AndExpression, is OrExpression -> {
                if (expr1Type is BoolType && expr2Type is BoolType) BoolType else ErrorType
            }
            is EqExpression, is NeqExpression ->
                if (expr1Type == expr2Type || isEqualNullable(
                        expr1Type,
                        expr2Type
                    )
                ) BoolType else ErrorType
            is GtExpression, is LtExpression, is GeExpression, is LeExpression -> {
                if (expr1Type == expr2Type) BoolType else ErrorType
            }
        }
    }

    private fun isEqualNullable(expr1Type: Type, expr2Type: Type): Boolean {
        return expr1Type == expr2Type || (expr1Type is NullableDataStructure && expr2Type is NullType) || (expr1Type is NullType && expr2Type is NullableDataStructure)
    }

    /**
     * Infer type
     *
     * Infers type of expression
     *
     * @param currentSymbolTable
     * @param expression
     * @return
     */
    fun inferType(currentSymbolTable: SymbolTableVisitor, expression: ExpressionNode): Type {
        return getExpressionType(expression, currentSymbolTable)
    }

    /**
     * Redeclared variable check
     *
     * @param currentSymbolTable
     * @param identifier
     * @param ctx
     */
    fun redeclaredVariableCheck(currentSymbolTable: SymbolTableVisitor, identifier: String, ctx: ParserRuleContext) {
        if (currentSymbolTable.getTypeOf(identifier) != ErrorType) {
            redeclarationError(identifier, currentSymbolTable.getTypeOf(identifier), ctx)
        }
    }

    /**
     * Incompatible types check
     *
     * @param lhsType
     * @param rhsType
     * @param text
     * @param ctx
     */
    fun incompatibleTypesCheck(lhsType: Type, rhsType: Type, text: String, ctx: ParserRuleContext) {
        if (rhsType is NullType && lhsType !is NullableDataStructure && lhsType !is NullType) {
            nonNullableAssignedToNull(lhsType.toString(), ctx)
        } else if (rhsType != NullType && lhsType != ErrorType && rhsType != ErrorType && lhsType != rhsType) {
            declareAssignError(text, rhsType, lhsType, ctx)
        }
    }

    /**
     * Undeclared identifier check
     *
     * @param currentSymbolTable
     * @param identifier
     * @param ctx
     */
    fun undeclaredIdentifierCheck(currentSymbolTable: SymbolTableVisitor, identifier: String, ctx: ParserRuleContext) {
        if (currentSymbolTable.getTypeOf(identifier) == ErrorType) {
            undeclaredAssignError(identifier, ctx)
        }
    }

    /**
     * Not data structure check
     *
     * @param currentSymbolTable
     * @param identifier
     * @param ctx
     */
    fun notDataStructureCheck(currentSymbolTable: SymbolTableVisitor, identifier: String, ctx: ParserRuleContext) {
        if (currentSymbolTable.getTypeOf(identifier) !is DataStructureType && currentSymbolTable.getTypeOf(identifier) !is NullType) {
            nonDataStructureMethodError(identifier, ctx)
        }
    }

    /**
     * Not valid method name for data structure check
     *
     * @param currentSymbolTable
     * @param identifier
     * @param method
     * @param ctx
     * @param overrideType
     * @return
     */
    fun notValidMethodNameForDataStructureCheck(
        currentSymbolTable: SymbolTableVisitor,
        identifier: String,
        method: String,
        ctx: ParserRuleContext,
        overrideType: Type = ErrorType
    ): Boolean {
        val dataStructureType = if (overrideType is ErrorType) {
            currentSymbolTable.getTypeOf(identifier)
        } else {
            overrideType
        }

        if (dataStructureType is DataStructureType && !dataStructureType.containsMethod(method)) {
            unsupportedMethodError(dataStructureType.toString(), method, ctx)
            return false
        }

        return true
    }

    private fun invalidNumberOfArgumentsCheck(
        dataStructureType: DataStructureType,
        method: DataStructureMethod,
        numArgs: Int,
        ctx: ParserRuleContext
    ) {
        val correctNumberOfArgs =
            numArgs >= method.argumentTypes.filter { it.second }.size && numArgs <= method.argumentTypes.size
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

    /**
     * Primitive internal type for data structure check
     *
     * @param internalType
     * @param ctx
     */
    fun primitiveInternalTypeForDataStructureCheck(internalType: Type, ctx: ParserRuleContext) {
        if (internalType !is PrimitiveType) {
            dataStructureInternalTypeNotPrimitiveError(internalType, ctx)
        }
    }

    /**
     * Primitive arg types check
     *
     * @param argTypes
     * @param methodName
     * @param dataStructureType
     * @param ctx
     */
    fun primitiveArgTypesCheck(
        argTypes: List<Type>,
        methodName: String,
        dataStructureType: DataStructureType,
        ctx: VAlgoLangParser.MethodCallContext
    ) {
        argTypes.forEachIndexed { index, type ->
            if (type !is PrimitiveType) {
                val argCtx = ctx.arg_list().getRuleContext(VAlgoLangParser.ExprContext::class.java, index)
                val argName = ctx.arg_list().getChild(index).text
                typeOfArgsInMethodCallError(dataStructureType.toString(), methodName, type.toString(), argName, argCtx)
            }
        }
    }

    /**
     * Incompatible argument types check
     *
     * @param dataStructureType
     * @param argumentTypes
     * @param dataStructureMethod
     * @param ctx
     */
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
            is VAlgoLangParser.MethodCallContext -> ctx.arg_list()
            is VAlgoLangParser.DataStructureConstructorContext -> ctx.arg_list()
            else -> null
        } ?: return

        val expectedTypes = dataStructureMethod.argumentTypes
        if (dataStructureMethod != ErrorMethod &&
            (dataStructureMethod.varargs || expectedTypes.size == argumentTypes.size) && expectedTypes.isNotEmpty()
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
                    val argCtx = argumentCtx.getRuleContext(VAlgoLangParser.ExprContext::class.java, index)
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

    /**
     * Incompatible operator type check
     *
     * @param operator
     * @param opExpr
     * @param currentSymbolTable
     * @param ctx
     */
    fun incompatibleOperatorTypeCheck(
        operator: String,
        opExpr: ExpressionNode,
        currentSymbolTable: SymbolTableVisitor,
        ctx: VAlgoLangParser.ExprContext
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

    /**
     * Check expression type with expected type
     *
     * @param expression
     * @param expected
     * @param currentSymbolTable
     * @param ctx
     */
    fun checkExpressionTypeWithExpectedType(
        expression: ExpressionNode,
        expected: Type,
        currentSymbolTable: SymbolTableVisitor,
        ctx: ParserRuleContext
    ) {
        checkExpressionTypeWithExpectedTypes(expression, setOf(expected), currentSymbolTable, ctx)
    }

    /**
     * Check expression type with expected types
     *
     * @param expression
     * @param expected
     * @param currentSymbolTable
     * @param ctx
     */
    fun checkExpressionTypeWithExpectedTypes(
        expression: ExpressionNode,
        expected: Set<Type>,
        currentSymbolTable: SymbolTableVisitor,
        ctx: ParserRuleContext
    ) {
        val actual = inferType(currentSymbolTable, expression)
        if (!expected.contains(actual)) {
            unexpectedExpressionTypeError(expected, actual, ctx)
        }
    }

    /**
     * Global return check
     *
     * @param inFunction
     * @param ctx
     */
    fun globalReturnCheck(inFunction: Boolean, ctx: VAlgoLangParser.ReturnStatementContext) {
        if (!inFunction) {
            globalReturnError(ctx)
        }
    }

    /**
     * Break or continue outside loop check
     *
     * @param action
     * @param inLoop
     * @param ctx
     */
    fun breakOrContinueOutsideLoopCheck(action: String, inLoop: Boolean, ctx: ParserRuleContext) {
        if (!inLoop) {
            breakOrContinueOutsideLoopError(action, ctx)
        }
    }

    /**
     * Incompatible return types check
     *
     * @param currentSymbolTable
     * @param functionReturnType
     * @param expression
     * @param ctx
     */
    fun incompatibleReturnTypesCheck(
        currentSymbolTable: SymbolTableVisitor,
        functionReturnType: Type,
        expression: ExpressionNode,
        ctx: VAlgoLangParser.ReturnStatementContext
    ) {
        val type = inferType(currentSymbolTable, expression)
        if (!isEqualNullable(type, functionReturnType)) {
            returnTypeError(type.toString(), functionReturnType.toString(), ctx)
        }
    }

    /**
     * Invalid number of arguments for functions check
     *
     * @param identifier
     * @param currentSymbolTable
     * @param numArgs
     * @param ctx
     */
    fun invalidNumberOfArgumentsForFunctionsCheck(
        identifier: String,
        currentSymbolTable: SymbolTableVisitor,
        numArgs: Int,
        ctx: VAlgoLangParser.FunctionCallContext
    ) {
        val functionData = currentSymbolTable.getData(identifier)
        if (functionData is FunctionData) {
            val expected = functionData.parameters.size
            if (numArgs != expected) {
                numOfArgsInFunctionCallError(identifier, numArgs, expected, ctx)
            }
        }
    }

    /**
     * Incompatible argument types for functions check
     *
     * @param identifier
     * @param currentSymbolTable
     * @param argTypes
     * @param ctx
     */
    fun incompatibleArgumentTypesForFunctionsCheck(
        identifier: String,
        currentSymbolTable: SymbolTableVisitor,
        argTypes: List<Type>,
        ctx: VAlgoLangParser.FunctionCallContext
    ) {
        val functionData = currentSymbolTable.getData(identifier)
        if (functionData is FunctionData) {
            val parameters = functionData.parameters
            argTypes.forEachIndexed { index, type ->
                if (type != parameters[index].type) {
                    val argCtx = ctx.arg_list().getRuleContext(VAlgoLangParser.ExprContext::class.java, index)
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

    /**
     * Missing return check
     *
     * @param identifier
     * @param statements
     * @param type
     * @param ctx
     */
    fun missingReturnCheck(
        identifier: String,
        statements: List<StatementNode>,
        type: Type,
        ctx: VAlgoLangParser.FunctionContext
    ) {
        if (!checkStatementsHaveReturn(statements)) {
            missingReturnError(identifier, type.toString(), ctx)
        }
    }

    private fun checkStatementsHaveReturn(statements: List<StatementNode>): Boolean {
        return statements.any {
            when (it) {
                is ReturnNode -> true
                is IfStatementNode ->
                    checkStatementsHaveReturn(it.statements) &&
                        it.elifs.all { elif -> checkStatementsHaveReturn(elif.statements) } &&
                        checkStatementsHaveReturn(it.elseBlock.statements)
                is WhileStatementNode -> checkStatementsHaveReturn(it.statements)
                is ForStatementNode -> checkStatementsHaveReturn(it.statements)
                else -> false
            }
        }
    }

    /**
     * Void type declaration check
     *
     * @param rhsType
     * @param identifier
     * @param ctx
     */
    fun voidTypeDeclarationCheck(rhsType: Type, identifier: String, ctx: ParserRuleContext) {
        if (rhsType is VoidType) {
            voidTypeDeclarationError(identifier, ctx)
        }
    }

    /**
     * Undeclared function check
     *
     * @param currentSymbolTable
     * @param identifier
     * @param inFunction
     * @param argTypes
     * @param ctx
     */
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

    /**
     * Redeclared function check
     *
     * @param currentSymbolTable
     * @param identifier
     * @param returnType
     * @param parameters
     * @param ctx
     */
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

    /**
     * Incompatible multiple function call
     *
     * @param identifier
     * @param functionData
     * @param lhsType
     * @param ctx
     */
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

    /**
     * Too many inferred functions check
     *
     * @param currentSymbolTable
     * @param ctx
     */
    fun tooManyInferredFunctionsCheck(currentSymbolTable: SymbolTableVisitor, ctx: ParserRuleContext) {
        val functions = currentSymbolTable.getFunctions()
        functions.forEach { (identifier, data) ->
            val functionData = data as FunctionData
            if (functionData.inferred) {
                undeclaredAssignError(identifier, ctx)
            }
        }
    }

    /**
     * Array2d dimensions match check
     *
     * @param initialiser
     * @param dataStructureType
     * @param ctx
     */
    fun array2DDimensionsMatchCheck(
        initialiser: InitialiserNode,
        dataStructureType: DataStructureType,
        ctx: ParserRuleContext
    ) {
        if (initialiser is Array2DInitialiserNode && dataStructureType is ArrayType && dataStructureType.is2D) {
            val nestedExpressions = initialiser.nestedExpressions
            if (nestedExpressions.isNotEmpty() && nestedExpressions.any { it.size != nestedExpressions[0].size }) {
                array2DDimensionError(ctx)
            }
        }
    }

    /**
     * All expressions are same type check
     *
     * @param expected
     * @param expressions
     * @param currentSymbolTable
     * @param ctx
     */
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

    /**
     * Datastructure constructor check
     *
     * @param dataStructureType
     * @param initialValue
     * @param argumentTypes
     * @param ctx
     */
    fun datastructureConstructorCheck(
        dataStructureType: DataStructureType,
        initialValue: List<ExpressionNode>,
        argumentTypes: List<Type>,
        ctx: VAlgoLangParser.DataStructureConstructorContext
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

    /**
     * Unable to infer type check
     *
     * @param rhsType
     * @param ctx
     */
    fun unableToInferTypeCheck(rhsType: Type, ctx: ParserRuleContext) {
        if (rhsType is NullType) {
            unableToInferTypeFromNullType(ctx)
        }
    }

    /**
     * Invalid array elem assignment
     *
     * @param identifier
     * @param type
     * @param ctx
     */
    fun invalidArrayElemAssignment(identifier: String, type: Type, ctx: VAlgoLangParser.Assignment_lhsContext) {
        if (type is StringType) {
            stringImmutabilityError(identifier, ctx)
        } else if (type !is ArrayType) {
            incorrectLHSForDataStructureElem(identifier, "Array", type, ctx)
        }
    }

    /**
     * Incompatible initialiser check
     *
     * @param dataStructureType
     * @param initialiser
     * @param ctx
     */
    fun incompatibleInitialiserCheck(
        dataStructureType: DataStructureType,
        initialiser: ASTNode,
        ctx: ParserRuleContext
    ) {
        val is2D = dataStructureType is ArrayType && dataStructureType.is2D
        val arrayPrefix = if (dataStructureType is ArrayType) {
            if (is2D) "2D " else "1D "
        } else ""
        if ((initialiser is Array2DInitialiserNode && !is2D) || (initialiser is DataStructureInitialiserNode && is2D)) {
            incompatibleDataStructureInitialisation("${arrayPrefix}$dataStructureType", ctx)
        }
    }

    /**
     * For loop range type check
     *
     * @param symbolTable
     * @param startExpr
     * @param endExpr
     * @param ctx
     */
    fun forLoopRangeTypeCheck(
        symbolTable: SymbolTableVisitor,
        startExpr: ExpressionNode,
        endExpr: ExpressionNode,
        ctx: VAlgoLangParser.RangeHeaderContext
    ) {
        val startExprType = inferType(symbolTable, startExpr)
        val endExprType = inferType(symbolTable, endExpr)
        if (!(
            startExprType is NumberType && endExprType is NumberType ||
                startExprType is CharType && endExprType is CharType
            )
        ) {
            forLoopRangeNotNumberOrChar(startExprType.toString(), endExprType.toString(), ctx)
        }
    }

    /**
     * For loop range update number type check
     *
     * @param symbolTable
     * @param change
     * @param ctx
     */
    fun forLoopRangeUpdateNumberTypeCheck(
        symbolTable: SymbolTableVisitor,
        change: ExpressionNode,
        ctx: ParserRuleContext
    ) {
        val type = inferType(symbolTable, change)
        if (type !is NumberType) {
            forLoopRangeUpdateNotNumber(type.toString(), ctx)
        }
    }

    /**
     * For loop identifier not being reassigned check
     *
     * @param identifier
     * @param forLoopIdentifiers
     * @param ctx
     */
    fun forLoopIdentifierNotBeingReassignedCheck(
        identifier: String,
        forLoopIdentifiers: Set<String>,
        ctx: ParserRuleContext
    ) {
        if (forLoopIdentifiers.contains(identifier)) {
            forLoopIdentifierBeingReassignedError(identifier, ctx)
        }
    }

    /**
     * Check annotation arguments
     *
     * @param ctx
     * @param symbolTable
     * @param arguments
     * @return
     */
    fun checkAnnotationArguments(
        ctx: ParserRuleContext,
        symbolTable: SymbolTableVisitor,
        arguments: List<ExpressionNode>
    ): List<Type> = when (ctx) {
        is VAlgoLangParser.AnimationSpeedUpAnnotationContext -> {
            if (arguments.isEmpty() || arguments.size > 2) {
                invalidArgumentsForAnnotationError(ctx.SPEED().symbol.text, ctx)
            } else {
                if (arguments.size == 2) {
                    checkExpressionTypeWithExpectedType(arguments[1], BoolType, symbolTable, ctx)
                }
                checkExpressionTypeWithExpectedType(arguments.first(), NumberType, symbolTable, ctx)
            }
            emptyList()
        }
        is VAlgoLangParser.SubtitleAnnotationContext -> {
            if (arguments.size > 2) {
                invalidArgumentsForAnnotationError(ctx.show.text, ctx)
                emptyList()
            } else {
                val types = arguments.map {
                    inferType(symbolTable, it)
                }
                val expectedTypes = mutableSetOf(NumberType, BoolType)
                // Order independent type checking
                types.forEach {
                    if (!expectedTypes.contains(it)) {
                        unexpectedExpressionTypeError(expectedTypes, it, ctx)
                    }
                    expectedTypes.remove(it)
                }
                types
            }
        }
        else -> throw NotImplementedError("Annotation argument check not implemented")
    }

    /** Arrays **/

    private fun getArrayElemType(expression: ArrayElemNode, currentSymbolTable: SymbolTableVisitor): Type {
        // To extend to multiple dimensions perform below recursively
        val arrayType = currentSymbolTable.getTypeOf(expression.identifier)
        return if (arrayType is ArrayType) {
            if (arrayType.is2D) {
                if (expression.indices.size == 2) arrayType.internalType else ArrayType(arrayType.internalType)
            } else {
                arrayType.internalType
            }
        } else {
            ErrorType
        }
    }

    /**
     * Check array constructor item lengths match
     *
     * @param openConstructorSize
     * @param closeConstructorSize
     * @param ctx
     */
    fun checkArrayConstructorItemLengthsMatch(
        openConstructorSize: Int,
        closeConstructorSize: Int,
        ctx: ParserRuleContext
    ) {
        if (openConstructorSize != closeConstructorSize) {
            incorrectConstructorItemSize(openConstructorSize, closeConstructorSize, ctx)
        }
    }

    /**
     * Check array dimensions not greater than two
     *
     * @param arrayDimension
     * @param ctx
     */
    fun checkArrayDimensionsNotGreaterThanTwo(arrayDimension: Int, ctx: ParserRuleContext) {
        if (arrayDimension > 2) {
            incompatibleArrayDimension(arrayDimension, ctx)
        }
    }

    /**
     * Check array dimensions match constructor arguments
     *
     * @param dataStructureType
     * @param argumentsSize
     * @param ctx
     */
    fun checkArrayDimensionsMatchConstructorArguments(
        dataStructureType: DataStructureType,
        argumentsSize: Int,
        ctx: ParserRuleContext
    ) {
        if (dataStructureType is ArrayType && argumentsSize != 0) {
            if ((dataStructureType.is2D && argumentsSize != 2) || (!dataStructureType.is2D && argumentsSize != 1)) {
                incompatibleArrayDimensionWithConstructorArguments(dataStructureType.is2D, argumentsSize, ctx)
            }
        }
    }

    /**
     * Check array elem has correct number of indices
     *
     * @param indices
     * @param is2DArray
     * @param ctx
     */
    fun checkArrayElemHasCorrectNumberOfIndices(
        indices: List<ExpressionNode>,
        is2DArray: Boolean,
        ctx: ParserRuleContext
    ) {
        val hasCorrectNumberOfIndices = if (is2DArray) indices.size == 2 else indices.size == 1
        if (!hasCorrectNumberOfIndices) {
            maxArrayIndexingExceededError(is2DArray, indices.size, ctx)
        }
    }

    /**
     * Check array elem index types
     *
     * @param indices
     * @param currentSymbolTable
     * @param ctx
     */
    fun checkArrayElemIndexTypes(
        indices: List<ExpressionNode>,
        currentSymbolTable: SymbolTableVisitor,
        ctx: ParserRuleContext
    ) {
        indices.forEach {
            checkExpressionTypeWithExpectedTypes(it, setOf(NumberType), currentSymbolTable, ctx)
        }
    }

    /** Binary trees **/

    private fun getBinaryTreeNodeType(
        expression: BinaryTreeNodeElemAccessNode,
        currentSymbolTable: SymbolTableVisitor
    ): Type {
        val type = currentSymbolTable.getTypeOf(expression.identifier)
        return if (type is BinaryTreeNodeType) {
            if (expression.accessChain.isNotEmpty()) {
                val lastValue = expression.accessChain.last()
                lastValue.returnType
            } else {
                type
            }
        } else {
            ErrorType
        }
    }
}
