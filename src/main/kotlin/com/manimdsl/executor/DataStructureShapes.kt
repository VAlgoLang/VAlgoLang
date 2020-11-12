package com.manimdsl.executor

import com.manimdsl.ExitStatus
import com.manimdsl.errorhandling.ErrorHandler

sealed class BoundaryShape(var x1: Double = 0.0, var y1: Double = 0.0) {

    abstract var width: Int
    abstract var height: Int

    abstract var maxSize: Int
    abstract val priority: Int

    abstract val minDimensions: Pair<Int, Int>
    abstract val dynamicWidth: Boolean
    abstract val dynamicHeight: Boolean
    abstract val strictRatio: Boolean

    abstract fun setCoords(x: Double, y: Double): BoundaryShape

    abstract fun clone(): BoundaryShape

    fun coordInShape(x: Double, y: Double): Boolean {
        return (x >= x1) && (y >= y1) && (x <= (x1 + width)) && (y <= (y1 + height))
    }

    fun corners(): List<Pair<Double, Double>> {
        // UL, UR, LL, LR
        return listOf(Pair(x1, y1 + height), Pair(x1 + width, y1 + height), Pair(x1, y1), Pair(x1 + width, y1))
    }

    fun area(): Int {
        return width * height
    }

    fun overlapsShape(boundaryShape: BoundaryShape): Boolean {
        val l1 = Pair(x1, y1 + height)
        val r1 = Pair(x1 + width, y1)
        val l2 = Pair(boundaryShape.x1, boundaryShape.y1 + boundaryShape.height)
        val r2 = Pair(boundaryShape.x1 + boundaryShape.width, boundaryShape.y1)
        // If one rectangle is on left side of other
        if (l1.first >= r2.first || l2.first >= r1.first) {
            return false
        }

        // If one rectangle is above other
        return !(l1.second <= r2.second || l2.second <= r1.second)
    }

    fun offsetWidth(offset: Int = 1): BoundaryShape {
        width += offset
        return this
    }

    fun offsetHeight(offset: Int = 1): BoundaryShape {
        height += offset
        y1 -= offset
        return this
    }

    fun shiftHorizontalToRight(offset: Double): BoundaryShape {
        x1 += offset
        return this
    }

    fun shiftVerticalUpwards(offset: Double): BoundaryShape {
        y1 -= offset
        return this
    }
}

data class SquareBoundary(
    override val minDimensions: Pair<Int, Int> = Pair(4, 4),
    override var width: Int = minDimensions.first,
    override var height: Int = minDimensions.second, override var maxSize: Int = 0
) : BoundaryShape() {
    override val dynamicWidth: Boolean = false
    override val dynamicHeight: Boolean = true
    override val strictRatio: Boolean = true
    override val priority: Int = 3

    override fun setCoords(x: Double, y: Double): SquareBoundary {
        this.x1 = x
        this.y1 = y
        return this
    }

    override fun clone(): SquareBoundary {
        val square = SquareBoundary(minDimensions, width, height, maxSize)
        square.x1 = x1
        square.y1 = y1
        return square
    }
}

data class TallBoundary(
    override val minDimensions: Pair<Int, Int> = Pair(2, 4),
    override var width: Int = minDimensions.first,
    override var height: Int = minDimensions.second, override var maxSize: Int = 0
) : BoundaryShape() {
    override val dynamicWidth: Boolean = false
    override val dynamicHeight: Boolean = true
    override val strictRatio: Boolean = false
    override val priority: Int = 2

    override fun setCoords(x: Double, y: Double): TallBoundary {
        this.x1 = x
        this.y1 = y
        return this
    }

    override fun clone(): TallBoundary {
        val tall = TallBoundary(minDimensions, width, height, maxSize)
        tall.x1 = x1
        tall.y1 = y1
        return tall
    }
}

