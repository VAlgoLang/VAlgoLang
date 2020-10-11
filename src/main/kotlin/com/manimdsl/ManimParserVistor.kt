package com.manimdsl

import antlr.ManimParser
import antlr.ManimParserBaseVisitor

class ManimParserVisitor: ManimParserBaseVisitor<ASTNode>() {
    override fun visitProgram(ctx: ManimParser.ProgramContext?): ASTNode {
        return super.visitProgram(ctx)
    }

    override fun visitSleepStatement(ctx: ManimParser.SleepStatementContext?): ASTNode {
        return super.visitSleepStatement(ctx)
    }

    override fun visitDeclarationStatement(ctx: ManimParser.DeclarationStatementContext?): ASTNode {
        return super.visitDeclarationStatement(ctx)
    }

    override fun visitAssignmentStatement(ctx: ManimParser.AssignmentStatementContext?): ASTNode {
        return super.visitAssignmentStatement(ctx)
    }

    override fun visitMethodCallStatement(ctx: ManimParser.MethodCallStatementContext?): ASTNode {
        return super.visitMethodCallStatement(ctx)
    }

    override fun visitArgumentList(ctx: ManimParser.ArgumentListContext?): ASTNode {
        return super.visitArgumentList(ctx)
    }

    override fun visitMethodCall(ctx: ManimParser.MethodCallContext?): ASTNode {
        return super.visitMethodCall(ctx)
    }

    override fun visitStackCreate(ctx: ManimParser.StackCreateContext?): ASTNode {
        return super.visitStackCreate(ctx)
    }

    override fun visitIdentifier(ctx: ManimParser.IdentifierContext?): ASTNode {
        return super.visitIdentifier(ctx)
    }

    override fun visitMethodCallExpression(ctx: ManimParser.MethodCallExpressionContext?): ASTNode {
        return super.visitMethodCallExpression(ctx)
    }

    override fun visitBinaryExpression(ctx: ManimParser.BinaryExpressionContext?): ASTNode {
        return super.visitBinaryExpression(ctx)
    }

    override fun visitUnaryOperator(ctx: ManimParser.UnaryOperatorContext?): ASTNode {
        return super.visitUnaryOperator(ctx)
    }

    override fun visitNumberLiteral(ctx: ManimParser.NumberLiteralContext?): ASTNode {
        return super.visitNumberLiteral(ctx)
    }

    override fun visitTypes(ctx: ManimParser.TypesContext?): ASTNode {
        return super.visitTypes(ctx)
    }
}