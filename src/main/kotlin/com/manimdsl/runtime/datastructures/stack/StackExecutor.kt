package com.manimdsl.runtime.datastructures.stack

import com.manimdsl.frontend.*
import com.manimdsl.linearrepresentation.*
import com.manimdsl.linearrepresentation.datastructures.stack.InitManimStack
import com.manimdsl.linearrepresentation.datastructures.stack.StackPopObject
import com.manimdsl.linearrepresentation.datastructures.stack.StackPushObject
import com.manimdsl.runtime.*
import com.manimdsl.runtime.datastructures.BoundaryShape
import com.manimdsl.runtime.datastructures.DataStructureExecutor
import com.manimdsl.runtime.datastructures.TallBoundary
import com.manimdsl.runtime.utility.getBoundaries
import com.manimdsl.stylesheet.Stylesheet
import java.util.*

/**
 * Stack Executor
 *
 * @property variables: Map from identifier of a variable in frame to its current execution value.
 * @property linearRepresentation: Reference to linear representation list being constructed.
 * @property frame: Frame executor is in.
 * @property stylesheet: Stylesheet object provided by user for styling.
 * @property animationSpeeds: Animation speeds deque maintaining speeds at different points of execution.
 * @property dataStructureBoundaries: Map from data structure identifier it's shape boundary.
 * @property variableNameGenerator: Top level VariableNameGenerator.
 * @property codeTextVariable: Python identifier of code block.
 * @constructor Creates a new StackExecutor with runtime operations defined inside for the binary tree.
 *
 */

