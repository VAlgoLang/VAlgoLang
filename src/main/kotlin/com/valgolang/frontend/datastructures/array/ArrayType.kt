package com.valgolang.frontend.datastructures.array

import com.valgolang.frontend.BoolType
import com.valgolang.frontend.NumberType
import com.valgolang.frontend.Type
import com.valgolang.frontend.VoidType
import com.valgolang.frontend.datastructures.ConstructorMethod
import com.valgolang.frontend.datastructures.DataStructureMethod
import com.valgolang.frontend.datastructures.DataStructureType
import com.valgolang.frontend.datastructures.ErrorMethod

open class ArrayType(
    override var internalType: Type,
    var is2D: Boolean = false,
) : DataStructureType(internalType) {
    override val methods: MutableMap<String, DataStructureMethod> = mutableMapOf(
        "size" to Size(), "swap" to Swap(), "contains" to Contains(argumentTypes = listOf(internalType to true))
    )

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

    data class Contains(
        override val returnType: Type = BoolType,
        override val argumentTypes: List<Pair<Type, Boolean>> = listOf(),
        override val varargs: Boolean = false
    ) : DataStructureMethod

    data class Swap(
        override val returnType: Type = VoidType,
        override var argumentTypes: List<Pair<Type, Boolean>> = listOf(
            NumberType to true,
            NumberType to true,
            BoolType to false
        ),
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
        methods["swap"] =
            Swap(argumentTypes = listOf(NumberType to true, NumberType to true, NumberType to true, NumberType to true))
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ArrayType

        if (internalType != other.internalType) return false
        if (is2D != other.is2D) return false

        return true
    }

    override fun hashCode(): Int {
        var result = internalType.hashCode()
        result = 31 * result + is2D.hashCode()
        return result
    }
}
