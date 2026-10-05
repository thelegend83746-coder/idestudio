package com.idestudio.app.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.idestudio.app.IdeStudioApp
import com.idestudio.app.data.model.Project
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as IdeStudioApp).projectRepository

    val projects: StateFlow<List<Project>> = repository.projects.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun refreshProjects() {
        repository.loadProjects()
    }

    fun deleteProject(project: Project, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val result = repository.deleteProject(project)
            onComplete(result.isSuccess)
        }
    }

    fun renameProject(project: Project, newName: String, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val result = repository.renameProject(project, newName)
            onComplete(result.isSuccess)
        }
    }

    fun duplicateProject(project: Project, newName: String, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val result = repository.duplicateProject(project, newName)
            onComplete(result.isSuccess)
        }
    }

    fun exportProjectZip(project: Project, outputFile: File, onComplete: (File?) -> Unit) {
        viewModelScope.launch {
            val result = repository.exportProjectZip(project, outputFile)
            onComplete(result.getOrNull())
        }
    }

    fun importProjectZip(zipFile: File, onComplete: (Project?) -> Unit) {
        viewModelScope.launch {
            val result = repository.importProjectZip(zipFile)
            onComplete(result.getOrNull())
        }
    }
}
