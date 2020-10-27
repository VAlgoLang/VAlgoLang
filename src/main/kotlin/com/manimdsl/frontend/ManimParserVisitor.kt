package com.manimdsl.frontend

import antlr.ManimParser.*
import antlr.ManimParserBaseVisitor

class ManimParserVisitor : ManimParserBaseVisitor<ASTNode>() {
    val symbolTable = SymbolTableVisitor()

    val lineNumberNodeMap = mutableMapOf<Int, StatementNode>()

    private val semanticAnalyser = SemanticAnalysis()
    private var inFunction: Boolean = false
    private var functionReturnType: Type = VoidType

    override fun visitProgram(ctx: ProgramContext): ProgramNode {
        val functions = ctx.function().map { visit(it) as FunctionNode }
        semanticAnalyser.tooManyInferredFunctionsCheck(symbolTable, ctx)
        return ProgramNode(
                functions,
                flattenStatements(visit(ctx.stat()) as StatementNode))
    }

    override fun visitFunction(ctx: FunctionContext): FunctionNode {
        inFunction = true
        val identifier = ctx.IDENT().symbol.text
        val type = if (ctx.type() != null) {
            visit(ctx.type()) as Type
        } else {
            VoidType
        }
        functionReturnType = type

        val currentScope = symbolTable.getCurrentScopeID()
        val scope = symbolTable.enterScope()
        val parameters: List<ParameterNode> =
            visitParameterList(ctx.param_list() as ParameterListContext?).parameters

        symbolTable.goToScope(currentScope)

        // Define function symbol in parent scope
        semanticAnalyser.redeclaredFunctionCheck(symbolTable, identifier, type, parameters, ctx)
        symbolTable.addVariable(
            identifier,
            FunctionData(inferred = false, firstTime = false, parameters = parameters, type = type)
        )

        symbolTable.goToScope(scope)

        val statements = visitAndFlattenStatements(ctx.statements)
        symbolTable.leaveScope()


        if (functionReturnType !is VoidType) {
            semanticAnalyser.missingReturnCheck(identifier, statements, functionReturnType, ctx)
        }


        inFunction = false
        functionReturnType = VoidType
        lineNumberNodeMap[ctx.start.line] = FunctionNode(ctx.start.line, scope, identifier, parameters, statements)
        return lineNumberNodeMap[ctx.start.line] as FunctionNode
    }

    override fun visitParameterList(ctx: ParameterListContext?): ParameterListNode{
        if (ctx == null) {
            return ParameterListNode(listOf())
        }
        return ParameterListNode(ctx.param().map { visit(it) as ParameterNode })
    }

    override fun visitParameter(ctx: ParameterContext): ParameterNode {
        val identifier = ctx.IDENT().symbol.text
        semanticAnalyser.redeclaredVariableCheck(symbolTable, identifier, ctx)

        val type = visit(ctx.type()) as Type
        symbolTable.addVariable(identifier, IdentifierData(type))
        return ParameterNode(identifier, type)
    }

    override fun visitReturnStatement(ctx: ReturnStatementContext): ReturnNode {
        semanticAnalyser.globalReturnCheck(inFunction, ctx)
        val expression = if (ctx.expr() != null) visit(ctx.expr()) as ExpressionNode else VoidNode(ctx.start.line)
        semanticAnalyser.incompatibleReturnTypesCheck(symbolTable, functionReturnType, expression, ctx)
        lineNumberNodeMap[ctx.start.line] = ReturnNode(ctx.start.line, expression)
        return lineNumberNodeMap[ctx.start.line] as ReturnNode
    }

    /** Statements **/

    override fun visitSleepStatement(ctx: SleepStatementContext): SleepNode {
        lineNumberNodeMap[ctx.start.line] = SleepNode(ctx.start.line, visit(ctx.expr()) as ExpressionNode)
        return lineNumberNodeMap[ctx.start.line] as SleepNode
    }

    private fun visitAndFlattenStatements(statementContext: StatContext?): List<StatementNode> {
        return if (statementContext != null) {
            flattenStatements(visit(statementContext) as StatementNode)
        } else {
            emptyList()
        }
    }

    private fun flattenStatements(statement: StatementNode): List<StatementNode> {
        val statements = mutableListOf<StatementNode>()
        var statementNode = statement
        // Flatten consecutive statements
        while (statementNode is ConsecutiveStatementNode) {
            statements.addAll(flattenStatements(statementNode.stat1))
            statementNode = statementNode.stat2
        }
        statements.add(statementNode)
        return statements
    }

    override fun visitConsecutiveStatement(ctx: ConsecutiveStatementContext): ASTNode {
        return ConsecutiveStatementNode(visit(ctx.stat1) as StatementNode, visit(ctx.stat2) as StatementNode)
    }

