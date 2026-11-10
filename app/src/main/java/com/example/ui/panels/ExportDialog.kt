package com.example.ui.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.export.VectorExporter
import com.example.model.ColorMode
import com.example.viewmodel.EditorUiState
import com.example.viewmodel.EditorViewModel

@Composable
fun ExportDialog(
    viewModel: EditorViewModel,
    uiState: EditorUiState,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedFormat by remember { mutableStateOf(VectorExporter.ExportFormat.PNG) }
    var selectedPpi by remember { mutableIntStateOf(300) }
    var selectedScale by remember { mutableFloatStateOf(1.0f) }
    var includeBleed by remember { mutableStateOf(true) }
    var includeCropMarks by remember { mutableStateOf(true) }

    val activeArtboard = uiState.document.activeArtboard()
    val (wPx, hPx) = VectorExporter.calculatePixelSize(activeArtboard, selectedPpi, selectedScale)
    val memoryEstimateMb = VectorExporter.estimateMemoryMb(wPx, hPx)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        color = Color(0xFF161923),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2A2E3D))
    ) {
        Column(
            modifier = Modifier
                .padding(18.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = Color(0xFF00E5FF))
                    Text("Export Artwork", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF90A4AE))
                }
            }

            // Format Selection
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Export Format", color = Color(0xFFB0BEC5), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    VectorExporter.ExportFormat.values().forEach { fmt ->
                        val isSelected = selectedFormat == fmt
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedFormat = fmt }
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) Color(0xFF00E5FF) else Color(0xFF2A2E3D),
                                    shape = RoundedCornerShape(8.dp)
                                ),
                            color = if (isSelected) Color(0xFF00E5FF).copy(alpha = 0.15f) else Color(0xFF1E222D)
                        ) {
                            Text(
                                text = fmt.name,
                                color = if (isSelected) Color(0xFF00E5FF) else Color.White,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier
                                    .padding(vertical = 8.dp)
                                    .wrapContentWidth(Alignment.CenterHorizontally)
                            )
                        }
                    }
                }
            }

            // Resolution / PPI Preset (for Bitmaps and PDF)
            if (selectedFormat != VectorExporter.ExportFormat.SVG) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Raster Resolution (PPI)", color = Color(0xFFB0BEC5), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(72 to "72 ppi (Screen)", 150 to "150 ppi (Web)", 300 to "300 ppi (Print)").forEach { (ppi, label) ->
                            val isSelected = selectedPpi == ppi
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { selectedPpi = ppi }
                                    .border(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) Color(0xFF00E5FF) else Color(0xFF2A2E3D),
                                        shape = RoundedCornerShape(8.dp)
                                    ),
                                color = if (isSelected) Color(0xFF00E5FF).copy(alpha = 0.15f) else Color(0xFF1E222D)
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) Color(0xFF00E5FF) else Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier
                                        .padding(vertical = 8.dp)
                                        .wrapContentWidth(Alignment.CenterHorizontally)
                                )
                            }
                        }
                    }
                }
            }

            // PDF Print Options
            if (selectedFormat == VectorExporter.ExportFormat.PDF) {
                Surface(
                    color = Color(0xFF1E222D),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2A2E3D))
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Print Press Marks", color = Color(0xFF00E5FF), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Include 3mm Bleed Area", color = Color.White, fontSize = 12.sp)
                            Switch(
                                checked = includeBleed,
                                onCheckedChange = { includeBleed = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00E5FF))
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Add Corner Crop & Registration Marks", color = Color.White, fontSize = 12.sp)
                            Switch(
                                checked = includeCropMarks,
                                onCheckedChange = { includeCropMarks = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00E5FF))
                            )
                        }
                    }
                }
            }

            // Output Dimensions & Memory Readout Card
            Surface(
                color = Color(0xFF1E222D),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Artboard: ${activeArtboard.name}", color = Color(0xFF90A4AE), fontSize = 11.sp)
                        Text(
                            text = if (uiState.document.colorMode == ColorMode.CMYK) "CMYK Profile: SWOP" else "Color: sRGB",
                            color = Color(0xFFB388FF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (selectedFormat != VectorExporter.ExportFormat.SVG) {
                        Text("Pixel Dimensions: $wPx × $hPx px", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Estimated RAM footprint: %.2f MB".format(memoryEstimateMb), color = Color(0xFF00E5FF), fontSize = 11.sp)
                    } else {
                        Text("Vector Output: Resolution-independent SVG", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Export Result Notification if available
            if (uiState.exportResult != null) {
                Surface(
                    color = Color(0xFF00E5FF).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Export Complete!", color = Color(0xFF00E5FF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("Size: ${uiState.exportResult.fileSizeFormatted} (${uiState.exportResult.widthPx}x${uiState.exportResult.heightPx})", color = Color.White, fontSize = 11.sp)
                        }
                        Button(
                            onClick = {
                                VectorExporter.shareFile(
                                    context = context,
                                    file = uiState.exportResult.file,
                                    mimeType = selectedFormat.mimeType,
                                    title = "Share ${uiState.document.name}"
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF), contentColor = Color(0xFF00363A))
                        ) {
                            Text("Share File", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Action Button
            Button(
                onClick = {
                    val config = VectorExporter.ExportConfig(
                        format = selectedFormat,
                        ppi = selectedPpi,
                        scale = selectedScale,
                        includeBleed = includeBleed,
                        includeCropMarks = includeCropMarks,
                        artboardId = activeArtboard.id
                    )
                    viewModel.exportCurrentArtwork(config)
                },
                enabled = !uiState.isExporting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("action_export_generate"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00E5FF),
                    contentColor = Color(0xFF00363A)
                )
            ) {
                if (uiState.isExporting) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color(0xFF00363A))
                    Spacer(Modifier.width(8.dp))
                    Text("Rendering Artwork...", fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Generate & Share ${selectedFormat.name}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}
