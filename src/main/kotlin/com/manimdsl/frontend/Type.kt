package com.manimdsl.frontend

import com.manimdsl.linearrepresentation.*
import com.manimdsl.shapes.Rectangle

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
        text: String,
    ): Pair<List<ManimInstr>, MObject>

    abstract fun initRelativeToObject(
        identifier: String,
        text: String,
        moveRelativeTo: String
    ): Pair<List<ManimInstr>, MObject>
}

interface DataStructureMethod {
    fun animateMethod(arguments: List<String>, options: Map<String, Any> = emptyMap()): Pair<List<ManimInstr>, MObject?>
    val returnType: Type
    val argumentTypes: List<Type>
}

object ErrorMethod : DataStructureMethod {
    override fun animateMethod(arguments: List<String>, options: Map<String, Any>): Pair<List<ManimInstr>, MObject?> {
        return Pair(emptyList(), null)
    }

    override val returnType: Type = NoType
    override val argumentTypes: List<Type> = emptyList()
}

// This is used to collect arguments up into method call node
data class ArgumentNode(val arguments: List<ExpressionNode>) : ASTNode()
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
        DataStructureMethod {
        /** arguments = {value} **/
        override fun animateMethod(
            arguments: List<String>,
            options: Map<String, Any>
        ): Pair<List<ManimInstr>, MObject?> {
            val rectangleShape = Rectangle(arguments[0])
            val rectangle = NewMObject(
                rectangleShape,
                (options["generator"] as VariableNameGenerator).generateShapeName(rectangleShape)
            )
            return Pair(
                listOf(
                    rectangle,
                    MoveObject(rectangle.ident, (options["top"] as MObject).ident, ObjectSide.ABOVE),
                ), rectangle
            )
        }
    }

    data class PopMethod(override val returnType: Type, override var argumentTypes: List<Type> = listOf()) :
        DataStructureMethod {
        override fun animateMethod(
            arguments: List<String>,
            options: Map<String, Any>
        ): Pair<List<ManimInstr>, MObject?> {
            return Pair(
                listOf(
                    MoveObject(
                        (options["top"] as MObject).ident,
                        (options["second"] as MObject).ident,
                        ObjectSide.ABOVE,
                        20,
                        options["fadeOut"] as? Boolean? ?: true
                    ),
                ), null
            )
        }
    }

    override fun containsMethod(method: String): Boolean {
        return methods.containsKey(method)
    }

    override fun getMethodByName(method: String): DataStructureMethod {
        return methods.getOrDefault(method, ErrorMethod)
    }

    override fun init(identifier: String, x: Int, y: Int, text: String): Pair<List<ManimInstr>, MObject> {
        val stackInit = InitStructure(x, y, Alignment.HORIZONTAL, identifier, text)

        // Add to stack of objects to keep track of identifier
        return Pair(listOf(stackInit), stackInit)
    }

    override fun initRelativeToObject(
        identifier: String,
        text: String,
        moveRelativeTo: String
    ): Pair<List<ManimInstr>, MObject> {
        val stackInit = InitStructureRelative(Alignment.HORIZONTAL, identifier, text, moveRelativeTo)
        return Pair(listOf(stackInit), stackInit)
    }
}


object NoType: Type()
