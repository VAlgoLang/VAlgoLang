package com.valgolang.linearrepresentation

import com.valgolang.frontend.DataStructureType
import com.valgolang.stylesheet.StylesheetProperty

/** Objects **/

/**
 * Manim object instruction for creating shapes on scene
 *
 * @constructor Create empty M object
 */
abstract class MObject : ManimInstr() {
    abstract val ident: String
    abstract val classPath: String
    abstract val className: String
    abstract val pythonVariablePrefix: String
    abstract fun getConstructor(): String
}

/**
 * Manim instruction with boundary
 *
 * @constructor Create empty Manim instr with boundary
 */
interface ManimInstrWithBoundary {
    val uid: String
    fun setNewBoundary(corners: List<Pair<Double, Double>>, newMaxSize: Int)
}

/**
 * Shape with boundary - has a unique id and style
 *
 * @property uid
 * @constructor Create empty Shape with boundary
 */
abstract class ShapeWithBoundary(override val uid: String) : MObject(), ManimInstrWithBoundary {
    val style = PythonStyle()
}

/**
 * Data structure initialising Manim object
 *
 * @property type
 * @property ident
 * @property uid
 * @property text
 * @property boundaries
 * @constructor Create empty Data structure m object
 */
abstract class DataStructureMObject(
    open val type: DataStructureType,
    override val ident: String,
    override val uid: String,
    open var text: String,
    private var boundaries: List<Pair<Double, Double>> = emptyList()
) : ShapeWithBoundary(uid)

/**
 * Code block
 *
 * @property lines
 * @property ident
 * @property codeTextName
 * @property pointerName
 * @property textColor
 * @property runtime
 * @property syntaxHighlightingOn
 * @property syntaxHighlightingStyle
 * @property tabSpacing
 * @property boundaries
 * @constructor Create empty Code block
 */
data class CodeBlock(
    val lines: List<List<String>>,
    override val ident: String,
    val codeTextName: String,
    val pointerName: String,
    val textColor: String? = null,
    override val runtime: Double = 1.0,
    val syntaxHighlightingOn: Boolean = true,
    val syntaxHighlightingStyle: String = "inkpot",
    val tabSpacing: Int = 2,
    private var boundaries: List<Pair<Double, Double>> = emptyList()
) : ShapeWithBoundary(uid = "_code") {
    override val classPath: String = "python/code_block.py"
    override val className: String = "CodeBlock"
    override val pythonVariablePrefix: String = "code_block"

    override fun setNewBoundary(corners: List<Pair<Double, Double>>, newMaxSize: Int) {
        boundaries = corners
    }

    init {
        textColor?.let { style.addStyleAttribute(TextColor(it)) }
    }

    override fun getConstructor(): String {
        return "$ident = $className(code_lines, $boundaries, syntax_highlighting=${
        syntaxHighlightingOn.toString().capitalize()
        }, syntax_highlighting_style=\"$syntaxHighlightingStyle\", tab_spacing=$tabSpacing)"
    }

    override fun toPython(): List<String> {
        val codeLines = StringBuilder()
        codeLines.append("[")
        (lines.indices).forEach {
            codeLines.append("[")
            codeLines.append("\'${lines[it].joinToString("\',\'")}\'")
            if (it == lines.size - 1) {
                codeLines.append("]")
            } else {
                codeLines.append("], ")
            }
        }
        codeLines.append("]")

        return listOf(
            "# Builds code visualisation pane",
            "code_lines = $codeLines",
            getConstructor(),
            "$codeTextName = $ident.build()",
            "self.code_end = $ident.code_end",
            "self.code_end = min(sum([len(elem) for elem in code_lines]), self.code_end)",
            "self.play(FadeIn($codeTextName[self.code_start:self.code_end].move_to($ident.move_position)${getRuntimeString()}))",
            "# Constructs current line pointer",
            "$pointerName = ArrowTip(color=YELLOW).scale($ident.boundary_width * 0.7/5.0).flip(TOP)"
        )
    }
}

/**
 * Subtitle block
 *
 * @property variableNameGenerator
 * @property boundary
 * @property textColor
 * @property duration
 * @property runtime
 * @property ident
 * @constructor Create empty Subtitle block
 */