    override fun visitDeclarationStatement(ctx: DeclarationStatementContext): DeclarationNode {
        val identifier = ctx.IDENT().symbol.text
        semanticAnalyser.redeclaredVariableCheck(symbolTable, identifier, ctx)

        val rhs = visit(ctx.expr()) as ExpressionNode

        var rhsType = semanticAnalyser.inferType(symbolTable, rhs)
        val lhsType = if (ctx.type() != null) {
            visit(ctx.type()) as Type
        } else {
            rhsType
        }

        if (rhs is FunctionCallNode && symbolTable.getTypeOf(rhs.functionIdentifier) != ErrorType) {
            val functionData = symbolTable.getData(rhs.functionIdentifier) as FunctionData
            semanticAnalyser.incompatibleMultipleFunctionCall(rhs.functionIdentifier, functionData, lhsType, ctx)
            if (functionData.inferred && functionData.firstTime) {
                functionData.type = lhsType
                rhsType = lhsType
                functionData.firstTime = false
            }
        }

        semanticAnalyser.voidTypeDeclarationCheck(rhsType, identifier, ctx)
        semanticAnalyser.incompatibleTypesCheck(lhsType, rhsType, identifier, ctx)

        symbolTable.addVariable(identifier, IdentifierData(rhsType))
        lineNumberNodeMap[ctx.start.line] = DeclarationNode(ctx.start.line, identifier, rhs)
        return lineNumberNodeMap[ctx.start.line] as DeclarationNode
    }

    override fun visitAssignmentStatement(ctx: AssignmentStatementContext): AssignmentNode {
        val expression = visit(ctx.expr()) as ExpressionNode
        val lhs = ctx.assignment_lhs().text
        val lhsType = (visit(ctx.assignment_lhs()) as Type)
        var rhsType = semanticAnalyser.inferType(symbolTable, expression)

        if (expression is FunctionCallNode && symbolTable.getTypeOf(expression.functionIdentifier) != ErrorType) {
            val functionData = symbolTable.getData(expression.functionIdentifier) as FunctionData
            if (functionData.inferred) {
                functionData.type = lhsType
                rhsType = lhsType
            }
        }

        semanticAnalyser.incompatibleTypesCheck(lhsType, rhsType, lhs, ctx)
        lineNumberNodeMap[ctx.start.line] = AssignmentNode(ctx.start.line, lhs, expression)
        return lineNumberNodeMap[ctx.start.line] as AssignmentNode
    }

    override fun visitIdentifierAssignment(ctx: IdentifierAssignmentContext): Type {
        val identifier = ctx.IDENT().symbol.text

        semanticAnalyser.undeclaredIdentifierCheck(symbolTable, identifier, ctx)
        return symbolTable.getTypeOf(identifier)
    }

    override fun visitArrayElemAssignment(ctx: ArrayElemAssignmentContext): Type {
        val arrayAccessNode = visit(ctx.array_elem()) as ArrayElemNode
        val arrayType = symbolTable.getTypeOf(arrayAccessNode.arrayIdentifier)

        // Return element type
        return if (arrayType is ArrayType) {
            arrayType.internalType
        } else {
            ErrorType
        }
    }

    override fun visitMethodCallStatement(ctx: MethodCallStatementContext): ASTNode {
        return visit(ctx.method_call())
    }

    override fun visitCommentStatement(ctx: CommentStatementContext): CommentNode {
        // Command command given for render purposes
        lineNumberNodeMap[ctx.start.line] = CommentNode(ctx.start.line, ctx.STRING().text)
        return lineNumberNodeMap[ctx.start.line] as CommentNode
    }

    override fun visitIfStatement(ctx: IfStatementContext): ASTNode {
        // if
        val ifScope = symbolTable.enterScope()
        val ifCondition = visit(ctx.ifCond) as ExpressionNode
        semanticAnalyser.checkExpressionTypeWithExpectedType(ifCondition, BoolType, symbolTable, ctx)
        val ifStatements = visitAndFlattenStatements(ctx.ifStat)
        symbolTable.leaveScope()

        // elif
        val elifs = ctx.elseIf().map { visit(it) as ElifNode }

        // else
        val elseNode = if (ctx.elseStat != null) {
            val scope = symbolTable.enterScope()
            val statements = visitAndFlattenStatements(ctx.elseStat)
            symbolTable.leaveScope()
            ElseNode(ctx.elseStat.start.line - 1, scope, statements)
        } else {
            ElseNode(ctx.stop.line, 0, emptyList())
        }

        ctx.ELSE()?.let { lineNumberNodeMap[it.symbol.line - 1] = elseNode }

        val ifStatementNode =
            IfStatementNode(ctx.start.line, ctx.stop.line, ifScope, ifCondition, ifStatements, elifs, elseNode)
        lineNumberNodeMap[ctx.start.line] = ifStatementNode
        return ifStatementNode
    }

