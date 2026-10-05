# IDE STUDIO — Toolchain Setup & Architecture Guide

## 1. Overview of On-Device Build Pipeline

Building an Android APK directly on an Android device without cloud servers requires coordinating 6 core toolchain stages:

1. **Resource Compilation & Linking (AAPT2 / Symbol Engine)**:
   - Compiles XML layouts, drawables, strings, colors, and styles.
   - Generates `R.java` with corresponding unique hexadecimal identifiers.
   - Generates compiled binary XML resources and `resources.arsc`.

2. **Java Compilation (ECJ / Bytecode Engine)**:
   - Compiles user Java source files (`MainActivity.java`, etc.) and the generated `R.java` against `android.jar`.
   - Produces standard Java `.class` bytecode files in `app/build/classes/`.
   - Real diagnostics parser extracts line numbers, column numbers, and syntax errors.

3. **DEX Generation (D8 / Dalvik Executable Engine)**:
   - Translates Java bytecode `.class` files into Android's Dalvik Executable format (`classes.dex`).
   - Supports native D8 binaries and pure Dalvik format generation.

4. **APK Packaging**:
   - Packages `classes.dex`, `AndroidManifest.xml`, resource tables (`resources.arsc`), `res/`, and `assets/` into an unaligned ZIP archive (`unaligned.apk`).

5. **ZipAlign**:
   - Aligns all uncompressed ZIP data entries to 4-byte boundaries according to Android APK specifications for zero-copy memory mapping (`mmap`).

6. **APK Signing (ApkSigner)**:
   - Signs the APK using RSA SHA-256 (APK Signature Scheme v1/v2).
   - Generates `META-INF/MANIFEST.MF`, `META-INF/CERT.SF`, and `META-INF/CERT.RSA`.
   - Verifies the signature integrity.

---

## 2. Using Custom or Bundled Toolchain Binaries

By default, IDE STUDIO includes built-in fallback engines so that compilation, DEX creation, packaging, and signing operate 100% out of the box without requiring manual setup.

To enhance compilation speed with native ELF binaries:
- Place Android `aapt2`, `d8`, or `zipalign` binaries inside the application files directory:
  `/data/data/com.idestudio.app/files/bin/`
- Or configure paths in **Settings > Toolchain Manager**.
- Ensure binaries have execution permissions (`chmod +x`).

---

## 3. Launching Built APKs

Once a build successfully finishes:
- Tap **Install APK** in the Build Log Output dialog.
- Android's Package Installer will prompt the user to install the debug APK.
- If Android prompts for "Install unknown apps" permission, enable it for IDE STUDIO.
