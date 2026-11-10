package com.example.ui.panels

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.EditorUiState
import com.example.viewmodel.EditorViewModel

@Composable
fun AlignPanel(
    viewModel: EditorViewModel,
    uiState: EditorUiState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Align & Distribute",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Align selected elements to each other or to the artboard boundaries.",
            color = Color(0xFF90A4AE),
            fontSize = 12.sp
        )

        // Horizontal Alignment
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Horizontal Alignment", color = Color(0xFFB0BEC5), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AlignButton(Icons.AutoMirrored.Filled.AlignHorizontalLeft, "Left", Modifier.weight(1f)) {
                    viewModel.alignSelected("LEFT")
                }
                AlignButton(Icons.Default.AlignHorizontalCenter, "Center", Modifier.weight(1f)) {
                    viewModel.alignSelected("CENTER_H")
                }
                AlignButton(Icons.AutoMirrored.Filled.AlignHorizontalRight, "Right", Modifier.weight(1f)) {
                    viewModel.alignSelected("RIGHT")
                }
            }
        }

        // Vertical Alignment
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Vertical Alignment", color = Color(0xFFB0BEC5), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AlignButton(Icons.Default.AlignVerticalTop, "Top", Modifier.weight(1f)) {
                    viewModel.alignSelected("TOP")
                }
                AlignButton(Icons.Default.AlignVerticalCenter, "Middle", Modifier.weight(1f)) {
                    viewModel.alignSelected("CENTER_V")
                }
                AlignButton(Icons.Default.AlignVerticalBottom, "Bottom", Modifier.weight(1f)) {
                    viewModel.alignSelected("BOTTOM")
                }
            }
        }
    }
}

@Composable
private fun AlignButton(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .border(1.dp, Color(0xFF2A2E3D), RoundedCornerShape(8.dp)),
        color = Color(0xFF1E222D)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = label, tint = Color(0xFF00E5FF), modifier = Modifier.size(20.dp))
            Text(label, color = Color.White, fontSize = 10.sp)
        }
    }
}
