# SolarSnap Build Requirements

## Java Version Requirement

This project requires **Java 21** (JDK 21) to build successfully.

### Current Issue
The FLIR SDK libraries (`androidsdk-release.aar` and `thermalsdk-release.aar`) are compiled with Java 21 (class file version 65.0). The current system has Java 17 installed, which cannot load these libraries.

### Error Message
```
bad class file: class file has wrong version 65.0, should be 61.0
```

### Solution
Install JDK 21 from one of these sources:
- [Oracle JDK 21](https://www.oracle.com/java/technologies/downloads/#java21)
- [OpenJDK 21](https://adoptium.net/teapot/)
- [Amazon Corretto 21](https://aws.amazon.com/corretto/)

After installation, set `JAVA_HOME` environment variable to point to JDK 21.

## Build Command
Once Java 21 is installed:
```bash
.\gradlew.bat assembleDebug
```

## Project Status

### Completed Features
1. ✅ Login Screen
2. ✅ Site Selection Dashboard (with ACE design compliance)
3. ✅ Thermal Inspection/Capture Screen
4. ✅ Site Map / Fault Map Screen
5. ✅ Inspection History Screen
6. ✅ Uploads / Sync Page
7. ✅ Reports / Analytics Page
8. ✅ Settings / Configuration Page

### Integration Status
- All activities are registered in AndroidManifest.xml
- Navigation between screens is implemented
- Mock data is in place for testing
- ACE design principles applied throughout (high contrast, large buttons, minimal UI)

### Next Steps
1. Install Java 21
2. Build the project
3. Test on ACE camera device
4. Integrate real FLIR SDK functionality
5. Add backend API integration
6. Implement actual barcode scanning
7. Add GPS location tracking
