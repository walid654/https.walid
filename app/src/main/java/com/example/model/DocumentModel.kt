package com.example.model

import java.util.UUID
import kotlin.math.*


enum class ShapeType(val displayName: String) {
    RECTANGLE("Rectangle"),
    ROUNDED_RECT("Rounded Rectangle"),
    ELLIPSE("Ellipse"),
    POLYGON("Polygon"),
    STAR("Star"),
    LINE("Line"),
    SPIRAL("Spiral")
}

sealed interface VectorElement {
    val id: String
    val name: String
    val fill: VpPaint
    val stroke: VpPaint
    val strokeStyle: StrokeStyle
    val opacity: Float
    val blendMode: BlendMode
    val transform: Matrix2D
    val isLocked: Boolean
    val isHidden: Boolean

    fun getBoundingBox(): Rect2D
    fun withTransform(newTransform: Matrix2D): VectorElement
    fun withFill(newFill: VpPaint): VectorElement
    fun withStroke(newStroke: VpPaint, newStyle: StrokeStyle? = null): VectorElement
    fun withOpacity(newOpacity: Float): VectorElement
    fun withLocked(locked: Boolean): VectorElement
    fun withHidden(hidden: Boolean): VectorElement
    fun toPathSegments(): List<PathSegment>
}

data class PathElement(
    override val id: String = UUID.randomUUID().toString(),
    override val name: String = "Path",
    val segments: List<PathSegment> = emptyList(),
    val closed: Boolean = true,
    override val fill: VpPaint = VpPaint.Solid(VpColor.rgb(0, 229, 255)),
    override val stroke: VpPaint = VpPaint.Solid(VpColor.rgb(30, 34, 45)),
    override val strokeStyle: StrokeStyle = StrokeStyle(width = 2f),
    override val opacity: Float = 1.0f,
    override val blendMode: BlendMode = BlendMode.NORMAL,
    override val transform: Matrix2D = Matrix2D.IDENTITY,
    override val isLocked: Boolean = false,
    override val isHidden: Boolean = false
) : VectorElement {

    override fun getBoundingBox(): Rect2D {
        if (segments.isEmpty()) return Rect2D()
        val mappedPoints = segments.map { transform.mapPoint(it.anchor) }
        var minX = mappedPoints.minOf { it.x }
        var maxX = mappedPoints.maxOf { it.x }
        var minY = mappedPoints.minOf { it.y }
        var maxY = mappedPoints.maxOf { it.y }

        // Expand slightly for handles and stroke
        val strokeExpand = strokeStyle.width / 2f
        return Rect2D(minX - strokeExpand, minY - strokeExpand, maxX + strokeExpand, maxY + strokeExpand)
    }

    override fun withTransform(newTransform: Matrix2D) = copy(transform = newTransform)
    override fun withFill(newFill: VpPaint) = copy(fill = newFill)
    override fun withStroke(newStroke: VpPaint, newStyle: StrokeStyle?) =
        copy(stroke = newStroke, strokeStyle = newStyle ?: strokeStyle)
    override fun withOpacity(newOpacity: Float) = copy(opacity = newOpacity)
    override fun withLocked(locked: Boolean) = copy(isLocked = locked)
    override fun withHidden(hidden: Boolean) = copy(isHidden = hidden)
    override fun toPathSegments(): List<PathSegment> = segments
}

