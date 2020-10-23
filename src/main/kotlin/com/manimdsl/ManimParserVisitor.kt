package com.manimdsl

import antlr.ManimParser
import antlr.ManimParser.*
import antlr.ManimParserBaseVisitor
import com.manimdsl.frontend.*

class ManimParserVisitor : ManimParserBaseVisitor<ASTNode>() {
    val symbolTable = SymbolTableVisitor()

    val lineNumberNodeMap = mutableMapOf<Int, ASTNode>()

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

        val scope = symbolTable.enterScope()
        val parameters: List<ParameterNode> =
            visitParameterList(ctx.param_list() as ParameterListContext?).parameters
        val statements =
            if (ctx.statements != null) flattenStatements(visit(ctx.statements) as StatementNode) else emptyList()
        symbolTable.leaveScope()

        semanticAnalyser.redeclaredFunctionCheck(symbolTable, identifier, type, parameters, ctx)

        if (functionReturnType !is VoidType) {
            semanticAnalyser.missingReturnCheck(identifier, statements, functionReturnType, ctx)
        }

        symbolTable.addVariable(identifier, FunctionData(inferred = false, firstTime = false, parameters = parameters, type = type))

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
        val expression = visit(ctx.expr()) as ExpressionNode
        semanticAnalyser.incompatibleReturnTypesCheck(symbolTable, functionReturnType, expression, ctx)
        lineNumberNodeMap[ctx.start.line] = ReturnNode(ctx.start.line, expression)
        return lineNumberNodeMap[ctx.start.line] as ReturnNode

    }

    /** Statements **/

    override fun visitSleepStatement(ctx: SleepStatementContext): SleepNode {
        lineNumberNodeMap[ctx.start.line] = SleepNode(ctx.start.line, visit(ctx.expr()) as ExpressionNode)
        return lineNumberNodeMap[ctx.start.line] as SleepNode
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
        val identifier = ctx.IDENT().symbol.text
        var rhsType = semanticAnalyser.inferType(symbolTable, expression)
        val identifierType = symbolTable.getTypeOf(identifier)

        if (expression is FunctionCallNode && symbolTable.getTypeOf(expression.functionIdentifier) != ErrorType) {
            val functionData = symbolTable.getData(expression.functionIdentifier) as FunctionData
            if (functionData.inferred) {
                functionData.type = identifierType
                rhsType = identifierType
            }
        }

        semanticAnalyser.undeclaredIdentifierCheck(symbolTable, identifier, ctx)
        semanticAnalyser.incompatibleTypesCheck(identifierType, rhsType, identifier, ctx)
        lineNumberNodeMap[ctx.start.line] = AssignmentNode(ctx.start.line, identifier, expression)
        return lineNumberNodeMap[ctx.start.line] as AssignmentNode
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
        val ifStatements =
            if (ctx.ifStat != null) flattenStatements(visit(ctx.ifStat) as StatementNode) else emptyList()
        symbolTable.leaveScope()

        // elif
        val elifs = ctx.elseIf().map { visit(it) as Elif }

        // else
        val (elseScope, elseStatement) = if (ctx.elseStat != null) {
            val scope = symbolTable.enterScope()
            val statements = flattenStatements(visit(ctx.elseStat) as StatementNode)
            symbolTable.leaveScope()
            Pair(scope, statements)
        } else {
            Pair(0, emptyList())
        }

        return IfStatement(ctx.start.line, ifScope, ifCondition, ifStatements, elifs, elseScope, elseStatement)
    }

    override fun visitElseIf(ctx: ElseIfContext): ASTNode {
        val elifScope = symbolTable.enterScope()

        val elifCondition = visit(ctx.elifCond) as ExpressionNode
        semanticAnalyser.checkExpressionTypeWithExpectedType(elifCondition, BoolType, symbolTable, ctx)
        val elifStatements =
            if (ctx.elifStat != null) flattenStatements(visit(ctx.elifStat) as StatementNode) else emptyList()

        symbolTable.leaveScope()

        return Elif(elifScope, elifCondition, elifStatements)
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
            semanticAnalyser.invalidNumberOfArgumentsCheck(dataStructureType, methodName, arguments.size, ctx)

            val method = dataStructureType.getMethodByName(methodName)

            // Assume for now we only have one type inside the data structure and data structure functions only deal with this type
            val argTypes = arguments.map { semanticAnalyser.inferType(symbolTable, it) }.toList()
            semanticAnalyser.primitiveArgTypesCheck(argTypes, methodName, dataStructureType, ctx)
            semanticAnalyser.incompatibleArgumentTypesCheck(
                dataStructureType,
                argTypes,
                method,
                ctx
            )
            method

        } else {
            ErrorMethod
        }

        val node = MethodCallNode(ctx.start.line, ctx.IDENT(0).symbol.text, dataStructureMethod, arguments)

        if (dataStructureMethod.returnType is ErrorType) {
            lineNumberNodeMap[ctx.start.line] = node
        }
        return node
    }

    override fun visitFunctionCall(ctx: FunctionCallContext): FunctionCallNode {
        val identifier = ctx.IDENT().symbol.text
        val arguments: List<ExpressionNode> =
                visitArgumentList(ctx.arg_list() as ArgumentListContext?).arguments
        val argTypes = arguments.map { semanticAnalyser.inferType(symbolTable, it) }.toList()

        semanticAnalyser.undeclaredFunctionCheck(symbolTable, identifier, inFunction, argTypes, ctx)
        semanticAnalyser.invalidNumberOfArgumentsForFunctionsCheck(identifier, symbolTable, arguments.size, ctx)
        semanticAnalyser.incompatibleArgumentTypesForFunctionsCheck(identifier, symbolTable, argTypes, ctx)

        return FunctionCallNode(ctx.start.line, ctx.IDENT().symbol.text, arguments)
    }

    override fun visitDataStructureContructor(ctx: DataStructureContructorContext): ASTNode {
        return ConstructorNode(ctx.start.line, visit(ctx.data_structure_type()) as DataStructureType, listOf())
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
}