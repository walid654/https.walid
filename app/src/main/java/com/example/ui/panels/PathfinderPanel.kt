package com.example.ui.panels

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pathfinder.PathfinderOps
import com.example.viewmodel.EditorUiState
import com.example.viewmodel.EditorViewModel

@Composable
fun PathfinderPanel(
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
                text = "Pathfinder Boolean Ops",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${uiState.selectedElementIds.size} Selected",
                color = Color(0xFF00E5FF),
                fontSize = 12.sp
            )
        }

        Text(
            text = "Combine overlapping shapes into clean vector paths. Select 2 overlapping shapes.",
            color = Color(0xFF90A4AE),
            fontSize = 12.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PathfinderCard(
                title = "Unite",
                description = "Merge into one path",
                icon = Icons.Default.AddBox,
                modifier = Modifier.weight(1f)
            ) {
                viewModel.runPathfinder(PathfinderOps.Operation.UNITE)
            }

            PathfinderCard(
                title = "Minus Front",
                description = "Subtract top shape",
                icon = Icons.Default.IndeterminateCheckBox,
                modifier = Modifier.weight(1f)
            ) {
                viewModel.runPathfinder(PathfinderOps.Operation.MINUS_FRONT)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PathfinderCard(
                title = "Intersect",
                description = "Keep overlapping area",
                icon = Icons.Default.FilterFrames,
                modifier = Modifier.weight(1f)
            ) {
                viewModel.runPathfinder(PathfinderOps.Operation.INTERSECT)
            }

            PathfinderCard(
                title = "Exclude",
                description = "Cut out overlap",
                icon = Icons.Default.CropFree,
                modifier = Modifier.weight(1f)
            ) {
                viewModel.runPathfinder(PathfinderOps.Operation.EXCLUDE)
            }
        }
    }
}

@Composable
private fun PathfinderCard(
    title: String,
    description: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .border(1.dp, Color(0xFF2A2E3D), RoundedCornerShape(12.dp)),
        color = Color(0xFF1E222D)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(icon, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(18.dp))
                Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            Text(description, color = Color(0xFF78909C), fontSize = 10.sp)
        }
    }
}