data class ShapeElement(
    override val id: String = UUID.randomUUID().toString(),
    override val name: String = "Shape",
    val shapeType: ShapeType = ShapeType.RECTANGLE,
    val bounds: Rect2D = Rect2D(0f, 0f, 100f, 100f),
    val cornerRadius: Float = 16f,
    val sides: Int = 6,
    val starPoints: Int = 5,
    val innerRadiusRatio: Float = 0.5f,
    override val fill: VpPaint = VpPaint.Solid(VpColor.rgb(0, 229, 255)),
    override val stroke: VpPaint = VpPaint.Solid(VpColor.rgb(20, 23, 31)),
    override val strokeStyle: StrokeStyle = StrokeStyle(width = 2f),
    override val opacity: Float = 1.0f,
    override val blendMode: BlendMode = BlendMode.NORMAL,
    override val transform: Matrix2D = Matrix2D.IDENTITY,
    override val isLocked: Boolean = false,
    override val isHidden: Boolean = false
) : VectorElement {

    override fun getBoundingBox(): Rect2D {
        val p1 = transform.mapPoint(Point2D(bounds.left, bounds.top))
        val p2 = transform.mapPoint(Point2D(bounds.right, bounds.top))
        val p3 = transform.mapPoint(Point2D(bounds.right, bounds.bottom))
        val p4 = transform.mapPoint(Point2D(bounds.left, bounds.bottom))
        val pts = listOf(p1, p2, p3, p4)
        val strokeExpand = strokeStyle.width / 2f
        return Rect2D(
            pts.minOf { it.x } - strokeExpand,
            pts.minOf { it.y } - strokeExpand,
            pts.maxOf { it.x } + strokeExpand,
            pts.maxOf { it.y } + strokeExpand
        )
    }

    override fun withTransform(newTransform: Matrix2D) = copy(transform = newTransform)
    override fun withFill(newFill: VpPaint) = copy(fill = newFill)
    override fun withStroke(newStroke: VpPaint, newStyle: StrokeStyle?) =
        copy(stroke = newStroke, strokeStyle = newStyle ?: strokeStyle)
    override fun withOpacity(newOpacity: Float) = copy(opacity = newOpacity)
    override fun withLocked(locked: Boolean) = copy(isLocked = locked)
    override fun withHidden(hidden: Boolean) = copy(isHidden = hidden)

    override fun toPathSegments(): List<PathSegment> {
        val center = Point2D(bounds.centerX, bounds.centerY)
        val radius = min(bounds.width, bounds.height) / 2f
        return when (shapeType) {
            ShapeType.RECTANGLE -> listOf(
                PathSegment(Point2D(bounds.left, bounds.top)),
                PathSegment(Point2D(bounds.right, bounds.top)),
                PathSegment(Point2D(bounds.right, bounds.bottom)),
                PathSegment(Point2D(bounds.left, bounds.bottom))
            )
            ShapeType.ROUNDED_RECT -> {
                val r = cornerRadius.coerceIn(0f, min(bounds.width, bounds.height) / 2f)
                val k = r * 0.5522847498f // Bezier circle approximation constant
                listOf(
                    PathSegment(Point2D(bounds.left + r, bounds.top), Point2D(-k, 0f), Point2D(0f, 0f)),
                    PathSegment(Point2D(bounds.right - r, bounds.top), Point2D(0f, 0f), Point2D(k, 0f)),
                    PathSegment(Point2D(bounds.right, bounds.top + r), Point2D(0f, -k), Point2D(0f, 0f)),
                    PathSegment(Point2D(bounds.right, bounds.bottom - r), Point2D(0f, 0f), Point2D(0f, k)),
                    PathSegment(Point2D(bounds.right - r, bounds.bottom), Point2D(k, 0f), Point2D(0f, 0f)),
                    PathSegment(Point2D(bounds.left + r, bounds.bottom), Point2D(0f, 0f), Point2D(-k, 0f)),
                    PathSegment(Point2D(bounds.left, bounds.bottom - r), Point2D(0f, k), Point2D(0f, 0f)),
                    PathSegment(Point2D(bounds.left, bounds.top + r), Point2D(0f, 0f), Point2D(0f, -k))
                )
            }
            ShapeType.ELLIPSE -> {
                val rx = bounds.width / 2f
                val ry = bounds.height / 2f
                val kx = rx * 0.5522847498f
                val ky = ry * 0.5522847498f
                listOf(
                    PathSegment(Point2D(center.x, bounds.top), Point2D(-kx, 0f), Point2D(kx, 0f)),
                    PathSegment(Point2D(bounds.right, center.y), Point2D(0f, -ky), Point2D(0f, ky)),
                    PathSegment(Point2D(center.x, bounds.bottom), Point2D(kx, 0f), Point2D(-kx, 0f)),
                    PathSegment(Point2D(bounds.left, center.y), Point2D(0f, ky), Point2D(0f, -ky))
                )
            }
            ShapeType.POLYGON -> BezierMath.createPolygonSegments(center, radius, sides)
            ShapeType.STAR -> BezierMath.createStarSegments(center, radius, innerRadiusRatio, starPoints)
            ShapeType.LINE -> listOf(
                PathSegment(Point2D(bounds.left, bounds.top)),
                PathSegment(Point2D(bounds.right, bounds.bottom))
            )
            ShapeType.SPIRAL -> BezierMath.createSpiralSegments(center, radius)
        }
    }
}

