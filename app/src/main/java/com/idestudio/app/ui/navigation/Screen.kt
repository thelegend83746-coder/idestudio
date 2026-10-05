package com.idestudio.app.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Home : Screen("home")
    object TemplatePicker : Screen("template_picker")
    object ConfigureProject : Screen("configure_project/{templateId}") {
        fun createRoute(templateId: String) = "configure_project/$templateId"
    }
    object Editor : Screen("editor/{projectId}") {
        fun createRoute(projectId: String) = "editor/$projectId"
    }
    object Toolchain : Screen("toolchain")
    object Settings : Screen("settings")
    object About : Screen("about")
    object AiBuilder : Screen("ai_builder?projectId={projectId}") {
        fun createRoute(projectId: String? = null) = if (projectId != null) "ai_builder?projectId=$projectId" else "ai_builder"
    }
}
