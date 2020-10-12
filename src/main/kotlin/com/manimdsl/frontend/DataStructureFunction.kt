package com.manimdsl.frontend

enum class Stack(val allowedNumOfArgs:Int) {
    POP(1),
    PUSH(2)
}

val stackFunctions = Stack.values()

sealed class DataStructureFunction()
