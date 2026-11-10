package com.example.ui.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import kotlin.math.roundToInt

data class DocumentPreset(
    val name: String,
    val category: String,
    val widthPt: Float,
    val heightPt: Float,
    val unit: DocumentUnit,
    val ratioLabel: String,
    val defaultMode: ColorMode,
    val defaultPpi: Int,
    val defaultBleedPt: Float = 0f
)

val DOCUMENT_PRESETS = listOf(
    // Print
    DocumentPreset("A4 Print", "Print", 595.28f, 841.89f, DocumentUnit.MM, "1:1.41", ColorMode.CMYK, 300, 9f),
    DocumentPreset("Business Card", "Print", 252f, 144f, DocumentUnit.IN, "7:4", ColorMode.CMYK, 300, 8.5f),
    DocumentPreset("US Letter", "Print", 612f, 792f, DocumentUnit.IN, "1:1.29", ColorMode.CMYK, 300, 9f),
    DocumentPreset("Poster A3", "Print", 841.89f, 1190.55f, DocumentUnit.MM, "1:1.41", ColorMode.CMYK, 300, 12f),
    // Web
    DocumentPreset("Desktop 1080p", "Web", 1440f, 810f, DocumentUnit.PX, "16:9", ColorMode.RGB, 72),
    DocumentPreset("Web Banner", "Web", 900f, 472.5f, DocumentUnit.PX, "1.91:1", ColorMode.RGB, 72),
    // Mobile
    DocumentPreset("Mobile Screen", "Mobile", 390f * 0.75f, 844f * 0.75f, DocumentUnit.PX, "9:19.5", ColorMode.RGB, 72),
    DocumentPreset("Tablet Canvas", "Mobile", 820f * 0.75f, 1180f * 0.75f, DocumentUnit.PX, "3:4", ColorMode.RGB, 150),
    // Social
    DocumentPreset("Square Post", "Social", 810f, 810f, DocumentUnit.PX, "1:1", ColorMode.RGB, 72),
    DocumentPreset("Story / Reel", "Social", 810f, 1440f, DocumentUnit.PX, "9:16", ColorMode.RGB, 72)
)

