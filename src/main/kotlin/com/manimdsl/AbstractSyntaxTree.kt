package com.manimdsl

sealed class ASTNode

// Animation Command Specific Nodes
sealed class AnimationNode
data class SleepNode(val sleepTime: Float)

// Algorithm Specific Nodes
sealed class ProgramNode(open val lineNumber: Int)
data class AssignmentNode(override val lineNumber: Int) : ProgramNode(lineNumber)

