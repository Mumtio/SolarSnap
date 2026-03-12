package com.solarsnap.app;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.solarsnap.app.repository.InspectionRepository;
import com.solarsnap.app.database.entities.InspectionEntity;
import com.solarsnap.app.network.ApiClient;
import com.solarsnap.app.network.SolarSnapApiService;
import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class InspectionHistoryActivity extends AppCompatActivity {
    
    private LinearLayout inspectionListContainer;
    private LinearLayout inspectionDetailsPanel;
    private EditText searchPanelInput;
    private List<InspectionEntity> allRecords;
    private List<InspectionEntity> filteredRecords;
    private String currentFilter = "all";
    private InspectionRepository inspectionRepository;
    private InspectionEntity selectedInspection;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inspection_history);
        
        inspectionListContainer = findViewById(R.id.inspectionListContainer);
        inspectionDetailsPanel = findViewById(R.id.inspectionDetailsPanel);
        searchPanelInput = findViewById(R.id.searchPanelInput);
        
        // Initialize repository
        inspectionRepository = new InspectionRepository(this);
        
        // Load real data from database
        loadInspectionHistory();
        setupSearchAndFilters();
        setupButtonListeners();
    }
    
    private void loadInspectionHistory() {
        inspectionRepository.getAllInspections(new InspectionRepository.InspectionsCallback() {
            @Override
            public void onSuccess(List<InspectionEntity> inspections) {
                runOnUiThread(() -> {
                    allRecords = new ArrayList<>(inspections);
                    filteredRecords = new ArrayList<>(inspections);
                    displayInspectionRecords();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(InspectionHistoryActivity.this, 
                        "Error loading inspections: " + error, Toast.LENGTH_LONG).show();
                    // Create empty lists to prevent crashes
                    allRecords = new ArrayList<>();
                    filteredRecords = new ArrayList<>();
                    displayInspectionRecords();
                });
            }
        });
    }
    
    private void setupSearchAndFilters() {
        // Search functionality
        searchPanelInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterRecords();
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        // Filter buttons
        findViewById(R.id.buttonFilterAll).setOnClickListener(v -> {
            currentFilter = "all";
            updateFilterButtons();
            filterRecords();
        });
        
        findViewById(R.id.buttonFilterFaults).setOnClickListener(v -> {
            currentFilter = "faults";
            updateFilterButtons();
            filterRecords();
        });
        
        findViewById(R.id.buttonFilterCritical).setOnClickListener(v -> {
            currentFilter = "critical";
            updateFilterButtons();
            filterRecords();
        });
        
        findViewById(R.id.buttonFilterToday).setOnClickListener(v -> {
            currentFilter = "today";
            updateFilterButtons();
            filterRecords();
        });
    }
    
    private void updateFilterButtons() {
        int accentColor = Color.parseColor("#00D9FF");
        int whiteColor = Color.WHITE;
        int blackColor = Color.BLACK;
        
        Button btnAll = findViewById(R.id.buttonFilterAll);
        Button btnFaults = findViewById(R.id.buttonFilterFaults);
        Button btnCritical = findViewById(R.id.buttonFilterCritical);
        Button btnToday = findViewById(R.id.buttonFilterToday);
        
        // Reset all buttons
        btnAll.setBackgroundTintList(android.content.res.ColorStateList.valueOf(whiteColor));
        btnAll.setTextColor(blackColor);
        btnFaults.setBackgroundTintList(android.content.res.ColorStateList.valueOf(whiteColor));
        btnFaults.setTextColor(blackColor);
        btnCritical.setBackgroundTintList(android.content.res.ColorStateList.valueOf(whiteColor));
        btnCritical.setTextColor(blackColor);
        btnToday.setBackgroundTintList(android.content.res.ColorStateList.valueOf(whiteColor));
        btnToday.setTextColor(blackColor);
        
        // Highlight active filter
        switch (currentFilter) {
            case "all":
                btnAll.setBackgroundTintList(android.content.res.ColorStateList.valueOf(accentColor));
                btnAll.setTextColor(blackColor);
                break;
            case "faults":
                btnFaults.setBackgroundTintList(android.content.res.ColorStateList.valueOf(accentColor));
                btnFaults.setTextColor(blackColor);
                break;
            case "critical":
                btnCritical.setBackgroundTintList(android.content.res.ColorStateList.valueOf(accentColor));
                btnCritical.setTextColor(blackColor);
                break;
            case "today":
                btnToday.setBackgroundTintList(android.content.res.ColorStateList.valueOf(accentColor));
                btnToday.setTextColor(blackColor);
                break;
        }
    }
    
    private void filterRecords() {
        String searchQuery = searchPanelInput.getText().toString().toLowerCase();
        filteredRecords.clear();
        
        for (InspectionEntity record : allRecords) {
            // Safe null checks
            String panelId = record.getPanelId() != null ? record.getPanelId() : "";
            boolean matchesSearch = searchQuery.isEmpty() || 
                panelId.toLowerCase().contains(searchQuery);
            
            boolean matchesFilter = false;
            String severity = record.getSeverity() != null ? record.getSeverity() : "HEALTHY";
            
            switch (currentFilter) {
                case "all":
                    matchesFilter = true;
                    break;
                case "faults":
                    matchesFilter = !severity.equals("HEALTHY");
                    break;
                case "critical":
                    matchesFilter = severity.equals("CRITICAL");
                    break;
                case "today":
                    // Check if inspection was done today
                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                        String today = sdf.format(new Date());
                        String inspectionDate = sdf.format(record.getTimestamp());
                        matchesFilter = today.equals(inspectionDate);
                    } catch (Exception e) {
                        matchesFilter = false; // Skip if date parsing fails
                    }
                    break;
            }
            
            if (matchesSearch && matchesFilter) {
                filteredRecords.add(record);
            }
        }
        
        displayInspectionRecords();
    }
    
    private void displayInspectionRecords() {
        inspectionListContainer.removeAllViews();
        
        if (filteredRecords.isEmpty()) {
            TextView emptyView = new TextView(this);
            emptyView.setText("No records found");
            emptyView.setTextColor(Color.parseColor("#C3C3C3"));
            emptyView.setTextSize(16);
            emptyView.setGravity(Gravity.CENTER);
            emptyView.setPadding(0, 40, 0, 40);
            inspectionListContainer.addView(emptyView);
            return;
        }
        
        for (InspectionEntity record : filteredRecords) {
            View recordCard = createRecordCard(record);
            inspectionListContainer.addView(recordCard);
        }
    }
    
    private View createRecordCard(InspectionEntity record) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setBackgroundColor(Color.parseColor("#1A2B3D"));
        
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, 16);
        card.setLayoutParams(cardParams);
        card.setPadding(0, 0, 0, 0);
        
        // Color indicator on the left
        View colorIndicator = new View(this);
        LinearLayout.LayoutParams indicatorParams = new LinearLayout.LayoutParams(8, ViewGroup.LayoutParams.MATCH_PARENT);
        colorIndicator.setLayoutParams(indicatorParams);
        
        // Safe severity check with null handling
        String severity = record.getSeverity();
        if (severity == null) {
            severity = "HEALTHY"; // Default fallback
        }
        
        int indicatorColor;
        switch (severity.toUpperCase()) {
            case "CRITICAL":
                indicatorColor = Color.parseColor("#F44336");
                break;
            case "WARNING":
                indicatorColor = Color.parseColor("#FF9800");
                break;
            default:
                indicatorColor = Color.parseColor("#4CAF50");
                break;
        }
        colorIndicator.setBackgroundColor(indicatorColor);
        card.addView(colorIndicator);
        
        // Content area
        LinearLayout contentLayout = new LinearLayout(this);
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        contentLayout.setPadding(16, 16, 16, 16);
        LinearLayout.LayoutParams contentParams = new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f
        );
        contentLayout.setLayoutParams(contentParams);
        
        // Panel ID
        TextView panelIdText = new TextView(this);
        String panelId = record.getPanelId() != null ? record.getPanelId() : "Unknown";
        panelIdText.setText("Panel ID: " + panelId);
        panelIdText.setTextColor(Color.WHITE);
        panelIdText.setTextSize(16);
        panelIdText.setTypeface(null, android.graphics.Typeface.BOLD);
        contentLayout.addView(panelIdText);
        
        // Site ID
        TextView locationText = new TextView(this);
        String siteId = record.getSiteId() != null ? record.getSiteId() : "Unknown";
        locationText.setText("Site: " + siteId);
        locationText.setTextColor(Color.parseColor("#C3C3C3"));
        locationText.setTextSize(13);
        contentLayout.addView(locationText);
        
        // Status and Delta T
        LinearLayout statusLayout = new LinearLayout(this);
        statusLayout.setOrientation(LinearLayout.HORIZONTAL);
        
        TextView statusText = new TextView(this);
        statusText.setText("Status: " + severity);
        statusText.setTextColor(indicatorColor);
        statusText.setTextSize(14);
        statusText.setTypeface(null, android.graphics.Typeface.BOLD);
        statusLayout.addView(statusText);
        
        TextView deltaTempText = new TextView(this);
        deltaTempText.setText("  ΔT: +" + String.format("%.1f", record.getDeltaTemp()) + "°C");
        deltaTempText.setTextColor(indicatorColor);
        deltaTempText.setTextSize(14);
        statusLayout.addView(deltaTempText);
        
        contentLayout.addView(statusLayout);
        
        // Time and Temperature
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d HH:mm", Locale.getDefault());
        String formattedTime = sdf.format(record.getTimestamp());
        TextView timeText = new TextView(this);
        timeText.setText("Time: " + formattedTime + "  Temp: " + String.format("%.1f°C", record.getTemperature()));
        timeText.setTextColor(Color.parseColor("#C3C3C3"));
        timeText.setTextSize(12);
        contentLayout.addView(timeText);
        
        card.addView(contentLayout);
        
        // Click listener
        card.setOnClickListener(v -> showInspectionDetails(record));
        
        return card;
    }
    
    private void showInspectionDetails(InspectionEntity record) {
        selectedInspection = record; // Store the selected inspection
        inspectionDetailsPanel.setVisibility(View.VISIBLE);
        
        TextView detailPanelIdLabel = findViewById(R.id.detailPanelIdLabel);
        TextView detailFaultLabel = findViewById(R.id.detailFaultLabel);
        TextView detailDeltaTempLabel = findViewById(R.id.detailDeltaTempLabel);
        TextView detailTimestampLabel = findViewById(R.id.detailTimestampLabel);
        TextView detailInspectorLabel = findViewById(R.id.detailInspectorLabel);
        TextView detailGpsLabel = findViewById(R.id.detailGpsLabel);
        
        detailPanelIdLabel.setText(record.getPanelId());
        detailFaultLabel.setText(record.getIssueType());
        detailDeltaTempLabel.setText("+" + String.format("%.1f", record.getDeltaTemp()) + "°C");
        
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault());
        detailTimestampLabel.setText(sdf.format(record.getTimestamp()));
        
        detailInspectorLabel.setText("Inspector"); // Could be enhanced with actual inspector info
        
        String gpsText = "N/A";
        if (record.getLatitude() != 0.0 && record.getLongitude() != 0.0) {
            gpsText = String.format("%.4f, %.4f", record.getLatitude(), record.getLongitude());
        }
        detailGpsLabel.setText(gpsText);
        
        // Set colors based on status
        int statusColor;
        switch (record.getSeverity()) {
            case "CRITICAL":
                statusColor = Color.parseColor("#F44336");
                break;
            case "WARNING":
                statusColor = Color.parseColor("#FF9800");
                break;
            default:
                statusColor = Color.parseColor("#4CAF50");
                break;
        }
        
        detailFaultLabel.setTextColor(statusColor);
        detailDeltaTempLabel.setTextColor(statusColor);
    }
    
    private void setupButtonListeners() {
        findViewById(R.id.buttonViewFullImage).setOnClickListener(v -> 
            Toast.makeText(this, "Opening full thermal image", Toast.LENGTH_SHORT).show());
        
        findViewById(R.id.buttonOpenOnMap).setOnClickListener(v -> {
            Intent intent = new Intent(this, SiteMapActivity.class);
            startActivity(intent);
        });
        
        findViewById(R.id.buttonReinspectPanel).setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        });
        
        findViewById(R.id.buttonCloseDetails).setOnClickListener(v -> 
            inspectionDetailsPanel.setVisibility(View.GONE));
        
        findViewById(R.id.buttonExportRecord).setOnClickListener(v -> showExportDialog());
        
        findViewById(R.id.buttonGenerateReport).setOnClickListener(v -> {
            Intent intent = new Intent(this, ReportsActivity.class);
            startActivity(intent);
        });
        
        findViewById(R.id.buttonDeleteRecord).setOnClickListener(v -> showDeleteConfirmation());
        
        findViewById(R.id.historyFilterIcon).setOnClickListener(v -> 
            Toast.makeText(this, "Advanced filters", Toast.LENGTH_SHORT).show());
        
        findViewById(R.id.historyExportIcon).setOnClickListener(v -> showExportDialog());
    }
    
    private void showExportDialog() {
        String[] exportOptions = {"Export as PDF", "Export as CSV", "Export as JSON"};
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("EXPORT RECORD")
            .setItems(exportOptions, (dialog, which) -> {
                String format = "";
                switch (which) {
                    case 0: format = "pdf"; break;
                    case 1: format = "csv"; break;
                    case 2: format = "json"; break;
                }
                exportHistoryData(format);
            })
            .show();
    }
    
    private void exportHistoryData(String format) {
        Toast.makeText(this, "Exporting history as " + format.toUpperCase() + "...", 
            Toast.LENGTH_SHORT).show();
        
        // Prepare export request
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("format", format);
        requestBody.addProperty("severity", currentFilter);
        
        // Add search filter if active
        String searchQuery = searchPanelInput.getText().toString();
        if (!searchQuery.isEmpty()) {
            requestBody.addProperty("panelId", searchQuery);
        }
        
        // Call backend API
        SolarSnapApiService apiService = ApiClient.getApiService(this);
        apiService.exportHistory(requestBody).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject result = response.body();
                    if (result.has("success") && result.get("success").getAsBoolean()) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(InspectionHistoryActivity.this);
                        builder.setTitle("Export Complete")
                            .setMessage("History export completed!\n\n" +
                                "File: " + result.get("filename").getAsString() + "\n" +
                                "Format: " + format.toUpperCase() + "\n" +
                                "Records: " + result.get("recordCount").getAsString())
                            .setPositiveButton("Download", (dialog, which) -> {
                                Toast.makeText(InspectionHistoryActivity.this, 
                                    "Downloading " + format.toUpperCase() + " file...", Toast.LENGTH_SHORT).show();
                            })
                            .setNegativeButton("Close", null)
                            .show();
                    } else {
                        String error = result.has("error") && result.getAsJsonObject("error").has("message") ? 
                            result.getAsJsonObject("error").get("message").getAsString() : "Export failed";
                        Toast.makeText(InspectionHistoryActivity.this, error, Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(InspectionHistoryActivity.this, 
                        "Export failed: Server error", Toast.LENGTH_LONG).show();
                }
            }
            
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Toast.makeText(InspectionHistoryActivity.this, 
                    "Export failed: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
    
    private void showDeleteConfirmation() {
        if (selectedInspection == null) {
            Toast.makeText(this, "No inspection selected", Toast.LENGTH_SHORT).show();
            return;
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Record")
            .setMessage("Are you sure you want to delete this inspection record?\n\n" +
                "Panel ID: " + selectedInspection.getPanelId() + "\n" +
                "Site: " + selectedInspection.getSiteId() + "\n" +
                "Status: " + selectedInspection.getSeverity())
            .setPositiveButton("Delete", (dialog, which) -> {
                deleteInspectionRecord(selectedInspection);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    private void deleteInspectionRecord(InspectionEntity inspection) {
        Toast.makeText(this, "Deleting inspection record...", Toast.LENGTH_SHORT).show();
        
        // Prepare delete request
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("inspectionId", inspection.getInspectionUuid());
        
        // Call backend API
        SolarSnapApiService apiService = ApiClient.getApiService(this);
        apiService.deleteInspectionRecord(requestBody).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject result = response.body();
                    if (result.has("success") && result.get("success").getAsBoolean()) {
                        // Remove from local database
                        inspectionRepository.deleteInspection(inspection.getInspectionUuid(), 
                            new InspectionRepository.DeleteCallback() {
                                @Override
                                public void onSuccess() {
                                    runOnUiThread(() -> {
                                        Toast.makeText(InspectionHistoryActivity.this, 
                                            "Record deleted successfully", Toast.LENGTH_SHORT).show();
                                        
                                        // Remove from lists and refresh UI
                                        allRecords.remove(inspection);
                                        filteredRecords.remove(inspection);
                                        displayInspectionRecords();
                                        
                                        // Hide details panel
                                        inspectionDetailsPanel.setVisibility(View.GONE);
                                        selectedInspection = null;
                                    });
                                }
                                
                                @Override
                                public void onError(String error) {
                                    runOnUiThread(() -> {
                                        Toast.makeText(InspectionHistoryActivity.this, 
                                            "Local delete failed: " + error, Toast.LENGTH_LONG).show();
                                    });
                                }
                            });
                    } else {
                        String error = result.has("error") && result.getAsJsonObject("error").has("message") ? 
                            result.getAsJsonObject("error").get("message").getAsString() : "Delete failed";
                        Toast.makeText(InspectionHistoryActivity.this, error, Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(InspectionHistoryActivity.this, 
                        "Delete failed: Server error", Toast.LENGTH_LONG).show();
                }
            }
            
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Toast.makeText(InspectionHistoryActivity.this, 
                    "Delete failed: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
