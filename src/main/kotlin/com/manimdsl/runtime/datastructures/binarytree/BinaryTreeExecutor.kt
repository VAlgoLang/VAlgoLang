package com.manimdsl.runtime.datastructures.binarytree

import com.manimdsl.frontend.*
import com.manimdsl.linearrepresentation.*
import com.manimdsl.runtime.*
import com.manimdsl.runtime.datastructures.DSExecutor
import com.manimdsl.runtime.utility.getBoundaries
import com.manimdsl.stylesheet.Stylesheet

class BinaryTreeExecutor(
    override val variables: MutableMap<String, ExecValue>,
    override val linearRepresentation: MutableList<ManimInstr>,
    override val frame: VirtualMachine.Frame,
    override val stylesheet: Stylesheet,
    override val animationSpeeds: java.util.ArrayDeque<Double>,
    override val dataStructureBoundaries: MutableMap<String, BoundaryShape>,
    override val variableNameGenerator: VariableNameGenerator,
    override val codeTextVariable: String
) : DSExecutor {

    override fun executeConstructor(node: ConstructorNode, dsUID: String, assignLHS: AssignLHS): ExecValue {
        if (node.type is TreeType) {
            val ident = variableNameGenerator.generateNameFromPrefix("tree")
            val root = frame.executeExpression(node.arguments.first()) as BinaryTreeNodeValue
            val position = stylesheet.getPosition(dsUID)
            dataStructureBoundaries[dsUID] = SquareBoundary(maxSize = 1)
            if (stylesheet.userDefinedPositions() && position == null) {
                return RuntimeError("Missing position values for $dsUID", lineNumber = node.lineNumber)
            }
            val boundaries = getBoundaries(position)
            val binaryTreeValue = BinaryTreeValue(manimObject = EmptyMObject, value = root)
            val initTreeStructure = InitTreeStructure(
                node.type,
                ident,
                text = assignLHS.identifier,
                root = root,
                boundaries = boundaries,
                uid = dsUID,
                runtime = animationSpeeds.first(),
                render = stylesheet.renderDataStructure(frame.functionNamePrefix + assignLHS.identifier)
            )

            binaryTreeValue.style = stylesheet.getStyle(assignLHS.identifier, binaryTreeValue)
            binaryTreeValue.animatedStyle = stylesheet.getAnimatedStyle(assignLHS.identifier, binaryTreeValue)
            binaryTreeValue.manimObject = initTreeStructure
            linearRepresentation.add(initTreeStructure)
            root.attachTree(binaryTreeValue)
            // Remove any variables pointing to node from variable block as it now belongs to a tree
            removeNodeFromVariableState(root)
            return binaryTreeValue
        } else {
            val value = frame.executeExpression(node.arguments.first()) as PrimitiveValue
            val nodeStructure = NodeStructure(
                variableNameGenerator.generateNameFromPrefix("node"),
                value.value.toString(),
                0
            )
            linearRepresentation.add(nodeStructure)
            return BinaryTreeNodeValue(NullValue, NullValue, value, manimObject = nodeStructure, depth = 0)
        }
    }

    fun executeTreeAssignment(
        accessNode: BinaryTreeNodeAccess,
        assignedValue: ExecValue,
    ): ExecValue {
        val isRoot = accessNode is BinaryTreeRootAccessNode
        val node = if (accessNode is BinaryTreeRootAccessNode) {
            accessNode.elemAccessNode
        } else {
            accessNode as BinaryTreeNodeElemAccessNode
        }

        if (assignedValue is EmptyValue || assignedValue is NullValue) {
            return executeTreeDelete(
                (variables[node.identifier]!! as BinaryTreeValue).value,
                node,
            )
        }
        if (assignedValue is DoubleValue) {
            return executeTreeEdit(
                (variables[node.identifier]!! as BinaryTreeValue).value,
                node,
                assignedValue,
            )
        }

        if (isRoot || assignedValue is BinaryTreeNodeValue) {
            return executeTreeAppend(
                (variables[node.identifier]!! as BinaryTreeValue).value,
                node,
                assignedValue as BinaryTreeNodeValue,
            )
        }

        return EmptyValue
    }

    fun executeTreeAccess(
        rootNode: BinaryTreeNodeValue,
        elemAccessNode: BinaryTreeNodeElemAccessNode,
    ): Pair<ExecValue, ExecValue> {
        if (elemAccessNode.accessChain.isEmpty()) {
            return Pair(EmptyValue, rootNode)
        }

        var parentValue: BinaryTreeNodeValue? = rootNode
        for (access in elemAccessNode.accessChain.take(elemAccessNode.accessChain.size - 1)) {
            if (access is NodeType.Left && parentValue?.left is BinaryTreeNodeValue) {
                parentValue = parentValue.left as? BinaryTreeNodeValue
            } else if (parentValue?.right is BinaryTreeNodeValue) {
                parentValue = parentValue.right as? BinaryTreeNodeValue
            } else {
                parentValue = null
                break
            }
        }

        parentValue ?: return Pair(
            EmptyValue,
            RuntimeError("Accessed child does not exist", lineNumber = elemAccessNode.lineNumber)
        )

        val accessedValue = when (elemAccessNode.accessChain.last()) {
            is NodeType.Right -> parentValue.right
            is NodeType.Left -> parentValue.left
            is NodeType.Value -> {
                if (parentValue.binaryTreeValue?.animatedStyle != null) {
                    linearRepresentation.add(
                        TreeNodeRestyle(
                            parentValue.manimObject.shape.ident,
                            parentValue.binaryTreeValue!!.animatedStyle!!,
                            parentValue.binaryTreeValue!!.animatedStyle!!.highlight,
                            animationString = parentValue.binaryTreeValue!!.animatedStyle!!.animationStyle,
                            runtime = animationSpeeds.first(),
                            render = stylesheet.renderDataStructure(frame.functionNamePrefix + elemAccessNode.identifier)
                        )
                    )
                    linearRepresentation.add(
                        TreeNodeRestyle(
                            parentValue.manimObject.shape.ident,
                            parentValue.binaryTreeValue!!.style,
                            animationString = parentValue.binaryTreeValue!!.animatedStyle!!.animationStyle,
                            runtime = animationSpeeds.first(),
                            render = stylesheet.renderDataStructure(frame.functionNamePrefix + elemAccessNode.identifier)
                        )
                    )
                }
                val value = parentValue.value
                value
            }
            else -> RuntimeError("Unknown tree access", lineNumber = elemAccessNode.lineNumber)
        }

        return Pair(parentValue, accessedValue)
    }

    private fun executeTreeAppend(
        rootNode: BinaryTreeNodeValue,
        binaryTreeElemNode: BinaryTreeNodeElemAccessNode,
        childValue: BinaryTreeNodeValue,
    ): ExecValue {
        val (parent, _) = executeTreeAccess(
            rootNode,
            binaryTreeElemNode,
        )
        if (parent is RuntimeError)
            return parent
        if (childValue.binaryTreeValue != null && childValue.binaryTreeValue == rootNode.binaryTreeValue) {
            return RuntimeError("Tree cannot self reference", childValue.manimObject, binaryTreeElemNode.lineNumber)
        } else if (parent is BinaryTreeNodeValue) {
            var isLeft = false
            when (binaryTreeElemNode.accessChain.last()) {
                is NodeType.Left -> {
                    parent.left = childValue
                    childValue.depth = parent.depth + 1
                    isLeft = true
                }
                is NodeType.Right -> {
                    parent.right = childValue
                    childValue.depth = parent.depth + 1
                    isLeft = false
                }
            }

            if (parent.binaryTreeValue != null) {
                if (parent.binaryTreeValue!!.animatedStyle != null) {
                    linearRepresentation.add(
                        TreeNodeRestyle(
                            parent.manimObject.shape.ident,
                            parent.binaryTreeValue!!.animatedStyle!!,
                            parent.binaryTreeValue!!.animatedStyle!!.highlight,
                            animationString = parent.binaryTreeValue!!.animatedStyle!!.animationStyle,
                            runtime = animationSpeeds.first(),
                            render = stylesheet.renderDataStructure(frame.functionNamePrefix + binaryTreeElemNode.identifier)
                        )
                    )
                }
                if (stylesheet.renderDataStructure(frame.functionNamePrefix + binaryTreeElemNode.identifier)) {
                    val boundary =
                        dataStructureBoundaries[frame.functionNamePrefix + (parent.binaryTreeValue!!.manimObject as InitTreeStructure).text]!!
                    boundary.maxSize += childValue.nodeCount()
                    dataStructureBoundaries[frame.functionNamePrefix + (parent.binaryTreeValue!!.manimObject as InitTreeStructure).text] =
                        boundary
                }
                childValue.attachTree(parent.binaryTreeValue!!)
                linearRepresentation.add(
                    TreeAppendObject(
                        parent,
                        childValue,
                        parent.binaryTreeValue!!,
                        isLeft,
                        runtime = animationSpeeds.first(),
                        render = stylesheet.renderDataStructure(frame.functionNamePrefix + binaryTreeElemNode.identifier)
                    )
                )
                if (parent.binaryTreeValue!!.animatedStyle != null) {
                    linearRepresentation.add(
                        TreeNodeRestyle(
                            parent.manimObject.shape.ident,
                            parent.binaryTreeValue!!.style,
                            animationString = parent.binaryTreeValue!!.animatedStyle!!.animationStyle,
                            runtime = animationSpeeds.first(),
                            render = stylesheet.renderDataStructure(frame.functionNamePrefix + binaryTreeElemNode.identifier)
                        )
                    )
                }
            } else {
                linearRepresentation.add(
                    NodeAppendObject(
                        parent,
                        childValue,
                        isLeft,
                        runtime = animationSpeeds.first(),
                        render = false
                    )
                )
            }
        }

        if (rootNode.binaryTreeValue == null) {
            frame.insertVariable(binaryTreeElemNode.identifier, rootNode)
        }
        return EmptyValue
    }

    private fun executeTreeEdit(
        rootNode: BinaryTreeNodeValue,
        binaryTreeElemNode: BinaryTreeNodeElemAccessNode,
        childValue: DoubleValue,
    ): ExecValue {
        val (_, node) = executeTreeAccess(
            rootNode,
            binaryTreeElemNode,
        )
        if (node is RuntimeError)
            return node
        else if (node is BinaryTreeNodeValue) {
            val btNodeValue = BinaryTreeNodeValue(node.left, node.right, childValue, node.manimObject, depth = 0)
            node.binaryTreeValue!!.value = btNodeValue
            val instructions = mutableListOf<ManimInstr>(
                TreeEditValue(
                    node,
                    childValue,
                    node.binaryTreeValue!!,
                    runtime = animationSpeeds.first(),
                    render = stylesheet.renderDataStructure(frame.functionNamePrefix + binaryTreeElemNode.identifier)
                )
            )
            if (node.binaryTreeValue != null) {
                if (node.binaryTreeValue!!.animatedStyle != null) {
                    instructions.add(
                        0,
                        TreeNodeRestyle(
                            node.manimObject.shape.ident,
                            node.binaryTreeValue!!.animatedStyle!!,
                            node.binaryTreeValue!!.animatedStyle!!.highlight,
                            animationString = node.binaryTreeValue!!.animatedStyle!!.animationStyle,
                            runtime = animationSpeeds.first(),
                            render = stylesheet.renderDataStructure(frame.functionNamePrefix + binaryTreeElemNode.identifier)
                        )
                    )
                    instructions.add(
                        TreeNodeRestyle(
                            node.manimObject.shape.ident,
                            node.binaryTreeValue!!.style,
                            animationString = node.binaryTreeValue!!.animatedStyle!!.animationStyle,
                            runtime = animationSpeeds.first(),
                            render = stylesheet.renderDataStructure(frame.functionNamePrefix + binaryTreeElemNode.identifier)
                        )
                    )
                }
                linearRepresentation.addAll(instructions)
            }
        }

        if (rootNode.binaryTreeValue == null || !stylesheet.renderDataStructure(frame.functionNamePrefix + binaryTreeElemNode.identifier)) {
            frame.insertVariable(binaryTreeElemNode.identifier, rootNode)
        }
        return EmptyValue
    }

    fun executeRootAccess(
        binaryTreeRootAccessNode: BinaryTreeRootAccessNode
    ): Pair<ExecValue, ExecValue> {
        val treeNode = variables[binaryTreeRootAccessNode.identifier]!! as BinaryTreeValue
        return executeTreeAccess(
            treeNode.value,
            binaryTreeRootAccessNode.elemAccessNode,
        )
    }

    private fun executeTreeDelete(
        rootNode: BinaryTreeNodeValue,
        binaryTreeElemNode: BinaryTreeNodeElemAccessNode,
    ): ExecValue {
        val (parent, _) = executeTreeAccess(
            rootNode,
            binaryTreeElemNode,
        )
        if (parent is RuntimeError)
            return parent
        else if (parent is BinaryTreeNodeValue) {
            when (binaryTreeElemNode.accessChain.last()) {
                is NodeType.Left -> {
                    parent.left = NullValue
                    if (parent.binaryTreeValue != null) {
                        linearRepresentation.add(
                            TreeDeleteObject(
                                parent,
                                parent.binaryTreeValue!!,
                                true,
                                runtime = animationSpeeds.first(),
                                render = stylesheet.renderDataStructure(frame.functionNamePrefix + binaryTreeElemNode.identifier)
                            )
                        )
                    }
                }
                is NodeType.Right -> {
                    parent.right = NullValue
                    if (parent.binaryTreeValue != null) {
                        linearRepresentation.add(
                            TreeDeleteObject(
                                parent,
                                parent.binaryTreeValue!!,
                                false,
                                runtime = animationSpeeds.first(),
                                render = stylesheet.renderDataStructure(frame.functionNamePrefix + binaryTreeElemNode.identifier)
                            )
                        )
                    }
                }
            }
        }
        frame.insertVariable(binaryTreeElemNode.identifier, rootNode)
        frame.updateVariableState()
        return EmptyValue
    }

    private fun removeNodeFromVariableState(parent: ITreeNodeValue) {
        if (parent is BinaryTreeNodeValue && parent.binaryTreeValue != null) {
            variables.filter { (_, v) -> v == parent }.keys.forEach(frame::removeVariable)
            removeNodeFromVariableState(parent.left)
            removeNodeFromVariableState(parent.right)
        }
    }
}
