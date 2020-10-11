package com.manimdsl

import antlr.ManimParser
import antlr.ManimParserBaseVisitor

class ManimParserVisitor: ManimParserBaseVisitor<ASTNode>() {
    override fun visitProgram(ctx: ManimParser.ProgramContext): ProgramNode {
        return ProgramNode(ctx.stat().map { visit(it) as StatementNode })
    }

    override fun visitSleepStatement(ctx: ManimParser.SleepStatementContext): SleepNode {
        return SleepNode(visit(ctx.expr()) as ExpressionNode)
    }

    override fun visitDeclarationStatement(ctx: ManimParser.DeclarationStatementContext): DeclarationNode {
        // Type work done in Symbol Table - we can keep it entirely external or can reference from the AST Node here
        return DeclarationNode(ctx.start.line, ctx.IDENT().symbol.text, visit(ctx.expr()) as ExpressionNode)
    }

    override fun visitAssignmentStatement(ctx: ManimParser.AssignmentStatementContext): AssignmentNode {
        return AssignmentNode(ctx.start.line, ctx.IDENT().symbol.text, visit(ctx.expr()) as ExpressionNode)
    }

    override fun visitMethodCallStatement(ctx: ManimParser.MethodCallStatementContext): MethodCallNode {
        // This one ignores return value/is a command returning void
        return visitMethodCall(ctx.method_call() as ManimParser.MethodCallContext)
    }

    override fun visitArgumentList(ctx: ManimParser.ArgumentListContext): ArgumentNode {
        return ArgumentNode(ctx.expr().map { visit(it) as ExpressionNode })
    }

    override fun visitMethodCall(ctx: ManimParser.MethodCallContext): MethodCallNode {
        // Type signature of methods to be determined by symbol table
        val arguments = visitArgumentList(ctx.arg_list() as ManimParser.ArgumentListContext).arguments
        return MethodCallNode(ctx.start.line, ctx.IDENT()[0].symbol.text, ctx.IDENT()[1].symbol.text, arguments)
    }

    override fun visitStackCreate(ctx: ManimParser.StackCreateContext?): ASTNode {
        return super.visitStackCreate(ctx)
    }

    override fun visitIdentifier(ctx: ManimParser.IdentifierContext): IdentifierNode {
        return IdentifierNode(ctx.text, ctx.start.line)
    }

    override fun visitMethodCallExpression(ctx: ManimParser.MethodCallExpressionContext): MethodCallNode {
        return visitMethodCall(ctx.method_call() as ManimParser.MethodCallContext)
    }

    override fun visitBinaryExpression(ctx: ManimParser.BinaryExpressionContext?): ASTNode {
        return super.visitBinaryExpression(ctx)
    }

    override fun visitUnaryOperator(ctx: ManimParser.UnaryOperatorContext?): ASTNode {
        return super.visitUnaryOperator(ctx)
    }

    override fun visitNumberLiteral(ctx: ManimParser.NumberLiteralContext): NumberNode {
        return NumberNode(ctx.NUMBER().symbol.text.toDouble(), ctx.start.line)
    }

    override fun visitIntType(ctx: ManimParser.IntTypeContext): IntType {
        return IntType
    }

    override fun visitStackType(ctx: ManimParser.StackTypeContext): StackType {
        return StackType
    }
}