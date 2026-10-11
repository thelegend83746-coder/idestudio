# IDE STUDIO — Technical Architecture Document

## 1. System Overview
IDE STUDIO is a native Android Integrated Development Environment (IDE) and on-device APK builder written in Kotlin using Jetpack Compose and Material 3. It generates and edits Java + XML Android projects and compiles them directly on Android devices.

```
+-----------------------------------------------------------------------------------+
|                                  IDE STUDIO (UI)                                  |
|  HomeScreen  |  CreateProjectScreen  |  EditorScreen  |  BuildLog  |  Settings    |
+-----------------------------------------------------------------------------------+
                                          |
                                     (Flow / MVVM)
                                          v
+-----------------------------------------------------------------------------------+
|                                VIEWMODELS & REPOSITORIES                          |
|  HomeViewModel | CreateProjectViewModel | EditorViewModel | BuildAiViewModel       |
|  ProjectRepository                  | PreferencesRepository                       |
+-----------------------------------------------------------------------------------+
                                          |
                +-------------------------+-------------------------+
                |                                                   |
                v                                                   v
+-------------------------------+                   +-------------------------------+
|       BUILD ENGINE PIPELINE   |                   |       BUILD AI SUBSYSTEM      |
|  1. Project Validator         |                   |  OllamaClient (OkHttp)        |
|  2. SdkPlatformManager        |                   |  DiffEngine (line-by-line)    |
|  3. RJavaGenerator            |                   |  ProjectPatcher (Safe patch)  |
|  4. ResourceCompilerBridge    |                   |  Auto-backup (.bak) & rollback|
|  5. JavaCompilerBridge (ECJ)  |                   +-------------------------------+
|  6. DexConverterBridge (D8)   |
|  7. ApkPackager (ZIP)         |
|  8. ZipAligner (4-byte pad)   |
|  9. ApkSigner (RSA/v1 signer) |
| 10. ApkVerifier (DEX/Digest)  |
+-------------------------------+
```

## 2. Directory Structure
```
ide-studio/
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── res/
│       │   ├── values/ (strings.xml, colors.xml, themes.xml)
│       │   ├── drawable/ (ic_launcher_background.xml, ic_launcher_foreground.xml)
│       │   ├── xml/ (file_paths.xml, backup_rules.xml, data_extraction_rules.xml)
│       │   └── mipmap-anydpi-v26/ (ic_launcher.xml)
│       └── java/com/idestudio/
│           ├── IDEStudioApplication.kt
│           ├── MainActivity.kt
│           ├── ai/
│           │   ├── DiffEngine.kt
│           │   ├── OllamaClient.kt
│           │   └── ProjectPatcher.kt
│           ├── data/
│           │   ├── model/ (Project, FileNode, EditorSettings, BuildState, Template, etc.)
│           │   ├── repository/ (ProjectRepository, PreferencesRepository)
│           │   └── templates/ (TemplateManager)
│           ├── engine/
│           │   ├── ApkPackager.kt
│           │   ├── ApkSigner.kt
│           │   ├── ApkVerifier.kt
│           │   ├── BuildEngine.kt
│           │   ├── DexConverterBridge.kt
│           │   ├── JavaCompilerBridge.kt
│           │   ├── ResourceCompilerBridge.kt
│           │   ├── RJavaGenerator.kt
│           │   ├── SdkPlatformManager.kt
│           │   └── ZipAligner.kt
│           ├── ui/
│           │   ├── ai/ (BuildAiScreen, BuildAiViewModel, AiAssistantDialog)
│           │   ├── buildlog/ (BuildLogPanel)
│           │   ├── create/ (CreateProjectScreen, CreateProjectViewModel)
│           │   ├── editor/ (EditorScreen, EditorViewModel, CodeEditorSurface, SyntaxHighlighter, FileTabs)
│           │   ├── explorer/ (ProjectExplorerView, FileTreeItem)
│           │   ├── home/ (HomeScreen, HomeViewModel)
│           │   ├── navigation/ (Screen, NavGraph)
│           │   ├── settings/ (SettingsScreen, EditorSettingsScreen, BuildRunSettingsScreen, AboutScreen)
│           │   └── theme/ (Color, Theme, Type)
│           └── util/ (FileUtils, PathValidator)
├── docs/
│   ├── ARCHITECTURE.md
│   ├── BUILD_PIPELINE.md
│   ├── REFERENCE_COMPARISON_CHECKLIST.md
│   ├── SDK_SETUP_GUIDE.md
│   └── TEST_RESULTS.md
├── build.gradle.kts
├── settings.gradle.kts
└── gradle/
    ├── libs.versions.toml
    └── wrapper/gradle-wrapper.properties
```

## 3. Data & Threading Model
- **Non-blocking UI**: All compilation, DEX conversion, signing, file indexing, and network requests run strictly on `Dispatchers.IO` or `Dispatchers.Default` coroutines.
- **StateFlow & Compose**: UI surfaces observe immutable state flows (`uiState`, `buildState`, `editorSettings`) ensuring predictable lifecycle-aware state restoration during activity restarts or orientation changes.
- **Storage Isolation**: Projects reside strictly in `/storage/emulated/0/test-folder/projects`, verified against path traversal attacks.