class StackExecutor(
    override val variables: MutableMap<String, ExecValue>,
    override val linearRepresentation: MutableList<ManimInstr>,
    override val frame: VirtualMachine.Frame,
    override val stylesheet: Stylesheet,
    override val animationSpeeds: ArrayDeque<Double>,
    override val dataStructureBoundaries: MutableMap<String, BoundaryShape>,
    override val variableNameGenerator: VariableNameGenerator,
    override val codeTextVariable: String,
    private val locallyCreatedDynamicVariables: MutableSet<String>,
) : DataStructureExecutor {

    override fun executeConstructor(node: ConstructorNode, dsUID: String, assignLHS: AssignLHS): ExecValue {
        val stackValue = StackValue(EmptyMObject, Stack())
        val initStructureIdent = variableNameGenerator.generateNameFromPrefix("stack")
        stackValue.style = stylesheet.getStyle(assignLHS.identifier, stackValue)
        stackValue.animatedStyle = stylesheet.getAnimatedStyle(assignLHS.identifier, stackValue)
        val position = stylesheet.getPosition(dsUID)
        locallyCreatedDynamicVariables.add(dsUID)
        dataStructureBoundaries[dsUID] = TallBoundary()
        if (stylesheet.userDefinedPositions() && position == null) {
            return RuntimeError("Missing position values for $dsUID", lineNumber = node.lineNumber)
        }
        val boundaries = getBoundaries(position)
        val numStack = variables.values.filterIsInstance(StackValue::class.java).lastOrNull()
        val (instructions, newObject) = if (numStack == null) {
            val stackInit = InitManimStack(
                node.type,
                initStructureIdent,
                Coord(2.0, -1.0),
                Alignment.HORIZONTAL,
                assignLHS.identifier,
                color = stackValue.style.borderColor,
                textColor = stackValue.style.textColor,
                creationStyle = stackValue.style.creationStyle,
                creationTime = stackValue.style.creationTime,
                showLabel = stackValue.style.showLabel,
                boundaries = boundaries,
                uid = dsUID,
                render = stylesheet.renderDataStructure(dsUID)
            )
            // Add to stack of objects to keep track of identifier
            Pair(listOf(stackInit), stackInit)
        } else {
            val stackInit = InitManimStack(
                node.type,
                initStructureIdent,
                RelativeToMoveIdent,
                Alignment.HORIZONTAL,
                assignLHS.identifier,
                color = stackValue.style.borderColor,
                textColor = stackValue.style.textColor,
                creationStyle = stackValue.style.creationStyle,
                creationTime = stackValue.style.creationTime,
                showLabel = stackValue.style.showLabel,
                boundaries = boundaries,
                uid = dsUID,
                render = stylesheet.renderDataStructure(dsUID)
            )
            Pair(listOf(stackInit), stackInit)
        }
        linearRepresentation.addAll(instructions)
        val newObjectStyle = stackValue.style
        val initialiserNodeExpressions =
            with(node.initialiser) {
                if (this is DataStructureInitialiserNode) {
                    expressions
                } else {
                    emptyList()
                }
            }
        initialiserNodeExpressions.map { frame.executeExpression(it) }.forEach {
            val rectangle = Rectangle(
                variableNameGenerator.generateNameFromPrefix("rectangle"),
                it.toString(),
                initStructureIdent,
                color = newObjectStyle.borderColor,
                textColor = newObjectStyle.textColor
            )
            val clonedValue = it.clone()
            clonedValue.manimObject = rectangle
            stackValue.stack.push(clonedValue)
            linearRepresentation.add(rectangle)
            linearRepresentation.add(
                StackPushObject(
                    rectangle.ident,
                    initStructureIdent,
                    runtime = newObjectStyle.animate?.animationTime ?: animationSpeeds.first(),
                    render = stylesheet.renderDataStructure(dsUID)
                )
            )
        }
        stackValue.manimObject = newObject
        return stackValue
    }

    fun executeStackMethodCall(
        node: MethodCallNode,
        ds: StackValue,
        insideMethodCall: Boolean,
        isExpression: Boolean
    ): ExecValue {
        return when (node.dataStructureMethod) {
            is StackType.PushMethod -> {
                val value = frame.executeExpression(node.arguments[0], true)
                if (value is RuntimeError) {
                    return value
                }
                val dataStructureIdentifier = (ds.manimObject as InitManimStack).ident
                val dsUID = (ds.manimObject as InitManimStack).uid
                val boundaryShape = dataStructureBoundaries[dsUID]!!
                boundaryShape.maxSize++
                dataStructureBoundaries[dsUID] = boundaryShape
                val hasOldMObject = value.manimObject !is EmptyMObject
                val newObjectStyle = ds.animatedStyle ?: ds.style
                val rectangle = if (hasOldMObject) value.manimObject as Rectangle else Rectangle(
                    variableNameGenerator.generateNameFromPrefix("rectangle"),
                    value.toString(),
                    dataStructureIdentifier,
                    color = newObjectStyle.borderColor,
                    textColor = newObjectStyle.textColor,
                )

                val instructions: MutableList<ManimInstr> =
                    mutableListOf(
                        StackPushObject(
                            rectangle.ident,
                            dataStructureIdentifier,
                            hasOldMObject,
                            creationStyle = ds.style.creationStyle,
                            runtime = ds.animatedStyle?.animationTime ?: animationSpeeds.first(),
                            render = stylesheet.renderDataStructure(dsUID)
                        ),
                        RestyleRectangle(
                            rectangle,
                            ds.style,
                            ds.animatedStyle?.animationTime ?: animationSpeeds.first(),
                            render = stylesheet.renderDataStructure(dsUID)
                        )
                    )
                if (!hasOldMObject) {
                    instructions.add(0, rectangle)
                }

                linearRepresentation.addAll(instructions)
                val clonedValue = value.clone()
                clonedValue.manimObject = rectangle
                ds.stack.push(clonedValue)
                EmptyValue
            }
            is StackType.PopMethod -> {
                if (ds.stack.empty()) {
                    return RuntimeError(
                        value = "Attempted to pop from empty stack ${node.instanceIdentifier}",
                        lineNumber = frame.getPc()
                    )
                }
                val poppedValue = ds.stack.pop()
                val dataStructureIdentifier = (ds.manimObject as InitManimStack).ident

                val topOfStack = poppedValue.manimObject as Rectangle
                val instructions = mutableListOf<ManimInstr>(
                    StackPopObject(
                        topOfStack.ident,
                        dataStructureIdentifier,
                        insideMethodCall,
                        runtime = ds.animatedStyle?.animationTime ?: animationSpeeds.first(),
                        render = stylesheet.renderDataStructure(frame.functionNamePrefix + node.instanceIdentifier)
                    )
                )
                ds.animatedStyle?.let {
                    instructions.add(
                        0,
                        RestyleRectangle(
                            topOfStack,
                            it,
                            it.animationTime ?: animationSpeeds.first(),
                            render = stylesheet.renderDataStructure(frame.functionNamePrefix + node.instanceIdentifier)
                        )
                    )
                }
                linearRepresentation.addAll(instructions)
                return if (isExpression) poppedValue else EmptyValue
            }
            is StackType.IsEmptyMethod -> {
                return BoolValue(ds.stack.isEmpty())
            }
            is StackType.SizeMethod -> {
                return DoubleValue(ds.stack.size.toDouble())
            }
            is StackType.PeekMethod -> {
                if (ds.stack.empty()) {
                    return RuntimeError(value = "Attempted to peek empty stack", lineNumber = frame.getPc())
                }
                val clonedPeekValue = ds.stack.peek().clone()
                clonedPeekValue.manimObject = EmptyMObject
                return clonedPeekValue
            }

            else -> EmptyValue
        }
    }
}
