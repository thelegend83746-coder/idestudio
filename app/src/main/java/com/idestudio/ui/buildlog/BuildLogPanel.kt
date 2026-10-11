package com.idestudio.ui.buildlog

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.idestudio.data.model.BuildLogEntry
import com.idestudio.data.model.BuildState
import com.idestudio.data.model.BuildStatus
import com.idestudio.data.model.Diagnostic
import com.idestudio.data.model.LogLevel
import com.idestudio.ui.theme.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun BuildLogPanel(
    buildState: BuildState,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onNavigateToError: (filePath: String, lineNumber: Int) -> Unit,
    onCancelBuild: () -> Unit
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    // Auto-scroll to latest log
    LaunchedEffect(buildState.logs.size) {
        if (buildState.logs.isNotEmpty() && isExpanded) {
            listState.animateScrollToItem(buildState.logs.size - 1)
        }
    }

    Surface(
        color = DarkSurfaceElevated,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Status Badge Icon
                    when (buildState.status) {
                        BuildStatus.SUCCESS -> Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusSuccess, modifier = Modifier.size(18.dp))
                        BuildStatus.FAILURE -> Icon(Icons.Default.Error, contentDescription = null, tint = StatusError, modifier = Modifier.size(18.dp))
                        BuildStatus.CANCELLED -> Icon(Icons.Default.Cancel, contentDescription = null, tint = StatusWarning, modifier = Modifier.size(18.dp))
                        BuildStatus.IDLE -> Icon(Icons.Default.Terminal, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                        else -> CircularProgressIndicator(modifier = Modifier.size(16.dp), color = BrandPrimary, strokeWidth = 2.dp)
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = "Build Output: ${buildState.stageMessage}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = when (buildState.status) {
                            BuildStatus.SUCCESS -> StatusSuccess
                            BuildStatus.FAILURE -> StatusError
                            else -> TextPrimary
                        },
                        maxLines = 1
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (buildState.isRunning) {
                        TextButton(onClick = onCancelBuild, contentPadding = PaddingValues(horizontal = 6.dp)) {
                            Text("Cancel", color = StatusError, fontSize = 12.sp)
                        }
                    }

                    IconButton(onClick = onToggleExpand, modifier = Modifier.size(24.dp)) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            tint = TextSecondary
                        )
                    }
                }
            }

            // Expanded Content
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .background(DarkBackground)
                ) {
                    // Success Banner with Actions (Install / Share)
                    if (buildState.status == BuildStatus.SUCCESS && buildState.outputApkPath != null) {
                        Surface(
                            color = StatusSuccess.copy(alpha = 0.12f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, StatusSuccess.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "APK Generated (${buildState.apkSizeBytes / 1024} KB)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = StatusSuccess
                                    )
                                    Text(
                                        text = buildState.outputApkPath,
                                        fontSize = 10.sp,
                                        color = TextSecondary,
                                        maxLines = 1
                                    )
                                }

                                Row {
                                    FilledTonalButton(
                                        onClick = { installApk(context, File(buildState.outputApkPath)) },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = StatusSuccess)
                                    ) {
                                        Icon(Icons.Default.GetApp, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Install", fontSize = 12.sp, color = Color.White)
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    OutlinedButton(
                                        onClick = { shareApk(context, File(buildState.outputApkPath)) },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp), tint = TextPrimary)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Share", fontSize = 12.sp, color = TextPrimary)
                                    }
                                }
                            }
                        }
                    }

                    // Diagnostic Error Navigation Cards
                    if (buildState.errorDiagnostics.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            for (diag in buildState.errorDiagnostics.take(3)) {
                                Surface(
                                    color = StatusError.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp)
                                        .clickable {
                                            if (diag.filePath != null && diag.lineNumber != null) {
                                                onNavigateToError(diag.filePath, diag.lineNumber)
                                            }
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = StatusError, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            diag.filePath?.let { path ->
                                                Text(
                                                    text = "${File(path).name} : line ${diag.lineNumber ?: 1}",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp,
                                                    color = StatusError
                                                )
                                            }
                                            Text(
                                                text = diag.message,
                                                fontSize = 11.sp,
                                                color = TextPrimary,
                                                maxLines = 2
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Real Logs Stream
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(buildState.logs) { log ->
                            Row(verticalAlignment = Alignment.Top) {
                                Text(
                                    text = timeFormat.format(Date(log.timestamp)),
                                    fontFamily = CodeFontFamily,
                                    fontSize = 11.sp,
                                    color = TextTertiary,
                                    modifier = Modifier.width(55.dp)
                                )

                                Text(
                                    text = log.message,
                                    fontFamily = CodeFontFamily,
                                    fontSize = 11.sp,
                                    color = when (log.level) {
                                        LogLevel.ERROR -> StatusError
                                        LogLevel.WARNING -> StatusWarning
                                        LogLevel.INFO -> TextPrimary
                                    },
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun installApk(context: Context, apkFile: File) {
    if (!apkFile.exists()) return
    try {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {}
}

private fun shareApk(context: Context, apkFile: File) {
    if (!apkFile.exists()) return
    try {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.android.package-archive"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share APK"))
    } catch (_: Exception) {}
}
