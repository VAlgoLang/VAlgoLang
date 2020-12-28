package com.manimdsl.runtime.datastructures

import com.manimdsl.frontend.*
import com.manimdsl.linearrepresentation.DataStructureMObject
import com.manimdsl.linearrepresentation.InitTreeStructure
import com.manimdsl.runtime.ExecValue
import com.manimdsl.runtime.datastructures.array.Array2DValue
import com.manimdsl.runtime.datastructures.array.ArrayValue
import com.manimdsl.runtime.datastructures.binarytree.BinaryTreeValue
import com.manimdsl.runtime.datastructures.stack.StackValue
import com.manimdsl.runtime.utility.makeExpressionNode

fun makeConstructorNode(assignedValue: ExecValue, lineNumber: Int): ConstructorNode {
    return when (assignedValue) {
        is ArrayValue -> {
            val dim = NumberNode(lineNumber, assignedValue.array.size.toDouble())
            val initialiser = DataStructureInitialiserNode(
                assignedValue.array.map { makeExpressionNode(it, lineNumber) }
            )
            val type = (assignedValue.manimObject as DataStructureMObject).type
            ConstructorNode(lineNumber, type, listOf(dim), initialiser)
        }
        is Array2DValue -> {
            val dimY = NumberNode(lineNumber, assignedValue.array.size.toDouble())
            val dimX = NumberNode(lineNumber, assignedValue.array[0].size.toDouble())
            val initialiser = Array2DInitialiserNode(
                assignedValue.array.map {
                    it.map { v -> makeExpressionNode(v, lineNumber) }
                }
            )
            val type = (assignedValue.manimObject as DataStructureMObject).type
            ConstructorNode(lineNumber, type, listOf(dimY, dimX), initialiser)
        }
        is StackValue -> {
            val type = (assignedValue.manimObject as DataStructureMObject).type
            val initialiser = DataStructureInitialiserNode(
                assignedValue.stack.map { makeExpressionNode(it, lineNumber) }
            )
            ConstructorNode(lineNumber, type, listOf(), initialiser)
        }
        is BinaryTreeValue -> {
            with(assignedValue.manimObject as InitTreeStructure) {
                val internalType = this.type.internalType
                val root = ConstructorNode(lineNumber, NodeType(internalType), listOf(makeExpressionNode(assignedValue.value.value, lineNumber)), EmptyInitialiserNode)
                ConstructorNode(lineNumber, TreeType(internalType), listOf(root), EmptyInitialiserNode)
            }
        }
        /** Extend here with other data structure values **/
        else -> ConstructorNode(lineNumber, ArrayType(NullType), listOf(), EmptyInitialiserNode)
    }
}
