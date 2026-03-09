# SolarSnap Project Structure

## Application Architecture

```
SolarSnap/
├── Login Flow
│   └── LoginActivity → SiteSelectionActivity → MainActivity
│
├── Core Features
│   ├── Thermal Imaging (FLIR ACE SDK)
│   ├── Asset ID Scanning
│   ├── GPS Tracking
│   └── Fault Classification
│
└── Data Models
    ├── PanelInspection
    └── SolarSite
```

## File Organization

### Java Source Files
```
app/src/main/java/com/flir/atlassdk/acecamerasample/
├── LoginActivity.java              # User authentication
├── SiteSelectionActivity.java      # Solar farm selection
├── MainActivity.java                # Main thermal inspection
└── models/
    ├── PanelInspection.java        # Inspection data model
    └── SolarSite.java              # Site information model
```

### Layout Files
```
app/src/main/res/layout/
├── activity_login.xml              # Login screen
├── activity_site_selection.xml     # Site list
└── activity_main_portrait.xml      # Thermal camera UI
```

### Resources
```
app/src/main/res/
├── values/
│   ├── strings.xml                 # App strings
│   ├── colors.xml                  # Color scheme
│   └── styles.xml                  # UI themes
└── mipmap-*/                       # App icons
```

## Key Components

### 1. MainActivity (Thermal Inspection)
- FLIR camera connection
- Thermal/visual streaming
- Temperature measurement
- Asset linking
- GPS tagging
- Severity classification

### 2. PanelInspection Model
```java
- assetId: String
- temperature: double
- deltaTemp: double
- severity: String (HEALTHY/WARNING/CRITICAL)
- issueType: String
- latitude/longitude: double
- timestamp: long
- thermalImagePath: String
- visualImagePath: String
- uploaded: boolean
```

### 3. Inspection Workflow
```
1. Login → Authenticate user
2. Site Selection → Choose solar farm
3. Start Camera → Connect FLIR ACE
4. Scan Asset → Detect panel ID
5. Capture → Take thermal + visual
6. Flag Issue → Classify severity
7. Save → Store with GPS metadata
```

## Severity Classification Logic

```java
if (deltaTemp > 15.0) {
    severity = "CRITICAL"
    issueType = "hotspot"
} else if (deltaTemp > 8.0) {
    severity = "WARNING"
    issueType = "elevated_temp"
} else {
    severity = "HEALTHY"
    issueType = "none"
}
```

## Dependencies

### Core
- FLIR ThermalSDK (thermalsdk-release.aar)
- FLIR AndroidSDK (androidsdk-release.aar)

### Android
- AndroidX AppCompat
- ConstraintLayout
- Material Components

### Location & ML
- Google Play Services Location
- ML Kit Barcode Scanning
- CameraX

## Build Configuration

- **Min SDK**: 33
- **Target SDK**: 36
- **Compile SDK**: 36
- **Java Version**: 21
- **ABI**: arm64-v8a

## Permissions

```xml
- CAMERA (thermal imaging)
- INTERNET (SDK communication)
- ACCESS_FINE_LOCATION (GPS)
- ACCESS_COARSE_LOCATION (GPS)
- WRITE_EXTERNAL_STORAGE (images)
- READ_EXTERNAL_STORAGE (images)
```

## Color Scheme

- **Primary**: #1E3A5F (Dark Blue)
- **Accent**: #00D9FF (Cyan)
- **Healthy**: #4CAF50 (Green)
- **Warning**: #FF9800 (Orange)
- **Critical**: #F44336 (Red)
- **Background**: #0A1628 (Dark)

## Implementation Status

### ✅ Completed
- Login screen
- Site selection
- Thermal camera integration
- Temperature measurement
- Asset ID simulation
- Severity classification
- GPS location tracking
- UI/UX design

### 🔄 Ready for Enhancement
- ML Kit barcode scanner (currently simulated)
- Backend API integration
- Offline data caching
- Map view
- Report generation
- Multi-user sync

## Next Steps

1. Integrate ML Kit for real barcode scanning
2. Connect to backend API for data sync
3. Implement offline mode with local database
4. Add map view with geospatial pins
5. Generate PDF/CSV reports
6. Add user management
