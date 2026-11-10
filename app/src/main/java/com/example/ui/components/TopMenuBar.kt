package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
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
import com.example.pathfinder.PathfinderOps
import com.example.viewmodel.EditorPanel
import com.example.viewmodel.EditorUiState
import com.example.viewmodel.EditorViewModel
import com.example.viewmodel.PaintTarget
import kotlin.math.roundToInt

@Composable
fun TopMenuBar(
    viewModel: EditorViewModel,
    uiState: EditorUiState,
    onBackToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showFileMenu by remember { mutableStateOf(false) }
    var showEditMenu by remember { mutableStateOf(false) }
    var showObjectMenu by remember { mutableStateOf(false) }
    var showViewMenu by remember { mutableStateOf(false) }
    var showWindowMenu by remember { mutableStateOf(false) }
    var showStrokeMenu by remember { mutableStateOf(false) }
    var showOpacityMenu by remember { mutableStateOf(false) }

    val selectedCount = uiState.selectedElementIds.size

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        color = Color(0xFF1E212B),
        tonalElevation = 6.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2B2F3D))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Row 1: Adobe Illustrator Desktop Style Menu Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .background(Color(0xFF161922))
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Back / Home icon
                    IconButton(
                        onClick = onBackToHome,
                        modifier = Modifier.size(28.dp).testTag("nav_home_button")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Home",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // App Badge (Ai Icon)
                    Surface(
                        color = Color(0xFFFF9A00),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.size(20.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("Vp", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                    }

                    Spacer(Modifier.width(4.dp))

                    // Menu Item: File
                    Box {
                        MenuTextButton("File") { showFileMenu = !showFileMenu }
                        DropdownMenu(
                            expanded = showFileMenu,
                            onDismissRequest = { showFileMenu = false },
                            modifier = Modifier.background(Color(0xFF1E222F))
                        ) {
                            DropdownMenuItem(
                                text = { Text("Home / Files", color = Color.White) },
                                leadingIcon = { Icon(Icons.Default.Home, null, tint = Color(0xFF00E5FF), modifier = Modifier.size(18.dp)) },
                                onClick = { showFileMenu = false; onBackToHome() }
                            )
                            DropdownMenuItem(
                                text = { Text("Document Setup...", color = Color.White) },
                                leadingIcon = { Icon(Icons.Default.Settings, null, tint = Color(0xFFB0BEC5), modifier = Modifier.size(18.dp)) },
                                onClick = { showFileMenu = false; viewModel.setActivePanel(EditorPanel.PROPERTIES) }
                            )
                            DropdownMenuItem(
                                text = { Text("Export Artwork (SVG/PDF/PNG)...", color = Color.White) },
                                leadingIcon = { Icon(Icons.Default.Share, null, tint = Color(0xFFFF80AB), modifier = Modifier.size(18.dp)) },
                                onClick = { showFileMenu = false; viewModel.setActivePanel(EditorPanel.EXPORT) }
                            )
                        }
                    }

                    // Menu Item: Edit
                    Box {
                        MenuTextButton("Edit") { showEditMenu = !showEditMenu }
                        DropdownMenu(
                            expanded = showEditMenu,
                            onDismissRequest = { showEditMenu = false },
                            modifier = Modifier.background(Color(0xFF1E222F))
                        ) {
                            DropdownMenuItem(
                                text = { Text("Undo", color = if (uiState.canUndo) Color.White else Color(0xFF546E7A)) },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Undo, null, tint = if (uiState.canUndo) Color(0xFF00E5FF) else Color(0xFF546E7A), modifier = Modifier.size(18.dp)) },
                                onClick = { showEditMenu = false; viewModel.undo() }
                            )
                            DropdownMenuItem(
                                text = { Text("Redo", color = if (uiState.canRedo) Color.White else Color(0xFF546E7A)) },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Redo, null, tint = if (uiState.canRedo) Color(0xFF00E5FF) else Color(0xFF546E7A), modifier = Modifier.size(18.dp)) },
                                onClick = { showEditMenu = false; viewModel.redo() }
                            )
                            DropdownMenuItem(
                                text = { Text("Duplicate Selected", color = Color.White) },
                                leadingIcon = { Icon(Icons.Default.ContentCopy, null, tint = Color(0xFFB0BEC5), modifier = Modifier.size(18.dp)) },
                                onClick = { showEditMenu = false; viewModel.duplicateSelectedElements() }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete Selected", color = Color(0xFFFF5252)) },
                                leadingIcon = { Icon(Icons.Default.Delete, null, tint = Color(0xFFFF5252), modifier = Modifier.size(18.dp)) },
                                onClick = { showEditMenu = false; viewModel.deleteSelectedElements() }
                            )
                        }
                    }

                    // Menu Item: Object
                    Box {
                        MenuTextButton("Object") { showObjectMenu = !showObjectMenu }
                        DropdownMenu(
                            expanded = showObjectMenu,
                            onDismissRequest = { showObjectMenu = false },
                            modifier = Modifier.background(Color(0xFF1E222F))
                        ) {
                            DropdownMenuItem(
                                text = { Text("Bring Forward", color = Color.White) },
                                leadingIcon = { Icon(Icons.Default.FlipToFront, null, tint = Color(0xFF00E5FF), modifier = Modifier.size(18.dp)) },
                                onClick = { showObjectMenu = false; viewModel.bringForward() }
                            )
                            DropdownMenuItem(
                                text = { Text("Send Backward", color = Color.White) },
                                leadingIcon = { Icon(Icons.Default.FlipToBack, null, tint = Color(0xFFB0BEC5), modifier = Modifier.size(18.dp)) },
                                onClick = { showObjectMenu = false; viewModel.sendBackward() }
                            )
                            DropdownMenuItem(
                                text = { Text("Rotate 90° Clockwise", color = Color.White) },
                                leadingIcon = { Icon(Icons.Default.RotateRight, null, tint = Color(0xFF00E5FF), modifier = Modifier.size(18.dp)) },
                                onClick = { showObjectMenu = false; viewModel.rotateSelection(90f) }
                            )
                            DropdownMenuItem(
                                text = { Text("Pathfinder: Unite (Merge)", color = Color.White) },
                                leadingIcon = { Icon(Icons.Default.AddBox, null, tint = Color(0xFF69F0AE), modifier = Modifier.size(18.dp)) },
                                onClick = { showObjectMenu = false; viewModel.runPathfinder(PathfinderOps.Operation.UNITE) }
                            )
                            DropdownMenuItem(
                                text = { Text("Pathfinder: Minus Front (Cut)", color = Color.White) },
                                leadingIcon = { Icon(Icons.Default.IndeterminateCheckBox, null, tint = Color(0xFFFF80AB), modifier = Modifier.size(18.dp)) },
                                onClick = { showObjectMenu = false; viewModel.runPathfinder(PathfinderOps.Operation.MINUS_FRONT) }
                            )
                        }
                    }

                    // Menu Item: View
                    Box {
                        MenuTextButton("View") { showViewMenu = !showViewMenu }
                        DropdownMenu(
                            expanded = showViewMenu,
                            onDismissRequest = { showViewMenu = false },
                            modifier = Modifier.background(Color(0xFF1E222F))
                        ) {
                            DropdownMenuItem(
                                text = { Text("Zoom In (+25%)", color = Color.White) },
                                leadingIcon = { Icon(Icons.Default.ZoomIn, null, tint = Color(0xFF00E5FF), modifier = Modifier.size(18.dp)) },
                                onClick = { showViewMenu = false; viewModel.zoomIn() }
                            )
                            DropdownMenuItem(
                                text = { Text("Zoom Out (-25%)", color = Color.White) },
                                leadingIcon = { Icon(Icons.Default.ZoomOut, null, tint = Color(0xFF00E5FF), modifier = Modifier.size(18.dp)) },
                                onClick = { showViewMenu = false; viewModel.zoomOut() }
                            )
                            DropdownMenuItem(
                                text = { Text("Fit Artboard in Window", color = Color.White) },
                                leadingIcon = { Icon(Icons.Default.FitScreen, null, tint = Color(0xFF00E5FF), modifier = Modifier.size(18.dp)) },
                                onClick = { showViewMenu = false; viewModel.zoomFit() }
                            )
                            DropdownMenuItem(
                                text = { Text(if (uiState.showGrid) "Hide Grid" else "Show Grid", color = Color.White) },
                                leadingIcon = { Icon(Icons.Default.GridOn, null, tint = Color(0xFFB0BEC5), modifier = Modifier.size(18.dp)) },
                                onClick = { showViewMenu = false; viewModel.toggleGrid() }
                            )
                            DropdownMenuItem(
                                text = { Text(if (uiState.showRulers) "Hide Rulers" else "Show Rulers", color = Color.White) },
                                leadingIcon = { Icon(Icons.Default.Straighten, null, tint = Color(0xFFB0BEC5), modifier = Modifier.size(18.dp)) },
                                onClick = { showViewMenu = false; viewModel.toggleRulers() }
                            )
                            DropdownMenuItem(
                                text = { Text(if (uiState.softProofCmyk) "Turn Off CMYK Proof" else "CMYK Soft Proof", color = Color.White) },
                                leadingIcon = { Icon(Icons.Default.Visibility, null, tint = Color(0xFFFF9100), modifier = Modifier.size(18.dp)) },
                                onClick = { showViewMenu = false; viewModel.toggleSoftProof() }
                            )
                        }
                    }

                    // Menu Item: Window
                    Box {
                        MenuTextButton("Window") { showWindowMenu = !showWindowMenu }
                        DropdownMenu(
                            expanded = showWindowMenu,
                            onDismissRequest = { showWindowMenu = false },
                            modifier = Modifier.background(Color(0xFF1E222F))
                        ) {
                            EditorPanel.values().filterNot { it == EditorPanel.NONE }.forEach { panel ->
                                DropdownMenuItem(
                                    text = { Text(panel.name.lowercase().replaceFirstChar { it.uppercase() }, color = Color.White) },
                                    leadingIcon = {
                                        val icon = when (panel) {
                                            EditorPanel.PROPERTIES -> Icons.Default.Tune
                                            EditorPanel.COLOR -> Icons.Default.Palette
                                            EditorPanel.SWATCHES -> Icons.Default.ColorLens
                                            EditorPanel.LAYERS -> Icons.Default.Layers
                                            EditorPanel.PATHFINDER -> Icons.Default.MergeType
                                            EditorPanel.ALIGN -> Icons.Default.AlignHorizontalCenter
                                            EditorPanel.ARTBOARDS -> Icons.Default.DashboardCustomize
                                            EditorPanel.EXPORT -> Icons.Default.Share
                                            else -> Icons.Default.Widgets
                                        }
                                        Icon(icon, null, tint = Color(0xFF00E5FF), modifier = Modifier.size(18.dp))
                                    },
                                    onClick = { showWindowMenu = false; viewModel.setActivePanel(panel) }
                                )
                            }
                        }
                    }
                }

                // Right controls: Quick Undo/Redo & Export Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { viewModel.undo() },
                        enabled = uiState.canUndo,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Undo",
                            tint = if (uiState.canUndo) Color.White else Color(0xFF546E7A),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.redo() },
                        enabled = uiState.canRedo,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "Redo",
                            tint = if (uiState.canRedo) Color.White else Color(0xFF546E7A),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    FilledTonalButton(
                        onClick = { viewModel.setActivePanel(EditorPanel.EXPORT) },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color(0xFF00E5FF),
                            contentColor = Color(0xFF00363A)
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(24.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("Export", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Row 2: Illustrator Control Bar (Document Tab, Selection, Fill, Stroke, Opacity, Align, Setup)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .background(Color(0xFF232734))
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Document Tab Readout (e.g. "Untitled-1 @ 74% (CMYK/Preview)")
                Surface(
                    color = Color(0xFF181B24),
                    shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF33384A))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "${uiState.document.name} @ ${(uiState.zoom * 100).roundToInt()}% (${uiState.document.colorMode}/Preview)",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                VerticalDivider(color = Color(0xFF363B4D), modifier = Modifier.height(18.dp))

                // Selection Readout
                Text(
                    text = if (selectedCount == 0) "No Selection" else if (selectedCount == 1) "1 Object" else "$selectedCount Objects",
                    color = Color(0xFF90A4AE),
                    fontSize = 11.sp
                )

                // Fill Swatch Chip
                val fillColor = when (val f = uiState.activeFill) {
                    is VpPaint.Solid -> ColorEngine.toComposeColor(f.color, uiState.document.profile, uiState.softProofCmyk)
                    is VpPaint.Linear -> ColorEngine.toComposeColor(f.stops.first().color, uiState.document.profile)
                    else -> Color.Transparent
                }
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFF14171F))
                        .border(1.dp, Color(0xFF4C5468), RoundedCornerShape(3.dp))
                        .clickable {
                            viewModel.setPaintTarget(PaintTarget.FILL)
                            viewModel.setActivePanel(EditorPanel.COLOR)
                        }
                ) {
                    Box(modifier = Modifier.fillMaxSize().padding(2.dp).background(fillColor))
                }

                // Stroke Swatch Chip
                val strokeColor = when (val s = uiState.activeStroke) {
                    is VpPaint.Solid -> ColorEngine.toComposeColor(s.color, uiState.document.profile, uiState.softProofCmyk)
                    else -> Color.Transparent
                }
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFF14171F))
                        .border(1.dp, Color(0xFF4C5468), RoundedCornerShape(3.dp))
                        .clickable {
                            viewModel.setPaintTarget(PaintTarget.STROKE)
                            viewModel.setActivePanel(EditorPanel.COLOR)
                        }
                ) {
                    Box(modifier = Modifier.fillMaxSize().padding(2.dp).background(strokeColor))
                }

                // Stroke Weight Dropdown
                Box {
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { showStrokeMenu = !showStrokeMenu }
                            .border(1.dp, Color(0xFF3A4052), RoundedCornerShape(4.dp)),
                        color = Color(0xFF1A1D27)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text("Stroke: %.0f pt".format(uiState.activeStrokeStyle.width), color = Color.White, fontSize = 11.sp)
                            Icon(Icons.Default.ArrowDropDown, null, tint = Color(0xFF90A4AE), modifier = Modifier.size(14.dp))
                        }
                    }
                    DropdownMenu(
                        expanded = showStrokeMenu,
                        onDismissRequest = { showStrokeMenu = false },
                        modifier = Modifier.background(Color(0xFF1E222F))
                    ) {
                        listOf(0.5f, 1f, 2f, 3f, 4f, 6f, 8f, 12f, 16f, 24f).forEach { w ->
                            DropdownMenuItem(
                                text = { Text("${w} pt", color = Color.White, fontSize = 12.sp) },
                                onClick = {
                                    viewModel.updateSelectedStrokeWidth(w)
                                    showStrokeMenu = false
                                }
                            )
                        }
                    }
                }

                // Opacity selector
                Box {
                    val opacityPct = 100
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { showOpacityMenu = !showOpacityMenu }
                            .border(1.dp, Color(0xFF3A4052), RoundedCornerShape(4.dp)),
                        color = Color(0xFF1A1D27)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text("Opacity: 100%", color = Color.White, fontSize = 11.sp)
                            Icon(Icons.Default.ArrowDropDown, null, tint = Color(0xFF90A4AE), modifier = Modifier.size(14.dp))
                        }
                    }
                    DropdownMenu(
                        expanded = showOpacityMenu,
                        onDismissRequest = { showOpacityMenu = false },
                        modifier = Modifier.background(Color(0xFF1E222F))
                    ) {
                        listOf(100, 80, 60, 40, 20).forEach { pct ->
                            DropdownMenuItem(
                                text = { Text("$pct%", color = Color.White, fontSize = 12.sp) },
                                onClick = {
                                    viewModel.updateSelectedOpacity(pct / 100f)
                                    showOpacityMenu = false
                                }
                            )
                        }
                    }
                }

                VerticalDivider(color = Color(0xFF363B4D), modifier = Modifier.height(18.dp))

                // Quick Align Icons in Control Bar
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(onClick = { viewModel.alignSelected("LEFT") }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.AlignHorizontalLeft, contentDescription = "Align Left", tint = Color(0xFF00E5FF), modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = { viewModel.alignSelected("CENTER_H") }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.AlignHorizontalCenter, contentDescription = "Align Center", tint = Color(0xFF00E5FF), modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = { viewModel.alignSelected("RIGHT") }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.AlignHorizontalRight, contentDescription = "Align Right", tint = Color(0xFF00E5FF), modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = { viewModel.alignSelected("TOP") }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.AlignVerticalTop, contentDescription = "Align Top", tint = Color(0xFF00E5FF), modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = { viewModel.alignSelected("CENTER_V") }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.AlignVerticalCenter, contentDescription = "Align Middle", tint = Color(0xFF00E5FF), modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = { viewModel.alignSelected("BOTTOM") }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.AlignVerticalBottom, contentDescription = "Align Bottom", tint = Color(0xFF00E5FF), modifier = Modifier.size(16.dp))
                    }
                }

                VerticalDivider(color = Color(0xFF363B4D), modifier = Modifier.height(18.dp))

                // Document Setup button
                OutlinedButton(
                    onClick = { viewModel.setActivePanel(EditorPanel.PROPERTIES) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(24.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text("Document Setup", fontSize = 10.sp)
                }

                // Preferences button
                OutlinedButton(
                    onClick = { viewModel.setActivePanel(EditorPanel.PROPERTIES) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(24.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF90A4AE))
                ) {
                    Text("Preferences", fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun MenuTextButton(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        color = Color(0xFFDCE2EC),
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .clip(RoundedCornerShape(3.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 3.dp)
    )
}
