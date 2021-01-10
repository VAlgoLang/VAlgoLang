package com.manimdsl.runtime

import com.google.gson.Gson
import com.manimdsl.ExitStatus
import com.manimdsl.errorhandling.ErrorHandler.addRuntimeError
import com.manimdsl.frontend.*
import com.manimdsl.linearrepresentation.*
import com.manimdsl.linearrepresentation.datastructures.binarytree.TreeNodeRestyle
import com.manimdsl.runtime.datastructures.BoundaryShape
import com.manimdsl.runtime.datastructures.Scene
import com.manimdsl.runtime.datastructures.WideBoundary
import com.manimdsl.runtime.datastructures.array.Array2DValue
import com.manimdsl.runtime.datastructures.array.ArrayExecutor
import com.manimdsl.runtime.datastructures.array.ArrayValue
import com.manimdsl.runtime.datastructures.binarytree.BinaryTreeExecutor
import com.manimdsl.runtime.datastructures.binarytree.BinaryTreeNodeValue
import com.manimdsl.runtime.datastructures.binarytree.BinaryTreeValue
import com.manimdsl.runtime.datastructures.binarytree.NullValue
import com.manimdsl.runtime.datastructures.makeConstructorNode
import com.manimdsl.runtime.datastructures.stack.StackExecutor
import com.manimdsl.runtime.datastructures.stack.StackValue
import com.manimdsl.runtime.utility.convertToIdent
import com.manimdsl.runtime.utility.getBoundaries
import com.manimdsl.runtime.utility.wrapCode
import com.manimdsl.runtime.utility.wrapString
import com.manimdsl.stylesheet.PositionProperties
import com.manimdsl.stylesheet.Stylesheet
import java.util.*