    override fun visitElseIf(ctx: ElseIfContext): ASTNode {
        val elifScope = symbolTable.enterScope()

        val elifCondition = visit(ctx.elifCond) as ExpressionNode
        semanticAnalyser.checkExpressionTypeWithExpectedType(elifCondition, BoolType, symbolTable, ctx)
        val elifStatements = visitAndFlattenStatements(ctx.elifStat)

        symbolTable.leaveScope()

        val elifNode = ElifNode(ctx.elifCond.start.line, elifScope, elifCondition, elifStatements)
        lineNumberNodeMap[ctx.elifCond.start.line] = elifNode
        return elifNode
    }

    /** Expressions **/

    override fun visitMethodCallExpression(ctx: MethodCallExpressionContext): ASTNode {
        return visit(ctx.method_call())
    }

    override fun visitArgumentList(ctx: ArgumentListContext?): ArgumentNode {
        return ArgumentNode((ctx?.expr()
            ?: listOf<ExprContext>()).map { visit(it) as ExpressionNode })
    }

    override fun visitMethodCall(ctx: MethodCallContext): MethodCallNode {
        // Type signature of methods to be determined by symbol table
        val arguments: List<ExpressionNode> =
            visitArgumentList(ctx.arg_list() as ArgumentListContext?).arguments
        val identifier = ctx.IDENT(0).symbol.text
        val methodName = ctx.IDENT(1).symbol.text

        semanticAnalyser.undeclaredIdentifierCheck(symbolTable, identifier, ctx)
        semanticAnalyser.notDataStructureCheck(symbolTable, identifier, ctx)
        semanticAnalyser.notValidMethodNameForDataStructureCheck(symbolTable, identifier, methodName, ctx)

        val dataStructureType = symbolTable.getTypeOf(identifier)

        val dataStructureMethod = if (dataStructureType is DataStructureType) {
            val method = dataStructureType.getMethodByName(methodName)

            // Assume for now we only have one type inside the data structure and data structure functions only deal with this type
            val argTypes = arguments.map { semanticAnalyser.inferType(symbolTable, it) }.toList()
            semanticAnalyser.primitiveArgTypesCheck(argTypes, methodName, dataStructureType, ctx)
            semanticAnalyser.incompatibleArgumentTypesCheck(
                dataStructureType,
                argTypes,
                method,
                ctx.arg_list()
            )
            method

        } else {
            ErrorMethod
        }

        lineNumberNodeMap[ctx.start.line] = MethodCallNode(ctx.start.line, ctx.IDENT(0).symbol.text, dataStructureMethod, arguments)
        return lineNumberNodeMap[ctx.start.line] as MethodCallNode
    }

    override fun visitFunctionCall(ctx: FunctionCallContext): FunctionCallNode {
        val identifier = ctx.IDENT().symbol.text
        val arguments: List<ExpressionNode> =
            visitArgumentList(ctx.arg_list() as ArgumentListContext?).arguments
        val argTypes = arguments.map { semanticAnalyser.inferType(symbolTable, it) }.toList()

        semanticAnalyser.undeclaredFunctionCheck(symbolTable, identifier, inFunction, argTypes, ctx)
        semanticAnalyser.invalidNumberOfArgumentsForFunctionsCheck(identifier, symbolTable, arguments.size, ctx)
        semanticAnalyser.incompatibleArgumentTypesForFunctionsCheck(identifier, symbolTable, argTypes, ctx)

        val functionCallNode = FunctionCallNode(ctx.start.line, ctx.IDENT().symbol.text, arguments)

        lineNumberNodeMap[ctx.start.line] = functionCallNode

        return functionCallNode
    }

    override fun visitDataStructureContructor(ctx: DataStructureContructorContext): ASTNode {
        val dataStructureType = visit(ctx.data_structure_type()) as DataStructureType

        // Check arguments
        val arguments = if (ctx.arg_list() != null) {
            val expressions = (visit(ctx.arg_list()) as ArgumentNode).arguments
            val argumentTypes = expressions.map {
                semanticAnalyser.inferType(symbolTable, it)
            }
            semanticAnalyser.incompatibleArgumentTypesCheck(
                dataStructureType,
                argumentTypes,
                dataStructureType.getConstructor(),
                ctx.arg_list()
            )
            expressions
        } else {
            emptyList()
        }

        // Check intial values
        val initialValue = if (ctx.data_structure_initialiser() != null) {
            val initialiser = visit(ctx.data_structure_initialiser()) as DataStructureInitialiserNode
            semanticAnalyser.allExpressionsAreSameTypeCheck(
                dataStructureType.internalType,
                initialiser.expressions,
                symbolTable,
                ctx
            )
            initialiser.expressions
        } else {
            emptyList()
        }

        return ConstructorNode(ctx.start.line, dataStructureType, arguments, initialValue)
    }

