package com.example.ui.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.viewmodel.EditorUiState
import com.example.viewmodel.EditorViewModel

@Composable
fun SwatchesPanel(
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
                text = "Swatches Library",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            FilledTonalButton(
                onClick = {
                    val activeColor = when (val p = uiState.activeFill) {
                        is VpPaint.Solid -> p.color
                        else -> VpColor.rgb(0, 229, 255)
                    }
                    viewModel.addSwatch(
                        Swatch(
                            name = "Custom ${uiState.document.swatches.size + 1}",
                            color = activeColor,
                            isGlobal = true
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
                Text("New Swatch", fontSize = 11.sp)
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 75.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 240.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(uiState.document.swatches) { swatch ->
                val composeColor = ColorEngine.toComposeColor(swatch.color, uiState.document.profile, uiState.softProofCmyk)

                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { viewModel.updateSelectedColor(swatch.color) }
                        .border(1.dp, Color(0xFF2A2E3D), RoundedCornerShape(8.dp)),
                    color = Color(0xFF1E222D)
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .background(composeColor)
                        ) {
                            if (swatch.isSpot) {
                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(2.dp),
                                    color = Color.Black.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(2.dp)
                                ) {
                                    Text("SPOT", color = Color.White, fontSize = 7.sp, modifier = Modifier.padding(2.dp))
                                }
                            }
                        }
                        Text(
                            text = swatch.name,
                            color = Color(0xFFCFD8DC),
                            fontSize = 10.sp,
                            maxLines = 1,
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                }
            }
        }
    }
}
