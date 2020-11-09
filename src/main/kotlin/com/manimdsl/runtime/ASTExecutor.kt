package com.manimdsl.runtime

import com.manimdsl.ExitStatus
import com.manimdsl.errorhandling.ErrorHandler.addRuntimeError
import com.manimdsl.executor.*
import com.manimdsl.frontend.*
import com.manimdsl.linearrepresentation.*
import com.manimdsl.shapes.Rectangle
import com.manimdsl.stylesheet.Stylesheet
import comcreat.manimdsl.linearrepresentation.*
import java.util.*

class VirtualMachine(
    private val program: ProgramNode,
    private val symbolTableVisitor: SymbolTableVisitor,
    private val statements: MutableMap<Int, StatementNode>,
    private val fileLines: List<String>,
    private val stylesheet: Stylesheet
) {

    private val linearRepresentation = mutableListOf<ManimInstr>()
    private val variableNameGenerator = VariableNameGenerator(symbolTableVisitor)
    private val codeBlockVariable: String = variableNameGenerator.generateNameFromPrefix("code_block")
    private val codeTextVariable: String = variableNameGenerator.generateNameFromPrefix("code_text")
    private val pointerVariable: String = variableNameGenerator.generateNameFromPrefix("pointer")
    private val displayLine: MutableList<Int> = mutableListOf()
    private val displayCode: MutableList<String> = mutableListOf()
    private val dataStructureBoundaries = mutableMapOf<String, BoundaryShape>()
    private val acceptableNonStatements = setOf("}", "{", "")
    private val MAX_DISPLAYED_VARIABLES = 4
    private val WRAP_LINE_LENGTH = 50
    private val ALLOCATED_STACKS = Runtime.getRuntime().freeMemory() / 1000000
    private val STEP_INTO_DEFAULT = stylesheet.getStepIntoIsDefault()
    private val MAX_NUMBER_OF_LOOPS = 10000
    private val hideCode = stylesheet.getHideCode()

    init {
        fileLines.indices.forEach {
            if (statements[it + 1] !is NoRenderAnimationNode &&
                (acceptableNonStatements.any { x -> fileLines[it].contains(x) } || statements[it + 1] is CodeNode)
            ) {
                displayCode.add(fileLines[it])
                displayLine.add(1 + (displayLine.lastOrNull() ?: 0))
            } else {
                displayLine.add(displayLine.lastOrNull() ?: 0)
            }
        }
    }

    fun runProgram(): Pair<ExitStatus, List<ManimInstr>> {
        if (!hideCode) {
            linearRepresentation.add(PartitionBlock("1/3", "2/3"))
            linearRepresentation.add(VariableBlock(listOf(), "variable_block", "variable_vg", "variable_frame"))
            linearRepresentation.add(
                CodeBlock(
                    displayCode.map { it.chunked(WRAP_LINE_LENGTH) },
                    codeBlockVariable,
                    codeTextVariable,
                    pointerVariable
                )
            )
        }

        val variables = mutableMapOf<String, ExecValue>()
        val result = Frame(
            program.statements.first().lineNumber,
            fileLines.size,
            variables,
            hideCode = hideCode,
            updateVariableState = !hideCode
        ).runFrame()
        linearRepresentation.add(Sleep(1.0))
        return if (result is RuntimeError) {
            addRuntimeError(result.value, result.lineNumber)
            Pair(ExitStatus.RUNTIME_ERROR, linearRepresentation)
        } else {
            val (exitStatus, computedBoundaries) = Scene().compute(dataStructureBoundaries.toList(), hideCode)
            if (exitStatus != ExitStatus.EXIT_SUCCESS) {
                return Pair(exitStatus, linearRepresentation)
            }
            val linearRepresentationWithBoundaries = linearRepresentation.map {
                if (it is DataStructureMObject) {
                    val boundaryShape = computedBoundaries[it.ident]!!
                    it.setNewBoundary(boundaryShape.corners(), boundaryShape.maxSize)
                }
                it
            }
            Pair(ExitStatus.EXIT_SUCCESS, linearRepresentationWithBoundaries)
        }
    }


    private fun wrapString(text: String): String {
        val sb = StringBuilder(text)
        for (index in WRAP_LINE_LENGTH until text.length step WRAP_LINE_LENGTH)
            sb.insert(index, "\\n")
        return sb.toString()
    }

    private inner class Frame(
        private var pc: Int,
        private var finalLine: Int,
        private var variables: MutableMap<String, ExecValue>,
        val depth: Int = 1,
        private val showMoveToLine: Boolean = true,
        private var stepInto: Boolean = STEP_INTO_DEFAULT,
        private var leastRecentlyUpdatedQueue: LinkedList<Int> = LinkedList(),
        private var displayedDataMap: MutableMap<Int, Pair<String, PrimitiveValue>> = mutableMapOf(),
        private val updateVariableState: Boolean = true,
        private val hideCode: Boolean = false
    ) {
        private var previousStepIntoState = stepInto

        fun insertVariable(identifier: String, value: ExecValue) {
            if (value is PrimitiveValue) {
                val index = displayedDataMap.filterValues { it.first == identifier }.keys
                if (index.isEmpty()) {
                    // not been visualised
                    // if there is space
                    if (displayedDataMap.size < MAX_DISPLAYED_VARIABLES) {
                        val newIndex = displayedDataMap.size
                        leastRecentlyUpdatedQueue.addLast(newIndex)
                        displayedDataMap[newIndex] = Pair(identifier, value)
                    } else {
                        // if there is no space
                        val oldest = leastRecentlyUpdatedQueue.removeFirst()
                        displayedDataMap[oldest] = Pair(identifier, value)
                        leastRecentlyUpdatedQueue.addLast(oldest)
                    }
                } else {
                    // being visualised
                    leastRecentlyUpdatedQueue.remove(index.first())
                    leastRecentlyUpdatedQueue.addLast(index.first())
                    displayedDataMap[index.first()] = Pair(identifier, value)
                }
            }
        }

        // instantiate new Frame and execute on scoping changes e.g. recursion
        fun runFrame(): ExecValue {
            if (depth > ALLOCATED_STACKS) {
                return RuntimeError(value = "Stack Overflow Error. Program failed to terminate.", lineNumber = pc)
            }

            if (updateVariableState) {
                variables.forEach { (identifier, execValue) -> insertVariable(identifier, execValue) }
                updateVariableState()
            }

            while (pc <= finalLine) {
                if (statements.containsKey(pc)) {
                    val statement = statements[pc]!!

                    if (statement is CodeNode) {
                        moveToLine()
                    }

                    val value = executeStatement(statement)
                    if (statement is ReturnNode || value !is EmptyValue) return value

                }

                fetchNextStatement()
            }
            return EmptyValue
        }

        private fun getVariableState(): List<String> {
            return displayedDataMap.toSortedMap().map { wrapString("${it.value.first} = ${it.value.second}") }
        }

        private fun executeStatement(statement: StatementNode): ExecValue = when (statement) {
            is ReturnNode -> executeExpression(statement.expression)
            is FunctionNode -> {
                // just go onto next line, this is just a label
                EmptyValue
            }
            is SleepNode -> executeSleep(statement)
            is AssignmentNode -> executeAssignment(statement)
            is DeclarationNode -> executeAssignment(statement)
            is MethodCallNode -> executeMethodCall(statement, false, false)
            is FunctionCallNode -> executeFunctionCall(statement)
            is IfStatementNode -> executeIfStatement(statement)
            is WhileStatementNode -> executeWhileStatement(statement)
            is LoopStatementNode -> executeLoopStatement(statement)
            is StartCodeTrackingNode -> {
                previousStepIntoState = stepInto
                stepInto = statement.isStepInto
                EmptyValue
            }
            is StopCodeTrackingNode -> {
                stepInto = previousStepIntoState
                EmptyValue
            }
            else -> EmptyValue
        }

        private fun executeLoopStatement(statement: LoopStatementNode): ExecValue = when (statement) {
            is BreakNode -> {
                pc = statement.loopEndLineNumber
                BreakValue
            }
            is ContinueNode -> {
                pc = statement.loopStartLineNumber
                ContinueValue
            }
        }


        private fun executeSleep(statement: SleepNode): ExecValue {
            linearRepresentation.add(Sleep((executeExpression(statement.sleepTime) as DoubleValue).value))
            return EmptyValue
        }

        private fun addSleep(length: Double) {
            linearRepresentation.add(Sleep(length))
        }

        private fun moveToLine(line: Int = pc) {
            if (showMoveToLine && !hideCode) {
                linearRepresentation.add(
                    MoveToLine(
                        displayLine[line - 1],
                        pointerVariable,
                        codeBlockVariable,
                        codeTextVariable
                    )
                )
            }
        }

        private fun executeFunctionCall(statement: FunctionCallNode): ExecValue {
            // create new stack frame with argument variables
            val executedArguments = mutableListOf<ExecValue>()
            statement.arguments.forEach {
                val executed = executeExpression(it)
                if (executed is RuntimeError)
                    return executed
                else
                    executedArguments.add(executed)
            }
            val argumentNames =
                (symbolTableVisitor.getData(statement.functionIdentifier) as FunctionData).parameters.map { it.identifier }
            val argumentVariables = (argumentNames zip executedArguments).toMap().toMutableMap()
            val functionNode = program.functions.find { it.identifier == statement.functionIdentifier }!!
            val finalStatementLine = functionNode.statements.last().lineNumber
            // program counter will forward in loop, we have popped out of stack
            val returnValue = Frame(
                functionNode.lineNumber,
                finalStatementLine,
                argumentVariables,
                depth + 1,
                showMoveToLine = stepInto,
                stepInto = stepInto && previousStepIntoState,   // In the case of nested stepInto/stepOver
                updateVariableState = updateVariableState,
                hideCode = hideCode
            ).runFrame()
            // to visualise popping back to assignment we can move pointer to the prior statement again
            if (stepInto) moveToLine()
            return returnValue
        }


        private fun fetchNextStatement() {
            ++pc
        }

        private fun executeArrayElemAssignment(arrayElemNode: ArrayElemNode, assignedValue: ExecValue): ExecValue {
            val index = executeExpression(arrayElemNode.index) as DoubleValue
            val arrayValue = variables[arrayElemNode.identifier] as ArrayValue
            return if (index.value.toInt() !in arrayValue.array.indices) {
                RuntimeError(value = "Array index out of bounds", lineNumber = arrayElemNode.lineNumber)
            } else {
                arrayValue.array[index.value.toInt()] = assignedValue
                val style = arrayValue.animatedStyle
                style?.let {
                    linearRepresentation.add(
                        ArrayElemRestyle(
                            (arrayValue.manimObject as ArrayStructure).ident,
                            listOf(index.value.toInt()),
                            it,
                            it.pointer
                        )
                    )
                }
                linearRepresentation.add(
                    ArrayElemAssignObject(
                        (arrayValue.manimObject as ArrayStructure).ident,
                        index.value.toInt(),
                        assignedValue,
                        arrayValue.animatedStyle
                    )
                )
                if (arrayValue.animatedStyle != null) {
                    linearRepresentation.add(
                        ArrayElemRestyle(
                            (arrayValue.manimObject as ArrayStructure).ident,
                            listOf(index.value.toInt()),
                            arrayValue.style
                        )
                    )
                }
                EmptyValue
            }
        }

        private fun executeAssignment(node: DeclarationOrAssignment): ExecValue {
            val assignedValue = executeExpression(node.expression, identifier = node.identifier)

            with(node.identifier) {
                when (this) {
                    is BinaryTreeRootAccessNode -> {
                        return executeTreeAppend((variables[identifier]!! as BinaryTreeValue).value, elemAccessNode, assignedValue as BinaryTreeNodeValue)
                    }
                    is BinaryTreeNodeElemAccessNode -> {
                        return executeTreeAppend((variables[identifier]!! as BinaryTreeNodeValue), this, assignedValue as BinaryTreeNodeValue)
                    }
                    is IdentifierNode -> variables[node.identifier.identifier] = assignedValue
                    is ArrayElemNode -> {
                        return executeArrayElemAssignment(this, assignedValue)
                    }
                }
            }
            return if (assignedValue is RuntimeError) {
                assignedValue
            } else {
                if (node.identifier is IdentifierNode) {
                    insertVariable(node.identifier.identifier, assignedValue)
                    updateVariableState()
                }
                EmptyValue
            }
        }

        private fun executeTreeAppend(rootNode: BinaryTreeNodeValue, binaryTreeElemNode: BinaryTreeNodeElemAccessNode, childValue: BinaryTreeNodeValue): ExecValue {
            val (parent, _) = executeTreeAccess(rootNode, binaryTreeElemNode)
            if (parent is RuntimeError)
                return parent
            else if (parent is BinaryTreeNodeValue) {
                when (binaryTreeElemNode.accessChain.last()) {
                    is NodeType.Left -> {
                        parent.left = childValue
                        childValue.depth = parent.depth+1
                        if (parent.binaryTreeValue != null) {
                            childValue.attachTree(parent.binaryTreeValue!!)
                            linearRepresentation.add(NodeFocusObject(parent))
                            linearRepresentation.add(TreeAppendObject(parent, childValue, parent.binaryTreeValue!!, true))
                            linearRepresentation.add(NodeFocusObject(childValue))
                            linearRepresentation.add(NodeUnfocusObject(parent))
                            linearRepresentation.add(NodeUnfocusObject(childValue))

                        } else {
                            linearRepresentation.add(NodeAppendObject(parent, childValue, true))
                        }
                    }
                    is NodeType.Right -> {
                        parent.right = childValue
                        childValue.depth = parent.depth+1
                        if (parent.binaryTreeValue != null) {
                            childValue.attachTree(parent.binaryTreeValue!!)
                            linearRepresentation.add(NodeFocusObject(parent))
                            linearRepresentation.add(TreeAppendObject(parent, childValue, parent.binaryTreeValue!!, false))
                            linearRepresentation.add(NodeFocusObject(childValue))
                            linearRepresentation.add(NodeUnfocusObject(parent))
                            linearRepresentation.add(NodeUnfocusObject(childValue))
                        } else {
                            linearRepresentation.add(NodeAppendObject(parent, childValue, false))
                        }
                    }
                }

            }
            return EmptyValue
        }

        private fun updateVariableState() {
            if (showMoveToLine && !hideCode)
                linearRepresentation.add(UpdateVariableState(getVariableState(), "variable_block"))
        }

        private fun executeExpression(
            node: ExpressionNode,
            insideMethodCall: Boolean = false,
            identifier: AssignLHS = EmptyLHS
        ): ExecValue = when (node) {
            is IdentifierNode -> variables[node.identifier]!!
            is NumberNode -> DoubleValue(node.double)
            is MethodCallNode -> executeMethodCall(node, insideMethodCall, true)
            is AddExpression -> executeBinaryOp(node) { x, y -> x + y }
            is SubtractExpression -> executeBinaryOp(node) { x, y -> x - y }
            is MultiplyExpression -> executeBinaryOp(node) { x, y -> x * y }
            is PlusExpression -> executeUnaryOp(node) { x -> x }
            is MinusExpression -> executeUnaryOp(node) { x -> DoubleValue(-(x as DoubleValue).value) }
            is BoolNode -> BoolValue(node.value)
            is AndExpression -> executeShortCircuitOp(
                node,
                false
            ) { x, y -> BoolValue((x as BoolValue).value && (y as BoolValue).value) }
            is OrExpression -> executeShortCircuitOp(
                node,
                true
            ) { x, y -> BoolValue((x as BoolValue).value || (y as BoolValue).value) }
            is EqExpression -> executeBinaryOp(node) { x, y -> BoolValue(x == y) }
            is NeqExpression -> executeBinaryOp(node) { x, y -> BoolValue(x != y) }
            is GtExpression -> executeBinaryOp(node) { x, y -> BoolValue(x > y) }
            is LtExpression -> executeBinaryOp(node) { x, y -> BoolValue(x < y) }
            is GeExpression -> executeBinaryOp(node) { x, y -> BoolValue(x >= y) }
            is LeExpression -> executeBinaryOp(node) { x, y -> BoolValue(x <= y) }
            is NotExpression -> executeUnaryOp(node) { x -> BoolValue(!x) }
            is ConstructorNode -> executeConstructor(node, identifier)
            is FunctionCallNode -> executeFunctionCall(node)
            is VoidNode -> VoidValue
            is ArrayElemNode -> executeArrayElem(node)
            is BinaryTreeNodeElemAccessNode -> executeTreeAccess(
                    variables[node.identifier]!! as BinaryTreeNodeValue,
                    node
            ).second
            is BinaryTreeRootAccessNode -> executeRootAccess(node).second
            is NullNode -> TODO()
        }

        private fun executeRootAccess(binaryTreeRootAccessNode: BinaryTreeRootAccessNode): Pair<ExecValue, ExecValue> {
            val treeNode = variables[binaryTreeRootAccessNode.identifier]!! as BinaryTreeValue
            return executeTreeAccess(treeNode.value, binaryTreeRootAccessNode.elemAccessNode)
        }

        private fun executeTreeAccess(rootNode: BinaryTreeNodeValue, elemAccessNode: BinaryTreeNodeElemAccessNode): Pair<ExecValue, ExecValue> {
            if (elemAccessNode.accessChain.isEmpty()) {
                return Pair(EmptyValue, rootNode)
            }

            val parentValue = elemAccessNode.accessChain.take(elemAccessNode.accessChain.size-1).foldRight(rootNode) { method, current ->
                if (method is NodeType.Left) {
                    current.left?:return Pair(RuntimeError("Accessed child does not exist", lineNumber = elemAccessNode.lineNumber), RuntimeError("Accessed child does not exist", lineNumber = elemAccessNode.lineNumber))
                } else {
                    current.right?:return Pair(RuntimeError("Accessed child does not exist", lineNumber = elemAccessNode.lineNumber), RuntimeError("Accessed child does not exist", lineNumber = elemAccessNode.lineNumber))
                }
            }
            val accessedValue = when (elemAccessNode.accessChain.last()) {
                is NodeType.Right -> parentValue.right?:RuntimeError("Accessed child does not exist", lineNumber = elemAccessNode.lineNumber)
                is NodeType.Left -> parentValue.left?:RuntimeError("Accessed child does not exist", lineNumber = elemAccessNode.lineNumber)
                else -> RuntimeError("Unknown tree access", lineNumber = elemAccessNode.lineNumber)
            }

            return Pair(parentValue, accessedValue)
        }

        private fun executeArrayElem(node: ArrayElemNode): ExecValue {
            val arrayValue = variables[node.identifier] as ArrayValue
            val index = executeExpression(node.index) as DoubleValue
            return if (index.value.toInt() !in arrayValue.array.indices) {
                RuntimeError(value = "Array index out of bounds", lineNumber = node.lineNumber)
            } else {
                with(arrayValue.animatedStyle) {
                    if (showMoveToLine && this != null) {
                        linearRepresentation.add(
                            ArrayElemRestyle(
                                (arrayValue.manimObject as ArrayStructure).ident,
                                listOf(index.value.toInt()),
                                this,
                                pointer
                            )
                        )
                        linearRepresentation.add(
                            ArrayElemRestyle(
                                (arrayValue.manimObject as ArrayStructure).ident,
                                listOf(index.value.toInt()),
                                arrayValue.style
                            )
                        )
                    }
                }
                arrayValue.array[index.value.toInt()]
            }
        }

        private fun executeMethodCall(node: MethodCallNode, insideMethodCall: Boolean, isExpression: Boolean): ExecValue {
            return when (val ds = variables[node.instanceIdentifier]) {
                is StackValue -> {
                    return executeStackMethodCall(node, ds, insideMethodCall, isExpression)
                }
                is ArrayValue -> {
                    return executeArrayMethodCall(node, ds)
                }

                else -> EmptyValue
            }
        }


        private fun executeArrayMethodCall(node: MethodCallNode, ds: ArrayValue): ExecValue {
            return when (node.dataStructureMethod) {
                is ArrayType.Size -> {
                    DoubleValue(ds.array.size.toDouble())
                }
                is ArrayType.Swap -> {
                    val index1 = (executeExpression(node.arguments[0]) as DoubleValue).value.toInt()
                    val index2 = (executeExpression(node.arguments[1]) as DoubleValue).value.toInt()
                    val longSwap =
                        if (node.arguments.size != 3) false else (executeExpression(node.arguments[2]) as BoolValue).value
                    val arrayIdent = (ds.manimObject as ArrayStructure).ident
                    val arraySwap =
                        if (longSwap) {
                            ArrayLongSwap(
                                arrayIdent,
                                Pair(index1, index2),
                                variableNameGenerator.generateNameFromPrefix("elem1"),
                                variableNameGenerator.generateNameFromPrefix("elem2"),
                                variableNameGenerator.generateNameFromPrefix("animations")
                            )
                        } else {
                            ArrayShortSwap(arrayIdent, Pair(index1, index2))
                        }
                    val swap = mutableListOf(arraySwap)
                    with(ds.animatedStyle) {
                        if (this != null) {
                            swap.add(0, ArrayElemRestyle(arrayIdent, listOf(index1, index2), this, this.pointer))
                            swap.add(ArrayElemRestyle(arrayIdent, listOf(index1, index2), ds.style))
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

        private fun executeStackMethodCall(node: MethodCallNode, ds: StackValue, insideMethodCall: Boolean, isExpression: Boolean): ExecValue {
            return when (node.dataStructureMethod) {
                is StackType.PushMethod -> {
                    val value = executeExpression(node.arguments[0], true)
                    if (value is RuntimeError) {
                        return value
                    }
                    val dataStructureIdentifier = (ds.manimObject as InitManimStack).ident
                    val boundaryShape = dataStructureBoundaries[dataStructureIdentifier]!!
                    boundaryShape.maxSize++
                    dataStructureBoundaries[dataStructureIdentifier] = boundaryShape
                    val hasOldMObject = value.manimObject !is EmptyMObject
                    val oldMObject = value.manimObject
                    val newObjectStyle = ds.animatedStyle ?: ds.style
                    val rectangle = if (hasOldMObject) oldMObject else NewMObject(
                        Rectangle(
                            variableNameGenerator.generateNameFromPrefix("rectangle"),
                            value.value.toString(),
                            dataStructureIdentifier,
                            color = newObjectStyle.borderColor,
                            textColor = newObjectStyle.textColor
                        ),
                        codeTextVariable
                    )

                    val instructions: MutableList<ManimInstr> =
                        mutableListOf(
                            StackPushObject(
                                rectangle.shape,
                                dataStructureIdentifier,
                                hasOldMObject
                            ),
                            RestyleObject(rectangle.shape, ds.style)
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
                            lineNumber = pc
                        )
                    }
                    val poppedValue = ds.stack.pop()
                    val dataStructureIdentifier = (ds.manimObject as InitManimStack).ident

                    val topOfStack = poppedValue.manimObject
                    val instructions = mutableListOf<ManimInstr>(
                        StackPopObject(
                            topOfStack.shape,
                            dataStructureIdentifier,
                            insideMethodCall
                        )
                    )
                    ds.animatedStyle?.let { instructions.add(0, RestyleObject(topOfStack.shape, it)) }
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
                        return RuntimeError(value = "Attempted to peek empty stack", lineNumber = pc)
                    }
                    val clonedPeekValue = ds.stack.peek().clone()
                    clonedPeekValue.manimObject = EmptyMObject
                    return clonedPeekValue
                }

                else -> EmptyValue
            }
        }

        private fun executeConstructor(node: ConstructorNode, assignLHS: AssignLHS): ExecValue {
            return when (node.type) {
                is StackType -> {
                    val stackValue = StackValue(EmptyMObject, Stack())
                    val initStructureIdent = variableNameGenerator.generateNameFromPrefix("stack")
                    dataStructureBoundaries[initStructureIdent] = TallBoundary()
                    stackValue.style = stylesheet.getStyle(assignLHS.identifier, stackValue)
                    stackValue.animatedStyle = stylesheet.getAnimatedStyle(assignLHS.identifier, stackValue)
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
                            showLabel = stackValue.style.showLabel
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
                            numStack.manimObject.shape,
                            color = stackValue.style.borderColor,
                            textColor = stackValue.style.textColor,
                        )
                        Pair(listOf(stackInit), stackInit)
                    }
                    linearRepresentation.addAll(instructions)
                    val newObjectStyle = stackValue.style
                    node.initialValue.map { executeExpression(it) }.forEach {
                        val rectangle = Rectangle(
                            variableNameGenerator.generateNameFromPrefix("rectangle"),
                            it.value.toString(),
                            initStructureIdent,
                            color = newObjectStyle.borderColor,
                            textColor = newObjectStyle.textColor
                        )

                        val newRectangle = NewMObject(
                            rectangle,
                            codeTextVariable
                        )
                        val clonedValue = it.clone()
                        clonedValue.manimObject = newRectangle
                        stackValue.stack.push(clonedValue)
                        linearRepresentation.add(newRectangle)
                        linearRepresentation.add(StackPushObject(rectangle, initStructureIdent))
                    }
                    stackValue.manimObject = newObject
                    stackValue
                }
                is ArrayType -> {
                    val arraySize =
                        if (node.arguments.isNotEmpty()) executeExpression(node.arguments[0]) as DoubleValue else DoubleValue(
                            node.initialValue.size.toDouble()
                        )
                    val arrayValue = if (node.initialValue.isEmpty()) {
                        ArrayValue(
                            EmptyMObject,
                            Array(arraySize.value.toInt()) { _ ->
                                getDefaultValueForType(
                                    node.type.internalType,
                                    node.lineNumber
                                )
                            })
                    } else {
                        if (node.initialValue.size != arraySize.value.toInt()) {
                            RuntimeError("Initialisation of array failed.", lineNumber = node.lineNumber)
                        } else {
                            ArrayValue(EmptyMObject, node.initialValue.map { executeExpression(it) }.toTypedArray())
                        }
                    }
                    val ident = variableNameGenerator.generateNameFromPrefix("array")
                    dataStructureBoundaries[ident] = WideBoundary(maxSize = arraySize.value.toInt())
                    if (arrayValue is ArrayValue) {
                        arrayValue.style = stylesheet.getStyle(assignLHS.identifier, arrayValue)
                        arrayValue.animatedStyle = stylesheet.getAnimatedStyle(assignLHS.identifier, arrayValue)
                        val arrayStructure = ArrayStructure(
                            node.type,
                            ident,
                            assignLHS.identifier,
                            arrayValue.array.clone(),
                            color = arrayValue.style.borderColor,
                            textColor = arrayValue.style.textColor,
                            showLabel = arrayValue.style.showLabel
                        )
                        linearRepresentation.add(arrayStructure)
                        arrayValue.manimObject = arrayStructure
                    }
                    arrayValue
                }
                is TreeType -> {
                    val ident = variableNameGenerator.generateNameFromPrefix("tree")
                    dataStructureBoundaries[ident] = SquareBoundary()
                    val root = executeExpression(node.arguments.first()) as BinaryTreeNodeValue
                    val initTreeStructure = InitTreeStructure(
                            node.type,
                            ident,
                            text = assignLHS.identifier,
                            root = root
                    )
                    linearRepresentation.add(initTreeStructure)
                    val binaryTreeValue = BinaryTreeValue(manimObject = initTreeStructure, value = root)
                    root.attachTree(binaryTreeValue)
                    return binaryTreeValue
                }
                is NodeType -> {
                    val value = executeExpression(node.arguments.first()) as PrimitiveValue
                    val nodeStructure = NodeStructure(
                        variableNameGenerator.generateNameFromPrefix("node"),
                        value.value.toString(),
                        0
                    )
                    linearRepresentation.add(nodeStructure)
                    return BinaryTreeNodeValue(null, null, value, manimObject = nodeStructure, depth = 0)
                }
                else -> EmptyValue
            }
        }

        private fun getDefaultValueForType(type: Type, lineNumber: Int): ExecValue {
            return when (type) {
                NumberType -> DoubleValue(0.0)
                BoolType -> BoolValue(false)
                is ArrayType -> getDefaultValueForType(type.internalType, lineNumber)
                else -> RuntimeError(value = "Cannot create data structure with type $type", lineNumber = lineNumber)
            }
        }

        private fun executeUnaryOp(node: UnaryExpression, op: (first: ExecValue) -> ExecValue): ExecValue {
            val subExpression = executeExpression(node.expr)
            return if (subExpression is RuntimeError) {
                subExpression
            } else {
                op(subExpression)
            }
        }

        // Used for and and or to short-circuit with first value
        private fun executeShortCircuitOp(
            node: BinaryExpression,
            shortCircuitValue: Boolean,
            op: (first: ExecValue, seconds: ExecValue) -> ExecValue
        ): ExecValue {

            val leftExpression = executeExpression(node.expr1)
            if (leftExpression is RuntimeError || leftExpression.value == shortCircuitValue) {
                return leftExpression
            }
            val rightExpression = executeExpression(node.expr2)

            if (rightExpression is RuntimeError) {
                return rightExpression
            }
            return op(
                leftExpression,
                rightExpression
            )
        }

        private fun executeBinaryOp(
            node: BinaryExpression,
            op: (first: ExecValue, seconds: ExecValue) -> ExecValue
        ): ExecValue {

            val leftExpression = executeExpression(node.expr1)
            if (leftExpression is RuntimeError) {
                return leftExpression
            }
            val rightExpression = executeExpression(node.expr2)

            if (rightExpression is RuntimeError) {
                return rightExpression
            }
            return op(
                leftExpression,
                rightExpression
            )
        }


        private fun executeWhileStatement(whileStatementNode: WhileStatementNode): ExecValue {
            if (showMoveToLine) addSleep(0.5)

            var conditionValue: ExecValue
            var execValue: ExecValue
            var loopCount = 0

            while (loopCount < MAX_NUMBER_OF_LOOPS) {
                conditionValue = executeExpression(whileStatementNode.condition)
                if (conditionValue is RuntimeError) {
                    return conditionValue
                } else if (conditionValue is BoolValue) {
                    if (!conditionValue.value) {
                        pc = whileStatementNode.endLineNumber
                        return EmptyValue
                    } else {
                        pc = whileStatementNode.lineNumber
                    }
                }

                execValue = Frame(
                    whileStatementNode.statements.first().lineNumber,
                    whileStatementNode.statements.last().lineNumber,
                    variables,
                    depth,
                    showMoveToLine = showMoveToLine,
                    stepInto = stepInto,
                    hideCode = hideCode
                ).runFrame()

                when (execValue) {
                    is BreakValue -> {
                        pc = whileStatementNode.endLineNumber
                        moveToLine()
                        return EmptyValue
                    }
                    is ContinueValue -> {
                        pc = whileStatementNode.lineNumber
                        moveToLine()
                        continue
                    }
                    !is EmptyValue -> {
                        return execValue
                    }
                }

                pc = whileStatementNode.lineNumber
                moveToLine()
                loopCount++
            }

            return RuntimeError("Max number of loop executions exceeded", lineNumber = whileStatementNode.lineNumber)
        }

        private fun executeIfStatement(ifStatementNode: IfStatementNode): ExecValue {
            if (showMoveToLine && !hideCode) addSleep(0.5)
            var conditionValue = executeExpression(ifStatementNode.condition)
            if (conditionValue is RuntimeError) {
                return conditionValue
            } else {
                conditionValue = conditionValue as BoolValue
            }
            // Set pc to end of if statement as branching is handled here
            pc = ifStatementNode.endLineNumber

            //If
            if (conditionValue.value) {
                val execValue = Frame(
                    ifStatementNode.statements.first().lineNumber,
                    ifStatementNode.statements.last().lineNumber,
                    variables,
                    depth,
                    showMoveToLine = showMoveToLine,
                    stepInto = stepInto,
                    updateVariableState = updateVariableState,
                    hideCode = hideCode
                ).runFrame()
                if (execValue is EmptyValue) {
                    pc = ifStatementNode.endLineNumber
                }
                return execValue
            }

            // Elif
            for (elif in ifStatementNode.elifs) {
                moveToLine(elif.lineNumber)
                if (showMoveToLine && !hideCode) addSleep(0.5)
                // Add statement to code
                conditionValue = executeExpression(elif.condition) as BoolValue
                if (conditionValue.value) {
                    val execValue = Frame(
                        elif.statements.first().lineNumber,
                        elif.statements.last().lineNumber,
                        variables,
                        depth,
                        showMoveToLine = showMoveToLine,
                        stepInto = stepInto,
                        updateVariableState = updateVariableState,
                        hideCode = hideCode
                    ).runFrame()
                    if (execValue is EmptyValue) {
                        pc = ifStatementNode.endLineNumber
                    }
                    return execValue
                }
            }

            // Else
            if (ifStatementNode.elseBlock.statements.isNotEmpty()) {
                moveToLine(ifStatementNode.elseBlock.lineNumber)
                if (showMoveToLine && !hideCode) addSleep(0.5)
                val execValue = Frame(
                    ifStatementNode.elseBlock.statements.first().lineNumber,
                    ifStatementNode.elseBlock.statements.last().lineNumber,
                    variables,
                    depth,
                    showMoveToLine = showMoveToLine,
                    stepInto = stepInto,
                    updateVariableState = updateVariableState,
                    hideCode = hideCode
                ).runFrame()
                if (execValue is EmptyValue) {
                    pc = ifStatementNode.endLineNumber
                }
                return execValue
            }
            return EmptyValue
        }
    }

}
