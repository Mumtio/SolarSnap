# SolarSnap Android App

A professional Android application for solar panel thermal inspection using FLIR ACE thermal cameras. Features real-time thermal imaging, GPS tracking, offline operation, and comprehensive inspection management for solar farm maintenance teams.

## 🎯 Overview

SolarSnap Android App is the mobile frontend for the SolarSnap inspection system, designed specifically for field technicians conducting thermal inspections of solar panels. The app integrates with FLIR ACE thermal cameras to provide real-time thermal imaging, automatic fault detection, and comprehensive inspection documentation.

## ✨ Key Features

### 🔥 Thermal Inspection
- **Real-time thermal imaging** with FLIR ACE camera integration
- **Live temperature measurement** with hotspot detection
- **Dual-view display** (thermal + visual) for comprehensive analysis
- **Automatic temperature delta calculation** for fault severity assessment
- **One-touch thermal image capture** with GPS coordinates

### 📱 Mobile-Optimized UI
- **ACE-compliant design** optimized for 4" field devices
- **High contrast interface** for outdoor visibility
- **Glove-friendly controls** with large touch targets
- **Physical button navigation** support (Up/Down/Center/Back)
- **Minimal text input** with filter-based interactions

### 🗺️ Site Management
- **Interactive site mapping** with up to 1,200+ panel visualization
- **Panel-by-panel inspection workflow** with progress tracking
- **GPS-based location tracking** for accurate fault positioning
- **Barcode scanning** for panel ID verification
- **Real-time inspection status** updates

### 📊 Data Management
- **Offline-first operation** for remote locations
- **Automatic cloud sync** when connectivity is available
- **Comprehensive inspection history** with search and filtering
- **Image compression and optimization** for efficient storage
- **Progress tracking** with completion statistics

### 🔐 Enterprise Security
- **Secure authentication** with company-level access control
- **JWT token management** with automatic refresh
- **Role-based permissions** (Inspector, Manager, Admin)
- **Encrypted data storage** for sensitive information

## 🚀 Quick Start

### Prerequisites

**Required:**
- **Java 21 (JDK 21)** - Essential for FLIR SDK compilation
- **Android SDK 33+** (Target SDK 36)
- **FLIR ACE Thermal Camera** - For thermal imaging functionality

**Optional:**
- **Android Studio** - For development and debugging
- **Physical Android device** - Recommended for testing thermal features

### Installation Steps

1. **Install Java 21**
   ```bash
   # Download from one of these sources:
   # - Oracle JDK 21: https://www.oracle.com/java/technologies/downloads/#java21
   # - OpenJDK 21: https://adoptium.net/
   # - Amazon Corretto 21: https://aws.amazon.com/corretto/
   
   # Verify installation
   java -version
   # Should output: java version "21.x.x"
   ```

2. **Set JAVA_HOME Environment Variable**
   ```bash
   # Windows
   setx JAVA_HOME "C:\Program Files\Java\jdk-21"
   setx PATH "%PATH%;%JAVA_HOME%\bin"
   
   # Linux/Mac
   export JAVA_HOME=/path/to/jdk-21
   export PATH=$JAVA_HOME/bin:$PATH
   
   # Add to ~/.bashrc or ~/.zshrc for persistence
   ```

3. **Clone and Build Project**
   ```bash
   cd Frontend
   
   # Make gradlew executable (Linux/Mac)
   chmod +x gradlew
   
   # Build debug APK
   ./gradlew assembleDebug
   
   # Windows
   gradlew.bat assembleDebug
   ```

4. **Install on Device**
   ```bash
   # Install via ADB
   ./gradlew installDebug
   
   # Or manually install APK
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

### First Run Setup

1. **Launch SolarSnap** on your Android device
2. **Login** with test credentials:
   - Email: `inspector1@solartech.com`
   - Password: `password123`
   - Company ID: `SOLARTECH-001`
3. **Grant permissions** for camera, location, and storage
4. **Connect FLIR ACE camera** (if available)
5. **Start inspection** workflow

## 📱 App Architecture

### Screen Flow
```
Login → Site Selection → Thermal Inspection → [Site Map | History | Uploads | Reports]
                    ↓
              Panel Selection → Thermal Capture → Data Entry → Save/Sync
