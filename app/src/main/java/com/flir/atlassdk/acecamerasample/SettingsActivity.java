package com.flir.atlassdk.acecamerasample;

import android.content.Intent;
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

public class SettingsActivity extends AppCompatActivity {
    
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
        
        initializeViews();
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
                Toast.makeText(SettingsActivity.this, 
                    "Warning threshold set to " + seekBar.getProgress() + "°C", 
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
                Toast.makeText(SettingsActivity.this, 
                    "Critical threshold set to " + seekBar.getProgress() + "°C", 
                    Toast.LENGTH_SHORT).show();
            }
        });
        
        // Detection feature switches
        SwitchCompat switchHotspot = findViewById(R.id.switchHotspotDetection);
        switchHotspot.setOnCheckedChangeListener((buttonView, isChecked) -> 
            Toast.makeText(this, "Hotspot detection " + (isChecked ? "enabled" : "disabled"), 
                Toast.LENGTH_SHORT).show());
        
        SwitchCompat switchCellCrack = findViewById(R.id.switchCellCrackDetection);
        switchCellCrack.setOnCheckedChangeListener((buttonView, isChecked) -> 
            Toast.makeText(this, "Cell crack detection " + (isChecked ? "enabled" : "disabled"), 
                Toast.LENGTH_SHORT).show());
        
        SwitchCompat switchDiodeFault = findViewById(R.id.switchDiodeFaultDetection);
        switchDiodeFault.setOnCheckedChangeListener((buttonView, isChecked) -> 
            Toast.makeText(this, "Diode fault detection " + (isChecked ? "enabled" : "disabled"), 
                Toast.LENGTH_SHORT).show());
        
        // Palette buttons
        buttonPaletteIron.setOnClickListener(v -> selectPalette("Iron"));
        buttonPaletteRainbow.setOnClickListener(v -> selectPalette("Rainbow"));
        buttonPaletteGrayscale.setOnClickListener(v -> selectPalette("Grayscale"));
        buttonPaletteArctic.setOnClickListener(v -> selectPalette("Arctic"));
        
        // Calibrate button
        findViewById(R.id.buttonCalibrate).setOnClickListener(v -> calibrateCamera());
        
        // Inspection preference switches
        SwitchCompat switchAutoSave = findViewById(R.id.switchAutoSave);
        switchAutoSave.setOnCheckedChangeListener((buttonView, isChecked) -> 
            Toast.makeText(this, "Auto-save " + (isChecked ? "enabled" : "disabled"), 
                Toast.LENGTH_SHORT).show());
        
        SwitchCompat switchRequirePanelScan = findViewById(R.id.switchRequirePanelScan);
        switchRequirePanelScan.setOnCheckedChangeListener((buttonView, isChecked) -> 
            Toast.makeText(this, "Panel ID scan " + (isChecked ? "required" : "optional"), 
                Toast.LENGTH_SHORT).show());
        
        SwitchCompat switchQuickScanMode = findViewById(R.id.switchQuickScanMode);
        switchQuickScanMode.setOnCheckedChangeListener((buttonView, isChecked) -> 
            Toast.makeText(this, "Quick scan mode " + (isChecked ? "enabled" : "disabled"), 
                Toast.LENGTH_SHORT).show());
        
        // Inspection mode radio group
        RadioGroup radioGroupInspectionMode = findViewById(R.id.radioGroupInspectionMode);
        radioGroupInspectionMode.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioFullInspection) {
                Toast.makeText(this, "Default mode: Full Inspection", Toast.LENGTH_SHORT).show();
            } else if (checkedId == R.id.radioQuickScan) {
                Toast.makeText(this, "Default mode: Quick Scan", Toast.LENGTH_SHORT).show();
            }
        });
        
        // Connectivity switches
        SwitchCompat switchCloudSync = findViewById(R.id.switchCloudSync);
        switchCloudSync.setOnCheckedChangeListener((buttonView, isChecked) -> 
            Toast.makeText(this, "Cloud sync " + (isChecked ? "enabled" : "disabled"), 
                Toast.LENGTH_SHORT).show());
        
        SwitchCompat switchAutoUpload = findViewById(R.id.switchAutoUpload);
        switchAutoUpload.setOnCheckedChangeListener((buttonView, isChecked) -> 
            Toast.makeText(this, "Auto-upload " + (isChecked ? "enabled" : "disabled"), 
                Toast.LENGTH_SHORT).show());
        
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
                warningThresholdSeekBar.setProgress(8);
                criticalThresholdSeekBar.setProgress(15);
                selectPalette("Iron");
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
                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            })
            .setNegativeButton("Cancel", null)
            .show();
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
