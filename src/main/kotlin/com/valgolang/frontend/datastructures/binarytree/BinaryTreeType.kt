package com.valgolang.frontend.datastructures.binarytree

import com.valgolang.frontend.ast.NullType
import com.valgolang.frontend.ast.Type
import com.valgolang.frontend.ast.VoidType
import com.valgolang.frontend.datastructures.*

/**
 * Binary tree type
 *
 * Represents the type for a binary tree (wrapper type for a node)
 *
 * @property internalType
 * @constructor Create empty Binary tree type
 */
data class BinaryTreeType(
    override var internalType: Type,
) : DataStructureType(internalType) {
    override val methods: MutableMap<String, DataStructureMethod> = hashMapOf(
        "root" to Root(internalType as BinaryTreeNodeType)
    )

    override fun containsMethod(method: String): Boolean {
        return methods.containsKey(method)
    }

    override fun getMethodByName(method: String): DataStructureMethod {
        return methods.getOrDefault(method, ErrorMethod)
    }

    override fun getConstructor(): ConstructorMethod {
        return BinaryTreeConstructor(internalType as BinaryTreeNodeType)
    }

    override fun equals(other: Any?): Boolean {
        return other is BinaryTreeType && other.internalType == internalType
    }

    override fun toString(): String = "Tree<$internalType>"

    override fun hashCode(): Int {
        var result = internalType.hashCode()
        result = 31 * result + methods.hashCode()
        return result
    }

    /**
     * Root
     *
     * @property returnType
     * @property argumentTypes
     * @property varargs
     * @constructor Create empty Root
     */
    class Root(
        override val returnType: Type,
        override var argumentTypes: List<Pair<Type, Boolean>> = listOf(),
        override val varargs: Boolean = false
    ) : DataStructureMethod {
        override fun toString(): String {
            return "root"
        }
    }

    /**
     * Binary tree constructor
     *
     * @constructor
     *
     * @param nodeType
     */
    class BinaryTreeConstructor(nodeType: BinaryTreeNodeType) : ConstructorMethod {
        override val minRequiredArgsWithoutInitialValue: Int = 1
        override val returnType: Type = VoidType
        override val argumentTypes: List<Pair<Type, Boolean>> = listOf(nodeType to true)
        override val varargs: Boolean = false

        override fun toString(): String = "constructor"
    }
}

/**
 * Binary tree node type
 *
 * Represents nodes that make up a binary tree
 *
 * @property internalType
 * @constructor Create empty Binary tree node type
 */
data class BinaryTreeNodeType(
    override var internalType: Type,
) : DataStructureType(internalType), NullableDataStructure {
    override val methods: MutableMap<String, DataStructureMethod> = hashMapOf(
        "left" to Left(this), "right" to Right(this), "value" to Value(internalType)
    )

    /**
     * Node constructor
     *
     * @constructor
     *
     * @param internalType
     */
    class NodeConstructor(internalType: Type) : ConstructorMethod {
        override val minRequiredArgsWithoutInitialValue: Int = 1
        override val returnType: Type = VoidType
        override val argumentTypes: List<Pair<Type, Boolean>> = listOf(internalType to true)
        override val varargs: Boolean = false

        override fun toString(): String = "constructor"
    }

    /**
     * Left
     *
     * @property returnType
     * @property argumentTypes
     * @property varargs
     * @constructor Create empty Left
     */
    class Left(
        override val returnType: Type,
        override var argumentTypes: List<Pair<Type, Boolean>> = listOf(),
        override val varargs: Boolean = false
    ) : DataStructureMethod {
        override fun toString(): String {
            return "left"
        }
    }

    /**
     * Right
     *
     * @property returnType
     * @property argumentTypes
     * @property varargs
     * @constructor Create empty Right
     */
    class Right(
        override val returnType: Type,
        override var argumentTypes: List<Pair<Type, Boolean>> = listOf(),
        override val varargs: Boolean = false
    ) : DataStructureMethod {
        override fun toString(): String {
            return "right"
        }
    }

    /**
     * Value
     *
     * @property returnType
     * @property argumentTypes
     * @property varargs
     * @constructor Create empty Value
     */
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
        return NodeConstructor(internalType)
    }

    override fun toString(): String = "Node<$internalType>"
    override fun equals(other: Any?): Boolean {
        if (this === other || other is NullType) return true
        if (javaClass != other?.javaClass) return false

        other as BinaryTreeNodeType

        if (internalType != other.internalType) return false

        return true
    }

    override fun hashCode(): Int {
        return internalType.hashCode()
    }
}
