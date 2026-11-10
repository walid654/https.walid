package com.example.pathfinder

import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.RectF
import com.example.model.*
import java.util.UUID

object PathfinderOps {

    enum class Operation {
        UNITE,
        MINUS_FRONT,
        INTERSECT,
        EXCLUDE
    }

    /**
     * Executes a boolean pathfinder operation between two vector elements.
     * [bottomElement] is the back element, [topElement] is the front element.
     */
    fun combineElements(
        bottomElement: VectorElement,
        topElement: VectorElement,
        operation: Operation
    ): VectorElement? {
        val pathA = toAndroidPath(bottomElement)
        val pathB = toAndroidPath(topElement)
        val resultPath = Path()

        val op = when (operation) {
            Operation.UNITE -> Path.Op.UNION
            Operation.MINUS_FRONT -> Path.Op.DIFFERENCE
            Operation.INTERSECT -> Path.Op.INTERSECT
            Operation.EXCLUDE -> Path.Op.XOR
        }

        val success = resultPath.op(pathA, pathB, op)
        if (!success || resultPath.isEmpty) return null

        val segments = samplePathToSegments(resultPath)
        return PathElement(
            id = UUID.randomUUID().toString(),
            name = "${bottomElement.name} ${operation.name.lowercase().replace('_', ' ')}",
            segments = segments,
            closed = true,
            fill = bottomElement.fill,
            stroke = bottomElement.stroke,
            strokeStyle = bottomElement.strokeStyle,
            opacity = bottomElement.opacity,
            blendMode = bottomElement.blendMode
        )
    }

    /**
     * Converts any VectorElement into an android.graphics.Path
     */
    fun toAndroidPath(element: VectorElement): Path {
        val path = Path()
        when (element) {
            is PathElement -> {
                if (element.segments.isNotEmpty()) {
                    val p0 = element.transform.mapPoint(element.segments[0].anchor)
                    path.moveTo(p0.x, p0.y)
                    for (i in 1 until element.segments.size) {
                        val prev = element.segments[i - 1]
                        val curr = element.segments[i]
                        val cp1 = element.transform.mapPoint(prev.absoluteOutHandle)
                        val cp2 = element.transform.mapPoint(curr.absoluteInHandle)
                        val ep = element.transform.mapPoint(curr.anchor)
                        if (prev.hasOutHandle() || curr.hasInHandle()) {
                            path.cubicTo(cp1.x, cp1.y, cp2.x, cp2.y, ep.x, ep.y)
                        } else {
                            path.lineTo(ep.x, ep.y)
                        }
                    }
                    if (element.closed) {
                        val last = element.segments.last()
                        val first = element.segments.first()
                        if (last.hasOutHandle() || first.hasInHandle()) {
                            val cp1 = element.transform.mapPoint(last.absoluteOutHandle)
                            val cp2 = element.transform.mapPoint(first.absoluteInHandle)
                            val ep = element.transform.mapPoint(first.anchor)
                            path.cubicTo(cp1.x, cp1.y, cp2.x, cp2.y, ep.x, ep.y)
                        }
                        path.close()
                    }
                }
            }
            is ShapeElement -> {
                val segments = element.toPathSegments()
                if (segments.isNotEmpty()) {
                    val p0 = element.transform.mapPoint(segments[0].anchor)
                    path.moveTo(p0.x, p0.y)
                    for (i in 1 until segments.size) {
                        val prev = segments[i - 1]
                        val curr = segments[i]
                        val cp1 = element.transform.mapPoint(prev.absoluteOutHandle)
                        val cp2 = element.transform.mapPoint(curr.absoluteInHandle)
                        val ep = element.transform.mapPoint(curr.anchor)
                        if (prev.hasOutHandle() || curr.hasInHandle()) {
                            path.cubicTo(cp1.x, cp1.y, cp2.x, cp2.y, ep.x, ep.y)
                        } else {
                            path.lineTo(ep.x, ep.y)
                        }
                    }
                    path.close()
                }
            }
            is TextElement -> {
                val box = element.getBoundingBox()
                path.addRect(RectF(box.left, box.top, box.right, box.bottom), Path.Direction.CW)
            }
            is GroupElement -> {
                for (child in element.children) {
                    path.addPath(toAndroidPath(child))
                }
            }
        }
        return path
    }

    /**
     * Samples an android.graphics.Path back into discrete PathSegments with smooth anchors.
     */
    private fun samplePathToSegments(path: Path, sampleCount: Int = 36): List<PathSegment> {
        val measure = PathMeasure(path, false)
        val segments = mutableListOf<PathSegment>()
        val pos = FloatArray(2)
        val tan = FloatArray(2)

        do {
            val length = measure.length
            if (length > 0) {
                val step = length / sampleCount
                for (i in 0 until sampleCount) {
                    val distance = i * step
                    measure.getPosTan(distance, pos, tan)
                    val tangentLen = step * 0.35f
                    val inH = Point2D(-tan[0] * tangentLen, -tan[1] * tangentLen)
                    val outH = Point2D(tan[0] * tangentLen, tan[1] * tangentLen)
                    segments.add(
                        PathSegment(
                            anchor = Point2D(pos[0], pos[1]),
                            inHandle = inH,
                            outHandle = outH,
                            isSmooth = true
                        )
                    )
                }
            }
        } while (measure.nextContour())

        return segments
    }
}
