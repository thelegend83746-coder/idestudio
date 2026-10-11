package com.idestudio.ui.create

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.idestudio.data.model.Project
import com.idestudio.data.model.Template
import com.idestudio.data.repository.ProjectRepository
import com.idestudio.util.PathValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CreateProjectUiState(
    val currentStep: Int = 1, // Step 1: Template selection, Step 2: Configuration
    val selectedTemplate: Template = Template.ALL_TEMPLATES[0],
    val appName: String = "My Application",
    val packageName: String = "com.example.myapplication",
    val saveLocation: String = "/storage/emulated/0/test-folder/projects",
    val minSdk: Int = 21,
    val targetSdk: Int = 34,
    val language: String = "Java",
    val appNameError: String? = null,
    val packageNameError: String? = null,
    val isCreating: Boolean = false,
    val creationError: String? = null,
    val createdProject: Project? = null
)

class CreateProjectViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ProjectRepository(application)

    private val _uiState = MutableStateFlow(
        CreateProjectUiState(saveLocation = repository.defaultProjectsDir.absolutePath)
    )
    val uiState: StateFlow<CreateProjectUiState> = _uiState.asStateFlow()

    fun selectTemplate(template: Template) {
        _uiState.value = _uiState.value.copy(selectedTemplate = template)
    }

    fun nextStep() {
        if (_uiState.value.currentStep == 1) {
            _uiState.value = _uiState.value.copy(currentStep = 2)
        }
    }

    fun previousStep() {
        if (_uiState.value.currentStep == 2) {
            _uiState.value = _uiState.value.copy(currentStep = 1)
        }
    }

    fun updateAppName(name: String) {
        val autoPkg = "com.example." + name.trim().lowercase().replace("[^a-z0-9_]".toRegex(), "")
        val error = if (!PathValidator.isValidAppName(name)) "Invalid app name (letters, numbers, underscores only)" else null
        _uiState.value = _uiState.value.copy(
            appName = name,
            packageName = if (_uiState.value.packageName.startsWith("com.example.")) autoPkg else _uiState.value.packageName,
            appNameError = error
        )
    }

    fun updatePackageName(pkg: String) {
        val error = if (!PathValidator.isValidPackageName(pkg)) "Invalid package format (e.g. com.example.app)" else null
        _uiState.value = _uiState.value.copy(
            packageName = pkg,
            packageNameError = error
        )
    }

    fun updateMinSdk(api: Int) {
        _uiState.value = _uiState.value.copy(minSdk = api)
    }

    fun createProject() {
        val state = _uiState.value
        if (state.appNameError != null || state.packageNameError != null) return

        if (!PathValidator.isValidAppName(state.appName)) {
            _uiState.value = state.copy(appNameError = "App name cannot be empty")
            return
        }
        if (!PathValidator.isValidPackageName(state.packageName)) {
            _uiState.value = state.copy(packageNameError = "Invalid package name format")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCreating = true, creationError = null)
            val result = repository.createProject(
                name = state.appName.trim(),
                packageName = state.packageName.trim(),
                templateId = state.selectedTemplate.id,
                minSdk = state.minSdk,
                targetSdk = state.targetSdk
            )

            result.onSuccess { project ->
                _uiState.value = _uiState.value.copy(isCreating = false, createdProject = project)
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(isCreating = false, creationError = err.message ?: "Project creation failed")
            }
        }
    }
}
