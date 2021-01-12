package com.valgolang.frontend.ast

open class ASTNode

data class ProgramNode(
    val functions: List<FunctionNode>,
    val statements: List<StatementNode>
) : ASTNode()

data class FunctionNode(
    override val lineNumber: Int,
    val scope: Int,
    val identifier: String,
    val parameters: List<ParameterNode>,
    val statements: List<StatementNode>
) : CodeNode(lineNumber)

data class ParameterListNode(val parameters: List<ParameterNode>) : ASTNode()

data class ParameterNode(val identifier: String, val type: Type) : ASTNode()

// This is used to collect arguments up into method call node
data class ArgumentNode(val arguments: List<ExpressionNode>) : ASTNode()

// Code Specific Nodes holding line number
abstract class CodeNode(override val lineNumber: Int) : StatementNode(lineNumber)
