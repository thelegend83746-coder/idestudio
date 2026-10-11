package com.idestudio.ui.create

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idestudio.data.model.Project
import com.idestudio.data.model.Template
import com.idestudio.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateProjectScreen(
    viewModel: CreateProjectViewModel,
    onNavigateBack: () -> Unit,
    onProjectCreated: (Project) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.createdProject) {
        uiState.createdProject?.let { project ->
            onProjectCreated(project)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.currentStep == 1) "Choose Template" else "Configure Project",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.currentStep == 2) viewModel.previousStep() else onNavigateBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        },
        bottomBar = {
            Surface(
                color = DarkSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (uiState.currentStep == 2) {
                        OutlinedButton(
                            onClick = { viewModel.previousStep() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                        ) {
                            Text("Previous")
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    if (uiState.currentStep == 1) {
                        Button(
                            onClick = { viewModel.nextStep() },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
                        ) {
                            Text("Next")
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    } else {
                        Button(
                            onClick = { viewModel.createProject() },
                            enabled = !uiState.isCreating && uiState.appNameError == null && uiState.packageNameError == null,
                            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
                        ) {
                            if (uiState.isCreating) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Creating...")
                            } else {
                                Text("Create Project")
                            }
                        }
                    }
                }
            }
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.currentStep == 1) {
                // Step 1: Template Selection
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = "SELECT A PROJECT TEMPLATE",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextTertiary,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    items(Template.ALL_TEMPLATES, key = { it.id }) { template ->
                        val isSelected = uiState.selectedTemplate.id == template.id
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) BrandPrimary else DarkBorder,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { viewModel.selectTemplate(template) },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) DarkSurfaceElevated else DarkSurface
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) BrandPrimary else DarkSurfaceElevated),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = when (template.id) {
                                            "simple_app" -> Icons.Default.PhoneAndroid
                                            "fullscreen_app" -> Icons.Default.Fullscreen
                                            "navigation_drawer_app" -> Icons.Default.Menu
                                            "basic_activity" -> Icons.Default.ViewAgenda
                                            "empty_activity" -> Icons.Default.Layers
                                            else -> Icons.Default.Apps
                                        },
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else TextSecondary
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = template.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = template.description,
                                        fontSize = 13.sp,
                                        color = TextSecondary,
                                        lineHeight = 18.sp
                                    )
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = BrandPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Step 2: Configuration
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Text(
                        text = "PROJECT DETAILS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextTertiary
                    )

                    // App Name
                    OutlinedTextField(
                        value = uiState.appName,
                        onValueChange = { viewModel.updateAppName(it) },
                        label = { Text("Application Name") },
                        isError = uiState.appNameError != null,
                        supportingText = {
                            uiState.appNameError?.let { Text(it, color = StatusError) }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Package Name
                    OutlinedTextField(
                        value = uiState.packageName,
                        onValueChange = { viewModel.updatePackageName(it) },
                        label = { Text("Package Name") },
                        isError = uiState.packageNameError != null,
                        supportingText = {
                            uiState.packageNameError?.let { Text(it, color = StatusError) }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Save Location (read only display)
                    OutlinedTextField(
                        value = uiState.saveLocation,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Save Location") },
                        supportingText = { Text("Projects are safely isolated inside device storage", color = TextTertiary) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Language Selection (Fixed to Java as per PRD)
                    OutlinedTextField(
                        value = "Java (Android Native)",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Language") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Minimum SDK Selector
                    var expandedMinSdk by remember { mutableStateOf(false) }
                    val supportedApiList = listOf(21, 23, 24, 26, 28, 29, 30, 31, 32, 33, 34, 35, 36)

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = "API ${uiState.minSdk} (Android ${getAndroidVersionName(uiState.minSdk)})",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Minimum SDK") },
                            trailingIcon = {
                                IconButton(onClick = { expandedMinSdk = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expandedMinSdk = true }
                        )

                        DropdownMenu(
                            expanded = expandedMinSdk,
                            onDismissRequest = { expandedMinSdk = false },
                            modifier = Modifier.background(DarkSurface)
                        ) {
                            supportedApiList.forEach { api ->
                                DropdownMenuItem(
                                    text = { Text("API $api (Android ${getAndroidVersionName(api)})", color = TextPrimary) },
                                    onClick = {
                                        viewModel.updateMinSdk(api)
                                        expandedMinSdk = false
                                    }
                                )
                            }
                        }
                    }

                    uiState.creationError?.let { err ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = StatusError.copy(alpha = 0.15f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = StatusError)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(err, color = StatusError, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun getAndroidVersionName(api: Int): String {
    return when (api) {
        21 -> "5.0 Lollipop"
        23 -> "6.0 Marshmallow"
        24 -> "7.0 Nougat"
        26 -> "8.0 Oreo"
        28 -> "9.0 Pie"
        29 -> "10.0"
        30 -> "11.0"
        31 -> "12.0"
        32 -> "12L"
        33 -> "13.0"
        34 -> "14.0"
        35 -> "15.0"
        36 -> "16.0"
        else -> "$api"
    }
}
