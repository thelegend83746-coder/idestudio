package com.idestudio.ui.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idestudio.ai.ConnectionTestResult
import com.idestudio.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildAiScreen(
    viewModel: BuildAiViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var isApiKeyVisible by remember { mutableStateOf(false) }
    var isModelDropdownExpanded by remember { mutableStateOf(false) }

    val defaultModelOptions = listOf(
        "qwen2.5-coder:7b",
        "qwen2.5-coder:14b",
        "deepseek-coder:6.7b",
        "llama3.2:3b",
        "codellama:7b"
    )

    val availableModels = remember(uiState.discoveredModels) {
        if (uiState.discoveredModels.isNotEmpty()) {
            (uiState.discoveredModels + defaultModelOptions).distinct()
        } else {
            defaultModelOptions
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Build AI Settings", fontWeight = FontWeight.Bold, color = TextPrimary) },
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
                text = "PROVIDER CONFIGURATION",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextTertiary
            )

            // Endpoint URL
            OutlinedTextField(
                value = uiState.endpointUrl,
                onValueChange = { viewModel.updateEndpoint(it) },
                label = { Text("Provider Endpoint URL") },
                placeholder = { Text("https://api.ollama.com") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // API Key
            OutlinedTextField(
                value = uiState.apiKey,
                onValueChange = { viewModel.updateApiKey(it) },
                label = { Text("Ollama Cloud API Key") },
                placeholder = { Text("ollama_key_...") },
                singleLine = true,
                visualTransformation = if (isApiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { isApiKeyVisible = !isApiKeyVisible }) {
                        Icon(
                            imageVector = if (isApiKeyVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (isApiKeyVisible) "Hide key" else "Show key",
                            tint = TextSecondary
                        )
                    }
                },
                supportingText = { Text("API keys are never written to project files or logs", color = TextTertiary) },
                modifier = Modifier.fillMaxWidth()
            )

            // Test Connection Action & Result
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { viewModel.testConnection() },
                    enabled = !uiState.isTesting,
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
                ) {
                    if (uiState.isTesting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Testing...")
                    } else {
                        Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Test Connection")
                    }
                }
            }

            // Connection Result Badge
            val currentTestResult = uiState.testResult
            if (currentTestResult != null) {
                when (currentTestResult) {
                    is ConnectionTestResult.Success -> {
                        Surface(
                            color = StatusSuccess.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusSuccess)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(currentTestResult.message, fontWeight = FontWeight.Bold, color = StatusSuccess, fontSize = 13.sp)
                                    Text("Latency: ${currentTestResult.latencyMs}ms · Models found: ${currentTestResult.availableModels.size}", color = TextSecondary, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                    is ConnectionTestResult.Failure -> {
                        Surface(
                            color = StatusError.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = StatusError)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(currentTestResult.message, color = StatusError, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "MODEL & PERSONA",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextTertiary
            )

            // Active Model Dropdown
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = uiState.activeModel,
                    onValueChange = { viewModel.updateActiveModel(it) },
                    label = { Text("Active Model") },
                    trailingIcon = {
                        IconButton(onClick = { isModelDropdownExpanded = true }) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                DropdownMenu(
                    expanded = isModelDropdownExpanded,
                    onDismissRequest = { isModelDropdownExpanded = false },
                    modifier = Modifier.background(DarkSurface)
                ) {
                    availableModels.forEach { model ->
                        DropdownMenuItem(
                            text = { Text(model, color = TextPrimary) },
                            onClick = {
                                viewModel.updateActiveModel(model)
                                isModelDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // System Instructions
            OutlinedTextField(
                value = uiState.systemInstructions,
                onValueChange = { viewModel.updateSystemInstructions(it) },
                label = { Text("System Instructions or Persona") },
                minLines = 4,
                maxLines = 8,
                modifier = Modifier.fillMaxWidth()
            )

            // Save Settings Button
            Button(
                onClick = { viewModel.saveSettings() },
                colors = ButtonDefaults.buttonColors(containerColor = BrandAccentAi),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Settings", fontWeight = FontWeight.SemiBold)
            }

            if (uiState.isSavedSuccess) {
                Text(
                    text = "Settings saved persistently.",
                    color = StatusSuccess,
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}
