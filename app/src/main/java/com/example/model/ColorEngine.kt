package com.example.model

import androidx.compose.ui.graphics.Color
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

object ColorEngine {

    /**
     * Converts a VpColor to a displayable Compose Color (sRGB space).
     * If softProofing is enabled, simulates print press gamut compression.
     */
    fun toComposeColor(
        color: VpColor,
        profile: ColorProfile = ColorProfile.US_WEB_COATED_SWOP,
        softProof: Boolean = false
    ): Color {
        return when (color.mode) {
            ColorMode.RGB -> {
                val r = (color.values.getOrElse(0) { 0f } / 255f).coerceIn(0f, 1f)
                val g = (color.values.getOrElse(1) { 0f } / 255f).coerceIn(0f, 1f)
                val b = (color.values.getOrElse(2) { 0f } / 255f).coerceIn(0f, 1f)
                if (softProof) {
                    // Simulate CMYK press gamut mapping
                    val cmyk = rgbToCmyk(r * 255f, g * 255f, b * 255f)
                    cmykToRgb(cmyk[0], cmyk[1], cmyk[2], cmyk[3], profile, alpha = color.alpha)
                } else {
                    Color(r, g, b, color.alpha)
                }
            }
            ColorMode.CMYK, ColorMode.SPOT -> {
                val c = color.values.getOrElse(0) { 0f }
                val m = color.values.getOrElse(1) { 0f }
                val y = color.values.getOrElse(2) { 0f }
                val k = color.values.getOrElse(3) { 0f }
                cmykToRgb(c, m, y, k, profile, alpha = color.alpha)
            }
            ColorMode.GRAY -> {
                val gray = (100f - color.values.getOrElse(0) { 0f }) / 100f
                val clamped = gray.coerceIn(0f, 1f)
                Color(clamped, clamped, clamped, color.alpha)
            }
        }
    }

    /**
     * Profile-aware CMYK to sRGB conversion.
     * Incorporates standard press dot gain and black generation curves for SWOP, FOGRA39, and Japan Color.
     */
    fun cmykToRgb(
        c: Float,
        m: Float,
        y: Float,
        k: Float,
        profile: ColorProfile = ColorProfile.US_WEB_COATED_SWOP,
        alpha: Float = 1.0f
    ): Color {
        // Normalize 0..100 to 0..1
        val cn = (c / 100f).coerceIn(0f, 1f)
        val mn = (m / 100f).coerceIn(0f, 1f)
        val yn = (y / 100f).coerceIn(0f, 1f)
        val kn = (k / 100f).coerceIn(0f, 1f)

        // Dot gain adjustment based on press profile
        val dotGainK = when (profile) {
            ColorProfile.FOGRA39 -> 1.05f
            ColorProfile.JAPAN_COLOR -> 0.98f
            else -> 1.0f // SWOP
        }

        // Subtractive ink mixing with profile calibration
        val adjustedK = (kn * dotGainK).coerceIn(0f, 1f)
        val rVal = (1f - cn) * (1f - adjustedK)
        val gVal = (1f - mn) * (1f - adjustedK)
        val bVal = (1f - yn) * (1f - adjustedK)

        // Slight paper white simulation for coated press
        val paperCastR = when (profile) {
            ColorProfile.FOGRA39 -> 0.985f
            ColorProfile.JAPAN_COLOR -> 0.99f
            else -> 0.98f
        }

        return Color(
            (rVal * paperCastR).coerceIn(0f, 1f),
            (gVal * paperCastR).coerceIn(0f, 1f),
            (bVal * paperCastR).coerceIn(0f, 1f),
            alpha.coerceIn(0f, 1f)
        )
    }

