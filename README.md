# IDE Studio — Professional Android IDE

**IDE Studio** is a professional, mobile-native Android IDE designed for creating, editing, managing, compiling, and running **Java-based native Android applications directly on an Android device**.

---

## Key Highlights

- **100% Offline-First Core:** Project creation, filesystem operations, multi-tab code editing, Java syntax highlighting, formatting, and APK build run completely offline without remote servers.
- **Java Native Focus:** Built exclusively for native Java Android application development with standard Android directory structures and AndroidX integration.
- **Authoritative AI Approval Layer:** Optional online AI functionality powered by Ollama Cloud API. AI proposals (`create_file`, `update_file`, `create_folder`, `rename_folder`, `delete_file`, `delete_folder`) require explicit individual approval before touching disk. Zero silent code injection.
- **Design System:** Follows the approved mobile IDE visual truth with light gray backgrounds (`#F5F7FA`), white rounded cards (`18dp`), indigo primary actions (`#4361EE`), cyan accents (`#00B4D8`), and dark typography.

---

## Directory Structure

```text
/storage/emulated/0/test-folder/ide-studio/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   ├── java/com/idestudio/app/
│   │   │   ├── core/
│   │   │   │   ├── constants/ (AppConstants.java)
│   │   │   │   ├── storage/ (SecurePreferences.java)
│   │   │   │   └── utils/ (PackageValidator.java, PathSanitizer.java)
│   │   │   ├── data/
│   │   │   │   └── models/ (ProjectMeta.java, FileNode.java, SdkInfo.java, TemplateModel.java, ChatMessage.java)
│   │   │   ├── domain/
│   │   │   │   ├── project/ (LocalProjectStore.java, ProjectGenerator.java)
│   │   │   │   └── filesystem/ (FileTreeManager.java, SafeFileOperations.java)
│   │   │   ├── editor/
│   │   │   │   ├── engine/ (JavaSyntaxHighlighter.java, AutoIndentWatcher.java, JavaCodeFormatter.java)
│   │   │   │   ├── tabs/ (OpenFileDocument.java, EditorTabAdapter.java)
│   │   │   │   └── history/ (UndoRedoManager.java)
│   │   │   ├── builder/
│   │   │   │   ├── pipeline/ (BuildCoordinator.java, RJavaGenerator.java, ApkBuilder.java, BuildResult.java)
│   │   │   │   └── parser/ (BuildError.java, BuildLogParser.java)
│   │   │   ├── ai/
│   │   │   │   ├── client/ (OllamaCloudClient.java)
│   │   │   │   ├── context/ (ProjectContextBuilder.java)
│   │   │   │   ├── parser/ (AIProposalParser.java)
│   │   │   │   └── approval/ (AIProposal.java, ApprovalManager.java)
│   │   │   └── ui/
│   │   │       ├── home/ (HomeActivity.java, ProjectAdapter.java)
│   │   │       ├── create/ (ChooseTemplateActivity.java, ConfigureProjectActivity.java, SdkPickerDialog.java)
│   │   │       ├── editor/ (EditorActivity.java)
│   │   │       ├── explorer/ (ProjectExplorerManager.java, FileTreeAdapter.java)
│   │   │       ├── build/ (BuildOutputDialog.java)
│   │   │       ├── ai/ (AIChatActivity.java, ChatMessageAdapter.java)
│   │   │       └── settings/ (SettingsActivity.java)
│   │   └── res/
│   │       ├── layout/
│   │       ├── values/ (colors.xml, strings.xml, styles.xml)
│   │       └── drawable/ (icons and rounded shapes)
│   ├── src/test/ (Complete unit, integration, and security test suites)
│   └── build.gradle
├── build.gradle (root)
├── settings.gradle
├── projects/ (Default directory for created user projects)
├── README.md
└── RELEASE_NOTES.md
```

---

## User Workflows

### 1. Creating a Project
1. Open IDE Studio.
2. Tap the `+` Floating Action Button at the bottom-right.
3. Tap the **Create Project** pill directly above the button.
4. Select one of the 4 approved templates: **Empty Activity**, **Basic Activity**, **Jetpack**, or **Compose**.
5. Tap **Next**.
6. Enter an **App Name**. The package name automatically generates as `com.<appname>` with full manual override capability.
7. Select **Minimum SDK** and **Target SDK** via the rounded dialog.
8. Tap **Create**. The project files are generated immediately on disk, and `MainActivity.java` opens automatically in the Code Editor.

### 2. Developing & Editing Code
1. Tap the upper-left `☰` icon to open the **Files & Folders** Project Explorer drawer.
2. Swipe left on the drawer or tap any file to open it in a tab.
3. Long-press a folder to: **Rename**, **Create File**, **Create Folder**, or **Delete**.
4. Long-press a file to: **Rename** or **Delete**.
5. Real-time features in the editor:
   - Java syntax highlighting (keywords, types, strings, comments, numbers, annotations).
   - Synchronized line numbers gutter.
   - Block-aware `{}` auto-indentation on Enter.
   - Multi-tab support with dirty state tracking (`*`).
   - Undo and Redo buttons.
   - Save button persisting directly to disk.
   - Three-dot menu `⋮` for **Format Code**.

### 3. Offline Build & Run
1. Tap the **Play / Build APK** button in the editor toolbar.
2. The **Build Output** dialog opens:
   - Validates Java code and generates `R.java`.
   - Packages `classes.dex` and compiles debug APK.
   - Displays real-time dark terminal build logs.
3. On build success: Tap **Install / Run** to prompt the Android system package installer.
4. On build failure:
   - Error card clearly displays file name, line number, and error message.
   - Tap **Copy Error** to copy diagnostics.
   - Tap **Fix with Build AI** to open the error directly in the AI assistant.

### 4. Build AI & Authoritative Approval
1. Open Build AI from the editor three-dot menu `⋮` or from Build Output.
2. Chat with the AI regarding features, code updates, or build errors.
3. The AI proposes changes in interactive review cards.
4. **Authoritative Gate:**
   - Tap **Approve** to commit the change to the physical project files on disk.
   - Tap **Reject** to discard the change without modifying any files.
   - Zero code touches disk without explicit user approval.

---

## Android Permissions Required

When running the application APK on an Android device:
1. **Manage External Storage (All Files Access):** Required on Android 11+ (`Settings -> Apps -> Special app access -> All files access -> IDE Studio`).
2. **Internet:** Required only for optional Ollama Cloud API communication (all normal IDE functions remain completely offline).
