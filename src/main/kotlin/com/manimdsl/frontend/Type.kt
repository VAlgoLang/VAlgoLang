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
    fun animateMethod(
        arguments: List<String> = emptyList(),
        variableNameGenerator: NameGenerator = DummyNameGenerator,
        animationFlags: AnimationFlags = AnimationFlags(),
        vararg mObjects: MObject = emptyArray(),
    ): Pair<List<ManimInstr>, MObject?>

    val returnType: Type
    val argumentTypes: List<Type>
}

object ErrorMethod : DataStructureMethod {
    override fun animateMethod(
        arguments: List<String>,
        variableNameGenerator: NameGenerator,
        animationFlags: AnimationFlags,
        vararg mObjects: MObject
    ): Pair<List<ManimInstr>, MObject?> {
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
        override fun animateMethod(
            arguments: List<String>,
            variableNameGenerator: NameGenerator,
            animationFlags: AnimationFlags,
            vararg mObjects: MObject
        ): Pair<List<ManimInstr>, MObject?> {
            val rectangleShape = Rectangle(arguments[0])
            val topOfStack = mObjects[0]
            val oldShape = mObjects[1]
            val rectangle = if (animationFlags[OldShape]) oldShape else NewMObject(
                rectangleShape,
                variableNameGenerator.generateShapeName(rectangleShape)
            )

            val instructions =
                mutableListOf<ManimInstr>(MoveObject(rectangle.ident, topOfStack.ident, ObjectSide.ABOVE))
            if (!animationFlags[OldShape]) {
                instructions.add(0, rectangle)
            }
            return Pair(
                instructions, rectangle
            )
        }

    }

    data class PopMethod(override val returnType: Type, override var argumentTypes: List<Type> = listOf()) :
        DataStructureMethod {
        override fun animateMethod(
            arguments: List<String>,
            variableNameGenerator: NameGenerator,
            animationFlags: AnimationFlags,
            vararg mObjects: MObject
        ): Pair<List<ManimInstr>, MObject?> {
            val topOfStack = mObjects[0]
            val secondTopOfStack = mObjects[1]
            return Pair(
                listOf(
                    MoveObject(
                        topOfStack.ident,
                        secondTopOfStack.ident,
                        ObjectSide.ABOVE,
                        20,
                        animationFlags[FadeOut]
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


object NoType : Type()
