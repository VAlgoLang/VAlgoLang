package com.manimdsl.linearrepresentation

import com.manimdsl.frontend.DataStructureType
import com.manimdsl.runtime.BinaryTreeNodeValue
import com.manimdsl.runtime.ExecValue
import com.manimdsl.shapes.*

/** Objects **/

sealed class MObject : ManimInstr() {
    abstract val shape: Shape
}

sealed class ShapeWithBoundary(open val uid: String) : MObject() {
    abstract fun setNewBoundary(corners: List<Pair<Double, Double>>, newMaxSize: Int)
    abstract fun setShape()
}

sealed class DataStructureMObject(
    open val type: DataStructureType,
    open val ident: String,
    override val uid: String,
    open var text: String,
    private var boundaries: List<Pair<Double, Double>> = emptyList()
) : ShapeWithBoundary(uid)

/** Positioning **/
interface Position
object RelativeToMoveIdent : Position

data class Coord(val x: Double, val y: Double) : Position {
    override fun toString(): String {
        return "$x, $y"
    }
}

enum class ObjectSide(var coord: Coord) {
    ABOVE(Coord(0.0, 0.25)),
    BELOW(Coord(0.0, -0.25)),
    LEFT(Coord(-0.25, 0.0)),
    RIGHT(Coord(0.25, 0.0));

    fun addOffset(offset: Int): Coord {
        if (this == ABOVE) {
            return Coord(this.coord.x, this.coord.y + offset)
        }
        return coord
    }

    override fun toString(): String {
        return coord.toString()
    }
}

/** MObjects **/
data class CodeBlock(
    val lines: List<List<String>>,
    val ident: String,
    val codeTextName: String,
    val pointerName: String,
    val textColor: String? = null,
    override val runtime: Double = 1.0,
    val syntaxHighlightingOn: Boolean = true,
    val syntaxHighlightingStyle: String = "inkpot",
    val tabSpacing: Int = 2,
    private var boundaries: List<Pair<Double, Double>> = emptyList()
) : ShapeWithBoundary(uid = "_code") {

    override var shape: Shape = NullShape

    override fun setNewBoundary(corners: List<Pair<Double, Double>>, newMaxSize: Int) {
        boundaries = corners
        setShape()
    }

    override fun setShape() {
        shape = CodeBlockShape(ident, textColor, syntaxHighlightingOn, syntaxHighlightingStyle, tabSpacing, boundaries)
    }

    override fun toPython(): List<String> {
        val codeLines = StringBuilder()
        codeLines.append("[")
        (lines.indices).forEach {
            codeLines.append("[")
            codeLines.append("\"${lines[it].joinToString("\",\"")}\"")
            if (it == lines.size - 1) {
                codeLines.append("]")
            } else {
                codeLines.append("], ")
            }
        }
        codeLines.append("]")

        return listOf(
            "# Building code visualisation pane",
            "code_lines = $codeLines",
            shape.getConstructor(),
            "$codeTextName = $ident.build()",
            "self.code_end = $ident.code_end",
            "self.code_end = min(sum([len(elem) for elem in code_lines]), self.code_end)",
            "self.play(FadeIn($codeTextName[self.code_start:self.code_end].move_to($ident.move_position)${getRuntimeString()}))",
            "# Constructing current line pointer",
            "$pointerName = ArrowTip(color=YELLOW).scale($ident.boundary_width * 0.7/5.0).flip(TOP)"
        )
    }
}

data class VariableBlock(
    val variables: List<String>,
    val ident: String,
    val variableGroupName: String,
    val textColor: String? = null,
    override val runtime: Double = 1.0,
    private var boundaries: List<Pair<Double, Double>> = emptyList(),
) : ShapeWithBoundary(uid = "_variables") {
    override var shape: Shape = NullShape

    override fun setNewBoundary(corners: List<Pair<Double, Double>>, newMaxSize: Int) {
        boundaries = corners
        setShape()
    }

    override fun setShape() {
        shape = VariableBlockShape(ident, variables, textColor, boundaries)
    }

    override fun toPython(): List<String> {
        return listOf(
            "# Building variable visualisation pane",
            shape.getConstructor(),
            "$variableGroupName = $ident.build()",
            "self.play(FadeIn($variableGroupName)${getRuntimeString()})"
        )
    }
}

data class NodeStructure(
    val ident: String,
    val value: String,
    val depth: Int,
    override val shape: Shape = NodeShape(ident, value),
    override val runtime: Double = 1.0

) : MObject() {
    override fun toPython(): List<String> {
        return listOf(
            "$ident = ${shape.getConstructor()}"
        )
    }
}

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
    override var shape: Shape = NullShape

    override fun setShape() {
        shape = InitTreeShape(ident, "\"$text\"", root, boundaries)
    }

    override fun setNewBoundary(corners: List<Pair<Double, Double>>, newMaxSize: Int) {
        maxSize = newMaxSize
        boundaries = corners
        setShape()
    }

    override fun toPython(): List<String> {
        return listOf(
            "# Constructing a new tree: $ident",
            shape.getConstructor(),
            getInstructionString("$ident.create_init(0)", false)
        )
    }
}

