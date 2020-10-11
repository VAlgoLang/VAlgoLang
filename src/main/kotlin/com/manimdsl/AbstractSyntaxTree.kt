package com.manimdsl

sealed class ASTNode
data class ProgramNode(val statements: List<StatementNode>): ASTNode()

// All statements as in parser
sealed class StatementNode: ASTNode()

// Animation Command Specific type for easy detection by code generator
sealed class AnimationNode: StatementNode()
data class SleepNode(val sleepTime: ExpressionNode): AnimationNode()

// Algorithm Specific Nodes holding line number
sealed class AlgorithmNode(open val lineNumber: Int) : StatementNode()
data class DeclarationNode(override val lineNumber: Int, val identifier: String, val expression: ExpressionNode) : AlgorithmNode(lineNumber)

// Expressions
sealed class ExpressionNode: ASTNode()

