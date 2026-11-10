package com.example.model

import kotlin.math.*

data class Point2D(val x: Float = 0f, val y: Float = 0f) {
    operator fun plus(other: Point2D) = Point2D(x + other.x, y + other.y)
    operator fun minus(other: Point2D) = Point2D(x - other.x, y - other.y)
    operator fun times(factor: Float) = Point2D(x * factor, y * factor)
    operator fun div(factor: Float) = Point2D(x / factor, y / factor)

    fun distanceTo(other: Point2D): Float {
        val dx = x - other.x
        val dy = y - other.y
        return sqrt(dx * dx + dy * dy)
    }

    fun angleTo(other: Point2D): Float {
        return atan2(other.y - y, other.x - x) * (180f / Math.PI.toFloat())
    }
}

data class Rect2D(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 0f,
    val bottom: Float = 0f
) {
    val width: Float get() = max(0f, right - left)
    val height: Float get() = max(0f, bottom - top)
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f

    fun contains(pt: Point2D): Boolean {
        return pt.x in left..right && pt.y in top..bottom
    }

    fun inflate(padding: Float): Rect2D {
        return Rect2D(left - padding, top - padding, right + padding, bottom + padding)
    }

    fun union(other: Rect2D): Rect2D {
        return Rect2D(
            min(left, other.left),
            min(top, other.top),
            max(right, other.right),
            max(bottom, other.bottom)
        )
    }

    companion object {
        fun fromPoints(p1: Point2D, p2: Point2D): Rect2D {
            return Rect2D(
                left = min(p1.x, p2.x),
                top = min(p1.y, p2.y),
                right = max(p1.x, p2.x),
                bottom = max(p1.y, p2.y)
            )
        }

        fun fromCenter(center: Point2D, halfWidth: Float, halfHeight: Float): Rect2D {
            return Rect2D(
                center.x - halfWidth,
                center.y - halfHeight,
                center.x + halfWidth,
                center.y + halfHeight
            )
        }
    }
}

/**
 * 2D Affine Transformation Matrix
 * [ a  c  tx ]
 * [ b  d  ty ]
 * [ 0  0  1  ]
 */
data class Matrix2D(
    val a: Float = 1f,
    val b: Float = 0f,
    val c: Float = 0f,
    val d: Float = 1f,
    val tx: Float = 0f,
    val ty: Float = 0f
) {
    fun mapPoint(pt: Point2D): Point2D {
        return Point2D(
            x = a * pt.x + c * pt.y + tx,
            y = b * pt.x + d * pt.y + ty
        )
    }

    fun mapPoints(pts: List<Point2D>): List<Point2D> {
        return pts.map { mapPoint(it) }
    }

    fun multiply(other: Matrix2D): Matrix2D {
        return Matrix2D(
            a = a * other.a + c * other.b,
            b = b * other.a + d * other.b,
            c = a * other.c + c * other.d,
            d = b * other.c + d * other.d,
            tx = a * other.tx + c * other.ty + tx,
            ty = b * other.tx + d * other.ty + ty
        )
    }

    fun translate(dx: Float, dy: Float): Matrix2D {
        return multiply(translation(dx, dy))
    }

    fun scale(sx: Float, sy: Float, cx: Float = 0f, cy: Float = 0f): Matrix2D {
        return multiply(scaling(sx, sy, cx, cy))
    }

    fun rotate(degrees: Float, cx: Float = 0f, cy: Float = 0f): Matrix2D {
        return multiply(rotation(degrees, cx, cy))
    }

    fun invert(): Matrix2D? {
        val det = a * d - b * c
        if (abs(det) < 1e-6f) return null
        val invDet = 1f / det
        return Matrix2D(
            a = d * invDet,
            b = -b * invDet,
            c = -c * invDet,
            d = a * invDet,
            tx = (c * ty - d * tx) * invDet,
            ty = (b * tx - a * ty) * invDet
        )
    }

    operator fun times(other: Matrix2D): Matrix2D = multiply(other)

    fun isIdentity(): Boolean {
        return a == 1f && b == 0f && c == 0f && d == 1f && tx == 0f && ty == 0f
    }

    companion object {
        val IDENTITY = Matrix2D()

        fun translation(tx: Float, ty: Float) = Matrix2D(tx = tx, ty = ty)

        fun scaling(sx: Float, sy: Float, cx: Float = 0f, cy: Float = 0f): Matrix2D {
            if (cx == 0f && cy == 0f) return Matrix2D(a = sx, d = sy)
            return translation(cx, cy)
                .multiply(Matrix2D(a = sx, d = sy))
                .multiply(translation(-cx, -cy))
        }

        fun rotation(degrees: Float, cx: Float = 0f, cy: Float = 0f): Matrix2D {
            val rad = Math.toRadians(degrees.toDouble()).toFloat()
            val cosTheta = cos(rad)
            val sinTheta = sin(rad)
            if (cx == 0f && cy == 0f) {
                return Matrix2D(a = cosTheta, b = sinTheta, c = -sinTheta, d = cosTheta)
            }
            return translation(cx, cy)
                .multiply(Matrix2D(a = cosTheta, b = sinTheta, c = -sinTheta, d = cosTheta))
                .multiply(translation(-cx, -cy))
        }
    }
}

/**
 * Bezier path segment representing an anchor and its incoming & outgoing control handles (relative dx, dy)
 */
data class PathSegment(
    val anchor: Point2D,
    val inHandle: Point2D = Point2D(0f, 0f),   // Offset relative to anchor
    val outHandle: Point2D = Point2D(0f, 0f), // Offset relative to anchor
    val isSmooth: Boolean = false
) {
    val absoluteInHandle: Point2D get() = anchor + inHandle
    val absoluteOutHandle: Point2D get() = anchor + outHandle

    fun hasInHandle(): Boolean = inHandle.x != 0f || inHandle.y != 0f
    fun hasOutHandle(): Boolean = outHandle.x != 0f || outHandle.y != 0f
}

