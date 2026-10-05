package com.idestudio.app

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.idestudio.app.ui.about.AboutScreen
import com.idestudio.app.ui.ai.AiBuilderScreen
import com.idestudio.app.ui.ai.AiBuilderViewModel
import com.idestudio.app.ui.editor.EditorScreen
import com.idestudio.app.ui.editor.EditorViewModel
import com.idestudio.app.ui.home.HomeScreen
import com.idestudio.app.ui.home.HomeViewModel
import com.idestudio.app.ui.navigation.Screen
import com.idestudio.app.ui.settings.SettingsScreen
import com.idestudio.app.ui.splash.SplashScreen
import com.idestudio.app.ui.template.ConfigureProjectScreen
import com.idestudio.app.ui.template.TemplatePickerScreen
import com.idestudio.app.ui.theme.BackgroundLight
import com.idestudio.app.ui.theme.IDEStudioTheme
import com.idestudio.app.ui.toolchain.ToolchainScreen

class MainActivity : ComponentActivity() {

    private val homeViewModel: HomeViewModel by viewModels()
    private val editorViewModel: EditorViewModel by viewModels()
    private val aiBuilderViewModel: AiBuilderViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request storage permission if needed on Android 11+
        checkStoragePermissions()

        setContent {
            IDEStudioTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BackgroundLight
                ) {
                    AppNavigation(
                        homeViewModel = homeViewModel,
                        editorViewModel = editorViewModel,
                        aiBuilderViewModel = aiBuilderViewModel
                    )
                }
            }
        }
    }

    private fun checkStoragePermissions() {
        if (Build.VERSION.SDK_INT >= 30) {
            try {
                val isManagerMethod = Environment::class.java.getMethod("isExternalStorageManager")
                val isManager = isManagerMethod.invoke(null) as? Boolean ?: true
                if (!isManager) {
                    val intent = Intent("android.settings.MANAGE_APP_ALL_FILES_ACCESS_PERMISSION").apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                }
            } catch (ignored: Exception) {}
        }
    }
}

@Composable
fun AppNavigation(
    homeViewModel: HomeViewModel,
    editorViewModel: EditorViewModel,
    aiBuilderViewModel: AiBuilderViewModel
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onSplashFinished = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                viewModel = homeViewModel,
                onNavigateToTemplatePicker = { navController.navigate(Screen.TemplatePicker.route) },
                onNavigateToProject = { projectId ->
                    navController.navigate(Screen.Editor.createRoute(projectId))
                },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToToolchain = { navController.navigate(Screen.Toolchain.route) },
                onNavigateToAbout = { navController.navigate(Screen.About.route) },
                onNavigateToAiBuilder = { navController.navigate(Screen.AiBuilder.createRoute()) }
            )
        }

        composable(Screen.TemplatePicker.route) {
            TemplatePickerScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToConfigure = { templateId ->
                    navController.navigate(Screen.ConfigureProject.createRoute(templateId))
                }
            )
        }

        composable(
            route = Screen.ConfigureProject.route,
            arguments = listOf(navArgument("templateId") { type = NavType.StringType })
        ) { backStackEntry ->
            val templateId = backStackEntry.arguments?.getString("templateId") ?: "simple_app"
            ConfigureProjectScreen(
                templateId = templateId,
                onNavigateBack = { navController.popBackStack() },
                onProjectCreated = { projectId ->
                    navController.navigate(Screen.Editor.createRoute(projectId)) {
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
            EditorScreen(
                projectId = projectId,
                viewModel = editorViewModel,
                onNavigateBack = {
                    homeViewModel.refreshProjects()
                    navController.popBackStack()
                },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(Screen.Toolchain.route) {
            ToolchainScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToToolchain = { navController.navigate(Screen.Toolchain.route) },
                onNavigateToAbout = { navController.navigate(Screen.About.route) }
            )
        }

        composable(Screen.About.route) {
            AboutScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.AiBuilder.route,
            arguments = listOf(navArgument("projectId") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId")
            AiBuilderScreen(
                projectId = projectId,
                viewModel = aiBuilderViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
