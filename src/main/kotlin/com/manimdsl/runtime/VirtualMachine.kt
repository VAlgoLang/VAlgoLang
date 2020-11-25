package com.manimdsl.runtime

import com.google.gson.Gson
import com.manimdsl.ExitStatus
import com.manimdsl.errorhandling.ErrorHandler.addRuntimeError
import com.manimdsl.frontend.*
import com.manimdsl.linearrepresentation.*
import com.manimdsl.runtime.utility.getBoundaries
import com.manimdsl.runtime.utility.wrapCode
import com.manimdsl.shapes.Rectangle
import com.manimdsl.stylesheet.PositionProperties
import com.manimdsl.stylesheet.Stylesheet
import java.util.*

class VirtualMachine(
    private val program: ProgramNode,
    private val symbolTableVisitor: SymbolTableVisitor,
    private val statements: MutableMap<Int, StatementNode>,
    private val fileLines: List<String>,
    private val stylesheet: Stylesheet,
    private val returnBoundaries: Boolean = false
) {

    private val linearRepresentation = mutableListOf<ManimInstr>()
    private val variableNameGenerator = VariableNameGenerator(symbolTableVisitor)
    private val codeBlockVariable: String = variableNameGenerator.generateNameFromPrefix("code_block")
    private val codeTextVariable: String = variableNameGenerator.generateNameFromPrefix("code_text")
    private val pointerVariable: String = variableNameGenerator.generateNameFromPrefix("pointer")
    private val displayLine: MutableList<Int> = mutableListOf()
    private val displayCode: MutableList<String> = mutableListOf()
    private val dataStructureBoundaries = mutableMapOf<String, BoundaryShape>()
    private var acceptableNonStatements = setOf("}", "{")
    private val MAX_DISPLAYED_VARIABLES = 4
    private val WRAP_LINE_LENGTH = 50
    private val ALLOCATED_STACKS = Runtime.getRuntime().freeMemory() / 1000000
    private val STEP_INTO_DEFAULT = stylesheet.getStepIntoIsDefault()
    private val MAX_NUMBER_OF_LOOPS = 10000
    private val hideCode = stylesheet.getHideCode()
    private var animationSpeeds = ArrayDeque(listOf(1.0))

    init {
        if (stylesheet.getDisplayNewLinesInCode()) {
            acceptableNonStatements = acceptableNonStatements.plus("")
        }
        fileLines.indices.forEach {
            if (statements[it + 1] !is NoRenderAnimationNode &&
                (acceptableNonStatements.any { x -> fileLines[it].contains(x) } || statements[it + 1] is CodeNode)
            ) {
                if (fileLines[it].isEmpty()) {
                    if (stylesheet.getDisplayNewLinesInCode()) {
                        displayCode.add(" ")
                        displayLine.add(1 + (displayLine.lastOrNull() ?: 0))
                    }
                } else {
                    displayCode.add(fileLines[it])
                    displayLine.add(1 + (displayLine.lastOrNull() ?: 0))
                }
            } else {
                displayLine.add(displayLine.lastOrNull() ?: 0)
            }
        }
    }

    fun runProgram(): Pair<ExitStatus, List<ManimInstr>> {
        if (!hideCode) {
            linearRepresentation.add(PartitionBlock("1/3", "2/3"))
            linearRepresentation.add(
                VariableBlock(
                    listOf(),
                    "variable_block",
                    "variable_vg",
                    "variable_frame",
                    runtime = animationSpeeds.first()
                )
            )
            linearRepresentation.add(
                CodeBlock(
                    wrapCode(displayCode),
                    codeBlockVariable,
                    codeTextVariable,
                    pointerVariable,
                    runtime = animationSpeeds.first(),
                    syntaxHighlightingOn = stylesheet.getSyntaxHighlighting(),
                    syntaxHighlightingStyle = stylesheet.getSyntaxHighlightingStyle(),
                    tabSpacing = stylesheet.getTabSpacing()
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
        linearRepresentation.add(Sleep(1.0, runtime = animationSpeeds.first()))
        return if (result is RuntimeError) {
            addRuntimeError(result.value, result.lineNumber)
            Pair(ExitStatus.RUNTIME_ERROR, linearRepresentation)
        } else if (returnBoundaries || !stylesheet.userDefinedPositions()) {
            val (exitStatus, computedBoundaries) = Scene().compute(dataStructureBoundaries.toList(), hideCode)
            if (returnBoundaries) {
                val boundaries = mutableMapOf<String, Map<String, PositionProperties>>()
                boundaries["auto"] = computedBoundaries.mapValues { it.value.positioning() }
                boundaries["stylesheet"] = stylesheet.getPositions().filter { it.key in dataStructureBoundaries.keys }
                val gson = Gson()
                println(gson.toJson(boundaries))
            }
            if (exitStatus != ExitStatus.EXIT_SUCCESS) {
                return Pair(exitStatus, linearRepresentation)
            }
            val linearRepresentationWithBoundaries = linearRepresentation.map {
                if (it is DataStructureMObject) {
                    val boundaryShape = computedBoundaries[it.uid]!!
                    it.setNewBoundary(boundaryShape.corners(), boundaryShape.maxSize)
                }
                it
            }
            Pair(ExitStatus.EXIT_SUCCESS, linearRepresentationWithBoundaries)
        } else {
            linearRepresentation.forEach {
                if (it is DataStructureMObject) {
                    it.setShape()
                }
            }
            Pair(ExitStatus.EXIT_SUCCESS, linearRepresentation)
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
        private var displayedDataMap: MutableMap<Int, Pair<String, ExecValue>> = mutableMapOf(),
        private val updateVariableState: Boolean = true,
        private val hideCode: Boolean = false,
        private val functionNamePrefix: String = "",
        private val localDataStructure: MutableSet<String> ? = null
    ) {
        private var previousStepIntoState = stepInto

        fun insertVariable(identifier: String, value: ExecValue) {
            if (value is PrimitiveValue || value is ITreeNodeValue) {
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

        fun removeVariable(identifier: String) {
            displayedDataMap = displayedDataMap.filter { (_, v) -> v.first != identifier }.toMutableMap()
        }

        fun convertToIdent(dataStructureVariable: MutableSet<String>?) {
            if (dataStructureVariable != null) {
                val idents = dataStructureVariable.map { (variables[it]!!.manimObject as DataStructureMObject).ident }
                dataStructureVariable.forEach {
                    variables[it] = EmptyValue
                }
                dataStructureVariable.clear()
                dataStructureVariable.addAll(idents)
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
                    if (statement is ReturnNode || value !is EmptyValue) {
                        convertToIdent(localDataStructure)
                        return value
                    }
                }

                fetchNextStatement()
            }
            convertToIdent(localDataStructure)
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
            is ForStatementNode -> executeForStatement(statement)
            is LoopStatementNode -> executeLoopStatement(statement)
            is InternalArrayMethodCallNode -> executeInternalArrayMethodCall(statement)
            is StartSpeedChangeNode -> {
                val condition = executeExpression(statement.condition)
                val factor = executeExpression(statement.speedChange)
                if (condition is BoolValue && factor is DoubleValue) {
                    if (factor.value <= 0) {
                        RuntimeError("Non positive speed change provided", lineNumber = statement.lineNumber)
                    }
                    if (condition.value) {
                        animationSpeeds.addFirst(1.0 / factor.value)
                    } else {
                        animationSpeeds.addFirst(animationSpeeds.first)
                    }
                    EmptyValue
                } else if (condition is BoolValue) {
                    factor
                } else {
                    condition
                }
            }
            is StopSpeedChangeNode -> {
                animationSpeeds.removeFirst()
                EmptyValue
            }
            is StartCodeTrackingNode -> {
                val condition = executeExpression(statement.condition)
                if (condition is BoolValue) {
                    previousStepIntoState = stepInto
                    if (condition.value) {
                        stepInto = statement.isStepInto
                    }
                    EmptyValue
                } else {
                    condition
                }
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
            linearRepresentation.add(
                Sleep(
                    (executeExpression(statement.sleepTime) as DoubleValue).value,
                    runtime = animationSpeeds.first()
                )
            )
            return EmptyValue
        }

        private fun addSleep(length: Double) {
            linearRepresentation.add(Sleep(length, runtime = animationSpeeds.first()))
        }

        private fun moveToLine(line: Int = pc) {
            if (showMoveToLine && !hideCode && !fileLines[line - 1].isEmpty()) {
                linearRepresentation.add(
                    MoveToLine(
                        displayLine[line - 1],
                        pointerVariable,
                        codeBlockVariable,
                        codeTextVariable,
                        runtime = animationSpeeds.first()
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

            val localDataStructure = mutableSetOf<String>()

            // program counter will forward in loop, we have popped out of stack
            val returnValue = Frame(
                functionNode.lineNumber,
                finalStatementLine,
                argumentVariables,
                depth + 1,
                showMoveToLine = stepInto,
                stepInto = stepInto && previousStepIntoState, // In the case of nested stepInto/stepOver
                updateVariableState = updateVariableState,
                hideCode = hideCode,
                functionNamePrefix = "${functionNode.identifier}.",
                localDataStructure = localDataStructure
            ).runFrame()

            if (localDataStructure.isNotEmpty()) {
                linearRepresentation.add(CleanUpLocalDataStructures(localDataStructure, animationSpeeds.first()))
            }

            // to visualise popping back to assignment we can move pointer to the prior statement again
            if (stepInto) moveToLine()
//            if (updateVariableState) updateVariableState()
            return returnValue
        }

        private fun fetchNextStatement() {
            ++pc
        }

        private fun executeArrayElemAssignment(arrayElemNode: ArrayElemNode, assignedValue: ExecValue): ExecValue {
            val indices = arrayElemNode.indices.map { executeExpression(it) as DoubleValue }
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
                                        runtime = animationSpeeds.first()
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
                                        secondIndices = listOf(index)
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
                                    runtime = animationSpeeds.first()
                                )
                            )
                            arrayValue.animatedStyle?.let {
                                linearRepresentation.add(
                                    ArrayElemRestyle(
                                        (arrayValue.manimObject as Array2DStructure).ident,
                                        listOf(index2),
                                        arrayValue.style,
                                        secondIndices = listOf(index),
                                        runtime = animationSpeeds.first()
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
                                    runtime = it.animationTime ?: animationSpeeds.first()
                                )
                            )
                        }
                        linearRepresentation.add(
                            ArrayElemAssignObject(
                                (arrayValue.manimObject as ArrayStructure).ident,
                                index.value.toInt(),
                                assignedValue,
                                arrayValue.animatedStyle,
                                runtime = animationSpeeds.first()
                            )
                        )
                        arrayValue.animatedStyle?.let {
                            linearRepresentation.add(
                                ArrayElemRestyle(
                                    (arrayValue.manimObject as ArrayStructure).ident,
                                    listOf(index.value.toInt()),
                                    arrayValue.style,
                                    runtime = animationSpeeds.first()
                                )
                            )
                        }
                        EmptyValue
                    }
                }
                else -> EmptyValue
            }
        }

        private fun executeAssignment(node: DeclarationOrAssignment): ExecValue {
            if (variables.containsKey(node.identifier.identifier)) {
                with(variables[node.identifier.identifier]?.manimObject) {
                    if (this is DataStructureMObject) {
                        linearRepresentation.add(CleanUpLocalDataStructures(setOf(this.ident), animationSpeeds.first()))
                    }
                }
            }

            val assignedValue = executeExpression(node.expression, identifier = node.identifier)
            return if (assignedValue is RuntimeError) {
                assignedValue
            } else {
                with(node.identifier) {
                    when (this) {
                        is BinaryTreeRootAccessNode -> {
                            if (assignedValue is EmptyValue) {
                                return executeTreeDelete(
                                    (variables[identifier]!! as BinaryTreeValue).value,
                                    elemAccessNode
                                )
                            }
                            if (assignedValue is DoubleValue) {
                                return executeTreeEdit(
                                    (variables[identifier]!! as BinaryTreeValue).value,
                                    elemAccessNode,
                                    assignedValue
                                )
                            }
                            return executeTreeAppend(
                                (variables[identifier]!! as BinaryTreeValue).value,
                                elemAccessNode,
                                assignedValue as BinaryTreeNodeValue
                            )
                        }
                        is BinaryTreeNodeElemAccessNode -> {
                            if (assignedValue is NullValue) {
                                return executeTreeDelete(variables[identifier]!! as BinaryTreeNodeValue, this)
                            }
                            if (assignedValue is DoubleValue) {
                                return executeTreeEdit(
                                    (variables[identifier]!! as BinaryTreeNodeValue),
                                    this,
                                    assignedValue
                                )
                            }
                            if (assignedValue is BinaryTreeNodeValue) {
                                return executeTreeAppend(
                                    (variables[identifier]!! as BinaryTreeNodeValue),
                                    this,
                                    assignedValue
                                )
                            }
                        }
                        is IdentifierNode -> {
                            if (assignedValue is BinaryTreeNodeValue && assignedValue.binaryTreeValue != null) {
                                linearRepresentation.add(
                                    TreeNodeRestyle(
                                        assignedValue.manimObject.shape.ident,
                                        assignedValue.binaryTreeValue!!.animatedStyle!!,
                                        assignedValue.binaryTreeValue!!.animatedStyle!!.highlight,
                                        runtime = animationSpeeds.first()
                                    )
                                )
                                linearRepresentation.add(
                                    TreeNodeRestyle(
                                        assignedValue.manimObject.shape.ident,
                                        assignedValue.binaryTreeValue!!.style,
                                        runtime = animationSpeeds.first()
                                    )
                                )
                            }
                            if (localDataStructure != null && node is DeclarationNode && assignedValue.manimObject is DataStructureMObject) {
                                localDataStructure.add(node.identifier.identifier)
                            }
                            variables[node.identifier.identifier] = assignedValue
                        }
                        is ArrayElemNode -> {
                            return executeArrayElemAssignment(this, assignedValue)
                        }
                    }
                }
                if (node.identifier is IdentifierNode) {
                    insertVariable(node.identifier.identifier, assignedValue)
                    updateVariableState()
                }
                EmptyValue
            }
        }

        private fun executeTreeEdit(
            rootNode: BinaryTreeNodeValue,
            binaryTreeElemNode: BinaryTreeNodeElemAccessNode,
            childValue: DoubleValue
        ): ExecValue {
            val (_, node) = executeTreeAccess(rootNode, binaryTreeElemNode)
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
                        runtime = animationSpeeds.first()
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
                                runtime = animationSpeeds.first()
                            )
                        )
                        instructions.add(
                            TreeNodeRestyle(
                                node.manimObject.shape.ident,
                                node.binaryTreeValue!!.style,
                                animationString = node.binaryTreeValue!!.animatedStyle!!.animationStyle,
                                runtime = animationSpeeds.first()
                            )
                        )
                    }
                    linearRepresentation.addAll(instructions)
                }
            }

            if (rootNode.binaryTreeValue == null) {
                insertVariable(binaryTreeElemNode.identifier, rootNode)
            }
            return EmptyValue
        }

        private fun executeTreeAppend(
            rootNode: BinaryTreeNodeValue,
            binaryTreeElemNode: BinaryTreeNodeElemAccessNode,
            childValue: BinaryTreeNodeValue
        ): ExecValue {
            val (parent, _) = executeTreeAccess(rootNode, binaryTreeElemNode)
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
                                runtime = animationSpeeds.first()
                            )
                        )
                    }

                    val boundary =
                        dataStructureBoundaries[(parent.binaryTreeValue!!.manimObject as InitTreeStructure).text]!!
                    boundary.maxSize += nodeCount(childValue)
                    dataStructureBoundaries[(parent.binaryTreeValue!!.manimObject as InitTreeStructure).text] =
                        boundary
                    childValue.attachTree(parent.binaryTreeValue!!)
                    linearRepresentation.add(
                        TreeAppendObject(
                            parent,
                            childValue,
                            parent.binaryTreeValue!!,
                            isLeft,
                            runtime = animationSpeeds.first()
                        )
                    )
                    if (parent.binaryTreeValue!!.animatedStyle != null) {
                        linearRepresentation.add(
                            TreeNodeRestyle(
                                parent.manimObject.shape.ident,
                                parent.binaryTreeValue!!.style,
                                animationString = parent.binaryTreeValue!!.animatedStyle!!.animationStyle,
                                runtime = animationSpeeds.first()
                            )
                        )
                    }
                } else {
                    linearRepresentation.add(
                        NodeAppendObject(
                            parent,
                            childValue,
                            isLeft,
                            runtime = animationSpeeds.first()
                        )
                    )
                }
            }

            if (rootNode.binaryTreeValue == null) {
                insertVariable(binaryTreeElemNode.identifier, rootNode)
            }
            return EmptyValue
        }

        private fun removeNodeFromVariableState(parent: ITreeNodeValue) {
            if (parent is BinaryTreeNodeValue && parent.binaryTreeValue != null) {
                variables.filter { (_, v) -> v == parent }.keys.forEach(this::removeVariable)
                removeNodeFromVariableState(parent.left)
                removeNodeFromVariableState(parent.right)
            }
        }

        private fun updateVariableState() {
            if (showMoveToLine && !hideCode)
                linearRepresentation.add(
                    UpdateVariableState(
                        getVariableState(),
                        "variable_block",
                        runtime = animationSpeeds.first()
                    )
                )
        }

        private fun executeExpression(
            node: ExpressionNode,
            insideMethodCall: Boolean = false,
            identifier: AssignLHS = EmptyLHS,
        ): ExecValue = when (node) {
            is IdentifierNode -> variables[node.identifier]!!
            is NumberNode -> DoubleValue(node.double)
            is CharNode -> CharValue(node.value)
            is MethodCallNode -> executeMethodCall(node, insideMethodCall, true)
            is AddExpression -> executeBinaryOp(node) { x, y -> x + y }
            is SubtractExpression -> executeBinaryOp(node) { x, y -> x - y }
            is DivideExpression -> executeBinaryOp(node) { x, y -> x / y }
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
            is ArrayElemNode -> executeArrayElem(node, identifier)
            is BinaryTreeNodeElemAccessNode -> executeTreeAccess(
                variables[node.identifier]!! as BinaryTreeNodeValue,
                node
            ).second
            is BinaryTreeRootAccessNode -> executeRootAccess(node).second
            is NullNode -> NullValue
            is CastExpressionNode -> executeCastExpression(node)
            is InternalArrayMethodCallNode -> {
                executeInternalArrayMethodCall(node)
            }
        }

        private fun executeRootAccess(binaryTreeRootAccessNode: BinaryTreeRootAccessNode): Pair<ExecValue, ExecValue> {
            val treeNode = variables[binaryTreeRootAccessNode.identifier]!! as BinaryTreeValue
            return executeTreeAccess(treeNode.value, binaryTreeRootAccessNode.elemAccessNode)
        }

        private fun executeTreeDelete(
            rootNode: BinaryTreeNodeValue,
            binaryTreeElemNode: BinaryTreeNodeElemAccessNode
        ): ExecValue {
            val (parent, _) = executeTreeAccess(rootNode, binaryTreeElemNode)
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
                                    runtime = animationSpeeds.first()
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
                                    runtime = animationSpeeds.first()
                                )
                            )
                        }
                    }
                }
            }
            insertVariable(binaryTreeElemNode.identifier, rootNode)
            updateVariableState()
            return EmptyValue
        }

        private fun executeTreeAccess(
            rootNode: BinaryTreeNodeValue,
            elemAccessNode: BinaryTreeNodeElemAccessNode
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
                                runtime = animationSpeeds.first()
                            )
                        )
                        linearRepresentation.add(
                            TreeNodeRestyle(
                                parentValue.manimObject.shape.ident,
                                parentValue.binaryTreeValue!!.style,
                                animationString = parentValue.binaryTreeValue!!.animatedStyle!!.animationStyle,
                                runtime = animationSpeeds.first()
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

        private fun executeCastExpression(node: CastExpressionNode): ExecValue {
            val exprValue = executeExpression(node.expr)

            return when (node.targetType) {
                is CharType -> CharValue((exprValue as DoubleAlias).toDouble().toChar())
                is NumberType -> DoubleValue((exprValue as DoubleAlias).toDouble())
                else -> throw UnsupportedOperationException("Not implemented yet")
            }
        }

        private fun executeArrayElem(node: ArrayElemNode, identifier: AssignLHS): ExecValue {
            return when (val arrayValue = variables[node.identifier]) {
                is ArrayValue -> executeArrayElemSingle(node, arrayValue)
                is Array2DValue -> executeArrayElem2D(node, arrayValue, identifier)
                else -> EmptyValue
            }
        }

        private fun executeArrayElemSingle(node: ArrayElemNode, arrayValue: ArrayValue): ExecValue {
            val index = executeExpression(node.indices.first()) as DoubleValue
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
                                this.pointer,
                                animationString = this.animationStyle,
                                runtime = animationSpeeds.first()
                            )
                        )
                        linearRepresentation.add(
                            ArrayElemRestyle(
                                (arrayValue.manimObject as ArrayStructure).ident,
                                listOf(index.value.toInt()),
                                arrayValue.style,
                                runtime = animationSpeeds.first()
                            )
                        )
                    }
                }
                arrayValue.array[index.value.toInt()]
            }
        }

        private fun executeArrayElem2D(node: ArrayElemNode, arrayValue: Array2DValue, assignLHS: AssignLHS): ExecValue {
            val indices = node.indices.map { executeExpression(it) as DoubleValue }
            return if (indices.size == 2) {
                if (indices.first().value.toInt() !in arrayValue.array.indices || indices[1].value.toInt() !in arrayValue.array[indices.first().value.toInt()].indices) {
                    RuntimeError(value = "Array index out of bounds", lineNumber = node.lineNumber)
                } else {
                    with(arrayValue.animatedStyle) {
                        if (showMoveToLine && this != null) {
                            linearRepresentation.add(
                                ArrayElemRestyle(
                                    (arrayValue.manimObject as Array2DStructure).ident,
                                    listOf(indices[1].value.toInt()),
                                    this,
                                    this.pointer,
                                    animationString = this.animationStyle,
                                    secondIndices = listOf(indices.first().value.toInt()),
                                    runtime = animationSpeeds.first()
                                )
                            )
                            linearRepresentation.add(
                                ArrayElemRestyle(
                                    (arrayValue.manimObject as Array2DStructure).ident,
                                    listOf(indices[1].value.toInt()),
                                    arrayValue.style,
                                    secondIndices = listOf(indices.first().value.toInt()),
                                    runtime = animationSpeeds.first()
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
                    val dsUID = functionNamePrefix + assignLHS.identifier
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

        private fun executeMethodCall(
            node: MethodCallNode,
            insideMethodCall: Boolean,
            isExpression: Boolean
        ): ExecValue {
            return when (val ds = variables[node.instanceIdentifier]) {
                is StackValue -> {
                    return executeStackMethodCall(node, ds, insideMethodCall, isExpression)
                }
                is ArrayValue -> {
                    return executeArrayMethodCall(node, ds)
                }
                is Array2DValue -> {
                    return execute2DArrayMethodCall(node, ds)
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
                                variableNameGenerator.generateNameFromPrefix("animations"),
                                runtime = ds.animatedStyle?.animationTime ?: animationSpeeds.first()
                            )
                        } else {
                            ArrayShortSwap(
                                arrayIdent,
                                Pair(index1, index2),
                                runtime = ds.animatedStyle?.animationTime ?: animationSpeeds.first()
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
                                    runtime = animationSpeeds.first()
                                )
                            )
                            swap.add(
                                ArrayElemRestyle(
                                    arrayIdent,
                                    listOf(index1, index2),
                                    ds.style,
                                    runtime = animationSpeeds.first()
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

        private fun executeInternalArrayMethodCall(node: InternalArrayMethodCallNode): ExecValue {
            val ds = variables[node.instanceIdentifier] as Array2DValue
            val index = (executeExpression(node.index) as DoubleValue).value.toInt()
            return when (node.dataStructureMethod) {
                is ArrayType.Size -> DoubleValue(ds.array[index].size.toDouble())
                is ArrayType.Swap -> {
                    val fromToIndices = node.arguments.map { (executeExpression(it) as DoubleValue).value.toInt() }
                    array2dSwap(ds, listOf(index, fromToIndices[0], index, fromToIndices[1]))
                }
                else -> EmptyValue
            }
        }

        private fun execute2DArrayMethodCall(node: MethodCallNode, ds: Array2DValue): ExecValue {
            return when (node.dataStructureMethod) {
                is ArrayType.Size -> {
                    DoubleValue(ds.array.size.toDouble())
                }
                is ArrayType.Swap -> {
                    val indices = node.arguments.map { (executeExpression(it) as DoubleValue).value.toInt() }
                    array2dSwap(ds, indices)
                }
                else -> EmptyValue
            }
        }

        private fun array2dSwap(ds: Array2DValue, indices: List<Int>): EmptyValue {
            val arrayIdent = (ds.manimObject as Array2DStructure).ident
            val arraySwap =
                Array2DSwap(arrayIdent, indices, runtime = ds.animatedStyle?.animationTime ?: animationSpeeds.first())
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
                            runtime = animationSpeeds.first()
                        )
                    )
                    swap.add(
                        ArrayElemRestyle(
                            arrayIdent,
                            listOf(indices[1], indices[3]),
                            ds.style,
                            secondIndices = listOf(indices[0], indices[2]),
                            runtime = animationSpeeds.first()
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

        private fun executeStackMethodCall(
            node: MethodCallNode,
            ds: StackValue,
            insideMethodCall: Boolean,
            isExpression: Boolean
        ): ExecValue {
            return when (node.dataStructureMethod) {
                is StackType.PushMethod -> {
                    val value = executeExpression(node.arguments[0], true)
                    if (value is RuntimeError) {
                        return value
                    }
                    val dataStructureIdentifier = (ds.manimObject as InitManimStack).ident
                    val dsUID = (ds.manimObject as InitManimStack).uid
                    val boundaryShape = dataStructureBoundaries[dsUID]!!
                    boundaryShape.maxSize++
                    dataStructureBoundaries[dsUID] = boundaryShape
                    val hasOldMObject = value.manimObject !is EmptyMObject
                    val oldMObject = value.manimObject
                    val newObjectStyle = ds.animatedStyle ?: ds.style
                    val rectangle = if (hasOldMObject) oldMObject else NewMObject(
                        Rectangle(
                            variableNameGenerator.generateNameFromPrefix("rectangle"),
                            value.toString(),
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
                                hasOldMObject,
                                creationStyle = ds.style.creationStyle,
                                runtime = ds.animatedStyle?.animationTime ?: animationSpeeds.first()
                            ),
                            RestyleObject(
                                rectangle.shape,
                                ds.style,
                                ds.animatedStyle?.animationTime ?: animationSpeeds.first()
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
                            insideMethodCall,
                            runtime = ds.animatedStyle?.animationTime ?: animationSpeeds.first()
                        )
                    )
                    ds.animatedStyle?.let {
                        instructions.add(
                            0,
                            RestyleObject(topOfStack.shape, it, it.animationTime ?: animationSpeeds.first())
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
            val dsUID = functionNamePrefix + assignLHS.identifier
            return when (node.type) {
                is StackType -> {
                    val stackValue = StackValue(EmptyMObject, Stack())
                    val initStructureIdent = variableNameGenerator.generateNameFromPrefix("stack")
                    stackValue.style = stylesheet.getStyle(assignLHS.identifier, stackValue)
                    stackValue.animatedStyle = stylesheet.getAnimatedStyle(assignLHS.identifier, stackValue)
                    val position = stylesheet.getPosition(dsUID)
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
                            uid = dsUID
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
                            creationStyle = stackValue.style.creationStyle,
                            creationTime = stackValue.style.creationTime,
                            showLabel = stackValue.style.showLabel,
                            boundaries = boundaries,
                            uid = dsUID
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
                    initialiserNodeExpressions.map { executeExpression(it) }.forEach {
                        val rectangle = Rectangle(
                            variableNameGenerator.generateNameFromPrefix("rectangle"),
                            it.toString(),
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
                        linearRepresentation.add(
                            StackPushObject(
                                rectangle,
                                initStructureIdent,
                                runtime = newObjectStyle.animate?.animationTime ?: animationSpeeds.first()
                            )
                        )
                    }
                    stackValue.manimObject = newObject
                    stackValue
                }
                is ArrayType -> {
                    val is2DArray = node.arguments.size == 2
                    return if (is2DArray) {
                        execute2DArrayConstructor(node, assignLHS)
                    } else {
                        execute1DArrayConstructor(node, assignLHS)
                    }
                }
                is TreeType -> {
                    val ident = variableNameGenerator.generateNameFromPrefix("tree")
                    val root = executeExpression(node.arguments.first()) as BinaryTreeNodeValue
                    val position = stylesheet.getPosition(dsUID)
                    dataStructureBoundaries[dsUID] = SquareBoundary(maxSize = 1)
                    if (stylesheet.userDefinedPositions() && position == null) {
                        return RuntimeError("Missing position values for $dsUID", lineNumber = node.lineNumber)
                    }
                    val boundaries = getBoundaries(position)
                    val initTreeStructure = InitTreeStructure(
                        node.type,
                        ident,
                        text = assignLHS.identifier,
                        root = root,
                        boundaries = boundaries,
                        uid = dsUID,
                        runtime = animationSpeeds.first()
                    )
                    linearRepresentation.add(initTreeStructure)
                    val binaryTreeValue = BinaryTreeValue(manimObject = initTreeStructure, value = root)
                    binaryTreeValue.style = stylesheet.getStyle(assignLHS.identifier, binaryTreeValue)
                    binaryTreeValue.animatedStyle = stylesheet.getAnimatedStyle(assignLHS.identifier, binaryTreeValue)
                    root.attachTree(binaryTreeValue)
                    // Remove any variables pointing to node from variable block as it now belongs to a tree
                    removeNodeFromVariableState(root)
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
                    return BinaryTreeNodeValue(NullValue, NullValue, value, manimObject = nodeStructure, depth = 0)
                }
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
                if (node.arguments.isNotEmpty()) executeExpression(node.arguments[0]) as DoubleValue else DoubleValue(
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
                    ArrayValue(EmptyMObject, initialiserExpressions.map { executeExpression(it) }.toTypedArray())
                }
            }
            if (assignLHS !is ArrayElemNode) {
                val ident = variableNameGenerator.generateNameFromPrefix("array")
                if (arrayValue is ArrayValue) {
                    arrayValue.style = stylesheet.getStyle(assignLHS.identifier, arrayValue)
                    arrayValue.animatedStyle = stylesheet.getAnimatedStyle(assignLHS.identifier, arrayValue)
                    val dsUID = functionNamePrefix + assignLHS.identifier
                    val position = stylesheet.getPosition(dsUID)
                    dataStructureBoundaries[dsUID] = WideBoundary(maxSize = arraySize.value.toInt())
                    if (stylesheet.userDefinedPositions() && position == null) {
                        return RuntimeError("Missing position values for $dsUID", lineNumber = node.lineNumber)
                    }
                    val boundaries = getBoundaries(position)
                    val arrayStructure = ArrayStructure(
                        node.type,
                        ident,
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
            val arraySizes = node.arguments.map { executeExpression(it) as DoubleValue }
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
                        Array(arrayDimensions.second) { _ -> getDefaultValueForType(node.type.internalType, node.lineNumber) }
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
                            exprList.map { executeExpression(it) }.toTypedArray()
                        }.toTypedArray()
                    )
                }
            }

            if (arrayValue is Array2DValue) {
                val ident = variableNameGenerator.generateNameFromPrefix("array")
                val dsUID = functionNamePrefix + assignLHS.identifier
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
            if (showMoveToLine && !hideCode) addSleep(0.5)

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

                val localDataStructure = mutableSetOf<String>()

                execValue = Frame(
                    whileStatementNode.statements.first().lineNumber,
                    whileStatementNode.statements.last().lineNumber,
                    variables,
                    depth,
                    showMoveToLine = showMoveToLine,
                    stepInto = stepInto,
                    hideCode = hideCode,
                    localDataStructure = localDataStructure
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

                if (localDataStructure.isNotEmpty()) {
                    linearRepresentation.add(CleanUpLocalDataStructures(localDataStructure, animationSpeeds.first()))
                }
                pc = whileStatementNode.lineNumber
                moveToLine()
                loopCount++
            }

            return RuntimeError("Max number of loop executions exceeded", lineNumber = whileStatementNode.lineNumber)
        }

        private fun removeForLoopCounter(forStatementNode: ForStatementNode) {
            val identifier = forStatementNode.beginStatement.identifier.identifier
            val index = displayedDataMap.filterValues { it.first == identifier }.keys
            displayedDataMap.remove(index.first())
            variables.remove(identifier)
        }

        private fun executeForStatement(forStatementNode: ForStatementNode): ExecValue {

            var conditionValue: ExecValue
            var execValue: ExecValue
            var loopCount = 0

            executeAssignment(forStatementNode.beginStatement)

            val start = executeExpression(forStatementNode.beginStatement.expression) as DoubleAlias
            val end = executeExpression(forStatementNode.endCondition) as DoubleAlias
            val lineNumber = forStatementNode.lineNumber

            val condition = if (start < end) {
                LtExpression(
                    lineNumber,
                    IdentifierNode(lineNumber, forStatementNode.beginStatement.identifier.identifier),
                    NumberNode(lineNumber, end.toDouble())
                )
            } else {
                GtExpression(
                    lineNumber,
                    IdentifierNode(lineNumber, forStatementNode.beginStatement.identifier.identifier),
                    NumberNode(lineNumber, end.toDouble())
                )
            }

            while (loopCount < MAX_NUMBER_OF_LOOPS) {
                conditionValue = executeExpression(condition)
                if (conditionValue is RuntimeError) {
                    return conditionValue
                } else if (conditionValue is BoolValue) {
                    if (!conditionValue.value) {
                        removeForLoopCounter(forStatementNode)
                        pc = forStatementNode.endLineNumber
                        moveToLine()
                        return EmptyValue
                    } else {
                        pc = forStatementNode.lineNumber
                    }
                }

                val localDataStructure = mutableSetOf<String>()

                execValue = Frame(
                    forStatementNode.statements.first().lineNumber,
                    forStatementNode.statements.last().lineNumber,
                    variables,
                    depth,
                    showMoveToLine = showMoveToLine,
                    stepInto = stepInto,
                    hideCode = hideCode,
                    displayedDataMap = displayedDataMap,
                    localDataStructure = localDataStructure
                ).runFrame()

                when (execValue) {
                    is BreakValue -> {
                        removeForLoopCounter(forStatementNode)
                        pc = forStatementNode.endLineNumber
                        moveToLine()
                        return EmptyValue
                    }
                    is ContinueValue -> {
                        executeAssignment(forStatementNode.updateCounter)
                        pc = forStatementNode.lineNumber
                        moveToLine()
                        continue
                    }
                    !is EmptyValue -> {
                        return execValue
                    }
                }

                if (showMoveToLine && !hideCode) addSleep(0.5)
                executeAssignment(forStatementNode.updateCounter)
                if (localDataStructure.isNotEmpty()) {
                    linearRepresentation.add(CleanUpLocalDataStructures(localDataStructure, animationSpeeds.first()))
                }
                pc = forStatementNode.lineNumber
                moveToLine()
                loopCount++
            }

            return RuntimeError("Max number of loop executions exceeded", lineNumber = forStatementNode.lineNumber)
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
            val localDataStructure = mutableSetOf<String>()

            // If
            if (conditionValue.value) {
                val execValue = Frame(
                    ifStatementNode.statements.first().lineNumber,
                    ifStatementNode.statements.last().lineNumber,
                    variables,
                    depth,
                    showMoveToLine = showMoveToLine,
                    stepInto = stepInto,
                    updateVariableState = updateVariableState,
                    hideCode = hideCode,
                    localDataStructure = localDataStructure
                ).runFrame()
                if (execValue is EmptyValue) {
                    pc = ifStatementNode.endLineNumber
                }
                if (localDataStructure.isNotEmpty()) {
                    linearRepresentation.add(CleanUpLocalDataStructures(localDataStructure, animationSpeeds.first()))
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
                        hideCode = hideCode,
                        localDataStructure = localDataStructure
                    ).runFrame()
                    if (execValue is EmptyValue) {
                        pc = ifStatementNode.endLineNumber
                    }
                    if (localDataStructure.isNotEmpty()) {
                        linearRepresentation.add(CleanUpLocalDataStructures(localDataStructure, animationSpeeds.first()))
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
                    hideCode = hideCode,
                    localDataStructure = localDataStructure
                ).runFrame()
                if (execValue is EmptyValue) {
                    pc = ifStatementNode.endLineNumber
                }
                if (localDataStructure.isNotEmpty()) {
                    linearRepresentation.add(CleanUpLocalDataStructures(localDataStructure, animationSpeeds.first()))
                }
                return execValue
            }
            return EmptyValue
        }
    }
}
