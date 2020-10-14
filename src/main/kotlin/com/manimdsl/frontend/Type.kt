package com.manimdsl.frontend


sealed class Type: ASTNode()

sealed class PrimitiveType: Type()
object NumberType: PrimitiveType() {
    override fun toString(): String {
        return "number"
    }
}

sealed class DataStructureType(open var internalType: Type, open val methods: HashMap<String, DataStructureMethod>): Type() {
    abstract fun containsMethod(method: String): Boolean
    abstract fun getMethodByName(method: String): DataStructureMethod
}

// Pre-defined methods for each data structure
open class DataStructureMethod(open val returnType: Type, open var argumentTypes: List<Type>)
data class ErrorMethod(override val returnType: Type = NoType, override var argumentTypes: List<Type> = listOf()) : DataStructureMethod(returnType, argumentTypes)

data class StackType(override var internalType: Type = NumberType,
                     override val methods: HashMap<String, DataStructureMethod> = hashMapOf("push" to PushMethod(argumentTypes=listOf(NumberType)), "pop" to PopMethod(internalType))): DataStructureType(internalType, methods) {

    data class PushMethod(override val returnType: Type = NoType, override var argumentTypes: List<Type>): DataStructureMethod(returnType, argumentTypes)
    data class PopMethod(override val returnType: Type, override var argumentTypes: List<Type> = listOf()): DataStructureMethod(returnType, argumentTypes)

    override fun containsMethod(method: String): Boolean {
        return methods.containsKey(method)
    }

    override fun getMethodByName(method: String): DataStructureMethod {
        return methods[method]!!
    }

    override fun toString(): String {
        return "Stack<${internalType}>"
    }
}

object NoType: Type()