data class SubtitleBlock(
    val variableNameGenerator: VariableNameGenerator,
    private var boundary: List<Pair<Double, Double>> = emptyList(),
    val textColor: String? = null,
    var duration: Double,
    override val runtime: Double = 1.0,
    override val ident: String = variableNameGenerator.generateNameFromPrefix("subtitle_block")
) : ShapeWithBoundary("_subtitle") {
    override val classPath: String = "python/subtitles.py"
    override val className: String = "SubtitleBlock"
    override val pythonVariablePrefix: String = "subtitle_block"

    init {
        textColor?.let { style.addStyleAttribute(TextColor(it)) }
    }

    override fun getConstructor(): String {
        val coordinatesString =
            if (boundary.isEmpty()) "" else "[${boundary.joinToString(", ") { "[${it.first}, ${it.second}, 0]" }}]"

        return "$ident = $className(self.get_time() + $duration, $coordinatesString$style)"
    }

    override fun toPython(): List<String> {
        return listOf(
            "# Builds subtitle pane",
            getConstructor(),
            "self.time_objects.append($ident)"
        )
    }

    override fun setNewBoundary(corners: List<Pair<Double, Double>>, newMaxSize: Int) {
        boundary = corners
    }
}

/**
 * Variable block
 *
 * @property variables
 * @property ident
 * @property variableGroupName
 * @property textColor
 * @property runtime
 * @property boundaries
 * @constructor Create empty Variable block
 */
data class VariableBlock(
    val variables: List<String>,
    override val ident: String,
    val variableGroupName: String,
    val textColor: String? = null,
    override val runtime: Double = 1.0,
    private var boundaries: List<Pair<Double, Double>> = emptyList(),
) : ShapeWithBoundary(uid = "_variables") {
    override val classPath: String = "python/variable_block.py"
    override val className: String = "VariableBlock"
    override val pythonVariablePrefix: String = "variable_block"

    init {
        textColor?.let { style.addStyleAttribute(TextColor(it)) }
    }

    override fun getConstructor(): String {
        return "$ident = $className(${"[\'${variables.joinToString("\',\'")}\']"}, $boundaries$style)"
    }

    override fun setNewBoundary(corners: List<Pair<Double, Double>>, newMaxSize: Int) {
        boundaries = corners
    }

    override fun toPython(): List<String> {
        return listOf(
            "# Builds variable visualisation pane",
            getConstructor(),
            "$variableGroupName = $ident.build()",
            "self.play(FadeIn($variableGroupName)${getRuntimeString()})"
        )
    }
}

/**
 * Rectangle
 *
 * @property ident
 * @property text
 * @property dataStructureIdentifier
 * @property runtime
 * @constructor
 *
 * @param color
 * @param textColor
 */
class Rectangle(
    override val ident: String,
    val text: String,
    private val dataStructureIdentifier: String,
    color: String? = null,
    textColor: String? = null,
    override val runtime: Double = 1.0,
) : MObject() {
    override val classPath: String = "python/rectangle.py"
    override val className: String = "RectangleBlock"
    override val pythonVariablePrefix: String = "rectangle"
    val style = PythonStyle()

    init {
        color?.let { style.addStyleAttribute(Color(it)) }
        textColor?.let { style.addStyleAttribute(TextColor(it)) }
    }

    fun restyle(styleProperties: StylesheetProperty, runtimeString: String): List<String> {
        val instructions = mutableListOf<String>()

        styleProperties.borderColor?.let {
            instructions.add(
                "FadeToColor($ident.shape, ${
                styleProperties.handleColourValue(
                    it
                )
                })"
            )
        }
        styleProperties.textColor?.let {
            instructions.add(
                "FadeToColor($ident.text, ${
                styleProperties.handleColourValue(
                    it
                )
                })"
            )
        }

        return if (instructions.isEmpty()) {
            emptyList()
        } else {
            listOf(
                "# Changes color of \"$ident\"",
                "self.play_animation(${instructions.joinToString(", ")}$runtimeString)"
            )
        }
    }

    override fun getConstructor(): String {
        return "$ident = $className(\"${text}\", ${dataStructureIdentifier}$style)"
    }

    override fun toPython(): List<String> {
        return listOf(
            "# Constructs a new $className with value $text",
            getConstructor(),
        )
    }
}

/**
 * Empty Manim object
 *
 * @constructor Create empty Empty m object
 */
object EmptyMObject : MObject() {
    override val ident: String = ""
    override val classPath: String = ""
    override val className: String = ""
    override val pythonVariablePrefix: String = ""

    override fun getConstructor(): String = ""

    override val runtime: Double
        get() = 1.0

    override fun toPython(): List<String> = emptyList()
}
