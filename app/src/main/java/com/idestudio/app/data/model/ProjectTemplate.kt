package com.idestudio.app.data.model

data class ProjectTemplate(
    val id: String,
    val name: String,
    val description: String,
    val defaultMainActivity: String = "MainActivity",
    val previewType: TemplatePreviewType = TemplatePreviewType.SIMPLE
)

enum class TemplatePreviewType {
    SIMPLE
}

object ProjectTemplates {
    val TEMPLATES = listOf(
        ProjectTemplate(
            id = "simple_app",
            name = "Standard App",
            description = "Single-screen Android application with MainActivity, layout, and resource configurations.",
            defaultMainActivity = "MainActivity",
            previewType = TemplatePreviewType.SIMPLE
        )
    )

    fun getById(id: String): ProjectTemplate {
        return TEMPLATES.first()
    }
}
