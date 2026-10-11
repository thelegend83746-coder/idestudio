package com.idestudio.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object CreateProject : Screen("create_project")
    object Editor : Screen("editor/{projectId}") {
        fun createRoute(projectId: String) = "editor/$projectId"
    }
    object Settings : Screen("settings")
    object EditorSettings : Screen("settings/editor")
    object BuildRunSettings : Screen("settings/build_run")
    object About : Screen("settings/about")
    object BuildAiSettings : Screen("settings/ai")
}
