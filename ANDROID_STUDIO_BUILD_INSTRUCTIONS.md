# Android Studio Release Build Instructions

## CRITICAL: Avoiding Previous Mistakes

Based on the previous FLIR submission issues, follow these steps exactly to ensure a clean production release:

### ⚠️ PREVIOUS ISSUE ANALYSIS
- **Problem**: Debug APK triggered false positives (Google & Ikarus antivirus)
- **Root Cause**: Debug certificates, unoptimized code, development metadata
- **Solution**: Build signed release APK with production optimizations

## 🎯 STEP-BY-STEP RELEASE BUILD

### 1. Pre-Build Verification
```
✓ Verify Java 21 is installed and set as project JDK
✓ Ensure FLIR SDK files are in correct location
✓ Check signing configuration is ready
✓ Confirm all dependencies are resolved
```

### 2. Open Project in Android Studio
1. Launch Android Studio
2. Open existing project: `Frontend/` folder
3. Wait for Gradle sync to complete
4. Verify no build errors in "Build" tab

### 3. Configure Release Signing (CRITICAL)
1. Go to `Build` → `Generate Signed Bundle / APK`
2. Select `APK` option
3. Create new keystore or use existing:
   - **Keystore path**: `Frontend/app/solarsnap-release.keystore`
   - **Keystore password**: [Use secure password]
   - **Key alias**: `solarsnap-release`
   - **Key password**: [Use secure password]
4. Save keystore information securely

### 4. Build Configuration Check
Verify `app/build.gradle.kts` has correct release settings:

```kotlin
buildTypes {
    release {
        isMinifyEnabled = true          // ✓ Code optimization
        isShrinkResources = true        // ✓ Resource optimization
        proguardFiles(...)              // ✓ Code obfuscation
        isDebuggable = false           // ✓ Remove debug info
        isJniDebuggable = false        // ✓ Remove JNI debug
        // signingConfig = signingConfigs.getByName("release")
    }
}
```

### 5. Execute Release Build
1. In Android Studio, select `Build` → `Select Build Variant`
2. Change from `debug` to `release`
3. Go to `Build` → `Generate Signed Bundle / APK`
4. Select `APK`, choose keystore, enter passwords
5. Select `release` build variant
6. Check both signature versions (V1 & V2)
7. Click `Finish`

### 6. Verify Release APK
**Location**: `Frontend/app/build/outputs/apk/release/app-arm64-v8a-release.apk`

**Verification Checklist**:
```
✓ File size: ~90-100MB (includes FLIR SDK)
✓ Filename contains "release" not "debug"
✓ APK is signed (check with: aapt dump badging app.apk)
✓ No debug certificates present
✓ Code is obfuscated (ProGuard applied)
```

### 7. Final Quality Checks

#### A. APK Analysis in Android Studio
1. Go to `Build` → `Analyze APK`
2. Select the release APK
3. Verify:
   - No debug symbols in native libraries
   - Classes are obfuscated (names like `a.b.c`)
   - Resources are optimized
   - No development certificates

#### B. Installation Test
1. Install on physical device: `adb install app-arm64-v8a-release.apk`
2. Launch app and verify functionality
3. Check app info shows release version
4. Test core features (login, camera, etc.)

#### C. Security Verification
1. Verify app is signed with release certificate
2. Check no debug flags are enabled
3. Confirm ProGuard obfuscation applied
4. Validate network security config

## 🚫 CRITICAL DON'Ts (Lessons from Previous Issues)

### ❌ NEVER Submit Debug Builds
- Debug APKs contain development certificates
- Unoptimized code triggers antivirus heuristics
- Debug metadata appears suspicious to scanners

### ❌ NEVER Skip Code Obfuscation
- Always enable ProGuard/R8 for release builds
- Obfuscated code reduces false positive triggers
- Smaller APK size and better security

### ❌ NEVER Use Debug Signing
- Always use production keystore for releases
- Debug certificates are flagged by security tools
- Release signing provides proper app identity

### ❌ NEVER Include Debug Symbols
- Remove all debugging information from release
- Native libraries should not contain debug symbols
- Reduces APK size and security surface

## ✅ SUCCESS INDICATORS

### Expected Release APK Characteristics:
- **Size**: 90-100MB (optimized with FLIR SDK)
- **Architecture**: ARM64-v8a only
- **Signing**: Production certificate (not debug)
- **Obfuscation**: ProGuard/R8 applied
- **Optimization**: Minified and shrunk resources
- **Debug Info**: Completely removed

### VirusTotal Expected Results:
- **Clean Scan**: 0/70+ detections expected
- **No False Positives**: Release builds rarely trigger heuristics
- **Major Vendors**: Microsoft, Kaspersky, Symantec should report "Clean"

## 📋 Pre-Submission Checklist

Before uploading to FLIR Box.com folder:

```
□ APK filename contains "release" not "debug"
□ APK size is appropriate (~90-100MB)
□ App installs and launches successfully
□ Core functionality tested (login, camera, capture)
□ No debug certificates present
□ ProGuard obfuscation verified
□ Release notes updated with current date
□ SBOM file reflects current dependencies
□ All three files ready: APK + SBOM + Release Notes
```

## 🔧 Troubleshooting Common Issues

### Build Fails - Java Version
**Error**: "Unsupported class file major version"
**Solution**: Ensure Java 21 is set in Android Studio (File → Project Structure → SDK Location)

### Signing Fails - Keystore Issues
**Error**: "Keystore was tampered with, or password was incorrect"
**Solution**: Recreate keystore or verify password accuracy

### APK Too Large
**Error**: APK exceeds expected size
**Solution**: Verify ABI splitting is enabled (ARM64-v8a only)

### ProGuard Errors
**Error**: "Warning: can't find referenced class"
**Solution**: Add appropriate keep rules in proguard-rules.pro

## 📞 Support Resources

- **FLIR SDK Documentation**: Check atlas-java-sdk-android-2.17.0/javadoc/
- **Android Studio Help**: Help → Android Studio Help
- **Gradle Build Issues**: View → Tool Windows → Build
- **APK Analysis**: Build → Analyze APK

---

**REMEMBER**: The goal is a production-quality, signed, optimized release APK that will pass VirusTotal scanning without false positives. Take time to verify each step rather than rushing the build process.