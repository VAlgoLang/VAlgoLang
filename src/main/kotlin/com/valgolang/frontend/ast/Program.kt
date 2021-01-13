package com.valgolang.frontend.ast

/**
 * AST node
 *
 * Collectively represents the abstract syntax tree of the program.
 *
 * @constructor Create empty A s t node
 */
open class ASTNode

/**
 * Program node
 *
 * Root level AST node that represents the program in its entirety
 *
 * @property functions
 * @property statements
 * @constructor Create empty Program node
 */
data class ProgramNode(
    val functions: List<FunctionNode>,
    val statements: List<StatementNode>
) : ASTNode()

/**
 * Function node
 *
 * Represents a function declaration
 *
 * @property lineNumber
 * @property scope
 * @property identifier
 * @property parameters
 * @property statements
 * @constructor Create empty Function node
 */
data class FunctionNode(
    override val lineNumber: Int,
    val scope: Int,
    val identifier: String,
    val parameters: List<ParameterNode>,
    val statements: List<StatementNode>
) : CodeNode(lineNumber)

/**
 * Parameter list node
 *
 * List of parameters for functions
 *
 * @property parameters
 * @constructor Create empty Parameter list node
 */
data class ParameterListNode(val parameters: List<ParameterNode>) : ASTNode()

/**
 * Parameter node
 *
 * An identifier and type representing a parameter of a function
 *
 * @property identifier
 * @property type
 * @constructor Create empty Parameter node
 */
data class ParameterNode(val identifier: String, val type: Type) : ASTNode()

/**
 * Argument node
 *
 * Arguments passed into method/function calls
 *
 * @property arguments
 * @constructor Create empty Argument node
 */ // This is used to collect arguments up into method call node
data class ArgumentNode(val arguments: List<ExpressionNode>) : ASTNode()

/**
 * Code node
 *
 * A node that represents a line of code in the input file
 *
 * @property lineNumber
 * @constructor Create empty Code node
 */ // Code Specific Nodes holding line number
abstract class CodeNode(override val lineNumber: Int) : StatementNode(lineNumber)
