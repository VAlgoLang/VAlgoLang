package com.valgolang.linearrepresentation.datastructures.binarytree

import com.valgolang.frontend.DataStructureType
import com.valgolang.linearrepresentation.DataStructureMObject
import com.valgolang.linearrepresentation.MObject
import com.valgolang.runtime.datastructures.binarytree.BinaryTreeNodeValue

/**
 * Node initialisation
 *
 * @property ident
 * @property value
 * @property depth
 * @property runtime
 * @constructor Create empty Node structure
 */
data class NodeStructure(
    override val ident: String,
    val value: String,
    val depth: Int,
    override val runtime: Double = 1.0
) : MObject() {
    override val classPath: String = "python/binary_tree.py"
    override val className: String = "Node"
    override val pythonVariablePrefix: String = ""

    override fun getConstructor(): String {
        return "Node(\"$value\")"
    }

    override fun toPython(): List<String> {
        return listOf(
            "# Constructs a new binary tree node with value $value",
            "$ident = ${getConstructor()}"
        )
    }
}

/**
 * Tree initialisation
 *
 * @property type
 * @property ident
 * @property boundaries
 * @property maxSize
 * @property text
 * @property root
 * @property uid
 * @property runtime
 * @property render
 * @constructor Create empty Init tree structure
 */
data class InitTreeStructure(
    override val type: DataStructureType,
    override val ident: String,
    private var boundaries: List<Pair<Double, Double>> = emptyList(),
    private var maxSize: Int = -1,
    override var text: String,
    val root: BinaryTreeNodeValue,
    override val uid: String,
    override val runtime: Double,
    override val render: Boolean
) : DataStructureMObject(type, ident, uid, text) {
    override val classPath: String = "python/binary_tree.py"
    override val className: String = "Tree"
    override val pythonVariablePrefix: String = ""

    override fun getConstructor(): String {
        val coordinatesString = boundaries.joinToString(", ") { "[${it.first}, ${it.second}, 0]" }
        return "$ident = $className($coordinatesString, ${root.manimObject.ident}, \"$text\")"
    }

    override fun setNewBoundary(corners: List<Pair<Double, Double>>, newMaxSize: Int) {
        maxSize = newMaxSize
        boundaries = corners
    }

    override fun toPython(): List<String> {
        return listOf(
            "# Constructs a new $type \"$text\"",
            getConstructor(),
            getInstructionString("$ident.create_init(0)", false)
        )
    }
}