@Composable
fun NewDocumentDialog(
    onDismiss: () -> Unit,
    onCreateDocument: (
        name: String,
        width: Float,
        height: Float,
        units: DocumentUnit,
        colorMode: ColorMode,
        profile: ColorProfile,
        rasterPpi: Int,
        bleed: Bleed
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    var docName by remember { mutableStateOf("Untitled Design") }
    var selectedCategory by remember { mutableStateOf("Print") }
    var selectedPreset by remember { mutableStateOf(DOCUMENT_PRESETS.first()) }
    var selectedUnit by remember { mutableStateOf(DocumentUnit.MM) }

    var widthInput by remember { mutableStateOf("%.1f".format(595.28f / DocumentUnit.MM.pointsPerUnit)) }
    var heightInput by remember { mutableStateOf("%.1f".format(841.89f / DocumentUnit.MM.pointsPerUnit)) }

    var isLandscape by remember { mutableStateOf(false) }
    var colorMode by remember { mutableStateOf(ColorMode.CMYK) }
    var colorProfile by remember { mutableStateOf(ColorProfile.US_WEB_COATED_SWOP) }
    var rasterPpi by remember { mutableIntStateOf(300) }
    var bleedInput by remember { mutableStateOf("3.0") }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(14.dp),
        color = Color(0xFF161923),
        shape = RoundedCornerShape(22.dp),
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
                Column {
                    Text("New Document Setup", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Select preset size, ratio, units & resolution", color = Color(0xFF90A4AE), fontSize = 11.sp)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF90A4AE))
                }
            }

            // Name
            OutlinedTextField(
                value = docName,
                onValueChange = { docName = it },
                label = { Text("Document Title", color = Color(0xFF90A4AE)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00E5FF),
                    unfocusedBorderColor = Color(0xFF2A2E3D),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Category Tabs
            TabRow(
                selectedTabIndex = when (selectedCategory) {
                    "Print" -> 0
                    "Mobile" -> 1
                    "Web" -> 2
                    else -> 3
                },
                containerColor = Color(0xFF1E222D),
                contentColor = Color(0xFF00E5FF)
            ) {
                listOf("Print", "Mobile", "Web", "Social").forEach { cat ->
                    Tab(
                        selected = selectedCategory == cat,
                        onClick = {
                            selectedCategory = cat
                            val preset = DOCUMENT_PRESETS.first { it.category == cat }
                            selectedPreset = preset
                            selectedUnit = preset.unit
                            colorMode = preset.defaultMode
                            colorProfile = if (preset.defaultMode == ColorMode.CMYK) ColorProfile.US_WEB_COATED_SWOP else ColorProfile.SRGB
                            rasterPpi = preset.defaultPpi
                            widthInput = "%.1f".format(preset.widthPt / preset.unit.pointsPerUnit)
                            heightInput = "%.1f".format(preset.heightPt / preset.unit.pointsPerUnit)
                            bleedInput = "%.1f".format(preset.defaultBleedPt / preset.unit.pointsPerUnit)
                        },
                        text = { Text(cat, fontSize = 12.sp) }
                    )
                }
            }

            // Presets in Category with Ratio Badges
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Preset Sizes & Ratios", color = Color(0xFFB0BEC5), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DOCUMENT_PRESETS.filter { it.category == selectedCategory }.forEach { preset ->
                        val isSelected = selectedPreset == preset
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    selectedPreset = preset
                                    selectedUnit = preset.unit
                                    colorMode = preset.defaultMode
                                    colorProfile = if (preset.defaultMode == ColorMode.CMYK) ColorProfile.US_WEB_COATED_SWOP else ColorProfile.SRGB
                                    rasterPpi = preset.defaultPpi
                                    widthInput = "%.1f".format(preset.widthPt / preset.unit.pointsPerUnit)
                                    heightInput = "%.1f".format(preset.heightPt / preset.unit.pointsPerUnit)
                                    bleedInput = "%.1f".format(preset.defaultBleedPt / preset.unit.pointsPerUnit)
                                }
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) Color(0xFF00E5FF) else Color(0xFF2A2E3D),
                                    shape = RoundedCornerShape(10.dp)
                                ),
                            color = if (isSelected) Color(0xFF00E5FF).copy(alpha = 0.15f) else Color(0xFF1E222D)
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(preset.name, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                Surface(
                                    color = Color(0xFF2E3444),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = preset.ratioLabel,
                                        color = Color(0xFF00E5FF),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Units Selector (Inches, Points, Pixels, Feet, mm, cm)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Measurement Units (Scale)", color = Color(0xFFB0BEC5), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DocumentUnit.values().forEach { u ->
                        val isSelected = selectedUnit == u
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    // Convert current width & height to new unit
                                    val currentWpt = (widthInput.toFloatOrNull() ?: 100f) * selectedUnit.pointsPerUnit
                                    val currentHpt = (heightInput.toFloatOrNull() ?: 100f) * selectedUnit.pointsPerUnit
                                    selectedUnit = u
                                    widthInput = "%.1f".format(currentWpt / u.pointsPerUnit)
                                    heightInput = "%.1f".format(currentHpt / u.pointsPerUnit)
                                }
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) Color(0xFF00E5FF) else Color(0xFF2A2E3D),
                                    shape = RoundedCornerShape(8.dp)
                                ),
                            color = if (isSelected) Color(0xFF00E5FF).copy(alpha = 0.2f) else Color(0xFF1E222D)
                        ) {
                            Text(
                                text = u.name,
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

            // Custom Width, Height, and Orientation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = widthInput,
                    onValueChange = { widthInput = it },
                    label = { Text("Width (${selectedUnit.symbol})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = heightInput,
                    onValueChange = { heightInput = it },
                    label = { Text("Height (${selectedUnit.symbol})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                )

                // Orientation toggle
                IconButton(
                    onClick = {
                        isLandscape = !isLandscape
                        val w = widthInput
                        widthInput = heightInput
                        heightInput = w
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1E222D))
                        .border(1.dp, Color(0xFF2A2E3D), RoundedCornerShape(8.dp))
                ) {
                    Icon(
                        imageVector = if (isLandscape) Icons.Default.ScreenRotation else Icons.Default.StayCurrentPortrait,
                        contentDescription = "Orientation",
                        tint = Color(0xFF00E5FF)
                    )
                }
            }

            // Color Mode (CMYK vs RGB)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Color Mode", color = Color(0xFFB0BEC5), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                colorMode = ColorMode.CMYK
                                colorProfile = ColorProfile.US_WEB_COATED_SWOP
                            }
                            .border(
                                width = if (colorMode == ColorMode.CMYK) 1.5.dp else 1.dp,
                                color = if (colorMode == ColorMode.CMYK) Color(0xFFFF4081) else Color(0xFF2A2E3D),
                                shape = RoundedCornerShape(8.dp)
                            ),
                        color = if (colorMode == ColorMode.CMYK) Color(0xFFFF4081).copy(alpha = 0.15f) else Color(0xFF1E222D)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("CMYK (Commercial Print)", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("SWOP Press Profile", color = Color(0xFFFF80AB), fontSize = 9.sp)
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                colorMode = ColorMode.RGB
                                colorProfile = ColorProfile.SRGB
                            }
                            .border(
                                width = if (colorMode == ColorMode.RGB) 1.5.dp else 1.dp,
                                color = if (colorMode == ColorMode.RGB) Color(0xFF00E5FF) else Color(0xFF2A2E3D),
                                shape = RoundedCornerShape(8.dp)
                            ),
                        color = if (colorMode == ColorMode.RGB) Color(0xFF00E5FF).copy(alpha = 0.15f) else Color(0xFF1E222D)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("RGB (Digital / Screens)", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("sRGB Standard Gamut", color = Color(0xFF80D8FF), fontSize = 9.sp)
                        }
                    }
                }
            }

            // Raster Effects PPI
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Raster Resolution (PPI)", color = Color(0xFFB0BEC5), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(72 to "72 ppi (Screen)", 150 to "150 ppi (Medium)", 300 to "300 ppi (Print High-Res)").forEach { (ppi, label) ->
                        val isSelected = rasterPpi == ppi
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { rasterPpi = ppi }
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
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier
                                    .padding(vertical = 8.dp)
                                    .wrapContentWidth(Alignment.CenterHorizontally)
                            )
                        }
                    }
                }
            }

            // Create Button
            Button(
                onClick = {
                    val wPt = (widthInput.toFloatOrNull() ?: 500f) * selectedUnit.pointsPerUnit
                    val hPt = (heightInput.toFloatOrNull() ?: 500f) * selectedUnit.pointsPerUnit
                    val bPt = (bleedInput.toFloatOrNull() ?: 0f) * selectedUnit.pointsPerUnit
                    onCreateDocument(
                        docName,
                        wPt,
                        hPt,
                        selectedUnit,
                        colorMode,
                        colorProfile,
                        rasterPpi,
                        Bleed(bPt, bPt, bPt, bPt)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("action_create_document"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00E5FF),
                    contentColor = Color(0xFF00363A)
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Create Artboard", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
