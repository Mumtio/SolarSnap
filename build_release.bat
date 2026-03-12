@echo off
echo ========================================
echo SolarSnap Production Release Build
echo ========================================
echo.

echo Cleaning previous builds...
call gradlew clean

echo.
echo Building production release APK...
call gradlew assembleRelease

echo.
echo ========================================
echo Build Complete!
echo ========================================

if exist "app\build\outputs\apk\release\app-arm64-v8a-release.apk" (
    echo ✅ SUCCESS: Release APK created
    echo.
    echo 📁 Location: app\build\outputs\apk\release\
    echo 📦 File: app-arm64-v8a-release.apk
    echo.
    echo 🔍 NEXT STEPS:
    echo 1. Test the release APK on device
    echo 2. Run VirusTotal scan to verify 0%% detection
    echo 3. Submit to FLIR for evaluation
    echo.
    echo ⚠️  IMPORTANT: Only submit RELEASE APK to FLIR
    echo    Debug APKs will trigger false positives!
) else (
    echo ❌ ERROR: Release APK not found
    echo Check build errors above
)

echo.
pause