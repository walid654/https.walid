package com.example.model

enum class ColorMode {
    RGB,
    CMYK,
    GRAY,
    SPOT
}

enum class ColorProfile(val displayName: String, val isCmyk: Boolean) {
    SRGB("sRGB (Standard Screen)", false),
    US_WEB_COATED_SWOP("U.S. Web Coated (SWOP) v2", true),
    FOGRA39("FOGRA39 (ISO 12647-2:2004)", true),
    JAPAN_COLOR("Japan Color 2001 Coated", true)
}

enum class DocumentUnit(val symbol: String, val pointsPerUnit: Float, val displayName: String) {
    PX("px", 0.75f, "Pixels"),
    PT("pt", 1.0f, "Points"),
    PC("pc", 12.0f, "Picas"),
    IN("in", 72.0f, "Inches"),
    FT("ft", 864.0f, "Feet"),
    MM("mm", 2.83465f, "Millimeters"),
    CM("cm", 28.3465f, "Centimeters")
}

data class VpColor(
    val mode: ColorMode = ColorMode.RGB,
    /**
     * RGB: [r, g, b] in 0..255
     * CMYK: [c, m, y, k] in 0..100
     * GRAY: [gray] in 0..100
     * SPOT: [c, m, y, k] in 0..100 (press alternate)
     */
    val values: List<Float> = listOf(0f, 0f, 0f),
    val alpha: Float = 1.0f,
    val swatchId: String? = null,
    val spotName: String? = null
) {
    companion object {
        fun rgb(r: Int, g: Int, b: Int, alpha: Float = 1.0f) = VpColor(
            mode = ColorMode.RGB,
            values = listOf(r.toFloat().coerceIn(0f, 255f), g.toFloat().coerceIn(0f, 255f), b.toFloat().coerceIn(0f, 255f)),
            alpha = alpha.coerceIn(0f, 1f)
        )

        fun cmyk(c: Float, m: Float, y: Float, k: Float, alpha: Float = 1.0f) = VpColor(
            mode = ColorMode.CMYK,
            values = listOf(c.coerceIn(0f, 100f), m.coerceIn(0f, 100f), y.coerceIn(0f, 100f), k.coerceIn(0f, 100f)),
            alpha = alpha.coerceIn(0f, 1f)
        )

        fun pureBlack() = cmyk(0f, 0f, 0f, 100f)
        fun richBlack() = cmyk(60f, 40f, 40f, 100f) // 240% ink coverage
        fun white() = rgb(255, 255, 255)
        fun black() = rgb(0, 0, 0)
        fun transparent() = VpColor(ColorMode.RGB, listOf(0f, 0f, 0f), 0f)
    }

    /**
     * Total ink coverage for CMYK (0 to 400%)
     */
    val totalInkLimit: Float
        get() = if (mode == ColorMode.CMYK || mode == ColorMode.SPOT) {
            values.take(4).sum()
        } else {
            0f
        }
}

data class GradientStop(
    val offset: Float, // 0.0 to 1.0
    val color: VpColor
)

sealed class VpPaint {
    data object None : VpPaint()
    data class Solid(val color: VpColor) : VpPaint()
    data class Linear(
        val stops: List<GradientStop> = listOf(
            GradientStop(0f, VpColor.rgb(0, 229, 255)),
            GradientStop(1f, VpColor.rgb(179, 136, 255))
        ),
        val angleDegrees: Float = 45f
    ) : VpPaint()
    data class Radial(
        val stops: List<GradientStop> = listOf(
            GradientStop(0f, VpColor.rgb(255, 255, 255)),
            GradientStop(1f, VpColor.rgb(30, 34, 45))
        ),
        val radiusRatio: Float = 1.0f
    ) : VpPaint()
}

enum class StrokeCap {
    BUTT,
    ROUND,
    SQUARE
}

enum class StrokeJoin {
    MITER,
    ROUND,
    BEVEL
}

enum class StrokeAlignment {
    CENTER,
    INSIDE,
    OUTSIDE
}

data class StrokeStyle(
    val width: Float = 2.0f,
    val cap: StrokeCap = StrokeCap.ROUND,
    val join: StrokeJoin = StrokeJoin.ROUND,
    val miterLimit: Float = 4.0f,
    val dashes: List<Float>? = null,
    val dashPhase: Float = 0f,
    val alignment: StrokeAlignment = StrokeAlignment.CENTER
)

enum class BlendMode {
    NORMAL,
    MULTIPLY,
    SCREEN,
    OVERLAY,
    DARKEN,
    LIGHTEN,
    COLOR_DODGE,
    COLOR_BURN
}
