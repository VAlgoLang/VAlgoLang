package com.manimdsl.frontend

class DataStructureHandler {

    private fun convertTypeToDataStructure(type: DataStructureType): DataStructure {
        return when (type) {
            is StackType -> DataStructure.STACK
        }
    }

    fun convertStringToMethod(methodName: String, dataStructureType: DataStructureType): DataStructureMethod {
        return when (convertTypeToDataStructure(dataStructureType)) {
            DataStructure.STACK -> when (methodName) {
                "pop" -> StackPop
                "push" -> StackPush
                else -> ErrorMethod
            }
        }
    }

}

enum class DataStructure {
    STACK
}


sealed class DataStructureMethod(open val numberOfArguments: Int, open val dataStructure: DataStructure) {
    open fun hasValidNumberOfArguments(numArgs: Int): Boolean {
        return numberOfArguments == numArgs
    }
}

interface SetMethod
interface GetMethod

object StackPop : DataStructureMethod(numberOfArguments = 0, dataStructure = DataStructure.STACK), GetMethod {
    override fun toString(): String {
        return "pop"
    }
}

object StackPush : DataStructureMethod(numberOfArguments = 1, dataStructure = DataStructure.STACK), SetMethod {
    override fun toString(): String {
        return "push"
    }
}

object ErrorMethod : DataStructureMethod(numberOfArguments = -1, dataStructure = DataStructure.STACK) {
    override fun hasValidNumberOfArguments(numArgs: Int): Boolean = false
}



