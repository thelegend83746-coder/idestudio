package com.idestudio.app.ui.ai

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.idestudio.app.IdeStudioApp
import com.idestudio.app.data.model.AiMessage
import com.idestudio.app.data.model.MessageSender
import com.idestudio.app.data.model.Project
import com.idestudio.app.data.repository.AiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class AiBuilderViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as IdeStudioApp
    private val aiRepository = AiRepository(app.settingsRepository)
    private val projectRepository = app.projectRepository

    val messages: StateFlow<List<AiMessage>> = aiRepository.messages

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private val _currentProject = MutableStateFlow<Project?>(null)
    val currentProject: StateFlow<Project?> = _currentProject.asStateFlow()

    fun loadProject(projectId: String?) {
        if (projectId != null) {
            _currentProject.value = projectRepository.projects.value.find { it.id == projectId }
        }
    }

    fun sendMessage(prompt: String, activeFile: File? = null) {
        if (prompt.isBlank() || _isSending.value) return

        val userMessage = AiMessage(
            sender = MessageSender.USER,
            text = prompt.trim()
        )
        aiRepository.addMessage(userMessage)

        _isSending.value = true
        viewModelScope.launch {
            val content = activeFile?.let { if (it.exists()) it.readText() else null }
            aiRepository.sendMessage(
                userPrompt = prompt.trim(),
                currentProject = _currentProject.value,
                activeFile = activeFile,
                activeFileContent = content
            )
            _isSending.value = false
        }
    }

    fun applyChange(message: AiMessage, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = aiRepository.applyProposedChange(message)
            onComplete(result.isSuccess)
        }
    }

    fun clearChat() {
        aiRepository.clearMessages()
    }
}
