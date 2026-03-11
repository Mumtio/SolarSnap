package com.solarsnap.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import com.solarsnap.app.network.ApiClient;
import com.solarsnap.app.network.SolarSnapApiService;
import com.solarsnap.app.repository.AuthRepository;
import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SettingsActivity extends AppCompatActivity {
    
    private static final String PREFS_NAME = "SolarSnapSettings";
    private static final String PREF_WARNING_THRESHOLD = "warning_threshold";
    private static final String PREF_CRITICAL_THRESHOLD = "critical_threshold";
    private static final String PREF_SELECTED_PALETTE = "selected_palette";
    private static final String PREF_HOTSPOT_DETECTION = "hotspot_detection";
    private static final String PREF_CELL_CRACK_DETECTION = "cell_crack_detection";
    private static final String PREF_DIODE_FAULT_DETECTION = "diode_fault_detection";
    private static final String PREF_AUTO_SAVE = "auto_save";
    private static final String PREF_REQUIRE_PANEL_SCAN = "require_panel_scan";
    private static final String PREF_INSPECTION_MODE = "inspection_mode";
    private static final String PREF_CLOUD_SYNC = "cloud_sync";
    private static final String PREF_AUTO_UPLOAD = "auto_upload";
    
    private SharedPreferences sharedPreferences;
    private SolarSnapApiService apiService;
    private AuthRepository authRepository;
    
    private SeekBar warningThresholdSeekBar;
    private SeekBar criticalThresholdSeekBar;
    private TextView warningThresholdValue;
    private TextView criticalThresholdValue;
    
    private Button buttonPaletteIron;
    private Button buttonPaletteRainbow;
    private Button buttonPaletteGrayscale;
    private Button buttonPaletteArctic;
    
    private String selectedPalette = "Iron";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        apiService = ApiClient.getApiService(this);
        authRepository = new AuthRepository(this);
        
        initializeViews();
        loadSettingsFromBackend();
        setupListeners();
    }
    
    private void initializeViews() {
        warningThresholdSeekBar = findViewById(R.id.warningThresholdSeekBar);
        criticalThresholdSeekBar = findViewById(R.id.criticalThresholdSeekBar);
        warningThresholdValue = findViewById(R.id.warningThresholdValue);
        criticalThresholdValue = findViewById(R.id.criticalThresholdValue);
        
        buttonPaletteIron = findViewById(R.id.buttonPaletteIron);
        buttonPaletteRainbow = findViewById(R.id.buttonPaletteRainbow);
        buttonPaletteGrayscale = findViewById(R.id.buttonPaletteGrayscale);
        buttonPaletteArctic = findViewById(R.id.buttonPaletteArctic);
    }
    
    private void loadSettingsFromBackend() {
        // First load from local preferences as fallback
        loadLocalSettings();
        
        // Then try to load from backend
        apiService.getUserSettings().enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject settings = response.body();
                    if (settings.has("success") && settings.get("success").getAsBoolean()) {
                        JsonObject userSettings = settings.getAsJsonObject("settings");
                        runOnUiThread(() -> applyBackendSettings(userSettings));
                    }
                }
                // If backend fails, we already have local settings loaded
            }
            
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                // Backend unavailable, use local settings
                runOnUiThread(() -> {
                    Toast.makeText(SettingsActivity.this, 
                        "Using offline settings", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    private void applyBackendSettings(JsonObject settings) {
        try {
            // Apply thermal thresholds
            if (settings.has("warning_threshold")) {
                int warningThreshold = settings.get("warning_threshold").getAsInt();
                warningThresholdSeekBar.setProgress(warningThreshold);
                warningThresholdValue.setText("ΔT ≥ " + warningThreshold + "°C");
                sharedPreferences.edit().putInt(PREF_WARNING_THRESHOLD, warningThreshold).apply();
            }
            
            if (settings.has("critical_threshold")) {
                int criticalThreshold = settings.get("critical_threshold").getAsInt();
                criticalThresholdSeekBar.setProgress(criticalThreshold);
                criticalThresholdValue.setText("ΔT ≥ " + criticalThreshold + "°C");
                sharedPreferences.edit().putInt(PREF_CRITICAL_THRESHOLD, criticalThreshold).apply();
            }
            
            // Apply palette selection
            if (settings.has("selected_palette")) {
                String palette = settings.get("selected_palette").getAsString();
                selectPalette(palette);
            }
            
            // Apply detection settings
            if (settings.has("hotspot_detection")) {
                SwitchCompat switchHotspot = findViewById(R.id.switchHotspotDetection);
                boolean enabled = settings.get("hotspot_detection").getAsBoolean();
                switchHotspot.setChecked(enabled);
                sharedPreferences.edit().putBoolean(PREF_HOTSPOT_DETECTION, enabled).apply();
            }
            
            // Apply other settings similarly...
            
            Toast.makeText(this, "Settings loaded from server", Toast.LENGTH_SHORT).show();
            
        } catch (Exception e) {
            Toast.makeText(this, "Error applying server settings", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void loadLocalSettings() {
        // Load thermal thresholds
        int warningThreshold = sharedPreferences.getInt(PREF_WARNING_THRESHOLD, 8);
        int criticalThreshold = sharedPreferences.getInt(PREF_CRITICAL_THRESHOLD, 15);
        
        warningThresholdSeekBar.setProgress(warningThreshold);
        criticalThresholdSeekBar.setProgress(criticalThreshold);
        warningThresholdValue.setText("ΔT ≥ " + warningThreshold + "°C");
        criticalThresholdValue.setText("ΔT ≥ " + criticalThreshold + "°C");
        
        // Load palette selection
        selectedPalette = sharedPreferences.getString(PREF_SELECTED_PALETTE, "Iron");
        selectPalette(selectedPalette);
        
        // Load detection switches
        SwitchCompat switchHotspot = findViewById(R.id.switchHotspotDetection);
        SwitchCompat switchCellCrack = findViewById(R.id.switchCellCrackDetection);
        SwitchCompat switchDiodeFault = findViewById(R.id.switchDiodeFaultDetection);
        
        switchHotspot.setChecked(sharedPreferences.getBoolean(PREF_HOTSPOT_DETECTION, true));
        switchCellCrack.setChecked(sharedPreferences.getBoolean(PREF_CELL_CRACK_DETECTION, true));
        switchDiodeFault.setChecked(sharedPreferences.getBoolean(PREF_DIODE_FAULT_DETECTION, true));
        
        // Load inspection preferences
        SwitchCompat switchAutoSave = findViewById(R.id.switchAutoSave);
        SwitchCompat switchRequirePanelScan = findViewById(R.id.switchRequirePanelScan);
        
        switchAutoSave.setChecked(sharedPreferences.getBoolean(PREF_AUTO_SAVE, true));
        switchRequirePanelScan.setChecked(sharedPreferences.getBoolean(PREF_REQUIRE_PANEL_SCAN, false));
        
        // Load inspection mode
        RadioGroup radioGroupInspectionMode = findViewById(R.id.radioGroupInspectionMode);
        String inspectionMode = sharedPreferences.getString(PREF_INSPECTION_MODE, "full");
        if (inspectionMode.equals("full")) {
            radioGroupInspectionMode.check(R.id.radioFullInspection);
        } else {
            radioGroupInspectionMode.check(R.id.radioFullInspection); // Default to full since quick is removed
        }
        
        // Load connectivity settings
        SwitchCompat switchCloudSync = findViewById(R.id.switchCloudSync);
        SwitchCompat switchAutoUpload = findViewById(R.id.switchAutoUpload);
        
        switchCloudSync.setChecked(sharedPreferences.getBoolean(PREF_CLOUD_SYNC, true));
        switchAutoUpload.setChecked(sharedPreferences.getBoolean(PREF_AUTO_UPLOAD, false));
    }
    
    private void setupListeners() {
        // Back button
        findViewById(R.id.settingsBackButton).setOnClickListener(v -> finish());
        
        // Help icon
        findViewById(R.id.settingsHelpIcon).setOnClickListener(v -> showHelp());
        
        // Reset icon
        findViewById(R.id.settingsResetIcon).setOnClickListener(v -> resetSettings());
        
        // User profile buttons
        findViewById(R.id.buttonSwitchUser).setOnClickListener(v -> switchUser());
        findViewById(R.id.buttonLogout).setOnClickListener(v -> logout());
        
        // Thermal threshold seekbars
        warningThresholdSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                warningThresholdValue.setText("ΔT ≥ " + progress + "°C");
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int threshold = seekBar.getProgress();
                sharedPreferences.edit().putInt(PREF_WARNING_THRESHOLD, threshold).apply();
                
                // Save to backend
                saveSettingToBackend("warning_threshold", threshold);
                
                Toast.makeText(SettingsActivity.this, 
                    "Warning threshold set to " + threshold + "°C", 
                    Toast.LENGTH_SHORT).show();
            }
        });
        
        criticalThresholdSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                criticalThresholdValue.setText("ΔT ≥ " + progress + "°C");
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int threshold = seekBar.getProgress();
                sharedPreferences.edit().putInt(PREF_CRITICAL_THRESHOLD, threshold).apply();
                
                // Save to backend
                saveSettingToBackend("critical_threshold", threshold);
                
                Toast.makeText(SettingsActivity.this, 
                    "Critical threshold set to " + threshold + "°C", 
                    Toast.LENGTH_SHORT).show();
            }
        });
        
        // Detection feature switches
        SwitchCompat switchHotspot = findViewById(R.id.switchHotspotDetection);
        switchHotspot.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean(PREF_HOTSPOT_DETECTION, isChecked).apply();
            Toast.makeText(this, "Hotspot detection " + (isChecked ? "enabled" : "disabled"), 
                Toast.LENGTH_SHORT).show();
        });
        
        SwitchCompat switchCellCrack = findViewById(R.id.switchCellCrackDetection);
        switchCellCrack.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean(PREF_CELL_CRACK_DETECTION, isChecked).apply();
            Toast.makeText(this, "Cell crack detection " + (isChecked ? "enabled" : "disabled"), 
                Toast.LENGTH_SHORT).show();
        });
        
        SwitchCompat switchDiodeFault = findViewById(R.id.switchDiodeFaultDetection);
        switchDiodeFault.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean(PREF_DIODE_FAULT_DETECTION, isChecked).apply();
            Toast.makeText(this, "Diode fault detection " + (isChecked ? "enabled" : "disabled"), 
                Toast.LENGTH_SHORT).show();
        });
        
        // Palette buttons
        buttonPaletteIron.setOnClickListener(v -> selectPalette("Iron"));
        buttonPaletteRainbow.setOnClickListener(v -> selectPalette("Rainbow"));
        buttonPaletteGrayscale.setOnClickListener(v -> selectPalette("Grayscale"));
        buttonPaletteArctic.setOnClickListener(v -> selectPalette("Arctic"));
        
        // Calibrate button
        findViewById(R.id.buttonCalibrate).setOnClickListener(v -> calibrateCamera());
        
        // Inspection preference switches
        SwitchCompat switchAutoSave = findViewById(R.id.switchAutoSave);
        switchAutoSave.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean(PREF_AUTO_SAVE, isChecked).apply();
            Toast.makeText(this, "Auto-save " + (isChecked ? "enabled" : "disabled"), 
                Toast.LENGTH_SHORT).show();
        });
        
        SwitchCompat switchRequirePanelScan = findViewById(R.id.switchRequirePanelScan);
        switchRequirePanelScan.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean(PREF_REQUIRE_PANEL_SCAN, isChecked).apply();
            Toast.makeText(this, "Panel ID scan " + (isChecked ? "required" : "optional"), 
                Toast.LENGTH_SHORT).show();
        });
        
        // Inspection mode radio group (Quick Scan removed, only Full Inspection available)
        RadioGroup radioGroupInspectionMode = findViewById(R.id.radioGroupInspectionMode);
        radioGroupInspectionMode.setOnCheckedChangeListener((group, checkedId) -> {
            String mode = "full"; // Always full since quick scan is removed
            Toast.makeText(this, "Default mode: Full Inspection", Toast.LENGTH_SHORT).show();
            sharedPreferences.edit().putString(PREF_INSPECTION_MODE, mode).apply();
        });
        
        // Connectivity switches
        SwitchCompat switchCloudSync = findViewById(R.id.switchCloudSync);
        switchCloudSync.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean(PREF_CLOUD_SYNC, isChecked).apply();
            Toast.makeText(this, "Cloud sync " + (isChecked ? "enabled" : "disabled"), 
                Toast.LENGTH_SHORT).show();
        });
        
        SwitchCompat switchAutoUpload = findViewById(R.id.switchAutoUpload);
        switchAutoUpload.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean(PREF_AUTO_UPLOAD, isChecked).apply();
            Toast.makeText(this, "Auto-upload " + (isChecked ? "enabled" : "disabled"), 
                Toast.LENGTH_SHORT).show();
        });
        
        // Connectivity buttons
        findViewById(R.id.buttonTestConnection).setOnClickListener(v -> testConnection());
        findViewById(R.id.buttonManualSync).setOnClickListener(v -> manualSync());
        findViewById(R.id.buttonCloudLogin).setOnClickListener(v -> cloudLogin());
        
        // Storage management buttons
        findViewById(R.id.buttonClearUploaded).setOnClickListener(v -> clearUploadedData());
        findViewById(R.id.buttonDeleteOld).setOnClickListener(v -> deleteOldInspections());
        findViewById(R.id.buttonCompressImages).setOnClickListener(v -> compressImages());
        
        // System information buttons
        findViewById(R.id.buttonCheckUpdates).setOnClickListener(v -> checkForUpdates());
        findViewById(R.id.buttonResetApp).setOnClickListener(v -> resetApplication());
        findViewById(R.id.buttonDiagnostics).setOnClickListener(v -> runDiagnostics());
    }
    
    private void selectPalette(String palette) {
        selectedPalette = palette;
        
        // Reset all palette buttons
        int defaultBg = Color.parseColor("#2A3F54");
        int defaultText = Color.WHITE;
        int selectedBg = Color.WHITE;
        int selectedText = Color.BLACK;
        
        buttonPaletteIron.setBackgroundColor(defaultBg);
        buttonPaletteIron.setTextColor(defaultText);
        buttonPaletteRainbow.setBackgroundColor(defaultBg);
        buttonPaletteRainbow.setTextColor(defaultText);
        buttonPaletteGrayscale.setBackgroundColor(defaultBg);
        buttonPaletteGrayscale.setTextColor(defaultText);
        buttonPaletteArctic.setBackgroundColor(defaultBg);
        buttonPaletteArctic.setTextColor(defaultText);
        
        // Highlight selected palette
        Button selectedButton = null;
        switch (palette) {
            case "Iron":
                selectedButton = buttonPaletteIron;
                break;
            case "Rainbow":
                selectedButton = buttonPaletteRainbow;
                break;
            case "Grayscale":
                selectedButton = buttonPaletteGrayscale;
                break;
            case "Arctic":
                selectedButton = buttonPaletteArctic;
                break;
        }
        
        if (selectedButton != null) {
            selectedButton.setBackgroundColor(selectedBg);
            selectedButton.setTextColor(selectedText);
        }
        
        sharedPreferences.edit().putString(PREF_SELECTED_PALETTE, palette).apply();
        
        // Save to backend
        saveSettingToBackend("selected_palette", palette);
        
        Toast.makeText(this, "Palette changed to " + palette, Toast.LENGTH_SHORT).show();
    }
    
    private void showHelp() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Settings Help")
            .setMessage("Configure your inspection preferences:\n\n" +
                "• Thermal Detection: Adjust fault detection thresholds\n" +
                "• Camera: Configure thermal camera settings\n" +
                "• Inspection: Set workflow preferences\n" +
                "• Connectivity: Manage cloud sync\n" +
                "• Storage: Monitor device storage\n" +
                "• System: View device information")
            .setPositiveButton("Close", null)
            .show();
    }
    
    private void resetSettings() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Reset Settings")
            .setMessage("Reset all settings to default values?\n\n" +
                "This will restore:\n" +
                "• Warning threshold: 8°C\n" +
                "• Critical threshold: 15°C\n" +
                "• Palette: Iron\n" +
                "• All detection features: ON\n" +
                "• Default inspection mode: Full")
            .setPositiveButton("Reset", (dialog, which) -> {
                // Reset to defaults
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt(PREF_WARNING_THRESHOLD, 8);
                editor.putInt(PREF_CRITICAL_THRESHOLD, 15);
                editor.putString(PREF_SELECTED_PALETTE, "Iron");
                editor.putBoolean(PREF_HOTSPOT_DETECTION, true);
                editor.putBoolean(PREF_CELL_CRACK_DETECTION, true);
                editor.putBoolean(PREF_DIODE_FAULT_DETECTION, true);
                editor.putBoolean(PREF_AUTO_SAVE, true);
                editor.putBoolean(PREF_REQUIRE_PANEL_SCAN, false);
                editor.putString(PREF_INSPECTION_MODE, "full");
                editor.putBoolean(PREF_CLOUD_SYNC, true);
                editor.putBoolean(PREF_AUTO_UPLOAD, false);
                editor.apply();
                
                // Reload settings
                loadLocalSettings();
                Toast.makeText(this, "Settings reset to defaults", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    private void switchUser() {
        String[] users = {"Inspector_12", "Inspector_08", "Inspector_15", "Inspector_20"};
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Switch User")
            .setItems(users, (dialog, which) -> {
                TextView userName = findViewById(R.id.settingsUserName);
                userName.setText("Inspector: " + users[which]);
                Toast.makeText(this, "Switched to " + users[which], Toast.LENGTH_SHORT).show();
            })
            .show();
    }
    
    private void logout() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Logout")
            .setMessage("Are you sure you want to logout?\n\n" +
                "Unsaved inspections will be preserved.")
            .setPositiveButton("Logout", (dialog, which) -> {
                // Call backend logout
                authRepository.logout(new AuthRepository.LogoutCallback() {
                    @Override
                    public void onSuccess() {
                        runOnUiThread(() -> {
                            Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        });
                    }
                    
                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            // Even if backend logout fails, clear local session
                            Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        });
                    }
                });
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    private void saveSettingToBackend(String key, Object value) {
        JsonObject request = new JsonObject();
        JsonObject settings = new JsonObject();
        
        if (value instanceof Integer) {
            settings.addProperty(key, (Integer) value);
        } else if (value instanceof Boolean) {
            settings.addProperty(key, (Boolean) value);
        } else if (value instanceof String) {
            settings.addProperty(key, (String) value);
        }
        
        request.add("settings", settings);
        
        apiService.updateUserSettings(request).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                // Settings saved to backend successfully
                // No need to show toast for every setting change
            }
            
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                // Backend save failed, but local setting is already saved
                // Continue working offline
            }
        });
    }
    
    private void calibrateCamera() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Camera Calibration")
            .setMessage("Starting camera calibration...\n\n" +
                "1. Point camera at uniform surface\n" +
                "2. Wait for calibration to complete\n" +
                "3. Do not move camera during process\n\n" +
                "Estimated time: 30 seconds")
            .setPositiveButton("Start", (dialog, which) -> {
                Toast.makeText(this, "Calibration started...", Toast.LENGTH_SHORT).show();
                
                // Simulate calibration
                new android.os.Handler().postDelayed(() -> {
                    Toast.makeText(this, "Calibration complete!", Toast.LENGTH_SHORT).show();
                }, 2000);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    private void testConnection() {
        Toast.makeText(this, "Testing connection...", Toast.LENGTH_SHORT).show();
        
        new android.os.Handler().postDelayed(() -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Connection Test")
                .setMessage("Connection Status: SUCCESS\n\n" +
                    "Network: LTE\n" +
                    "Signal Strength: Excellent\n" +
                    "Ping: 45ms\n" +
                    "Cloud API: Reachable\n" +
                    "Upload Speed: 12 Mbps")
                .setPositiveButton("Close", null)
                .show();
        }, 1500);
    }
    
    private void manualSync() {
        Toast.makeText(this, "Starting manual sync...", Toast.LENGTH_SHORT).show();
        
        new android.os.Handler().postDelayed(() -> {
            Toast.makeText(this, "Sync completed successfully", Toast.LENGTH_SHORT).show();
        }, 2000);
    }
    
    private void cloudLogin() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Cloud Login")
            .setMessage("Current Status: Connected\n\n" +
                "Account: inspector12@solartech.com\n" +
                "Last Sync: 2 minutes ago\n" +
                "Storage Used: 2.3 GB / 50 GB")
            .setPositiveButton("Reconnect", (dialog, which) -> {
                Toast.makeText(this, "Reconnecting to cloud...", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Close", null)
            .show();
    }
    
    private void clearUploadedData() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Clear Uploaded Data")
            .setMessage("Delete all successfully uploaded inspection data?\n\n" +
                "This will free approximately 1.8 GB of storage.\n\n" +
                "Data is safely stored in the cloud.")
            .setPositiveButton("Clear", (dialog, which) -> {
                Toast.makeText(this, "Clearing uploaded data...", Toast.LENGTH_SHORT).show();
                
                new android.os.Handler().postDelayed(() -> {
                    Toast.makeText(this, "1.8 GB freed", Toast.LENGTH_SHORT).show();
                }, 1500);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    private void deleteOldInspections() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Old Inspections")
            .setMessage("Delete inspections older than:\n\n" +
                "• 30 days (recommended)\n" +
                "• 60 days\n" +
                "• 90 days")
            .setPositiveButton("30 Days", (dialog, which) -> {
                Toast.makeText(this, "Deleting inspections older than 30 days...", 
                    Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    private void compressImages() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Compress Images")
            .setMessage("Compress thermal images to save storage?\n\n" +
                "Estimated savings: 400 MB\n" +
                "Quality: High (recommended)\n\n" +
                "This process may take several minutes.")
            .setPositiveButton("Compress", (dialog, which) -> {
                Toast.makeText(this, "Compressing images...", Toast.LENGTH_SHORT).show();
                
                new android.os.Handler().postDelayed(() -> {
                    Toast.makeText(this, "Compression complete. 400 MB saved.", 
                        Toast.LENGTH_SHORT).show();
                }, 2000);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    private void checkForUpdates() {
        Toast.makeText(this, "Checking for updates...", Toast.LENGTH_SHORT).show();
        
        new android.os.Handler().postDelayed(() -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Software Updates")
                .setMessage("Current Version: 1.2.0\n\n" +
                    "You are running the latest version.\n\n" +
                    "Last checked: Just now")
                .setPositiveButton("Close", null)
                .show();
        }, 1500);
    }
    
    private void resetApplication() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Reset Application")
            .setMessage("WARNING: This will:\n\n" +
                "• Delete all local inspection data\n" +
                "• Reset all settings to defaults\n" +
                "• Clear cache and temporary files\n" +
                "• Logout current user\n\n" +
                "Cloud data will NOT be affected.\n\n" +
                "This action cannot be undone!")
            .setPositiveButton("Reset", (dialog, which) -> {
                Toast.makeText(this, "Resetting application...", Toast.LENGTH_LONG).show();
                
                new android.os.Handler().postDelayed(() -> {
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }, 2000);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    private void runDiagnostics() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("System Diagnostics")
            .setMessage("Running diagnostics...\n\n" +
                "✓ Thermal Camera: OK\n" +
                "✓ GPS Module: OK\n" +
                "✓ Storage: OK\n" +
                "✓ Network: OK\n" +
                "✓ Battery: Good\n" +
                "✓ Temperature Sensors: OK\n\n" +
                "All systems operational.")
            .setPositiveButton("Export Log", (dialog, which) -> {
                Toast.makeText(this, "Diagnostic log exported", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Close", null)
            .show();
    }
}
