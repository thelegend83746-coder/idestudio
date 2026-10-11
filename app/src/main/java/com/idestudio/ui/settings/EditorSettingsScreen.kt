package com.idestudio.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idestudio.data.model.EditorSettings
import com.idestudio.data.repository.PreferencesRepository
import com.idestudio.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorSettingsScreen(
    prefRepository: PreferencesRepository,
    onNavigateBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val settings by prefRepository.editorSettings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Editor Settings", fontWeight = FontWeight.Bold, color = TextPrimary) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "TYPOGRAPHY & FORMATTING",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextTertiary
            )

            // Font Size
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Font Size", fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Text("${settings.fontSizeSp.toInt()} sp", color = BrandPrimary, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = settings.fontSizeSp,
                        onValueChange = { newVal ->
                            coroutineScope.launch {
                                prefRepository.updateEditorSettings(settings.copy(fontSizeSp = newVal))
                            }
                        },
                        valueRange = 12f..24f,
                        steps = 11,
                        colors = SliderDefaults.colors(
                            thumbColor = BrandPrimary,
                            activeTrackColor = BrandPrimary
                        )
                    )
                }
            }

            // Tab Size
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Tab Size", fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Text("Number of spaces per indentation level", fontSize = 12.sp, color = TextSecondary)
                    }

                    Row {
                        listOf(2, 4, 8).forEach { size ->
                            FilterChip(
                                selected = settings.tabSize == size,
                                onClick = {
                                    coroutineScope.launch {
                                        prefRepository.updateEditorSettings(settings.copy(tabSize = size))
                                    }
                                },
                                label = { Text("$size", fontSize = 12.sp) },
                                modifier = Modifier.padding(start = 4.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BrandPrimary,
                                    selectedLabelColor = androidx.compose.ui.graphics.Color.White
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "DISPLAY & BEHAVIOR",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextTertiary
            )

            // Switch items
            EditorSettingSwitchCard(
                title = "Show Line Numbers",
                description = "Display line numbers in left editor gutter",
                checked = settings.showLineNumbers,
                onCheckedChange = { checked ->
                    coroutineScope.launch { prefRepository.updateEditorSettings(settings.copy(showLineNumbers = checked)) }
                }
            )

            EditorSettingSwitchCard(
                title = "Highlight Current Line",
                description = "Subtly highlight the active cursor line",
                checked = settings.highlightCurrentLine,
                onCheckedChange = { checked ->
                    coroutineScope.launch { prefRepository.updateEditorSettings(settings.copy(highlightCurrentLine = checked)) }
                }
            )

            EditorSettingSwitchCard(
                title = "Word Wrap",
                description = "Wrap long code lines to avoid horizontal scrolling",
                checked = settings.wordWrap,
                onCheckedChange = { checked ->
                    coroutineScope.launch { prefRepository.updateEditorSettings(settings.copy(wordWrap = checked)) }
                }
            )

            EditorSettingSwitchCard(
                title = "Auto Complete",
                description = "Show intelligent code suggestions while typing",
                checked = settings.autoComplete,
                onCheckedChange = { checked ->
                    coroutineScope.launch { prefRepository.updateEditorSettings(settings.copy(autoComplete = checked)) }
                }
            )

            EditorSettingSwitchCard(
                title = "Dark Editor Theme",
                description = "Use dark slate theme optimized for mobile screens",
                checked = settings.isDarkTheme,
                onCheckedChange = { checked ->
                    coroutineScope.launch { prefRepository.updateEditorSettings(settings.copy(isDarkTheme = checked)) }
                }
            )
        }
    }
}

@Composable
fun EditorSettingSwitchCard(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(modifier = Modifier.height(2.dp))
                Text(description, fontSize = 12.sp, color = TextSecondary, lineHeight = 16.sp)
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = androidx.compose.ui.graphics.Color.White,
                    checkedTrackColor = BrandPrimary
                )
            )
        }
    }
}
