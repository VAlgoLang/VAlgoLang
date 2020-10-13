package com.manimdsl.frontend

interface AbstractSyntaxTreeVisitor<T> {

    /** All statements making up program **/
    fun visitProgramNode(programNode: ProgramNode): T?

    fun visitStatementNode(statementNode: StatementNode): T?

    /** Animation Command Specific type for easy detection **/
    fun visitAnimationNode(animationNode: AnimationNode): T?

    fun visitSleepNode(sleepNode: SleepNode): T?

    fun visitCommentNode(commentNode: CommentNode): T?

    /** Code Specific Nodes holding line number **/
    fun visitCodeNode(codeNode: CodeNode): T?

    fun visitDeclarationNode(declarationNode: DeclarationNode): T?

    fun visitAssignmentNode(assignmentNode: AssignmentNode): T?

    /** Expressions **/
    fun visitExpressionNode(expressionNode: ExpressionNode): T?

    fun visitIdentifierNode(identifierNode: IdentifierNode): T?

    fun visitNumberNode(numberNode: NumberNode): T?

    fun visitMethodCallNode(methodCallNode: MethodCallNode): T?

    fun visitConstructorNode(constructorNode: ConstructorNode): T?

    /** Binary Expressions **/
    fun visitBinaryExpression(binaryExpression: BinaryExpression): T?

    fun visitAddExpression(addExpression: AddExpression): T?

    fun visitSubtractExpression(subtractExpression: SubtractExpression): T?

    fun visitMultiplyExpression(multiplyExpression: MultiplyExpression): T?

    /** Unary Expressions **/
    fun visitUnaryExpression(unaryExpression: UnaryExpression): T?

    fun visitPlusExpression(plusExpression: PlusExpression): T?

    fun visitMinusExpression(minusExpression: MinusExpression): T?
}