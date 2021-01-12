package com.valgolang.frontend.datastructures.list

import com.valgolang.frontend.ast.NumberType
import com.valgolang.frontend.ast.Type
import com.valgolang.frontend.ast.VoidType
import com.valgolang.frontend.datastructures.ConstructorMethod
import com.valgolang.frontend.datastructures.DataStructureMethod
import com.valgolang.frontend.datastructures.array.ArrayType

/**
 * List type
 *
 * Represents a list type. Currently implemented as an array-based list.
 *
 * @property internalType
 * @constructor Create empty List type
 */
data class ListType(override var internalType: Type) : ArrayType(internalType) {
    override val methods: MutableMap<String, DataStructureMethod> = mutableMapOf(
        "size" to Size(), "prepend" to Prepend(argumentTypes = listOf(internalType to true)), "append" to Append(argumentTypes = listOf(internalType to true))
    )

    object ListConstructor : ConstructorMethod {
        override val minRequiredArgsWithoutInitialValue: Int = 0
        override val returnType: Type = VoidType
        override val argumentTypes: List<Pair<Type, Boolean>> = listOf()
        override val varargs: Boolean = true

        override fun toString(): String = "constructor"
    }

    /**
     * Prepend
     *
     * @property returnType
     * @property argumentTypes
     * @property varargs
     * @constructor Create empty Prepend
     */
    data class Prepend(
        override val returnType: Type = VoidType,
        override var argumentTypes: List<Pair<Type, Boolean>> = listOf(),
        override val varargs: Boolean = false
    ) : DataStructureMethod

    /**
     * Append
     *
     * @property returnType
     * @property argumentTypes
     * @property varargs
     * @constructor Create empty Append
     */
    data class Append(
        override val returnType: Type = VoidType,
        override var argumentTypes: List<Pair<Type, Boolean>> = listOf(
            NumberType to true,
        ),
        override val varargs: Boolean = false
    ) : DataStructureMethod

    override fun getConstructor(): ConstructorMethod {
        return ListConstructor
    }

    override fun toString(): String = "List<$internalType>"
}