object BezierMath {

    fun evalCubic(p0: Point2D, p1: Point2D, p2: Point2D, p3: Point2D, t: Float): Point2D {
        val u = 1f - t
        val tt = t * t
        val uu = u * u
        val uuu = uu * u
        val ttt = tt * t

        val x = uuu * p0.x + 3f * uu * t * p1.x + 3f * u * tt * p2.x + ttt * p3.x
        val y = uuu * p0.y + 3f * uu * t * p1.y + 3f * u * tt * p2.y + ttt * p3.y
        return Point2D(x, y)
    }

    /**
     * Compute tight bounding box of cubic Bezier curve
     */
    fun cubicBounds(p0: Point2D, p1: Point2D, p2: Point2D, p3: Point2D): Rect2D {
        var minX = min(p0.x, p3.x)
        var maxX = max(p0.x, p3.x)
        var minY = min(p0.y, p3.y)
        var maxY = max(p0.y, p3.y)

        // Find extrema by setting derivative to 0: a*t^2 + b*t + c = 0
        fun checkRoots(v0: Float, v1: Float, v2: Float, v3: Float, onRoot: (Float) -> Unit) {
            val a = 3f * (-v0 + 3f * v1 - 3f * v2 + v3)
            val b = 6f * (v0 - 2f * v1 + v2)
            val c = 3f * (v1 - v0)
            if (abs(a) < 1e-6f) {
                if (abs(b) > 1e-6f) {
                    val t = -c / b
                    if (t in 0f..1f) onRoot(t)
                }
            } else {
                val discr = b * b - 4f * a * c
                if (discr >= 0f) {
                    val sqrtD = sqrt(discr)
                    val t1 = (-b + sqrtD) / (2f * a)
                    val t2 = (-b - sqrtD) / (2f * a)
                    if (t1 in 0f..1f) onRoot(t1)
                    if (t2 in 0f..1f) onRoot(t2)
                }
            }
        }

        checkRoots(p0.x, p1.x, p2.x, p3.x) { t ->
            val pt = evalCubic(p0, p1, p2, p3, t)
            minX = min(minX, pt.x)
            maxX = max(maxX, pt.x)
        }
        checkRoots(p0.y, p1.y, p2.y, p3.y) { t ->
            val pt = evalCubic(p0, p1, p2, p3, t)
            minY = min(minY, pt.y)
            maxY = max(maxY, pt.y)
        }

        return Rect2D(minX, minY, maxX, maxY)
    }

    /**
     * Check if point is close to cubic curve (within threshold distance)
     */
    fun isPointNearCubic(
        pt: Point2D,
        p0: Point2D,
        p1: Point2D,
        p2: Point2D,
        p3: Point2D,
        threshold: Float = 16f
    ): Boolean {
        val bounds = cubicBounds(p0, p1, p2, p3).inflate(threshold)
        if (!bounds.contains(pt)) return false

        // Sample 12 intervals along the curve for fast hit-testing
        val samples = 12
        for (i in 0..samples) {
            val t = i.toFloat() / samples
            val samplePt = evalCubic(p0, p1, p2, p3, t)
            if (pt.distanceTo(samplePt) <= threshold) {
                return true
            }
        }
        return false
    }

    /**
     * Generate regular polygon vertices
     */
    fun createPolygonSegments(center: Point2D, radius: Float, sides: Int): List<PathSegment> {
        val n = sides.coerceIn(3, 32)
        val segments = mutableListOf<PathSegment>()
        val angleStep = (2f * Math.PI.toFloat()) / n
        val startAngle = -Math.PI.toFloat() / 2f // Top vertex first

        for (i in 0 until n) {
            val angle = startAngle + i * angleStep
            val x = center.x + radius * cos(angle)
            val y = center.y + radius * sin(angle)
            segments.add(PathSegment(Point2D(x, y)))
        }
        return segments
    }

    /**
     * Generate star vertices
     */
    fun createStarSegments(
        center: Point2D,
        outerRadius: Float,
        innerRadiusRatio: Float = 0.5f,
        points: Int = 5
    ): List<PathSegment> {
        val numPoints = points.coerceIn(3, 32)
        val innerRadius = outerRadius * innerRadiusRatio.coerceIn(0.1f, 0.9f)
        val segments = mutableListOf<PathSegment>()
        val totalVertices = numPoints * 2
        val angleStep = (2f * Math.PI.toFloat()) / totalVertices
        val startAngle = -Math.PI.toFloat() / 2f

        for (i in 0 until totalVertices) {
            val angle = startAngle + i * angleStep
            val r = if (i % 2 == 0) outerRadius else innerRadius
            val x = center.x + r * cos(angle)
            val y = center.y + r * sin(angle)
            segments.add(PathSegment(Point2D(x, y)))
        }
        return segments
    }

    /**
     * Generate Archimedean spiral segments
     */
    fun createSpiralSegments(
        center: Point2D,
        maxRadius: Float,
        rotations: Float = 2.5f,
        stepCount: Int = 36
    ): List<PathSegment> {
        val segments = mutableListOf<PathSegment>()
        val maxAngle = rotations * 2f * Math.PI.toFloat()
        val angleStep = maxAngle / stepCount

        for (i in 0..stepCount) {
            val theta = i * angleStep
            val r = maxRadius * (theta / maxAngle)
            val x = center.x + r * cos(theta)
            val y = center.y + r * sin(theta)
            segments.add(PathSegment(Point2D(x, y)))
        }
        return segments
    }
}
