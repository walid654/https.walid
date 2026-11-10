package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.EditorTool
import com.example.viewmodel.EditorUiState
import com.example.viewmodel.EditorViewModel
import kotlin.math.roundToInt

@Composable
fun BottomStatusBar(
    viewModel: EditorViewModel,
    uiState: EditorUiState,
    modifier: Modifier = Modifier
) {
    var showZoomMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(26.dp),
        color = Color(0xFF1E212B),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2B2F3D))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left Controls: Zoom Dropdown, Rotation Readout, Artboard Nav
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Zoom Dropdown
                Box {
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .clickable { showZoomMenu = !showZoomMenu }
                            .border(1.dp, Color(0xFF33384A), RoundedCornerShape(3.dp)),
                        color = Color(0xFF151821)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "%.1f %%".format(uiState.zoom * 100f),
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Icon(Icons.Default.ArrowDropDown, null, tint = Color(0xFF90A4AE), modifier = Modifier.size(12.dp))
                        }
                    }

                    DropdownMenu(
                        expanded = showZoomMenu,
                        onDismissRequest = { showZoomMenu = false },
                        modifier = Modifier.background(Color(0xFF1E222F))
                    ) {
                        listOf(25f, 50f, 75f, 100f, 150f, 200f, 400f).forEach { z ->
                            DropdownMenuItem(
                                text = { Text("${z.roundToInt()}%", color = Color.White, fontSize = 11.sp) },
                                onClick = {
                                    viewModel.updateZoomAndPan(z / 100f / uiState.zoom, 0f, 0f)
                                    showZoomMenu = false
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Fit on Screen", color = Color(0xFF00E5FF), fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            onClick = {
                                viewModel.zoomFit()
                                showZoomMenu = false
                            }
                        )
                    }
                }

                // Rotation Readout
                Text("0°", color = Color(0xFF90A4AE), fontSize = 10.sp)

                VerticalDivider(color = Color(0xFF33384A), modifier = Modifier.height(14.dp))

                // Artboard Navigation (|< < 1 > >|)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Prev Artboard",
                        tint = Color(0xFF78909C),
                        modifier = Modifier.size(14.dp)
                    )
                    Text("1", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Next Artboard",
                        tint = Color(0xFF78909C),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Right: Workflow Hint Text (Exactly like Adobe Illustrator status bar in screenshot)
            val hint = when (uiState.currentTool) {
                EditorTool.PEN -> "Click to place anchor | Drag to create Bézier curve | Hold Alt to split tangent"
                EditorTool.CURVATURE -> "Tap to add curve anchor | Double-tap to create sharp corner"
                EditorTool.DIRECT_SELECTION -> "Tap anchor to edit coordinates | Drag handle to adjust curvature"
                EditorTool.SHAPE_BUILDER -> "Drag across overlapping shapes to merge | Hold Alt to subtract"
                EditorTool.ERASER -> "Drag across paths to erase/split"
                EditorTool.ROTATE -> "Drag to rotate selection | Hold Shift for 45° increments"
                EditorTool.SCALE -> "Drag to scale selection | Hold Shift for proportional scale"
                else -> "Click object to select | Shift+Click multiple | Alt+Drag to duplicate"
            }

            Text(
                text = hint,
                color = Color(0xFF78909C),
                fontSize = 10.sp,
                maxLines = 1,
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(start = 12.dp)
            )
        }
    }
}
