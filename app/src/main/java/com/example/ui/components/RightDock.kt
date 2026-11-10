package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.viewmodel.EditorPanel
import com.example.viewmodel.EditorUiState
import com.example.viewmodel.EditorViewModel

@Composable
fun RightDock(
    viewModel: EditorViewModel,
    uiState: EditorUiState,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .width(42.dp)
            .fillMaxHeight(),
        color = Color(0xFF1E212B),
        tonalElevation = 6.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2B2F3D))
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // 1. Properties
            DockIconButton(
                icon = Icons.Default.Tune,
                label = "Properties",
                isActive = uiState.activePanel == EditorPanel.PROPERTIES,
                tag = "dock_properties"
            ) {
                viewModel.setActivePanel(EditorPanel.PROPERTIES)
            }

            // 2. Layers
            DockIconButton(
                icon = Icons.Default.Layers,
                label = "Layers",
                isActive = uiState.activePanel == EditorPanel.LAYERS,
                tag = "dock_layers"
            ) {
                viewModel.setActivePanel(EditorPanel.LAYERS)
            }

            // 3. Color
            DockIconButton(
                icon = Icons.Default.Palette,
                label = "Color",
                isActive = uiState.activePanel == EditorPanel.COLOR,
                tag = "dock_color"
            ) {
                viewModel.setActivePanel(EditorPanel.COLOR)
            }

            // 4. Swatches
            DockIconButton(
                icon = Icons.Default.ColorLens,
                label = "Swatches",
                isActive = uiState.activePanel == EditorPanel.SWATCHES,
                tag = "dock_swatches"
            ) {
                viewModel.setActivePanel(EditorPanel.SWATCHES)
            }

            // 5. Pathfinder & Align
            DockIconButton(
                icon = Icons.Default.MergeType,
                label = "Pathfinder",
                isActive = uiState.activePanel == EditorPanel.PATHFINDER || uiState.activePanel == EditorPanel.ALIGN,
                tag = "dock_pathfinder"
            ) {
                viewModel.setActivePanel(EditorPanel.PATHFINDER)
            }

            // 6. Artboards
            DockIconButton(
                icon = Icons.Default.DashboardCustomize,
                label = "Artboards",
                isActive = uiState.activePanel == EditorPanel.ARTBOARDS,
                tag = "dock_artboards"
            ) {
                viewModel.setActivePanel(EditorPanel.ARTBOARDS)
            }

            Spacer(Modifier.weight(1f))

            // 7. Export
            DockIconButton(
                icon = Icons.Default.Share,
                label = "Export",
                isActive = uiState.activePanel == EditorPanel.EXPORT,
                tag = "dock_export"
            ) {
                viewModel.setActivePanel(EditorPanel.EXPORT)
            }
        }
    }
}

@Composable
private fun DockIconButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    tag: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (isActive) Color(0xFF00E5FF).copy(alpha = 0.22f) else Color.Transparent)
            .border(
                width = if (isActive) 1.dp else 0.dp,
                color = if (isActive) Color(0xFF00E5FF) else Color.Transparent,
                shape = RoundedCornerShape(6.dp)
            )
            .testTag(tag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isActive) Color(0xFF00E5FF) else Color(0xFF90A4AE),
            modifier = Modifier.size(17.dp)
        )
    }
}
