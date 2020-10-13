package com.manimdsl.frontend

class DataStructureHandler {
    enum class DataStructure {
        STACK,
        NONE
    }

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
            else -> ErrorMethod
        }
    }



}

sealed class DataStructureMethod(open val numberOfArguments: Int, open val dataStructure: DataStructureHandler.DataStructure) {
    open fun hasValidNumberOfArguments(numArgs: Int): Boolean {
        return numberOfArguments == numArgs
    }

}


object StackPop : DataStructureMethod(numberOfArguments = 0, dataStructure = DataStructureHandler.DataStructure.STACK) {
    override fun toString(): String {
        return "pop"
    }
}

object StackPush : DataStructureMethod(numberOfArguments = 1, dataStructure = DataStructureHandler.DataStructure.STACK) {
    override fun toString(): String {
        return "push"
    }
}

object ErrorMethod : DataStructureMethod(numberOfArguments = -1, dataStructure = DataStructureHandler.DataStructure.NONE) {
    override fun hasValidNumberOfArguments(numArgs: Int): Boolean = false
}



