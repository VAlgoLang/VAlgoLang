package com.valgolang.frontend.datastructures

import com.valgolang.frontend.ErrorType
import com.valgolang.frontend.Type

interface NullableDataStructure

abstract class DataStructureType(
    open var internalType: Type,
) : Type() {
    abstract val methods: MutableMap<String, DataStructureMethod>
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
    val argumentTypes: List<Pair<Type, Boolean>>

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
