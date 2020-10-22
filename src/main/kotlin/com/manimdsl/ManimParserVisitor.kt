package com.manimdsl

import antlr.ManimParser
import antlr.ManimParserBaseVisitor
import com.manimdsl.frontend.*

class ManimParserVisitor : ManimParserBaseVisitor<ASTNode>() {
    val symbolTable = SymbolTableVisitor()

    val lineNumberNodeMap = mutableMapOf<Int, ASTNode>()

    private val semanticAnalyser = SemanticAnalysis()
    private var inFunction: Boolean = false
    private var functionReturnType: Type = VoidType

    override fun visitProgram(ctx: ManimParser.ProgramContext): ProgramNode {
        val functions = ctx.function().map { visit(it) as FunctionNode }
        semanticAnalyser.tooManyInferredFunctionsCheck(symbolTable, ctx)
        return ProgramNode(
                functions,
                ctx.stat().map { visit(it) as StatementNode })
    }

    override fun visitFunction(ctx: ManimParser.FunctionContext): FunctionNode {
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
            visitParameterList(ctx.param_list() as ManimParser.ParameterListContext?).parameters
        val statements = ctx.stat().map { visit(it) as StatementNode }
        symbolTable.leaveScope()

        semanticAnalyser.redeclaredFunctionCheck(symbolTable, identifier, type, parameters, ctx)

        if (functionReturnType !is VoidType) {
            semanticAnalyser.missingReturnCheck(identifier, statements, functionReturnType, ctx)
        }

        symbolTable.addVariable(identifier, FunctionData(inferred = false, firstTime = false, parameters = parameters, type = type))

        inFunction = false
        functionReturnType = VoidType
        val node = FunctionNode(ctx.start.line, scope, identifier, parameters, statements)
        lineNumberNodeMap[ctx.start.line] = node
        return node
    }

    override fun visitParameterList(ctx: ManimParser.ParameterListContext?): ParameterListNode{
        if (ctx == null) {
            return ParameterListNode(listOf())
        }
        return ParameterListNode(ctx.param().map { visit(it) as ParameterNode })
    }

    override fun visitParameter(ctx: ManimParser.ParameterContext): ParameterNode {
        val identifier = ctx.IDENT().symbol.text
        semanticAnalyser.redeclaredVariableCheck(symbolTable, identifier, ctx)

        val type = visit(ctx.type()) as Type
        symbolTable.addVariable(identifier, IdentifierData(type))
        return ParameterNode(identifier, type)
    }

    override fun visitReturnStatement(ctx: ManimParser.ReturnStatementContext): ReturnNode {
        semanticAnalyser.globalReturnCheck(inFunction, ctx)
        val expression = visit(ctx.expr()) as ExpressionNode
        semanticAnalyser.incompatibleReturnTypesCheck(symbolTable, functionReturnType, expression, ctx)
        lineNumberNodeMap[ctx.start.line] = ReturnNode(ctx.start.line, expression)
        return lineNumberNodeMap[ctx.start.line] as ReturnNode

    }

    override fun visitSleepStatement(ctx: ManimParser.SleepStatementContext): SleepNode {
        lineNumberNodeMap[ctx.start.line] = SleepNode(ctx.start.line, visit(ctx.expr()) as ExpressionNode)
        return lineNumberNodeMap[ctx.start.line] as SleepNode
    }