data class InitManimStack(
    override val type: DataStructureType,
    override val ident: String,
    val position: Position,
    val alignment: Alignment,
    override var text: String,
    val moveToShape: Shape? = null,
    val color: String? = null,
    val textColor: String? = null,
    val showLabel: Boolean? = null,
    val creationStyle: String? = null,
    val creationTime: Double? = null,
    private var boundaries: List<Pair<Double, Double>> = emptyList(),
    private var maxSize: Int = -1,
    override val uid: String,
    override val runtime: Double = 1.0,
    override val render: Boolean
) : DataStructureMObject(type, ident, uid, text, boundaries) {
    override var shape: Shape = NullShape

    override fun toPython(): List<String> {
        val creationString = if (creationStyle != null) ", creation_style=\"$creationStyle\"" else ""
        val runtimeString = if (creationTime != null) ", run_time=$creationTime" else ""
        val python =
            mutableListOf("# Constructing new $type \"${text}\"", shape.getConstructor())
        val newIdent = if (showLabel == null || showLabel) "\"$text\"" else ""
        python.add(getInstructionString("$ident.create_init($newIdent$creationString)$runtimeString", true))
        return python
    }

    override fun setShape() {
        shape = InitManimStackShape(ident, text, boundaries, alignment, color, textColor)
    }

    override fun setNewBoundary(corners: List<Pair<Double, Double>>, newMaxSize: Int) {
        maxSize = newMaxSize
        boundaries = corners
        setShape()
    }
}

data class ArrayStructure(
    override val type: DataStructureType,
    override val ident: String,
    override val render: Boolean,
    override var text: String,
    val values: Array<ExecValue>,
    val color: String? = null,
    val textColor: String? = null,
    var creationString: String? = null,
    override val runtime: Double = 1.0,
    val showLabel: Boolean? = null,
    var maxSize: Int = -1,
    private var boundaries: List<Pair<Double, Double>> = emptyList(),
    override val uid: String
) : DataStructureMObject(type, ident, uid, text, boundaries) {
    override var shape: Shape = NullShape

    init {
        if (creationString == null) creationString = "FadeIn"
    }

    override fun toPython(): List<String> {
        return listOf(
            "# Constructing new $type \"$text\"",
            shape.getConstructor(),
            if (render && (showLabel == null || showLabel)) "self.play($creationString($ident.title)${getRuntimeString()})" else "",
            getInstructionString(
                "[$creationString(array_elem.all${getRuntimeString()}) for array_elem in $ident.array_elements]",
                true
            )
        )
    }

    override fun setShape() {
        shape = ArrayShape(ident, values, text, boundaries, color, textColor, showLabel)
    }

    override fun setNewBoundary(corners: List<Pair<Double, Double>>, newMaxSize: Int) {
        maxSize = newMaxSize
        boundaries = corners
        setShape()
    }
}

data class Array2DStructure(
    override val type: DataStructureType,
    override val ident: String,
    override val render: Boolean,
    override var text: String,
    val values: Array<Array<ExecValue>>,
    val color: String? = null,
    val textColor: String? = null,
    var creationString: String? = null,
    override val runtime: Double = 1.0,
    val showLabel: Boolean? = null,
    var maxSize: Int = -1,
    private var boundaries: List<Pair<Double, Double>> = emptyList(),
    override val uid: String
) : DataStructureMObject(type, ident, uid, text, boundaries) {
    override var shape: Shape = NullShape

    init {
        if (creationString == null) creationString = "FadeIn"
    }

    override fun toPython(): List<String> {
        return listOf(
            "# Constructing new $type \"$text\"",
            shape.getConstructor(),
            if (render && (showLabel == null || showLabel)) "self.play($creationString($ident.title))" else "",
            getInstructionString("$ident.build(\"$creationString\")", true)
        )
    }

    override fun setShape() {
        shape = Array2DShape(ident, values, text, boundaries, color, textColor, showLabel)
    }

    override fun setNewBoundary(corners: List<Pair<Double, Double>>, newMaxSize: Int) {
        maxSize = newMaxSize
        boundaries = corners
        setShape()
    }
}

data class NewMObject(override val shape: Shape, val codeBlockVariable: String, override val runtime: Double = 1.0) :
    MObject() {
    override fun toPython(): List<String> {
        return listOf(
            "# Constructs a new ${shape.className} with value ${shape.text}",
            shape.getConstructor(),
        )
    }
}

object EmptyMObject : MObject() {
    override val shape: Shape = NullShape
    override val runtime: Double
        get() = 1.0

    override fun toPython(): List<String> = emptyList()
}
