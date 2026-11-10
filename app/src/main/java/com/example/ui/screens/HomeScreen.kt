package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.repository.ProjectRepository
import com.example.ui.panels.DOCUMENT_PRESETS
import com.example.ui.panels.NewDocumentDialog
import com.example.viewmodel.EditorViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    viewModel: EditorViewModel,
    onOpenEditor: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { ProjectRepository(context) }
    var savedProjects by remember { mutableStateOf(repository.listSavedProjects()) }
    var showNewDocDialog by remember { mutableStateOf(false) }

    // System File Picker for Opening Vector/JSON/SVG Files
    val openFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            // Open default template or import
            val defaultTpl = repository.getTemplates().first()
            viewModel.openDocument(defaultTpl)
            onOpenEditor()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F1117))
            .statusBarsPadding()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 20.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            // Header: Studio Title + Primary "New File" and "Open File" Actions
            item {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "VERTOR PHICS PRO",
                                color = Color(0xFF00E5FF),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 2.sp
                            )
                            Text(
                                text = "Vector Workspace",
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Top Action Buttons: New File & Open File
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { openFileLauncher.launch("*/*") },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF37474F)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("home_open_file_btn")
                            ) {
                                Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF00E5FF))
                                Spacer(Modifier.width(6.dp))
                                Text("Open File", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }

                            FilledTonalButton(
                                onClick = { showNewDocDialog = true },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = Color(0xFF00E5FF),
                                    contentColor = Color(0xFF00363A)
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("home_new_file_btn")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("New File", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Quick Category Presets (Mobile, Website, A4 Print, Social Media)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Quick Sizes & Aspect Ratios",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // A4 Print
                        PresetQuickCard(
                            title = "A4 Print",
                            ratio = "1:1.41",
                            desc = "210×297 mm • CMYK",
                            color = Color(0xFFFF4081),
                            modifier = Modifier.weight(1f)
                        ) {
                            val preset = DOCUMENT_PRESETS.first { it.name == "A4 Print" }
                            viewModel.createNewDocument(
                                "A4 Document",
                                preset.widthPt,
                                preset.heightPt,
                                preset.unit,
                                preset.defaultMode,
                                ColorProfile.US_WEB_COATED_SWOP,
                                preset.defaultPpi,
                                Bleed(preset.defaultBleedPt, preset.defaultBleedPt, preset.defaultBleedPt, preset.defaultBleedPt)
                            )
                            onOpenEditor()
                        }

                        // Mobile Screen
                        PresetQuickCard(
                            title = "Mobile Screen",
                            ratio = "9:19.5",
                            desc = "390×844 px • RGB",
                            color = Color(0xFF00E5FF),
                            modifier = Modifier.weight(1f)
                        ) {
                            val preset = DOCUMENT_PRESETS.first { it.name == "Mobile Screen" }
                            viewModel.createNewDocument(
                                "Mobile UI",
                                preset.widthPt,
                                preset.heightPt,
                                preset.unit,
                                preset.defaultMode,
                                ColorProfile.SRGB,
                                preset.defaultPpi,
                                Bleed()
                            )
                            onOpenEditor()
                        }

                        // Web 1080p
                        PresetQuickCard(
                            title = "Web 1080p",
                            ratio = "16:9",
                            desc = "1920×1080 px • RGB",
                            color = Color(0xFFB388FF),
                            modifier = Modifier.weight(1f)
                        ) {
                            val preset = DOCUMENT_PRESETS.first { it.name == "Desktop 1080p" }
                            viewModel.createNewDocument(
                                "Web Layout",
                                preset.widthPt,
                                preset.heightPt,
                                preset.unit,
                                preset.defaultMode,
                                ColorProfile.SRGB,
                                preset.defaultPpi,
                                Bleed()
                            )
                            onOpenEditor()
                        }
                    }
                }
            }

            // Recent Projects List
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Recent Projects",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${savedProjects.size} saved",
                        color = Color(0xFF78909C),
                        fontSize = 12.sp
                    )
                }
            }

            if (savedProjects.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp)),
                        color = Color(0xFF161923),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2A2E3D))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, tint = Color(0xFF546E7A), modifier = Modifier.size(36.dp))
                            Text("No saved documents", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text("Tap 'New File' or choose a quick size preset above to start designing.", color = Color(0xFF78909C), fontSize = 12.sp)
                        }
                    }
                }
            } else {
                items(savedProjects) { project ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                viewModel.openDocument(project)
                                onOpenEditor()
                            }
                            .border(1.dp, Color(0xFF2A2E3D), RoundedCornerShape(12.dp)),
                        color = Color(0xFF161923)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    modifier = Modifier.size(40.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF1E222D)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Palette,
                                        contentDescription = null,
                                        tint = if (project.colorMode == ColorMode.CMYK) Color(0xFFFF4081) else Color(0xFF00E5FF),
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = project.name,
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    val dateStr = SimpleDateFormat("MMM d, yyyy  h:mm a", Locale.getDefault()).format(Date(project.lastModified))
                                    Text(
                                        text = "${project.colorMode} (${project.profile.displayName}) • $dateStr",
                                        color = Color(0xFF90A4AE),
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                FilledTonalButton(
                                    onClick = {
                                        viewModel.openDocument(project)
                                        onOpenEditor()
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    modifier = Modifier.height(30.dp),
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = Color(0xFF00E5FF).copy(alpha = 0.15f),
                                        contentColor = Color(0xFF00E5FF)
                                    )
                                ) {
                                    Text("Open", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                IconButton(
                                    onClick = {
                                        repository.deleteProject(project.id)
                                        savedProjects = repository.listSavedProjects()
                                    },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Delete",
                                        tint = Color(0xFF78909C),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // New Document Modal Dialog
        if (showNewDocDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                NewDocumentDialog(
                    onDismiss = { showNewDocDialog = false },
                    onCreateDocument = { name, w, h, units, mode, profile, ppi, bleed ->
                        viewModel.createNewDocument(name, w, h, units, mode, profile, ppi, bleed)
                        showNewDocDialog = false
                        onOpenEditor()
                    }
                )
            }
        }
    }
}

@Composable
private fun PresetQuickCard(
    title: String,
    ratio: String,
    desc: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .border(1.dp, Color(0xFF2A2E3D), RoundedCornerShape(12.dp)),
        color = Color(0xFF161923)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Surface(
                color = color.copy(alpha = 0.2f),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = ratio,
                    color = color,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
            Text(title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(desc, color = Color(0xFF78909C), fontSize = 9.sp, maxLines = 1)
        }
    }
}
