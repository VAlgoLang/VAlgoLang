package com.manimdsl.runtime.datastructures.array

import com.manimdsl.frontend.*
import com.manimdsl.linearrepresentation.EmptyMObject
import com.manimdsl.linearrepresentation.ManimInstr
import com.manimdsl.linearrepresentation.VariableNameGenerator
import com.manimdsl.linearrepresentation.datastructures.array.*
import com.manimdsl.runtime.*
import com.manimdsl.runtime.datastructures.BoundaryShape
import com.manimdsl.runtime.datastructures.DataStructureExecutor
import com.manimdsl.runtime.datastructures.SquareBoundary
import com.manimdsl.runtime.datastructures.WideBoundary
import com.manimdsl.runtime.utility.getBoundaries
import com.manimdsl.stylesheet.Stylesheet

/**
 * Array Executor
 *
 * @property variables: Map from identifier of a variable in frame to its current execution value.
 * @property linearRepresentation: Reference to linear representation list being constructed.
 * @property frame: Frame executor is in.
 * @property stylesheet: Stylesheet object provided by user for styling.
 * @property animationSpeeds: Animation speeds deque maintaining speeds at different points of execution.
 * @property dataStructureBoundaries: Map from data structure identifier it's shape boundary.
 * @property variableNameGenerator: Top level VariableNameGenerator.
 * @property codeTextVariable: Python identifier of code block.
 * @constructor Creates a new Array Executor with runtime operations defined inside.
 *
 */

