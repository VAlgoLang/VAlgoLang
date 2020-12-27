package com.manimdsl.runtime.datastructures

import com.manimdsl.frontend.AssignLHS
import com.manimdsl.frontend.ConstructorNode
import com.manimdsl.linearrepresentation.ManimInstr
import com.manimdsl.linearrepresentation.VariableNameGenerator
import com.manimdsl.runtime.BoundaryShape
import com.manimdsl.runtime.ExecValue
import com.manimdsl.runtime.VirtualMachine
import com.manimdsl.stylesheet.Stylesheet

interface DSMethodExecutor {
    val variables: MutableMap<String, ExecValue>
    val linearRepresentation: MutableList<ManimInstr>
    val frame: VirtualMachine.Frame
    val stylesheet: Stylesheet
    val animationSpeeds: java.util.ArrayDeque<Double>
    val dataStructureBoundaries: MutableMap<String, BoundaryShape>
    val variableNameGenerator: VariableNameGenerator
    val codeTextVariable: String

    fun executeConstructor(node: ConstructorNode, dsUID: String, assignLHS: AssignLHS): ExecValue
}