```

### Core Components

**Activities:**
- `LoginActivity` - Authentication and company selection
- `SiteSelectionActivity` - Dashboard with site overview and progress
- `MainActivity` - Primary thermal inspection interface
- `SiteMapActivity` - Interactive site mapping and fault visualization
- `InspectionHistoryActivity` - Historical data browsing and search
- `UploadsActivity` - Sync management and upload queue
- `ReportsActivity` - Analytics dashboard and export tools

**Models:**
- `PanelInspection` - Inspection data structure
- `SolarSite` - Site configuration and metadata
- `UploadProgressService` - Background sync management

**Services:**
- `ThermalCameraService` - FLIR SDK integration
- `LocationService` - GPS tracking and coordinates
- `SyncService` - Background data synchronization

## 🎨 Design Principles

### ACE Platform Optimization

The app follows FLIR ACE design guidelines for rugged field use:

**Visual Design:**
- **Dark theme** with high contrast for outdoor visibility
- **Large touch targets** (48-80dp) for glove operation
- **Minimal UI elements** to maximize thermal view area
- **Status indicators** for quick system health assessment

**Interaction Design:**
- **Physical button support** for navigation without touch
- **Gesture-based controls** for common actions
- **Voice feedback** for hands-free operation
- **Haptic feedback** for confirmation actions

**Color Coding:**
- 🟢 **Green (#4CAF50)** - Healthy panels, successful operations
- 🟠 **Orange (#FF9800)** - Warning conditions, attention needed
- 🔴 **Red (#F44336)** - Critical faults, immediate action required
- ⚪ **Gray (#757575)** - Not inspected, neutral states
- 🔵 **Blue (#2196F3)** - Information, navigation elements

## 🌐 Backend Integration

The app is configured to connect to the production SolarSnap Backend API:
- **Production API**: `https://solarsnap-backend.onrender.com/api/v1/`
- **Health Check**: `https://solarsnap-backend.onrender.com/health`
- **Authentication**: JWT-based with automatic token refresh
- **Network Security**: HTTPS enforced for production, HTTP allowed for local development

### Test Credentials
```
Email: inspector1@solartech.com
Password: password123
Company ID: SOLARTECH-001
```

### App Configuration

**API Endpoints** (`app/src/main/java/com/solarsnap/app/network/ApiClient.java`):
```java
public class ApiClient {
    // Production backend URL
    private static final String BASE_URL = "https://solarsnap-backend.onrender.com/api/v1/";
    
    // Alternative URLs for different environments
    // For local development: "http://10.0.2.2:5000/api/v1/" (Android emulator)
    // For physical device: "http://YOUR_COMPUTER_IP:5000/api/v1/"
}
```

**FLIR SDK Configuration** (`build.gradle.kts`):
```kotlin
android {
    compileSdk 36
    
    defaultConfig {
        minSdk 33
        targetSdk 36
        // FLIR SDK requires specific configurations
    }
    
    // FLIR SDK dependencies
    repositories {
        flatDir { dirs("../atlas-java-sdk-android-2.17.0") }
    }
}
```

### Permissions (`AndroidManifest.xml`)

```xml
<!-- Camera and thermal imaging -->
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />

<!-- Location services -->
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />

<!-- Storage and file management -->
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />

<!-- Network connectivity -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

<!-- Device features -->
<uses-permission android:name="android.permission.VIBRATE" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
```

## 🔥 FLIR SDK Integration

### Thermal Camera Setup

The app integrates with FLIR Atlas SDK 2.17.0 for thermal imaging:

**Camera Initialization:**
```java
// Initialize FLIR camera connection
private void initializeThermalCamera() {
    try {
        // Setup camera discovery
        DiscoveryFactory.getInstance().scan(
            DiscoveryEventListener.cameraFound(this::onCameraFound),
            DiscoveryEventListener.discoveryError(this::onDiscoveryError)
        );
    } catch (Exception e) {
        Log.e(TAG, "Failed to initialize thermal camera", e);
    }
}
```