class ArrayExecutor(
    override val variables: MutableMap<String, ExecValue>,
    override val linearRepresentation: MutableList<ManimInstr>,
    override val frame: VirtualMachine.Frame,
    override val stylesheet: Stylesheet,
    override val animationSpeeds: java.util.ArrayDeque<Double>,
    override val dataStructureBoundaries: MutableMap<String, BoundaryShape>,
    override val variableNameGenerator: VariableNameGenerator,
    override val codeTextVariable: String
) : DataStructureExecutor {

    override fun executeConstructor(node: ConstructorNode, dsUID: String, assignLHS: AssignLHS): ExecValue {
        val is2DArray = node.arguments.size == 2
        return if (is2DArray) {
            execute2DArrayConstructor(node, assignLHS)
        } else {
            execute1DArrayConstructor(node, assignLHS)
        }
    }

    private fun execute1DArrayConstructor(node: ConstructorNode, assignLHS: AssignLHS): ExecValue {
        val initialiserExpressions =
            with(node.initialiser) {
                if (this is DataStructureInitialiserNode) {
                    expressions
                } else {
                    emptyList()
                }
            }
        val arraySize =
            if (node.arguments.isNotEmpty()) frame.executeExpression(node.arguments[0]) as DoubleValue else DoubleValue(
                initialiserExpressions.size.toDouble()
            )
        val arrayValue = if (initialiserExpressions.isEmpty()) {
            ArrayValue(
                EmptyMObject,
                Array(arraySize.value.toInt()) { _ ->
                    getDefaultValueForType(
                        node.type.internalType,
                        node.lineNumber
                    )
                }
            )
        } else {
            if (initialiserExpressions.size != arraySize.value.toInt()) {
                RuntimeError("Initialisation of array failed.", lineNumber = node.lineNumber)
            } else {
                ArrayValue(EmptyMObject, initialiserExpressions.map { frame.executeExpression(it) }.toTypedArray())
            }
        }
        if (assignLHS !is ArrayElemNode) {
            val ident = variableNameGenerator.generateNameFromPrefix("array")
            if (arrayValue is ArrayValue) {
                arrayValue.style = stylesheet.getStyle(assignLHS.identifier, arrayValue)
                arrayValue.animatedStyle = stylesheet.getAnimatedStyle(assignLHS.identifier, arrayValue)
                val dsUID = frame.functionNamePrefix + assignLHS.identifier
                val position = stylesheet.getPosition(dsUID)
                dataStructureBoundaries[dsUID] = WideBoundary(maxSize = arraySize.value.toInt())
                if (stylesheet.userDefinedPositions() && position == null) {
                    return RuntimeError("Missing position values for $dsUID", lineNumber = node.lineNumber)
                }
                val boundaries = getBoundaries(position)
                val arrayStructure = ArrayStructure(
                    node.type,
                    ident,
                    stylesheet.renderDataStructure(frame.functionNamePrefix + assignLHS.identifier),
                    assignLHS.identifier,
                    arrayValue.array.clone(),
                    color = arrayValue.style.borderColor,
                    textColor = arrayValue.style.textColor,
                    creationString = arrayValue.style.creationStyle,
                    runtime = arrayValue.style.creationTime ?: animationSpeeds.first(),
                    showLabel = arrayValue.style.showLabel,
                    boundaries = boundaries,
                    uid = dsUID
                )
                linearRepresentation.add(arrayStructure)
                arrayValue.manimObject = arrayStructure
            }
        }
        return arrayValue
    }

    private fun execute2DArrayConstructor(node: ConstructorNode, assignLHS: AssignLHS): ExecValue {
        val arraySizes = node.arguments.map { frame.executeExpression(it) as DoubleValue }
        val nestedInitialiserExpressions =
            with(node.initialiser) {
                if (this is Array2DInitialiserNode) {
                    nestedExpressions
                } else {
                    emptyList()
                }
            }
        val arrayDimensions = Pair(arraySizes[0].value.toInt(), arraySizes[1].value.toInt())
        val arrayValue = if (nestedInitialiserExpressions.isEmpty()) {
            Array2DValue(
                EmptyMObject,
                Array(arrayDimensions.first) { _ ->
                    Array(arrayDimensions.second) { _ ->
                        getDefaultValueForType(
                            node.type.internalType,
                            node.lineNumber
                        )
                    }
                }
            )
        } else {
            if (nestedInitialiserExpressions.size != arrayDimensions.first || nestedInitialiserExpressions[0].size != arrayDimensions.second) {
                RuntimeError(
                    "Array initialiser dimensions do not match those in constructor",
                    lineNumber = node.lineNumber
                )
            } else {
                Array2DValue(
                    EmptyMObject,
                    nestedInitialiserExpressions.map { exprList ->
                        exprList.map { frame.executeExpression(it) }.toTypedArray()
                    }.toTypedArray()
                )
            }
        }

        if (arrayValue is Array2DValue) {
            val ident = variableNameGenerator.generateNameFromPrefix("array")
            val dsUID = frame.functionNamePrefix + assignLHS.identifier
            val position = stylesheet.getPosition(dsUID)
            dataStructureBoundaries[dsUID] = SquareBoundary(maxSize = arraySizes.sumBy { it.value.toInt() })
            arrayValue.style = stylesheet.getStyle(assignLHS.identifier, arrayValue)
            arrayValue.animatedStyle = stylesheet.getAnimatedStyle(assignLHS.identifier, arrayValue)
            if (stylesheet.userDefinedPositions() && position == null) {
                return RuntimeError("Missing position values for $dsUID", lineNumber = node.lineNumber)
            }
            val boundaries = getBoundaries(position)
            val arrayStructure = Array2DStructure(
                node.type,
                ident,
                stylesheet.renderDataStructure(frame.functionNamePrefix + assignLHS.identifier),
                assignLHS.identifier,
                arrayValue.array.map { it.clone() }.toTypedArray(),
                color = arrayValue.style.borderColor,
                textColor = arrayValue.style.textColor,
                creationString = arrayValue.style.creationStyle,
                runtime = arrayValue.style.creationTime ?: animationSpeeds.first(),
                showLabel = arrayValue.style.showLabel,
                boundaries = boundaries,
                uid = dsUID
            )
            linearRepresentation.add(arrayStructure)
            arrayValue.manimObject = arrayStructure
        }

        return arrayValue
    }

    private fun getDefaultValueForType(type: Type, lineNumber: Int): ExecValue {
        return when (type) {
            NumberType -> DoubleValue(0.0)
            BoolType -> BoolValue(false)
            is ArrayType -> getDefaultValueForType(type.internalType, lineNumber)
            else -> RuntimeError(value = "Cannot create data structure with type $type", lineNumber = lineNumber)
        }
    }

    fun executeArrayElemAssignment(arrayElemNode: ArrayElemNode, assignedValue: ExecValue): ExecValue {
        val indices = arrayElemNode.indices.map { frame.executeExpression(it) as DoubleValue }
        return when (val arrayValue = variables[arrayElemNode.identifier]) {
            is Array2DValue -> {
                val index = indices.first().value.toInt()

                return if (indices.size == 1) {
                    if (index !in arrayValue.array.indices) {
                        RuntimeError(value = "Index out of bounds exception", lineNumber = arrayElemNode.lineNumber)
                    } else {
                        // Assigning row
                        val newArray = (assignedValue as ArrayValue).value
                        if (newArray.size != arrayValue.array[index].size) {
                            RuntimeError(value = "Dimensions do not match", lineNumber = arrayElemNode.lineNumber)
                        } else {
                            arrayValue.array[index] = newArray
                            linearRepresentation.add(
                                ArrayReplaceRow(
                                    (arrayValue.manimObject as Array2DStructure).ident,
                                    index,
                                    arrayValue.value[index],
                                    runtime = animationSpeeds.first(),
                                    render = stylesheet.renderDataStructure(frame.functionNamePrefix + arrayElemNode.identifier)
                                )
                            )
                            EmptyValue
                        }
                    }
                } else {
                    val index2 = indices[1].value.toInt()
                    if (indices.first().value.toInt() !in arrayValue.array.indices || index2 !in arrayValue.array[index].indices) {
                        RuntimeError(value = "Array index out of bounds", lineNumber = arrayElemNode.lineNumber)
                    } else {
                        arrayValue.array[index][index2] = assignedValue
                        arrayValue.animatedStyle?.let {
                            linearRepresentation.add(
                                ArrayElemRestyle(
                                    (arrayValue.manimObject as Array2DStructure).ident,
                                    listOf(index2),
                                    it,
                                    it.pointer,
                                    animationString = it.animationStyle,
                                    runtime = it.animationTime ?: animationSpeeds.first(),
                                    secondIndices = listOf(index),
                                    render = stylesheet.renderDataStructure(frame.functionNamePrefix + arrayElemNode.identifier)
                                )
                            )
                        }
                        linearRepresentation.add(
                            ArrayElemAssignObject(
                                (arrayValue.manimObject as Array2DStructure).ident,
                                index2,
                                assignedValue,
                                arrayValue.animatedStyle,
                                secondIndex = index,
                                runtime = animationSpeeds.first(),
                                render = stylesheet.renderDataStructure(frame.functionNamePrefix + arrayElemNode.identifier)
                            )
                        )
                        arrayValue.animatedStyle?.let {
                            linearRepresentation.add(
                                ArrayElemRestyle(
                                    (arrayValue.manimObject as Array2DStructure).ident,
                                    listOf(index2),
                                    arrayValue.style,
                                    secondIndices = listOf(index),
                                    runtime = animationSpeeds.first(),
                                    render = stylesheet.renderDataStructure(frame.functionNamePrefix + arrayElemNode.identifier)
                                )
                            )
                        }
                        EmptyValue
                    }
                }
            }
            is ArrayValue -> {
                val index = indices.first()
                if (indices.first().value.toInt() !in arrayValue.array.indices) {
                    RuntimeError(value = "Array index out of bounds", lineNumber = arrayElemNode.lineNumber)
                } else {
                    arrayValue.array[index.value.toInt()] = assignedValue
                    arrayValue.animatedStyle?.let {
                        linearRepresentation.add(
                            ArrayElemRestyle(
                                (arrayValue.manimObject as ArrayStructure).ident,
                                listOf(index.value.toInt()),
                                it,
                                it.pointer,
                                animationString = it.animationStyle,
                                runtime = it.animationTime ?: animationSpeeds.first(),
                                render = stylesheet.renderDataStructure(frame.functionNamePrefix + arrayElemNode.identifier)
                            )
                        )
                    }
                    linearRepresentation.add(
                        ArrayElemAssignObject(
                            (arrayValue.manimObject as ArrayStructure).ident,
                            index.value.toInt(),
                            assignedValue,
                            arrayValue.animatedStyle,
                            runtime = animationSpeeds.first(),
                            render = stylesheet.renderDataStructure(frame.functionNamePrefix + arrayElemNode.identifier)
                        )
                    )
                    arrayValue.animatedStyle?.let {
                        linearRepresentation.add(
                            ArrayElemRestyle(
                                (arrayValue.manimObject as ArrayStructure).ident,
                                listOf(index.value.toInt()),
                                arrayValue.style,
                                runtime = animationSpeeds.first(),
                                render = stylesheet.renderDataStructure(frame.functionNamePrefix + arrayElemNode.identifier)
                            )
                        )
                    }
                    EmptyValue
                }
            }
            else -> EmptyValue
        }
    }

    fun executeArrayElem(node: ArrayElemNode, identifier: AssignLHS): ExecValue {
        return when (val arrayValue = variables[node.identifier]) {
            is ArrayValue -> executeArrayElemSingle(node, arrayValue)
            is Array2DValue -> executeArrayElem2D(node, arrayValue, identifier)
            is StringValue -> {
                val index = (frame.executeExpression(node.indices.first()) as DoubleValue).value.toInt()
                if (index !in arrayValue.value.indices) {
                    RuntimeError(value = "Array index out of bounds", lineNumber = node.lineNumber)
                } else CharValue(arrayValue.value[index])
            }
            else -> EmptyValue
        }
    }

    private fun executeArrayElemSingle(node: ArrayElemNode, arrayValue: ArrayValue): ExecValue {
        val index = frame.executeExpression(node.indices.first()) as DoubleValue
        return if (index.value.toInt() !in arrayValue.array.indices) {
            RuntimeError(value = "Array index out of bounds", lineNumber = node.lineNumber)
        } else {
            with(arrayValue.animatedStyle) {
                if (frame.getShowMoveToLine() && this != null) {
                    linearRepresentation.add(
                        ArrayElemRestyle(
                            (arrayValue.manimObject as ArrayStructure).ident,
                            listOf(index.value.toInt()),
                            this,
                            this.pointer,
                            animationString = this.animationStyle,
                            runtime = animationSpeeds.first(),
                            render = stylesheet.renderDataStructure(frame.functionNamePrefix + node.identifier)
                        )
                    )
                    linearRepresentation.add(
                        ArrayElemRestyle(
                            (arrayValue.manimObject as ArrayStructure).ident,
                            listOf(index.value.toInt()),
                            arrayValue.style,
                            runtime = animationSpeeds.first(),
                            render = stylesheet.renderDataStructure(frame.functionNamePrefix + node.identifier)
                        )
                    )
                }
            }
            arrayValue.array[index.value.toInt()]
        }
    }

    private fun executeArrayElem2D(node: ArrayElemNode, arrayValue: Array2DValue, assignLHS: AssignLHS): ExecValue {
        val indices = node.indices.map { frame.executeExpression(it) as DoubleValue }
        return if (indices.size == 2) {
            if (indices.first().value.toInt() !in arrayValue.array.indices || indices[1].value.toInt() !in arrayValue.array[indices.first().value.toInt()].indices) {
                RuntimeError(value = "Array index out of bounds", lineNumber = node.lineNumber)
            } else {
                with(arrayValue.animatedStyle) {
                    if (frame.getShowMoveToLine() && this != null) {
                        linearRepresentation.add(
                            ArrayElemRestyle(
                                (arrayValue.manimObject as Array2DStructure).ident,
                                listOf(indices[1].value.toInt()),
                                this,
                                this.pointer,
                                animationString = this.animationStyle,
                                secondIndices = listOf(indices.first().value.toInt()),
                                runtime = animationSpeeds.first(),
                                render = stylesheet.renderDataStructure(frame.functionNamePrefix + node.identifier)
                            )
                        )
                        linearRepresentation.add(
                            ArrayElemRestyle(
                                (arrayValue.manimObject as Array2DStructure).ident,
                                listOf(indices[1].value.toInt()),
                                arrayValue.style,
                                secondIndices = listOf(indices.first().value.toInt()),
                                runtime = animationSpeeds.first(),
                                render = stylesheet.renderDataStructure(frame.functionNamePrefix + node.identifier)
                            )
                        )
                    }
                }
                arrayValue.array[indices.first().value.toInt()][indices[1].value.toInt()]
            }
        } else {
            if (indices.first().value.toInt() !in arrayValue.array.indices) {
                RuntimeError(value = "Array index out of bounds", lineNumber = node.lineNumber)
            } else {
                val newArray = arrayValue.array[indices.first().value.toInt()].clone()
                val arrayValue2 = ArrayValue(
                    EmptyMObject,
                    newArray
                )
                val dsUID = frame.functionNamePrefix + assignLHS.identifier
                val ident = variableNameGenerator.generateNameFromPrefix("array")
                dataStructureBoundaries[dsUID] = WideBoundary(maxSize = newArray.size)
                arrayValue2.style = stylesheet.getStyle(node.identifier, arrayValue)
                arrayValue2.animatedStyle = stylesheet.getAnimatedStyle(node.identifier, arrayValue)
                val position = stylesheet.getPosition(dsUID)
                if (stylesheet.userDefinedPositions() && position == null) {
                    return RuntimeError("Missing position values for $dsUID", lineNumber = node.lineNumber)
                }
                val boundaries = getBoundaries(position)
                val arrayStructure = ArrayStructure(
                    ArrayType(node.internalType),
                    ident,
                    stylesheet.renderDataStructure(frame.functionNamePrefix + node.identifier),
                    assignLHS.identifier,
                    arrayValue2.array.clone(),
                    color = arrayValue2.style.borderColor,
                    textColor = arrayValue2.style.textColor,
                    creationString = arrayValue2.style.creationStyle,
                    runtime = arrayValue2.style.creationTime ?: animationSpeeds.first(),
                    showLabel = arrayValue2.style.showLabel,
                    boundaries = boundaries,
                    uid = dsUID
                )
                linearRepresentation.add(arrayStructure)
                arrayValue2.manimObject = arrayStructure
                arrayValue2
            }
        }
    }

    fun executeArrayMethodCall(node: MethodCallNode, ds: ArrayValue): ExecValue {
        return when (node.dataStructureMethod) {
            is ArrayType.Size -> {
                DoubleValue(ds.array.size.toDouble())
            }
            is ArrayType.Contains -> {
                BoolValue(ds.array.contains(frame.executeExpression(node.arguments[0])))
            }
            is ArrayType.Swap -> {
                val index1 = (frame.executeExpression(node.arguments[0]) as DoubleValue).value.toInt()
                val index2 = (frame.executeExpression(node.arguments[1]) as DoubleValue).value.toInt()
                val longSwap =
                    if (node.arguments.size != 3) false else (frame.executeExpression(node.arguments[2]) as BoolValue).value
                val arrayIdent = (ds.manimObject as ArrayStructure).ident
                val arraySwap =
                    if (longSwap) {
                        ArrayLongSwap(
                            arrayIdent,
                            Pair(index1, index2),
                            variableNameGenerator.generateNameFromPrefix("elem1"),
                            variableNameGenerator.generateNameFromPrefix("elem2"),
                            variableNameGenerator.generateNameFromPrefix("animations"),
                            runtime = ds.animatedStyle?.animationTime ?: animationSpeeds.first(),
                            render = stylesheet.renderDataStructure(frame.functionNamePrefix + node.identifier)
                        )
                    } else {
                        ArrayShortSwap(
                            arrayIdent,
                            Pair(index1, index2),
                            runtime = ds.animatedStyle?.animationTime ?: animationSpeeds.first(),
                            render = stylesheet.renderDataStructure(frame.functionNamePrefix + node.identifier)
                        )
                    }
                val swap = mutableListOf(arraySwap)
                with(ds.animatedStyle) {
                    if (this != null) {
                        swap.add(
                            0,
                            ArrayElemRestyle(
                                arrayIdent,
                                listOf(index1, index2),
                                this,
                                this.pointer,
                                runtime = animationSpeeds.first(),
                                render = stylesheet.renderDataStructure(frame.functionNamePrefix + node.identifier)
                            )
                        )
                        swap.add(
                            ArrayElemRestyle(
                                arrayIdent,
                                listOf(index1, index2),
                                ds.style,
                                runtime = animationSpeeds.first(),
                                render = stylesheet.renderDataStructure(frame.functionNamePrefix + node.identifier)
                            )
                        )
                    }
                }
                linearRepresentation.addAll(swap)
                val temp = ds.array[index1]
                ds.array[index1] = ds.array[index2]
                ds.array[index2] = temp
                EmptyValue
            }
            else -> EmptyValue
        }
    }

    fun executeInternalArrayMethodCall(node: InternalArrayMethodCallNode): ExecValue {
        val ds = variables[node.instanceIdentifier] as Array2DValue
        val index = (frame.executeExpression(node.index) as DoubleValue).value.toInt()
        return when (node.dataStructureMethod) {
            is ArrayType.Size -> DoubleValue(ds.array[index].size.toDouble())
            is ArrayType.Swap -> {
                val fromToIndices = node.arguments.map { (frame.executeExpression(it) as DoubleValue).value.toInt() }
                array2dSwap(ds, listOf(index, fromToIndices[0], index, fromToIndices[1]), node.instanceIdentifier)
            }
            else -> EmptyValue
        }
    }

    fun execute2DArrayMethodCall(node: MethodCallNode, ds: Array2DValue): ExecValue {
        return when (node.dataStructureMethod) {
            is ArrayType.Size -> {
                DoubleValue(ds.array.size.toDouble())
            }
            is ArrayType.Swap -> {
                val indices = node.arguments.map { (frame.executeExpression(it) as DoubleValue).value.toInt() }
                array2dSwap(ds, indices, node.instanceIdentifier)
            }
            else -> EmptyValue
        }
    }

    private fun array2dSwap(ds: Array2DValue, indices: List<Int>, identifier: String): EmptyValue {
        val arrayIdent = (ds.manimObject as Array2DStructure).ident
        val arraySwap =
            Array2DSwap(
                arrayIdent,
                indices,
                runtime = ds.animatedStyle?.animationTime ?: animationSpeeds.first(),
                render = stylesheet.renderDataStructure(frame.functionNamePrefix + identifier)
            )
        val swap = mutableListOf<ManimInstr>(arraySwap)
        with(ds.animatedStyle) {
            if (this != null) {
                swap.add(
                    0,
                    ArrayElemRestyle(
                        arrayIdent,
                        listOf(indices[1], indices[3]),
                        this,
                        this.pointer,
                        secondIndices = listOf(indices[0], indices[2]),
                        runtime = animationSpeeds.first(),
                        render = stylesheet.renderDataStructure(frame.functionNamePrefix + identifier)
                    )
                )
                swap.add(
                    ArrayElemRestyle(
                        arrayIdent,
                        listOf(indices[1], indices[3]),
                        ds.style,
                        secondIndices = listOf(indices[0], indices[2]),
                        runtime = animationSpeeds.first(),
                        render = stylesheet.renderDataStructure(frame.functionNamePrefix + identifier)
                    )
                )
            }
        }
        linearRepresentation.addAll(swap)
        val temp = ds.array[indices[0]][indices[1]]
        ds.array[indices[0]][indices[1]] = ds.array[indices[2]][indices[3]]
        ds.array[indices[2]][indices[3]] = temp
        return EmptyValue
    }
}
