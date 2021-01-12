package com.valgolang.frontend.datastructures

import com.valgolang.frontend.ast.ErrorType
import com.valgolang.frontend.ast.Type

/**
 * Nullable data structure
 *
 * @constructor Create empty Nullable data structure
 */
interface NullableDataStructure

/**
 * Data structure type
 *
 * Should be extended when adding new data structures to the language
 *
 * @property internalType
 * @constructor Create empty Data structure type
 */
abstract class DataStructureType(
    open var internalType: Type,
) : Type() {
    abstract val methods: MutableMap<String, DataStructureMethod>

    /**
     * Contains method
     *
     * Checks that method name is valid for DataStructureType
     *
     * @param method
     * @return
     */
    abstract fun containsMethod(method: String): Boolean

    /**
     * Get method by name
     *
     * Returns method by name
     *
     * @param method
     * @return
     */
    abstract fun getMethodByName(method: String): DataStructureMethod

    /**
     * Get constructor
     *
     * @return
     */
    abstract fun getConstructor(): ConstructorMethod

    /**
     * Get method name by method
     *
     * Finds method name given DataStructureMethod
     *
     * @param method
     * @return
     */
    fun getMethodNameByMethod(method: DataStructureMethod): String {
        return methods.toList().find { it.second == method }?.first ?: method.toString()
    }
}

/**
 * Data structure method
 *
 * Represents member methods a data structure implements
 *
 * @constructor Create empty Data structure method
 */
interface DataStructureMethod {
    val returnType: Type

    // List of pairs containing type to whether it is a required argument
    val argumentTypes: List<Pair<Type, Boolean>>

    // When true last type in argumentTypes will be used to as type of varargs
    val varargs: Boolean
}

/**
 * Constructor method
 *
 * Represents the constructor of a data structure
 *
 * @constructor Create empty Constructor method
 */
interface ConstructorMethod : DataStructureMethod {
    val minRequiredArgsWithoutInitialValue: Int
}

/**
 * Error method
 *
 * Used in semantic checks to return when method does not exist on data structure.
 *
 * @constructor Create empty Error method
 */
object ErrorMethod : DataStructureMethod {
    override val returnType: Type = ErrorType
    override val argumentTypes: List<Pair<Type, Boolean>> = emptyList()
    override val varargs: Boolean = false
}
