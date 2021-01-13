package com.valgolang.frontend.datastructures

import com.valgolang.frontend.ast.AssignLHS
import com.valgolang.frontend.ast.ExpressionNode
import com.valgolang.frontend.ast.InitialiserNode

/**
 * Constructor node
 *
 * Constructor used to instantiate data structures
 *
 * @property lineNumber
 * @property type
 * @property arguments
 * @property initialiser
 * @constructor Create empty Constructor node
 */
data class ConstructorNode(
    override val lineNumber: Int,
    val type: DataStructureType,
    val arguments: List<ExpressionNode>,
    val initialiser: InitialiserNode
) : ExpressionNode(lineNumber)

/**
 * Data structure initialiser node
 *
 * A node representing the intialiser value in a data structure constructor
 *
 * @property expressions
 * @constructor Create empty Data structure initialiser node
 */
data class DataStructureInitialiserNode(
    val expressions: List<ExpressionNode>
) : InitialiserNode()

/**
 * Method call node
 *
 * @property lineNumber
 * @property instanceIdentifier
 * @property dataStructureMethod
 * @property arguments
 * @property identifier
 * @constructor Create empty Method call node
 */
data class MethodCallNode(
    override val lineNumber: Int,
    val instanceIdentifier: String,
    val dataStructureMethod: DataStructureMethod,
    val arguments: List<ExpressionNode>,
    override val identifier: String = ""
) : ExpressionNode(lineNumber), AssignLHS