**Thermal Image Capture:**
```java
// Capture thermal image with metadata
private void captureThermalImage() {
    if (camera != null && camera.isConnected()) {
        camera.capture(new CaptureCallback() {
            @Override
            public void onImageCaptured(ThermalImage image) {
                // Process thermal image
                processThermalImage(image);
            }
        });
    }
}
```

### Emulator Support

For development without physical FLIR hardware:

```java
// Enable thermal emulation mode
private void setupThermalEmulator() {
    // Copy CSQ file for emulation
    setupEmulatorCSQFile();
    
    // Configure emulator settings
    EmulatorSettings settings = new EmulatorSettings();
    settings.setEmulationFile("ace_emulator_04.csq");
    
    // Initialize emulated camera
    camera = CameraFactory.createEmulatedCamera(settings);
}
```

## 📊 Data Management

### Local Storage

**SQLite Database Schema:**
```sql
-- Inspections table
CREATE TABLE inspections (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    inspection_uuid TEXT UNIQUE,
    site_id TEXT,
    panel_id TEXT,
    temperature REAL,
    delta_temp REAL,
    severity TEXT,
    issue_type TEXT,
    latitude REAL,
    longitude REAL,
    thermal_image_path TEXT,
    visual_image_path TEXT,
    metadata TEXT,
    timestamp INTEGER,
    sync_status TEXT DEFAULT 'pending'
);

-- Upload queue table
CREATE TABLE upload_queue (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    inspection_id INTEGER,
    file_path TEXT,
    file_size INTEGER,
    upload_status TEXT DEFAULT 'pending',
    retry_count INTEGER DEFAULT 0,
    created_at INTEGER
);
```

### Cloud Synchronization

**Background Sync Service:**
```java
public class SyncService extends IntentService {
    @Override
    protected void onHandleIntent(Intent intent) {
        // Sync pending inspections
        syncPendingInspections();
        
        // Upload queued files
        uploadQueuedFiles();
        
        // Download site updates
        downloadSiteUpdates();
    }
}
```

**Offline-First Architecture:**
- All data stored locally first
- Background sync when network available
- Conflict resolution for concurrent edits
- Progress tracking for large uploads

## 🧪 Testing

### Unit Testing

```bash
# Run unit tests
./gradlew test

# Run with coverage
./gradlew testDebugUnitTestCoverage
```

### Integration Testing

```bash
# Run instrumented tests on device
./gradlew connectedAndroidTest

# Run specific test class
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.solarsnap.app.ThermalCameraTest
```

### Manual Testing Checklist

**Authentication:**
- [ ] Login with valid credentials
- [ ] Handle invalid credentials gracefully
- [ ] Token refresh on expiration
- [ ] Logout and clear session

**Thermal Inspection:**
- [ ] Camera connection and initialization
- [ ] Live thermal feed display
- [ ] Temperature measurement accuracy
- [ ] Image capture and storage
- [ ] GPS coordinate recording

**Data Sync:**
- [ ] Offline operation capability
- [ ] Background sync when online
- [ ] Upload progress tracking
- [ ] Conflict resolution

**UI/UX:**
- [ ] Responsive design on target devices
- [ ] High contrast visibility in sunlight
- [ ] Touch target accessibility
- [ ] Physical button navigation

## 🚀 Deployment

### Debug Build
```bash
# Generate debug APK
./gradlew assembleDebug

# APK location: app/build/outputs/apk/debug/app-debug.apk
```

### Release Build
```bash
# Generate signed release APK
./gradlew assembleRelease

# Requires keystore configuration in build.gradle
```

### Distribution Options

**Internal Distribution:**
- Direct APK installation via ADB
- Internal app store (Firebase App Distribution)
- Enterprise MDM deployment

**Play Store Distribution:**
- Google Play Console upload
- Internal testing tracks
- Staged rollout deployment

## 📁 Project Structure

