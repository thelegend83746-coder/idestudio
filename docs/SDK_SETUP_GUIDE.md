# IDE STUDIO — Android SDK & Offline Toolchain Setup Guide

## 1. Overview
IDE STUDIO is designed for fully offline Android project editing and APK building once the necessary toolchain components are cached on your Android device.

All project files and SDK dependencies reside inside `/storage/emulated/0/test-folder`.

---

## 2. Setting Up Android SDK Platforms

IDE STUDIO discovers Android platform libraries (`android.jar`) in the following search directories:
1. `/storage/emulated/0/test-folder/android-sdk/platforms/android-<API_LEVEL>/android.jar`
2. `/storage/emulated/0/test-folder/sdk/android-<API_LEVEL>.jar`
3. `/system/framework/framework.jar` (On-device Android framework fallback)

### Example Directory Layout:
```
/storage/emulated/0/test-folder/android-sdk/
└── platforms/
    ├── android-21/
    │   └── android.jar
    ├── android-30/
    │   └── android.jar
    └── android-34/
        └── android.jar
```

To provide an SDK platform:
1. Copy or extract `android.jar` (e.g. from the official Android SDK or command-line tools) into `/storage/emulated/0/test-folder/android-sdk/platforms/android-34/`.
2. Open **Settings -> Build & Run Settings** in IDE STUDIO.
3. Verify that the platform status updates to **READY**.

---

## 3. Toolchain Binaries Setup

### AAPT2 (Android Asset Packaging Tool 2)
Place the Android ARM64 `aapt2` executable at:
`/storage/emulated/0/test-folder/bin/aapt2`
and ensure executable permissions (`chmod +x /storage/emulated/0/test-folder/bin/aapt2`).

When AAPT2 is absent, IDE STUDIO automatically switches to its standalone Android resource compiler, parsing XML layouts and generating valid `R.java` symbol tables so that compilation continues uninterrupted.

### D8 / DX (DEX Bytecode Converter)
D8 is included via standard Android toolchain libraries. If custom D8 JARs are preferred, place them in `/storage/emulated/0/test-folder/tools/d8.jar`.

---

## 4. Offline Build Workflow
Once `android.jar` is located in `/storage/emulated/0/test-folder/android-sdk/`:
1. Project creation works offline.
2. Code editing and syntax highlighting work offline.
3. ECJ compilation, D8 conversion, and APK signing run completely offline without requiring network access.
4. AI features (Build AI) are the only subsystem requiring network access to connect with the configured Ollama Cloud provider.