data class WideBoundary(
    override val minDimensions: Pair<Int, Int> = Pair(4, 2),
    override var width: Int = minDimensions.first,
    override var height: Int = minDimensions.second, override var maxSize: Int = 0
) : BoundaryShape() {
    override val dynamicWidth: Boolean = true
    override val dynamicHeight: Boolean = false
    override val strictRatio: Boolean = false
    override val priority: Int = 1

    override fun setCoords(x: Double, y: Double): WideBoundary {
        this.x1 = x
        this.y1 = y
        return this
    }

    override fun clone(): WideBoundary {
        val wide = WideBoundary(minDimensions, width, height, maxSize)
        wide.x1 = x1
        wide.y1 = y1
        return wide
    }
}

enum class Corner(val coord: Pair<Double, Double>) {
    BL(Pair(-2.0, -4.0)),
    BR(Pair(7.0, -4.0)),
    TL(Pair(-2.0, 4.0)),
    TR(Pair(7.0, 4.0));

    fun next(): Corner {
        return when (this) {
            BL -> TR
            TL -> BR
            TR -> BR
            BR -> TL
        }
    }

    fun direction(secondScan: Boolean): ScanDir {
        return when (this) {
            BL -> if (secondScan) ScanDir.RIGHT else ScanDir.UP
            TL -> ScanDir.RIGHT
            TR -> if (secondScan) ScanDir.DOWN else ScanDir.LEFT
            BR -> ScanDir.LEFT
        }
    }
}

enum class ScanDir {
    UP,
    DOWN,
    LEFT,
    RIGHT;
}

class Scene {

    private val sceneShape = WideBoundary(width = 9, height = 8, maxSize = -1)
    private val fullSceneShape = WideBoundary(width = 14, height = 8, maxSize = -1)

    init {
        sceneShape.x1 = -2.0
        sceneShape.y1 = -4.0
        fullSceneShape.x1 = -7.0
        fullSceneShape.y1 = -4.0
    }

    private val sceneShapes = mutableListOf<BoundaryShape>()

    fun compute(shapes: List<Pair<String, BoundaryShape>>, fullScreen: Boolean): Pair<ExitStatus, Map<String, BoundaryShape>> {
        val total = shapes.sumBy { it.second.area() }
        if (total > sceneShape.area()) {
            ErrorHandler.addTooManyDatastructuresError()
            return Pair(ExitStatus.RUNTIME_ERROR, emptyMap())
        } else {
            val sortedShapes = shapes.sortedBy { -it.second.maxSize }.sortedBy { -it.second.priority }
            sortedShapes.forEach {
                val didAddToScene = when (it.second) {
                    is WideBoundary -> addToScene(Corner.BL, it.second)
                    is SquareBoundary -> addToScene(Corner.TL, it.second)
                    is TallBoundary -> addToScene(Corner.TR, it.second)
                }
                if (!didAddToScene) return Pair(ExitStatus.RUNTIME_ERROR, emptyMap())
            }
            sortedShapes.forEach { maximise(it.second) }
            centralise(fullScreen)
            return Pair(ExitStatus.EXIT_SUCCESS, sortedShapes.toMap())
        }
    }

    private fun addToScene(corner: Corner, boundaryShape: BoundaryShape, secondScan: Boolean = false): Boolean {
        return when (corner) {
            Corner.BL -> addOnSide(
                corner,
                boundaryShape.setCoords(Corner.BL.coord.first, Corner.BL.coord.second),
                secondScan
            )
            Corner.TL -> addOnSide(
                corner,
                boundaryShape.setCoords(Corner.TL.coord.first, Corner.TL.coord.second - boundaryShape.height),
                secondScan
            )
            Corner.TR -> addOnSide(
                corner,
                boundaryShape.setCoords(
                    Corner.TR.coord.first - boundaryShape.width,
                    Corner.TR.coord.second - boundaryShape.height
                ),
                secondScan
            )
            Corner.BR -> addOnSide(
                corner,
                boundaryShape.setCoords(Corner.BR.coord.first - boundaryShape.width, Corner.BR.coord.second),
                secondScan
            )
        }
    }

