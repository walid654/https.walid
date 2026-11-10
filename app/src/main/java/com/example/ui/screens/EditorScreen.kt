package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.canvas.VectorCanvas
import com.example.ui.components.*
import com.example.ui.panels.*
import com.example.viewmodel.EditorPanel
import com.example.viewmodel.EditorUiState
import com.example.viewmodel.EditorViewModel

@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    uiState: EditorUiState,
    onBackToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Back navigation handling
    BackHandler {
        if (uiState.activePanel != EditorPanel.NONE) {
            viewModel.setActivePanel(EditorPanel.NONE)
        } else {
            onBackToHome()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFF2C2F3A),
        topBar = {
            TopMenuBar(
                viewModel = viewModel,
                uiState = uiState,
                onBackToHome = onBackToHome
            )
        },
        bottomBar = {
            BottomStatusBar(
                viewModel = viewModel,
                uiState = uiState
            )
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isWideScreen = maxWidth > 680.dp

            Row(modifier = Modifier.fillMaxSize()) {
                // 1. Left Adobe Illustrator Tool Rail (Docked on left)
                ToolRail(
                    viewModel = viewModel,
                    uiState = uiState,
                    modifier = Modifier.fillMaxHeight()
                )

                // 2. Central Design Workspace
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    // Vector Artboard Canvas
                    VectorCanvas(
                        viewModel = viewModel,
                        uiState = uiState,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Touch Shortcut Bar (Shift, Alt, Split, Grid, Bleed)
                    TouchShortcutBar(
                        viewModel = viewModel,
                        uiState = uiState,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 12.dp, bottom = 12.dp)
                    )

                    // Contextual Task Bar near bottom-center when element selected
                    ContextualTaskBar(
                        viewModel = viewModel,
                        uiState = uiState,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp)
                    )

                    // Status notification pill
                    androidx.compose.animation.AnimatedVisibility(
                        visible = uiState.statusMessage != null,
                        enter = fadeIn() + slideInVertically { -it },
                        exit = fadeOut() + slideOutVertically { -it },
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 8.dp)
                    ) {
                        uiState.statusMessage?.let { msg ->
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                                    .clickable { viewModel.clearStatusMessage() },
                                color = Color(0xFF161923).copy(alpha = 0.95f),
                                tonalElevation = 8.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(15.dp))
                                    Text(msg, color = Color.White, fontSize = 11.sp)
                                    Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFF78909C), modifier = Modifier.size(13.dp))
                                }
                            }
                        }
                    }

                    // Mobile Drawer for Active Panel (when on narrow portrait phone)
                    if (!isWideScreen) {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = uiState.activePanel != EditorPanel.NONE,
                            enter = slideInVertically { it } + fadeIn(),
                            exit = slideOutVertically { it } + fadeOut(),
                            modifier = Modifier.align(Alignment.BottomCenter)
                        ) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .navigationBarsPadding(),
                                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                                color = Color(0xFF1E212B),
                                tonalElevation = 12.dp,
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2B2F3D))
                            ) {
                                Column {
                                    // Header close bar
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(36.dp)
                                                .height(4.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(Color(0xFF454D60))
                                        )
                                        IconButton(
                                            onClick = { viewModel.setActivePanel(EditorPanel.NONE) },
                                            modifier = Modifier
                                                .align(Alignment.CenterEnd)
                                                .padding(end = 6.dp)
                                                .size(26.dp)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Close Panel", tint = Color(0xFF90A4AE), modifier = Modifier.size(16.dp))
                                        }
                                    }

                                    PanelContent(viewModel, uiState)
                                }
                            }
                        }
                    }
                }

                // 3. Right Docked Inspector (when on tablet/landscape wide screen)
                if (isWideScreen) {
                    AnimatedVisibility(
                        visible = uiState.activePanel != EditorPanel.NONE,
                        enter = expandHorizontally(expandFrom = Alignment.End) + fadeIn(),
                        exit = shrinkHorizontally(shrinkTowards = Alignment.End) + fadeOut()
                    ) {
                        Surface(
                            modifier = Modifier
                                .width(310.dp)
                                .fillMaxHeight(),
                            color = Color(0xFF1E212B),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2B2F3D))
                        ) {
                            Column {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    IconButton(
                                        onClick = { viewModel.setActivePanel(EditorPanel.NONE) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF90A4AE), modifier = Modifier.size(16.dp))
                                    }
                                }
                                PanelContent(viewModel, uiState)
                            }
                        }
                    }
                }

                // 4. Right Dock Icon Strip (Properties, Layers, Color, Swatches, Pathfinder, Artboards, Export)
                RightDock(
                    viewModel = viewModel,
                    uiState = uiState,
                    modifier = Modifier.fillMaxHeight()
                )
            }
        }
    }
}

@Composable
private fun PanelContent(viewModel: EditorViewModel, uiState: EditorUiState) {
    when (uiState.activePanel) {
        EditorPanel.PROPERTIES -> PropertiesPanel(viewModel, uiState)
        EditorPanel.COLOR -> ColorPanel(viewModel, uiState)
        EditorPanel.SWATCHES -> SwatchesPanel(viewModel, uiState)
        EditorPanel.LAYERS -> LayersPanel(viewModel, uiState)
        EditorPanel.PATHFINDER -> PathfinderPanel(viewModel, uiState)
        EditorPanel.ALIGN -> AlignPanel(viewModel, uiState)
        EditorPanel.ARTBOARDS -> ArtboardsPanel(viewModel, uiState)
        EditorPanel.EXPORT -> ExportDialog(
            viewModel = viewModel,
            uiState = uiState,
            onDismiss = { viewModel.setActivePanel(EditorPanel.NONE) }
        )
        EditorPanel.NONE -> {}
    }
}
