package com.idestudio.app.ui.template

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.idestudio.app.ui.theme.PurplePrimary
import com.idestudio.app.ui.theme.TextPrimary
import com.idestudio.app.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigureProjectScreen(
    templateId: String,
    onNavigateBack: () -> Unit,
    onProjectCreated: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = (context.applicationContext as IdeStudioApp).projectRepository

    var appName by remember { mutableStateOf("My Application") }
    var projectName by remember { mutableStateOf("MyApplication") }
    var packageName by remember { mutableStateOf("com.example.myapplication") }
    var activityName by remember { mutableStateOf("MainActivity") }
    var minSdkText by remember { mutableStateOf("21") }
    var targetSdkText by remember { mutableStateOf("34") }
    var compileSdkText by remember { mutableStateOf("34") }

    var isCreating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val packageRegex = Regex("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+$")

    fun validate(): Boolean {
        if (appName.isBlank()) {
            errorMessage = "Application name cannot be empty."
            return false
        }
        if (projectName.isBlank()) {
            errorMessage = "Project name cannot be empty."
            return false
        }
        if (!projectName.matches(Regex("^[a-zA-Z0-9_\\-]+$"))) {
            errorMessage = "Project name can only contain letters, digits, '_' or '-'."
            return false
        }
        if (!packageName.matches(packageRegex)) {
            errorMessage = "Invalid package name format (e.g. com.example.app)."
            return false
        }
        if (activityName.isBlank() || !activityName.matches(Regex("^[a-zA-Z][a-zA-Z0-9_]*$"))) {
            errorMessage = "Invalid Main Activity name."
            return false
        }
        val minSdk = minSdkText.toIntOrNull() ?: 21
        val targetSdk = targetSdkText.toIntOrNull() ?: 34
        if (minSdk > targetSdk) {
            errorMessage = "Min SDK ($minSdk) cannot be greater than Target SDK ($targetSdk)."
            return false
        }
        errorMessage = null
        return true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Configure Project",
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
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onNavigateBack,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.width(120.dp),
                        enabled = !isCreating
                    ) {
                        Text("CANCEL", fontWeight = FontWeight.Bold, color = TextSecondary)
                    }

                    Button(
                        onClick = {
                            if (validate()) {
                                isCreating = true
                                scope.launch {
                                    val result = repository.createProject(
                                        name = projectName,
                                        appName = appName,
                                        packageName = packageName,
                                        mainActivityName = activityName,
                                        minSdk = minSdkText.toIntOrNull() ?: 21,
                                        targetSdk = targetSdkText.toIntOrNull() ?: 34,
                                        compileSdk = compileSdkText.toIntOrNull() ?: 34,
                                        templateId = templateId
                                    )
                                    isCreating = false
                                    if (result.isSuccess) {
                                        val project = result.getOrThrow()
                                        Toast.makeText(context, "Project '${project.name}' created!", Toast.LENGTH_SHORT).show()
                                        onProjectCreated(project.id)
                                    } else {
                                        errorMessage = result.exceptionOrNull()?.message ?: "Failed to create project."
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.width(120.dp),
                        enabled = !isCreating
                    ) {
                        if (isCreating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("CREATE", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        },
        containerColor = BackgroundLight
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            if (errorMessage != null) {
                Surface(
                    color = Color(0xFFFFEBEE),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Text(
                        text = errorMessage!!,
                        color = Color(0xFFC62828),
                        modifier = Modifier.padding(12.dp),
                        fontSize = 13.sp
                    )
                }
            }

            OutlinedTextField(
                value = appName,
                onValueChange = {
                    appName = it
                    projectName = it.replace("[^a-zA-Z0-9_]".toRegex(), "")
                    val safePkg = it.lowercase().replace("[^a-z0-9]".toRegex(), "")
                    packageName = "com.example.$safePkg"
                },
                label = { Text("Application Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = projectName,
                onValueChange = { projectName = it },
                label = { Text("Project Name (Folder)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = packageName,
                onValueChange = { packageName = it },
                label = { Text("Package Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = activityName,
                onValueChange = { activityName = it },
                label = { Text("Main Activity Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = minSdkText,
                    onValueChange = { minSdkText = it },
                    label = { Text("Min SDK") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = targetSdkText,
                    onValueChange = { targetSdkText = it },
                    label = { Text("Target SDK") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = compileSdkText,
                    onValueChange = { compileSdkText = it },
                    label = { Text("Compile SDK") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Target Language: Java & XML Resources",
                fontSize = 13.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Project Location: /storage/emulated/0/test-folder/IDE_Studio_Projects",
                fontSize = 11.sp,
                color = TextSecondary
            )
        }
    }
}
