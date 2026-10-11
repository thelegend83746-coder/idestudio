# IDE STUDIO — Native Android IDE & On-Device APK Builder

IDE STUDIO is a full-featured, native Android development environment built with Kotlin and Jetpack Compose. It enables developers to create, edit, compile, and package installable Android APKs directly on an Android device without requiring an external computer or cloud connection.

---

## 🌟 Key Features

1. **Native Jetpack Compose UI**:
   - Modern Material 3 interface matching the BUILD STUDIO design specifications.
   - Smooth navigation, project list, creation wizard, and settings.

2. **Unified Template Engine**:
   - Offers multiple template options in the UI (Simple App, Fullscreen App, Navigation Drawer, etc.) while sharing a robust, unified Java + XML `EmptyProject` template architecture.
   - Generates clean Java source code, XML layouts, strings, colors, styles, and manifests.

3. **On-Device Build Engine**:
   - **R.java Generation**: Automatic symbol extraction from project XML resources.
   - **Resource Validation & AAPT2**: Architecture-specific AAPT2 binaries (`arm64-v8a`, `armeabi-v7a`, `x86_64`).
   - **Java Compilation (ECJ)**: In-process Eclipse Java Compiler (`ecj.jar`) compiling Java 8 compatible bytecode.
   - **DEX Bytecode Conversion (D8)**: Conversion of `.class` files into `classes.dex` using Android's D8 engine (`d8.jar`).
   - **APK Packaging & 4-byte Zip Alignment**: Creates standard Android APK archives.
   - **Cryptographic Signing (ApkSigner)**: Sign APKs on-device with RSA SHA-256 (v1 and v2 schemes).
   - **Verification**: Complete integrity check confirming `classes.dex`, manifest, signatures, and SHA-256 checksums.

4. **Multi-SDK Platform Support**:
   - Bundles Android Platform APIs 30, 31, 32, 33, 34, 35, and 36 inside `assets/platform/`.

5. **Code Editor & Explorer**:
   - Syntax highlighting for Java and XML.
   - Multi-tab file management.
   - Interactive project file tree explorer.

---

## 🛠️ Project Structure

```
ide-studio/
├── app/
│   ├── build.gradle.kts
│   ├── libs/                   # Core toolchain JARs (ecj, d8, apksig, android.jar)
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── assets/             # Platforms 30-36, EmptyProject template, fonts, keys
│       ├── java/com/idestudio/ # Kotlin & Compose source code
│       ├── jniLibs/            # Pre-compiled AAPT2 binaries for arm64-v8a, armeabi-v7a, x86_64
│       └── res/                # App resources, themes, and icons
├── gradle/
├── .github/workflows/          # CI workflow to build and release APK
├── build.gradle.kts
└── settings.gradle.kts
```

---

## 📱 Building the App

This repository includes a GitHub Actions workflow (`.github/workflows/build-apk.yml`) that automatically builds and publishes an installable APK on every push to `main`.
