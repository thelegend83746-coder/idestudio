package com.idestudio.app.ui.home

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.idestudio.app.data.model.Project
import com.idestudio.app.ui.theme.BackgroundLight
import com.idestudio.app.ui.theme.PurplePrimary
import com.idestudio.app.ui.theme.TextPrimary
import com.idestudio.app.ui.theme.TextSecondary
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToTemplatePicker: () -> Unit,
    onNavigateToProject: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToToolchain: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToAiBuilder: () -> Unit
) {
    val context = LocalContext.current
    val projects by viewModel.projects.collectAsState()

    var showMenu by remember { mutableStateOf(false) }

    // Dialog states
    var projectToDelete by remember { mutableStateOf<Project?>(null) }
    var projectToRename by remember { mutableStateOf<Project?>(null) }
    var renameText by remember { mutableStateOf("") }
    var projectToDuplicate by remember { mutableStateOf<Project?>(null) }
    var duplicateText by remember { mutableStateOf("") }
    var projectDetails by remember { mutableStateOf<Project?>(null) }

    // Zip file picker for Import
    val importZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val tempZip = File(context.cacheDir, "imported_${System.currentTimeMillis()}.zip")
                inputStream?.use { input ->
                    tempZip.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                viewModel.importProjectZip(tempZip) { imported ->
                    if (imported != null) {
                        Toast.makeText(context, "Project '${imported.name}' imported successfully!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Failed to import project from ZIP.", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Import error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "IDE STUDIO",
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp,
                        color = TextPrimary,
                        letterSpacing = 0.5.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                ),
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = TextPrimary
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Import Project (ZIP)") },
                            onClick = {
                                showMenu = false
                                importZipLauncher.launch("application/zip")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Toolchain Manager") },
                            onClick = {
                                showMenu = false
                                onNavigateToToolchain()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("AI Builder") },
                            onClick = {
                                showMenu = false
                                onNavigateToAiBuilder()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Settings") },
                            onClick = {
                                showMenu = false
                                onNavigateToSettings()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("About") },
                            onClick = {
                                showMenu = false
                                onNavigateToAbout()
                            }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExpandableFab(
                onCreateNewProject = onNavigateToTemplatePicker,
                onImportProject = { importZipLauncher.launch("application/zip") }
            )
        },
        containerColor = BackgroundLight
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (projects.isEmpty()) {
                // Empty State matching reference
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(Color(0xFFEDE7F6), shape = RoundedCornerShape(20.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = PurplePrimary,
                            modifier = Modifier.size(44.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "No Projects Found",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Tap the + button below to create or import your first Android project.",
                        fontSize = 14.sp,
                        color = TextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = onNavigateToTemplatePicker,
                        colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Create New Project", fontWeight = FontWeight.SemiBold)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = "Projects",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }

                    items(projects, key = { it.id }) { project ->
                        ProjectCard(
                            project = project,
                            onClick = { onNavigateToProject(project.id) },
                            onRename = {
                                projectToRename = project
                                renameText = project.name
                            },
                            onDuplicate = {
                                projectToDuplicate = project
                                duplicateText = "${project.name}_Copy"
                            },
                            onExport = {
                                val exportDir = File("/storage/emulated/0/test-folder/IDE_Studio_Exports")
                                exportDir.mkdirs()
                                val exportFile = File(exportDir, "${project.name}.zip")
                                viewModel.exportProjectZip(project, exportFile) { file ->
                                    if (file != null) {
                                        Toast.makeText(context, "Exported to: ${file.absolutePath}", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, "Failed to export project.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onDelete = {
                                projectToDelete = project
                            }
                        )
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (projectToDelete != null) {
        val target = projectToDelete!!
        AlertDialog(
            onDismissRequest = { projectToDelete = null },
            title = { Text("Delete Project", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to permanently delete '${target.name}' and all its files? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteProject(target) { success ->
                            if (success) {
                                Toast.makeText(context, "Deleted ${target.name}", Toast.LENGTH_SHORT).show()
                            }
                        }
                        projectToDelete = null
                    }
                ) {
                    Text("Delete", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { projectToDelete = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Rename Dialog
    if (projectToRename != null) {
        val target = projectToRename!!
        AlertDialog(
            onDismissRequest = { projectToRename = null },
            title = { Text("Rename Project", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("Project Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (renameText.isNotBlank()) {
                            viewModel.renameProject(target, renameText) { success ->
                                if (success) {
                                    Toast.makeText(context, "Project renamed", Toast.LENGTH_SHORT).show()
                                }
                            }
                            projectToRename = null
                        }
                    }
                ) {
                    Text("Rename", color = PurplePrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { projectToRename = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Duplicate Dialog
    if (projectToDuplicate != null) {
        val target = projectToDuplicate!!
        AlertDialog(
            onDismissRequest = { projectToDuplicate = null },
            title = { Text("Duplicate Project", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = duplicateText,
                    onValueChange = { duplicateText = it },
                    label = { Text("New Project Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (duplicateText.isNotBlank()) {
                            viewModel.duplicateProject(target, duplicateText) { success ->
                                if (success) {
                                    Toast.makeText(context, "Project duplicated", Toast.LENGTH_SHORT).show()
                                }
                            }
                            projectToDuplicate = null
                        }
                    }
                ) {
                    Text("Duplicate", color = PurplePrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { projectToDuplicate = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}
