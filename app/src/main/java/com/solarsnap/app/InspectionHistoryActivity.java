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
            boolean matchesSearch = searchQuery.isEmpty() || 
                record.getPanelId().toLowerCase().contains(searchQuery);
            
            boolean matchesFilter = false;
            switch (currentFilter) {
                case "all":
                    matchesFilter = true;
                    break;
                case "faults":
                    matchesFilter = !record.getSeverity().equals("HEALTHY");
                    break;
                case "critical":
                    matchesFilter = record.getSeverity().equals("CRITICAL");
                    break;
                case "today":
                    // Check if inspection was done today
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    String today = sdf.format(new Date());
                    String inspectionDate = sdf.format(record.getTimestamp());
                    matchesFilter = today.equals(inspectionDate);
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
        
        int indicatorColor;
        switch (record.getSeverity()) {
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
        panelIdText.setText("Panel ID: " + record.getPanelId());
        panelIdText.setTextColor(Color.WHITE);
        panelIdText.setTextSize(16);
        panelIdText.setTypeface(null, android.graphics.Typeface.BOLD);
        contentLayout.addView(panelIdText);
        
        // Site ID
        TextView locationText = new TextView(this);
        locationText.setText("Site: " + record.getSiteId());
        locationText.setTextColor(Color.parseColor("#C3C3C3"));
        locationText.setTextSize(13);
        contentLayout.addView(locationText);
        
        // Status and Delta T
        LinearLayout statusLayout = new LinearLayout(this);
        statusLayout.setOrientation(LinearLayout.HORIZONTAL);
        
        TextView statusText = new TextView(this);
        statusText.setText("Status: " + record.getSeverity());
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
                Toast.makeText(this, "Exporting as " + exportOptions[which], 
                    Toast.LENGTH_SHORT).show();
            })
            .show();
    }
    
    private void showDeleteConfirmation() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Record")
            .setMessage("Are you sure you want to delete this inspection record?")
            .setPositiveButton("Delete", (dialog, which) -> {
                Toast.makeText(this, "Record deleted", Toast.LENGTH_SHORT).show();
                inspectionDetailsPanel.setVisibility(View.GONE);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
}