    override fun visitIdentifier(ctx: IdentifierContext): IdentifierNode {
        return IdentifierNode(ctx.start.line, ctx.text)
    }

    override fun visitBinaryExpression(ctx: BinaryExpressionContext): BinaryExpression {
        val expr1 = visit(ctx.left) as ExpressionNode
        val expr2 = visit(ctx.right) as ExpressionNode
        if (expr1 is IdentifierNode) {
            semanticAnalyser.undeclaredIdentifierCheck(symbolTable, expr1.identifier, ctx)
        }
        if (expr2 is IdentifierNode) {
            semanticAnalyser.undeclaredIdentifierCheck(symbolTable, expr2.identifier, ctx)
        }

        val binaryOpExpr = when (ctx.binary_operator.type) {
            ADD -> AddExpression(ctx.start.line, expr1, expr2)
            MINUS -> SubtractExpression(ctx.start.line, expr1, expr2)
            TIMES -> MultiplyExpression(ctx.start.line, expr1, expr2)
            AND -> AndExpression(ctx.start.line, expr1, expr2)
            OR -> OrExpression(ctx.start.line, expr1, expr2)
            EQ -> EqExpression(ctx.start.line, expr1, expr2)
            NEQ -> NeqExpression(ctx.start.line, expr1, expr2)
            GT -> GtExpression(ctx.start.line, expr1, expr2)
            GE -> GeExpression(ctx.start.line, expr1, expr2)
            LT -> LtExpression(ctx.start.line, expr1, expr2)
            LE -> LeExpression(ctx.start.line, expr1, expr2)
            else -> throw UnsupportedOperationException("Operation not supported")
        }

        semanticAnalyser.incompatibleOperatorTypeCheck(ctx.binary_operator.text, binaryOpExpr, symbolTable, ctx)

        return binaryOpExpr
    }

    override fun visitUnaryOperator(ctx: UnaryOperatorContext): UnaryExpression {
        val expr = visit(ctx.expr()) as ExpressionNode
        if (expr is IdentifierNode) {
            semanticAnalyser.undeclaredIdentifierCheck(symbolTable, expr.identifier, ctx)
        }
        val unaryOpExpr = when (ctx.unary_operator.type) {
            ADD -> PlusExpression(ctx.start.line, expr)
            MINUS -> MinusExpression(ctx.start.line, expr)
            NOT -> NotExpression(ctx.start.line, expr)
            else -> throw UnsupportedOperationException("Operation not supported")
        }

        semanticAnalyser.incompatibleOperatorTypeCheck(ctx.unary_operator.text, unaryOpExpr, symbolTable, ctx)

        return unaryOpExpr
    }

    /** Literals **/

    override fun visitNumberLiteral(ctx: NumberLiteralContext): NumberNode {
        return NumberNode(ctx.start.line, ctx.text.toDouble())
    }

    override fun visitBooleanLiteral(ctx: BooleanLiteralContext): ASTNode {
        return BoolNode(ctx.start.line, ctx.bool().text.toBoolean())
    }

    /** Types **/

    override fun visitPrimitiveType(ctx: PrimitiveTypeContext): PrimitiveType {
        return visit(ctx.primitive_type()) as PrimitiveType
    }

    override fun visitNumberType(ctx: NumberTypeContext): NumberType {
        return NumberType
    }

    override fun visitBoolType(ctx: BoolTypeContext): ASTNode {
        return BoolType
    }

    override fun visitDataStructureType(ctx: DataStructureTypeContext): DataStructureType {
        return visit(ctx.data_structure_type()) as DataStructureType
    }

    override fun visitStackType(ctx: StackTypeContext): StackType {
        // Stack only contains primitives as per grammar
        val containerType = visit(ctx.primitive_type()) as PrimitiveType
        return StackType(containerType)
    }

    override fun visitArrayType(ctx: ArrayTypeContext): ArrayType {
        val elementType = visit(ctx.primitive_type()) as Type
        return ArrayType(elementType)
    }

    override fun visitData_structure_initialiser(ctx: Data_structure_initialiserContext): ASTNode {
        return DataStructureInitialiserNode(ctx.expr().map { visit(it) as ExpressionNode })
    }

    override fun visitArray_elem(ctx: Array_elemContext): ArrayElemNode {
        val arrayIdentifier = ctx.IDENT().symbol.text
        val index = visit(ctx.expr()) as ExpressionNode

        semanticAnalyser.undeclaredIdentifierCheck(symbolTable, arrayIdentifier, ctx)
        semanticAnalyser.checkExpressionTypeWithExpectedType(index, NumberType, symbolTable, ctx)
        return ArrayElemNode(ctx.start.line, arrayIdentifier, index)
    }
}