package com.manimdsl

sealed class ASTNode
data class ProgramNode(val statements: List<StatementNode>): ASTNode()

// All statements as in parser
sealed class StatementNode: ASTNode()

// Animation Command Specific type for easy detection by code generator
sealed class AnimationNode: StatementNode()
data class SleepNode(val sleepTime: ExpressionNode): AnimationNode()

// Code Specific Nodes holding line number
sealed class CodeNode(open val lineNumber: Int) : StatementNode()
data class DeclarationNode(override val lineNumber: Int, val identifier: String, val expression: ExpressionNode): CodeNode(lineNumber)
data class AssignmentNode(override val lineNumber: Int, val identifier: String, val expression: ExpressionNode): CodeNode(lineNumber)

// Expressions
sealed class ExpressionNode(override val lineNumber: Int): CodeNode(lineNumber)
data class IdentifierNode(val identifier: String, override val lineNumber: Int): ExpressionNode(lineNumber)
data class NumberNode(val double: Double, override val lineNumber: Int): ExpressionNode(lineNumber)
data class MethodCallNode(override val lineNumber: Int, val instanceIdentifier: String, val methodIdentifier: String, val arguments: List<ExpressionNode>): ExpressionNode(lineNumber)
data class Constructor(override val lineNumber: Int, val type: Type, val arguments: List<ExpressionNode>): ExpressionNode(lineNumber)

// Types (to be used in symbol table also)
sealed class Type: ASTNode()
object IntType: Type()
object StackType: Type()

// This is used to collect arguments up into method call node
data class ArgumentNode(val arguments: List<ExpressionNode>) : ASTNode()