data class TextElement(
    override val id: String = UUID.randomUUID().toString(),
    override val name: String = "Text",
    val text: String = "Vertor Phics",
    val x: Float = 0f,
    val y: Float = 0f,
    val fontSize: Float = 28f,
    val fontFamily: String = "SansSerif",
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val letterSpacing: Float = 0f,
    override val fill: VpPaint = VpPaint.Solid(VpColor.rgb(255, 255, 255)),
    override val stroke: VpPaint = VpPaint.None,
    override val strokeStyle: StrokeStyle = StrokeStyle(width = 1f),
    override val opacity: Float = 1.0f,
    override val blendMode: BlendMode = BlendMode.NORMAL,
    override val transform: Matrix2D = Matrix2D.IDENTITY,
    override val isLocked: Boolean = false,
    override val isHidden: Boolean = false
) : VectorElement {

    override fun getBoundingBox(): Rect2D {
        val approxWidth = max(text.length * fontSize * 0.55f, 40f)
        val approxHeight = fontSize * 1.2f
        val p1 = transform.mapPoint(Point2D(x, y - approxHeight))
        val p2 = transform.mapPoint(Point2D(x + approxWidth, y))
        return Rect2D.fromPoints(p1, p2)
    }

    override fun withTransform(newTransform: Matrix2D) = copy(transform = newTransform)
    override fun withFill(newFill: VpPaint) = copy(fill = newFill)
    override fun withStroke(newStroke: VpPaint, newStyle: StrokeStyle?) =
        copy(stroke = newStroke, strokeStyle = newStyle ?: strokeStyle)
    override fun withOpacity(newOpacity: Float) = copy(opacity = newOpacity)
    override fun withLocked(locked: Boolean) = copy(isLocked = locked)
    override fun withHidden(hidden: Boolean) = copy(isHidden = hidden)
    override fun toPathSegments(): List<PathSegment> = emptyList()
}

data class GroupElement(
    override val id: String = UUID.randomUUID().toString(),
    override val name: String = "Group",
    val children: List<VectorElement> = emptyList(),
    override val fill: VpPaint = VpPaint.None,
    override val stroke: VpPaint = VpPaint.None,
    override val strokeStyle: StrokeStyle = StrokeStyle(),
    override val opacity: Float = 1.0f,
    override val blendMode: BlendMode = BlendMode.NORMAL,
    override val transform: Matrix2D = Matrix2D.IDENTITY,
    override val isLocked: Boolean = false,
    override val isHidden: Boolean = false
) : VectorElement {

    override fun getBoundingBox(): Rect2D {
        if (children.isEmpty()) return Rect2D()
        val childBounds = children.map { it.getBoundingBox() }
        var result = childBounds.first()
        for (i in 1 until childBounds.size) {
            result = result.union(childBounds[i])
        }
        return result
    }

    override fun withTransform(newTransform: Matrix2D) = copy(
        transform = newTransform,
        children = children.map { it.withTransform(newTransform.multiply(it.transform)) }
    )
    override fun withFill(newFill: VpPaint) = copy(
        children = children.map { it.withFill(newFill) }
    )
    override fun withStroke(newStroke: VpPaint, newStyle: StrokeStyle?) = copy(
        children = children.map { it.withStroke(newStroke, newStyle) }
    )
    override fun withOpacity(newOpacity: Float) = copy(opacity = newOpacity)
    override fun withLocked(locked: Boolean) = copy(isLocked = locked)
    override fun withHidden(hidden: Boolean) = copy(isHidden = hidden)
    override fun toPathSegments(): List<PathSegment> = children.flatMap { it.toPathSegments() }
}

