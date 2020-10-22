package com.manimdsl

import com.manimdsl.executor.*
import com.manimdsl.frontend.*
import com.manimdsl.linearrepresentation.*
import com.manimdsl.shapes.Rectangle
import java.util.*

class ASTExecutor(
    private val program: ProgramNode,
    private val symbolTableVisitor: SymbolTableVisitor,
    private val fileLines: List<String>
) {

    private val linearRepresentation = mutableListOf<ManimInstr>()
    private val variableNameGenerator = VariableNameGenerator(symbolTableVisitor)

    private val codeBlockVariable: String
    private val codeTextVariable: String
    private val pointerVariable: String

    init {
        pointerVariable = variableNameGenerator.generateNameFromPrefix("pointer")
        codeBlockVariable = variableNameGenerator.generateNameFromPrefix("code_block")
        codeTextVariable = variableNameGenerator.generateNameFromPrefix("code_text")
    }

    // Map of all the variables and there current values
    private val variables = mutableMapOf<String, ExecValue>()

    // Keep track of next instruction to execute
    private var programCounter = 0

    private val finalDSLCode = mutableListOf<String>()

    private fun executeExpression(node: ExpressionNode, insideMethodCall: Boolean = false): ExecValue {
        return when (node) {
            is IdentifierNode -> variables[node.identifier]!!
            is NumberNode -> DoubleValue(node.double)
            is MethodCallNode -> executeMethodCall(node, insideMethodCall)
            is AddExpression -> executeBinaryOp(node) { x, y -> x + y }
            is SubtractExpression -> executeBinaryOp(node) { x, y -> x - y }
            is MultiplyExpression -> executeBinaryOp(node) { x, y -> x * y }
            is PlusExpression -> executeUnaryOp(node) { x -> x }
            is MinusExpression -> executeUnaryOp(node) { x -> DoubleValue(-(x as DoubleValue).value) }
            is BoolNode -> BoolValue(node.value)
            is AndExpression -> executeBinaryOp(node) { x, y -> BoolValue((x as BoolValue).value && (y as BoolValue).value) }
            is OrExpression -> executeBinaryOp(node) { x, y -> BoolValue((x as BoolValue).value || (y as BoolValue).value) }
            is EqExpression -> executeBinaryOp(node) { x, y -> BoolValue(x == y) }
            is NeqExpression -> executeBinaryOp(node) { x, y -> BoolValue(x != y) }
            is GtExpression -> executeBinaryOp(node) { x, y -> BoolValue(x > y) }
            is LtExpression -> executeBinaryOp(node) { x, y -> BoolValue(x < y) }
            is GeExpression -> executeBinaryOp(node) { x, y -> BoolValue(x >= y) }
            is LeExpression -> executeBinaryOp(node) { x, y -> BoolValue(x <= y) }
            is NotExpression -> executeUnaryOp(node) { x -> BoolValue(!x) }
            else -> EmptyValue
        }
    }

    private fun executeMethodCall(node: MethodCallNode, insideMethodCall: Boolean): ExecValue {
        return when (val ds = variables[node.instanceIdentifier]) {
            is StackValue -> {
                return when (node.dataStructureMethod) {
                    is StackType.PushMethod -> {
                        val value = executeExpression(node.arguments[0], true)
                        val topOfStack = if (ds.stack.empty()) ds.manimObject else ds.stack.peek().manimObject

                        val hasOldMObject = value.manimObject !is EmptyMObject
                        val oldMObject = value.manimObject
                        val rectangle = if (hasOldMObject) oldMObject else NewMObject(
                            Rectangle(
                                variableNameGenerator.generateNameFromPrefix("rectangle"),
                                value.value.toString()
                            ),
                            codeTextVariable
                        )

                        val instructions =
                            mutableListOf<ManimInstr>(MoveObject(rectangle.shape, topOfStack.shape, ObjectSide.ABOVE))
                        if (!hasOldMObject) {
                            instructions.add(0, rectangle)
                        }

                        linearRepresentation.addAll(instructions)
                        value.manimObject = rectangle
                        ds.stack.push(value)
                        value
                    }
                    is StackType.PopMethod -> {
                        val poppedValue = ds.stack.pop()
                        val newTopOfStack = if (ds.stack.empty()) ds.manimObject else ds.stack.peek().manimObject

                        val topOfStack = poppedValue.manimObject
                        val instructions = listOf(
                            MoveObject(
                                topOfStack.shape,
                                newTopOfStack.shape,
                                ObjectSide.ABOVE,
                                20,
                                !insideMethodCall
                            ),
                        )

                        linearRepresentation.addAll(instructions)
                        return poppedValue
                    }
                    else -> EmptyValue
                }
            }
            else -> EmptyValue
        }
    }

    private fun executeConstructor(node: ConstructorNode, identifier: String): ExecValue {
        return when (node.type) {
            is StackType -> {
                val numStack = variables.values.filterIsInstance(StackValue::class.java).lastOrNull()
                val (instructions, newObject) = if (numStack == null) {
                    val stackInit = InitStructure(
                        Coord(2.0, -1.0),
                        Alignment.HORIZONTAL,
                        variableNameGenerator.generateNameFromPrefix("empty"),
                        identifier
                    )
                    // Add to stack of objects to keep track of identifier
                    Pair(listOf(stackInit), stackInit)
                } else {
                    val stackInit = InitStructure(
                        RelativeToMoveIdent,
                        Alignment.HORIZONTAL,
                        variableNameGenerator.generateNameFromPrefix("empty"),
                        identifier,
                        numStack.manimObject.shape
                    )
                    Pair(listOf(stackInit), stackInit)
                }
                linearRepresentation.addAll(instructions)
                StackValue(newObject, Stack())
            }
        }
    }

    private fun executeUnaryOp(node: UnaryExpression, op: (first: ExecValue) -> ExecValue): ExecValue {
        return op(executeExpression(node.expr))
    }

    private fun executeBinaryOp(
        node: BinaryExpression,
        op: (first: ExecValue, seconds: ExecValue) -> ExecValue
    ): ExecValue {
        return op(
            executeExpression(node.expr1),
            executeExpression(node.expr2)
        )
    }


    // Returns whether the program is complete and the state of all the variables after executing the statement
    fun executeNextStatement(): Pair<Boolean, List<ManimInstr>> {
        val node = program.statements[programCounter]
        programCounter++

        executeStatement(node)

        val endOfProgram = program.statements.size == programCounter
        if (endOfProgram) {
            linearRepresentation.add(0, CodeBlock(finalDSLCode, codeBlockVariable, codeTextVariable, pointerVariable))
        }

        return Pair(endOfProgram, linearRepresentation)
    }

    private fun executeStatement(statement: StatementNode) {
        if (statement is CodeNode) {
            finalDSLCode.add(fileLines[statement.lineNumber - 1])
            linearRepresentation.add(MoveToLine(finalDSLCode.size, pointerVariable, codeBlockVariable))
        }

        when (statement) {
            is DeclarationOrAssignment -> visitAssignmentOrDeclaration(statement)
            is ExpressionNode -> executeExpression(statement)
            is IfStatement -> executeIfStatement(statement)
            is SleepNode -> linearRepresentation.add(Sleep((executeExpression(statement.sleepTime) as DoubleValue).value))
        }
    }

    private fun addCodeNodeToCodeBlock(statement: StatementNode) {
        if (statement is CodeNode) {
            finalDSLCode.add(fileLines[statement.lineNumber - 1])
        }
    }

    private fun executeIfStatement(ifStatement: IfStatement) {
        var conditionValue = executeExpression(ifStatement.ifCondition) as BoolValue
        val currentScope = symbolTableVisitor.getCurrentScopeID()
        var conditionMet = false;

        //If
        if (conditionValue.value) {
            symbolTableVisitor.goToScope(ifStatement.ifScope)
            ifStatement.ifStatement.forEach { executeStatement(it) }
            symbolTableVisitor.goToScope(currentScope)
            conditionMet = true
        }

        // Elif
        for (elif in ifStatement.elifs) {
            // Add statement to code
            conditionValue = executeExpression(elif.condition) as BoolValue
            if (conditionValue.value && !conditionMet) {
                symbolTableVisitor.goToScope(elif.scope)
                elif.statements.forEach { executeStatement(it) }
                symbolTableVisitor.goToScope(currentScope)
                conditionMet = true
            } else {
                // TODO Add to code block and don't execute when condition is not met
                elif.statements.forEach { addCodeNodeToCodeBlock(it) }
            }
        }

        // Else
        if (!conditionMet) {
            symbolTableVisitor.goToScope(ifStatement.elseScope)
            ifStatement.elseStatement.forEach { executeStatement(it) }
            symbolTableVisitor.goToScope(currentScope)
        } else {
            // TODO Add to code block and don't execute when condition is not met
            ifStatement.elseStatement.forEach { addCodeNodeToCodeBlock(it) }
        }

    }

    private fun visitAssignmentOrDeclaration(node: DeclarationOrAssignment) {
        variables[node.identifier] =
            when (node.expression) {
                is ConstructorNode -> executeConstructor(node.expression as ConstructorNode, node.identifier)
                else -> executeExpression(node.expression)
            }
    }

    fun getValue(identifier: String): ExecValue {
        return this.variables.getOrDefault(identifier, EmptyValue)
    }
}