package com.valgolang.frontend.datastructures.stack

import com.valgolang.frontend.*
import com.valgolang.frontend.datastructures.ConstructorMethod
import com.valgolang.frontend.datastructures.DataStructureMethod
import com.valgolang.frontend.datastructures.DataStructureType
import com.valgolang.frontend.datastructures.ErrorMethod

/**
 * Stack type
 *
 * Type representing a Stack
 *
 * @property internalType
 * @constructor Create empty Stack type
 */
data class StackType(
    override var internalType: Type,
) : DataStructureType(internalType) {
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

    object StackConstructor : ConstructorMethod {
        override val minRequiredArgsWithoutInitialValue: Int = 0
        override val returnType: Type = VoidType
        override val argumentTypes: List<Pair<Type, Boolean>> = emptyList()
        override val varargs: Boolean = false

        override fun toString(): String = "constructor"
    }

    /**
     * Push method
     *
     * @property returnType
     * @property argumentTypes
     * @property varargs
     * @constructor Create empty Push method
     */
    data class PushMethod(
        override val returnType: Type = ErrorType,
        override var argumentTypes: List<Pair<Type, Boolean>> = emptyList(),
        override val varargs: Boolean = false
    ) : DataStructureMethod

    /**
     * Pop method
     *
     * @property returnType
     * @property argumentTypes
     * @property varargs
     * @constructor Create empty Pop method
     */
    data class PopMethod(
        override val returnType: Type,
        override var argumentTypes: List<Pair<Type, Boolean>> = emptyList(),
        override val varargs: Boolean = false
    ) : DataStructureMethod

    /**
     * Is empty method
     *
     * @property returnType
     * @property argumentTypes
     * @property varargs
     * @constructor Create empty Is empty method
     */
    data class IsEmptyMethod(
        override val returnType: Type = BoolType,
        override var argumentTypes: List<Pair<Type, Boolean>> = emptyList(),
        override val varargs: Boolean = false
    ) : DataStructureMethod

    /**
     * Size method
     *
     * @property returnType
     * @property argumentTypes
     * @property varargs
     * @constructor Create empty Size method
     */
    data class SizeMethod(
        override val returnType: Type = NumberType,
        override var argumentTypes: List<Pair<Type, Boolean>> = emptyList(),
        override val varargs: Boolean = false
    ) : DataStructureMethod

    /**
     * Peek method
     *
     * @property returnType
     * @property argumentTypes
     * @property varargs
     * @constructor Create empty Peek method
     */
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