package com.example.ui.components

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.viewmodel.*

@Composable
fun ToolRail(
    viewModel: EditorViewModel,
    uiState: EditorUiState,
    modifier: Modifier = Modifier
) {
    var showPenMenu by remember { mutableStateOf(false) }
    var showShapesMenu by remember { mutableStateOf(false) }
    var showBrushMenu by remember { mutableStateOf(false) }
    var showPencilMenu by remember { mutableStateOf(false) }
    var showEraserMenu by remember { mutableStateOf(false) }
    var showRotateMenu by remember { mutableStateOf(false) }
    var showScaleMenu by remember { mutableStateOf(false) }
    var showEyedropperMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .width(52.dp)
            .fillMaxHeight(),
        color = Color(0xFF1E212B),
        tonalElevation = 6.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2B2F3D))
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Scrollable Tools Column (All Adobe Illustrator Tools)
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // 1. Selection Tool (V)
                ToolButton(
                    icon = Icons.Default.NearMe,
                    label = "Selection (V)",
                    isSelected = uiState.currentTool == EditorTool.SELECTION,
                    onClick = { viewModel.selectTool(EditorTool.SELECTION) }
                )

                // 2. Direct Selection Tool (A)
                ToolButton(
                    icon = Icons.Default.OpenWith,
                    label = "Direct Selection (A)",
                    isSelected = uiState.currentTool == EditorTool.DIRECT_SELECTION,
                    onClick = { viewModel.selectTool(EditorTool.DIRECT_SELECTION) }
                )

                // 3. Pen Tool (P) with Bézier curve subtools flyout
                Box {
                    val penIcon = when (uiState.activePenSubTool) {
                        PenSubTool.PEN -> Icons.Default.Edit
                        PenSubTool.ADD_ANCHOR -> Icons.Default.AddLocationAlt
                        PenSubTool.DELETE_ANCHOR -> Icons.Default.RemoveCircleOutline
                        PenSubTool.CONVERT_ANCHOR -> Icons.Default.ChangeHistory
                    }
                    ToolButton(
                        icon = penIcon,
                        label = "Pen Tool",
                        isSelected = uiState.currentTool == EditorTool.PEN,
                        hasSubtools = true,
                        onClick = {
                            if (uiState.currentTool == EditorTool.PEN) {
                                showPenMenu = !showPenMenu
                            } else {
                                viewModel.selectTool(EditorTool.PEN)
                            }
                        }
                    )
                    DropdownMenu(
                        expanded = showPenMenu,
                        onDismissRequest = { showPenMenu = false },
                        modifier = Modifier.background(Color(0xFF1E222F))
                    ) {
                        PenSubTool.values().forEach { subTool ->
                            DropdownMenuItem(
                                text = { Text(subTool.displayName, color = Color.White, fontSize = 12.sp) },
                                onClick = {
                                    viewModel.setPenSubTool(subTool)
                                    showPenMenu = false
                                }
                            )
                        }
                    }
                }

                // 4. Curvature Tool (Shift+~)
                ToolButton(
                    icon = Icons.Default.Gesture,
                    label = "Curvature Tool",
                    isSelected = uiState.currentTool == EditorTool.CURVATURE,
                    onClick = { viewModel.selectTool(EditorTool.CURVATURE) }
                )

                // 5. Shapes Tool (M) with flyout
                Box {
                    val shapeIcon = when (uiState.activeShapeType) {
                        ShapeType.RECTANGLE -> Icons.Default.CropSquare
                        ShapeType.ROUNDED_RECT -> Icons.Default.RoundedCorner
                        ShapeType.ELLIPSE -> Icons.Default.Circle
                        ShapeType.STAR -> Icons.Default.Star
                        ShapeType.POLYGON -> Icons.Default.Hexagon
                        ShapeType.LINE -> Icons.Default.HorizontalRule
                        ShapeType.SPIRAL -> Icons.Default.Cyclone
                    }
                    ToolButton(
                        icon = shapeIcon,
                        label = "Shapes",
                        isSelected = uiState.currentTool == EditorTool.SHAPES,
                        hasSubtools = true,
                        onClick = {
                            if (uiState.currentTool == EditorTool.SHAPES) {
                                showShapesMenu = !showShapesMenu
                            } else {
                                viewModel.selectTool(EditorTool.SHAPES)
                            }
                        }
                    )
                    DropdownMenu(
                        expanded = showShapesMenu,
                        onDismissRequest = { showShapesMenu = false },
                        modifier = Modifier.background(Color(0xFF1E222F))
                    ) {
                        ShapeType.values().forEach { shape ->
                            DropdownMenuItem(
                                text = { Text(shape.displayName, color = Color.White, fontSize = 12.sp) },
                                onClick = {
                                    viewModel.setActiveShapeType(shape)
                                    viewModel.selectTool(EditorTool.SHAPES)
                                    showShapesMenu = false
                                }
                            )
                        }
                    }
                }

                // 6. Paintbrush Tool (B) with flyout
                Box {
                    ToolButton(
                        icon = Icons.Default.Brush,
                        label = "Paintbrush",
                        isSelected = uiState.currentTool == EditorTool.BRUSH,
                        hasSubtools = true,
                        onClick = {
                            if (uiState.currentTool == EditorTool.BRUSH) {
                                showBrushMenu = !showBrushMenu
                            } else {
                                viewModel.selectTool(EditorTool.BRUSH)
                            }
                        }
                    )
                    DropdownMenu(
                        expanded = showBrushMenu,
                        onDismissRequest = { showBrushMenu = false },
                        modifier = Modifier.background(Color(0xFF1E222F))
                    ) {
                        BrushSubTool.values().forEach { b ->
                            DropdownMenuItem(
                                text = { Text(b.displayName, color = Color.White, fontSize = 12.sp) },
                                onClick = {
                                    viewModel.setBrushSubTool(b)
                                    showBrushMenu = false
                                }
                            )
                        }
                    }
                }

                // 7. Shaper / Pencil Tool (N) with flyout
                Box {
                    ToolButton(
                        icon = Icons.Default.Create,
                        label = "Pencil",
                        isSelected = uiState.currentTool == EditorTool.PENCIL,
                        hasSubtools = true,
                        onClick = {
                            if (uiState.currentTool == EditorTool.PENCIL) {
                                showPencilMenu = !showPencilMenu
                            } else {
                                viewModel.selectTool(EditorTool.PENCIL)
                            }
                        }
                    )
                    DropdownMenu(
                        expanded = showPencilMenu,
                        onDismissRequest = { showPencilMenu = false },
                        modifier = Modifier.background(Color(0xFF1E222F))
                    ) {
                        PencilSubTool.values().forEach { p ->
                            DropdownMenuItem(
                                text = { Text(p.displayName, color = Color.White, fontSize = 12.sp) },
                                onClick = {
                                    viewModel.setPencilSubTool(p)
                                    showPencilMenu = false
                                }
                            )
                        }
                    }
                }

                // 8. Eraser Tool (Shift+E) with flyout
                Box {
                    ToolButton(
                        icon = Icons.Default.ContentCut,
                        label = "Eraser",
                        isSelected = uiState.currentTool == EditorTool.ERASER,
                        hasSubtools = true,
                        onClick = {
                            if (uiState.currentTool == EditorTool.ERASER) {
                                showEraserMenu = !showEraserMenu
                            } else {
                                viewModel.selectTool(EditorTool.ERASER)
                            }
                        }
                    )
                    DropdownMenu(
                        expanded = showEraserMenu,
                        onDismissRequest = { showEraserMenu = false },
                        modifier = Modifier.background(Color(0xFF1E222F))
                    ) {
                        EraserSubTool.values().forEach { e ->
                            DropdownMenuItem(
                                text = { Text(e.displayName, color = Color.White, fontSize = 12.sp) },
                                onClick = {
                                    viewModel.setEraserSubTool(e)
                                    showEraserMenu = false
                                }
                            )
                        }
                    }
                }

                // 9. Rotate Tool (R) with flyout
                Box {
                    ToolButton(
                        icon = Icons.Default.RotateRight,
                        label = "Rotate",
                        isSelected = uiState.currentTool == EditorTool.ROTATE,
                        hasSubtools = true,
                        onClick = {
                            if (uiState.currentTool == EditorTool.ROTATE) {
                                showRotateMenu = !showRotateMenu
                            } else {
                                viewModel.selectTool(EditorTool.ROTATE)
                            }
                        }
                    )
                    DropdownMenu(
                        expanded = showRotateMenu,
                        onDismissRequest = { showRotateMenu = false },
                        modifier = Modifier.background(Color(0xFF1E222F))
                    ) {
                        RotateSubTool.values().forEach { r ->
                            DropdownMenuItem(
                                text = { Text(r.displayName, color = Color.White, fontSize = 12.sp) },
                                onClick = {
                                    viewModel.setRotateSubTool(r)
                                    showRotateMenu = false
                                }
                            )
                        }
                    }
                }

                // 10. Scale Tool (S) with flyout
                Box {
                    ToolButton(
                        icon = Icons.Default.ZoomOutMap,
                        label = "Scale",
                        isSelected = uiState.currentTool == EditorTool.SCALE,
                        hasSubtools = true,
                        onClick = {
                            if (uiState.currentTool == EditorTool.SCALE) {
                                showScaleMenu = !showScaleMenu
                            } else {
                                viewModel.selectTool(EditorTool.SCALE)
                            }
                        }
                    )
                    DropdownMenu(
                        expanded = showScaleMenu,
                        onDismissRequest = { showScaleMenu = false },
                        modifier = Modifier.background(Color(0xFF1E222F))
                    ) {
                        ScaleSubTool.values().forEach { s ->
                            DropdownMenuItem(
                                text = { Text(s.displayName, color = Color.White, fontSize = 12.sp) },
                                onClick = {
                                    viewModel.setScaleSubTool(s)
                                    showScaleMenu = false
                                }
                            )
                        }
                    }
                }

                // 11. Width Tool (Shift+W)
                ToolButton(
                    icon = Icons.Default.LineWeight,
                    label = "Width Tool",
                    isSelected = uiState.currentTool == EditorTool.WIDTH,
                    onClick = { viewModel.selectTool(EditorTool.WIDTH) }
                )

                // 12. Free Transform Tool (E)
                ToolButton(
                    icon = Icons.Default.Transform,
                    label = "Free Transform",
                    isSelected = uiState.currentTool == EditorTool.FREE_TRANSFORM,
                    onClick = { viewModel.selectTool(EditorTool.FREE_TRANSFORM) }
                )

                // 13. Shape Builder Tool (Shift+M)
                ToolButton(
                    icon = Icons.Default.MergeType,
                    label = "Shape Builder",
                    isSelected = uiState.currentTool == EditorTool.SHAPE_BUILDER,
                    onClick = {
                        viewModel.selectTool(EditorTool.SHAPE_BUILDER)
                        viewModel.runShapeBuilderOnSelected()
                    }
                )

                // 14. Gradient Tool (G)
                ToolButton(
                    icon = Icons.Default.Gradient,
                    label = "Gradient Tool",
                    isSelected = uiState.currentTool == EditorTool.GRADIENT,
                    onClick = { viewModel.selectTool(EditorTool.GRADIENT) }
                )

                // 15. Eyedropper Tool (I) with flyout
                Box {
                    ToolButton(
                        icon = Icons.Default.Colorize,
                        label = "Eyedropper",
                        isSelected = uiState.currentTool == EditorTool.EYEDROPPER,
                        hasSubtools = true,
                        onClick = {
                            if (uiState.currentTool == EditorTool.EYEDROPPER) {
                                showEyedropperMenu = !showEyedropperMenu
                            } else {
                                viewModel.selectTool(EditorTool.EYEDROPPER)
                            }
                        }
                    )
                    DropdownMenu(
                        expanded = showEyedropperMenu,
                        onDismissRequest = { showEyedropperMenu = false },
                        modifier = Modifier.background(Color(0xFF1E222F))
                    ) {
                        EyedropperSubTool.values().forEach { eye ->
                            DropdownMenuItem(
                                text = { Text(eye.displayName, color = Color.White, fontSize = 12.sp) },
                                onClick = {
                                    viewModel.setEyedropperSubTool(eye)
                                    showEyedropperMenu = false
                                }
                            )
                        }
                    }
                }

                // 16. Blend Tool (W)
                ToolButton(
                    icon = Icons.Default.Grain,
                    label = "Blend Tool",
                    isSelected = uiState.currentTool == EditorTool.BLEND,
                    onClick = { viewModel.selectTool(EditorTool.BLEND) }
                )

                // 17. Type Tool (T)
                ToolButton(
                    icon = Icons.Default.TextFields,
                    label = "Type Tool",
                    isSelected = uiState.currentTool == EditorTool.TEXT,
                    onClick = { viewModel.selectTool(EditorTool.TEXT) }
                )

                // 18. Artboard Tool (Shift+O)
                ToolButton(
                    icon = Icons.Default.DashboardCustomize,
                    label = "Artboard Tool",
                    isSelected = uiState.currentTool == EditorTool.ARTBOARD,
                    onClick = { viewModel.selectTool(EditorTool.ARTBOARD) }
                )

                // 19. Hand Tool (H)
                ToolButton(
                    icon = Icons.Default.PanTool,
                    label = "Hand Tool",
                    isSelected = uiState.currentTool == EditorTool.HAND,
                    onClick = { viewModel.selectTool(EditorTool.HAND) }
                )

                // 20. Zoom Tool (Z)
                ToolButton(
                    icon = Icons.Default.Search,
                    label = "Zoom Tool",
                    isSelected = uiState.currentTool == EditorTool.ZOOM,
                    onClick = { viewModel.selectTool(EditorTool.ZOOM) }
                )
            }

            // Lower Section: Fill and Stroke Overlapping Chips, Mode Toggles, Screen Mode
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                HorizontalDivider(
                    color = Color(0xFF2B2F3D),
                    modifier = Modifier.padding(horizontal = 6.dp)
                )

                // Fill / Stroke active overlapping chip
                val fillColor = when (val f = uiState.activeFill) {
                    is VpPaint.Solid -> ColorEngine.toComposeColor(f.color, uiState.document.profile, uiState.softProofCmyk)
                    is VpPaint.Linear -> ColorEngine.toComposeColor(f.stops.first().color, uiState.document.profile)
                    else -> Color.Transparent
                }
                val strokeColor = when (val s = uiState.activeStroke) {
                    is VpPaint.Solid -> ColorEngine.toComposeColor(s.color, uiState.document.profile, uiState.softProofCmyk)
                    else -> Color.Transparent
                }

                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .testTag("fill_stroke_chips"),
                    contentAlignment = Alignment.Center
                ) {
                    // Stroke chip (behind, bottom-right)
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .align(Alignment.BottomEnd)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF252A36))
                            .border(
                                width = if (uiState.activePaintTarget == PaintTarget.STROKE) 2.dp else 1.dp,
                                color = if (uiState.activePaintTarget == PaintTarget.STROKE) Color(0xFF00E5FF) else Color(0xFF454D60),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .clickable {
                                viewModel.setPaintTarget(PaintTarget.STROKE)
                                viewModel.setActivePanel(EditorPanel.COLOR)
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(2.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(strokeColor)
                        )
                    }

                    // Fill chip (front, top-left)
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .align(Alignment.TopStart)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF252A36))
                            .border(
                                width = if (uiState.activePaintTarget == PaintTarget.FILL) 2.dp else 1.dp,
                                color = if (uiState.activePaintTarget == PaintTarget.FILL) Color(0xFF00E5FF) else Color(0xFF454D60),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .clickable {
                                viewModel.setPaintTarget(PaintTarget.FILL)
                                viewModel.setActivePanel(EditorPanel.COLOR)
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(2.dp))
                                .background(fillColor)
                        )
                    }
                }

                // Swap Curved Arrow & Default B&W Buttons Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = {
                            val tempFill = uiState.activeFill
                            if (tempFill is VpPaint.Solid) viewModel.updateSelectedColor(tempFill.color)
                        },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = "Swap (X)",
                            tint = Color(0xFF90A4AE),
                            modifier = Modifier.size(13.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            if (uiState.document.colorMode == ColorMode.CMYK) {
                                viewModel.updateSelectedColor(VpColor.pureBlack())
                            } else {
                                viewModel.updateSelectedColor(VpColor.black())
                            }
                        },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .border(1.dp, Color(0xFF90A4AE), RoundedCornerShape(1.dp))
                        ) {
                            Box(modifier = Modifier.size(5.dp).align(Alignment.BottomEnd).background(Color.White))
                            Box(modifier = Modifier.size(5.dp).align(Alignment.TopStart).background(Color.Black))
                        }
                    }
                }

                // Color / Gradient / None Buttons (<, >, /)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    MiniDrawModeBtn("●", "Color") {
                        viewModel.updateSelectedColor(VpColor.rgb(0, 229, 255))
                    }
                    MiniDrawModeBtn("◐", "Gradient") {
                        viewModel.selectTool(EditorTool.GRADIENT)
                    }
                    MiniDrawModeBtn("⊘", "None") {
                        viewModel.updateSelectedOpacity(0f)
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolButton(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    hasSubtools: Boolean = false,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) Color(0xFF00E5FF).copy(alpha = 0.22f) else Color.Transparent)
            .border(
                width = if (isSelected) 1.dp else 0.dp,
                color = if (isSelected) Color(0xFF00E5FF) else Color.Transparent,
                shape = RoundedCornerShape(6.dp)
            )
            .testTag("tool_${label.lowercase().replace(" ", "_")}")
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) Color(0xFF00E5FF) else Color(0xFFC0C7D5),
                modifier = Modifier.size(17.dp)
            )
            if (hasSubtools) {
                Box(
                    modifier = Modifier
                        .size(3.dp)
                        .align(Alignment.BottomEnd)
                        .padding(end = 1.dp, bottom = 1.dp)
                        .background(if (isSelected) Color(0xFF00E5FF) else Color(0xFF78909C))
                )
            }
        }
    }
}

@Composable
private fun MiniDrawModeBtn(symbol: String, label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(14.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(Color(0xFF282D3B))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(symbol, color = Color(0xFF90A4AE), fontSize = 8.sp, fontWeight = FontWeight.Bold)
    }
}