data class Layer(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Layer 1",
    val colorHex: Long = 0xFF00E5FF,
    val visible: Boolean = true,
    val locked: Boolean = false,
    val elements: List<VectorElement> = emptyList()
)

data class Bleed(
    val top: Float = 9f,    // standard 3mm in pt ~ 8.5 pt (9 pt)
    val right: Float = 9f,
    val bottom: Float = 9f,
    val left: Float = 9f
)

data class Artboard(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Artboard 1",
    val x: Float = 0f,
    val y: Float = 0f,
    val width: Float = 595.28f,  // A4 in pt: 210mm x 297mm = 595.28 x 841.89 pt
    val height: Float = 841.89f,
    val bleed: Bleed = Bleed()
) {
    fun contains(pt: Point2D): Boolean = pt.x in x..(x + width) && pt.y in y..(y + height)
    fun getBounds(): Rect2D = Rect2D(x, y, x + width, y + height)
}

data class Swatch(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val color: VpColor,
    val isGlobal: Boolean = false,
    val isSpot: Boolean = false
)

data class Document(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Untitled Artwork",
    val colorMode: ColorMode = ColorMode.RGB,
    val profile: ColorProfile = ColorProfile.SRGB,
    val rasterPpi: Int = 300,
    val units: DocumentUnit = DocumentUnit.PT,
    val artboards: List<Artboard> = listOf(Artboard()),
    val layers: List<Layer> = listOf(Layer()),
    val swatches: List<Swatch> = defaultSwatches(ColorMode.RGB),
    val selectedArtboardId: String = artboards.firstOrNull()?.id ?: "",
    val lastModified: Long = System.currentTimeMillis()
) {
    fun activeArtboard(): Artboard {
        return artboards.find { it.id == selectedArtboardId } ?: artboards.first()
    }

    companion object {
        fun defaultSwatches(mode: ColorMode): List<Swatch> {
            return if (mode == ColorMode.CMYK) {
                listOf(
                    Swatch(name = "Pure Black", color = VpColor.pureBlack(), isGlobal = true),
                    Swatch(name = "Rich Black", color = VpColor.richBlack(), isGlobal = true),
                    Swatch(name = "Process Cyan", color = VpColor.cmyk(100f, 0f, 0f, 0f), isGlobal = true),
                    Swatch(name = "Process Magenta", color = VpColor.cmyk(0f, 100f, 0f, 0f), isGlobal = true),
                    Swatch(name = "Process Yellow", color = VpColor.cmyk(0f, 0f, 100f, 0f), isGlobal = true),
                    Swatch(name = "Warm Red", color = VpColor.cmyk(0f, 85f, 80f, 0f), isGlobal = true),
                    Swatch(name = "Navy Press", color = VpColor.cmyk(100f, 75f, 0f, 40f), isGlobal = true),
                    Swatch(name = "Forest Green", color = VpColor.cmyk(80f, 0f, 90f, 30f), isGlobal = true),
                    Swatch(name = "Gold Foil Alternate", color = VpColor.cmyk(10f, 30f, 85f, 0f), isGlobal = true, isSpot = true)
                )
            } else {
                listOf(
                    Swatch(name = "Electric Cyan", color = VpColor.rgb(0, 229, 255)),
                    Swatch(name = "Neon Violet", color = VpColor.rgb(179, 136, 255)),
                    Swatch(name = "Sunset Orange", color = VpColor.rgb(255, 112, 67)),
                    Swatch(name = "Emerald Green", color = VpColor.rgb(0, 230, 118)),
                    Swatch(name = "Sunny Amber", color = VpColor.rgb(255, 215, 64)),
                    Swatch(name = "Studio Dark", color = VpColor.rgb(20, 23, 31)),
                    Swatch(name = "Pure White", color = VpColor.rgb(255, 255, 255)),
                    Swatch(name = "Slate Gray", color = VpColor.rgb(120, 144, 156))
                )
            }
        }
    }
}
