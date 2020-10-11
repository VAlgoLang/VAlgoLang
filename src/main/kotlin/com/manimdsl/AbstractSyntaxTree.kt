package com.manimdsl

sealed class ASTNode
data class ProgramNode(val statements: List<StatementNode>): ASTNode()

// Animation Command Specific Nodes
sealed class AnimationNode: ASTNode()
data class SleepNode(val sleepTime: Float): AnimationNode()

// Algorithm Specific Nodes

sealed class StatementNode(open val lineNumber: Int) : ASTNode()
data class AssignmentNode(override val lineNumber: Int) : StatementNode(lineNumber)

