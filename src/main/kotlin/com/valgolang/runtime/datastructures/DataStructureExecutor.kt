package com.valgolang.runtime.datastructures

import com.valgolang.frontend.*
import com.valgolang.linearrepresentation.DataStructureMObject
import com.valgolang.linearrepresentation.ManimInstr
import com.valgolang.linearrepresentation.VariableNameGenerator
import com.valgolang.linearrepresentation.datastructures.binarytree.InitTreeStructure
import com.valgolang.runtime.ExecValue
import com.valgolang.runtime.VirtualMachine
import com.valgolang.runtime.datastructures.array.Array2DValue
import com.valgolang.runtime.datastructures.array.ArrayValue
import com.valgolang.runtime.datastructures.binarytree.BinaryTreeValue
import com.valgolang.runtime.datastructures.stack.StackValue
import com.valgolang.runtime.utility.makeExpressionNode
import com.valgolang.stylesheet.Stylesheet

/**
 * Abstract Data Structure Executor. Maintains the runtime operations of a data structure.
 *
 * @property variables: Map from identifier of a variable in frame to its current execution value.
 * @property linearRepresentation: Reference to linear representation list being constructed.
 * @property frame: Frame executor is in.
 * @property stylesheet: Stylesheet object provided by user for styling.
 * @property animationSpeeds: Animation speeds deque maintaining speeds at different points of execution.
 * @property dataStructureBoundaries: Map from data structure identifier it's shape boundary.
 * @property variableNameGenerator: Top level VariableNameGenerator.
 * @property codeTextVariable: Python identifier of code block.
 * @constructor Creates a new DataStructureExecutor with runtime operations defined inside for the binary tree.
 *
 */

interface DataStructureExecutor {
    val variables: MutableMap<String, ExecValue>
    val linearRepresentation: MutableList<ManimInstr>
    val frame: VirtualMachine.Frame
    val stylesheet: Stylesheet
    val animationSpeeds: java.util.ArrayDeque<Double>
    val dataStructureBoundaries: MutableMap<String, BoundaryShape>
    val variableNameGenerator: VariableNameGenerator
    val codeTextVariable: String
    val locallyCreatedDynamicVariables: MutableSet<String>

    fun executeConstructor(node: ConstructorNode, dsUID: String, assignLHS: AssignLHS): ExecValue
}

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
