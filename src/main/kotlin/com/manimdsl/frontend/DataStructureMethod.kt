package com.manimdsl.frontend

enum class DataStructure {
    STACK {
        override val methods: List<DataStructureMethod>
            get() = mutableListOf(StackPop(), StackPush())

        override fun containsMethod(method: String): Boolean {
            return methods.map { it.toString() }.contains(method)
        }

        override fun hasValidNumberOfArguments(method: String, numArgs: Int): Boolean {
            return containsMethod(method) && methods.filter { it.toString() == method }[0].numberOfArguments == numArgs
        }

    };

    abstract val methods: List<DataStructureMethod>

    abstract fun containsMethod(method: String): Boolean
    abstract fun hasValidNumberOfArguments(method: String, numArgs: Int): Boolean
}

sealed class DataStructureMethod(open val numberOfArguments: Int)
data class StackPop(override val numberOfArguments: Int = 0) : DataStructureMethod(numberOfArguments) {
    override fun toString(): String {
        return "pop"
    }
}

data class StackPush(override val numberOfArguments: Int = 1) : DataStructureMethod(numberOfArguments) {
    override fun toString(): String {
        return "push"
    }
}

