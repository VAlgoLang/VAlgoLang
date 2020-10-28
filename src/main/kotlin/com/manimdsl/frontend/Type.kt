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
    abstract fun getConstructor(): ConstructorMethod
}


interface DataStructureMethod {
    val returnType: Type
    val argumentTypes: List<Type>

    // When true last type in argumentTypes will be used to as type of varargs
    val varargs: Boolean
}

interface ConstructorMethod : DataStructureMethod {
    val minRequiredArgsWithoutInitialValue: Int
}

object ErrorMethod : DataStructureMethod {
    override val returnType: Type = ErrorType
    override val argumentTypes: List<Type> = emptyList()
    override val varargs: Boolean = false
}

// This is used to collect arguments up into method call node
data class ArgumentNode(val arguments: List<ExpressionNode>) : ASTNode()


data class ArrayType(
    override var internalType: Type,
    override val methods: Map<String, DataStructureMethod> = hashMapOf(
        "length" to Length()
    )
) : DataStructureType(internalType, methods) {
    object ArrayConstructor : ConstructorMethod {
        override val minRequiredArgsWithoutInitialValue: Int = 1
        override val returnType: Type = VoidType
        override val argumentTypes: List<Type> = listOf(NumberType)
        override val varargs: Boolean = true

        override fun toString(): String = "constructor"
    }

    data class Length(
        override val returnType: Type = NumberType,
        override var argumentTypes: List<Type> = emptyList(),
        override val varargs: Boolean = false
    ) : DataStructureMethod

    override fun containsMethod(method: String): Boolean {
        return methods.containsKey(method)
    }

    override fun getMethodByName(method: String): DataStructureMethod {
        return methods.getOrDefault(method, ErrorMethod)
    }

    override fun getConstructor(): ConstructorMethod {
        return ArrayConstructor
    }

    override fun toString(): String = "Array<$internalType>"
}

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
    object StackConstructor : ConstructorMethod {
        override val minRequiredArgsWithoutInitialValue: Int = 0
        override val returnType: Type = VoidType
        override val argumentTypes: List<Type> = emptyList()
        override val varargs: Boolean = false

        override fun toString(): String = "constructor"
    }

    data class PushMethod(
        override val returnType: Type = ErrorType,
        override var argumentTypes: List<Type>,
        override val varargs: Boolean = false
    ) : DataStructureMethod

    data class PopMethod(
        override val returnType: Type,
        override var argumentTypes: List<Type> = listOf(),
        override val varargs: Boolean = false
    ) : DataStructureMethod

    data class IsEmptyMethod(
        override val returnType: Type = BoolType,
        override var argumentTypes: List<Type> = listOf(),
        override val varargs: Boolean = false
    ) : DataStructureMethod

    data class SizeMethod(
        override val returnType: Type = NumberType,
        override var argumentTypes: List<Type> = listOf(),
        override val varargs: Boolean = false
    ) : DataStructureMethod

    data class PeekMethod(
        override val returnType: Type,
        override var argumentTypes: List<Type> = listOf(),
        override val varargs: Boolean = false
    ) : DataStructureMethod

    override fun containsMethod(method: String): Boolean {
        return methods.containsKey(method)
    }

    override fun getMethodByName(method: String): DataStructureMethod {
        return methods.getOrDefault(method, ErrorMethod)
    }

    override fun getConstructor(): ConstructorMethod {
        return StackConstructor
    }

    override fun toString(): String = "Stack<$internalType>"
}


object ErrorType : Type()
object VoidType : Type() {
    override fun toString(): String {
        return "void"
    }
}
