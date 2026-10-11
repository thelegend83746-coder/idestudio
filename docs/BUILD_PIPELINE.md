# IDE STUDIO — On-Device Build Pipeline Specification

## 1. Pipeline Overview
IDE STUDIO executes a real 10-stage on-device compilation and packaging pipeline without relying on remote servers.

```
+---------------------------------------------------------------------------------------------+
|                                    10-STAGE BUILD PIPELINE                                  |
+---------------------------------------------------------------------------------------------+
 1. PREPARATION        --> Validate project directory and create build directories
 2. SDK DISCOVERY      --> Locate android.jar for targeted API level (or flag missing status)
 3. R.JAVA GENERATION  --> Parse res/ and extract ids, layouts, strings into valid R.java
 4. RESOURCE PROCESSING--> AAPT2 compile/link or standalone resource archive packaging
 5. JAVA COMPILATION   --> ECJ (Eclipse Compiler for Java) compiles .java sources to .class
 6. BYTECODE TO DEX    --> D8 converts Java 8 bytecode (.class) into classes.dex
 7. APK PACKAGING      --> Merge classes.dex, AndroidManifest.xml, res/, and assets/
 8. ZIP ALIGNMENT      --> Align uncompressed entries to 4-byte boundaries (zipalign)
 9. CRYPTOGRAPHIC SIGN --> Sign APK with RSA-2048 debug certificate (v1 signature block)
10. VERIFICATION       --> Verify DEX header magic, manifest, and signature blocks
```

## 2. Stage-by-Stage Breakdown

### Stage 1: Preparation & Directory Setup
- Validates the project directory structure.
- Prepares:
  - `build/generated/source/r/`
  - `build/intermediates/classes/`
  - `build/intermediates/dex/`
  - `build/intermediates/res/`
  - `build/outputs/`

### Stage 2: SDK Discovery (`SdkPlatformManager`)
- Checks candidate SDK paths for `android.jar`:
  - `/storage/emulated/0/test-folder/android-sdk/platforms/android-<api>/android.jar`
  - `/system/framework/framework.jar` (fallback)
- Honest reporting: If `android.jar` is missing, records exact missing path and halts build with actionable instructions.

### Stage 3: `R.java` Generation (`RJavaGenerator`)
- Extracts IDs from layout files (`@+id/btnClickMe` -> `R.id.btnClickMe = 0x7f010001;`).
- Extracts strings, colors, styles, drawables.
- Writes generated `R.java` to `build/generated/source/r/<package>/R.java` so that Java code resolves resources during ECJ compilation.

### Stage 4: Resource Processing (`ResourceCompilerBridge`)
- Validates XML syntax using streaming `XmlPullParser`.
- Uses native `aapt2` binary when present in toolchain; otherwise employs standalone Android resource packaging.

### Stage 5: Java Compilation (`JavaCompilerBridge`)
- Invokes the Eclipse Compiler for Java (`org.eclipse.jdt.internal.compiler.batch.Main`).
- Parameters: `-1.8 -encoding UTF-8 -d <classesDir> -classpath <android.jar> <sources...>`.
- Captures compiler stdout and stderr; parses error diagnostics (`file:line: error: msg`) to enable clickable error navigation in the Build Log Output panel.

### Stage 6: Bytecode to DEX (`DexConverterBridge`)
- Invokes D8 (`com.android.tools.r8.D8`) with `--min-api <api> --output <dexDir> <classes...>`.
- Outputs standard `classes.dex`.

### Stage 7: APK Packaging (`ApkPackager`)
- Assembles an unaligned `.apk` file combining `classes.dex`, `AndroidManifest.xml`, `res/`, and `assets/`.
- Uses `DEFLATED` compression for code/text and `STORED` (uncompressed) for binary assets.

### Stage 8: Zip Alignment (`ZipAligner`)
- Inspects stored ZIP entries and aligns data payload start offsets to 4-byte boundaries.

### Stage 9: Cryptographic Signing (`ApkSigner`)
- Generates or loads standard debug keystore (`androiddebugkey`, RSA 2048-bit).
- Produces:
  - `META-INF/MANIFEST.MF` (SHA-256 digests)
  - `META-INF/CERT.SF`
  - `META-INF/CERT.RSA` (SHA256withRSA signature block)

### Stage 10: Artifact Verification (`ApkVerifier`)
- Opens final APK archive:
  - Verifies `classes.dex` exists with valid DEX header magic bytes (`dex\n035\0`).
  - Verifies `AndroidManifest.xml` presence.
  - Verifies `META-INF/` signature certificates.
  - Computes SHA-256 checksum and final APK file size.
- Reports build duration, output APK path, and presents "Install APK" and "Share APK" actions.
