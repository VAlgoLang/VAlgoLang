package com.manimdsl.linearrepresentation.datastructures.list

import com.manimdsl.linearrepresentation.ManimInstr
import com.manimdsl.runtime.ExecValue

data class ListPrepend(
    val arrayIdent: String,
    val newElemValue: ExecValue,
    override val runtime: Double,
    override val render: Boolean
) :
    ManimInstr() {
    override fun toPython(): List<String> {
//        val instruction = getInstructionString("animations", true)
// //        return listOf("[$instruction for animations in array.prepend(${newElemValue})]")
        return listOf()
    }
}

data class ListAppend(
    val arrayIdent: String,
    val newElemValue: ExecValue,
    override val runtime: Double,
    override val render: Boolean
) :
    ManimInstr() {
    override fun toPython(): List<String> {
//        val instruction = getInstructionString("animations", true)
//        return listOf("[$instruction for animations in array.append(${newElemValue})]")
        return listOf()
    }
}
