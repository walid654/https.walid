package com.example.ui.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Layer
import com.example.viewmodel.EditorUiState
import com.example.viewmodel.EditorViewModel

@Composable
fun LayersPanel(
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
                text = "Layers & Hierarchy",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            FilledTonalButton(
                onClick = {
                    val newLayer = Layer(
                        name = "Layer ${uiState.document.layers.size + 1}",
                        colorHex = 0xFFB388FF
                    )
                    viewModel.updateDocument(
                        uiState.document.copy(layers = uiState.document.layers + newLayer)
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
                Text("New Layer", fontSize = 11.sp)
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 260.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(uiState.document.layers) { layer ->
                Surface(
                    color = Color(0xFF1E222D),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2A2E3D))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(layer.colorHex))
                                )
                                Text(
                                    text = layer.name,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Surface(
                                    color = Color(0xFF2E3444),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "${layer.elements.size} objects",
                                        color = Color(0xFFB0BEC5),
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(
                                    onClick = {
                                        val updated = uiState.document.layers.map {
                                            if (it.id == layer.id) it.copy(visible = !it.visible) else it
                                        }
                                        viewModel.updateDocument(uiState.document.copy(layers = updated))
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = if (layer.visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Toggle Visibility",
                                        tint = if (layer.visible) Color(0xFF00E5FF) else Color(0xFF546E7A),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        val updated = uiState.document.layers.map {
                                            if (it.id == layer.id) it.copy(locked = !it.locked) else it
                                        }
                                        viewModel.updateDocument(uiState.document.copy(layers = updated))
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = if (layer.locked) Icons.Default.Lock else Icons.Default.LockOpen,
                                        contentDescription = "Toggle Lock",
                                        tint = if (layer.locked) Color(0xFFFF9100) else Color(0xFF546E7A),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
