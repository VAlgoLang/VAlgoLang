package com.valgolang.frontend.ast

// Animation Command Specific type for easy detection
sealed class AnimationNode(override val lineNumber: Int) : StatementNode(lineNumber)

sealed class NoRenderAnimationNode(override val lineNumber: Int) : AnimationNode(lineNumber)

data class SleepNode(override val lineNumber: Int, val sleepTime: ExpressionNode) : NoRenderAnimationNode(lineNumber)

// Comments (not discarded so they can be rendered for educational purposes)
data class CommentNode(override val lineNumber: Int, val content: String) : AnimationNode(lineNumber)

// Step over to step over code and avoid additional animations
data class CodeTrackingNode(override val lineNumber: Int, val endLineNumber: Int, val statements: List<StatementNode>) :
    NoRenderAnimationNode(lineNumber)

sealed class AnnotationBlockNode(override val lineNumber: Int, open val condition: ExpressionNode) :
    NoRenderAnimationNode(lineNumber)

// Step into code and avoid additional animations
data class StartCodeTrackingNode(
    override val lineNumber: Int,
    val isStepInto: Boolean,
    override val condition: ExpressionNode
) :
    AnnotationBlockNode(lineNumber, condition)

data class StopCodeTrackingNode(
    override val lineNumber: Int,
    val isStepInto: Boolean,
    override val condition: ExpressionNode
) :
    AnnotationBlockNode(lineNumber, condition)

// Step into code and make speed changes
data class StartSpeedChangeNode(
    override val lineNumber: Int,
    val speedChange: ExpressionNode,
    override val condition: ExpressionNode
) :
    AnnotationBlockNode(lineNumber, condition)

data class StopSpeedChangeNode(override val lineNumber: Int, override val condition: ExpressionNode) :
    AnnotationBlockNode(lineNumber, condition)

data class SubtitleAnnotationNode(
    override val lineNumber: Int,
    val text: ExpressionNode,
    val duration: ExpressionNode? = null,
    override var condition: ExpressionNode,
    val showOnce: Boolean,
) : AnnotationBlockNode(lineNumber, condition)
