package com.example.ui.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.viewmodel.EditorUiState
import com.example.viewmodel.EditorViewModel
import com.example.viewmodel.PaintTarget
import kotlin.math.roundToInt

@Composable
fun ColorPanel(
    viewModel: EditorViewModel,
    uiState: EditorUiState,
    modifier: Modifier = Modifier
) {
    var colorSpaceMode by remember {
        mutableStateOf(if (uiState.document.colorMode == ColorMode.CMYK) ColorMode.CMYK else ColorMode.RGB)
    }

    // Retrieve active color based on target (Fill vs Stroke)
    val activePaint = if (uiState.activePaintTarget == PaintTarget.FILL) uiState.activeFill else uiState.activeStroke
    val activeColor = when (activePaint) {
        is VpPaint.Solid -> activePaint.color
        is VpPaint.Linear -> activePaint.stops.firstOrNull()?.color ?: VpColor.rgb(0, 229, 255)
        else -> VpColor.rgb(0, 229, 255)
    }

    // Local slider values
    var cmykC by remember(activeColor) {
        val c = if (activeColor.mode == ColorMode.CMYK) activeColor.values.getOrElse(0) { 0f }
        else ColorEngine.rgbToCmyk(activeColor.values.getOrElse(0) { 0f }, activeColor.values.getOrElse(1) { 0f }, activeColor.values.getOrElse(2) { 0f })[0]
        mutableFloatStateOf(c)
    }
    var cmykM by remember(activeColor) {
        val m = if (activeColor.mode == ColorMode.CMYK) activeColor.values.getOrElse(1) { 0f }
        else ColorEngine.rgbToCmyk(activeColor.values.getOrElse(0) { 0f }, activeColor.values.getOrElse(1) { 0f }, activeColor.values.getOrElse(2) { 0f })[1]
        mutableFloatStateOf(m)
    }
    var cmykY by remember(activeColor) {
        val y = if (activeColor.mode == ColorMode.CMYK) activeColor.values.getOrElse(2) { 0f }
        else ColorEngine.rgbToCmyk(activeColor.values.getOrElse(0) { 0f }, activeColor.values.getOrElse(1) { 0f }, activeColor.values.getOrElse(2) { 0f })[2]
        mutableFloatStateOf(y)
    }
    var cmykK by remember(activeColor) {
        val k = if (activeColor.mode == ColorMode.CMYK) activeColor.values.getOrElse(3) { 0f }
        else ColorEngine.rgbToCmyk(activeColor.values.getOrElse(0) { 0f }, activeColor.values.getOrElse(1) { 0f }, activeColor.values.getOrElse(2) { 0f })[3]
        mutableFloatStateOf(k)
    }

    var rgbR by remember(activeColor) {
        val r = if (activeColor.mode == ColorMode.RGB) activeColor.values.getOrElse(0) { 0f }
        else (ColorEngine.toComposeColor(activeColor, uiState.document.profile).red * 255f)
        mutableFloatStateOf(r)
    }
    var rgbG by remember(activeColor) {
        val g = if (activeColor.mode == ColorMode.RGB) activeColor.values.getOrElse(1) { 0f }
        else (ColorEngine.toComposeColor(activeColor, uiState.document.profile).green * 255f)
        mutableFloatStateOf(g)
    }
    var rgbB by remember(activeColor) {
        val b = if (activeColor.mode == ColorMode.RGB) activeColor.values.getOrElse(2) { 0f }
        else (ColorEngine.toComposeColor(activeColor, uiState.document.profile).blue * 255f)
        mutableFloatStateOf(b)
    }

    val totalInk = ColorEngine.getTotalInkCoverage(cmykC, cmykM, cmykY, cmykK)
    val isOutOfGamut = ColorEngine.isOutOfGamut(rgbR, rgbG, rgbB)

    val currentComposeColor = if (colorSpaceMode == ColorMode.CMYK) {
        ColorEngine.cmykToRgb(cmykC, cmykM, cmykY, cmykK, uiState.document.profile)
    } else {
        Color((rgbR / 255f).coerceIn(0f, 1f), (rgbG / 255f).coerceIn(0f, 1f), (rgbB / 255f).coerceIn(0f, 1f))
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Header: Target Switcher (Fill vs Stroke) & Color Preview
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = uiState.activePaintTarget == PaintTarget.FILL,
                    onClick = { viewModel.setPaintTarget(PaintTarget.FILL) },
                    label = { Text("Fill") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF00E5FF).copy(alpha = 0.25f),
                        selectedLabelColor = Color(0xFF00E5FF)
                    )
                )
                FilterChip(
                    selected = uiState.activePaintTarget == PaintTarget.STROKE,
                    onClick = { viewModel.setPaintTarget(PaintTarget.STROKE) },
                    label = { Text("Stroke") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF00E5FF).copy(alpha = 0.25f),
                        selectedLabelColor = Color(0xFF00E5FF)
                    )
                )
            }

            // Preview Swatch + Hex/CMYK readout
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (colorSpaceMode == ColorMode.CMYK) "C:${cmykC.roundToInt()}% M:${cmykM.roundToInt()}% Y:${cmykY.roundToInt()}% K:${cmykK.roundToInt()}%"
                    else ColorEngine.toHex(rgbR.roundToInt(), rgbG.roundToInt(), rgbB.roundToInt()),
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(currentComposeColor)
                        .border(1.dp, Color(0xFF454D60), RoundedCornerShape(8.dp))
                )
            }
        }

        // Color Mode Switcher (CMYK / RGB tabs)
        TabRow(
            selectedTabIndex = if (colorSpaceMode == ColorMode.CMYK) 0 else 1,
            containerColor = Color(0xFF1E222D),
            contentColor = Color(0xFF00E5FF)
        ) {
            Tab(
                selected = colorSpaceMode == ColorMode.CMYK,
                onClick = { colorSpaceMode = ColorMode.CMYK },
                text = { Text("CMYK Sliders") }
            )
            Tab(
                selected = colorSpaceMode == ColorMode.RGB,
                onClick = { colorSpaceMode = ColorMode.RGB },
                text = { Text("RGB Sliders") }
            )
        }

        // Sliders
        if (colorSpaceMode == ColorMode.CMYK) {
            ColorSliderRow("Cyan", cmykC, Color(0xFF00E5FF)) {
                cmykC = it
                viewModel.updateSelectedColor(VpColor.cmyk(cmykC, cmykM, cmykY, cmykK))
            }
            ColorSliderRow("Magenta", cmykM, Color(0xFFFF4081)) {
                cmykM = it
                viewModel.updateSelectedColor(VpColor.cmyk(cmykC, cmykM, cmykY, cmykK))
            }
            ColorSliderRow("Yellow", cmykY, Color(0xFFFFEB3B)) {
                cmykY = it
                viewModel.updateSelectedColor(VpColor.cmyk(cmykC, cmykM, cmykY, cmykK))
            }
            ColorSliderRow("Black (K)", cmykK, Color(0xFF90A4AE)) {
                cmykK = it
                viewModel.updateSelectedColor(VpColor.cmyk(cmykC, cmykM, cmykY, cmykK))
            }

            // Total Ink Limit Gauge
            Surface(
                color = if (totalInk > 300f) Color(0xFFFF9100).copy(alpha = 0.15f) else Color(0xFF1E222D),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (totalInk > 300f) Color(0xFFFF9100) else Color(0xFF2A2E3D))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (totalInk > 300f) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFF9100), modifier = Modifier.size(16.dp))
                        }
                        Text(
                            text = if (totalInk > 300f) "Ink Coverage: ${totalInk.roundToInt()}% (Exceeds 300% limit!)" else "Total Ink: ${totalInk.roundToInt()}% / 300%",
                            color = if (totalInk > 300f) Color(0xFFFFB74D) else Color(0xFF90A4AE),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Pure vs Rich black quick buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TextButton(
                            onClick = {
                                cmykC = 0f; cmykM = 0f; cmykY = 0f; cmykK = 100f
                                viewModel.updateSelectedColor(VpColor.pureBlack())
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("100K", fontSize = 10.sp, color = Color.White)
                        }
                        TextButton(
                            onClick = {
                                cmykC = 60f; cmykM = 40f; cmykY = 40f; cmykK = 100f
                                viewModel.updateSelectedColor(VpColor.richBlack())
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Rich Black", fontSize = 10.sp, color = Color(0xFF00E5FF))
                        }
                    }
                }
            }
        } else {
            // RGB Sliders
            ColorSliderRow("Red", rgbR, Color(0xFFFF5252), maxVal = 255f) {
                rgbR = it
                viewModel.updateSelectedColor(VpColor.rgb(rgbR.roundToInt(), rgbG.roundToInt(), rgbB.roundToInt()))
            }
            ColorSliderRow("Green", rgbG, Color(0xFF69F0AE), maxVal = 255f) {
                rgbG = it
                viewModel.updateSelectedColor(VpColor.rgb(rgbR.roundToInt(), rgbG.roundToInt(), rgbB.roundToInt()))
            }
            ColorSliderRow("Blue", rgbB, Color(0xFF448AFF), maxVal = 255f) {
                rgbB = it
                viewModel.updateSelectedColor(VpColor.rgb(rgbR.roundToInt(), rgbG.roundToInt(), rgbB.roundToInt()))
            }

            // Out-of-Gamut Warning
            if (isOutOfGamut) {
                Surface(
                    color = Color(0xFFFF5722).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF5722))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFF7043), modifier = Modifier.size(16.dp))
                        Text(
                            text = "Out of Gamut: Color will shift noticeably when printed in CMYK",
                            color = Color(0xFFFFAB91),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorSliderRow(
    label: String,
    value: Float,
    tint: Color,
    maxVal: Float = 100f,
    onValueChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = Color(0xFFB0BEC5), fontSize = 12.sp)
            Text("${value.roundToInt()}${if (maxVal == 100f) "%" else ""}", color = tint, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..maxVal,
            colors = SliderDefaults.colors(
                thumbColor = tint,
                activeTrackColor = tint,
                inactiveTrackColor = Color(0xFF2A2E3D)
            ),
            modifier = Modifier.height(24.dp)
        )
    }
}
