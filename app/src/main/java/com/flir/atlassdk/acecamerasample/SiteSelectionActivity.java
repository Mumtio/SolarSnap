package com.flir.atlassdk.acecamerasample;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.flir.atlassdk.acecamerasample.models.SolarSite;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SiteSelectionActivity extends AppCompatActivity {
    private SolarSite currentSite;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_site_selection);

        // Mock current site
        currentSite = new SolarSite("NV-Solar-04", "NV Solar Farm 04", 1200);
        currentSite.setInspectedPanels(640);
        
        initializeViews();
        setupButtonListeners();
    }

    private void initializeViews() {
        // Set current time
        TextView timeLabel = findViewById(R.id.timeLabel);
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        timeLabel.setText("Time: " + sdf.format(new Date()));

        // Site info
        TextView siteNameLabel = findViewById(R.id.siteNameLabel);
        siteNameLabel.setText(currentSite.getSiteName());

        TextView progressPercentLabel = findViewById(R.id.progressPercentLabel);
        int progress = currentSite.getProgressPercentage();
        progressPercentLabel.setText("Progress today: " + progress + "%");

        ProgressBar siteProgressBar = findViewById(R.id.siteProgressBar);
        siteProgressBar.setProgress(progress);

        // Inspection progress
        TextView scannedCountLabel = findViewById(R.id.scannedCountLabel);
        scannedCountLabel.setText(String.valueOf(currentSite.getInspectedPanels()));

        TextView remainingCountLabel = findViewById(R.id.remainingCountLabel);
        int remaining = currentSite.getTotalPanels() - currentSite.getInspectedPanels();
        remainingCountLabel.setText(String.valueOf(remaining));

        // Fault counts with visual dots
        TextView criticalCountLabel = findViewById(R.id.criticalCountLabel);
        criticalCountLabel.setText("4 ●●●●");

        TextView warningCountLabel = findViewById(R.id.warningCountLabel);
        warningCountLabel.setText("11 ●●●●●●●●●●●");

        TextView healthyCountLabel = findViewById(R.id.healthyCountLabel);
        healthyCountLabel.setText("625");
    }

    private void setupButtonListeners() {
        // Settings icon
        findViewById(R.id.settingsIcon).setOnClickListener(v -> openSettings());
        
        findViewById(R.id.buttonStartInspection).setOnClickListener(v -> startInspection());
        findViewById(R.id.buttonContinueInspection).setOnClickListener(v -> continueInspection());
        findViewById(R.id.buttonQuickScan).setOnClickListener(v -> quickScan());
        findViewById(R.id.buttonChangeSite).setOnClickListener(v -> changeSite());
        findViewById(R.id.buttonOpenMap).setOnClickListener(v -> openMap());
        findViewById(R.id.buttonSiteMap).setOnClickListener(v -> openSiteMap());
        findViewById(R.id.buttonHistory).setOnClickListener(v -> openHistory());
        findViewById(R.id.buttonPendingUploads).setOnClickListener(v -> openPendingUploads());
        findViewById(R.id.buttonReports).setOnClickListener(v -> generateReport());
    }

    private void startInspection() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("siteId", currentSite.getSiteId());
        intent.putExtra("siteName", currentSite.getSiteName());
        intent.putExtra("mode", "new");
        startActivity(intent);
    }

    private void continueInspection() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("siteId", currentSite.getSiteId());
        intent.putExtra("siteName", currentSite.getSiteName());
        intent.putExtra("mode", "continue");
        startActivity(intent);
    }

    private void quickScan() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("siteId", currentSite.getSiteId());
        intent.putExtra("siteName", currentSite.getSiteName());
        intent.putExtra("mode", "quick");
        startActivity(intent);
    }

    private void changeSite() {
        // Show site selection dialog with available sites
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Select Solar Site");
        
        // Mock list of available sites
        final String[] sites = {
            "NV Solar Farm 01",
            "NV Solar Farm 02", 
            "NV Solar Farm 03",
            "NV Solar Farm 04 (Current)",
            "CA Solar Farm 01",
            "AZ Solar Farm 01"
        };
        
        builder.setItems(sites, (dialog, which) -> {
            String selectedSite = sites[which];
            if (!selectedSite.contains("(Current)")) {
                // Update current site
                String siteId = "NV-Solar-0" + (which + 1);
                currentSite = new SolarSite(siteId, selectedSite, 1200);
                currentSite.setInspectedPanels(0);
                
                // Refresh UI
                initializeViews();
                
                android.widget.Toast.makeText(this, "Site changed to: " + selectedSite, 
                    android.widget.Toast.LENGTH_SHORT).show();
            } else {
                android.widget.Toast.makeText(this, "Already on this site", 
                    android.widget.Toast.LENGTH_SHORT).show();
            }
        });
        
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        
        builder.show();
    }

    private void openMap() {
        Intent intent = new Intent(this, SiteMapActivity.class);
        startActivity(intent);
    }

    private void openSiteMap() {
        Intent intent = new Intent(this, SiteMapActivity.class);
        startActivity(intent);
    }

    private void openHistory() {
        Intent intent = new Intent(this, InspectionHistoryActivity.class);
        startActivity(intent);
    }

    private void openPendingUploads() {
        Intent intent = new Intent(this, UploadsActivity.class);
        startActivity(intent);
    }

    private void generateReport() {
        Intent intent = new Intent(this, ReportsActivity.class);
        startActivity(intent);
    }
    
    private void openSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }
}
