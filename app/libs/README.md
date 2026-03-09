# FLIR SDK Libraries

This directory should contain the FLIR SDK library files required to build the SolarSnap application:

- `androidsdk-release.aar` (514 KB)
- `thermalsdk-release.aar` (101.5 MB)

## Why are these files not in the repository?

The `thermalsdk-release.aar` file exceeds GitHub's 100 MB file size limit and has been excluded from version control via `.gitignore`.

## How to obtain these files

1. Download the FLIR SDK from the official FLIR developer portal
2. Place the `.aar` files in this directory (`app/libs/`)
3. Build the project using `.\gradlew.bat assembleDebug`

## Build Requirements

- Java 21 (JDK 21) is required to build this project
- See `BUILD_REQUIREMENTS.md` in the root directory for complete build instructions

## Note for Developers

If you're setting up this project for the first time:
1. Obtain the FLIR SDK files from your team or the FLIR developer portal
2. Copy them to this directory
3. The build system will automatically include them via the `build.gradle.kts` configuration
