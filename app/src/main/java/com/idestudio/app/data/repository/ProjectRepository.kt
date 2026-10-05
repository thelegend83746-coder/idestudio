package com.idestudio.app.data.repository

import android.content.Context
import com.idestudio.app.data.model.Project
import com.idestudio.app.util.TemplateManager
import com.idestudio.app.util.ZipManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.util.UUID

class ProjectRepository(
    private val context: Context,
    private val projectsBaseDir: File
) {

    private val _projects = MutableStateFlow<List<Project>>(emptyList())
    val projects: StateFlow<List<Project>> = _projects.asStateFlow()

    init {
        loadProjects()
    }

    fun loadProjects() {
        if (!projectsBaseDir.exists()) {
            projectsBaseDir.mkdirs()
        }

        val list = mutableListOf<Project>()
        val projectDirs = projectsBaseDir.listFiles { file -> file.isDirectory } ?: emptyArray()

        for (dir in projectDirs) {
            val metaFile = File(dir, "IDE_STUDIO_PROJECT.json")
            if (metaFile.exists()) {
                try {
                    val content = metaFile.readText()
                    val json = JSONObject(content)
                    val project = Project.fromJson(json)
                    list.add(project)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                // If directory exists but no JSON, try creating a minimal project descriptor if it looks like an android project
                val manifest = File(dir, "app/src/main/AndroidManifest.xml")
                if (manifest.exists()) {
                    val p = Project(
                        id = UUID.randomUUID().toString(),
                        name = dir.name,
                        appName = dir.name,
                        packageName = "com.example." + dir.name.lowercase().replace("[^a-z0-9]".toRegex(), ""),
                        mainActivityName = "MainActivity",
                        rootPath = dir.absolutePath,
                        lastModified = dir.lastModified()
                    )
                    list.add(p)
                    try {
                        metaFile.writeText(p.toJson().toString(2))
                    } catch (ignored: Exception) {}
                }
            }
        }

        list.sortByDescending { it.lastModified }
        _projects.value = list
    }

    suspend fun createProject(
        name: String,
        appName: String,
        packageName: String,
        mainActivityName: String,
        minSdk: Int,
        targetSdk: Int,
        compileSdk: Int,
        templateId: String
    ): Result<Project> = withContext(Dispatchers.IO) {
        try {
            val safeProjectName = name.trim().replace("[^a-zA-Z0-9_\\-]".toRegex(), "_")
            val projectDir = File(projectsBaseDir, safeProjectName)
            if (projectDir.exists()) {
                return@withContext Result.failure(IllegalArgumentException("Project with name '$safeProjectName' already exists."))
            }

            val project = Project(
                id = UUID.randomUUID().toString(),
                name = safeProjectName,
                appName = appName.trim(),
                packageName = packageName.trim(),
                mainActivityName = mainActivityName.trim(),
                rootPath = projectDir.absolutePath,
                minSdk = minSdk,
                targetSdk = targetSdk,
                compileSdk = compileSdk,
                templateId = templateId,
                lastModified = System.currentTimeMillis(),
                createdTime = System.currentTimeMillis()
            )

            TemplateManager.generateProjectStructure(project)
            loadProjects()
            Result.success(project)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun deleteProject(project: Project): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val dir = project.rootDir
            val success = dir.deleteRecursively()
            loadProjects()
            Result.success(success)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun renameProject(project: Project, newName: String): Result<Project> = withContext(Dispatchers.IO) {
        try {
            val safeName = newName.trim().replace("[^a-zA-Z0-9_\\-]".toRegex(), "_")
            val targetDir = File(projectsBaseDir, safeName)
            if (targetDir.exists()) {
                return@withContext Result.failure(IllegalArgumentException("Target project folder already exists."))
            }

            val currentDir = project.rootDir
            val renamed = currentDir.renameTo(targetDir)
            if (!renamed) {
                return@withContext Result.failure(IllegalStateException("Failed to rename project directory."))
            }

            val updated = project.copy(
                name = safeName,
                rootPath = targetDir.absolutePath,
                lastModified = System.currentTimeMillis()
            )
            val metaFile = File(targetDir, "IDE_STUDIO_PROJECT.json")
            metaFile.writeText(updated.toJson().toString(2))

            loadProjects()
            Result.success(updated)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun duplicateProject(project: Project, newName: String): Result<Project> = withContext(Dispatchers.IO) {
        try {
            val safeName = newName.trim().replace("[^a-zA-Z0-9_\\-]".toRegex(), "_")
            val targetDir = File(projectsBaseDir, safeName)
            if (targetDir.exists()) {
                return@withContext Result.failure(IllegalArgumentException("Target project folder already exists."))
            }

            project.rootDir.copyRecursively(targetDir, overwrite = false)
            val duplicated = project.copy(
                id = UUID.randomUUID().toString(),
                name = safeName,
                rootPath = targetDir.absolutePath,
                createdTime = System.currentTimeMillis(),
                lastModified = System.currentTimeMillis()
            )
            val metaFile = File(targetDir, "IDE_STUDIO_PROJECT.json")
            metaFile.writeText(duplicated.toJson().toString(2))

            loadProjects()
            Result.success(duplicated)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun exportProjectZip(project: Project, outputZipFile: File): Result<File> = withContext(Dispatchers.IO) {
        try {
            val success = ZipManager.zipDirectory(project.rootDir, outputZipFile)
            if (success) {
                Result.success(outputZipFile)
            } else {
                Result.failure(IllegalStateException("Failed to zip project."))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun importProjectZip(zipFile: File): Result<Project> = withContext(Dispatchers.IO) {
        try {
            val tempDir = File(projectsBaseDir, "import_temp_" + System.currentTimeMillis())
            val unzipSuccess = ZipManager.unzipDirectory(zipFile, tempDir)
            if (!unzipSuccess) {
                tempDir.deleteRecursively()
                return@withContext Result.failure(IllegalStateException("Failed to extract ZIP archive."))
            }

            val metaFile = File(tempDir, "IDE_STUDIO_PROJECT.json")
            val project: Project = if (metaFile.exists()) {
                val json = JSONObject(metaFile.readText())
                val originalName = json.getString("name")
                val finalDir = File(projectsBaseDir, originalName)
                if (finalDir.exists()) {
                    val uniqueDir = File(projectsBaseDir, originalName + "_" + System.currentTimeMillis())
                    tempDir.renameTo(uniqueDir)
                    Project.fromJson(json).copy(rootPath = uniqueDir.absolutePath, name = uniqueDir.name)
                } else {
                    tempDir.renameTo(finalDir)
                    Project.fromJson(json).copy(rootPath = finalDir.absolutePath)
                }
            } else {
                val projectName = zipFile.nameWithoutExtension.replace("[^a-zA-Z0-9_\\-]".toRegex(), "_")
                val finalDir = File(projectsBaseDir, projectName)
                tempDir.renameTo(finalDir)
                Project(
                    id = UUID.randomUUID().toString(),
                    name = projectName,
                    appName = projectName,
                    packageName = "com.example." + projectName.lowercase(),
                    mainActivityName = "MainActivity",
                    rootPath = finalDir.absolutePath
                )
            }

            File(project.rootPath, "IDE_STUDIO_PROJECT.json").writeText(project.toJson().toString(2))
            loadProjects()
            Result.success(project)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
