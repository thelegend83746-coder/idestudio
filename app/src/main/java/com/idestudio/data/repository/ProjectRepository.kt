package com.idestudio.data.repository

import android.content.Context
import com.idestudio.data.model.FileNode
import com.idestudio.data.model.Project
import com.idestudio.data.templates.TemplateManager
import com.idestudio.util.FileUtils
import com.idestudio.util.PathValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.util.UUID

class ProjectRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    // Projects base directory on device storage
    val defaultProjectsDir: File
        get() {
            val externalDir = File("/storage/emulated/0/test-folder/projects")
            if (!externalDir.exists()) {
                externalDir.mkdirs()
            }
            return if (externalDir.exists() && externalDir.canWrite()) {
                externalDir
            } else {
                File(context.filesDir, "projects").apply { mkdirs() }
            }
        }

    private val _projects = MutableStateFlow<List<Project>>(emptyList())
    val projects: StateFlow<List<Project>> = _projects.asStateFlow()

    suspend fun refreshProjects() = withContext(Dispatchers.IO) {
        val projectList = mutableListOf<Project>()
        val baseDir = defaultProjectsDir
        if (baseDir.exists() && baseDir.isDirectory) {
            val dirs = baseDir.listFiles() ?: emptyArray()
            for (dir in dirs) {
                if (dir.isDirectory) {
                    val metaFile = File(dir, "idestudio.json")
                    if (metaFile.exists()) {
                        try {
                            val content = FileUtils.readFileSafe(metaFile)
                            val p = json.decodeFromString<Project>(content)
                            projectList.add(p)
                        } catch (_: Exception) {
                            // If meta file corrupt, synthesize minimal project info
                            projectList.add(
                                Project(
                                    id = dir.name,
                                    name = dir.name,
                                    packageName = "com.example.${dir.name.lowercase().replace('-', '_')}",
                                    rootPath = dir.absolutePath,
                                    lastModified = dir.lastModified()
                                )
                            )
                        }
                    }
                }
            }
        }
        _projects.value = projectList.sortedByDescending { it.lastModified }
    }

    suspend fun createProject(
        name: String,
        packageName: String,
        templateId: String,
        minSdk: Int,
        targetSdk: Int
    ): Result<Project> = withContext(Dispatchers.IO) {
        try {
            if (!PathValidator.isValidAppName(name)) {
                return@withContext Result.failure(IllegalArgumentException("Invalid application name: $name"))
            }
            if (!PathValidator.isValidPackageName(packageName)) {
                return@withContext Result.failure(IllegalArgumentException("Invalid package name: $packageName"))
            }

            val sanitizedDirName = name.trim().replace("\\s+".toRegex(), "_")
            val projectDir = File(defaultProjectsDir, sanitizedDirName)
            if (projectDir.exists()) {
                return@withContext Result.failure(IllegalStateException("A project named '$name' already exists at ${projectDir.absolutePath}"))
            }

            val project = Project(
                id = UUID.randomUUID().toString(),
                name = name,
                packageName = packageName,
                rootPath = projectDir.absolutePath,
                minSdk = minSdk,
                targetSdk = targetSdk,
                templateId = templateId,
                createdAt = System.currentTimeMillis(),
                lastModified = System.currentTimeMillis()
            )

            TemplateManager.generateProjectFiles(project, context)
            refreshProjects()
            Result.success(project)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteProject(project: Project): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val root = File(project.rootPath)
            if (root.exists()) {
                val deleted = FileUtils.deleteRecursivelySafe(root)
                if (deleted) {
                    refreshProjects()
                    Result.success(true)
                } else {
                    Result.failure(IOException("Failed to delete project directory: ${root.absolutePath}"))
                }
            } else {
                refreshProjects()
                Result.success(true)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun renameProject(project: Project, newName: String): Result<Project> = withContext(Dispatchers.IO) {
        try {
            if (!PathValidator.isValidAppName(newName)) {
                return@withContext Result.failure(IllegalArgumentException("Invalid project name: $newName"))
            }
            val root = File(project.rootPath)
            val updated = project.copy(name = newName, lastModified = System.currentTimeMillis())
            val metaFile = File(root, "idestudio.json")
            if (metaFile.exists()) {
                FileUtils.writeFileSafe(metaFile, json.encodeToString(Project.serializer(), updated))
            }
            refreshProjects()
            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loadProjectTree(project: Project): Result<FileNode> = withContext(Dispatchers.IO) {
        try {
            val rootDir = File(project.rootPath)
            if (!rootDir.exists()) {
                return@withContext Result.failure(IOException("Project directory does not exist: ${rootDir.absolutePath}"))
            }
            val rootNode = buildFileNode(rootDir, isRoot = true)
            Result.success(rootNode)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildFileNode(file: File, isRoot: Boolean = false): FileNode {
        if (!file.isDirectory) {
            return FileNode(file = file, isDirectory = false)
        }
        val childrenFiles = file.listFiles() ?: emptyArray()
        val sortedFiles = childrenFiles.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
        val childrenNodes = sortedFiles.map { buildFileNode(it) }
        return FileNode(
            file = file,
            isDirectory = true,
            isExpanded = isRoot, // Expand root folder by default
            children = childrenNodes
        )
    }
}