    private fun centralise(fullScreen: Boolean = false) {
        if (sceneShapes.isNotEmpty()) {
            val leftXCoord = sceneShapes.map { it.corners()[0] }.minOf { it.first}
            val rightXCoord = sceneShapes.map { it.corners()[1] }.maxOf { it.first }
            val topYCoord = sceneShapes.map { it.corners()[0] }.maxOf { it.second }
            val bottomYCoord = sceneShapes.map { it.corners()[2] }.minOf { it.second }

            val shapeOverallHeight = topYCoord - bottomYCoord
            val shapesOverallWidth = rightXCoord - leftXCoord
            val availableWidth = if (fullScreen) fullSceneShape.width else sceneShape.width
            val avaliableHeight = if (fullScreen) fullSceneShape.height else sceneShape.height

            if (availableWidth > shapesOverallWidth) {
                sceneShapes.filter { it !is SquareBoundary }.forEach { it.shiftHorizontalToRight((shapesOverallWidth - availableWidth) / 2) }
            }
            if (avaliableHeight > shapeOverallHeight) {
                sceneShapes.forEach {
                    when (it) {
                        !is SquareBoundary -> it.shiftVerticalUpwards((shapeOverallHeight - avaliableHeight) / 2)
                        else -> it.shiftVerticalUpwards(-(shapeOverallHeight - avaliableHeight) / 2)
                    }
                }
            }
        }
    }

    private fun maximise(boundaryShape: BoundaryShape) {
        if (boundaryShape.strictRatio) {
            while (isValidMaximisedShape(boundaryShape.clone().offsetHeight().offsetWidth())) {
                boundaryShape.offsetHeight().offsetWidth()
            }
        } else {
            if (boundaryShape.dynamicWidth) {
                while (isValidMaximisedShape(boundaryShape.clone().offsetWidth())) {
                    boundaryShape.offsetWidth()
                }
            } else if (boundaryShape.dynamicHeight) {
                while (isValidMaximisedShape(boundaryShape.clone().offsetHeight())) {
                    boundaryShape.offsetHeight()
                }
            }
        }
    }

    private fun addOnSide(corner: Corner, boundaryShape: BoundaryShape, secondScan: Boolean): Boolean {
        if (sceneShapes.any { it.overlapsShape(boundaryShape) }) {
            return addOnSide(
                corner,
                moveShapeInDirection(corner.direction(secondScan), boundaryShape),
                secondScan
            )
        } else if (!withinScene(boundaryShape)) {
            if (secondScan) {
                // Could not fit on anywhere on the scene
                ErrorHandler.addTooManyDatastructuresError()
                return false
            }
            return addToScene(corner.next(), boundaryShape, true)
        } else {
            sceneShapes.add(boundaryShape)
            return true
        }
    }

    private fun moveShapeInDirection(scanDir: ScanDir, boundaryShape: BoundaryShape): BoundaryShape {
        return when (scanDir) {
            ScanDir.UP -> boundaryShape.setCoords(boundaryShape.x1, boundaryShape.y1 + 1)
            ScanDir.DOWN -> boundaryShape.setCoords(boundaryShape.x1, boundaryShape.y1 - 1)
            ScanDir.LEFT -> boundaryShape.setCoords(boundaryShape.x1 - 1, boundaryShape.y1)
            ScanDir.RIGHT -> boundaryShape.setCoords(boundaryShape.x1 + 1, boundaryShape.y1)
        }
    }

    private fun isValidMaximisedShape(boundaryShape: BoundaryShape): Boolean {
        return withinScene(boundaryShape) && sceneShapes
            .count { it.overlapsShape(boundaryShape) } == 1
    }

    private fun withinScene(boundaryShape: BoundaryShape): Boolean {
        return boundaryShape.corners().all { sceneShape.coordInShape(it.first, it.second) }
    }

}