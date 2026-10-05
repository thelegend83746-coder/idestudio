package com.idestudio.app.ui.build

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.idestudio.app.data.model.BuildLogEntry
import com.idestudio.app.data.model.BuildStatus
import com.idestudio.app.ui.theme.BuildErrorRed
import com.idestudio.app.ui.theme.BuildWarningAmber
import com.idestudio.app.ui.theme.ConsoleAccent
import com.idestudio.app.ui.theme.ConsoleBackground
import com.idestudio.app.ui.theme.ConsoleText
import com.idestudio.app.ui.theme.PurplePrimary
import com.idestudio.app.ui.theme.RunGreen
import com.idestudio.app.ui.theme.TextPrimary
import com.idestudio.app.ui.theme.TextSecondary
import com.idestudio.app.util.ApkInstaller
import java.io.File

@Composable
fun BuildLogDialog(
    status: BuildStatus,
    logs: List<BuildLogEntry>,
    durationMs: Long,
    outputApk: File?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()

    // Auto-scroll to latest log entry
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Build Log Output",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        if (durationMs > 0) {
                            Text(
                                text = "Duration: ${String.format("%.2f", durationMs / 1000.0)}s",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    // Status Badge
                    StatusBadge(status)
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Console Log Window
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(ConsoleBackground, RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(logs) { entry ->
                            LogLineItem(entry)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons at Bottom
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Copy Logs Button
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val fullText = logs.joinToString("\n") { "[${it.stage}] ${it.message}" }
                            clipboard.setPrimaryClip(ClipData.newPlainText("IDE Studio Build Logs", fullText))
                            Toast.makeText(context, "Build logs copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy Logs", fontSize = 12.sp)
                    }

                    Row {
                        // Install APK Button if success
                        if (status == BuildStatus.SUCCESS && outputApk != null && outputApk.exists()) {
                            Button(
                                onClick = {
                                    val result = ApkInstaller.installApk(context, outputApk)
                                    if (result.isFailure) {
                                        Toast.makeText(context, "Install error: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = RunGreen),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Icon(Icons.Default.Android, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Install APK", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Close Button
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Done", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: BuildStatus) {
    val (bgColor, textColor, text) = when (status) {
        BuildStatus.RUNNING -> Triple(Color(0xFFE3F2FD), Color(0xFF1976D2), "BUILDING...")
        BuildStatus.SUCCESS -> Triple(Color(0xFFE8F5E9), RunGreen, "SUCCESS")
        BuildStatus.FAILED -> Triple(Color(0xFFFFEBEE), BuildErrorRed, "FAILED")
        BuildStatus.IDLE -> Triple(Color(0xFFF5F5F5), Color.Gray, "IDLE")
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (status) {
                BuildStatus.RUNNING -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        strokeWidth = 2.dp,
                        color = textColor
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                BuildStatus.SUCCESS -> {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = textColor, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                }
                BuildStatus.FAILED -> {
                    Icon(Icons.Default.Error, contentDescription = null, tint = textColor, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                }
                else -> {}
            }

            Text(
                text = text,
                color = textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun LogLineItem(entry: BuildLogEntry) {
    val color = when {
        entry.isError -> BuildErrorRed
        entry.isWarning -> BuildWarningAmber
        entry.isHeader -> ConsoleAccent
        else -> ConsoleText
    }

    val prefix = "[${entry.stage}] "

    Text(
        text = prefix + entry.message,
        fontFamily = FontFamily.Monospace,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        color = color,
        modifier = Modifier.padding(vertical = 1.dp)
    )
}
