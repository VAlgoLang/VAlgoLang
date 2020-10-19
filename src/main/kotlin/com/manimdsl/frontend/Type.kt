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

sealed class DataStructureType(
    open var internalType: Type,
    open val methods: HashMap<String, DataStructureMethod>
) : Type() {
    abstract fun containsMethod(method: String): Boolean
    abstract fun getMethodByName(method: String): DataStructureMethod

}

data class ParameterListNode(val parameters: List<ParameterNode>) : ASTNode()
data class ParameterNode(val identifier: String, val type: Type) : ASTNode()

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
    override var internalType: Type = NumberType,
    override val methods: HashMap<String, DataStructureMethod> = hashMapOf(
        "push" to PushMethod(
            argumentTypes = listOf(
                NumberType
            )
        ), "pop" to PopMethod(internalType)
    )
) : DataStructureType(internalType, methods) {

    data class PushMethod(override val returnType: Type = ErrorType, override var argumentTypes: List<Type>) :
        DataStructureMethod

    data class PopMethod(override val returnType: Type, override var argumentTypes: List<Type> = listOf()) :
        DataStructureMethod

    override fun containsMethod(method: String): Boolean {
        return methods.containsKey(method)
    }

    override fun getMethodByName(method: String): DataStructureMethod {
        return methods.getOrDefault(method, ErrorMethod)
    }

    override fun toString(): String {
        return "stack"
    }
}


object ErrorType : Type()
object VoidType: Type() {
    override fun toString(): String {
        return "void"
    }
}
