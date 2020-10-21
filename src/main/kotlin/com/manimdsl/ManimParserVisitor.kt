package com.manimdsl

import antlr.ManimParser.*
import antlr.ManimParserBaseVisitor
import com.manimdsl.frontend.*

class ManimParserVisitor : ManimParserBaseVisitor<ASTNode>() {
    val symbolTable = SymbolTableVisitor()
    private val semanticAnalyser = SemanticAnalysis()

    /** Program **/

    override fun visitProgram(ctx: ProgramContext): ProgramNode {
        return ProgramNode(ctx.stat().map { visit(it) as StatementNode })
    }

    /** Statements **/

    override fun visitSleepStatement(ctx: SleepStatementContext): SleepNode {
        return SleepNode(visit(ctx.expr()) as ExpressionNode)
    }

    override fun visitDeclarationStatement(ctx: DeclarationStatementContext): DeclarationNode {
        val identifier = ctx.IDENT().symbol.text
        semanticAnalyser.redeclaredVariableCheck(symbolTable, identifier, ctx)

        val rhs = visit(ctx.expr()) as ExpressionNode

        val rhsType = semanticAnalyser.inferType(symbolTable, rhs)
        val lhsType = if (ctx.type() != null) {
            visit(ctx.type()) as Type
        } else {
            rhsType
        }

        semanticAnalyser.incompatibleTypesCheck(lhsType, rhsType, identifier, ctx)

        symbolTable.addVariable(identifier, IdentifierData(rhsType))
        return DeclarationNode(ctx.start.line, identifier, rhs)
    }

    override fun visitAssignmentStatement(ctx: AssignmentStatementContext): AssignmentNode {
        val expression = visit(ctx.expr()) as ExpressionNode
        val identifier = ctx.IDENT().symbol.text
        val rhsType = semanticAnalyser.inferType(symbolTable, expression)
        val identifierType = symbolTable.getTypeOf(identifier)

        semanticAnalyser.undeclaredIdentifierCheck(symbolTable, identifier, ctx)
        semanticAnalyser.incompatibleTypesCheck(identifierType, rhsType, identifier, ctx)
        return AssignmentNode(ctx.start.line, identifier, expression)
    }

    override fun visitMethodCallStatement(ctx: MethodCallStatementContext): MethodCallNode {
        return visitMethodCall(ctx.method_call() as MethodCallContext)
    }

    override fun visitCommentStatement(ctx: CommentStatementContext): CommentNode {
        // Command command given for render purposes
        return CommentNode(ctx.STRING().text)
    }

    /** Expressions **/

    override fun visitMethodCallExpression(ctx: MethodCallExpressionContext): MethodCallNode {
        return visitMethodCall(ctx.method_call() as MethodCallContext)
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

        return MethodCallNode(ctx.start.line, ctx.IDENT(0).symbol.text, dataStructureMethod, arguments)
    }

    override fun visitDataStructureContructor(ctx: DataStructureContructorContext): ASTNode {
        return ConstructorNode(ctx.start.line, visit(ctx.data_structure_type()) as DataStructureType, listOf())
    }

    override fun visitIdentifier(ctx: IdentifierContext): IdentifierNode {
        return IdentifierNode(ctx.start.line, ctx.text)
    }

    override fun visitBinaryExpression(ctx: BinaryExpressionContext): BinaryExpression {
        val expr1 = visit(ctx.expr(0)) as ExpressionNode
        val expr2 = visit(ctx.expr(1)) as ExpressionNode
        if (expr1 is IdentifierNode) {
            semanticAnalyser.undeclaredIdentifierCheck(symbolTable, expr1.identifier, ctx)
        }
        if (expr2 is IdentifierNode) {
            semanticAnalyser.undeclaredIdentifierCheck(symbolTable, expr2.identifier, ctx)
        }
        return when (ctx.binary_operator.type) {
            ADD -> AddExpression(ctx.start.line, expr1, expr2)
            MINUS -> SubtractExpression(ctx.start.line, expr1, expr2)
            else -> MultiplyExpression(ctx.start.line, expr1, expr2)
        }
    }

    override fun visitUnaryOperator(ctx: UnaryOperatorContext): UnaryExpression {
        val expr = visit(ctx.expr()) as ExpressionNode
        if (expr is IdentifierNode) {
            semanticAnalyser.undeclaredIdentifierCheck(symbolTable, expr.identifier, ctx)
        }
        return when (ctx.unary_operator.type) {
            ADD -> PlusExpression(ctx.start.line, expr)
            else -> MinusExpression(ctx.start.line, expr)
        }
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