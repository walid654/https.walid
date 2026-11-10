package com.example.ui.components

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.EditorPanel
import com.example.viewmodel.EditorUiState
import com.example.viewmodel.EditorViewModel

@Composable
fun ContextualTaskBar(
    viewModel: EditorViewModel,
    uiState: EditorUiState,
    modifier: Modifier = Modifier
) {
    if (uiState.selectedElementIds.isEmpty()) return

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFF2A2E3D), RoundedCornerShape(16.dp))
            .testTag("contextual_task_bar"),
        color = Color(0xFF161923).copy(alpha = 0.95f),
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Selection count pill
            Surface(
                color = Color(0xFF00E5FF).copy(alpha = 0.15f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = "${uiState.selectedElementIds.size} Selected",
                    color = Color(0xFF00E5FF),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }

            // Duplicate
            IconButton(
                onClick = { viewModel.duplicateSelectedElements() },
                modifier = Modifier.size(34.dp).testTag("action_duplicate")
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Duplicate",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Bring Forward
            IconButton(
                onClick = { viewModel.bringForward() },
                modifier = Modifier.size(34.dp).testTag("action_bring_forward")
            ) {
                Icon(
                    imageVector = Icons.Default.FlipToFront,
                    contentDescription = "Bring Forward",
                    tint = Color(0xFFB0BEC5),
                    modifier = Modifier.size(18.dp)
                )
            }

            // Send Backward
            IconButton(
                onClick = { viewModel.sendBackward() },
                modifier = Modifier.size(34.dp).testTag("action_send_backward")
            ) {
                Icon(
                    imageVector = Icons.Default.FlipToBack,
                    contentDescription = "Send Backward",
                    tint = Color(0xFFB0BEC5),
                    modifier = Modifier.size(18.dp)
                )
            }

            // Quick Align Center
            IconButton(
                onClick = { viewModel.alignSelected("CENTER_H") },
                modifier = Modifier.size(34.dp).testTag("action_align_center")
            ) {
                Icon(
                    imageVector = Icons.Default.AlignHorizontalCenter,
                    contentDescription = "Center Horizontally",
                    tint = Color(0xFFB0BEC5),
                    modifier = Modifier.size(18.dp)
                )
            }

            // Properties Sheet Opener
            IconButton(
                onClick = { viewModel.setActivePanel(EditorPanel.PROPERTIES) },
                modifier = Modifier.size(34.dp).testTag("action_open_properties")
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Properties",
                    tint = Color(0xFF00E5FF),
                    modifier = Modifier.size(18.dp)
                )
            }

            // Delete
            IconButton(
                onClick = { viewModel.deleteSelectedElements() },
                modifier = Modifier.size(34.dp).testTag("action_delete")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color(0xFFFF5252),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
