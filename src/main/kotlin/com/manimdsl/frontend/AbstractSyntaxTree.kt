package com.manimdsl.frontend

open class ASTNode
data class ProgramNode(val statements: List<StatementNode>): ASTNode()

// All statements making up program
sealed class StatementNode: ASTNode()

// Animation Command Specific type for easy detection
sealed class AnimationNode: StatementNode()
data class SleepNode(val sleepTime: ExpressionNode): AnimationNode()
// Comments (not discarded so they can be rendered for educational purposes)
data class CommentNode(val content: String): AnimationNode()

// Code Specific Nodes holding line number - todo: replace with composition
sealed class CodeNode(open val lineNumber: Int) : StatementNode()

interface DeclarationOrAssignment{
    val lineNumber: Int;
    val identifier: String
    val expression: ExpressionNode
}

data class DeclarationNode(override val lineNumber: Int, override val identifier: String, override val expression: ExpressionNode): CodeNode(lineNumber), DeclarationOrAssignment
data class AssignmentNode(override val lineNumber: Int, override val identifier: String, override val expression: ExpressionNode): CodeNode(lineNumber), DeclarationOrAssignment

// Expressions
sealed class ExpressionNode(override val lineNumber: Int): CodeNode(lineNumber)
data class IdentifierNode(override val lineNumber: Int, val identifier: String): ExpressionNode(lineNumber)
data class NumberNode(override val lineNumber: Int, val double: Double): ExpressionNode(lineNumber)
data class MethodCallNode(override val lineNumber: Int, val instanceIdentifier: String, val dataStructureMethod: DataStructureMethod, val arguments: List<ExpressionNode>): ExpressionNode(lineNumber)
data class ConstructorNode(override val lineNumber: Int, val type: DataStructureType, val arguments: List<ExpressionNode>): ExpressionNode(lineNumber)

// Method calls
sealed class MethodClassNode()

// Binary Expressions
sealed class BinaryExpression(override val lineNumber: Int, open val expr1: ExpressionNode, open val expr2: ExpressionNode): ExpressionNode(lineNumber)
data class AddExpression(override val lineNumber: Int, override val expr1: ExpressionNode, override val expr2: ExpressionNode): BinaryExpression(lineNumber, expr1, expr2)
data class SubtractExpression(override val lineNumber: Int, override val expr1: ExpressionNode, override val expr2: ExpressionNode): BinaryExpression(lineNumber, expr1, expr2)
data class MultiplyExpression(override val lineNumber: Int, override val expr1: ExpressionNode, override val expr2: ExpressionNode): BinaryExpression(lineNumber, expr1, expr2)

// Unary Expressions
sealed class UnaryExpression(override val lineNumber: Int, open val expr: ExpressionNode) : ExpressionNode(lineNumber)
data class PlusExpression(override val lineNumber: Int, override val expr: ExpressionNode) :
    UnaryExpression(lineNumber, expr)

data class MinusExpression(override val lineNumber: Int, override val expr: ExpressionNode) :
    UnaryExpression(lineNumber, expr)


// Types (to be used in symbol table also)
sealed class Type : ASTNode()

// Primitive / Data structure distinction requested by code generation
sealed class PrimitiveType : Type()
object NumberType : PrimitiveType()

sealed class DataStructureType(
    open var internalType: Type,
    open val methods: HashMap<String, DataStructureMethod>
) : Type() {
    abstract fun containsMethod(method: String): Boolean
    abstract fun getMethodByName(method: String): DataStructureMethod

    /** Init methods are to return instructions needed to create MObject **/
    abstract fun init(
        identifier: String,
        x: Int,
        y: Int,
        text: String = "empty"
    ): Pair<List<ManimInstr>, Object>
}

abstract class DataStructureMethod(open val returnType: Type, open var argumentTypes: List<Type>) {
    abstract fun animateMethod(
        arguments: List<String>,
        options: Map<String, Any> = emptyMap()
    ): Pair<List<ManimInstr>, Object?>
}

data class StackType(
    override var internalType: Type = NumberType,
    override val methods: HashMap<String, DataStructureMethod> = hashMapOf(
        "push" to PushMethod(
            argumentTypes = listOf(
                NumberType
            )
        ), "pop" to PopMethod(internalType)
    )
) : DataStructureType(internalType, methods) {

    data class PushMethod(override val returnType: Type = NoType, override var argumentTypes: List<Type>) :
        DataStructureMethod(returnType, argumentTypes) {
        /** arguments = {value} **/
        override fun animateMethod(
            arguments: List<String>,
            options: Map<String, Any>
        ): Pair<List<ManimInstr>, Object?> {
            val rectangleShape = Rectangle(arguments[0])
            val rectangle = NewObject(rectangleShape, (options["generator"] as VariableNameGenerator).generateShapeName(rectangleShape))
            return Pair(
                listOf(
                    rectangle,
                    MoveObject(rectangle.ident, (options["top"] as Object).ident, ObjectSide.ABOVE),
                ), rectangle
            )
        }

    }

    data class PopMethod(override val returnType: Type, override var argumentTypes: List<Type> = listOf()) :
        DataStructureMethod(returnType, argumentTypes) {
        override fun animateMethod(
            arguments: List<String>,
            options: Map<String, Any>
        ): Pair<List<ManimInstr>, Object?> {
            return Pair(
                listOf(
                    MoveObject(
                        (options["top"] as Object).ident,
                        (options["second"] as Object).ident,
                        ObjectSide.ABOVE,
                        20,
                        options["fadeOut"] as? Boolean? ?: false
                    ),
                ), null
            )
        }
    }

    override fun containsMethod(method: String): Boolean {
        return methods.containsKey(method)
    }

    override fun getMethodByName(method: String): DataStructureMethod {
        return methods[method]!!
    }

    override fun init(identifier: String, x: Int, y: Int, text: String): Pair<List<ManimInstr>, Object> {
        val stackInit = InitStructure(x, y, Alignment.HORIZONTAL, identifier, text)

        // Add to stack of objects to keep track of identifier
        return Pair(listOf(stackInit), stackInit)
    }
}

object NoType: Type()
// This is used to collect arguments up into method call node
data class ArgumentNode(val arguments: List<ExpressionNode>) : ASTNode()
