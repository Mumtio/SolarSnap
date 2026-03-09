# SolarSnap Backend Integration Guide
## Complete Technical Documentation

**Project**: SolarSnap - Solar Panel Thermal Inspection App  
**Repository**: https://github.com/Mumtio/AceCamera  
**Platform**: Android (Java)  
**Last Updated**: March 9, 2026

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [Application Overview](#application-overview)
3. [Architecture](#architecture)
4. [Data Models](#data-models)
5. [API Endpoints (23 Total)](#api-endpoints)
6. [Screen-by-Screen Integration](#screen-integration)
7. [Authentication & Security](#authentication)
8. [Offline-First Sync Strategy](#sync-strategy)
9. [File Upload System](#file-uploads)
10. [Error Handling](#error-handling)
11. [Implementation Guide](#implementation)
12. [Code Examples](#code-examples)
13. [Testing & Deployment](#testing)
14. [Quick Reference](#quick-reference)

---

## Executive Summary

SolarSnap is an Android application that enables field inspectors to perform thermal inspections of solar panels using FLIR ACE cameras. The app captures thermal images, automatically detects faults, and syncs data to the cloud.

### Key Features
- Thermal image capture with FLIR SDK
- Automatic fault detection (hotspots, diode failures, etc.)
- GPS location tracking
- Barcode/QR panel identification
- Offline-first architecture
- Background sync when online
- Multi-site management
- PDF/CSV report generation

### Integration Requirements
- **23 REST API endpoints**
- **JWT authentication** with refresh tokens
- **Image storage** (thermal + visual photos)
- **Real-time sync** capabilities
- **Report generation** backend

### Technology Stack
- **Frontend**: Android (Java), FLIR Thermal SDK
- **Database**: Room (SQLite)
- **Networking**: Retrofit + OkHttp
- **Location**: Google Play Services
- **Scanning**: ML Kit Barcode Scanner


---

## Application Overview

### User Workflow

```
1. Inspector logs in → Authenticates with backend
2. Selects solar site → Loads site data and panel layout
3. Scans panel barcode → Validates panel ID
4. Captures thermal image → FLIR camera captures thermal data
5. App analyzes temperature → Detects anomalies automatically
6. Inspector classifies fault → Hotspot, diode failure, etc.
7. Saves locally → Stores in SQLite database
8. Queues for upload → Adds to background sync queue
9. Syncs when online → Uploads to backend automatically
10. Generates reports → Creates PDF/CSV reports
```

### Screen Flow

```
LoginActivity (Authentication)
    ↓
SiteSelectionActivity (Dashboard)
    ├→ MainActivity (Thermal Inspection Camera)
    ├→ SiteMapActivity (Panel Grid Map)
    ├→ InspectionHistoryActivity (Past Inspections)
    ├→ UploadsActivity (Sync Status)
    ├→ ReportsActivity (Analytics & Reports)
    └→ SettingsActivity (Configuration)
```

### Data Flow Architecture

```
[FLIR Camera] → [Thermal Image]
       ↓
[Temperature Analysis] → [Fault Detection]
       ↓
[GPS Location] + [Panel ID] + [Thermal Data]
       ↓
[Local SQLite Database]
       ↓
[Upload Queue]
       ↓
[Background Sync Service]
       ↓
[Backend API] → [Cloud Storage]
```


---

## Architecture

### System Components

**Frontend (Android App)**
- 8 Activity screens
- FLIR Thermal SDK integration
- Room database for offline storage
- Background sync service
- Image compression & upload

**Backend (Your Responsibility)**
- REST API (23 endpoints)
- JWT authentication
- Image storage (S3/CDN)
- Database (inspections, sites, users)
- Report generation service

**Data Sync**
- Offline-first: App works without internet
- Background sync: Uploads when connected
- Conflict resolution: Server timestamp wins
- Retry logic: 3 attempts with exponential backoff

### Database Schema (Local - Room/SQLite)

```sql
-- Inspections table
CREATE TABLE inspections (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    inspection_id TEXT UNIQUE,
    site_id TEXT NOT NULL,
    panel_id TEXT NOT NULL,
    temperature REAL,
    delta_temp REAL,
    severity TEXT CHECK(severity IN ('HEALTHY','WARNING','CRITICAL')),
    issue_type TEXT,
    latitude REAL,
    longitude REAL,
    timestamp INTEGER NOT NULL,
    thermal_image_path TEXT,
    visual_image_path TEXT,
    uploaded INTEGER DEFAULT 0,
    upload_retry_count INTEGER DEFAULT 0,
    created_at INTEGER NOT NULL
);

-- Upload queue table
CREATE TABLE upload_queue (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    upload_id TEXT UNIQUE,
    inspection_id INTEGER REFERENCES inspections(id),
    status TEXT CHECK(status IN ('pending','uploading','uploaded','failed')),
    priority INTEGER DEFAULT 2,
    retry_count INTEGER DEFAULT 0,
    error_message TEXT,
    created_at INTEGER NOT NULL,
    last_attempt_at INTEGER
);

-- Sites cache table
CREATE TABLE sites (
    site_id TEXT PRIMARY KEY,
    site_name TEXT NOT NULL,
    total_panels INTEGER,
    latitude REAL,
    longitude REAL,
    last_synced INTEGER
);

-- Panels cache table
CREATE TABLE panels (
    panel_id TEXT PRIMARY KEY,
    site_id TEXT REFERENCES sites(site_id),
    row INTEGER,
    column INTEGER,
    string_number INTEGER,
    status TEXT,
    last_inspection INTEGER
);
```


---

## Data Models

### 1. PanelInspection

**Location**: `app/src/main/java/com/flir/atlassdk/acecamerasample/models/PanelInspection.java`

```java
public class PanelInspection {
    private String assetId;           // Panel ID (e.g., "PNL-A7-4402")
    private double temperature;       // Max temperature in Celsius
    private double deltaTemp;         // ΔT from baseline
    private String severity;          // "HEALTHY", "WARNING", "CRITICAL"
    private String issueType;         // "hotspot", "diode", "connection", "shading", "none"
    private double latitude;          // GPS coordinates
    private double longitude;
    private long timestamp;           // Unix timestamp (milliseconds)
    private String thermalImagePath;  // Local file path
    private String visualImagePath;   // Optional visual photo
    private boolean uploaded;         // Sync status
}
```

**JSON Example**:
```json
{
  "assetId": "PNL-A7-4402",
  "temperature": 58.7,
  "deltaTemp": 18.5,
  "severity": "CRITICAL",
  "issueType": "hotspot",
  "latitude": 36.1234,
  "longitude": -115.2345,
  "timestamp": 1691424120000,
  "thermalImagePath": "/storage/.../thermal_1691424120.jpg",
  "uploaded": false
}
```

### 2. SolarSite

**Location**: `app/src/main/java/com/flir/atlassdk/acecamerasample/models/SolarSite.java`

```java
public class SolarSite {
    private String siteId;           // Unique identifier
    private String siteName;         // Display name
    private int totalPanels;         // Total panel count
    private int inspectedPanels;     // Inspected today
    private double latitude;         // Site location
    private double longitude;
}
```

**JSON Example**:
```json
{
  "siteId": "NV-Solar-04",
  "siteName": "NV Solar Farm 04",
  "totalPanels": 1200,
  "inspectedPanels": 640,
  "latitude": 36.1234,
  "longitude": -115.2345
}
```

### 3. User/Inspector (Backend Model)

```json
{
  "userId": "inspector_12",
  "email": "inspector12@solartech.com",
  "fullName": "John Smith",
  "role": "Field Engineer",
  "companyId": "SOLARTECH-001",
  "assignedSites": ["NV-Solar-04", "NV-Solar-03"],
  "permissions": ["inspect", "upload", "generate_reports"]
}
```

### 4. UploadRecord (Backend Model)

```json
{
  "uploadId": "upload_1691424120_001",
  "inspectionId": "insp_1691424120",
  "panelId": "PNL-A7-4402",
  "status": "pending",
  "fileSize": 3.2,
  "retryCount": 0,
  "lastAttempt": 1691424120000,
  "errorMessage": null
}
```


---

## API Endpoints

**Base URL**: `https://api.solarsnap.com/v1`

### Authentication (3 endpoints)

#### POST /auth/login
Authenticate inspector and return JWT token.

**Request**:
```json
{
  "email": "inspector12@solartech.com",
  "password": "hashed_password",
  "companyId": "SOLARTECH-001"
}
```

**Response** (200 OK):
```json
{
  "success": true,
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "refresh_token_here",
  "expiresIn": 3600,
  "user": {
    "userId": "inspector_12",
    "email": "inspector12@solartech.com",
    "fullName": "John Smith",
    "role": "Field Engineer",
    "companyId": "SOLARTECH-001"
  }
}
```

#### POST /auth/refresh
Refresh expired access token.

**Request**:
```json
{
  "refreshToken": "refresh_token_here"
}
```

**Response** (200 OK):
```json
{
  "success": true,
  "token": "new_access_token",
  "expiresIn": 3600
}
```

#### POST /auth/logout
Invalidate current session.

**Headers**: `Authorization: Bearer {token}`

**Response** (200 OK):
```json
{
  "success": true,
  "message": "Logged out successfully"
}
```


### Site Management (3 endpoints)

#### GET /sites
Get list of sites assigned to inspector.

**Headers**: `Authorization: Bearer {token}`  
**Query Params**: `companyId` (optional), `status` (optional)

**Response** (200 OK):
```json
{
  "success": true,
  "sites": [
    {
      "siteId": "NV-Solar-04",
      "siteName": "NV Solar Farm 04",
      "totalPanels": 1200,
      "inspectedToday": 640,
      "latitude": 36.1234,
      "longitude": -115.2345,
      "status": "active",
      "lastInspection": 1691424120000
    }
  ]
}
```

#### GET /sites/{siteId}
Get detailed site information.

**Response** (200 OK):
```json
{
  "success": true,
  "site": {
    "siteId": "NV-Solar-04",
    "siteName": "NV Solar Farm 04",
    "totalPanels": 1200,
    "rows": 15,
    "panelsPerRow": 80,
    "latitude": 36.1234,
    "longitude": -115.2345,
    "statistics": {
      "criticalFaults": 4,
      "warnings": 11,
      "healthyPanels": 625,
      "uninspected": 560
    }
  }
}
```

#### GET /sites/{siteId}/panels
Get panel layout and status for map view.

**Response** (200 OK):
```json
{
  "success": true,
  "panels": [
    {
      "panelId": "PNL-A7-0001",
      "row": 1,
      "column": 1,
      "status": "HEALTHY",
      "lastInspection": 1691424120000,
      "deltaTemp": 2.1
    },
    {
      "panelId": "PNL-A7-0126",
      "row": 11,
      "column": 5,
      "status": "CRITICAL",
      "deltaTemp": 22.3,
      "issueType": "diode_failure"
    }
  ]
}
```


### Inspections (4 endpoints)

#### POST /inspections
Create new inspection record.

**Headers**: `Authorization: Bearer {token}`, `Content-Type: application/json`

**Request**:
```json
{
  "siteId": "NV-Solar-04",
  "panelId": "PNL-A7-4402",
  "inspectorId": "inspector_12",
  "temperature": 58.7,
  "deltaTemp": 18.5,
  "severity": "CRITICAL",
  "issueType": "hotspot",
  "latitude": 36.1234,
  "longitude": -115.2345,
  "timestamp": 1691424120000,
  "thermalImageId": "img_thermal_1691424120",
  "metadata": {
    "cameraModel": "FLIR ACE",
    "ambientTemp": 35.2
  }
}
```

**Response** (201 Created):
```json
{
  "success": true,
  "inspectionId": "insp_1691424120_001",
  "message": "Inspection recorded successfully"
}
```

#### GET /inspections
List inspections with filtering.

**Query Params**:
- `siteId` (optional)
- `panelId` (optional)
- `severity` (optional): HEALTHY, WARNING, CRITICAL
- `startDate` (optional): Unix timestamp
- `endDate` (optional): Unix timestamp
- `limit` (default: 50)
- `offset` (default: 0)

**Response** (200 OK):
```json
{
  "success": true,
  "total": 640,
  "inspections": [
    {
      "inspectionId": "insp_1691424120_001",
      "panelId": "PNL-A7-4402",
      "severity": "CRITICAL",
      "issueType": "hotspot",
      "deltaTemp": 18.5,
      "timestamp": 1691424120000,
      "inspectorName": "John Smith",
      "thermalImageUrl": "https://cdn.solarsnap.com/thermal/img_1691424120.jpg"
    }
  ]
}
```

#### GET /inspections/{inspectionId}
Get detailed inspection information.

**Response** (200 OK):
```json
{
  "success": true,
  "inspection": {
    "inspectionId": "insp_1691424120_001",
    "siteId": "NV-Solar-04",
    "panelId": "PNL-A7-4402",
    "temperature": 58.7,
    "deltaTemp": 18.5,
    "severity": "CRITICAL",
    "issueType": "hotspot",
    "latitude": 36.1234,
    "longitude": -115.2345,
    "timestamp": 1691424120000,
    "thermalImageUrl": "https://cdn.solarsnap.com/thermal/img_1691424120.jpg",
    "metadata": {
      "cameraModel": "FLIR ACE",
      "ambientTemp": 35.2
    }
  }
}
```

#### DELETE /inspections/{inspectionId}
Delete inspection record.

**Response** (200 OK):
```json
{
  "success": true,
  "message": "Inspection deleted successfully"
}
```


### File Uploads (3 endpoints)

#### POST /upload/thermal
Upload thermal image file.

**Headers**: `Authorization: Bearer {token}`, `Content-Type: multipart/form-data`

**Form Data**:
- `file`: Binary image data (JPEG/PNG, max 5MB)
- `inspectionId`: Inspection ID
- `panelId`: Panel ID
- `timestamp`: Unix timestamp

**Response** (201 Created):
```json
{
  "success": true,
  "imageId": "img_thermal_1691424120",
  "url": "https://cdn.solarsnap.com/thermal/img_1691424120.jpg",
  "size": 3245678,
  "uploadedAt": 1691424125000
}
```

#### POST /upload/visual
Upload visual/RGB image (optional).

**Same format as thermal upload**

#### POST /upload/batch
Batch upload multiple inspections and images.

**Form Data**:
- `inspections`: JSON array of inspection objects
- `thermalImages[]`: Array of thermal image files
- `visualImages[]`: Array of visual image files

**Response** (200 OK):
```json
{
  "success": true,
  "uploaded": 15,
  "failed": 0,
  "results": [
    {
      "inspectionId": "insp_1691424120_001",
      "status": "success",
      "thermalImageUrl": "https://cdn.solarsnap.com/thermal/img_1691424120.jpg"
    }
  ]
}
```

### Reports (4 endpoints)

#### GET /reports/site/{siteId}
Generate site inspection summary.

**Query Params**: `startDate`, `endDate`, `format` (json/pdf/csv)

**Response** (200 OK):
```json
{
  "success": true,
  "report": {
    "siteId": "NV-Solar-04",
    "summary": {
      "totalPanels": 1200,
      "inspected": 640,
      "coverage": 53.3
    },
    "faultDistribution": {
      "critical": 4,
      "warning": 11,
      "healthy": 625
    },
    "faultTypes": {
      "hotspot": 3,
      "diodeFault": 1,
      "connectionFault": 2
    }
  }
}
```

#### GET /reports/fault
Fault-specific report.

#### GET /reports/maintenance
Maintenance action recommendations.

#### POST /reports/export
Export report as PDF/CSV and optionally email.

**Request**:
```json
{
  "reportType": "site",
  "siteId": "NV-Solar-04",
  "format": "pdf",
  "dateRange": {
    "start": 1691337720000,
    "end": 1691424120000
  },
  "email": "inspector12@solartech.com"
}
```

**Response** (200 OK):
```json
{
  "success": true,
  "downloadUrl": "https://cdn.solarsnap.com/reports/report_1691424120.pdf",
  "expiresAt": 1691510520000,
  "emailSent": true
}
```


### Sync & Upload Status (3 endpoints)

#### GET /sync/status
Get current sync status for inspector.

**Response** (200 OK):
```json
{
  "success": true,
  "syncStatus": {
    "lastSync": 1691424120000,
    "pending": 3,
    "uploading": 1,
    "completed": 42,
    "failed": 1
  }
}
```

#### GET /sync/queue
Get list of pending uploads.

**Response** (200 OK):
```json
{
  "success": true,
  "queue": [
    {
      "uploadId": "upload_1691424120_001",
      "inspectionId": "insp_1691424120_001",
      "panelId": "PNL-A7-4402",
      "status": "pending",
      "fileSize": 3.2,
      "retryCount": 0
    }
  ]
}
```

#### POST /sync/retry/{uploadId}
Retry a failed upload.

**Response** (200 OK):
```json
{
  "success": true,
  "status": "uploading",
  "message": "Upload retry initiated"
}
```

### Settings (3 endpoints)

#### GET /settings/user
Get user-specific settings.

**Response** (200 OK):
```json
{
  "success": true,
  "settings": {
    "thermalDetection": {
      "warningThreshold": 8.0,
      "criticalThreshold": 15.0,
      "hotspotDetection": true
    },
    "camera": {
      "palette": "Iron",
      "resolution": "640x480"
    },
    "inspection": {
      "autoSave": true,
      "requirePanelScan": true
    },
    "connectivity": {
      "cloudSync": true,
      "autoUpload": true
    }
  }
}
```

#### PUT /settings/user
Update user settings.

**Request**:
```json
{
  "thermalDetection": {
    "warningThreshold": 10.0
  },
  "camera": {
    "palette": "Rainbow"
  }
}
```

**Response** (200 OK):
```json
{
  "success": true,
  "message": "Settings updated successfully"
}
```

#### GET /settings/company/{companyId}
Get company-wide default settings.

**Response** (200 OK):
```json
{
  "success": true,
  "companySettings": {
    "companyId": "SOLARTECH-001",
    "defaultThresholds": {
      "warning": 8.0,
      "critical": 15.0
    },
    "requiredFields": ["panelId", "gps", "thermalImage"]
  }
}
```

---

**Total API Endpoints: 23**


---

## Screen Integration

### 1. LoginActivity
**File**: `LoginActivity.java`  
**Current**: Mock authentication  
**Needs**: Call `POST /auth/login`, store JWT token securely

```java
// TODO: Replace mock with real API call
loginButton.setOnClickListener(v -> {
    String email = emailInput.getText().toString();
    String password = passwordInput.getText().toString();
    String companyId = companyIdInput.getText().toString();
    
    // Call POST /auth/login
    // Store token in EncryptedSharedPreferences
    // Navigate to SiteSelectionActivity
});
```

### 2. SiteSelectionActivity (Dashboard)
**File**: `SiteSelectionActivity.java`  
**Current**: Mock single site  
**Needs**: Call `GET /sites`, `GET /sites/{siteId}`

**API Calls**:
- `GET /sites` - Load available sites on start
- `GET /sites/{siteId}` - Get site details and statistics
- `GET /inspections?siteId={siteId}&date=today` - Today's progress

### 3. MainActivity (Thermal Inspection)
**File**: `MainActivity.java`  
**Current**: FLIR SDK integrated, mock panel IDs  
**Needs**: Panel validation, inspection upload, image upload

**Workflow**:
1. Scan panel barcode → Validate with `GET /sites/{siteId}/panels/{panelId}`
2. Capture thermal image → Process with FLIR SDK
3. Get GPS location → FusedLocationProviderClient
4. Save locally → Room database
5. Queue for upload → Background service
6. Upload when online → `POST /inspections`, `POST /upload/thermal`

**Database Schema**:
```sql
CREATE TABLE inspections (
    id INTEGER PRIMARY KEY,
    inspection_id TEXT UNIQUE,
    site_id TEXT,
    panel_id TEXT,
    temperature REAL,
    delta_temp REAL,
    severity TEXT,
    issue_type TEXT,
    latitude REAL,
    longitude REAL,
    timestamp INTEGER,
    thermal_image_path TEXT,
    uploaded INTEGER DEFAULT 0
);
```

### 4. SiteMapActivity (Fault Map)
**File**: `SiteMapActivity.java`  
**Current**: Mock panel grid, filtering works  
**Needs**: Call `GET /sites/{siteId}/panels` for real panel data

**Features**:
- Color-coded panel grid (Green/Orange/Red/Gray)
- Filter by status (All/Faults/Warnings/Uninspected)
- Click panel for details
- Navigate to reinspection

### 5. InspectionHistoryActivity
**File**: `InspectionHistoryActivity.java`  
**Current**: Mock records with search/filter  
**Needs**: Call `GET /inspections` with pagination

**API Calls**:
- `GET /inspections?siteId={siteId}&limit=50&offset=0`
- `GET /inspections/{inspectionId}` - Details
- `DELETE /inspections/{inspectionId}` - Delete
- `POST /reports/export` - Export records

### 6. UploadsActivity (Sync Status)
**File**: `UploadsActivity.java`  
**Current**: Mock upload records  
**Needs**: Call `GET /sync/queue`, `POST /sync/retry`

**Upload States**:
- Pending: Waiting to upload
- Uploading: Currently uploading
- Uploaded: Successfully uploaded
- Failed: Upload failed (retry available)

### 7. ReportsActivity
**File**: `ReportsActivity.java`  
**Current**: Mock report data  
**Needs**: Call `GET /reports/site/{siteId}`, export endpoints

**Report Types**:
- Site Report: Overall inspection summary
- Fault Report: Detailed fault analysis
- Maintenance Report: Recommended actions

### 8. SettingsActivity
**File**: `SettingsActivity.java`  
**Current**: UI controls, no persistence  
**Needs**: Call `GET /settings/user`, `PUT /settings/user`

**Settings Categories**:
- Thermal Detection (thresholds)
- Camera Configuration (palette)
- Inspection Preferences (auto-save)
- Connectivity (cloud sync)


---

## Authentication

### JWT Token Management

**Access Token**:
- Format: JWT
- Expiration: 1 hour (3600 seconds)
- Storage: EncryptedSharedPreferences
- Header: `Authorization: Bearer {token}`

**Refresh Token**:
- Expiration: 7 days
- Used to obtain new access token
- Rotated on each refresh

### Implementation

```java
// 1. Login - Store tokens
SharedPreferences prefs = EncryptedSharedPreferences.create(...);
prefs.edit()
    .putString("access_token", response.token)
    .putString("refresh_token", response.refreshToken)
    .putLong("token_expires_at", System.currentTimeMillis() + 3600000)
    .apply();

// 2. Check expiration before API calls
if (System.currentTimeMillis() >= tokenExpiresAt) {
    // Call POST /auth/refresh
    // Update access token
}

// 3. Logout - Clear tokens
prefs.edit().clear().apply();
```

### Auth Interceptor (Automatic Token Refresh)

```java
public class AuthInterceptor implements Interceptor {
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        
        // Add token to request
        String token = getAccessToken();
        if (token != null) {
            request = request.newBuilder()
                .addHeader("Authorization", "Bearer " + token)
                .build();
        }
        
        Response response = chain.proceed(request);
        
        // Handle 401 Unauthorized
        if (response.code() == 401) {
            synchronized (this) {
                String newToken = refreshAccessToken();
                if (newToken != null) {
                    // Retry with new token
                    request = request.newBuilder()
                        .header("Authorization", "Bearer " + newToken)
                        .build();
                    return chain.proceed(request);
                }
            }
        }
        
        return response;
    }
}
```


---

## Sync Strategy

### Offline-First Architecture

The app MUST work offline. Inspections are saved locally and synced when online.

### Sync Process

```
1. Capture Inspection
   ↓
2. Save to Local Database (Room/SQLite)
   ↓
3. Add to Upload Queue
   ↓
4. Trigger Background Sync Service
   ↓
5. Check Network Connectivity
   ↓
6. Upload Inspection Data (POST /inspections)
   ↓
7. Upload Thermal Image (POST /upload/thermal)
   ↓
8. Mark as Uploaded
   ↓
9. Remove from Queue
```

### Background Sync Service

```java
public class SyncService extends JobIntentService {
    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        if (!isNetworkAvailable()) return;
        
        List<UploadQueueEntity> pending = database.uploadQueueDao()
            .getPendingUploads();
        
        for (UploadQueueEntity upload : pending) {
            try {
                InspectionEntity inspection = database.inspectionDao()
                    .getById(upload.inspectionId);
                
                // Upload inspection
                uploadInspection(inspection);
                
                // Upload images
                uploadThermalImage(inspection.thermalImagePath);
                
                // Mark as uploaded
                inspection.uploaded = true;
                database.inspectionDao().update(inspection);
                
                upload.status = "uploaded";
                database.uploadQueueDao().update(upload);
                
            } catch (Exception e) {
                // Handle failure
                upload.status = "failed";
                upload.retryCount++;
                upload.errorMessage = e.getMessage();
                database.uploadQueueDao().update(upload);
            }
        }
    }
}
```

### Retry Logic

- **Automatic retry**: Exponential backoff (1s, 2s, 4s, 8s...)
- **Max attempts**: 3 retries
- **Manual retry**: Available in UploadsActivity
- **Priority**: Critical faults uploaded first

### Conflict Resolution

- **Server wins**: Server timestamp is source of truth
- **No merging**: Local changes are uploaded, not merged
- **Flagging**: Conflicts flagged for manual review

### Network Monitoring

```java
public class NetworkMonitor extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        ConnectivityManager cm = (ConnectivityManager) 
            context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        
        if (activeNetwork != null && activeNetwork.isConnected()) {
            // Trigger sync
            Intent syncIntent = new Intent(context, SyncService.class);
            SyncService.enqueueWork(context, syncIntent);
        }
    }
}
```


---

## File Uploads

### Image Specifications

**Thermal Images**:
- Format: JPEG or PNG
- Resolution: 640×480 (FLIR ACE native)
- Color depth: 24-bit RGB
- Max size: 5 MB
- Metadata: EXIF with temperature data

**Visual Images** (optional):
- Format: JPEG
- Resolution: 1920×1080
- Max size: 3 MB

### Upload Process

**1. Compress Before Upload**:
```java
public Bitmap compressImage(String imagePath, int quality) {
    Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos);
    return BitmapFactory.decodeByteArray(baos.toByteArray(), 0, baos.size());
}
```

**2. Multipart Upload**:
```java
public void uploadThermalImage(File imageFile, String inspectionId) {
    RequestBody requestFile = RequestBody.create(
        MediaType.parse("image/jpeg"), imageFile);
    
    MultipartBody.Part body = MultipartBody.Part.createFormData(
        "file", imageFile.getName(), requestFile);
    
    RequestBody inspectionIdBody = RequestBody.create(
        MediaType.parse("text/plain"), inspectionId);
    
    Call<UploadResponse> call = apiService.uploadThermalImage(
        body, inspectionIdBody);
    
    call.enqueue(new Callback<UploadResponse>() {
        @Override
        public void onResponse(Call<UploadResponse> call, 
                             Response<UploadResponse> response) {
            if (response.isSuccessful()) {
                String imageUrl = response.body().url;
                updateInspectionImageUrl(inspectionId, imageUrl);
            }
        }
        
        @Override
        public void onFailure(Call<UploadResponse> call, Throwable t) {
            logUploadError(inspectionId, t.getMessage());
        }
    });
}
```

**3. Progress Tracking**:
```java
public class ProgressRequestBody extends RequestBody {
    private File file;
    private UploadCallback callback;
    
    @Override
    public void writeTo(BufferedSink sink) throws IOException {
        long fileLength = file.length();
        byte[] buffer = new byte[4096];
        long uploaded = 0;
        
        try (FileInputStream in = new FileInputStream(file)) {
            int read;
            while ((read = in.read(buffer)) != -1) {
                uploaded += read;
                sink.write(buffer, 0, read);
                
                int progress = (int) ((uploaded * 100) / fileLength);
                callback.onProgress(progress);
            }
        }
    }
}
```

### Storage Locations

**Local (Android)**:
```
/storage/emulated/0/Android/data/com.flir.atlassdk.acecamerasample/files/
    ├── thermal/
    ├── visual/
    ├── temp/
    └── cache/
```

**Cloud (Backend)**:
```
https://cdn.solarsnap.com/
    ├── thermal/{siteId}/{date}/img_{timestamp}.jpg
    ├── visual/{siteId}/{date}/img_{timestamp}.jpg
    └── reports/{siteId}/{reportId}.pdf
```


---

## Error Handling

### HTTP Status Codes

**Success**:
- `200 OK` - Request successful
- `201 Created` - Resource created
- `204 No Content` - Successful deletion

**Client Errors**:
- `400 Bad Request` - Invalid data
- `401 Unauthorized` - Invalid/expired token
- `403 Forbidden` - Insufficient permissions
- `404 Not Found` - Resource not found
- `409 Conflict` - Duplicate resource
- `422 Unprocessable Entity` - Validation error
- `429 Too Many Requests` - Rate limit

**Server Errors**:
- `500 Internal Server Error`
- `502 Bad Gateway`
- `503 Service Unavailable`

### Error Response Format

```json
{
  "success": false,
  "error": {
    "code": "INVALID_PANEL_ID",
    "message": "Panel ID 'PNL-A7-9999' not found in site 'NV-Solar-04'",
    "details": {
      "panelId": "PNL-A7-9999",
      "siteId": "NV-Solar-04"
    },
    "timestamp": 1691424120000
  }
}
```

### Error Handling Strategy

**1. Network Errors**:
```java
try {
    Response<InspectionResponse> response = apiCall.execute();
    if (response.isSuccessful()) {
        // Success
    } else {
        handleHttpError(response.code(), response.errorBody());
    }
} catch (IOException e) {
    // Network error - save for retry
    saveForRetry(inspection);
    showToast("No connection. Will retry when online.");
}
```

**2. Validation Errors**:
```java
if (response.code() == 422) {
    ErrorResponse error = parseError(response.errorBody());
    showValidationError(error.message);
}
```

**3. Authentication Errors**:
```java
if (response.code() == 401) {
    if (refreshToken()) {
        retryRequest();
    } else {
        logout();
    }
}
```


---

## Implementation

### Phase 1: Authentication & API Setup
1. Add Retrofit/OkHttp dependencies
2. Create API service interface (23 endpoints)
3. Implement authentication interceptor
4. Set up token storage (EncryptedSharedPreferences)
5. Integrate login/logout

### Phase 2: Local Database
1. Add Room dependencies
2. Create entities (Inspection, UploadQueue, Site, Panel)
3. Create DAOs
4. Implement database migrations

### Phase 3: Core Features
1. Integrate sites API
2. Implement panel validation
3. Add inspection upload
4. Implement barcode scanning (ML Kit)
5. Add GPS location tracking

### Phase 4: File Upload & Sync
1. Implement image compression
2. Create multipart upload
3. Add progress tracking
4. Implement background sync service
5. Add network monitoring

### Phase 5: Reports & Settings
1. Integrate report APIs
2. Implement PDF/CSV export
3. Add settings persistence
4. Implement company settings sync

### Phase 6: Testing & Optimization
1. Unit tests for API calls
2. Integration tests for sync
3. UI tests for critical flows
4. Performance optimization
5. Memory leak detection


---

## Code Examples

### Retrofit API Service

```java
public interface SolarSnapApiService {
    // Authentication
    @POST("auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);
    
    @POST("auth/refresh")
    Call<RefreshResponse> refreshToken(@Body RefreshRequest request);
    
    // Sites
    @GET("sites")
    Call<SitesResponse> getSites();
    
    @GET("sites/{siteId}")
    Call<SiteDetailResponse> getSiteDetails(@Path("siteId") String siteId);
    
    @GET("sites/{siteId}/panels")
    Call<PanelsResponse> getSitePanels(@Path("siteId") String siteId);
    
    // Inspections
    @POST("inspections")
    Call<InspectionResponse> createInspection(@Body InspectionRequest request);
    
    @GET("inspections")
    Call<InspectionsResponse> getInspections(
        @Query("siteId") String siteId,
        @Query("limit") Integer limit,
        @Query("offset") Integer offset
    );
    
    // File Uploads
    @Multipart
    @POST("upload/thermal")
    Call<UploadResponse> uploadThermalImage(
        @Part MultipartBody.Part file,
        @Part("inspectionId") RequestBody inspectionId
    );
    
    // Reports
    @GET("reports/site/{siteId}")
    Call<SiteReportResponse> getSiteReport(@Path("siteId") String siteId);
    
    // Sync
    @GET("sync/status")
    Call<SyncStatusResponse> getSyncStatus();
    
    // Settings
    @GET("settings/user")
    Call<UserSettingsResponse> getUserSettings();
    
    @PUT("settings/user")
    Call<UpdateSettingsResponse> updateUserSettings(@Body UpdateSettingsRequest request);
}
```

### Retrofit Client Setup

```java
public class ApiClient {
    private static final String BASE_URL = "https://api.solarsnap.com/v1/";
    private static SolarSnapApiService apiService;
    
    public static SolarSnapApiService getApiService(Context context) {
        if (apiService == null) {
            OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new AuthInterceptor(context))
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
            
            Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
            
            apiService = retrofit.create(SolarSnapApiService.class);
        }
        return apiService;
    }
}
```

### Repository Pattern

```java
public class InspectionRepository {
    private SolarSnapApiService apiService;
    private InspectionDao inspectionDao;
    
    public InspectionRepository(Context context) {
        this.apiService = ApiClient.getApiService(context);
        this.inspectionDao = AppDatabase.getInstance(context).inspectionDao();
    }
    
    // Save locally and queue for upload
    public void saveInspection(PanelInspection inspection) {
        InspectionEntity entity = toEntity(inspection);
        long id = inspectionDao.insert(entity);
        
        UploadQueueEntity uploadQueue = new UploadQueueEntity();
        uploadQueue.inspectionId = id;
        uploadQueue.status = "pending";
        uploadQueue.priority = inspection.getSeverity().equals("CRITICAL") ? 1 : 2;
        
        inspectionDao.insertUploadQueue(uploadQueue);
        triggerSync();
    }
    
    // Upload to backend
    public void uploadInspection(InspectionEntity entity) throws IOException {
        InspectionRequest request = toRequest(entity);
        Response<InspectionResponse> response = apiService
            .createInspection(request)
            .execute();
        
        if (response.isSuccessful()) {
            entity.uploaded = true;
            entity.inspectionId = response.body().inspectionId;
            inspectionDao.update(entity);
        } else {
            throw new IOException("Upload failed: " + response.code());
        }
    }
}
```

### Dependencies (build.gradle)

```gradle
dependencies {
    // Retrofit
    implementation 'com.squareup.retrofit2:retrofit:2.9.0'
    implementation 'com.squareup.retrofit2:converter-gson:2.9.0'
    implementation 'com.squareup.okhttp3:logging-interceptor:4.11.0'
    
    // Room
    implementation 'androidx.room:room-runtime:2.5.2'
    annotationProcessor 'androidx.room:room-compiler:2.5.2'
    
    // Security
    implementation 'androidx.security:security-crypto:1.1.0-alpha06'
    
    // WorkManager
    implementation 'androidx.work:work-runtime:2.8.1'
    
    // Location
    implementation 'com.google.android.gms:play-services-location:21.0.1'
    
    // Barcode Scanning
    implementation 'com.google.mlkit:barcode-scanning:17.2.0'
    
    // Image Loading
    implementation 'com.github.bumptech.glide:glide:4.15.1'
}
```


---

## Testing

### Unit Tests
- Test API service methods
- Test data transformations
- Test business logic

### Integration Tests
- Test database operations
- Test API integration
- Test sync logic

### Manual Testing Checklist
- [ ] Login with valid/invalid credentials
- [ ] Site selection and switching
- [ ] Thermal image capture
- [ ] Panel barcode scanning
- [ ] GPS location accuracy
- [ ] Offline inspection capture
- [ ] Online sync after reconnection
- [ ] Upload retry for failed uploads
- [ ] Report generation
- [ ] Settings persistence
- [ ] Logout

---

## Quick Reference

### API Base URLs
```
Production:  https://api.solarsnap.com/v1
Staging:     https://staging-api.solarsnap.com/v1
Development: https://dev-api.solarsnap.com/v1
```

### Authentication
```
Header: Authorization: Bearer {access_token}
Token Expiration: 3600 seconds (1 hour)
Refresh Token: 7 days
```

### Date Format
```
Unix timestamp in milliseconds (Long)
Example: 1691424120000
```

### Severity Levels
```
HEALTHY  - No issues detected
WARNING  - Minor issue, monitor
CRITICAL - Immediate action required
```

### Issue Types
```
hotspot          - Thermal hotspot detected
diode_failure    - Diode malfunction
cell_crack       - Physical cell damage
connection_fault - Electrical connection issue
shading          - Shading affecting performance
none             - No issue detected
```

### Upload Status
```
pending   - Waiting to upload
uploading - Currently uploading
uploaded  - Successfully uploaded
failed    - Upload failed (retry available)
```

### Image Specifications
```
Thermal: JPEG/PNG, 640×480, max 5MB
Visual:  JPEG, 1920×1080, max 3MB
Compression: 85% quality
```

---

## Support

### For Backend Developers
- **API Documentation**: https://docs.solarsnap.com/api
- **Postman Collection**: Available on request
- **Swagger UI**: https://api.solarsnap.com/swagger

### For Frontend Developers
- **GitHub**: https://github.com/Mumtio/AceCamera
- **Issues**: GitHub Issues
- **Documentation**: See README.md

### Common Issues

**1. Token Expiration**
- Symptom: 401 errors
- Solution: Implement auto-refresh in interceptor

**2. Upload Failures**
- Symptom: Images not uploading
- Solution: Check network, implement retry logic

**3. Database Conflicts**
- Symptom: Duplicate records
- Solution: Use unique constraints

**4. Memory Issues**
- Symptom: App crashes
- Solution: Optimize image handling, recycle bitmaps

**5. GPS Accuracy**
- Symptom: Inaccurate location
- Solution: Request high accuracy, wait for GPS fix

---

## Summary

This guide provides complete specifications for integrating the SolarSnap Android frontend with your backend API.

**Key Deliverables**:
- 23 REST API endpoints
- JWT authentication system
- Image storage infrastructure
- Report generation service
- Real-time sync capabilities

**Next Steps**:
1. Review API endpoint specifications
2. Set up development environment
3. Implement authentication endpoints
4. Build core inspection APIs
5. Set up image storage
6. Implement sync endpoints
7. Add report generation
8. Test integration
9. Deploy to staging
10. Production deployment

**Questions?** Contact the development team for clarification.

---

**End of Backend Integration Guide**

*Last Updated: March 9, 2026*  
*Version: 1.0*  
*Repository: https://github.com/Mumtio/AceCamera*
