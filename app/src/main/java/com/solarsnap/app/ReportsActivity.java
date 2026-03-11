package com.solarsnap.app;

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
import com.solarsnap.app.repository.InspectionRepository;
import com.solarsnap.app.repository.SiteRepository;
import com.solarsnap.app.database.entities.InspectionEntity;
import com.solarsnap.app.database.entities.SiteEntity;
import com.solarsnap.app.network.ApiClient;
import com.solarsnap.app.network.SolarSnapApiService;
import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

public class ReportsActivity extends AppCompatActivity {
    
    private String currentFilter = "Today";
    private String currentSiteId = null;
    private LinearLayout faultTypesContainer;
    private LinearLayout tempDistributionContainer;
    private InspectionRepository inspectionRepository;
    private SiteRepository siteRepository;
    private SolarSnapApiService apiService;
    private List<InspectionEntity> inspections;
    private List<SiteEntity> sites;
    
    // Filter buttons
    private Button filterToday;
    private Button filterLast7Days;
    private Button filterLast30Days;
    private Button filterCustom;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports);
        
        inspectionRepository = new InspectionRepository(this);
        siteRepository = new SiteRepository(this);
        apiService = ApiClient.getApiService(this);
        
        initializeViews();
        setupButtonListeners();
        loadSites();
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
    
    private void loadSites() {
        siteRepository.getSites(new SiteRepository.SitesCallback() {
            @Override
            public void onSuccess(List<SiteEntity> siteList) {
                sites = siteList;
                if (!sites.isEmpty() && currentSiteId == null) {
                    currentSiteId = sites.get(0).getSiteId();
                    runOnUiThread(() -> {
                        TextView siteLabel = findViewById(R.id.reportSiteLabel);
                        siteLabel.setText("Site: " + sites.get(0).getSiteName());
                    });
                }
            }
            
            @Override
            public void onError(String error) {
                Toast.makeText(ReportsActivity.this, "Error loading sites: " + error, 
                    Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void loadReportData() {
        // Load inspections from database
        inspectionRepository.getAllInspections(new InspectionRepository.InspectionsCallback() {
            @Override
            public void onSuccess(List<InspectionEntity> inspectionList) {
                inspections = inspectionList;
                runOnUiThread(() -> {
                    loadFaultTypes();
                    loadTemperatureDistribution();
                });
            }
            
            @Override
            public void onError(String error) {
                inspections = null;
                runOnUiThread(() -> {
                    Toast.makeText(ReportsActivity.this, "Error loading report data: " + error, 
                        Toast.LENGTH_SHORT).show();
                    // Load empty data
                    loadFaultTypes();
                    loadTemperatureDistribution();
                });
            }
        });
    }
    
    private void loadFaultTypes() {
        faultTypesContainer.removeAllViews();
        
        Map<String, Integer> faultTypes = new HashMap<>();
        
        if (inspections != null && !inspections.isEmpty()) {
            // Count fault types from real data
            for (InspectionEntity inspection : inspections) {
                if (inspection.getIssueType() != null && !inspection.getIssueType().equals("None")) {
                    faultTypes.put(inspection.getIssueType(), 
                        faultTypes.getOrDefault(inspection.getIssueType(), 0) + 1);
                }
            }
        }
        
        // If no data, show empty state
        if (faultTypes.isEmpty()) {
            TextView emptyView = new TextView(this);
            emptyView.setText("No faults found for selected period");
            emptyView.setTextColor(Color.parseColor("#C3C3C3"));
            emptyView.setTextSize(14);
            emptyView.setGravity(Gravity.CENTER);
            emptyView.setPadding(0, 20, 0, 20);
            faultTypesContainer.addView(emptyView);
            return;
        }
        
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
        
        if (inspections == null || inspections.isEmpty()) {
            TextView emptyView = new TextView(this);
            emptyView.setText("No temperature data available");
            emptyView.setTextColor(Color.parseColor("#C3C3C3"));
            emptyView.setTextSize(14);
            emptyView.setGravity(Gravity.CENTER);
            emptyView.setPadding(0, 20, 0, 20);
            tempDistributionContainer.addView(emptyView);
            return;
        }
        
        // Count temperature ranges from real data
        int range35_40 = 0, range40_45 = 0, range45_50 = 0, range50plus = 0;
        
        for (InspectionEntity inspection : inspections) {
            if (inspection.getTemperature() != 0.0) {
                double temp = inspection.getTemperature();
                if (temp >= 35 && temp < 40) {
                    range35_40++;
                } else if (temp >= 40 && temp < 45) {
                    range40_45++;
                } else if (temp >= 45 && temp < 50) {
                    range45_50++;
                } else if (temp >= 50) {
                    range50plus++;
                }
            }
        }
        
        // Add temperature distribution rows
        if (range35_40 > 0) addTempRangeRow("35–40°C", range35_40, Math.min(range35_40 / 10, 15));
        if (range40_45 > 0) addTempRangeRow("40–45°C", range40_45, Math.min(range40_45 / 10, 15));
        if (range45_50 > 0) addTempRangeRow("45–50°C", range45_50, Math.min(range45_50 / 10, 15));
        if (range50plus > 0) addTempRangeRow("50+°C", range50plus, Math.min(range50plus / 10, 15));
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
        if (sites == null || sites.isEmpty()) {
            Toast.makeText(this, "No sites available", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String[] siteNames = new String[sites.size()];
        for (int i = 0; i < sites.size(); i++) {
            siteNames[i] = sites.get(i).getSiteName();
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Site")
            .setItems(siteNames, (dialog, which) -> {
                currentSiteId = sites.get(which).getSiteId();
                TextView siteLabel = findViewById(R.id.reportSiteLabel);
                siteLabel.setText("Site: " + siteNames[which]);
                loadReportData();
                Toast.makeText(this, "Site changed to " + siteNames[which], 
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
        if (currentSiteId == null) {
            Toast.makeText(this, "Please select a site first", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Show progress dialog
        android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(this);
        progressDialog.setTitle("Generating " + reportType);
        progressDialog.setMessage("Please wait...");
        progressDialog.setCancelable(false);
        progressDialog.show();
        
        // Call backend API to generate report
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("site_id", currentSiteId);
        requestBody.addProperty("report_type", reportType.toLowerCase().replace(" ", "_"));
        requestBody.addProperty("date_range", currentFilter.toLowerCase().replace(" ", "_"));
        
        apiService.generateReport(requestBody).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                progressDialog.dismiss();
                
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject result = response.body();
                    if (result.has("success") && result.get("success").getAsBoolean()) {
                        String reportUrl = result.has("report_url") ? 
                            result.get("report_url").getAsString() : "Generated successfully";
                        
                        AlertDialog.Builder builder = new AlertDialog.Builder(ReportsActivity.this);
                        builder.setTitle(reportType + " Generated")
                            .setMessage("Report generated successfully!\n\n" +
                                "Report ID: " + result.get("report_id").getAsString() + "\n" +
                                "Status: Ready for download\n" +
                                "Size: " + result.get("file_size").getAsString())
                            .setPositiveButton("Download", (dialog, which) -> {
                                Toast.makeText(ReportsActivity.this, 
                                    "Downloading report...", Toast.LENGTH_SHORT).show();
                            })
                            .setNegativeButton("Close", null)
                            .show();
                    } else {
                        String error = result.has("message") ? 
                            result.get("message").getAsString() : "Unknown error";
                        Toast.makeText(ReportsActivity.this, 
                            "Failed to generate report: " + error, Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(ReportsActivity.this, 
                        "Server error: " + response.code(), Toast.LENGTH_LONG).show();
                }
            }
            
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                progressDialog.dismiss();
                Toast.makeText(ReportsActivity.this, 
                    "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
    
    private void exportData(String format) {
        if (currentSiteId == null) {
            Toast.makeText(this, "Please select a site first", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Toast.makeText(this, "Exporting data as " + format + "...", 
            Toast.LENGTH_SHORT).show();
        
        // Call backend API to export data
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("site_id", currentSiteId);
        requestBody.addProperty("format", format.toLowerCase());
        requestBody.addProperty("date_range", currentFilter.toLowerCase().replace(" ", "_"));
        
        apiService.exportData(requestBody).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject result = response.body();
                    if (result.has("success") && result.get("success").getAsBoolean()) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(ReportsActivity.this);
                        builder.setTitle("Export Complete")
                            .setMessage(format + " export completed!\n\n" +
                                "File: " + result.get("filename").getAsString() + "\n" +
                                "Size: " + result.get("file_size").getAsString() + "\n" +
                                "Records: " + result.get("record_count").getAsString())
                            .setPositiveButton("Download", (dialog, which) -> {
                                Toast.makeText(ReportsActivity.this, 
                                    "Downloading " + format + " file...", Toast.LENGTH_SHORT).show();
                            })
                            .setNegativeButton("Close", null)
                            .show();
                    } else {
                        String error = result.has("message") ? 
                            result.get("message").getAsString() : "Export failed";
                        Toast.makeText(ReportsActivity.this, error, Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(ReportsActivity.this, 
                        "Export failed: Server error", Toast.LENGTH_LONG).show();
                }
            }
            
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Toast.makeText(ReportsActivity.this, 
                    "Export failed: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
    
    private void sendToCloud() {
        if (currentSiteId == null) {
            Toast.makeText(this, "Please select a site first", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Get upload statistics first
        inspectionRepository.getStatistics(currentSiteId, new InspectionRepository.StatisticsCallback() {
            @Override
            public void onSuccess(int total, int critical, int warning, int healthy) {
                AlertDialog.Builder builder = new AlertDialog.Builder(ReportsActivity.this);
                builder.setTitle("Send to Cloud Dashboard")
                    .setMessage("Upload report data to cloud?\n\n" +
                        "Data to upload:\n" +
                        "• " + total + " inspection records\n" +
                        "• " + (critical + warning) + " fault reports\n" +
                        "• Thermal images\n" +
                        "• GPS coordinates\n\n" +
                        "Upload will begin immediately.")
                    .setPositiveButton("Upload", (dialog, which) -> {
                        // Call backend sync API
                        JsonObject requestBody = new JsonObject();
                        requestBody.addProperty("site_id", currentSiteId);
                        requestBody.addProperty("sync_type", "full");
                        
                        apiService.syncToCloud(requestBody).enqueue(new Callback<JsonObject>() {
                            @Override
                            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                                if (response.isSuccessful() && response.body() != null) {
                                    JsonObject result = response.body();
                                    if (result.has("success") && result.get("success").getAsBoolean()) {
                                        Toast.makeText(ReportsActivity.this, 
                                            "Upload to cloud completed!", Toast.LENGTH_SHORT).show();
                                    } else {
                                        String error = result.has("message") ? 
                                            result.get("message").getAsString() : "Upload failed";
                                        Toast.makeText(ReportsActivity.this, error, Toast.LENGTH_LONG).show();
                                    }
                                } else {
                                    Toast.makeText(ReportsActivity.this, 
                                        "Upload failed: Server error", Toast.LENGTH_LONG).show();
                                }
                            }
                            
                            @Override
                            public void onFailure(Call<JsonObject> call, Throwable t) {
                                Toast.makeText(ReportsActivity.this, 
                                    "Upload failed: " + t.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            }
            
            @Override
            public void onError(String error) {
                Toast.makeText(ReportsActivity.this, 
                    "Error getting statistics: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
