package com.idestudio.ai

import com.idestudio.data.model.Project
import com.idestudio.util.FileUtils
import com.idestudio.util.PathValidator
import java.io.File
import java.io.IOException

data class PatchApplicationResult(
    val success: Boolean,
    val modifiedFiles: List<File>,
    val backupsCreated: List<File>,
    val errorMessage: String? = null
)

object ProjectPatcher {

    fun applyChanges(
        project: Project,
        proposals: List<FileChangeProposal>
    ): PatchApplicationResult {
        val rootDir = File(project.rootPath)
        val modifiedList = mutableListOf<File>()
        val backupsList = mutableListOf<File>()

        for (prop in proposals) {
            val targetFile = File(rootDir, prop.relativeFilePath)

            // Safeguard 1: Directory traversal check
            if (PathValidator.containsTraversal(prop.relativeFilePath)) {
                return PatchApplicationResult(
                    success = false,
                    modifiedFiles = emptyList(),
                    backupsCreated = backupsList,
                    errorMessage = "Rejected path with directory traversal: ${prop.relativeFilePath}"
                )
            }

            // Safeguard 2: Ensure file is strictly inside project root
            if (!PathValidator.isPathWithinRoot(targetFile, rootDir)) {
                return PatchApplicationResult(
                    success = false,
                    modifiedFiles = emptyList(),
                    backupsCreated = backupsList,
                    errorMessage = "Security violation: Path is outside project directory: ${prop.relativeFilePath}"
                )
            }

            // Safeguard 3: Create backup before overwriting
            if (targetFile.exists()) {
                val backup = FileUtils.createBackup(targetFile)
                if (backup != null) {
                    backupsList.add(backup)
                }
            }

            // Apply change
            try {
                FileUtils.writeFileSafe(targetFile, prop.modifiedContent)
                modifiedList.add(targetFile)
            } catch (e: IOException) {
                // Restore all backups on failure
                rollback(backupsList)
                return PatchApplicationResult(
                    success = false,
                    modifiedFiles = emptyList(),
                    backupsCreated = emptyList(),
                    errorMessage = "Failed to write patch to ${targetFile.name}: ${e.message}"
                )
            }
        }

        return PatchApplicationResult(
            success = true,
            modifiedFiles = modifiedList,
            backupsCreated = backupsList
        )
    }

    fun rollback(backups: List<File>): Boolean {
        var allRestored = true
        for (backup in backups) {
            if (backup.exists()) {
                val originalPath = backup.absolutePath.removeSuffix(".bak")
                val originalFile = File(originalPath)
                val ok = FileUtils.restoreBackup(backup, originalFile)
                if (!ok) allRestored = false
            }
        }
        return allRestored
    }
}
