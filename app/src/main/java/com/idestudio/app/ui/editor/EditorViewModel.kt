package com.idestudio.app.ui.editor

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.idestudio.app.IdeStudioApp
import com.idestudio.app.data.model.BuildLogEntry
import com.idestudio.app.data.model.BuildStatus
import com.idestudio.app.data.model.EditorTab
import com.idestudio.app.data.model.FileNode
import com.idestudio.app.data.model.Project
import com.idestudio.app.toolchain.BuildPipeline
import com.idestudio.app.util.FileUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class EditorViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as IdeStudioApp
    private val projectRepository = app.projectRepository
    private val buildPipeline = BuildPipeline(app.toolchainManager)

    private val _project = MutableStateFlow<Project?>(null)
    val project: StateFlow<Project?> = _project.asStateFlow()

    private val _fileTree = MutableStateFlow<FileNode?>(null)
    val fileTree: StateFlow<FileNode?> = _fileTree.asStateFlow()

    private val _tabs = MutableStateFlow<List<EditorTab>>(emptyList())
    val tabs: StateFlow<List<EditorTab>> = _tabs.asStateFlow()

    private val _activeTabId = MutableStateFlow<String?>(null)
    val activeTabId: StateFlow<String?> = _activeTabId.asStateFlow()

    val activeTab: EditorTab?
        get() = _tabs.value.find { it.id == _activeTabId.value }

    // Undo / Redo Stacks
    private val undoStacks = mutableMapOf<String, ArrayDeque<String>>()
    private val redoStacks = mutableMapOf<String, ArrayDeque<String>>()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    // Search and Replace
    private val _isSearchVisible = MutableStateFlow(false)
    val isSearchVisible: StateFlow<Boolean> = _isSearchVisible.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _replaceQuery = MutableStateFlow("")
    val replaceQuery: StateFlow<String> = _replaceQuery.asStateFlow()

    private val _searchMatches = MutableStateFlow<List<Int>>(emptyList())
    val searchMatches: StateFlow<List<Int>> = _searchMatches.asStateFlow()

    private val _currentMatchIndex = MutableStateFlow(0)
    val currentMatchIndex: StateFlow<Int> = _currentMatchIndex.asStateFlow()

    // Build State
    val buildStatus: StateFlow<BuildStatus> = buildPipeline.buildStatus
    val buildLogs: StateFlow<List<BuildLogEntry>> = buildPipeline.logs
    val generatedApk: StateFlow<File?> = buildPipeline.generatedApk
    val buildDurationMs: StateFlow<Long> = buildPipeline.buildDurationMs

    private val _showBuildDialog = MutableStateFlow(false)
    val showBuildDialog: StateFlow<Boolean> = _showBuildDialog.asStateFlow()

    fun loadProject(projectId: String) {
        val proj = projectRepository.projects.value.find { it.id == projectId }
            ?: projectRepository.projects.value.find { it.name == projectId }

        if (proj != null) {
            _project.value = proj
            refreshTree()

            // Open MainActivity.java by default if available
            val packageSubPath = proj.packageName.replace('.', '/')
            val mainActivityFile = File(proj.javaDir, "$packageSubPath/${proj.mainActivityName}.java")
            if (mainActivityFile.exists()) {
                openFile(mainActivityFile)
            } else {
                // If not found, open manifest
                if (proj.manifestFile.exists()) {
                    openFile(proj.manifestFile)
                }
            }
        }
    }

    fun refreshTree() {
        val root = _project.value?.rootDir ?: return
        viewModelScope.launch {
            _fileTree.value = FileUtils.buildFileTree(root)
        }
    }

    fun openFile(file: File) {
        if (!file.exists() || file.isDirectory) return

        val existing = _tabs.value.find { it.file.absolutePath == file.absolutePath }
        if (existing != null) {
            _activeTabId.value = existing.id
            updateUndoRedoStates(existing.id)
            return
        }

        val content = try {
            file.readText()
        } catch (e: Exception) {
            "/* Binary or unreadable file: ${file.name} */"
        }

        val newTab = EditorTab(
            file = file,
            title = file.name,
            content = content,
            originalContent = content,
            isModified = false
        )

        _tabs.value = _tabs.value + newTab
        _activeTabId.value = newTab.id

        undoStacks[newTab.id] = ArrayDeque()
        redoStacks[newTab.id] = ArrayDeque()
        updateUndoRedoStates(newTab.id)
    }

    fun selectTab(tab: EditorTab) {
        _activeTabId.value = tab.id
        updateUndoRedoStates(tab.id)
    }

    fun closeTab(tab: EditorTab) {
        val currentList = _tabs.value
        val index = currentList.indexOf(tab)
        val newList = currentList.filter { it.id != tab.id }
        _tabs.value = newList

        if (_activeTabId.value == tab.id) {
            if (newList.isNotEmpty()) {
                val newIndex = (index - 1).coerceAtLeast(0)
                _activeTabId.value = newList[newIndex].id
                updateUndoRedoStates(newList[newIndex].id)
            } else {
                _activeTabId.value = null
                _canUndo.value = false
                _canRedo.value = false
            }
        }
    }

    fun closeAllTabs() {
        _tabs.value = emptyList()
        _activeTabId.value = null
        _canUndo.value = false
        _canRedo.value = false
    }

    fun onContentChanged(newContent: String) {
        val tab = activeTab ?: return
        if (tab.content != newContent) {
            // Push old content to undo stack
            val undoStack = undoStacks.getOrPut(tab.id) { ArrayDeque() }
            if (undoStack.size > 50) undoStack.removeFirst()
            undoStack.addLast(tab.content)

            // Clear redo on new edit
            redoStacks[tab.id]?.clear()

            tab.content = newContent
            tab.isModified = tab.content != tab.originalContent

            // Trigger recomposition by updating tab list
            _tabs.value = _tabs.value.map { if (it.id == tab.id) it else it }
            updateUndoRedoStates(tab.id)
            updateSearchMatches()
        }
    }

    fun undo() {
        val tab = activeTab ?: return
        val undoStack = undoStacks[tab.id] ?: return
        if (undoStack.isNotEmpty()) {
            val prevContent = undoStack.removeLast()
            val redoStack = redoStacks.getOrPut(tab.id) { ArrayDeque() }
            redoStack.addLast(tab.content)

            tab.content = prevContent
            tab.isModified = tab.content != tab.originalContent
            _tabs.value = _tabs.value.map { if (it.id == tab.id) it else it }
            updateUndoRedoStates(tab.id)
            updateSearchMatches()
        }
    }

    fun redo() {
        val tab = activeTab ?: return
        val redoStack = redoStacks[tab.id] ?: return
        if (redoStack.isNotEmpty()) {
            val nextContent = redoStack.removeLast()
            val undoStack = undoStacks.getOrPut(tab.id) { ArrayDeque() }
            undoStack.addLast(tab.content)

            tab.content = nextContent
            tab.isModified = tab.content != tab.originalContent
            _tabs.value = _tabs.value.map { if (it.id == tab.id) it else it }
            updateUndoRedoStates(tab.id)
            updateSearchMatches()
        }
    }

    private fun updateUndoRedoStates(tabId: String) {
        _canUndo.value = (undoStacks[tabId]?.size ?: 0) > 0
        _canRedo.value = (redoStacks[tabId]?.size ?: 0) > 0
    }

    fun saveActiveFile(): Boolean {
        val tab = activeTab ?: return false
        return try {
            tab.file.writeText(tab.content)
            tab.originalContent = tab.content
            tab.isModified = false
            _tabs.value = _tabs.value.map { if (it.id == tab.id) it else it }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun saveAllOpenFiles() {
        _tabs.value.forEach { tab ->
            if (tab.isModified) {
                try {
                    tab.file.writeText(tab.content)
                    tab.originalContent = tab.content
                    tab.isModified = false
                } catch (ignored: Exception) {}
            }
        }
        _tabs.value = _tabs.value.map { it }
    }

    fun createNewFile(parentDir: File, fileName: String): Boolean {
        val file = File(parentDir, fileName)
        if (file.exists()) return false
        return try {
            file.createNewFile()
            refreshTree()
            openFile(file)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun createNewFolder(parentDir: File, folderName: String): Boolean {
        val folder = File(parentDir, folderName)
        if (folder.exists()) return false
        val success = folder.mkdirs()
        if (success) refreshTree()
        return success
    }

    fun renameFileNode(node: FileNode, newName: String): Boolean {
        val target = File(node.file.parentFile, newName)
        if (target.exists()) return false
        val success = node.file.renameTo(target)
        if (success) {
            // Close tab if open
            val openTab = _tabs.value.find { it.file.absolutePath == node.file.absolutePath }
            if (openTab != null) {
                closeTab(openTab)
                openFile(target)
            }
            refreshTree()
        }
        return success
    }

    fun deleteFileNode(node: FileNode): Boolean {
        val openTab = _tabs.value.find { it.file.absolutePath == node.file.absolutePath }
        if (openTab != null) {
            closeTab(openTab)
        }
        val success = node.file.deleteRecursively()
        if (success) refreshTree()
        return success
    }

    // Search and Replace methods
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        updateSearchMatches()
    }

    fun setReplaceQuery(query: String) {
        _replaceQuery.value = query
    }

    fun toggleSearchVisibility(visible: Boolean) {
        _isSearchVisible.value = visible
        if (!visible) {
            _searchQuery.value = ""
            _searchMatches.value = emptyList()
        }
    }

    private fun updateSearchMatches() {
        val query = _searchQuery.value
        val tab = activeTab
        if (query.isEmpty() || tab == null) {
            _searchMatches.value = emptyList()
            _currentMatchIndex.value = 0
            return
        }

        val text = tab.content
        val matches = mutableListOf<Int>()
        var index = text.indexOf(query, 0, ignoreCase = true)
        while (index >= 0) {
            matches.add(index)
            index = text.indexOf(query, index + query.length, ignoreCase = true)
        }
        _searchMatches.value = matches
        _currentMatchIndex.value = 0
    }

    fun findNext() {
        val matches = _searchMatches.value
        if (matches.isNotEmpty()) {
            _currentMatchIndex.value = (_currentMatchIndex.value + 1) % matches.size
        }
    }

    fun findPrevious() {
        val matches = _searchMatches.value
        if (matches.isNotEmpty()) {
            _currentMatchIndex.value = if (_currentMatchIndex.value > 0) _currentMatchIndex.value - 1 else matches.size - 1
        }
    }

    fun replaceCurrent() {
        val tab = activeTab ?: return
        val matches = _searchMatches.value
        if (matches.isEmpty()) return

        val matchPos = matches[_currentMatchIndex.value]
        val queryLen = _searchQuery.value.length
        val rep = _replaceQuery.value

        val newText = tab.content.substring(0, matchPos) + rep + tab.content.substring(matchPos + queryLen)
        onContentChanged(newText)
    }

    fun replaceAll() {
        val tab = activeTab ?: return
        val query = _searchQuery.value
        if (query.isEmpty()) return

        val newText = tab.content.replace(query, _replaceQuery.value, ignoreCase = true)
        onContentChanged(newText)
    }

    // Build Execution
    fun runBuild() {
        val proj = _project.value ?: return
        saveAllOpenFiles()
        _showBuildDialog.value = true

        viewModelScope.launch {
            buildPipeline.executeBuild(proj)
        }
    }

    fun closeBuildDialog() {
        _showBuildDialog.value = false
    }
}
