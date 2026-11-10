package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.SquareFoot
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.EditorTool
import com.example.viewmodel.EditorUiState
import com.example.viewmodel.EditorViewModel

@Composable
fun TouchShortcutBar(
    viewModel: EditorViewModel,
    uiState: EditorUiState,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFF2A2E3D), RoundedCornerShape(16.dp)),
        color = Color(0xFF161923).copy(alpha = 0.92f),
        tonalElevation = 6.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // [Shift] modifier key button
            FilterChip(
                selected = uiState.isShiftPressed,
                onClick = { viewModel.setShiftPressed(!uiState.isShiftPressed) },
                label = { Text("Shift", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF00E5FF).copy(alpha = 0.25f),
                    selectedLabelColor = Color(0xFF00E5FF),
                    containerColor = Color(0xFF1E222D),
                    labelColor = Color(0xFFB0BEC5)
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = uiState.isShiftPressed,
                    borderColor = if (uiState.isShiftPressed) Color(0xFF00E5FF) else Color(0xFF37474F)
                ),
                modifier = Modifier.height(32.dp).testTag("modifier_shift")
            )

            // [Alt] modifier key button
            FilterChip(
                selected = uiState.isAltPressed,
                onClick = { viewModel.setAltPressed(!uiState.isAltPressed) },
                label = { Text("Alt", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFFB388FF).copy(alpha = 0.25f),
                    selectedLabelColor = Color(0xFFB388FF),
                    containerColor = Color(0xFF1E222D),
                    labelColor = Color(0xFFB0BEC5)
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = uiState.isAltPressed,
                    borderColor = if (uiState.isAltPressed) Color(0xFFB388FF) else Color(0xFF37474F)
                ),
                modifier = Modifier.height(32.dp).testTag("modifier_alt")
            )

            // Pen Tool Specific: Cut/Split Handle [Alt] Action Button
            if (uiState.currentTool == EditorTool.PEN) {
                FilledTonalButton(
                    onClick = { viewModel.breakOrRetractPenHandle() },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color(0xFFB388FF).copy(alpha = 0.25f),
                        contentColor = Color(0xFFB388FF)
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Default.CallSplit, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Split [Alt]", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Shape Builder Action Button
            if (uiState.currentTool == EditorTool.SHAPE_BUILDER) {
                FilledTonalButton(
                    onClick = { viewModel.runShapeBuilderOnSelected() },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color(0xFF00E5FF).copy(alpha = 0.25f),
                        contentColor = Color(0xFF00E5FF)
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Merge Crossed", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Grid toggle
            IconButton(
                onClick = { viewModel.toggleGrid() },
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (uiState.showGrid) Color(0xFF00E5FF).copy(alpha = 0.2f) else Color.Transparent)
            ) {
                Icon(
                    imageVector = Icons.Default.GridOn,
                    contentDescription = "Toggle Grid",
                    tint = if (uiState.showGrid) Color(0xFF00E5FF) else Color(0xFF78909C),
                    modifier = Modifier.size(18.dp)
                )
            }

            // Bleed guideline toggle
            IconButton(
                onClick = { viewModel.toggleBleed() },
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (uiState.showBleed) Color(0xFFFF4081).copy(alpha = 0.2f) else Color.Transparent)
            ) {
                Icon(
                    imageVector = Icons.Default.SquareFoot,
                    contentDescription = "Toggle Bleed Guidelines",
                    tint = if (uiState.showBleed) Color(0xFFFF4081) else Color(0xFF78909C),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
