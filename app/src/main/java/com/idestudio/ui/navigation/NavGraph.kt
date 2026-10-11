package com.idestudio.ui.navigation

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.idestudio.data.repository.PreferencesRepository
import com.idestudio.ui.ai.BuildAiScreen
import com.idestudio.ui.ai.BuildAiViewModel
import com.idestudio.ui.create.CreateProjectScreen
import com.idestudio.ui.create.CreateProjectViewModel
import com.idestudio.ui.editor.EditorScreen
import com.idestudio.ui.editor.EditorViewModel
import com.idestudio.ui.home.HomeScreen
import com.idestudio.ui.home.HomeViewModel
import com.idestudio.ui.settings.AboutScreen
import com.idestudio.ui.settings.BuildRunSettingsScreen
import com.idestudio.ui.settings.EditorSettingsScreen
import com.idestudio.ui.settings.SettingsScreen

@Composable
fun AppNavGraph(
    navController: NavHostController
) {
    val context = LocalContext.current
    val prefRepository = PreferencesRepository(context)

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            val homeViewModel: HomeViewModel = viewModel()
            HomeScreen(
                viewModel = homeViewModel,
                onCreateProject = { navController.navigate(Screen.CreateProject.route) },
                onOpenProject = { project -> navController.navigate(Screen.Editor.createRoute(project.id)) },
                onOpenSettings = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(Screen.CreateProject.route) {
            val createViewModel: CreateProjectViewModel = viewModel()
            CreateProjectScreen(
                viewModel = createViewModel,
                onNavigateBack = { navController.popBackStack() },
                onProjectCreated = { project ->
                    navController.navigate(Screen.Editor.createRoute(project.id)) {
                        popUpTo(Screen.Home.route)
                    }
                }
            )
        }

        composable(
            route = Screen.Editor.route,
            arguments = listOf(navArgument("projectId") { type = NavType.StringType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
            val editorViewModel: EditorViewModel = viewModel()
            EditorScreen(
                viewModel = editorViewModel,
                projectId = projectId,
                onNavigateBack = { navController.popBackStack() },
                onOpenSettings = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEditorSettings = { navController.navigate(Screen.EditorSettings.route) },
                onNavigateToBuildRunSettings = { navController.navigate(Screen.BuildRunSettings.route) },
                onNavigateToBuildAiSettings = { navController.navigate(Screen.BuildAiSettings.route) },
                onNavigateToAbout = { navController.navigate(Screen.About.route) }
            )
        }

        composable(Screen.EditorSettings.route) {
            EditorSettingsScreen(
                prefRepository = prefRepository,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.BuildRunSettings.route) {
            BuildRunSettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.BuildAiSettings.route) {
            val aiViewModel: BuildAiViewModel = viewModel()
            BuildAiScreen(
                viewModel = aiViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.About.route) {
            AboutScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
