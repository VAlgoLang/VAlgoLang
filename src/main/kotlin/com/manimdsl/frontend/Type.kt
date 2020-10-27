package com.manimdsl.frontend

// Types (to be used in symbol table also)
sealed class Type : ASTNode()

// Primitive / Data structure distinction requested by code generation
sealed class PrimitiveType : Type()

object NumberType : PrimitiveType() {
    override fun toString(): String {
        return "number"
    }
}

object BoolType : PrimitiveType() {
    override fun toString(): String {
        return "boolean"
    }
}

sealed class DataStructureType(
    open var internalType: Type,
    open val methods: Map<String, DataStructureMethod>
) : Type() {
    abstract fun containsMethod(method: String): Boolean
    abstract fun getMethodByName(method: String): DataStructureMethod
}

data class ArrayType(
    override var internalType: Type,
    override val methods: Map<String, DataStructureMethod> = emptyMap()
) : DataStructureType(internalType, methods) {
    override fun containsMethod(method: String): Boolean {
        return false;
    }

    override fun getMethodByName(method: String): DataStructureMethod {
        return ErrorMethod
    }
}


interface DataStructureMethod {
    val returnType: Type
    val argumentTypes: List<Type>
}

object ErrorMethod : DataStructureMethod {
    override val returnType: Type = ErrorType
    override val argumentTypes: List<Type> = emptyList()
}

// This is used to collect arguments up into method call node
data class ArgumentNode(val arguments: List<ExpressionNode>) : ASTNode()
data class StackType(
    override var internalType: Type,
    override val methods: Map<String, DataStructureMethod> = hashMapOf(
        "push" to PushMethod(
            argumentTypes = listOf(
                internalType,
            )
        ),
        "pop" to PopMethod(internalType),
        "isEmpty" to IsEmptyMethod(),
        "size" to SizeMethod(),
        "peek" to PeekMethod(internalType)
    )
) : DataStructureType(internalType, methods) {

    data class PushMethod(override val returnType: Type = ErrorType, override var argumentTypes: List<Type>) :
        DataStructureMethod

    data class PopMethod(override val returnType: Type, override var argumentTypes: List<Type> = listOf()) :
        DataStructureMethod

    data class IsEmptyMethod(
        override val returnType: Type = BoolType,
        override var argumentTypes: List<Type> = listOf()
    ) : DataStructureMethod

    data class SizeMethod(
        override val returnType: Type = NumberType,
        override var argumentTypes: List<Type> = listOf()
    ) : DataStructureMethod

    data class PeekMethod(override val returnType: Type, override var argumentTypes: List<Type> = listOf()) :
        DataStructureMethod

    override fun containsMethod(method: String): Boolean {
        return methods.containsKey(method)
    }

    override fun getMethodByName(method: String): DataStructureMethod {
        return methods.getOrDefault(method, ErrorMethod)
    }

    override fun toString(): String {
        return "Stack<$internalType>"
    }
}


object ErrorType : Type()
object VoidType : Type() {
    override fun toString(): String {
        return "void"
    }
}
