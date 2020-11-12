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

object CharType : PrimitiveType() {
    override fun toString(): String {
        return "char"
    }
}

interface NullableDataStructure

sealed class DataStructureType(
    open var internalType: Type,
    open val methods: MutableMap<String, DataStructureMethod>
) : Type() {
    abstract fun containsMethod(method: String): Boolean
    abstract fun getMethodByName(method: String): DataStructureMethod
    abstract fun getConstructor(): ConstructorMethod

    fun getMethodNameByMethod(method: DataStructureMethod): String {
        return methods.toList().find { it.second == method }?.first ?: method.toString()
    }
}


interface DataStructureMethod {
    val returnType: Type
    // List of pairs containing type to whether it is a required argument
    val argumentTypes:  List<Pair<Type, Boolean>>

    // When true last type in argumentTypes will be used to as type of varargs
    val varargs: Boolean


}

interface ConstructorMethod : DataStructureMethod {
    val minRequiredArgsWithoutInitialValue: Int
}

object ErrorMethod : DataStructureMethod {
    override val returnType: Type = ErrorType
    override val argumentTypes: List<Pair<Type, Boolean>> = emptyList()
    override val varargs: Boolean = false
}

// This is used to collect arguments up into method call node
data class ArgumentNode(val arguments: List<ExpressionNode>) : ASTNode()

data class ArrayType(
    override var internalType: Type,
    var is2D: Boolean = false,
    override val methods: MutableMap<String, DataStructureMethod> = mutableMapOf(
        "size" to Size(), "swap" to Swap()
    )
) : DataStructureType(internalType, methods) {
    object ArrayConstructor : ConstructorMethod {
        override val minRequiredArgsWithoutInitialValue: Int = 1
        override val returnType: Type = VoidType
        override val argumentTypes: List<Pair<Type, Boolean>> = listOf(NumberType to false)
        override val varargs: Boolean = true

        override fun toString(): String = "constructor"
    }

    data class Size(
        override val returnType: Type = NumberType,
        override val argumentTypes: List<Pair<Type, Boolean>> = listOf(),
        override val varargs: Boolean = false
    ) : DataStructureMethod

    data class Swap(
        override val returnType: Type = VoidType,
        override var argumentTypes: List<Pair<Type, Boolean>> = listOf(NumberType to true, NumberType to true, BoolType to false),
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


    fun setTo2D() {
        is2D = true
        methods["swap"] = Swap(argumentTypes = listOf(NumberType to true, NumberType to true, NumberType to true, NumberType to true))
    }
}

data class BinaryTreeType(
    override var internalType: Type,
    override val methods: MutableMap<String, DataStructureMethod> = hashMapOf(
        "left" to Left(internalType), "right" to Right(internalType), "value" to Value(internalType)
    ),
) : DataStructureType(internalType, methods), NullableDataStructure {
    class BinaryTreeConstructor(internalType: Type) : ConstructorMethod {
        override val minRequiredArgsWithoutInitialValue: Int = 1
        override val returnType: Type = VoidType
        override val argumentTypes: List<Pair<Type, Boolean>> = listOf(internalType to true)
        override val varargs: Boolean = false

        override fun toString(): String = "constructor"
    }

    class Left(
        override val returnType: Type,
        override var argumentTypes: List<Pair<Type, Boolean>> = listOf(),
        override val varargs: Boolean = false
    ) : DataStructureMethod {
        override fun toString(): String {
            return "left"
        }
    }

    class Right(
        override val returnType: Type,
        override var argumentTypes: List<Pair<Type, Boolean>> = listOf(),
        override val varargs: Boolean = false
    ) : DataStructureMethod {
        override fun toString(): String {
            return "right"
        }
    }

    class Value(
        override val returnType: Type,
        override var argumentTypes: List<Pair<Type, Boolean>> = listOf(),
        override val varargs: Boolean = false
    ) : DataStructureMethod {
        override fun toString(): String {
            return "value"
        }
    }


    override fun containsMethod(method: String): Boolean {
        return methods.containsKey(method)
    }

    override fun getMethodByName(method: String): DataStructureMethod {
        return methods.getOrDefault(method, ErrorMethod)
    }

    override fun getConstructor(): ConstructorMethod {
        return BinaryTreeConstructor(internalType)
    }

    override fun toString(): String = "Node<$internalType>"
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BinaryTreeType

        if (internalType != other.internalType) return false

        return true
    }

    override fun hashCode(): Int {
        return internalType.hashCode()
    }
}

data class StackType(
    override var internalType: Type,
    override val methods: MutableMap<String, DataStructureMethod> = hashMapOf(
        "push" to PushMethod(
            argumentTypes = listOf(
                internalType to true,
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
        override val argumentTypes: List<Pair<Type, Boolean>> = emptyList()
        override val varargs: Boolean = false

        override fun toString(): String = "constructor"
    }

    data class PushMethod(
        override val returnType: Type = ErrorType,
        override var argumentTypes: List<Pair<Type, Boolean>> = emptyList(),
        override val varargs: Boolean = false
    ) : DataStructureMethod

    data class PopMethod(
        override val returnType: Type,
        override var argumentTypes: List<Pair<Type, Boolean>> = emptyList(),
        override val varargs: Boolean = false
    ) : DataStructureMethod

    data class IsEmptyMethod(
        override val returnType: Type = BoolType,
        override var argumentTypes: List<Pair<Type, Boolean>> = emptyList(),
        override val varargs: Boolean = false
    ) : DataStructureMethod

    data class SizeMethod(
        override val returnType: Type = NumberType,
        override var argumentTypes: List<Pair<Type, Boolean>> = emptyList(),
        override val varargs: Boolean = false
    ) : DataStructureMethod

    data class PeekMethod(
        override val returnType: Type,
        override var argumentTypes: List<Pair<Type, Boolean>> = emptyList(),
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

object NullType: Type() {
    override fun toString(): String {
        return "null"
    }
}

object ErrorType : Type() {
    override fun toString(): String {
        return "error"
    }
}
object VoidType : Type() {
    override fun toString(): String {
        return "void"
    }
}
