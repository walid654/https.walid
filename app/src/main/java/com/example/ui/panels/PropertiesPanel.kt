package com.example.ui.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.pathfinder.PathfinderOps
import com.example.viewmodel.*
import kotlin.math.roundToInt

@Composable
fun PropertiesPanel(
    viewModel: EditorViewModel,
    uiState: EditorUiState,
    modifier: Modifier = Modifier
) {
    val selectedElements = uiState.document.layers
        .flatMap { it.elements }
        .filter { it.id in uiState.selectedElementIds }

    val firstElement = selectedElements.firstOrNull()
    var showUnitsDropdown by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 560.dp)
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Tab Header: Properties | Libraries (Exactly like Adobe Illustrator)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Properties",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .border(
                            width = 1.dp,
                            color = Color(0xFF00E5FF).copy(alpha = 0.5f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
                Text(
                    text = "Libraries",
                    color = Color(0xFF78909C),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            Text(
                text = if (selectedElements.isEmpty()) "No Selection" else "${selectedElements.size} Selected",
                color = if (selectedElements.isEmpty()) Color(0xFF90A4AE) else Color(0xFF00E5FF),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        if (firstElement != null) {
            val bounds = firstElement.getBoundingBox()

            // 1. Quick Element Actions (Duplicate, Forward, Delete)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.duplicateSelectedElements() },
                    modifier = Modifier.weight(1f).testTag("prop_duplicate_btn"),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate", modifier = Modifier.size(14.dp), tint = Color(0xFF00E5FF))
                    Spacer(Modifier.width(4.dp))
                    Text("Duplicate", fontSize = 10.sp)
                }

                OutlinedButton(
                    onClick = { viewModel.bringForward() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.FlipToFront, contentDescription = "Forward", modifier = Modifier.size(14.dp), tint = Color(0xFFB0BEC5))
                    Spacer(Modifier.width(4.dp))
                    Text("Forward", fontSize = 10.sp)
                }

                OutlinedButton(
                    onClick = { viewModel.deleteSelectedElements() },
                    modifier = Modifier.weight(1f).testTag("prop_delete_btn"),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5252)),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(14.dp), tint = Color(0xFFFF5252))
                    Spacer(Modifier.width(4.dp))
                    Text("Delete", fontSize = 10.sp)
                }
            }

            // 2. Transform & Position Readout
            Surface(
                color = Color(0xFF1E222D),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2A2E3D))
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Transform", color = Color(0xFF90A4AE), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TransformField("X", "%.1f pt".format(bounds.left))
                        TransformField("Y", "%.1f pt".format(bounds.top))
                        TransformField("W", "%.1f pt".format(bounds.width))
                        TransformField("H", "%.1f pt".format(bounds.height))
                    }

                    // Flip Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val cx = bounds.centerX
                                val cy = bounds.centerY
                                val flipTransform = firstElement.transform.scale(-1f, 1f, cx, cy)
                                viewModel.updateDocument(
                                    uiState.document.copy(
                                        layers = uiState.document.layers.map { l ->
                                            l.copy(elements = l.elements.map { if (it.id == firstElement.id) it.withTransform(flipTransform) else it })
                                        }
                                    )
                                )
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Flip, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(3.dp))
                            Text("Flip Horiz", fontSize = 10.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                val cx = bounds.centerX
                                val cy = bounds.centerY
                                val flipTransform = firstElement.transform.scale(1f, -1f, cx, cy)
                                viewModel.updateDocument(
                                    uiState.document.copy(
                                        layers = uiState.document.layers.map { l ->
                                            l.copy(elements = l.elements.map { if (it.id == firstElement.id) it.withTransform(flipTransform) else it })
                                        }
                                    )
                                )
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Flip, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(3.dp))
                            Text("Flip Vert", fontSize = 10.sp)
                        }

                        OutlinedButton(
                            onClick = { viewModel.rotateSelection(90f) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.RotateRight, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(3.dp))
                            Text("Rotate 90°", fontSize = 10.sp)
                        }
                    }
                }
            }

            // 3. Alignment & Distribute
            Surface(
                color = Color(0xFF1E222D),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2A2E3D))
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Align", color = Color(0xFF90A4AE), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = if (selectedElements.size == 1) "To Artboard" else "To Selection",
                            color = Color(0xFF00E5FF),
                            fontSize = 10.sp
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        CompactAlignButton(Icons.AutoMirrored.Filled.AlignHorizontalLeft, "Left", Modifier.weight(1f)) {
                            viewModel.alignSelected("LEFT")
                        }
                        CompactAlignButton(Icons.Default.AlignHorizontalCenter, "Center H", Modifier.weight(1f)) {
                            viewModel.alignSelected("CENTER_H")
                        }
                        CompactAlignButton(Icons.AutoMirrored.Filled.AlignHorizontalRight, "Right", Modifier.weight(1f)) {
                            viewModel.alignSelected("RIGHT")
                        }
                        CompactAlignButton(Icons.Default.AlignVerticalTop, "Top", Modifier.weight(1f)) {
                            viewModel.alignSelected("TOP")
                        }
                        CompactAlignButton(Icons.Default.AlignVerticalCenter, "Middle V", Modifier.weight(1f)) {
                            viewModel.alignSelected("CENTER_V")
                        }
                        CompactAlignButton(Icons.Default.AlignVerticalBottom, "Bottom", Modifier.weight(1f)) {
                            viewModel.alignSelected("BOTTOM")
                        }
                    }
                }
            }

            // 4. Pathfinder: Shape Modes (মিলাই দেওয়া / কাট করা / Boolean Ops)
            Surface(
                color = Color(0xFF1E222D),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2A2E3D))
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Pathfinder", color = Color(0xFF90A4AE), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        if (selectedElements.size >= 2) {
                            Text("Ready (2 shapes)", color = Color(0xFF00E5FF), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Text("Select 2 shapes to unite/cut", color = Color(0xFF78909C), fontSize = 10.sp)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        PathfinderMiniBtn(
                            title = "Unite",
                            subtext = "Merge",
                            icon = Icons.Default.AddBox,
                            enabled = selectedElements.size >= 2,
                            modifier = Modifier.weight(1f)
                        ) {
                            viewModel.runPathfinder(PathfinderOps.Operation.UNITE)
                        }

                        PathfinderMiniBtn(
                            title = "Minus Front",
                            subtext = "Cut",
                            icon = Icons.Default.IndeterminateCheckBox,
                            enabled = selectedElements.size >= 2,
                            modifier = Modifier.weight(1f)
                        ) {
                            viewModel.runPathfinder(PathfinderOps.Operation.MINUS_FRONT)
                        }

                        PathfinderMiniBtn(
                            title = "Intersect",
                            subtext = "Overlap",
                            icon = Icons.Default.FilterFrames,
                            enabled = selectedElements.size >= 2,
                            modifier = Modifier.weight(1f)
                        ) {
                            viewModel.runPathfinder(PathfinderOps.Operation.INTERSECT)
                        }

                        PathfinderMiniBtn(
                            title = "Exclude",
                            subtext = "Hole",
                            icon = Icons.Default.CropFree,
                            enabled = selectedElements.size >= 2,
                            modifier = Modifier.weight(1f)
                        ) {
                            viewModel.runPathfinder(PathfinderOps.Operation.EXCLUDE)
                        }
                    }
                }
            }

            // 5. Appearance: Opacity & Stroke Width
            Surface(
                color = Color(0xFF1E222D),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2A2E3D))
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Appearance", color = Color(0xFF90A4AE), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Fill & Stroke Color", color = Color(0xFFB0BEC5), fontSize = 11.sp)
                        Button(
                            onClick = { viewModel.setActivePanel(EditorPanel.COLOR) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2E3D)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(24.dp)
                        ) {
                            Text("Color Wheel", color = Color(0xFF00E5FF), fontSize = 10.sp)
                        }
                    }

                    // Opacity Slider
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Opacity", color = Color(0xFFB0BEC5), fontSize = 11.sp)
                            Text("${(firstElement.opacity * 100).roundToInt()}%", color = Color(0xFF00E5FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = firstElement.opacity,
                            onValueChange = { viewModel.updateSelectedOpacity(it) },
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF00E5FF),
                                activeTrackColor = Color(0xFF00E5FF),
                                inactiveTrackColor = Color(0xFF2E3444)
                            )
                        )
                    }

                    // Stroke Width Slider
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Stroke Width", color = Color(0xFFB0BEC5), fontSize = 11.sp)
                            Text("%.1f pt".format(firstElement.strokeStyle.width), color = Color(0xFF00E5FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = firstElement.strokeStyle.width,
                            onValueChange = { viewModel.updateSelectedStrokeWidth(it) },
                            valueRange = 0f..24f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF00E5FF),
                                activeTrackColor = Color(0xFF00E5FF),
                                inactiveTrackColor = Color(0xFF2E3444)
                            )
                        )
                    }
                }
            }

        } else {
            // Document View (Identical to Adobe Illustrator's Properties Inspector when No Selection)
            val ab = uiState.document.activeArtboard()

            // 1. Document Section
            Surface(
                color = Color(0xFF1E222D),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2A2E3D))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Document", color = Color(0xFF90A4AE), fontSize = 12.sp, fontWeight = FontWeight.Bold)

                    // Units Dropdown
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Units:", color = Color(0xFFB0BEC5), fontSize = 12.sp)
                        Box {
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { showUnitsDropdown = !showUnitsDropdown }
                                    .border(1.dp, Color(0xFF3A4052), RoundedCornerShape(4.dp)),
                                color = Color(0xFF141720)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(uiState.document.units.displayName, color = Color.White, fontSize = 11.sp)
                                    Icon(Icons.Default.ArrowDropDown, null, tint = Color(0xFF90A4AE), modifier = Modifier.size(14.dp))
                                }
                            }
                            DropdownMenu(
                                expanded = showUnitsDropdown,
                                onDismissRequest = { showUnitsDropdown = false },
                                modifier = Modifier.background(Color(0xFF1E222F))
                            ) {
                                DocumentUnit.values().forEach { unit ->
                                    DropdownMenuItem(
                                        text = { Text("${unit.displayName} (${unit.symbol})", color = Color.White, fontSize = 12.sp) },
                                        onClick = {
                                            viewModel.setDocumentUnits(unit)
                                            showUnitsDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Artboard Indicator & Edit Artboards Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Artboard: 1 of ${uiState.document.artboards.size}", color = Color(0xFFB0BEC5), fontSize = 12.sp)
                        Button(
                            onClick = { viewModel.selectTool(EditorTool.ARTBOARD) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2E3D)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(24.dp)
                        ) {
                            Text("Edit Artboards", color = Color.White, fontSize = 10.sp)
                        }
                    }
                }
            }

            // 2. Rulers & Grids (Matching Illustrator Screenshot)
            Surface(
                color = Color(0xFF1E222D),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2A2E3D))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Ruler & Grids", color = Color(0xFF90A4AE), fontSize = 12.sp, fontWeight = FontWeight.Bold)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        InspectorToolToggle(
                            icon = Icons.Default.Straighten,
                            label = "Rulers",
                            isActive = uiState.showRulers,
                            modifier = Modifier.weight(1f)
                        ) {
                            viewModel.toggleRulers()
                        }

                        InspectorToolToggle(
                            icon = Icons.Default.GridOn,
                            label = "Grid",
                            isActive = uiState.showGrid,
                            modifier = Modifier.weight(1f)
                        ) {
                            viewModel.toggleGrid()
                        }

                        InspectorToolToggle(
                            icon = Icons.Default.GridOff,
                            label = "Transparency",
                            isActive = uiState.showTransparencyGrid,
                            modifier = Modifier.weight(1f)
                        ) {
                            viewModel.toggleTransparencyGrid()
                        }
                    }
                }
            }

            // 3. Guides & Snap Options (Matching Illustrator Screenshot)
            Surface(
                color = Color(0xFF1E222D),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2A2E3D))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Guides & Snap Options", color = Color(0xFF90A4AE), fontSize = 12.sp, fontWeight = FontWeight.Bold)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        InspectorToolToggle(
                            icon = Icons.Default.LocationSearching,
                            label = "Snap Point",
                            isActive = uiState.snapToPoint,
                            modifier = Modifier.weight(1f)
                        ) {
                            // toggle snap point
                        }

                        InspectorToolToggle(
                            icon = Icons.Default.GridGoldenratio,
                            label = "Snap Grid",
                            isActive = uiState.snapToGrid,
                            modifier = Modifier.weight(1f)
                        ) {
                            viewModel.toggleSnapToGrid()
                        }

                        InspectorToolToggle(
                            icon = Icons.Default.Shield,
                            label = "Bleed Box",
                            isActive = uiState.showBleed,
                            modifier = Modifier.weight(1f)
                        ) {
                            viewModel.toggleBleed()
                        }
                    }
                }
            }

            // 4. Preferences (Matching Illustrator Screenshot)
            Surface(
                color = Color(0xFF1E222D),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2A2E3D))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Preferences", color = Color(0xFF90A4AE), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("Keyboard Increment: 1 pt", color = Color(0xFFB0BEC5), fontSize = 11.sp)
                    Text("Color Engine: ${uiState.document.colorMode} (${uiState.document.profile.displayName})", color = Color(0xFF00E5FF), fontSize = 11.sp)
                    Text("Raster Effects: ${uiState.document.rasterPpi} PPI", color = Color(0xFFB388FF), fontSize = 11.sp)
                }
            }

            // 5. Quick Actions
            Surface(
                color = Color(0xFF1E222D),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2A2E3D))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Quick Actions", color = Color(0xFF90A4AE), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.setActivePanel(EditorPanel.PROPERTIES) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Text("Document Setup", fontSize = 10.sp)
                        }

                        FilledTonalButton(
                            onClick = { viewModel.setActivePanel(EditorPanel.EXPORT) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Color(0xFF00E5FF),
                                contentColor = Color(0xFF00363A)
                            )
                        ) {
                            Text("Export Vectors", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InspectorToolToggle(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .border(
                1.dp,
                if (isActive) Color(0xFF00E5FF) else Color(0xFF33384A),
                RoundedCornerShape(6.dp)
            ),
        color = if (isActive) Color(0xFF00E5FF).copy(alpha = 0.15f) else Color(0xFF141720)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (isActive) Color(0xFF00E5FF) else Color(0xFF90A4AE),
                modifier = Modifier.size(16.dp)
            )
            Text(
                label,
                color = if (isActive) Color.White else Color(0xFF90A4AE),
                fontSize = 9.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun TransformField(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color(0xFF78909C), fontSize = 9.sp)
        Text(value, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun CompactAlignButton(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .border(1.dp, Color(0xFF2A2E3D), RoundedCornerShape(4.dp)),
        color = Color(0xFF161923)
    ) {
        Box(
            modifier = Modifier.padding(vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = Color(0xFF00E5FF), modifier = Modifier.size(15.dp))
        }
    }
}

@Composable
private fun PathfinderMiniBtn(
    title: String,
    subtext: String,
    icon: ImageVector,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .border(
                1.dp,
                if (enabled) Color(0xFF00E5FF).copy(alpha = 0.5f) else Color(0xFF2A2E3D),
                RoundedCornerShape(6.dp)
            ),
        color = if (enabled) Color(0xFF161923) else Color(0xFF161923).copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Icon(
                icon,
                contentDescription = title,
                tint = if (enabled) Color(0xFF00E5FF) else Color(0xFF546E7A),
                modifier = Modifier.size(15.dp)
            )
            Text(
                title,
                color = if (enabled) Color.White else Color(0xFF546E7A),
                fontSize = 8.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Text(
                subtext,
                color = if (enabled) Color(0xFF90A4AE) else Color(0xFF37474F),
                fontSize = 7.sp,
                maxLines = 1
            )
        }
    }
}
