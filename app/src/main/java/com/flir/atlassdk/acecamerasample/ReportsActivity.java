package com.flir.atlassdk.acecamerasample;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.util.HashMap;
import java.util.Map;

public class ReportsActivity extends AppCompatActivity {
    
    private String currentFilter = "Today";
    private LinearLayout faultTypesContainer;
    private LinearLayout tempDistributionContainer;
    
    // Filter buttons
    private Button filterToday;
    private Button filterLast7Days;
    private Button filterLast30Days;
    private Button filterCustom;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports);
        
        initializeViews();
        setupButtonListeners();
        loadReportData();
    }
    
    private void initializeViews() {
        faultTypesContainer = findViewById(R.id.faultTypesContainer);
        tempDistributionContainer = findViewById(R.id.tempDistributionContainer);
        
        filterToday = findViewById(R.id.filterToday);
        filterLast7Days = findViewById(R.id.filterLast7Days);
        filterLast30Days = findViewById(R.id.filterLast30Days);
        filterCustom = findViewById(R.id.filterCustom);
    }
    
    private void setupButtonListeners() {
        findViewById(R.id.backButton).setOnClickListener(v -> finish());
        
        findViewById(R.id.filterIcon).setOnClickListener(v -> showFilterDialog());
        
        findViewById(R.id.exportIcon).setOnClickListener(v -> showExportDialog());
        
        // Filter buttons
        filterToday.setOnClickListener(v -> applyFilter("Today"));
        filterLast7Days.setOnClickListener(v -> applyFilter("Last 7 Days"));
        filterLast30Days.setOnClickListener(v -> applyFilter("Last 30 Days"));
        filterCustom.setOnClickListener(v -> showCustomDatePicker());
        
        // Report generation buttons
        findViewById(R.id.buttonGenerateSiteReport).setOnClickListener(v -> 
            generateReport("Site Report"));
        findViewById(R.id.buttonGenerateFaultReport).setOnClickListener(v -> 
            generateReport("Fault Report"));
        findViewById(R.id.buttonGenerateMaintenanceReport).setOnClickListener(v -> 
            generateReport("Maintenance Report"));
        
        // Export buttons
        findViewById(R.id.buttonExportPDF).setOnClickListener(v -> exportData("PDF"));
        findViewById(R.id.buttonExportCSV).setOnClickListener(v -> exportData("CSV"));
        findViewById(R.id.buttonSendToCloud).setOnClickListener(v -> sendToCloud());
    }
    
    private void applyFilter(String filter) {
        currentFilter = filter;
        
        // Update button states
        resetFilterButtons();
        
        Button selectedButton = null;
        switch (filter) {
            case "Today":
                selectedButton = filterToday;
                break;
            case "Last 7 Days":
                selectedButton = filterLast7Days;
                break;
            case "Last 30 Days":
                selectedButton = filterLast30Days;
                break;
            case "Custom":
                selectedButton = filterCustom;
                break;
        }
        
        if (selectedButton != null) {
            selectedButton.setBackgroundColor(Color.WHITE);
            selectedButton.setTextColor(Color.BLACK);
        }
        
        // Update date label
        TextView dateLabel = findViewById(R.id.reportDateLabel);
        dateLabel.setText("Date Range: " + filter);
        
        // Reload data with new filter
        loadReportData();
    }
    
    private void resetFilterButtons() {
        int defaultBg = Color.parseColor("#2A3F54");
        int defaultText = Color.WHITE;
        
        filterToday.setBackgroundColor(defaultBg);
        filterToday.setTextColor(defaultText);
        filterLast7Days.setBackgroundColor(defaultBg);
        filterLast7Days.setTextColor(defaultText);
        filterLast30Days.setBackgroundColor(defaultBg);
        filterLast30Days.setTextColor(defaultText);
        filterCustom.setBackgroundColor(defaultBg);
        filterCustom.setTextColor(defaultText);
    }
    
    private void loadReportData() {
        // Load fault types
        loadFaultTypes();
        
        // Load temperature distribution
        loadTemperatureDistribution();
    }
    
    private void loadFaultTypes() {
        faultTypesContainer.removeAllViews();
        
        // Mock fault type data
        Map<String, Integer> faultTypes = new HashMap<>();
        faultTypes.put("Hotspot", 3);
        faultTypes.put("Diode Failure", 1);
        faultTypes.put("Connection Fault", 2);
        faultTypes.put("Shading Issue", 5);
        faultTypes.put("Cell Crack", 4);
        
        for (Map.Entry<String, Integer> entry : faultTypes.entrySet()) {
            addFaultTypeRow(entry.getKey(), entry.getValue());
        }
    }
    
    private void addFaultTypeRow(String faultType, int count) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 0, 0, 16);
        
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        row.setLayoutParams(rowParams);
        
        // Fault type label
        TextView label = new TextView(this);
        label.setText(faultType);
        label.setTextColor(Color.parseColor("#C3C3C3"));
        label.setTextSize(14);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f
        );
        label.setLayoutParams(labelParams);
        row.addView(label);
        
        // Count
        TextView countText = new TextView(this);
        countText.setText(String.valueOf(count));
        countText.setTextColor(Color.WHITE);
        countText.setTextSize(16);
        countText.setTypeface(null, android.graphics.Typeface.BOLD);
        countText.setPadding(0, 0, 16, 0);
        row.addView(countText);
        
        // Visual bar
        TextView bar = new TextView(this);
        StringBuilder barBuilder = new StringBuilder();
        for (int i = 0; i < count; i++) {
            barBuilder.append("█");
        }
        bar.setText(barBuilder.toString());
        bar.setTextColor(Color.parseColor("#00D9FF"));
        bar.setTextSize(14);
        row.addView(bar);
        
        faultTypesContainer.addView(row);
    }
    
    private void loadTemperatureDistribution() {
        tempDistributionContainer.removeAllViews();
        
        // Mock temperature distribution data
        addTempRangeRow("35–40°C", 280, 12);
        addTempRangeRow("40–45°C", 310, 13);
        addTempRangeRow("45–50°C", 40, 3);
        addTempRangeRow("50+°C", 10, 1);
    }
    
    private void addTempRangeRow(String range, int count, int barLength) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 0, 0, 16);
        
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        row.setLayoutParams(rowParams);
        
        // Temperature range label
        TextView label = new TextView(this);
        label.setText(range);
        label.setTextColor(Color.parseColor("#C3C3C3"));
        label.setTextSize(14);
        label.setMinWidth(120);
        row.addView(label);
        
        // Count
        TextView countText = new TextView(this);
        countText.setText(count + " panels");
        countText.setTextColor(Color.WHITE);
        countText.setTextSize(14);
        countText.setPadding(16, 0, 16, 0);
        LinearLayout.LayoutParams countParams = new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f
        );
        countText.setLayoutParams(countParams);
        row.addView(countText);
        
        // Visual bar
        TextView bar = new TextView(this);
        StringBuilder barBuilder = new StringBuilder();
        for (int i = 0; i < barLength; i++) {
            barBuilder.append("█");
        }
        bar.setText(barBuilder.toString());
        
        // Color code based on temperature
        if (range.startsWith("50+")) {
            bar.setTextColor(Color.parseColor("#F44336")); // Red
        } else if (range.startsWith("45")) {
            bar.setTextColor(Color.parseColor("#FF9800")); // Orange
        } else {
            bar.setTextColor(Color.parseColor("#4CAF50")); // Green
        }
        bar.setTextSize(14);
        row.addView(bar);
        
        tempDistributionContainer.addView(row);
    }
    
    private void showFilterDialog() {
        String[] options = {"Site Selection", "Inspector Filter", "Date Range"};
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Filter Options")
            .setItems(options, (dialog, which) -> {
                switch (which) {
                    case 0:
                        showSiteSelection();
                        break;
                    case 1:
                        showInspectorFilter();
                        break;
                    case 2:
                        showCustomDatePicker();
                        break;
                }
            })
            .show();
    }
    
    private void showSiteSelection() {
        String[] sites = {"NV Solar Farm 04", "Desert Solar 2", "Arizona Plant"};
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Site")
            .setItems(sites, (dialog, which) -> {
                TextView siteLabel = findViewById(R.id.reportSiteLabel);
                siteLabel.setText("Site: " + sites[which]);
                loadReportData();
                Toast.makeText(this, "Site changed to " + sites[which], 
                    Toast.LENGTH_SHORT).show();
            })
            .show();
    }
    
    private void showInspectorFilter() {
        String[] inspectors = {"All", "Inspector_12", "Inspector_08", "Inspector_15"};
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Filter by Inspector")
            .setItems(inspectors, (dialog, which) -> {
                TextView inspectorLabel = findViewById(R.id.reportInspectorLabel);
                inspectorLabel.setText("Inspector: " + inspectors[which]);
                loadReportData();
                Toast.makeText(this, "Filtered by " + inspectors[which], 
                    Toast.LENGTH_SHORT).show();
            })
            .show();
    }
    
    private void showCustomDatePicker() {
        Toast.makeText(this, "Custom date picker would open here", 
            Toast.LENGTH_SHORT).show();
    }
    
    private void showExportDialog() {
        String[] options = {"Export PDF", "Export CSV", "Export JSON", "Send to Cloud"};
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Export Options")
            .setItems(options, (dialog, which) -> {
                switch (which) {
                    case 0:
                        exportData("PDF");
                        break;
                    case 1:
                        exportData("CSV");
                        break;
                    case 2:
                        exportData("JSON");
                        break;
                    case 3:
                        sendToCloud();
                        break;
                }
            })
            .show();
    }
    
    private void generateReport(String reportType) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Generate " + reportType)
            .setMessage("Generating " + reportType + " for:\n\n" +
                "Site: NV Solar Farm 04\n" +
                "Date Range: " + currentFilter + "\n" +
                "Panels: 640 inspected\n" +
                "Faults: 15 total\n\n" +
                "Report will include:\n" +
                "• Panel IDs and locations\n" +
                "• Fault types and severity\n" +
                "• Temperature data\n" +
                "• Thermal images\n" +
                "• GPS coordinates\n" +
                "• Inspector information")
            .setPositiveButton("Generate", (dialog, which) -> {
                Toast.makeText(this, reportType + " generated successfully", 
                    Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    private void exportData(String format) {
        Toast.makeText(this, "Exporting data as " + format + "...", 
            Toast.LENGTH_SHORT).show();
        
        // Simulate export process
        new android.os.Handler().postDelayed(() -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Export Complete")
                .setMessage(format + " file saved to:\n/storage/emulated/0/SolarSnap/Reports/\n\n" +
                    "File: report_" + System.currentTimeMillis() + "." + format.toLowerCase() + "\n" +
                    "Size: 2.4 MB\n" +
                    "Records: 640 panels")
                .setPositiveButton("Open", (dialog, which) -> {
                    Toast.makeText(this, "Opening " + format + " file...", 
                        Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Close", null)
                .show();
        }, 1500);
    }
    
    private void sendToCloud() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Send to Cloud Dashboard")
            .setMessage("Upload report data to cloud?\n\n" +
                "Data to upload:\n" +
                "• 640 inspection records\n" +
                "• 15 fault reports\n" +
                "• Thermal images (38 MB)\n" +
                "• GPS coordinates\n\n" +
                "Estimated upload time: 2-3 minutes")
            .setPositiveButton("Upload", (dialog, which) -> {
                Toast.makeText(this, "Uploading to cloud...", Toast.LENGTH_SHORT).show();
                
                // Simulate upload
                new android.os.Handler().postDelayed(() -> {
                    Toast.makeText(this, "Upload complete!", Toast.LENGTH_SHORT).show();
                }, 2000);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
}
