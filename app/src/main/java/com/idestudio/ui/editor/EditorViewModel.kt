package com.idestudio.ui.editor

import android.app.Application
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.idestudio.ai.FileChangeProposal
import com.idestudio.ai.OllamaClient
import com.idestudio.ai.ProjectPatcher
import com.idestudio.data.model.BuildState
import com.idestudio.data.model.EditorSettings
import com.idestudio.data.model.FileNode
import com.idestudio.data.model.Project
import com.idestudio.data.repository.PreferencesRepository
import com.idestudio.data.repository.ProjectRepository
import com.idestudio.engine.BuildEngine
import com.idestudio.util.FileUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException

data class EditorUiState(
    val project: Project? = null,
    val rootFileNode: FileNode? = null,
    val openTabs: List<EditorTab> = emptyList(),
    val activeFile: File? = null,
    val activeContent: TextFieldValue = TextFieldValue(""),
    val isExplorerOpen: Boolean = false,
    val isBuildLogExpanded: Boolean = false,
    val targetErrorLine: Int? = null,
    // AI Assistant
    val isAiAssistantOpen: Boolean = false,
    val isAiGenerating: Boolean = false,
    val aiGenerationError: String? = null,
    val aiProposals: List<FileChangeProposal> = emptyList(),
    val editorMessage: String? = null
)

class EditorViewModel(application: Application) : AndroidViewModel(application) {

    private val projectRepo = ProjectRepository(application)
    private val prefRepo = PreferencesRepository(application)
    val buildEngine = BuildEngine(application)
    private val ollamaClient = OllamaClient()

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    val editorSettings: StateFlow<EditorSettings> = prefRepo.editorSettings
    val buildState: StateFlow<BuildState> = buildEngine.buildState

    // Undo / Redo history
    private val undoStack = mutableListOf<String>()
    private val redoStack = mutableListOf<String>()

    fun initProject(projectId: String) {
        viewModelScope.launch {
            projectRepo.refreshProjects()
            val proj = projectRepo.projects.value.firstOrNull { it.id == projectId }
            if (proj != null) {
                _uiState.update { it.copy(project = proj) }
                loadProjectTree(proj)
                // Open default MainActivity.java if available
                val mainActivity = File(proj.srcDir, "MainActivity.java")
                if (mainActivity.exists()) {
                    openFile(mainActivity)
                }
            }
        }
    }

    fun loadProjectTree(project: Project) {
        viewModelScope.launch {
            val result = projectRepo.loadProjectTree(project)
            result.onSuccess { node ->
                _uiState.update { it.copy(rootFileNode = node) }
            }
        }
    }

    fun toggleExplorer() {
        _uiState.update { it.copy(isExplorerOpen = !it.isExplorerOpen) }
    }

    fun toggleBuildLog() {
        _uiState.update { it.copy(isBuildLogExpanded = !it.isBuildLogExpanded) }
    }

    fun openFile(file: File) {
        if (!file.exists() || file.isDirectory) return

        try {
            val content = FileUtils.readFileSafe(file)
            val currentTabs = _uiState.value.openTabs
            val existingTab = currentTabs.firstOrNull { it.file.absolutePath == file.absolutePath }

            val updatedTabs = if (existingTab == null) {
                currentTabs + EditorTab(file = file, isDirty = false)
            } else {
                currentTabs
            }

            undoStack.clear()
            redoStack.clear()
            undoStack.add(content)

            _uiState.update {
                it.copy(
                    openTabs = updatedTabs,
                    activeFile = file,
                    activeContent = TextFieldValue(content, TextRange(0)),
                    isExplorerOpen = false,
                    targetErrorLine = null
                )
            }
        } catch (_: IOException) {}
    }

    fun closeTab(file: File) {
        val currentTabs = _uiState.value.openTabs
        val remainingTabs = currentTabs.filterNot { it.file.absolutePath == file.absolutePath }
        val nextActive = if (_uiState.value.activeFile?.absolutePath == file.absolutePath) {
            remainingTabs.lastOrNull()?.file
        } else {
            _uiState.value.activeFile
        }

        _uiState.update { it.copy(openTabs = remainingTabs) }

        if (nextActive != null) {
            openFile(nextActive)
        } else {
            _uiState.update { it.copy(activeFile = null, activeContent = TextFieldValue("")) }
        }
    }