```
Frontend/
├── app/
│   ├── src/main/
│   │   ├── java/com/solarsnap/app/
│   │   │   ├── LoginActivity.java              # Authentication screen
│   │   │   ├── SiteSelectionActivity.java      # Dashboard and site selection
│   │   │   ├── MainActivity.java                # Primary thermal inspection
│   │   │   ├── SiteMapActivity.java            # Interactive site mapping
│   │   │   ├── InspectionHistoryActivity.java  # Historical data browser
│   │   │   ├── UploadsActivity.java            # Sync management
│   │   │   ├── ReportsActivity.java            # Analytics and reporting
│   │   │   ├── models/
│   │   │   │   ├── PanelInspection.java        # Inspection data model
│   │   │   │   ├── SolarSite.java              # Site configuration model
│   │   │   │   └── UploadProgressService.java  # Upload management
│   │   │   ├── services/
│   │   │   │   ├── ThermalCameraService.java   # FLIR SDK integration
│   │   │   │   ├── LocationService.java        # GPS tracking
│   │   │   │   └── SyncService.java            # Background synchronization
│   │   │   └── utils/
│   │   │       ├── ApiClient.java              # HTTP client
│   │   │       ├── DatabaseHelper.java         # SQLite management
│   │   │       └── FileUtils.java              # File operations
│   │   ├── res/
│   │   │   ├── layout/                         # XML layout files
│   │   │   ├── values/                         # Colors, strings, styles
│   │   │   ├── drawable/                       # Icons and graphics
│   │   │   └── menu/                           # Menu definitions
│   │   └── AndroidManifest.xml                 # App configuration
│   ├── build.gradle.kts                        # Module build configuration
│   └── proguard-rules.pro                      # Code obfuscation rules
├── gradle/                                     # Gradle wrapper files
├── build.gradle.kts                            # Project build configuration
├── settings.gradle.kts                         # Project settings
├── gradle.properties                           # Build properties
├── local.properties                            # Local SDK paths
└── README.md                                   # This file
```

## 🔍 Troubleshooting

### Common Build Issues

**Java Version Error:**
```
Error: class file has wrong version 65.0, should be 61.0
```
**Solution:** Install Java 21 and set JAVA_HOME correctly

**FLIR SDK Not Found:**
```
Error: Could not find atlas-java-sdk-android-2.17.0
```
**Solution:** Ensure FLIR SDK is in the correct directory and flatDir is configured

**Gradle Sync Failed:**
```
Error: Could not resolve dependencies
```
**Solution:** Check internet connection and Gradle cache (`./gradlew --refresh-dependencies`)

### Runtime Issues

**Camera Connection Failed:**
```
ThermalLog: Failed to connect to ACE camera
```
**Solutions:**
- Verify FLIR ACE camera is connected via USB
- Check USB debugging permissions
- Try thermal emulator mode for development

**Location Permission Denied:**
```
SecurityException: Location permission not granted
```
**Solution:** Grant location permissions in device settings or app permissions

**Network Sync Failed:**
```
IOException: Unable to sync with server
```
**Solutions:**
- Check network connectivity
- Verify API endpoint configuration
- Check authentication token validity

### Performance Optimization

**Memory Usage:**
- Monitor thermal image memory allocation
- Implement image compression for large files
- Use background threads for heavy operations

**Battery Optimization:**
- Minimize GPS polling frequency
- Optimize thermal camera usage
- Implement efficient sync scheduling

**Storage Management:**
- Implement automatic cleanup of old files
- Compress images before storage
- Monitor available storage space

## 📈 Development Roadmap

### Current Status (v1.0)
- ✅ Complete UI implementation (7 screens)
- ✅ FLIR SDK integration structure
- ✅ Authentication and security
- ✅ Local data storage
- ✅ Mock data and testing framework

### Upcoming Features (v1.1)
- 🔄 Real FLIR camera integration
- 🔄 Backend API connectivity
- 🔄 GPS location tracking
- 🔄 Barcode scanning implementation
- 🔄 Background sync optimization

### Future Enhancements (v2.0)
- 📋 Advanced thermal analysis algorithms
- 📋 Machine learning fault detection
- 📋 Augmented reality overlay
- 📋 Voice command integration
- 📋 Multi-language support

## 📄 License

Proprietary - FLIR App Challenge 2025

FLIR SDK usage requires appropriate licensing from FLIR Systems. Contact FLIR for commercial licensing terms.

---

**Platform**: Android (Min SDK 33, Target SDK 36)  
**Status**: Production Ready Beta  
**Last Updated**: March 2026
