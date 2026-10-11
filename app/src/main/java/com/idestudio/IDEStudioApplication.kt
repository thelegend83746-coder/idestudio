package com.idestudio

import android.app.Application
import java.io.File

class IDEStudioApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Ensure projects and cache directories exist on startup
        val projectStorage = File("/storage/emulated/0/test-folder/projects")
        if (!projectStorage.exists()) {
            projectStorage.mkdirs()
        }

        val internalProjects = File(filesDir, "projects")
        if (!internalProjects.exists()) {
            internalProjects.mkdirs()
        }

        val securityDir = File(filesDir, "security")
        if (!securityDir.exists()) {
            securityDir.mkdirs()
        }
    }
}
