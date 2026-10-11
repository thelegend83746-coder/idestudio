package com.idestudio.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.idestudio.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEditorSettings: () -> Unit,
    onNavigateToBuildRunSettings: () -> Unit,
    onNavigateToBuildAiSettings: () -> Unit,
    onNavigateToAbout: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    text = "PREFERENCES",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextTertiary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )
            }

            item {
                SettingsCategoryCard(
                    title = "Editor",
                    subtitle = "Font size, tab size, line numbers, syntax highlighting",
                    icon = Icons.Default.Code,
                    iconTint = BrandPrimary,
                    onClick = onNavigateToEditorSettings
                )
            }

            item {
                SettingsCategoryCard(
                    title = "Build & Run",
                    subtitle = "SDK platforms, android.jar, keystore, build pipeline",
                    icon = Icons.Default.Build,
                    iconTint = BrandSecondary,
                    onClick = onNavigateToBuildRunSettings
                )
            }

            item {
                SettingsCategoryCard(
                    title = "Build AI Settings",
                    subtitle = "Ollama Cloud API key, model selection, persona prompts",
                    icon = Icons.Default.AutoAwesome,
                    iconTint = BrandAccentAi,
                    onClick = onNavigateToBuildAiSettings
                )
            }

            item {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "INFORMATION",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextTertiary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )
            }

            item {
                SettingsCategoryCard(
                    title = "About Us",
                    subtitle = "IDE STUDIO version, architecture, licenses",
                    icon = Icons.Default.Info,
                    iconTint = StatusInfo,
                    onClick = onNavigateToAbout
                )
            }
        }
    }
}

@Composable
fun SettingsCategoryCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = subtitle, fontSize = 12.sp, color = TextSecondary, lineHeight = 16.sp)
            }

            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextTertiary)
        }
    }
}
