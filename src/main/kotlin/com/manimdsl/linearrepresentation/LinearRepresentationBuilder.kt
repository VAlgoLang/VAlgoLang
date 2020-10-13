package com.manimdsl.linearrepresentation

import com.manimdsl.ASTExecutor
import com.manimdsl.frontend.*

class LinearRepresentationBuilder(symbolTable: SymbolTable, astExecutor: ASTExecutor) :
    AbstractSyntaxTreeVisitor<ManimInstr> {

    /** All statements making up program **/
    override fun visitProgramNode(programNode: ProgramNode): ManimInstr? {
        TODO("Not yet implemented")
    }

    override fun visitStatementNode(statementNode: StatementNode): ManimInstr? {
        TODO("Not yet implemented")
    }

    /** Animation Command Specific type for easy detection **/
    override fun visitAnimationNode(animationNode: AnimationNode): ManimInstr? {
        TODO("Not yet implemented")
    }

    override fun visitSleepNode(sleepNode: SleepNode): ManimInstr? {
        TODO("Not yet implemented")
    }

    override fun visitCommentNode(commentNode: CommentNode): ManimInstr? {
        TODO("Not yet implemented")
    }

    /** Code Specific Nodes holding line number **/
    override fun visitCodeNode(codeNode: CodeNode): ManimInstr? {
        TODO("Not yet implemented")
    }

    override fun visitDeclarationNode(declarationNode: DeclarationNode): ManimInstr? {
        TODO("Not yet implemented")
    }

    override fun visitAssignmentNode(assignmentNode: AssignmentNode): ManimInstr? {
        TODO("Not yet implemented")
    }

    /** Expressions **/
    override fun visitExpressionNode(expressionNode: ExpressionNode): ManimInstr? {
        TODO("Not yet implemented")
    }

    override fun visitIdentifierNode(identifierNode: IdentifierNode): ManimInstr? {
        TODO("Not yet implemented")
    }

    override fun visitNumberNode(numberNode: NumberNode): ManimInstr? {
        TODO("Not yet implemented")
    }

    override fun visitMethodCallNode(methodCallNode: MethodCallNode): ManimInstr? {
        TODO("Not yet implemented")
    }

    override fun visitConstructorNode(constructorNode: ConstructorNode): ManimInstr? {
        TODO("Not yet implemented")
    }

    /** Binary Expressions **/
    override fun visitBinaryExpression(binaryExpression: BinaryExpression): ManimInstr? {
        TODO("Not yet implemented")
    }

    override fun visitAddExpression(addExpression: AddExpression): ManimInstr? {
        TODO("Not yet implemented")
    }

    override fun visitSubtractExpression(subtractExpression: SubtractExpression): ManimInstr? {
        TODO("Not yet implemented")
    }

    override fun visitMultiplyExpression(multiplyExpression: MultiplyExpression): ManimInstr? {
        TODO("Not yet implemented")
    }

    /** Unary Expressions **/
    override fun visitUnaryExpression(unaryExpression: UnaryExpression): ManimInstr? {
        TODO("Not yet implemented")
    }

    override fun visitPlusExpression(plusExpression: PlusExpression): ManimInstr? {
        TODO("Not yet implemented")
    }

    override fun visitMinusExpression(minusExpression: MinusExpression): ManimInstr? {
        TODO("Not yet implemented")
    }
}