    fun onContentChanged(newVal: TextFieldValue) {
        val currentText = _uiState.value.activeContent.text
        if (newVal.text != currentText) {
            if (undoStack.isEmpty() || undoStack.last() != currentText) {
                undoStack.add(currentText)
                if (undoStack.size > 50) undoStack.removeAt(0)
            }
            redoStack.clear()

            val active = _uiState.value.activeFile
            val updatedTabs = _uiState.value.openTabs.map { tab ->
                if (tab.file.absolutePath == active?.absolutePath) tab.copy(isDirty = true) else tab
            }

            _uiState.update {
                it.copy(
                    activeContent = newVal,
                    openTabs = updatedTabs
                )
            }
        } else {
            _uiState.update { it.copy(activeContent = newVal) }
        }
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            val prev = undoStack.removeAt(undoStack.size - 1)
            redoStack.add(_uiState.value.activeContent.text)
            _uiState.update { it.copy(activeContent = TextFieldValue(prev, TextRange(prev.length))) }
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val next = redoStack.removeAt(redoStack.size - 1)
            undoStack.add(_uiState.value.activeContent.text)
            _uiState.update { it.copy(activeContent = TextFieldValue(next, TextRange(next.length))) }
        }
    }

    fun insertSymbol(symbol: String) {
        val current = _uiState.value.activeContent
        val start = current.selection.start.coerceIn(0, current.text.length)
        val end = current.selection.end.coerceIn(0, current.text.length)
        val newText = current.text.replaceRange(start, end, symbol)
        val newCursor = start + symbol.length
        onContentChanged(TextFieldValue(newText, TextRange(newCursor)))
    }

    fun saveActiveFile() {
        val active = _uiState.value.activeFile ?: return
        try {
            FileUtils.writeFileSafe(active, _uiState.value.activeContent.text)
            val updatedTabs = _uiState.value.openTabs.map { tab ->
                if (tab.file.absolutePath == active.absolutePath) tab.copy(isDirty = false) else tab
            }
            _uiState.update {
                it.copy(
                    openTabs = updatedTabs,
                    editorMessage = "Saved ${active.name}"
                )
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(editorMessage = "Error saving: ${e.message}") }
        }
    }

    fun startBuild() {
        saveActiveFile()
        val proj = _uiState.value.project ?: return
        _uiState.update { it.copy(isBuildLogExpanded = true) }
        buildEngine.startBuild(proj, viewModelScope)
    }

    fun cancelBuild() {
        buildEngine.cancelBuild()
    }

    fun navigateToDiagnostic(filePath: String, lineNumber: Int) {
        val file = File(filePath)
        if (file.exists()) {
            openFile(file)
            _uiState.update {
                it.copy(
                    targetErrorLine = lineNumber,
                    isBuildLogExpanded = false
                )
            }
        }
    }

    // AI Assistant Workflows
    fun openAiAssistant() {
        _uiState.update {
            it.copy(
                isAiAssistantOpen = true,
                aiProposals = emptyList(),
                aiGenerationError = null
            )
        }
    }

    fun closeAiAssistant() {
        _uiState.update { it.copy(isAiAssistantOpen = false) }
    }

    fun requestAiAssistance(prompt: String) {
        val proj = _uiState.value.project ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isAiGenerating = true, aiGenerationError = null) }
            val active = _uiState.value.activeFile
            val contextMap = mutableMapOf<String, String>()
            if (active != null) {
                contextMap[active.name] = _uiState.value.activeContent.text
            }

            val settings = prefRepo.aiSettings.value
            val response = ollamaClient.requestAssistance(prompt, contextMap, settings)
            if (response.success) {
                _uiState.update {
                    it.copy(
                        isAiGenerating = false,
                        aiProposals = response.suggestedChanges
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isAiGenerating = false,
                        aiGenerationError = response.errorMessage ?: "Failed to generate AI response"
                    )
                }
            }
        }
    }

    fun applyAiProposals(proposals: List<FileChangeProposal>) {
        val proj = _uiState.value.project ?: return
        val patchResult = ProjectPatcher.applyChanges(proj, proposals)
        if (patchResult.success) {
            closeAiAssistant()
            loadProjectTree(proj)
            // Reload active file if modified
            _uiState.value.activeFile?.let { openFile(it) }
            _uiState.update { it.copy(editorMessage = "Applied AI changes to ${patchResult.modifiedFiles.size} files") }
        } else {
            _uiState.update { it.copy(aiGenerationError = patchResult.errorMessage) }
        }
    }

    // Explorer Operations
    fun createFileInProject(parent: File, name: String) {
        try {
            val target = File(parent, name)
            if (!target.exists()) {
                target.createNewFile()
                _uiState.value.project?.let { loadProjectTree(it) }
                openFile(target)
            }
        } catch (_: Exception) {}
    }

    fun createFolderInProject(parent: File, name: String) {
        try {
            val target = File(parent, name)
            if (!target.exists()) {
                target.mkdirs()
                _uiState.value.project?.let { loadProjectTree(it) }
            }
        } catch (_: Exception) {}
    }

    fun renameFileInProject(target: File, newName: String) {
        try {
            val destination = File(target.parentFile, newName)
            if (target.renameTo(destination)) {
                _uiState.value.project?.let { loadProjectTree(it) }
                if (_uiState.value.activeFile?.absolutePath == target.absolutePath) {
                    openFile(destination)
                }
            }
        } catch (_: Exception) {}
    }

    fun deleteFileInProject(target: File) {
        try {
            FileUtils.deleteRecursivelySafe(target)
            _uiState.value.project?.let { loadProjectTree(it) }
            if (_uiState.value.activeFile?.absolutePath == target.absolutePath) {
                closeTab(target)
            }
        } catch (_: Exception) {}
    }
}
