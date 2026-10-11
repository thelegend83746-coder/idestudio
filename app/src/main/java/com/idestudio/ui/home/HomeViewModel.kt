package com.idestudio.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.idestudio.data.model.Project
import com.idestudio.data.repository.ProjectRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val projects: List<Project> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val projectToDelete: Project? = null,
    val projectToRename: Project? = null
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ProjectRepository(application)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadProjects()
    }

    fun loadProjects() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                repository.refreshProjects()
                repository.projects.collect { list ->
                    _uiState.value = _uiState.value.copy(projects = list, isLoading = false)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to load projects: ${e.message}"
                )
            }
        }
    }

    fun requestDeleteProject(project: Project) {
        _uiState.value = _uiState.value.copy(projectToDelete = project)
    }

    fun dismissDeleteDialog() {
        _uiState.value = _uiState.value.copy(projectToDelete = null)
    }

    fun confirmDeleteProject() {
        val target = _uiState.value.projectToDelete ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(projectToDelete = null, isLoading = true)
            repository.deleteProject(target)
            loadProjects()
        }
    }

    fun requestRenameProject(project: Project) {
        _uiState.value = _uiState.value.copy(projectToRename = project)
    }

    fun dismissRenameDialog() {
        _uiState.value = _uiState.value.copy(projectToRename = null)
    }

    fun confirmRenameProject(newName: String) {
        val target = _uiState.value.projectToRename ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(projectToRename = null, isLoading = true)
            repository.renameProject(target, newName)
            loadProjects()
        }
    }
}