    override fun visitDeclarationStatement(ctx: ManimParser.DeclarationStatementContext): DeclarationNode {
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

    override fun visitAssignmentStatement(ctx: ManimParser.AssignmentStatementContext): AssignmentNode {
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
        val node = AssignmentNode(ctx.start.line, identifier, expression)
        lineNumberNodeMap[ctx.start.line] = node
        return node
    }

    override fun visitMethodCallStatement(ctx: ManimParser.MethodCallStatementContext): ASTNode {
        return visit(ctx.method_call())
    }

    override fun visitMethodCallExpression(ctx: ManimParser.MethodCallExpressionContext): ASTNode {
        return visit(ctx.method_call())
    }

    override fun visitArgumentList(ctx: ManimParser.ArgumentListContext?): ArgumentNode {
        return ArgumentNode((ctx?.expr()
            ?: listOf<ManimParser.ExprContext>()).map { visit(it) as ExpressionNode })
    }

    override fun visitMethodCall(ctx: ManimParser.MethodCallContext): MethodCallNode {
        // Type signature of methods to be determined by symbol table
        val arguments: List<ExpressionNode> =
            visitArgumentList(ctx.arg_list() as ManimParser.ArgumentListContext?).arguments
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

    override fun visitFunctionCall(ctx: ManimParser.FunctionCallContext): FunctionCallNode {
        val identifier = ctx.IDENT().symbol.text
        val arguments: List<ExpressionNode> =
                visitArgumentList(ctx.arg_list() as ManimParser.ArgumentListContext?).arguments
        val argTypes = arguments.map { semanticAnalyser.inferType(symbolTable, it) }.toList()

        semanticAnalyser.undeclaredFunctionCheck(symbolTable, identifier, inFunction, argTypes, ctx)
        semanticAnalyser.invalidNumberOfArgumentsForFunctionsCheck(identifier, symbolTable, arguments.size, ctx)
        semanticAnalyser.incompatibleArgumentTypesForFunctionsCheck(identifier, symbolTable, argTypes, ctx)

        return FunctionCallNode(ctx.start.line, ctx.IDENT().symbol.text, arguments)
    }

    override fun visitDataStructureContructor(ctx: ManimParser.DataStructureContructorContext): ASTNode {
        return ConstructorNode(ctx.start.line, visit(ctx.data_structure_type()) as DataStructureType, listOf())
    }

    override fun visitIdentifier(ctx: ManimParser.IdentifierContext): IdentifierNode {
        return IdentifierNode(ctx.start.line, ctx.text)
    }

    override fun visitBinaryExpression(ctx: ManimParser.BinaryExpressionContext): BinaryExpression {
        val expr1 = visit(ctx.expr(0)) as ExpressionNode
        val expr2 = visit(ctx.expr(1)) as ExpressionNode
        if (expr1 is IdentifierNode) {
            semanticAnalyser.undeclaredIdentifierCheck(symbolTable, expr1.identifier, ctx)
        }
        if (expr2 is IdentifierNode) {
            semanticAnalyser.undeclaredIdentifierCheck(symbolTable, expr2.identifier, ctx)
        }
        return when (ctx.binary_operator.type) {
            ManimParser.ADD -> AddExpression(ctx.start.line, expr1, expr2)
            ManimParser.MINUS -> SubtractExpression(ctx.start.line, expr1, expr2)
            else -> MultiplyExpression(ctx.start.line, expr1, expr2)
        }
    }

    override fun visitUnaryOperator(ctx: ManimParser.UnaryOperatorContext): UnaryExpression {
        val expr = visit(ctx.expr()) as ExpressionNode
        if (expr is IdentifierNode) {
            semanticAnalyser.undeclaredIdentifierCheck(symbolTable, expr.identifier, ctx)
        }
        return when (ctx.unary_operator.type) {
            ManimParser.ADD -> PlusExpression(ctx.start.line, expr)
            else -> MinusExpression(ctx.start.line, expr)
        }
    }

    override fun visitCommentStatement(ctx: ManimParser.CommentStatementContext): CommentNode {
        // Command command given for render purposes
        val node = CommentNode(ctx.start.line, ctx.STRING().text)
        lineNumberNodeMap[ctx.start.line] = node
        return node
    }

    override fun visitNumberLiteral(ctx: ManimParser.NumberLiteralContext): NumberNode {
        return NumberNode(ctx.start.line, ctx.NUMBER().symbol.text.toDouble())
    }

    override fun visitNumberType(ctx: ManimParser.NumberTypeContext): NumberType {
        return NumberType
    }

    override fun visitDataStructureType(ctx: ManimParser.DataStructureTypeContext): DataStructureType {
        return visit(ctx.data_structure_type()) as DataStructureType
    }

    override fun visitPrimitiveType(ctx: ManimParser.PrimitiveTypeContext): PrimitiveType {
        return visit(ctx.primitive_type()) as PrimitiveType
    }

    override fun visitStackType(ctx: ManimParser.StackTypeContext): StackType {
        // Stack only contains primitives as per grammar
        val containerType = visit(ctx.primitive_type()) as PrimitiveType
        return StackType(containerType)
    }
}