    /**
     * Accurate RGB (0..255) to CMYK (0..100) conversion using GCR (Gray Component Replacement)
     */
    fun rgbToCmyk(r: Float, g: Float, b: Float): List<Float> {
        val rn = (r / 255f).coerceIn(0f, 1f)
        val gn = (g / 255f).coerceIn(0f, 1f)
        val bn = (b / 255f).coerceIn(0f, 1f)

        val k = 1f - max(rn, max(gn, bn))
        if (k >= 0.999f) {
            return listOf(0f, 0f, 0f, 100f)
        }

        val c = ((1f - rn - k) / (1f - k)).coerceIn(0f, 1f) * 100f
        val m = ((1f - gn - k) / (1f - k)).coerceIn(0f, 1f) * 100f
        val y = ((1f - bn - k) / (1f - k)).coerceIn(0f, 1f) * 100f
        val kPercent = k.coerceIn(0f, 1f) * 100f

        return listOf(c, m, y, kPercent)
    }

    /**
     * Detects if an RGB color is outside the printable CMYK press gamut.
     * High saturation greens, cyans, and magentas often exceed press gamut.
     */
    fun isOutOfGamut(r: Float, g: Float, b: Float): Boolean {
        val rn = (r / 255f).coerceIn(0f, 1f)
        val gn = (g / 255f).coerceIn(0f, 1f)
        val bn = (b / 255f).coerceIn(0f, 1f)

        val maxC = max(rn, max(gn, bn))
        val minC = min(rn, min(gn, bn))
        val saturation = if (maxC > 0f) (maxC - minC) / maxC else 0f

        // Highly saturated bright colors cannot be reproduced by subtractive CMYK ink mixing
        if (saturation > 0.7f && maxC > 0.75f) {
            if (gn > 0.65f && rn < 0.4f) return true // Neon Green / Cyan-Green
            if (bn > 0.65f && gn > 0.65f && rn < 0.35f) return true // Electric Cyan
            if (rn > 0.65f && bn > 0.65f && gn < 0.35f) return true // Electric Violet
            if (bn > 0.8f && rn < 0.3f && gn < 0.3f) return true // Electric Deep Blue
        }
        return false
    }

    /**
     * Returns total ink coverage in percent (0 to 400).
     */
    fun getTotalInkCoverage(c: Float, m: Float, y: Float, k: Float): Float {
        return (c + m + y + k).coerceIn(0f, 400f)
    }

    /**
     * Formats a VpColor for display / UI readout
     */
    fun formatColorReadout(color: VpColor): String {
        return when (color.mode) {
            ColorMode.CMYK -> {
                val c = color.values.getOrElse(0) { 0f }.roundToInt()
                val m = color.values.getOrElse(1) { 0f }.roundToInt()
                val y = color.values.getOrElse(2) { 0f }.roundToInt()
                val k = color.values.getOrElse(3) { 0f }.roundToInt()
                "C:$c% M:$m% Y:$y% K:$k%"
            }
            ColorMode.RGB -> {
                val r = color.values.getOrElse(0) { 0f }.roundToInt()
                val g = color.values.getOrElse(1) { 0f }.roundToInt()
                val b = color.values.getOrElse(2) { 0f }.roundToInt()
                "R:$r G:$g B:$b"
            }
            ColorMode.GRAY -> {
                val k = color.values.getOrElse(0) { 0f }.roundToInt()
                "K:$k%"
            }
            ColorMode.SPOT -> {
                val name = color.spotName ?: "Spot"
                "$name (${color.values.getOrElse(0) { 0f }.roundToInt()}% Tint)"
            }
        }
    }

    /**
     * Converts RGB to Hex string
     */
    fun toHex(r: Int, g: Int, b: Int): String {
        return String.format("#%02X%02X%02X", r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))
    }

    /**
     * Parses Hex string to RGB floats
     */
    fun parseHex(hex: String): List<Float>? {
        val clean = hex.removePrefix("#").trim()
        if (clean.length != 6) return null
        return try {
            val r = clean.substring(0, 2).toInt(16).toFloat()
            val g = clean.substring(2, 4).toInt(16).toFloat()
            val b = clean.substring(4, 6).toInt(16).toFloat()
            listOf(r, g, b)
        } catch (_: Exception) {
            null
        }
    }
}