/**
 * Virtual Machine
 *
 * @property program: Root node of abstract syntax tree
 * @property symbolTableVisitor: Symbol table visitor built during parsing
 * @property statements: Line to node map mapping unique line numbers to StatementNodes in the abstract syntax tree.
 *                       Constructed during parsing
 * @property fileLines: Array of source code lines.
 * @property stylesheet: Stylesheet object with animation properties.
 * @property returnBoundaries: Optional CLI argument for whether to return the boundaries of the shapes. Used in Web UI.
 * @constructor Creates a new virtual machine
 *
 */

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
    private var subtitleBlockVariable: MObject = EmptyMObject
    private val pointerVariable: String = variableNameGenerator.generateNameFromPrefix("pointer")
    private val displayLine: MutableList<Int> = mutableListOf()
    private val displayCode: MutableList<String> = mutableListOf()
    private val dataStructureBoundaries = mutableMapOf<String, BoundaryShape>()
    private var acceptableNonStatements = setOf("}", "{")
    private val MAX_DISPLAYED_VARIABLES = 4
    private val ALLOCATED_STACKS = Runtime.getRuntime().freeMemory() / 1000000
    private val STEP_INTO_DEFAULT = stylesheet.getStepIntoIsDefault()
    private val MAX_NUMBER_OF_LOOPS = 10000
    private val SUBTITLE_DEFAULT_DURATION = 5
    private val hideCode = stylesheet.getHideCode()
    private val hideVariables = stylesheet.getHideVariables()
    private var animationSpeeds = ArrayDeque(listOf(1.0))

    init {
        setupFileLines()
    }

    fun runProgram(): Pair<ExitStatus, List<ManimInstr>> {
        if (!hideCode) {
            if (!hideVariables) {
                linearRepresentation.add(
                    VariableBlock(
                        listOf(),
                        "variable_block",
                        "variable_vg",
                        runtime = animationSpeeds.first()
                    )
                )
            }
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
            updateVariableState = !(hideCode || hideVariables)
        ).runFrame()
        linearRepresentation.add(Sleep(1.0, runtime = animationSpeeds.first()))
        return if (result is RuntimeError) {
            addRuntimeError(result.value, result.lineNumber)
            Pair(ExitStatus.RUNTIME_ERROR, linearRepresentation)
        } else if (returnBoundaries || !stylesheet.userDefinedPositions()) {
            val (exitStatus, computedBoundaries) = Scene().compute(
                dataStructureBoundaries.toList(),
                hideCode,
                hideVariables
            )
            if (returnBoundaries) {
                val boundaries = mutableMapOf<String, Map<String, PositionProperties>>()
                val genericShapeIDs = mutableSetOf<String>()
                if (!stylesheet.getHideCode()) {
                    genericShapeIDs.add("_code")
                    if (!stylesheet.getHideVariables()) genericShapeIDs.add("_variables")
                }
                boundaries["auto"] = computedBoundaries.mapValues { it.value.positioning() }
                boundaries["stylesheet"] = stylesheet.getPositions()
                    .filter { it.key in dataStructureBoundaries.keys || genericShapeIDs.contains(it.key) }
                val gson = Gson()
                println(gson.toJson(boundaries))
            }
            if (exitStatus != ExitStatus.EXIT_SUCCESS) {
                return Pair(exitStatus, linearRepresentation)
            }
            val linearRepresentationWithBoundaries = linearRepresentation.map {
                if (it is ManimInstrWithBoundary) {
                    val boundaryShape = computedBoundaries[it.uid]!!
                    it.setNewBoundary(boundaryShape.corners(), boundaryShape.maxSize)
                }
                it
            }
            Pair(ExitStatus.EXIT_SUCCESS, linearRepresentationWithBoundaries)
        } else {
            linearRepresentation.forEach {
                if (it is ShapeWithBoundary) {
                    if (it is CodeBlock || it is VariableBlock || it is SubtitleBlock) {
                        val position = stylesheet.getPosition(it.uid)
                        if (position == null) {
                            addRuntimeError("Missing positional parameter for ${it.uid}", 1)
                            return Pair(ExitStatus.RUNTIME_ERROR, linearRepresentation)
                        }
                        it.setNewBoundary(position.calculateManimCoord(), -1)
                    }
                }
            }
            Pair(ExitStatus.EXIT_SUCCESS, linearRepresentation)
        }
    }

    private fun setupFileLines() {
        if (stylesheet.getDisplayNewLinesInCode()) {
            acceptableNonStatements = acceptableNonStatements.plus("")
        }
        fileLines.indices.forEach {
            if (statements[it + 1] !is NoRenderAnimationNode &&
                (acceptableNonStatements.any { x -> fileLines[it].contains(x) } || statements[it + 1] is CodeNode)
            ) {
                if (fileLines[it].isEmpty()) {
                    if (stylesheet.getDisplayNewLinesInCode()) {
                        if (stylesheet.getSyntaxHighlighting()) {
                            displayCode.add(" ")
                        } else {
                            displayCode.add("")
                        }
                        displayLine.add(1 + (displayLine.lastOrNull() ?: 0))
                    }
                } else {
                    displayCode.add(
                        fileLines[it].replace("\'", "\\'").replace("\"", "\\\"")
                    ) // Escape chars to be compatible with python strings
                    displayLine.add(1 + (displayLine.lastOrNull() ?: 0))
                }
            } else {
                displayLine.add(displayLine.lastOrNull() ?: 0)
            }
        }
    }

    /**
     * Frame
     *
     * @property pc: Start line (program counter) for frame execution.
     * @property finalLine: Last line (inclusive) for frame execution.
     * @property variables: Map from identifier of a variable in frame to its current execution value.
     * @property depth: Number of frames top level scope.
     * @property showMoveToLine: Whether to visualise code stepping for the code executed in this frame.
     * @property stepInto: Whether to step into (rather than over) this frame in the code stepping visualisation.
     * @property mostRecentlyUpdatedQueue: Queue maintaining most recently updated variables.
     * @property displayedDataMap: Map maintaining the execution value and variable being displayed in variable block.
     * @property updateVariableState: Whether to not hide variable block.
     * @property hideCode: Whether to hide code block.
     * @property functionNamePrefix: Function name for stylesheet styling assignment disambiguation.
     * @property localDataStructures: Set of local data structures.
     * @constructor Creates a new execution frame.
     *
     */

    inner class Frame(
        private var pc: Int,
        private var finalLine: Int,
        private var variables: MutableMap<String, ExecValue>,
        private val depth: Int = 1,
        private var showMoveToLine: Boolean = true,
        private var stepInto: Boolean = STEP_INTO_DEFAULT,
        private var mostRecentlyUpdatedQueue: LinkedList<Int> = LinkedList(),
        private var displayedDataMap: MutableMap<Int, Pair<String, ExecValue>> = mutableMapOf(),
        private val updateVariableState: Boolean = true,
        private val hideCode: Boolean = false,
        val functionNamePrefix: String = "",
        private val localDataStructures: MutableSet<String> = mutableSetOf()
    ) {
        private var previousStepIntoState = stepInto

        /** Data Structure Executors **/
        private val btExecutor = BinaryTreeExecutor(
            variables,
            linearRepresentation,
            this,
            stylesheet,
            animationSpeeds,
            dataStructureBoundaries,
            variableNameGenerator,
            codeTextVariable,
            localDataStructures
        )
        private val arrExecutor = ArrayExecutor(
            variables,
            linearRepresentation,
            this,
            stylesheet,
            animationSpeeds,
            dataStructureBoundaries,
            variableNameGenerator,
            codeTextVariable,
            localDataStructures
        )
        private val stackExecutor = StackExecutor(
            variables,
            linearRepresentation,
            this,
            stylesheet,
            animationSpeeds,
            dataStructureBoundaries,
            variableNameGenerator,
            codeTextVariable,
            localDataStructures
        )

        /** FRAME UTILITIES **/
        fun getShowMoveToLine() = showMoveToLine

        fun getPc() = pc

        fun insertVariable(identifier: String, value: ExecValue) {
            if (shouldRenderInVariableState(value, functionNamePrefix + identifier)) {
                val index = displayedDataMap.filterValues { it.first == identifier }.keys
                if (index.isEmpty()) {
                    // not been visualised
                    // if there is space
                    if (displayedDataMap.size < MAX_DISPLAYED_VARIABLES) {
                        val newIndex = displayedDataMap.size
                        mostRecentlyUpdatedQueue.addLast(newIndex)
                        displayedDataMap[newIndex] = Pair(identifier, value)
                    } else {
                        // if there is no space
                        val oldest = mostRecentlyUpdatedQueue.removeFirst()
                        displayedDataMap[oldest] = Pair(identifier, value)
                        mostRecentlyUpdatedQueue.addLast(oldest)
                    }
                } else {
                    // being visualised
                    mostRecentlyUpdatedQueue.remove(index.first())
                    mostRecentlyUpdatedQueue.addLast(index.first())
                    displayedDataMap[index.first()] = Pair(identifier, value)
                }
            }
        }

        private fun shouldRenderInVariableState(value: ExecValue, identifier: String) =
            (value is BinaryTreeNodeValue && value.binaryTreeValue == null) ||
                value is PrimitiveValue ||
                (value is BinaryTreeValue && !stylesheet.renderDataStructure(identifier)) ||
                (value is ArrayValue && !stylesheet.renderDataStructure(identifier)) ||
                (value is StackValue && !stylesheet.renderDataStructure(identifier))

        fun removeVariable(identifier: String) {
            displayedDataMap = displayedDataMap.filter { (_, v) -> v.first != identifier }.toMutableMap()
        }

        private fun addSleep(length: Double) {
            linearRepresentation.add(Sleep(length, runtime = animationSpeeds.first()))
        }

        private fun moveToLine(line: Int = pc) {
            if (showMoveToLine && !hideCode && fileLines[line - 1].isNotEmpty()) {
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

        private fun fetchNextStatement() {
            ++pc
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
                    if (value is RuntimeError) {
                        return value
                    }
                    if (statement is ReturnNode || value !is EmptyValue) {
                        if (statement is ReturnNode && statement.expression is IdentifierNode && value !is PrimitiveValue) {
                            // Return variable data structure
                            localDataStructures.remove(functionNamePrefix + statement.expression.identifier)
                        }
                        if (localDataStructures.isNotEmpty() && value !is RuntimeError) {
                            linearRepresentation.add(
                                CleanUpLocalDataStructures(
                                    convertToIdent(
                                        localDataStructures,
                                        variables
                                    ),
                                    animationSpeeds.first()
                                )
                            )
                        }
                        return value
                    }
                }

                fetchNextStatement()
            }

            if (localDataStructures.isNotEmpty() && depth != 1) {
                linearRepresentation.add(
                    CleanUpLocalDataStructures(
                        convertToIdent(localDataStructures, variables),
                        animationSpeeds.first()
                    )
                )
            }
            return EmptyValue
        }

        private fun getVariableState(): List<String> {
            return displayedDataMap.toSortedMap().map { wrapString("${it.value.first} = ${it.value.second}") }
        }

        fun updateVariableState() {
            if (showMoveToLine && !hideCode && !hideVariables)
                linearRepresentation.add(
                    UpdateVariableState(
                        getVariableState(),
                        "variable_block",
                        runtime = animationSpeeds.first()
                    )
                )
        }

        /** STATEMENTS **/

        private fun executeStatement(statement: StatementNode): ExecValue = when (statement) {
            is ReturnNode -> executeExpression(statement.expression)
            is FunctionNode -> {
                // just go onto next line, this is just a label
                EmptyValue
            }
            is SleepNode -> {
                addSleep((executeExpression(statement.sleepTime) as DoubleValue).value)
                EmptyValue
            }
            is AssignmentNode -> executeAssignment(statement)
            is DeclarationNode -> executeAssignment(statement)
            is MethodCallNode -> executeMethodCall(statement, insideMethodCall = false, isExpression = false)
            is FunctionCallNode -> executeFunctionCall(statement)
            is IfStatementNode -> executeIfStatement(statement)
            is WhileStatementNode -> executeWhileStatement(statement)
            is ForStatementNode -> executeForStatement(statement)
            is LoopStatementNode -> executeLoopStatement(statement)
            is InternalArrayMethodCallNode -> arrExecutor.executeInternalArrayMethodCall(statement)
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
            is SubtitleAnnotationNode -> {
                val condition = executeExpression(statement.condition) as BoolValue
                if (condition.value) {
                    if (statement.showOnce) statement.condition = BoolNode(statement.lineNumber, false)

                    val duration: Double = if (statement.duration != null) {
                        (executeExpression(statement.duration) as DoubleValue).value
                    } else {
                        (stylesheet.getSubtitleStyle().duration ?: SUBTITLE_DEFAULT_DURATION) * animationSpeeds.first()
                    }

                    val text = executeExpression(statement.text, subtitleExpression = true) as StringValue
                    updateSubtitle(text.value, duration)
                    EmptyValue
                } else {
                    EmptyValue
                }
            }
            else -> EmptyValue
        }

        private fun updateSubtitle(text: String, duration: Double) {
            if (subtitleBlockVariable is EmptyMObject) {
                val dsUID = "_subtitle"
                dataStructureBoundaries[dsUID] = WideBoundary(maxSize = Int.MAX_VALUE)
                val position = stylesheet.getPosition(dsUID)
                val boundaries = if (position == null) emptyList() else getBoundaries(position)
                subtitleBlockVariable = SubtitleBlock(
                    variableNameGenerator,
                    runtime = animationSpeeds.first(),
                    boundary = boundaries,
                    textColor = stylesheet.getSubtitleStyle().textColor,
                    duration = duration
                )
                linearRepresentation.add(subtitleBlockVariable)
            }
            linearRepresentation.add(
                UpdateSubtitle(
                    (subtitleBlockVariable as SubtitleBlock),
                    wrapString(text, 65),
                    runtime = animationSpeeds.first()
                )
            )
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
                stepInto = stepInto && previousStepIntoState, // In the case of nested stepInto/stepOver
                updateVariableState = updateVariableState,
                hideCode = hideCode,
                functionNamePrefix = "${functionNode.identifier}.",
            ).runFrame()

            // to visualise popping back to assignment we can move pointer to the prior statement again
            if (stepInto) moveToLine()
            return returnValue
        }

        private fun executeAssignment(node: DeclarationOrAssignment): ExecValue {
            if (node.identifier is IdentifierNode && variables.containsKey(node.identifier.identifier)) {
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
                        is BinaryTreeNodeAccess -> return btExecutor.executeTreeAssignment(this, assignedValue)
                        is ArrayElemNode -> return arrExecutor.executeArrayElemAssignment(this, assignedValue)
                        is IdentifierNode -> {
                            if (assignedValue is BinaryTreeNodeValue && assignedValue.binaryTreeValue != null) {
                                linearRepresentation.add(
                                    TreeNodeRestyle(
                                        assignedValue.manimObject.ident,
                                        assignedValue.binaryTreeValue!!.animatedStyle!!,
                                        assignedValue.binaryTreeValue!!.animatedStyle!!.highlight,
                                        runtime = animationSpeeds.first(),
                                        render = stylesheet.renderDataStructure(functionNamePrefix + node.identifier)
                                    )
                                )
                                linearRepresentation.add(
                                    TreeNodeRestyle(
                                        assignedValue.manimObject.ident,
                                        assignedValue.binaryTreeValue!!.style,
                                        runtime = animationSpeeds.first(),
                                        render = stylesheet.renderDataStructure(functionNamePrefix + node.identifier)
                                    )
                                )
                            }

                            if (stepInto && node.expression is FunctionCallNode && assignedValue.manimObject is DataStructureMObject && (functionNamePrefix == "" || (node.expression as FunctionCallNode).functionIdentifier != functionNamePrefix.substringBefore('.'))) {
                                // Non recursive function call
                                linearRepresentation.add(
                                    CleanUpLocalDataStructures(
                                        setOf(assignedValue.manimObject.ident),
                                        animationSpeeds.first()
                                    )
                                )
                                val constructor = makeConstructorNode(assignedValue, node.lineNumber)
                                variables[node.identifier.identifier] = executeConstructor(constructor, node.identifier)
                            } else {
                                // Recursive call and regular assignment
                                variables[node.identifier.identifier] = assignedValue
                            }

                            insertVariable(node.identifier.identifier, assignedValue)
                            updateVariableState()
                        }
                    }
                }
                EmptyValue
            }
        }

        private fun executeWhileStatement(whileStatementNode: WhileStatementNode): ExecValue {
            if (showMoveToLine && !hideCode) addSleep(animationSpeeds.first() * 0.5)
            return executeLoopBranchingStatements(whileStatementNode, whileStatementNode.condition)
        }

        private fun executeForStatement(forStatementNode: ForStatementNode): ExecValue {
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

            return executeLoopBranchingStatements(forStatementNode, condition)
        }

        private fun removeForLoopCounter(forStatementNode: ForStatementNode) {
            val identifier = forStatementNode.beginStatement.identifier.identifier
            val index = displayedDataMap.filterValues { it.first == identifier }.keys
            displayedDataMap.remove(index.first())
            variables.remove(identifier)
        }

        private fun executeLoopBranchingStatements(loopNode: LoopNode, condition: ExpressionNode): ExecValue {
            var conditionValue: ExecValue
            var execValue: ExecValue
            var loopCount = 0
            val prevShowMoveToLine = showMoveToLine

            while (loopCount < MAX_NUMBER_OF_LOOPS) {
                conditionValue = executeExpression(condition)
                if (conditionValue is RuntimeError) {
                    return conditionValue
                } else if (conditionValue is BoolValue) {
                    if (!conditionValue.value) {
                        showMoveToLine = prevShowMoveToLine
                        if (loopNode is ForStatementNode) removeForLoopCounter(loopNode)
                        pc = loopNode.endLineNumber
                        moveToLine()
                        return EmptyValue
                    } else {
                        showMoveToLine = stepInto
                        pc = loopNode.lineNumber
                    }
                }

                execValue = Frame(
                    loopNode.statements.first().lineNumber,
                    loopNode.statements.last().lineNumber,
                    variables,
                    depth,
                    showMoveToLine = stepInto,
                    stepInto = stepInto && previousStepIntoState,
                    hideCode = hideCode,
                    mostRecentlyUpdatedQueue = if (loopNode is ForStatementNode) mostRecentlyUpdatedQueue else LinkedList(),
                    displayedDataMap = if (loopNode is ForStatementNode) displayedDataMap else mutableMapOf(),
                    functionNamePrefix = functionNamePrefix
                ).runFrame()

                when (execValue) {
                    is BreakValue -> {
                        showMoveToLine = prevShowMoveToLine
                        if (loopNode is ForStatementNode) removeForLoopCounter(loopNode)
                        pc = loopNode.endLineNumber
                        moveToLine()
                        return EmptyValue
                    }
                    is ContinueValue -> {
                        if (loopNode is ForStatementNode) executeAssignment(loopNode.updateCounter)
                        pc = loopNode.lineNumber
                        moveToLine()
                        continue
                    }
                    !is EmptyValue -> {
                        return execValue
                    }
                }

                if (showMoveToLine && !hideCode && loopNode is ForStatementNode) addSleep(animationSpeeds.first() * 0.5)

                if (loopNode is ForStatementNode) executeAssignment(loopNode.updateCounter)
                pc = loopNode.lineNumber
                moveToLine()
                loopCount++
            }

            return RuntimeError("Max number of loop executions exceeded", lineNumber = loopNode.lineNumber)
        }

        private fun executeIfStatement(ifStatementNode: IfStatementNode): ExecValue {
            if (showMoveToLine && !hideCode) addSleep(animationSpeeds.first() * 0.5)
            var conditionValue = executeExpression(ifStatementNode.condition)
            if (conditionValue is RuntimeError) {
                return conditionValue
            } else {
                conditionValue = conditionValue as BoolValue
            }
            // Set pc to end of if statement as branching is handled here
            pc = ifStatementNode.endLineNumber

            // If
            if (conditionValue.value) {
                return executeIfBranchingStatements(ifStatementNode.statements, ifStatementNode.endLineNumber)
            }

            // Elif
            for (elif in ifStatementNode.elifs) {
                moveToLine(elif.lineNumber)
                if (showMoveToLine && !hideCode) addSleep(animationSpeeds.first() * 0.5)
                // Add statement to code
                conditionValue = executeExpression(elif.condition) as BoolValue
                if (conditionValue.value) {
                    return executeIfBranchingStatements(elif.statements, ifStatementNode.endLineNumber)
                }
            }

            // Else
            if (ifStatementNode.elseBlock.statements.isNotEmpty()) {
                moveToLine(ifStatementNode.elseBlock.lineNumber)
                if (showMoveToLine && !hideCode) addSleep(animationSpeeds.first() * 0.5)
                return executeIfBranchingStatements(ifStatementNode.elseBlock.statements, ifStatementNode.endLineNumber)
            }
            return EmptyValue
        }

        private fun executeIfBranchingStatements(statements: List<StatementNode>, endLineNumber: Int): ExecValue {
            val execValue = Frame(
                statements.first().lineNumber,
                statements.last().lineNumber,
                variables,
                depth,
                showMoveToLine = showMoveToLine,
                stepInto = stepInto,
                updateVariableState = updateVariableState,
                hideCode = hideCode,
                functionNamePrefix = functionNamePrefix
            ).runFrame()

            if (execValue is EmptyValue) {
                pc = endLineNumber
            }

            return execValue
        }

        /** EXPRESSIONS **/

        fun executeExpression(
            node: ExpressionNode,
            insideMethodCall: Boolean = false,
            identifier: AssignLHS = EmptyLHS,
            subtitleExpression: Boolean = false
        ): ExecValue = when (node) {
            is IdentifierNode -> variables[node.identifier]!!
            is NumberNode -> DoubleValue(node.double)
            is CharNode -> CharValue(node.value)
            is MethodCallNode -> executeMethodCall(node, insideMethodCall, true)
            is AddExpression -> executeBinaryOp(node, subtitleExpression) { x, y -> x + y }
            is SubtractExpression -> executeBinaryOp(node, subtitleExpression) { x, y -> x - y }
            is DivideExpression -> executeBinaryOp(node, subtitleExpression) { x, y -> x / y }
            is MultiplyExpression -> executeBinaryOp(node, subtitleExpression) { x, y -> x * y }
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
            is EqExpression -> executeBinaryOp(node, subtitleExpression) { x, y -> BoolValue(x == y) }
            is NeqExpression -> executeBinaryOp(node, subtitleExpression) { x, y -> BoolValue(x != y) }
            is GtExpression -> executeBinaryOp(node, subtitleExpression) { x, y -> BoolValue(x > y) }
            is LtExpression -> executeBinaryOp(node, subtitleExpression) { x, y -> BoolValue(x < y) }
            is GeExpression -> executeBinaryOp(node, subtitleExpression) { x, y -> BoolValue(x >= y) }
            is LeExpression -> executeBinaryOp(node, subtitleExpression) { x, y -> BoolValue(x <= y) }
            is NotExpression -> executeUnaryOp(node) { x -> BoolValue(!x) }
            is ConstructorNode -> executeConstructor(node, identifier)
            is FunctionCallNode -> executeFunctionCall(node)
            is VoidNode -> VoidValue
            is ArrayElemNode -> arrExecutor.executeArrayElem(node, identifier, subtitleExpression)
            is BinaryTreeNodeElemAccessNode -> btExecutor.executeTreeAccess(
                variables[node.identifier]!! as BinaryTreeNodeValue,
                node
            ).second
            is BinaryTreeRootAccessNode -> btExecutor.executeRootAccess(node).second
            is NullNode -> NullValue
            is CastExpressionNode -> executeCastExpression(node)
            is InternalArrayMethodCallNode -> arrExecutor.executeInternalArrayMethodCall(node)
            is StringNode -> StringValue(node.value)
        }

        private fun executeCastExpression(node: CastExpressionNode): ExecValue {
            val exprValue = executeExpression(node.expr)

            return when (node.targetType) {
                is CharType -> CharValue((exprValue as DoubleAlias).toDouble().toChar())
                is NumberType -> {
                    if (exprValue is DoubleAlias) {
                        DoubleValue(exprValue.toDouble())
                    } else {
                        try {
                            DoubleValue((exprValue as StringValue).value.toDouble())
                        } catch (e: NumberFormatException) {
                            RuntimeError(value = "Invalid cast operation", lineNumber = node.lineNumber)
                        }
                    }
                }
                else -> RuntimeError(value = "Invalid cast operation", lineNumber = node.lineNumber)
            }
        }

        private fun executeMethodCall(
            node: MethodCallNode,
            insideMethodCall: Boolean,
            isExpression: Boolean
        ): ExecValue =
            if (variables[node.instanceIdentifier]?.manimObject?.render?.not() == true) {
                RuntimeError(value = "Method interaction with hidden data structure", lineNumber = node.lineNumber)
            } else when (val ds = variables[node.instanceIdentifier]) {
                is StackValue -> {
                    stackExecutor.executeStackMethodCall(node, ds, insideMethodCall, isExpression)
                }
                is ArrayValue -> {
                    arrExecutor.executeArrayMethodCall(node, ds)
                }
                is Array2DValue -> {
                    arrExecutor.execute2DArrayMethodCall(node, ds)
                }
                /** Extend with further data structures **/
                else -> EmptyValue
            }

        private fun executeConstructor(node: ConstructorNode, assignLHS: AssignLHS): ExecValue {
            val dsUID = functionNamePrefix + assignLHS.identifier
            return when (node.type) {
                is StackType -> stackExecutor.executeConstructor(node, dsUID, assignLHS)
                is ArrayType -> arrExecutor.executeConstructor(node, dsUID, assignLHS)
                is TreeType, is NodeType -> btExecutor.executeConstructor(node, dsUID, assignLHS)
                /** Extend with further data structures **/
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
            subtitleExpression: Boolean,
            op: (first: ExecValue, seconds: ExecValue) -> ExecValue
        ): ExecValue {

            val leftExpression = executeExpression(node.expr1, subtitleExpression = subtitleExpression)
            if (leftExpression is RuntimeError) {
                return leftExpression
            }
            val rightExpression = executeExpression(node.expr2, subtitleExpression = subtitleExpression)

            if (rightExpression is RuntimeError) {
                return rightExpression
            }
            return op(
                leftExpression,
                rightExpression
            )
        }
    }
}
