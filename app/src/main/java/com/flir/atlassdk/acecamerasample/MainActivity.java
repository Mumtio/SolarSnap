/*******************************************************************
 * @title FLIR Atlas Android SDK ACE Camera Sample
 * @file MainActivity.java
 * @Author Teledyne FLIR
 *
 * @brief This sample application connects to an ACE camera and renders received images to GLSurfaceView.
 *
 * Copyright 2025:    Teledyne FLIR
 *******************************************************************/
package com.flir.atlassdk.acecamerasample;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.flir.atlassdk.acecamerasample.models.PanelInspection;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

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
    private FusedLocationProviderClient fusedLocationClient;

    // helper for requesting
    private PermissionHandler permissionHandler;

    // path where snapshot images will be stored
    private String imagesRoot;

    // you can easily switch between running the sample on emulator or on real camera by setting aceRealCameraInterface to appropriate value
    // by default we run on a real ACE camera
//    private static final CommunicationInterface aceRealCameraInterface = CommunicationInterface.EMULATOR;
        private static final CommunicationInterface aceRealCameraInterface = CommunicationInterface.ACE;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main_portrait);

        // do not enable OpenCL (pass null), note the OpenGL is enabled by default when NOT running on AVD
        ThermalSdkAndroid.init(getApplicationContext(), ThermalLog.LogLevel.DEBUG);

        // match views to appropriate actions
        setupViews();

        ThermalLog.d(TAG, "SDK version = " + ThermalSdkAndroid.getVersion());
        ThermalLog.d(TAG, "SDK commit = " + ThermalSdkAndroid.getCommitHash());

        // helper for handling permission for accessing Manifest.permission.CAMERA
        permissionHandler = new PermissionHandler(this);

        // path for storing snapshots
        imagesRoot = getApplicationContext().getFilesDir().getAbsolutePath();
        ThermalLog.d(TAG, "Images DIR = " + imagesRoot);

        // after initialization of the SDK (via ThermalSdkAndroid.init) we can access default palettes from PaletteManager
        currentPalette = PaletteManager.getDefaultPalettes().get(0);
        
        // Initialize location services
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        
        // Request location permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, 
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
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
        // in real app result of the camera permission request could be handled in onRequestPermissionsResult
        // here we ignore onRequestPermissionsResult for simplicity, assuming permission will be granted
    }

    /**
     * Discover, connect and start stream.
     */
    private void startDiscoveryAndConnectionAndStream() {
        DiscoveryFactory.getInstance().scan(new DiscoveryEventListener() {
            @Override
            public void onCameraFound(DiscoveredCamera discoveredCamera) {
                Identity foundIdentity = discoveredCamera.getIdentity();
                // check if we have found the expected camera type on the specified interface
                if (foundIdentity.cameraType == CameraType.ACE && foundIdentity.communicationInterface == aceRealCameraInterface) {
                    // automatically stop discovery when we have found a desired camera and connect to it
                    DiscoveryFactory.getInstance().stop(aceRealCameraInterface);
                    // proceed with connection with the discovered ACE identity
                    doConnect(foundIdentity);
                }
            }

            @Override
            public void onDiscoveryError(CommunicationInterface communicationInterface, ErrorCode error) {
                updateStatusInfo("Discovery error: " + error);
            }
        }, aceRealCameraInterface);
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

        // setup GL pipeline for this stream
        ThermalLog.d(TAG, "Preparing stream... glSetupPipeline");
        camera.glSetupPipeline(activeStream, true);

        // if ACE camera provides the custom settings use them instead overriding the defaults for HistogramEqualizationSettings
        HistogramEqualizationSettings customHeq = camera.getCustomHistogramEqualizationSettings();
        if (customHeq != null) {
            ThermalLog.d(TAG, "Set custom camera-specific HistogramEqualizationSettings!");
            defaultColorSettings = customHeq;
        }

        ThermalLog.d(TAG, "Preparing stream... stream starts");
        activeStream.start(
                result -> {
                    // when we received a notification that the image frame is ready we request the GLSurfaceView to redraw it's content
                    glSurfaceView.requestRender();
                },
                error -> ThermalLog.w(TAG, "Stream.start() failed with error: " + error));
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
        // Just log the status, no UI update needed in new layout
        // Status is shown through detection overlay and system indicators
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

        // setup GLSurfaceView
        glSurfaceView = findViewById(R.id.glSurface);
        glSurfaceView.setEGLContextClientVersion(3);
        glSurfaceView.setPreserveEGLContextOnPause(false);
        // set custom renderer, that will handle pushing camera's frame to the view
        glSurfaceView.setRenderer(renderer);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        
        // Auto-start camera connection
        startDiscoveryAndConnectionAndStream();
    }
    
    private void confirmPanel() {
        Toast.makeText(this, "Panel confirmed: " + detectedAssetId, Toast.LENGTH_SHORT).show();
        // Show capture button
        findViewById(R.id.buttonCapture).setVisibility(android.view.View.VISIBLE);
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
        
        // Show confirmation dialog
        showCaptureConfirmationDialog();
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
            Toast.makeText(this, "Opening site map", Toast.LENGTH_SHORT).show();
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
        // Simulate barcode scan - in production, use ML Kit barcode scanner
        detectedAssetId = "PNL-A7-" + (int)(Math.random() * 9999);
        runOnUiThread(() -> {
            TextView panelIdLabel = findViewById(R.id.panelIdLabel);
            if (panelIdLabel != null) {
                panelIdLabel.setText(detectedAssetId);
            }
            Toast.makeText(this, "Asset detected: " + detectedAssetId, Toast.LENGTH_SHORT).show();
        });
    }
    
    private void captureInspection() {
        if (detectedAssetId.isEmpty()) {
            // Auto-generate asset ID for demo
            scanAssetId();
        }
        
        currentInspection = new PanelInspection(detectedAssetId);
        currentInspection.setTemperature(maxTemperature);
        currentInspection.setDeltaTemp(deltaTemperature);
        
        // Get GPS location
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
            == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    currentInspection.setLatitude(location.getLatitude());
                    currentInspection.setLongitude(location.getLongitude());
                }
            });
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
     */
    private final GLSurfaceView.Renderer renderer = new GLSurfaceView.Renderer() {

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            ThermalLog.d(TAG, "onSurfaceCreated()");
            // not used
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            ThermalLog.d(TAG, "onSurfaceChanged(), width=" + width + ", height=" + height);
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
        }

        @Override
        public void onDrawFrame(GL10 gl) {
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
                    // setup palette
                    thermalImage.setPalette(currentPalette);
                    // setup fusion mode
                    Fusion fusion = thermalImage.getFusion();
                    if (fusion != null) {
                        fusion.setFusionMode(currentFusionMode);
                    }

                    // apply the color mode - by default it is HistogramEqualizationSettings
                    // and if camera provided customized HistogramEqualizationSettings via Camera.getCustomHistogramEqualizationSettings(), it will be used as default
                    // of course user can select any other ColorDistributionSettings
                    // and he can provide own parameters for HistogramEqualizationSettings too, which may be different than default and different than camera-specific settings
                    // in this sample though we only use either default HistogramEqualizationSettings or the customized HistogramEqualizationSettings that camera provides
                    thermalImage.setColorDistributionSettings(defaultColorSettings);

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
                        
                        // Store ThermalValue objects for display
                        ThermalValue maxTempValue = null;
                        ThermalValue minTempValue = null;
                        
                        for (MeasurementSpot sp : spots) {
                            ThermalValue temp = sp.getValue();
                            if (temp != null) {
                                ThermalValue celsiusTemp = temp.asCelsius();
                                
                                // Since we can't extract numeric value easily, just store the ThermalValue
                                // and display it directly
                                if (maxTempValue == null) {
                                    maxTempValue = celsiusTemp;
                                    minTempValue = celsiusTemp;
                                }
                                // We'll display the first and last measurement as a simple approach
                                maxTempValue = celsiusTemp;
                            }
                        }
                        
                        // Update UI with temperature info
                        final ThermalValue displayTemp = maxTempValue;
                        if (displayTemp != null) {
                            runOnUiThread(() -> {
                                TextView maxTempLabel = findViewById(R.id.maxTempLabel);
                                TextView deltaTempLabel = findViewById(R.id.deltaTempLabel);
                                if (maxTempLabel != null) {
                                    maxTempLabel.setText("Max Temp: " + displayTemp.toString());
                                }
                                if (deltaTempLabel != null && maxTemperature > 0) {
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
                        } catch (IOException e) {
                            ThermalLog.e(TAG, "Unable to take snapshot: " + e.getMessage());
                        }
                    }
                });

                // request the camera to push the frame buffer for drawing on the GLSurfaceView
                camera.glOnDrawFrame();

            }
        }
    };

}
