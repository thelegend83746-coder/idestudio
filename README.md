# IDE STUDIO — On-Device Android IDE & APK Builder

**IDE STUDIO** is a production-quality, on-device Android IDE and APK builder built in Kotlin using Jetpack Compose and Material 3. Inspired by mobile development environments, IDE STUDIO allows developers to create, edit, compile, package, sign, and install real Android applications directly on an Android device without cloud dependencies.

All project code, files, and build artifacts reside strictly in:
`/storage/emulated/0/test-folder/IDE_Studio`

---

## Key Features

1. **Project Creation & Templates**:
   - **Simple App**: Clean single-screen activity with a greeting text and button listener.
   - **FAB App**: Floating Action Button with real touch response.
   - **Navigation Drawer**: Multi-section drawer layout.
   - **Fullscreen App**: Immersive fullscreen activity hiding system status and navigation bars.
   - Real metadata configuration (Application Name, Package Name, Main Activity, Min SDK, Target SDK, Compile SDK).
   - Strict syntax validation for package names and project identifiers.

2. **Hierarchical Project Explorer**:
   - Real file tree representation mirroring standard Android project structures (`app/src/main/java`, `res/layout`, `res/values`, `AndroidManifest.xml`).
   - Create files and folders.
   - Rename and delete files and folders.
   - File-type icons (Java, XML, Gradle, Images, Folders).
   - Real-time disk synchronization.

3. **Code Editor**:
   - Multi-tab file editing with horizontal scrolling, close buttons, and unsaved changes indicator (`•`).
   - Syntax highlighting for Java and XML.
   - Line numbers gutter.
   - Auto-indentation on new line.
   - Undo and redo stack support.
   - In-editor Find & Replace bar with match counting.
   - Smooth collapsing toolbar on vertical scroll/swipe (`NestedScrollConnection`).

4. **Real On-Device Build Pipeline**:
   - **Validation**: Project structure, manifest sanity, and SDK versions.
   - **Resource Compilation**: Real AAPT2 integration with embedded resource generator for `R.java` symbol mapping.
   - **Java Compilation**: In-process ECJ compiler / bytecode compiler with real line-by-line syntax and diagnostics parsing.
   - **DEX Generation**: D8 integration converting bytecode to Dalvik Executable `classes.dex`.
   - **APK Packaging**: Packages `classes.dex`, `res/`, `AndroidManifest.xml`, and assets into `unaligned.apk`.
   - **ZipAlign**: 4-byte boundary alignment.
   - **ApkSigner**: Cryptographic APK Signature Scheme v1/v2 using SHA-256 and RSA.
   - **Signature Verification**: Verifies `MANIFEST.MF`, `CERT.SF`, and `CERT.RSA`.
   - **Package Installer**: One-tap installation through Android's `PackageInstaller` via `FileProvider`.

5. **Toolchain Manager**:
   - Real-time detection and verification of build tools (AAPT2, ECJ, D8, ZipAlign, ApkSigner, android.jar).
   - Storage usage tracking.
   - One-tap validation check.

6. **AI Builder**:
   - Project-aware AI assistant.
   - Can inspect project files, propose code changes in code blocks, and apply changes directly to disk when confirmed.
   - Configurable for Gemini, OpenAI, or custom API endpoints.

7. **Project Import & Export**:
   - Export full project as a `.zip` archive.
   - Import `.zip` archives with path-traversal security verification.

---

## Directory Layout

```
/storage/emulated/0/test-folder/IDE_Studio/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── res/
│       │   │   ├── drawable/
│       │   │   ├── mipmap-anydpi-v26/
│       │   │   └── values/
│       │   │       ├── colors.xml
│       │   │       ├── strings.xml
│       │   │       └── themes.xml
│       │   └── java/com/idestudio/app/
│       │       ├── IdeStudioApp.kt
│       │       ├── MainActivity.kt
│       │       ├── data/
│       │       │   ├── model/
│       │       │   └── repository/
│       │       ├── toolchain/
│       │       │   ├── BuildPipeline.kt
│       │       │   ├── ToolchainManager.kt
│       │       │   ├── Aapt2Compiler.kt
│       │       │   ├── ResourceGenerator.kt
│       │       │   ├── JavaCompiler.kt
│       │       │   ├── DexGenerator.kt
│       │       │   ├── ApkPackager.kt
│       │       │   ├── ZipAligner.kt
│       │       │   └── ApkSigner.kt
│       │       ├── ui/
│       │       │   ├── home/
│       │       │   ├── template/
│       │       │   ├── editor/
│       │       │   ├── explorer/
│       │       │   ├── build/
│       │       │   ├── toolchain/
│       │       │   ├── ai/
│       │       │   ├── settings/
│       │       │   └── about/
│       │       └── util/
│       └── test/
│           └── java/com/idestudio/app/
```
