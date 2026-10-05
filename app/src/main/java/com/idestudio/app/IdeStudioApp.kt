package com.idestudio.app

import android.app.Application
import com.idestudio.app.data.repository.ProjectRepository
import com.idestudio.app.data.repository.SettingsRepository
import com.idestudio.app.toolchain.ToolchainManager
import java.io.File

class IdeStudioApp : Application() {

    lateinit var projectRepository: ProjectRepository
        private set
    lateinit var settingsRepository: SettingsRepository
        private set
    lateinit var toolchainManager: ToolchainManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        settingsRepository = SettingsRepository(this)
        
        // Base directory for user projects: inside /storage/emulated/0/test-folder/IDE_Studio_Projects
        // fallback to app external files dir if permission is restricted
        val baseProjectsDir = File("/storage/emulated/0/test-folder/IDE_Studio_Projects")
        if (!baseProjectsDir.exists()) {
            baseProjectsDir.mkdirs()
        }

        projectRepository = ProjectRepository(this, baseProjectsDir)
        toolchainManager = ToolchainManager(this)
    }

    companion object {
        lateinit var instance: IdeStudioApp
            private set
    }
}
