package com.idestudio.app.ui.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idestudio.app.IdeStudioApp
import com.idestudio.app.ui.theme.BackgroundLight
import com.idestudio.app.ui.theme.CardStroke
import com.idestudio.app.ui.theme.PurplePrimary
import com.idestudio.app.ui.theme.TextPrimary
import com.idestudio.app.ui.theme.TextSecondary
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToToolchain: () -> Unit,
    onNavigateToAbout: () -> Unit
) {
    val context = LocalContext.current
    val repository = (context.applicationContext as IdeStudioApp).settingsRepository
    val settings by repository.settings.collectAsState()

    var apiKeyText by remember(settings.aiApiKey) { mutableStateOf(settings.aiApiKey) }
    var customEndpointText by remember(settings.customAiEndpoint) { mutableStateOf(settings.customAiEndpoint) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = BackgroundLight
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // A. Application Section
            item {
                SettingsSectionHeader("Application")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardStroke)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        SettingSwitchItem(
                            title = "Auto-save files",
                            subtitle = "Automatically save modifications before building",
                            checked = settings.autoSave,
                            onCheckedChange = { repository.updateSettings(settings.copy(autoSave = it)) }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        SettingSwitchItem(
                            title = "Haptic feedback",
                            subtitle = "Vibrate on button taps and build completion",
                            checked = settings.hapticFeedback,
                            onCheckedChange = { repository.updateSettings(settings.copy(hapticFeedback = it)) }
                        )
                    }
                }
            }

            // B. Editor Section
            item {
                SettingsSectionHeader("Editor")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardStroke)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Font Size: ${settings.fontSize} sp",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                        Slider(
                            value = settings.fontSize.toFloat(),
                            onValueChange = {
                                repository.updateSettings(settings.copy(fontSize = it.roundToInt()))
                            },
                            valueRange = 10f..24f,
                            steps = 14
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        SettingSwitchItem(
                            title = "Line numbers",
                            subtitle = "Display line numbers on editor left gutter",
                            checked = settings.showLineNumbers,
                            onCheckedChange = { repository.updateSettings(settings.copy(showLineNumbers = it)) }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        SettingSwitchItem(
                            title = "Syntax highlighting",
                            subtitle = "Colorize Java keywords, strings, XML tags",
                            checked = settings.syntaxHighlighting,
                            onCheckedChange = { repository.updateSettings(settings.copy(syntaxHighlighting = it)) }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        SettingSwitchItem(
                            title = "Auto-indent",
                            subtitle = "Indent new lines automatically matching previous block",
                            checked = settings.autoIndent,
                            onCheckedChange = { repository.updateSettings(settings.copy(autoIndent = it)) }
                        )
                    }
                }
            }

            // C. Build & Run Section
            item {
                SettingsSectionHeader("Build & Run")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardStroke)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        SettingSwitchItem(
                            title = "Auto-install APK",
                            subtitle = "Launch package installer immediately after build succeeds",
                            checked = settings.autoInstallApk,
                            onCheckedChange = { repository.updateSettings(settings.copy(autoInstallApk = it)) }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        SettingSwitchItem(
                            title = "Clean build before run",
                            subtitle = "Delete build/classes and output cache before compiling",
                            checked = settings.cleanBuildBeforeRun,
                            onCheckedChange = { repository.updateSettings(settings.copy(cleanBuildBeforeRun = it)) }
                        )
                    }
                }
            }

            // D. AI Builder Configuration
            item {
                SettingsSectionHeader("AI Builder (Optional)")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardStroke)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Provider: ${settings.aiProvider.uppercase()}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = PurplePrimary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = apiKeyText,
                            onValueChange = {
                                apiKeyText = it
                                repository.updateSettings(settings.copy(aiApiKey = it.trim()))
                            },
                            label = { Text("API Key") },
                            placeholder = { Text("Enter Gemini or OpenAI API Key") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = customEndpointText,
                            onValueChange = {
                                customEndpointText = it
                                repository.updateSettings(settings.copy(customAiEndpoint = it.trim()))
                            },
                            label = { Text("Custom Endpoint (Optional)") },
                            placeholder = { Text("https://your-custom-ai.com/v1") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // E. Quick Links (Toolchain, About)
            item {
                SettingsSectionHeader("Information & Tools")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardStroke)
                ) {
                    Column {
                        SettingLinkItem(
                            title = "Toolchain Manager",
                            subtitle = "Inspect AAPT2, ECJ, D8, and android.jar status",
                            onClick = onNavigateToToolchain
                        )
                        SettingLinkItem(
                            title = "About IDE STUDIO",
                            subtitle = "Version, developer info, and open source licenses",
                            onClick = onNavigateToAbout
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        color = PurplePrimary,
        modifier = Modifier.padding(bottom = 4.dp, top = 4.dp)
    )
}

@Composable
fun SettingSwitchItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text(text = subtitle, fontSize = 11.sp, color = TextSecondary)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = PurplePrimary)
        )
    }
}

@Composable
fun SettingLinkItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text(text = subtitle, fontSize = 11.sp, color = TextSecondary)
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = Color(0xFFBDBDBD),
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}
