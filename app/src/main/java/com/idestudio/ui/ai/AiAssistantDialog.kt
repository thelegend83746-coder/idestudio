package com.idestudio.ui.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idestudio.ai.DiffEngine
import com.idestudio.ai.DiffType
import com.idestudio.ai.FileChangeProposal
import com.idestudio.ui.theme.*
import java.io.File

@Composable
fun AiAssistantDialog(
    activeFile: File?,
    isGenerating: Boolean,
    generationError: String?,
    proposals: List<FileChangeProposal>,
    onDismiss: () -> Unit,
    onRequestChange: (prompt: String) -> Unit,
    onApplyProposals: (List<FileChangeProposal>) -> Unit
) {
    var promptInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(BrandAccentAi, RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text("Build AI Assistant", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                activeFile?.let {
                    Text(
                        text = "Target file: ${it.name}",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                if (proposals.isEmpty()) {
                    // Prompt Input State
                    OutlinedTextField(
                        value = promptInput,
                        onValueChange = { promptInput = it },
                        label = { Text("What would you like to build or modify?") },
                        placeholder = { Text("e.g. Add a button that increments a counter and shows a Toast") },
                        minLines = 4,
                        maxLines = 6,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (isGenerating) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = BrandAccentAi)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Generating code changes via Ollama...", fontSize = 13.sp, color = TextSecondary)
                        }
                    }

                    generationError?.let { err ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(err, color = StatusError, fontSize = 12.sp)
                    }
                } else {
                    // Review Diff / Proposals State
                    Text(
                        text = "Proposed Modifications (${proposals.size} files):",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    for (prop in proposals) {
                        Surface(
                            color = DarkBackground,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = prop.relativeFilePath,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = BrandPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))

                                val diffLines = remember(prop) {
                                    val original = if (activeFile != null && activeFile.name == File(prop.relativeFilePath).name) {
                                        try { activeFile.readText() } catch (_: Exception) { "" }
                                    } else ""
                                    DiffEngine.computeDiff(original, prop.modifiedContent)
                                }

                                for (line in diffLines.take(15)) {
                                    val bg = when (line.type) {
                                        DiffType.ADDED -> StatusSuccess.copy(alpha = 0.2f)
                                        DiffType.DELETED -> StatusError.copy(alpha = 0.2f)
                                        DiffType.UNCHANGED -> Color.Transparent
                                    }
                                    val prefix = when (line.type) {
                                        DiffType.ADDED -> "+ "
                                        DiffType.DELETED -> "- "
                                        DiffType.UNCHANGED -> "  "
                                    }
                                    Text(
                                        text = prefix + line.text,
                                        fontFamily = CodeFontFamily,
                                        fontSize = 10.sp,
                                        color = when (line.type) {
                                            DiffType.ADDED -> StatusSuccess
                                            DiffType.DELETED -> StatusError
                                            DiffType.UNCHANGED -> TextSecondary
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(bg)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (proposals.isEmpty()) {
                Button(
                    onClick = { onRequestChange(promptInput) },
                    enabled = promptInput.isNotBlank() && !isGenerating,
                    colors = ButtonDefaults.buttonColors(containerColor = BrandAccentAi)
                ) {
                    Text("Generate")
                }
            } else {
                Button(
                    onClick = { onApplyProposals(proposals) },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusSuccess)
                ) {
                    Text("Apply Changes")
                }
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        containerColor = DarkSurfaceElevated,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary
    )
}
