package com.idestudio.app.data.model

import java.io.File
import org.json.JSONObject

data class Project(
    val id: String,
    val name: String,
    val appName: String,
    val packageName: String,
    val mainActivityName: String,
    val rootPath: String,
    val minSdk: Int = 21,
    val targetSdk: Int = 34,
    val compileSdk: Int = 34,
    val templateId: String = "simple_app",
    val lastModified: Long = System.currentTimeMillis(),
    val createdTime: Long = System.currentTimeMillis()
) {
    val rootDir: File get() = File(rootPath)
    val appDir: File get() = File(rootDir, "app")
    val srcDir: File get() = File(appDir, "src/main")
    val javaDir: File get() = File(srcDir, "java")
    val resDir: File get() = File(srcDir, "res")
    val manifestFile: File get() = File(srcDir, "AndroidManifest.xml")
    val buildDir: File get() = File(appDir, "build")
    val outputApk: File get() = File(buildDir, "bin/$name-debug.apk")

    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("name", name)
            put("appName", appName)
            put("packageName", packageName)
            put("mainActivityName", mainActivityName)
            put("rootPath", rootPath)
            put("minSdk", minSdk)
            put("targetSdk", targetSdk)
            put("compileSdk", compileSdk)
            put("templateId", templateId)
            put("lastModified", lastModified)
            put("createdTime", createdTime)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): Project {
            return Project(
                id = json.getString("id"),
                name = json.getString("name"),
                appName = json.optString("appName", json.getString("name")),
                packageName = json.getString("packageName"),
                mainActivityName = json.optString("mainActivityName", "MainActivity"),
                rootPath = json.getString("rootPath"),
                minSdk = json.optInt("minSdk", 21),
                targetSdk = json.optInt("targetSdk", 34),
                compileSdk = json.optInt("compileSdk", 34),
                templateId = json.optString("templateId", "simple_app"),
                lastModified = json.optLong("lastModified", System.currentTimeMillis()),
                createdTime = json.optLong("createdTime", System.currentTimeMillis())
            )
        }
    }
}
