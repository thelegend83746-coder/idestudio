package com.idestudio.ui.settings

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idestudio.data.model.SdkPlatformInfo
import com.idestudio.data.model.SdkStatus
import com.idestudio.engine.SdkPlatformManager
import com.idestudio.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildRunSettingsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val sdkManager = remember { SdkPlatformManager(context) }
    var platforms by remember { mutableStateOf<List<SdkPlatformInfo>>(emptyList()) }

    LaunchedEffect(Unit) {
        platforms = sdkManager.getPlatformInfoList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Build & Run Settings", fontWeight = FontWeight.Bold, color = TextPrimary) },
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "TOOLCHAIN ARCHITECTURE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextTertiary
                )
            }

            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Speed, contentDescription = null, tint = BrandSecondary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("On-Device Build Pipeline", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Pipeline uses ECJ for Java compilation, AAPT2 for resource processing, D8 for bytecode conversion, and on-device cryptographic APK signing.",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            lineHeight = 17.sp
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "PLATFORM API AVAILABILITY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextTertiary
                )
            }

            items(platforms, key = { it.apiLevel }) { platform ->
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "API ${platform.apiLevel} · Android ${platform.versionName} (${platform.codeName})",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = platform.androidJarPath ?: "Path: not found in local SDK search dirs",
                                fontSize = 11.sp,
                                color = TextTertiary,
                                maxLines = 1
                            )
                        }

                        // Real Status Pill
                        val (statusText, statusBg, statusFg) = when (platform.status) {
                            SdkStatus.READY, SdkStatus.INSTALLED -> Triple("READY", StatusSuccess.copy(alpha = 0.2f), StatusSuccess)
                            SdkStatus.UNSUPPORTED -> Triple("UNSUPPORTED", StatusWarning.copy(alpha = 0.2f), StatusWarning)
                            SdkStatus.MISSING -> Triple("MISSING", StatusError.copy(alpha = 0.2f), StatusError)
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(statusBg)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = statusText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = statusFg
                            )
                        }
                    }
                }
            }
        }
    }
}
