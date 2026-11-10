package com.example.ui.panels

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Artboard
import com.example.model.Bleed
import com.example.viewmodel.EditorUiState
import com.example.viewmodel.EditorViewModel

@Composable
fun ArtboardsPanel(
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Artboards & Print Bleed",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            FilledTonalButton(
                onClick = {
                    val count = uiState.document.artboards.size + 1
                    val newAb = Artboard(
                        name = "Artboard $count",
                        x = (count - 1) * 650f,
                        y = 0f,
                        width = 595.28f,
                        height = 841.89f,
                        bleed = Bleed(9f, 9f, 9f, 9f)
                    )
                    viewModel.openDocument(
                        uiState.document.copy(
                            artboards = uiState.document.artboards + newAb,
                            selectedArtboardId = newAb.id
                        )
                    )
                },
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = Color(0xFF00E5FF).copy(alpha = 0.2f),
                    contentColor = Color(0xFF00E5FF)
                ),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.height(30.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add Artboard", fontSize = 11.sp)
            }
        }

        uiState.document.artboards.forEach { artboard ->
            val isSelected = artboard.id == uiState.document.selectedArtboardId

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable {
                        viewModel.openDocument(uiState.document.copy(selectedArtboardId = artboard.id))
                        viewModel.centerArtboard(artboard)
                    }
                    .border(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) Color(0xFF00E5FF) else Color(0xFF2A2E3D),
                        shape = RoundedCornerShape(10.dp)
                    ),
                color = Color(0xFF1E222D)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(artboard.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(16.dp))
                            }
                        }
                        Text(
                            text = "Size: %.1f × %.1f pt  |  Bleed: %.1f pt (3mm)".format(artboard.width, artboard.height, artboard.bleed.top),
                            color = Color(0xFF90A4AE),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}
