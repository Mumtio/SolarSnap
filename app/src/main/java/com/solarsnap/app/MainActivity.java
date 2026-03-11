/*******************************************************************
 * @title FLIR Atlas Android SDK ACE Camera Sample
 * @file MainActivity.java
 * @Author Teledyne FLIR
 *
 * @brief This sample application connects to an ACE camera and renders received images to GLSurfaceView.
 *
 * Copyright 2025:    Teledyne FLIR
 *******************************************************************/
package com.solarsnap.app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import com.solarsnap.app.models.PanelInspection;
import com.solarsnap.app.repository.InspectionRepository;
import com.solarsnap.app.database.entities.InspectionEntity;
import com.solarsnap.app.sync.UploadService;
import com.solarsnap.app.network.UploadProgressService;

import com.flir.thermalsdk.ErrorCode;
import com.flir.thermalsdk.androidsdk.ThermalSdkAndroid;
import com.flir.thermalsdk.androidsdk.helpers.PermissionHandler;
import com.flir.thermalsdk.image.ColorDistributionSettings;
import com.flir.thermalsdk.image.HistogramEqualizationSettings;
import com.flir.thermalsdk.image.Palette;
import com.flir.thermalsdk.image.PaletteManager;
import com.flir.thermalsdk.image.TemperatureUnit;
import com.flir.thermalsdk.image.ThermalValue;
import com.flir.thermalsdk.image.fusion.Fusion;
import com.flir.thermalsdk.image.fusion.FusionMode;
import com.flir.thermalsdk.image.measurements.MeasurementShapeCollection;
import com.flir.thermalsdk.image.measurements.MeasurementSpot;
import com.flir.thermalsdk.live.Camera;
import com.flir.thermalsdk.live.CameraInformation;
import com.flir.thermalsdk.live.CameraType;
import com.flir.thermalsdk.live.CommunicationInterface;
import com.flir.thermalsdk.live.ConnectParameters;
import com.flir.thermalsdk.live.Identity;
import com.flir.thermalsdk.live.discovery.DiscoveredCamera;
import com.flir.thermalsdk.live.discovery.DiscoveryEventListener;
import com.flir.thermalsdk.live.discovery.DiscoveryFactory;
import com.flir.thermalsdk.live.streaming.Stream;
import com.flir.thermalsdk.log.ThermalLog;
import com.flir.thermalsdk.utils.FileUtils;
import com.flir.thermalsdk.utils.Pair;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * This sample application connects to an ACE camera and renders received images to GLSurfaceView.
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    // reference to ACE camera
    private Camera camera;
    // reference to active ACE stream
    private Stream activeStream;
    // user defined palette used to colorize thermal image in desired way
    private Palette currentPalette;
    // user defined fusion mode
    private FusionMode currentFusionMode = FusionMode.THERMAL_ONLY;
    // default ColorDistributionSettings - usually they should be overridden before starting stream if camera provides customized settings
    private ColorDistributionSettings defaultColorSettings = new HistogramEqualizationSettings();

    // delayed surface is required because when the GLSurfaceView is created the camera is not yet created/connected
    private boolean delayedSetSurface;
    private int delayedSurfaceWidth;
    private int delayedSurfaceHeight;

    // request to take a single snapshot from current frame
    private boolean snapshotRequested;

    // enable or disable measurements for live stream
    private boolean enableMeasurements = true;

    // label showing app status, connection status, errors
    private TextView appStatus;
    private TextView assetIdLabel;
    private TextView temperatureLabel;
    private TextView statusLabel;

    // GLSurfaceView which is used to render frames incoming from the camera
    private GLSurfaceView glSurfaceView;

    // Current inspection data
    private PanelInspection currentInspection;
    private String detectedAssetId = "";
    private double maxTemperature = 0.0;
    private double deltaTemperature = 0.0;
    
    // Location services
    private LocationManager locationManager;
    private Location lastKnownLocation;
    
    // Repository for saving inspections
    private InspectionRepository inspectionRepository;
    
    // Background sync service
    private UploadService uploadService;
    
    // Upload progress service
    private UploadProgressService uploadProgressService;
    
    // Barcode scanning
    private static final int BARCODE_SCAN_REQUEST = 1002;

    // helper for requesting
    private PermissionHandler permissionHandler;

    // path where snapshot images will be stored
    private String imagesRoot;

    // Automatic hardware detection - tries ACE first, falls back to emulator
    // This makes the app work on both real ACE devices and regular Android devices
    private CommunicationInterface detectedCameraInterface = null;
    private boolean hardwareDetectionComplete = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main_portrait);

        // do not enable OpenCL (pass null), note the OpenGL is enabled by default when NOT running on AVD
        ThermalSdkAndroid.init(getApplicationContext(), ThermalLog.LogLevel.DEBUG);

        // match views to appropriate actions
        setupViews();
        
        // Check if a specific panel ID was passed from site map for reinspection
        String panelId = getIntent().getStringExtra("panelId");
        if (panelId != null && !panelId.isEmpty()) {
            // Pre-populate the panel ID for reinspection
            detectedAssetId = panelId;
            TextView panelIdLabel = findViewById(R.id.panelIdLabel);
            if (panelIdLabel != null) {
                panelIdLabel.setText(detectedAssetId);
            }
            // Show capture button since panel is already identified
            findViewById(R.id.buttonCapture).setVisibility(android.view.View.VISIBLE);
            Log.d("MainActivity", "Panel ID pre-populated for reinspection: " + panelId);
            Toast.makeText(this, "Reinspecting panel: " + panelId, Toast.LENGTH_SHORT).show();
        } else {
            Log.d("MainActivity", "No panel ID provided - starting fresh inspection");
        }

        ThermalLog.d(TAG, "SDK version = " + ThermalSdkAndroid.getVersion());
        ThermalLog.d(TAG, "SDK commit = " + ThermalSdkAndroid.getCommitHash());

        // helper for handling permission for accessing Manifest.permission.CAMERA
        permissionHandler = new PermissionHandler(this);

        // path for storing snapshots
        imagesRoot = getApplicationContext().getFilesDir().getAbsolutePath();
        ThermalLog.d(TAG, "Images DIR = " + imagesRoot);

        // after initialization of the SDK (via ThermalSdkAndroid.init) we can access default palettes from PaletteManager
        currentPalette = PaletteManager.getDefaultPalettes().get(0);
        
        // Initialize location services (ACE-compatible)
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        
        // Initialize inspection repository
        inspectionRepository = new InspectionRepository(this);
        
        // Initialize and start background sync service
        uploadService = new UploadService(this);
        uploadService.startPeriodicSync();
        
        // Initialize upload progress service
        uploadProgressService = new UploadProgressService(this);
        
        // Setup CSQ file for potential emulator mode
        setupEmulatorCSQFile();
        
        // Start automatic hardware detection
        detectAndConnectCamera();
        
        // Request location permissions and start location updates
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, 
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
        } else {
            startLocationUpdates();
        }
    }
    
    /**
     * Run hardware detection every time the inspection page is loaded/resumed
     * This ensures we always use the best available camera option
     */
    private void runHardwareDetectionOnResume() {
        // Only run detection if we're not already connected to a camera
        // or if the previous connection failed
        if (camera == null || !hardwareDetectionComplete) {
            Log.d(TAG, "🔄 Running hardware detection on resume...");
            
            // Reset detection state
            hardwareDetectionComplete = false;
            detectedCameraInterface = null;
            
            // Disconnect any existing camera first
            if (camera != null) {
                Log.d(TAG, "🔌 Disconnecting previous camera connection...");
                disconnect();
                
                // Wait a moment for cleanup
                new android.os.Handler().postDelayed(() -> {
                    // Setup CSQ file again in case it was removed
                    setupEmulatorCSQFile();
                    
                    // Start fresh hardware detection
                    detectAndConnectCamera();
                }, 500);
            } else {
                // Setup CSQ file again in case it was removed
                setupEmulatorCSQFile();
                
                // Start fresh hardware detection
                detectAndConnectCamera();
            }
        } else {
            Log.d(TAG, "✅ Camera already connected, skipping hardware detection");
        }
    }
    
    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
            == PackageManager.PERMISSION_GRANTED) {
            
            // Request location updates using native Android LocationManager (ACE-compatible)
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, 
                5000, // 5 seconds
                10,   // 10 meters
                new LocationListener() {
                    @Override
                    public void onLocationChanged(Location location) {
                        lastKnownLocation = location;
                    }
                    
                    @Override
                    public void onStatusChanged(String provider, int status, Bundle extras) {}
                    
                    @Override
                    public void onProviderEnabled(String provider) {}
                    
                    @Override
                    public void onProviderDisabled(String provider) {}
                }
            );
            
            // Get last known location
            lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (glSurfaceView != null) {
            glSurfaceView.onPause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (glSurfaceView != null) {
            glSurfaceView.onResume();
        }
        
        // Run automatic hardware detection every time the inspection page is loaded
        // This ensures we always detect the best available camera (ACE hardware vs emulation)
        runHardwareDetectionOnResume();
    }
    
    /**
     * Handle OpenGL errors gracefully - especially important for emulator environments
     */
    private void handleGLErrors() {
        // This method can be called when GL errors are detected
        // to provide user feedback and attempt recovery
        runOnUiThread(() -> {
            Toast.makeText(this, 
                "⚠️ Graphics rendering issue detected. Thermal view may be unstable in emulator.", 
                Toast.LENGTH_LONG).show();
        });
    }
    
    /**
     * Force refresh the thermal view - useful when showing blank screen
     */
    private void refreshThermalView() {
        ThermalLog.d(TAG, "Forcing thermal view refresh...");
        
        if (glSurfaceView != null) {
            runOnUiThread(() -> {
                try {
                    // Force a render request
                    glSurfaceView.requestRender();
                    
                    // Also try to restart the stream if camera is available
                    if (camera != null && activeStream != null) {
                        new Thread(() -> {
                            try {
                                ThermalLog.d(TAG, "Attempting to restart thermal stream...");
                                activeStream.stop();
                                Thread.sleep(500); // Brief pause
                                startStream();
                            } catch (Exception e) {
                                ThermalLog.e(TAG, "Error restarting thermal stream: " + e.getMessage());
                            }
                        }).start();
                    }
                    
                } catch (Exception e) {
                    ThermalLog.e(TAG, "Error refreshing thermal view: " + e.getMessage());
                }
            });
        }
    }

    @Override
    protected void onStop() {
        // disconnect when the app is stopped
        disconnect();
        super.onStop();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        permissionHandler.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        // Handle location permission result
        if (requestCode == 100 && grantResults.length > 0 && 
            grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        }
    }
    
    // ACE Hardware Button Support
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                // Handle ACE back button - go to previous screen or exit
                onBackPressed();
                return true;
            case KeyEvent.KEYCODE_HOME:
                // Handle ACE home button - minimize app
                moveTaskToBack(true);
                return true;
            case KeyEvent.KEYCODE_BUTTON_A: // Trigger button on ACE
                // Handle ACE trigger button - capture inspection
                if (findViewById(R.id.buttonCapture).getVisibility() == android.view.View.VISIBLE) {
                    captureInspection();
                }
                return true;
            case KeyEvent.KEYCODE_DPAD_CENTER: // D-pad center
                // Handle D-pad center press
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == BARCODE_SCAN_REQUEST) {
            if (resultCode == RESULT_OK && data != null) {
                String scannedBarcode = data.getStringExtra(BarcodeScanActivity.EXTRA_SCANNED_BARCODE);
                if (scannedBarcode != null && !scannedBarcode.isEmpty()) {
                    detectedAssetId = scannedBarcode;
                    runOnUiThread(() -> {
                        TextView panelIdLabel = findViewById(R.id.panelIdLabel);
                        if (panelIdLabel != null) {
                            panelIdLabel.setText(detectedAssetId);
                        }
                        Toast.makeText(this, "Panel ID scanned: " + detectedAssetId, Toast.LENGTH_SHORT).show();
                    });
                }
            } else {
                Toast.makeText(this, "Barcode scanning cancelled", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Automatically detect available camera hardware and connect
     * Tries ACE hardware first, falls back to emulator mode if not found
     * This runs every time the inspection page is loaded to ensure optimal camera selection
     */
    private void detectAndConnectCamera() {
        Log.d(TAG, "🔍 Starting automatic camera detection...");
        updateStatusInfo("Detecting camera hardware...");
        
        // First, try to discover real ACE hardware
        tryACEHardwareDiscovery();
    }
    
    /**
     * Try to discover real ACE hardware first
     */
    private void tryACEHardwareDiscovery() {
        Log.d(TAG, "🔍 Attempting to discover ACE hardware...");
        
        DiscoveryFactory.getInstance().scan(new DiscoveryEventListener() {
            @Override
            public void onCameraFound(DiscoveredCamera discoveredCamera) {
                Identity foundIdentity = discoveredCamera.getIdentity();
                
                // Check if we found a real ACE camera
                if (foundIdentity.cameraType == CameraType.ACE && 
                    foundIdentity.communicationInterface == CommunicationInterface.ACE) {
                    
                    Log.d(TAG, "✅ ACE hardware detected: " + foundIdentity.deviceId);
                    detectedCameraInterface = CommunicationInterface.ACE;
                    hardwareDetectionComplete = true;
                    
                    // Stop discovery and connect to real hardware
                    DiscoveryFactory.getInstance().stop(CommunicationInterface.ACE);
                    
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, 
                            "✅ ACE Hardware Detected: " + foundIdentity.deviceId, 
                            Toast.LENGTH_LONG).show();
                        updateStatusInfo("Connected to ACE hardware: " + foundIdentity.deviceId);
                    });
                    
                    // Connect to real ACE hardware
                    doConnect(foundIdentity);
                }
            }
            
            @Override
            public void onDiscoveryError(CommunicationInterface communicationInterface, ErrorCode error) {
                Log.w(TAG, "ACE hardware discovery error: " + error);
                
                // If ACE discovery fails, try emulator mode
                if (!hardwareDetectionComplete) {
                    tryEmulatorMode();
                }
            }
        }, CommunicationInterface.ACE);
        
        // Set a timeout for ACE discovery (5 seconds)
        new android.os.Handler().postDelayed(() -> {
            if (!hardwareDetectionComplete) {
                Log.d(TAG, "⏰ ACE hardware discovery timeout - switching to emulator mode");
                DiscoveryFactory.getInstance().stop(CommunicationInterface.ACE);
                tryEmulatorMode();
            }
        }, 5000);
    }
    
    /**
     * Fall back to emulator mode if ACE hardware not found
     */
    private void tryEmulatorMode() {
        if (hardwareDetectionComplete) {
            return; // Already connected to something
        }
        
        Log.d(TAG, "🔄 Switching to emulator mode...");
        detectedCameraInterface = CommunicationInterface.EMULATOR;
        hardwareDetectionComplete = true;
        
        runOnUiThread(() -> {
            Toast.makeText(MainActivity.this, 
                "📱 Using Thermal Emulation (No ACE hardware detected)", 
                Toast.LENGTH_LONG).show();
            updateStatusInfo("Using thermal emulation mode");
        });
        
        // Start emulator discovery
        startEmulatorDiscovery();
    }
    
    /**
     * Start discovery for emulator mode
     */
    private void startEmulatorDiscovery() {
        DiscoveryFactory.getInstance().scan(new DiscoveryEventListener() {
            @Override
            public void onCameraFound(DiscoveredCamera discoveredCamera) {
                Identity foundIdentity = discoveredCamera.getIdentity();
                
                // Check if we found the emulator camera
                if (foundIdentity.cameraType == CameraType.ACE && 
                    foundIdentity.communicationInterface == CommunicationInterface.EMULATOR) {
                    
                    Log.d(TAG, "✅ Emulator camera found: " + foundIdentity.deviceId);
                    
                    // Stop discovery and connect to emulator
                    DiscoveryFactory.getInstance().stop(CommunicationInterface.EMULATOR);
                    
                    runOnUiThread(() -> {
                        updateStatusInfo("Connected to thermal emulator");
                    });
                    
                    // Connect to emulator
                    doConnect(foundIdentity);
                }
            }
            
            @Override
            public void onDiscoveryError(CommunicationInterface communicationInterface, ErrorCode error) {
                Log.e(TAG, "Emulator discovery error: " + error);
                runOnUiThread(() -> {
                    updateStatusInfo("Camera connection failed: " + error);
                    Toast.makeText(MainActivity.this, 
                        "Camera connection failed. Check CSQ file setup.", 
                        Toast.LENGTH_LONG).show();
                });
            }
        }, CommunicationInterface.EMULATOR);
    }

    /**
     * Connect to the specified Identity.
     *
     * @param identity Identity describing the ACE camera
     */
    private void doConnect(Identity identity) {
        // request camera permission, if granted proceed with connection to ACE camera
        boolean permission = permissionHandler.requestCameraPermission(0x09); // use some arbitrary value for a request code
        if (!permission) {
            ThermalLog.e(TAG, "doConnect, failed due to camera permission");
            updateStatusInfo("doConnect, failed due to camera permission");
            return;
        }

        // run the connection and start stream in a separate thread
        new Thread(() -> {
            try {
                updateStatusInfo("Please wait while connecting to " + identity.deviceId);
                ThermalLog.d(TAG, "Connecting to identity: " + identity);
                // create the camera instance for the first time
                if (camera == null) {
                    camera = new Camera();
                }
                // connect to given identity
                camera.connect(
                        identity,
                        error -> updateStatusInfo("Connection error: " + error),
                        new ConnectParameters());

                // print camera information after connecting
                CameraInformation cameraInfo = Objects.requireNonNull(camera.getRemoteControl()).cameraInformation().getSync();
                ThermalLog.d(TAG, "Camera connected: " + cameraInfo);

                updateStatusInfo("Streaming using " + identity.deviceId);
                // start live streaming
                startStream();
            } catch (IOException e) {
                updateStatusInfo("Connection error: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Setup and start ACE stream.
     */
    private void startStream() {
        ThermalLog.d(TAG, "Preparing stream... obtain available camera stream");
        // assume the camera we are using has at least 1 stream
        activeStream = camera.getStreams().get(0);
        ThermalLog.d(TAG, "Active stream obtained: " + activeStream);

        // setup GL pipeline for this stream
        ThermalLog.d(TAG, "Preparing stream... glSetupPipeline");
        camera.glSetupPipeline(activeStream, true);
        ThermalLog.d(TAG, "GL pipeline setup completed");

        // if ACE camera provides the custom settings use them instead overriding the defaults for HistogramEqualizationSettings
        HistogramEqualizationSettings customHeq = camera.getCustomHistogramEqualizationSettings();
        if (customHeq != null) {
            ThermalLog.d(TAG, "Set custom camera-specific HistogramEqualizationSettings!");
            defaultColorSettings = customHeq;
        } else {
            ThermalLog.d(TAG, "Using default HistogramEqualizationSettings");
        }

        ThermalLog.d(TAG, "Preparing stream... stream starts");
        activeStream.start(
                result -> {
                    // when we received a notification that the image frame is ready we request the GLSurfaceView to redraw it's content
                    ThermalLog.d(TAG, "Frame ready - requesting GLSurfaceView render");
                    glSurfaceView.requestRender();
                },
                error -> {
                    ThermalLog.w(TAG, "Stream.start() failed with error: " + error);
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, 
                            "⚠️ Thermal stream error: " + error, 
                            Toast.LENGTH_LONG).show();
                    });
                });
        
        ThermalLog.d(TAG, "Stream start initiated");
    }

    /**
     * Stop active stream and release related resources.
     */
    private void stopStream() {
        if (camera == null) {
            ThermalLog.w(TAG, "stopStream() failed, camera was null");
            return;
        }

        if (activeStream != null) {
            activeStream.stop();
        }
        camera.glTeardownPipeline();
    }

    /**
     * Completely disconnect from the camera, stop the stream and release resources if needed.
     */
    private void disconnect() {
        // stop stream and disconnect camera in a separate thread
        new Thread(() -> {
            ThermalLog.d(TAG, "disconnect()");
            if (camera == null) {
                return;
            }
            stopStream();
            camera.disconnect();
            camera = null;
        }).start();
    }

    /**
     * Helper function for updating app status label with a given status message.
     *
     * @param status message used to update app status label
     */
    private void updateStatusInfo(String status) {
        ThermalLog.d(TAG, "updateStatusInfo(): " + status);
        
        // Show camera mode in status
        String modeInfo = "";
        if (detectedCameraInterface == CommunicationInterface.ACE) {
            modeInfo = " [ACE Hardware]";
        } else if (detectedCameraInterface == CommunicationInterface.EMULATOR) {
            modeInfo = " [Thermal Emulation]";
        }
        
        final String fullStatus = status + modeInfo;
        
        // Update any status displays if needed
        runOnUiThread(() -> {
            // You can add status display updates here if you have status UI elements
            Log.i(TAG, "Status: " + fullStatus);
        });
    }

    /**
     * Bind UI buttons and switches to appropriate actions and setup GLSurfaceView.
     */
    private void setupViews() {
        // Find thermal inspection views
        TextView maxTempLabel = findViewById(R.id.maxTempLabel);
        TextView deltaTempLabel = findViewById(R.id.deltaTempLabel);
        TextView panelIdLabel = findViewById(R.id.panelIdLabel);
        
        // Setup button listeners
        findViewById(R.id.buttonCapture).setOnClickListener(v -> captureInspection());
        findViewById(R.id.buttonConfirmPanel).setOnClickListener(v -> confirmPanel());
        findViewById(R.id.buttonRescan).setOnClickListener(v -> rescanAsset());
        findViewById(R.id.buttonManualEntry).setOnClickListener(v -> manualEntry());
        findViewById(R.id.buttonFlagIssue).setOnClickListener(v -> showIssueClassificationDialog());
        findViewById(R.id.buttonSaveInspection).setOnClickListener(v -> saveInspection());
        findViewById(R.id.buttonMarkHealthy).setOnClickListener(v -> markAsHealthy());

        // Add long-press listener to thermal view for refresh (debugging feature)
        if (glSurfaceView != null) {
            glSurfaceView.setOnLongClickListener(v -> {
                ThermalLog.d(TAG, "Long press detected on thermal view - refreshing...");
                refreshThermalView();
                Toast.makeText(this, "🔄 Refreshing thermal view...", Toast.LENGTH_SHORT).show();
                return true;
            });
        }

        // setup GLSurfaceView with enhanced emulator compatibility
        glSurfaceView = findViewById(R.id.glSurface);
        glSurfaceView.setEGLContextClientVersion(3);
        glSurfaceView.setPreserveEGLContextOnPause(false);
        
        // Enhanced configuration for better emulator compatibility
        try {
            // Set debug flags for better error reporting in emulator
            glSurfaceView.setDebugFlags(GLSurfaceView.DEBUG_CHECK_GL_ERROR | GLSurfaceView.DEBUG_LOG_GL_CALLS);
        } catch (Exception e) {
            Log.w(TAG, "Could not set GL debug flags: " + e.getMessage());
        }
        
        // set custom renderer, that will handle pushing camera's frame to the view
        glSurfaceView.setRenderer(renderer);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        
        Log.d(TAG, "GLSurfaceView configured for thermal rendering");
        
        // Auto-start camera connection with hardware detection
        // detectAndConnectCamera(); // Already called in onCreate
    }
    
    private void confirmPanel() {
        // Get the panel ID from the UI label
        TextView panelIdLabel = findViewById(R.id.panelIdLabel);
        if (panelIdLabel != null && panelIdLabel.getText() != null) {
            String panelId = panelIdLabel.getText().toString().trim();
            if (!panelId.isEmpty() && !panelId.equals("PANEL ID")) {
                detectedAssetId = panelId;
                Toast.makeText(this, "Panel confirmed: " + detectedAssetId, Toast.LENGTH_SHORT).show();
                // Show capture button
                findViewById(R.id.buttonCapture).setVisibility(android.view.View.VISIBLE);
            } else {
                Toast.makeText(this, "No panel ID to confirm", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Panel ID not found", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void rescanAsset() {
        scanAssetId();
    }
    
    private void manualEntry() {
        // Show dialog for manual panel ID entry
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Manual Panel ID Entry");
        
        // Create input field
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setHint("Enter Panel ID (e.g., PNL-A7-1234)");
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        input.setPadding(50, 40, 50, 40);
        input.setTextSize(18);
        
        builder.setView(input);
        
        builder.setPositiveButton("Confirm", (dialog, which) -> {
            String enteredId = input.getText().toString().trim();
            if (!enteredId.isEmpty()) {
                detectedAssetId = enteredId;
                runOnUiThread(() -> {
                    TextView panelIdLabel = findViewById(R.id.panelIdLabel);
                    if (panelIdLabel != null) {
                        panelIdLabel.setText(detectedAssetId);
                    }
                    Toast.makeText(this, "Panel ID set: " + detectedAssetId, Toast.LENGTH_SHORT).show();
                    // Show capture button
                    findViewById(R.id.buttonCapture).setVisibility(android.view.View.VISIBLE);
                });
            } else {
                Toast.makeText(this, "Please enter a valid Panel ID", Toast.LENGTH_SHORT).show();
            }
        });
        
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        
        builder.show();
    }
    
    private void showIssueClassificationDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        android.view.View dialogView = getLayoutInflater().inflate(R.layout.dialog_issue_classification, null);
        builder.setView(dialogView);
        
        android.app.AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        
        dialogView.findViewById(R.id.buttonIssueHotspot).setOnClickListener(v -> {
            selectIssueType("Hotspot");
            dialog.dismiss();
        });
        
        dialogView.findViewById(R.id.buttonIssueDiode).setOnClickListener(v -> {
            selectIssueType("Diode Failure");
            dialog.dismiss();
        });
        
        dialogView.findViewById(R.id.buttonIssueCellCrack).setOnClickListener(v -> {
            selectIssueType("Cell Crack");
            dialog.dismiss();
        });
        
        dialogView.findViewById(R.id.buttonIssueConnection).setOnClickListener(v -> {
            selectIssueType("Connection Fault");
            dialog.dismiss();
        });
        
        dialogView.findViewById(R.id.buttonIssueOther).setOnClickListener(v -> {
            selectIssueType("Other");
            dialog.dismiss();
        });
        
        dialogView.findViewById(R.id.buttonCancelIssue).setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
    }
    
    private void selectIssueType(String issueType) {
        if (currentInspection != null) {
            currentInspection.setIssueType(issueType);
            
            // Auto-determine severity
            String severity;
            if (maxTemperature > 50.0) {
                severity = "CRITICAL";
            } else if (maxTemperature > 35.0) {
                severity = "WARNING";
            } else {
                severity = "HEALTHY";
            }
            currentInspection.setSeverity(severity);
            
            Toast.makeText(this, "Issue: " + issueType + " - " + severity, Toast.LENGTH_SHORT).show();
            
            // Show save button
            findViewById(R.id.buttonSaveInspection).setVisibility(android.view.View.VISIBLE);
        }
    }
    
    private void saveInspection() {
        if (currentInspection == null) {
            Toast.makeText(this, "No inspection data", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (inspectionRepository == null) {
            Toast.makeText(this, "Database not initialized", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            // Convert PanelInspection to InspectionEntity
            InspectionEntity entity = new InspectionEntity();
            
            // Safely get site ID from intent
            String siteId = getIntent().getStringExtra("siteId");
            if (siteId == null || siteId.isEmpty()) {
                Toast.makeText(this, "No site information available", Toast.LENGTH_SHORT).show();
                return;
            }
            
            entity.setSiteId(siteId);
            entity.setPanelId(currentInspection.getAssetId());
            entity.setTemperature(currentInspection.getTemperature());
            entity.setDeltaTemp(currentInspection.getDeltaTemp());
            entity.setSeverity(currentInspection.getSeverity());
            entity.setIssueType(currentInspection.getIssueType());
            entity.setLatitude(currentInspection.getLatitude());
            entity.setLongitude(currentInspection.getLongitude());
            entity.setTimestamp(currentInspection.getTimestamp());
            entity.setThermalImagePath(currentInspection.getThermalImagePath());
            entity.setVisualImagePath(currentInspection.getVisualImagePath());
            
            // Save to database
            inspectionRepository.saveInspection(entity, new InspectionRepository.InspectionCallback() {
                @Override
                public void onSuccess(InspectionEntity inspection) {
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, 
                            "Inspection saved successfully", Toast.LENGTH_SHORT).show();
                        showCaptureConfirmationDialog();
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, 
                            "Error saving inspection: " + error, Toast.LENGTH_LONG).show();
                        Log.e("MainActivity", "Error saving inspection", new Exception(error));
                    });
                }
            });
            
        } catch (Exception e) {
            Log.e("MainActivity", "Exception in saveInspection", e);
            Toast.makeText(this, "Error preparing inspection data: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    private void markAsHealthy() {
        if (detectedAssetId.isEmpty()) {
            Toast.makeText(this, "Please scan asset first", Toast.LENGTH_SHORT).show();
            return;
        }
        
        currentInspection = new PanelInspection(detectedAssetId);
        currentInspection.setSeverity("HEALTHY");
        currentInspection.setIssueType("none");
        currentInspection.setTemperature(maxTemperature);
        
        saveInspection();
    }
    
    private void showCaptureConfirmationDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        android.view.View dialogView = getLayoutInflater().inflate(R.layout.dialog_capture_confirmation, null);
        builder.setView(dialogView);
        
        android.app.AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        
        // Populate dialog with inspection data
        TextView savedPanelIdLabel = dialogView.findViewById(R.id.savedPanelIdLabel);
        TextView savedIssueLabel = dialogView.findViewById(R.id.savedIssueLabel);
        TextView savedDeltaTempLabel = dialogView.findViewById(R.id.savedDeltaTempLabel);
        TextView savedGpsLabel = dialogView.findViewById(R.id.savedGpsLabel);
        
        savedPanelIdLabel.setText(currentInspection.getAssetId());
        savedIssueLabel.setText(currentInspection.getIssueType());
        savedDeltaTempLabel.setText(String.format("+%.1f°C", currentInspection.getDeltaTemp()));
        savedGpsLabel.setText(String.format("%.1f, %.1f", 
            currentInspection.getLatitude(), currentInspection.getLongitude()));
        
        dialogView.findViewById(R.id.buttonNextPanel).setOnClickListener(v -> {
            dialog.dismiss();
            resetForNextPanel();
        });
        
        dialogView.findViewById(R.id.buttonOpenMapFromConfirm).setOnClickListener(v -> {
            dialog.dismiss();
            
            // Navigate to site map with the current site information from intent
            Intent intent = new Intent(MainActivity.this, SiteMapActivity.class);
            String siteId = getIntent().getStringExtra("siteId");
            String siteName = getIntent().getStringExtra("siteName");
            
            if (siteId != null) {
                intent.putExtra("siteId", siteId);
                intent.putExtra("siteName", siteName);
                intent.putExtra("refreshData", true); // Flag to refresh data
            }
            startActivity(intent);
        });
        
        dialog.show();
    }
    
    private void resetForNextPanel() {
        detectedAssetId = "";
        currentInspection = null;
        maxTemperature = 0.0;
        deltaTemperature = 0.0;
        
        // Reset UI
        findViewById(R.id.actionButtonsPanel).setVisibility(android.view.View.GONE);
        findViewById(R.id.buttonCapture).setVisibility(android.view.View.VISIBLE);
        
        Toast.makeText(this, "Ready for next panel", Toast.LENGTH_SHORT).show();
    }
    
    private void scanAssetId() {
        // Launch barcode scanning activity
        Intent intent = new Intent(this, BarcodeScanActivity.class);
        startActivityForResult(intent, BARCODE_SCAN_REQUEST);
    }
    
    private void captureInspection() {
        if (detectedAssetId.isEmpty()) {
            // Show error message instead of redirecting to barcode scanning
            Toast.makeText(this, "Please scan or confirm a panel ID first", Toast.LENGTH_SHORT).show();
            return;
        }
        
        currentInspection = new PanelInspection(detectedAssetId);
        currentInspection.setTemperature(maxTemperature);
        currentInspection.setDeltaTemp(deltaTemperature);
        
        // Get GPS location using native LocationManager (ACE-compatible)
        if (lastKnownLocation != null) {
            currentInspection.setLatitude(lastKnownLocation.getLatitude());
            currentInspection.setLongitude(lastKnownLocation.getLongitude());
        } else if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
            == PackageManager.PERMISSION_GRANTED) {
            // Try to get fresh location
            Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (location != null) {
                currentInspection.setLatitude(location.getLatitude());
                currentInspection.setLongitude(location.getLongitude());
            }
        }
        
        snapshotRequested = true;
        
        // Hide capture button, show action buttons
        findViewById(R.id.buttonCapture).setVisibility(android.view.View.GONE);
        findViewById(R.id.actionButtonsPanel).setVisibility(android.view.View.VISIBLE);
        
        Toast.makeText(this, "Image captured for " + detectedAssetId, Toast.LENGTH_SHORT).show();
    }
    
    private void flagCurrentIssue() {
        if (currentInspection == null) {
            Toast.makeText(this, "No inspection data available", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Show issue classification dialog instead
        showIssueClassificationDialog();
    }

    /**
     * Custom renderer, that will handle pushing camera's frame to the view
     * Enhanced with better error handling for emulator compatibility
     */
    private final GLSurfaceView.Renderer renderer = new GLSurfaceView.Renderer() {

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            ThermalLog.d(TAG, "onSurfaceCreated()");
            
            // Clear any previous GL errors
            while (gl.glGetError() != GL10.GL_NO_ERROR) {
                // Clear error queue
            }
            
            // Set up basic GL state for thermal rendering
            try {
                gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
                gl.glEnable(GL10.GL_TEXTURE_2D);
                
                // Additional GL setup for better emulator compatibility
                gl.glDisable(GL10.GL_DEPTH_TEST);
                gl.glDisable(GL10.GL_CULL_FACE);
                gl.glEnable(GL10.GL_BLEND);
                gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
                
                // Check for GL errors after setup
                int error = gl.glGetError();
                if (error != GL10.GL_NO_ERROR) {
                    ThermalLog.w(TAG, "GL error during surface creation: " + error);
                }
                
                ThermalLog.d(TAG, "GL surface created successfully");
            } catch (Exception e) {
                ThermalLog.e(TAG, "Exception during GL surface creation: " + e.getMessage());
            }
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            ThermalLog.d(TAG, "onSurfaceChanged(), width=" + width + ", height=" + height);
            
            try {
                // Clear any previous GL errors
                while (gl.glGetError() != GL10.GL_NO_ERROR) {
                    // Clear error queue
                }
                
                if (camera != null) {
                    // if camera instance is already set propagate event to Camera object
                    camera.glOnSurfaceChanged(width, height);
                    delayedSetSurface = false;
                } else {
                    ThermalLog.d(TAG, "Failed to set surface changed, because camera hasn't been created yet");
                    // delay setting up the camera.glOnSurfaceChanged until the camera is created and frame is requested to be drawn
                    delayedSetSurface = true;
                    delayedSurfaceWidth = width;
                    delayedSurfaceHeight = height;
                }
                
                // Check for GL errors after surface change
                int error = gl.glGetError();
                if (error != GL10.GL_NO_ERROR) {
                    ThermalLog.w(TAG, "GL error during surface change: " + error);
                }
                
            } catch (Exception e) {
                ThermalLog.e(TAG, "Exception during GL surface change: " + e.getMessage());
            }
        }

        @Override
        public void onDrawFrame(GL10 gl) {
            try {
                // Clear any accumulated GL errors before processing
                while (gl.glGetError() != GL10.GL_NO_ERROR) {
                    // Clear error queue silently
                }
                
                // propagate event to Camera object if it exists
                if (camera != null) {
                    if (!camera.glIsGlContextReady()) {
                        ThermalLog.w(TAG, "Skip processing for onDrawFrame because camera.glIsGlContextReady is NOT ready");
                        return;
                    }

                    // if the surface was create prior to the camera, we need to setup the camera.glOnSurfaceChanged here instead
                    if (delayedSetSurface) {
                        // The onSurfaceChanged was called before we had a camera, so set it now instead
                        camera.glOnSurfaceChanged(delayedSurfaceWidth, delayedSurfaceHeight);
                        delayedSetSurface = false;
                    }

                // we can set various thermal image stream parameters via glWithThermalImage() block since we need a thread-safe access to ThermalImage
                camera.glWithThermalImage(thermalImage -> {
                    try {
                        // Debug: Log thermal image properties
                        int width = thermalImage.getWidth();
                        int height = thermalImage.getHeight();
                        ThermalLog.d(TAG, "Thermal image dimensions: " + width + "x" + height);
                        
                        // setup palette
                        thermalImage.setPalette(currentPalette);
                        ThermalLog.d(TAG, "Palette set: " + (currentPalette != null ? currentPalette.name : "null"));
                        
                        // setup fusion mode
                        Fusion fusion = thermalImage.getFusion();
                        if (fusion != null) {
                            fusion.setFusionMode(currentFusionMode);
                            ThermalLog.d(TAG, "Fusion mode set: " + currentFusionMode);
                        } else {
                            ThermalLog.w(TAG, "Fusion is null - this may cause rendering issues");
                        }

                        // apply the color mode - by default it is HistogramEqualizationSettings
                        // and if camera provided customized HistogramEqualizationSettings via Camera.getCustomHistogramEqualizationSettings(), it will be used as default
                        // of course user can select any other ColorDistributionSettings
                        // and he can provide own parameters for HistogramEqualizationSettings too, which may be different than default and different than camera-specific settings
                        // in this sample though we only use either default HistogramEqualizationSettings or the customized HistogramEqualizationSettings that camera provides
                        thermalImage.setColorDistributionSettings(defaultColorSettings);
                        ThermalLog.d(TAG, "Color distribution settings applied");

                        // setup measurements
                        if (enableMeasurements) {
                            // access MeasurementShapeCollection for this ThermalImage
                            MeasurementShapeCollection measurements = thermalImage.getMeasurements();
                            // assume we want to add a few spot measurements
                            List<MeasurementSpot> spots = measurements.getSpots();
                            // measurements are persisted between frames, so make sure we add them once
                            if (spots.size() < 3) {
                                // get the image size, so that we know the range within which we can place spots
                                int w = thermalImage.getWidth();
                                int h = thermalImage.getHeight();
                                // add 3 spots at various places in the image
                                measurements.addSpot(w / 3, h / 3);
                                measurements.addSpot(w / 2, h / 2);
                                measurements.addSpot(w * 2 / 3, h * 2 / 3);
                            }

                            // Track max temperature and calculate delta
                            spots = measurements.getSpots();
                            
                            double maxTemp = 0.0;
                            double minTemp = Double.MAX_VALUE;
                            boolean hasValidTemperature = false;
                            
                            for (MeasurementSpot sp : spots) {
                                ThermalValue temp = sp.getValue();
                                if (temp != null) {
                                    ThermalValue celsiusTemp = temp.asCelsius();
                                    
                                    // Extract numeric temperature value
                                    // Note: ThermalValue doesn't have direct numeric access in some SDK versions
                                    // We'll parse the string representation as a workaround
                                    String tempStr = celsiusTemp.toString();
                                    try {
                                        // Parse temperature from string like "25.3°C"
                                        String numericPart = tempStr.replaceAll("[^0-9.-]", "");
                                        if (!numericPart.isEmpty()) {
                                            double tempValue = Double.parseDouble(numericPart);
                                            maxTemp = Math.max(maxTemp, tempValue);
                                            minTemp = Math.min(minTemp, tempValue);
                                            hasValidTemperature = true;
                                        }
                                    } catch (NumberFormatException e) {
                                        Log.w(TAG, "Could not parse temperature: " + tempStr);
                                    }
                                }
                            }
                            
                            if (hasValidTemperature) {
                                maxTemperature = maxTemp;
                                deltaTemperature = maxTemp - minTemp;
                                
                                // Update UI with real temperature info
                                runOnUiThread(() -> {
                                    TextView maxTempLabel = findViewById(R.id.maxTempLabel);
                                    TextView deltaTempLabel = findViewById(R.id.deltaTempLabel);
                                    if (maxTempLabel != null) {
                                        maxTempLabel.setText(String.format("Max Temp: %.1f°C", maxTemperature));
                                    }
                                    if (deltaTempLabel != null) {
                                        deltaTempLabel.setText(String.format("ΔT: +%.1f°C", deltaTemperature));
                                    }
                                });
                            }
                        }

                        if (snapshotRequested) {
                            // reset the flag first
                            snapshotRequested = false;

                            // we might set a desired temperature unit before storing an image
                            // this might we useful if for the live stream we display one unit, but for storing image we want another unit
                            thermalImage.setTemperatureUnit(TemperatureUnit.CELSIUS);

                            // we might also use the calculated scale ranges and apply them when storing the image
                            // this way the image will render with fine scale span regardless if we open it with autoscale option enabled or disabled
                            // the drawback is that we override the manual scale setting if there were any
                            // so this approach may not be suitable for certain scenarios
                            Pair<ThermalValue, ThermalValue> range = camera.glGetScaleRange();
                            ThermalLog.d(TAG, "glGetScaleRange when storing image: " + range.first + " - " + range.second);
                            thermalImage.getScale().setRange(range.first, range.second);

                            // define an image path - use a helper function to prepare the file name
                            String snapshotPath = FileUtils.prepareUniqueFileName(imagesRoot, "ACE_", "jpg");
                            try {
                                // save the snapshot at the given path
                                thermalImage.saveAs(snapshotPath);
                                ThermalLog.d(TAG, "Snapshot stored under: " + snapshotPath);
                                
                                // Upload image with progress tracking if panel ID is available
                                if (detectedAssetId != null && !detectedAssetId.isEmpty()) {
                                    uploadThermalImageWithProgress(detectedAssetId, snapshotPath);
                                }
                                
                            } catch (IOException e) {
                                ThermalLog.e(TAG, "Unable to take snapshot: " + e.getMessage());
                            }
                        }
                    } catch (Exception e) {
                        ThermalLog.e(TAG, "Exception in glWithThermalImage: " + e.getMessage());
                    }
                });

                // request the camera to push the frame buffer for drawing on the GLSurfaceView
                try {
                    ThermalLog.d(TAG, "Calling glOnDrawFrame...");
                    camera.glOnDrawFrame();
                    ThermalLog.d(TAG, "glOnDrawFrame completed successfully");
                    
                    // Check for GL errors after frame rendering
                    // Note: We don't use gl.glGetError() here as it's not available in this context
                    // The FLIR SDK handles GL errors internally
                    
                } catch (Exception e) {
                    ThermalLog.e(TAG, "Exception during glOnDrawFrame: " + e.getMessage());
                    // Continue execution - don't crash the app
                }

            } else {
                // Camera not ready - clear the frame buffer to prevent artifacts
                try {
                    gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
                    ThermalLog.d(TAG, "Camera not ready - cleared frame buffer");
                } catch (Exception e) {
                    // Ignore GL errors when camera is not ready
                    ThermalLog.w(TAG, "Could not clear frame buffer: " + e.getMessage());
                }
            }
            
        } catch (Exception e) {
            ThermalLog.e(TAG, "Exception in onDrawFrame: " + e.getMessage());
            // Don't crash the app - thermal rendering can be temperamental in emulators
        }
    }
};
    
    /**
     * Upload thermal image with progress tracking
     */
    private void uploadThermalImageWithProgress(String panelId, String imagePath) {
        runOnUiThread(() -> {
            // Show upload progress dialog
            android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(this);
            progressDialog.setTitle("Uploading Thermal Image");
            progressDialog.setMessage("Compressing and uploading image...");
            progressDialog.setProgressStyle(android.app.ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.setMax(100);
            progressDialog.setCancelable(false);
            progressDialog.show();
            
            // Get file size estimate
            String sizeEstimate = uploadProgressService.getUploadSizeEstimate(imagePath);
            progressDialog.setMessage("Uploading: " + sizeEstimate);
            
            uploadProgressService.uploadThermalImage(panelId, imagePath, 
                new UploadProgressService.UploadProgressListener() {
                    @Override
                    public void onProgress(int percentage) {
                        runOnUiThread(() -> {
                            progressDialog.setProgress(percentage);
                            if (percentage < 30) {
                                progressDialog.setMessage("Compressing image...");
                            } else if (percentage < 90) {
                                progressDialog.setMessage("Uploading to server...");
                            } else {
                                progressDialog.setMessage("Finalizing upload...");
                            }
                        });
                    }
                    
                    @Override
                    public void onSuccess(String response) {
                        runOnUiThread(() -> {
                            progressDialog.dismiss();
                            Toast.makeText(MainActivity.this, 
                                "Thermal image uploaded successfully!", Toast.LENGTH_SHORT).show();
                        });
                    }
                    
                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            progressDialog.dismiss();
                            Toast.makeText(MainActivity.this, 
                                "Upload failed: " + error, Toast.LENGTH_LONG).show();
                        });
                    }
                });
        });
    }
    
    /**
     * Setup CSQ file for potential FLIR emulator mode
     * Always prepares the CSQ file since we don't know if ACE hardware will be detected
     * This runs every time to ensure the CSQ file is available for emulation
     * Enhanced with better error handling and retry logic
     */
    private void setupEmulatorCSQFile() {
        try {
            // Create the files directory if it doesn't exist
            File filesDir = getFilesDir();
            if (!filesDir.exists()) {
                filesDir.mkdirs();
            }
            
            // Target file path where FLIR SDK expects the CSQ file
            // The SDK looks for "ace_emulator_04.csq" by default
            File csqFile = new File(filesDir, "ace_emulator_04.csq");
            
            // Always ensure we have a fresh, valid CSQ file
            boolean needsRefresh = !csqFile.exists() || csqFile.length() == 0;
            
            // Also refresh if file is older than 1 hour (helps with corruption issues)
            if (!needsRefresh && csqFile.exists()) {
                long fileAge = System.currentTimeMillis() - csqFile.lastModified();
                if (fileAge > 3600000) { // 1 hour in milliseconds
                    Log.d(TAG, "📁 CSQ file is old, refreshing...");
                    needsRefresh = true;
                }
            }
            
            if (needsRefresh) {
                Log.d(TAG, "📁 Preparing CSQ file for emulator mode: " + csqFile.getAbsolutePath());
                
                // Delete old file if it exists
                if (csqFile.exists()) {
                    csqFile.delete();
                }
                
                // Copy CSQ file from assets with retry logic
                boolean copySuccess = false;
                int retryCount = 0;
                int maxRetries = 3;
                
                while (!copySuccess && retryCount < maxRetries) {
                    try {
                        InputStream inputStream = getAssets().open("ace_emulator_04.csq");
                        FileOutputStream outputStream = new FileOutputStream(csqFile);
                        
                        byte[] buffer = new byte[4096]; // Larger buffer for better performance
                        int length;
                        long totalBytes = 0;
                        
                        while ((length = inputStream.read(buffer)) > 0) {
                            outputStream.write(buffer, 0, length);
                            totalBytes += length;
                        }
                        
                        outputStream.flush();
                        outputStream.close();
                        inputStream.close();
                        
                        // Verify the file was written correctly
                        if (csqFile.exists() && csqFile.length() > 0) {
                            Log.d(TAG, "✅ CSQ file prepared successfully. Size: " + csqFile.length() + " bytes");
                            copySuccess = true;
                        } else {
                            throw new IOException("CSQ file verification failed");
                        }
                        
                    } catch (IOException e) {
                        retryCount++;
                        Log.w(TAG, "CSQ file copy attempt " + retryCount + " failed: " + e.getMessage());
                        
                        if (retryCount < maxRetries) {
                            // Wait before retry
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                                break;
                            }
                        }
                    }
                }
                
                if (!copySuccess) {
                    throw new IOException("Failed to copy CSQ file after " + maxRetries + " attempts");
                }
                
            } else {
                Log.d(TAG, "✅ CSQ file already available: " + csqFile.getAbsolutePath() + 
                    " (Size: " + csqFile.length() + " bytes)");
            }
            
        } catch (IOException e) {
            Log.e(TAG, "❌ Error preparing CSQ file", e);
            runOnUiThread(() -> {
                Toast.makeText(this, "⚠️ Warning: CSQ file preparation failed - emulator mode may not work", 
                    Toast.LENGTH_LONG).show();
            });
        }
    }

}
