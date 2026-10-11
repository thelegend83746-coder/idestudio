package com.idestudio.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idestudio.R
import com.idestudio.data.model.Project
import com.idestudio.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onCreateProject: () -> Unit,
    onOpenProject: (Project) -> Unit,
    onOpenSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    var showRenameInput by remember { mutableStateOf("") }

    LaunchedEffect(uiState.projectToRename) {
        if (uiState.projectToRename != null) {
            showRenameInput = uiState.projectToRename?.name ?: ""
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(BrandPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Code,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.app_name),
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = TextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.title_settings),
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateProject,
                containerColor = BrandPrimary,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New Project", fontWeight = FontWeight.SemiBold) }
            )
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.projects.isEmpty() && !uiState.isLoading) {
                // Empty State
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.empty_projects_title),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.empty_projects_desc),
                        fontSize = 14.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onCreateProject,
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Create Project")
                    }
                }
            } else {
                // Project List
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = "RECENT PROJECTS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextTertiary,
                            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                        )
                    }

                    items(uiState.projects, key = { it.id }) { project ->
                        ProjectCard(
                            project = project,
                            onClick = { onOpenProject(project) },
                            onRename = { viewModel.requestRenameProject(project) },
                            onDelete = { viewModel.requestDeleteProject(project) }
                        )
                    }
                }
            }

            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = BrandPrimary
                )
            }
        }
    }

    // Delete Confirmation Dialog
    if (uiState.projectToDelete != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteDialog() },
            title = { Text("Delete Project?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Are you sure you want to delete '${uiState.projectToDelete?.name}'? This will permanently remove all project files from device storage.")
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmDeleteProject() },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.dismissDeleteDialog() }) {
                    Text("Cancel")
                }
            },
            containerColor = DarkSurfaceElevated,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary
        )
    }

    // Rename Dialog
    if (uiState.projectToRename != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissRenameDialog() },
            title = { Text("Rename Project", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = showRenameInput,
                        onValueChange = { showRenameInput = it },
                        label = { Text("Project Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmRenameProject(showRenameInput) },
                    enabled = showRenameInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
                ) {
                    Text("Rename")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.dismissRenameDialog() }) {
                    Text("Cancel")
                }
            },
            containerColor = DarkSurfaceElevated,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary
        )
    }
}

@Composable
fun ProjectCard(
    project: Project,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    var expandedMenu by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy · HH:mm", Locale.getDefault()) }
    val formattedDate = remember(project.lastModified) { dateFormat.format(Date(project.lastModified)) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(BrandPrimary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Android,
                    contentDescription = null,
                    tint = BrandPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = project.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = project.packageName,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Modified: $formattedDate",
                    fontSize = 11.sp,
                    color = TextTertiary
                )
            }

            Box {
                IconButton(onClick = { expandedMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Project options",
                        tint = TextSecondary
                    )
                }

                DropdownMenu(
                    expanded = expandedMenu,
                    onDismissRequest = { expandedMenu = false },
                    modifier = Modifier.background(DarkSurface)
                ) {
                    DropdownMenuItem(
                        text = { Text("Open in Editor", color = TextPrimary) },
                        onClick = {
                            expandedMenu = false
                            onClick()
                        },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = TextSecondary) }
                    )
                    DropdownMenuItem(
                        text = { Text("Rename", color = TextPrimary) },
                        onClick = {
                            expandedMenu = false
                            onRename()
                        },
                        leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null, tint = TextSecondary) }
                    )
                    Divider(color = DarkBorder)
                    DropdownMenuItem(
                        text = { Text("Delete", color = StatusError) },
                        onClick = {
                            expandedMenu = false
                            onDelete()
                        },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = StatusError) }
                    )
                }
            }
        }
    }
}
