package com.valgolang.frontend.ast

/**
 * Animation node
 *
 * Represents a node that customises the animation
 *
 * @property lineNumber
 * @constructor Create empty Animation node
 */ // Animation Command Specific type for easy detection
sealed class AnimationNode(override val lineNumber: Int) : StatementNode(lineNumber)

/**
 * No render animation node
 *
 * Animation node that should not be rendered in the code block when animated.
 *
 * @property lineNumber
 * @constructor Create empty No render animation node
 */
sealed class NoRenderAnimationNode(override val lineNumber: Int) : AnimationNode(lineNumber)

/**
 * Sleep node
 *
 * Adds wait to animation
 *
 * @property lineNumber
 * @property sleepTime
 * @constructor Create empty Sleep node
 */
data class SleepNode(override val lineNumber: Int, val sleepTime: ExpressionNode) : NoRenderAnimationNode(lineNumber)

/**
 * Code tracking node
 *
 * Wraps statements held in a annotation block node. In particular stepOver, stepInto.
 *
 * @property lineNumber
 * @property endLineNumber
 * @property statements
 * @constructor Create empty Code tracking node
 */
data class CodeTrackingNode(override val lineNumber: Int, val endLineNumber: Int, val statements: List<StatementNode>) :
    NoRenderAnimationNode(lineNumber)

/**
 * Annotation block node
 *
 * Refers to nodes that wrap an annotation block
 *
 * @property lineNumber
 * @property condition
 * @constructor Create empty Annotation block node
 */
sealed class AnnotationBlockNode(override val lineNumber: Int, open val condition: ExpressionNode) :
    NoRenderAnimationNode(lineNumber)

/**
 * Start code tracking node
 *
 * Used to mark where in the program a stepInto/stepOver starts
 *
 * @property lineNumber
 * @property isStepInto
 * @property condition
 * @constructor Create empty Start code tracking node
 */ // Step into code and avoid additional animations
data class StartCodeTrackingNode(
    override val lineNumber: Int,
    val isStepInto: Boolean,
    override val condition: ExpressionNode
) :
    AnnotationBlockNode(lineNumber, condition)

/**
 * Stop code tracking node
 *
 * Marks end of stepInto/stepOver block.
 *
 * @property lineNumber
 * @property isStepInto
 * @property condition
 * @constructor Create empty Stop code tracking node
 */
data class StopCodeTrackingNode(
    override val lineNumber: Int,
    val isStepInto: Boolean,
    override val condition: ExpressionNode
) :
    AnnotationBlockNode(lineNumber, condition)

/**
 * Start speed change node
 *
 * Used to mark where in the program a speed block starts
 *
 * @property lineNumber
 * @property speedChange
 * @property condition
 * @constructor Create empty Start speed change node
 */ // Step into code and make speed changes
data class StartSpeedChangeNode(
    override val lineNumber: Int,
    val speedChange: ExpressionNode,
    override val condition: ExpressionNode
) :
    AnnotationBlockNode(lineNumber, condition)

/**
 * Stop speed change node
 *
 * Used to mark where in the program a speed block ends
 *
 * @property lineNumber
 * @property condition
 * @constructor Create empty Stop speed change node
 */
data class StopSpeedChangeNode(override val lineNumber: Int, override val condition: ExpressionNode) :
    AnnotationBlockNode(lineNumber, condition)

/**
 * Subtitle annotation node
 *
 * Adds subtitle to with text to final animation
 *
 * @property lineNumber
 * @property text
 * @property duration
 * @property condition
 * @property showOnce
 * @constructor Create empty Subtitle annotation node
 */
data class SubtitleAnnotationNode(
    override val lineNumber: Int,
    val text: ExpressionNode,
    val duration: ExpressionNode? = null,
    override var condition: ExpressionNode,
    val showOnce: Boolean,
) : AnnotationBlockNode(lineNumber, condition)
