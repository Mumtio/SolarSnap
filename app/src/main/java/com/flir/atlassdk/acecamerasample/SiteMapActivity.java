package com.flir.atlassdk.acecamerasample;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;

public class SiteMapActivity extends AppCompatActivity {
    
    private LinearLayout mapGridContainer;
    private LinearLayout panelDetailsPopup;
    private int totalRows = 15;
    private int panelsPerRow = 20;
    private float zoomLevel = 1.0f;
    private String currentFilter = "all"; // Filter state: "all", "faults", "warnings", "uninspected"
    
    // Panel status colors
    private static final int COLOR_HEALTHY = Color.parseColor("#4CAF50");
    private static final int COLOR_WARNING = Color.parseColor("#FF9800");
    private static final int COLOR_CRITICAL = Color.parseColor("#F44336");
    private static final int COLOR_NOT_INSPECTED = Color.parseColor("#757575");
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_site_map);
        
        mapGridContainer = findViewById(R.id.mapGridContainer);
        panelDetailsPopup = findViewById(R.id.panelDetailsPopup);
        
        initializeViews();
        generateSolarFarmMap();
        setupButtonListeners();
    }
    
    private void initializeViews() {
        TextView mapSiteNameLabel = findViewById(R.id.mapSiteNameLabel);
        mapSiteNameLabel.setText("NV Solar Farm 04");
        
        TextView mapInspectedLabel = findViewById(R.id.mapInspectedLabel);
        mapInspectedLabel.setText("Inspected: 640");
        
        TextView mapRemainingLabel = findViewById(R.id.mapRemainingLabel);
        mapRemainingLabel.setText("Remaining: 560");
    }
    
    private void generateSolarFarmMap() {
        // Clear existing views except the title
        if (mapGridContainer.getChildCount() > 1) {
            mapGridContainer.removeViews(1, mapGridContainer.getChildCount() - 1);
        }
        
        // Generate rows of panels
        for (int row = 1; row <= totalRows; row++) {
            LinearLayout rowLayout = new LinearLayout(this);
            rowLayout.setOrientation(LinearLayout.HORIZONTAL);
            rowLayout.setGravity(Gravity.CENTER_VERTICAL);
            
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            );
            rowParams.setMargins(0, 0, 0, (int)(8 * zoomLevel));
            rowLayout.setLayoutParams(rowParams);
            
            // Row label
            TextView rowLabel = new TextView(this);
            rowLabel.setText("Row " + row + " ");
            rowLabel.setTextColor(Color.parseColor("#00D9FF"));
            rowLabel.setTextSize(12 * zoomLevel);
            rowLabel.setPadding((int)(8 * zoomLevel), 0, (int)(12 * zoomLevel), 0);
            rowLayout.addView(rowLabel);
            
            // Generate panels for this row
            for (int panel = 1; panel <= panelsPerRow; panel++) {
                View panelDot = createPanelDot(row, panel);
                rowLayout.addView(panelDot);
            }
            
            mapGridContainer.addView(rowLayout);
        }
    }
    
    private View createPanelDot(int row, int panel) {
        View dot = new View(this);
        
        int size = (int)(24 * zoomLevel);
        int margin = (int)(6 * zoomLevel);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
        params.setMargins(margin, 0, margin, 0);
        dot.setLayoutParams(params);
        
        // Simulate panel status
        int color = getPanelStatus(row, panel);
        
        // Apply filter - hide panels that don't match current filter
        if (!shouldShowPanel(color)) {
            dot.setVisibility(View.INVISIBLE);
        } else {
            dot.setVisibility(View.VISIBLE);
        }
        
        dot.setBackgroundColor(color);
        
        // Make it circular
        dot.setBackground(getDrawable(android.R.drawable.ic_menu_mylocation));
        dot.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));
        
        // Click listener
        final int finalRow = row;
        final int finalPanel = panel;
        dot.setOnClickListener(v -> showPanelDetails(finalRow, finalPanel, color));
        
        return dot;
    }
    
    private int getPanelStatus(int row, int panel) {
        // Simulate different panel statuses
        int panelNumber = (row - 1) * panelsPerRow + panel;
        
        if (panelNumber > 640) {
            return COLOR_NOT_INSPECTED; // Not inspected yet
        } else if (panelNumber == 126 || panelNumber == 245) {
            return COLOR_CRITICAL; // Critical fault
        } else if (panelNumber % 50 == 0) {
            return COLOR_WARNING; // Warning
        } else {
            return COLOR_HEALTHY; // Healthy
        }
    }
    
    private boolean shouldShowPanel(int color) {
        switch (currentFilter) {
            case "faults":
                // Show critical and warning panels only
                return color == COLOR_CRITICAL || color == COLOR_WARNING;
            case "warnings":
                // Show warning panels only
                return color == COLOR_WARNING;
            case "uninspected":
                // Show uninspected panels only
                return color == COLOR_NOT_INSPECTED;
            case "all":
            default:
                // Show all panels
                return true;
        }
    }
    
    private void showPanelDetails(int row, int panel, int statusColor) {
        panelDetailsPopup.setVisibility(View.VISIBLE);
        
        // Generate panel ID
        String panelId = String.format("PNL-A7-%04d", (row - 1) * panelsPerRow + panel);
        
        TextView popupPanelIdLabel = findViewById(R.id.popupPanelIdLabel);
        TextView popupRowLabel = findViewById(R.id.popupRowLabel);
        TextView popupStringLabel = findViewById(R.id.popupStringLabel);
        TextView popupStatusLabel = findViewById(R.id.popupStatusLabel);
        TextView popupDeltaTempLabel = findViewById(R.id.popupDeltaTempLabel);
        TextView popupLastInspectionLabel = findViewById(R.id.popupLastInspectionLabel);
        
        popupPanelIdLabel.setText(panelId);
        popupRowLabel.setText(String.valueOf(row));
        popupStringLabel.setText(String.valueOf((panel - 1) / 10 + 1));
        
        // Set status based on color
        if (statusColor == COLOR_CRITICAL) {
            popupStatusLabel.setText("CRITICAL");
            popupStatusLabel.setTextColor(COLOR_CRITICAL);
            popupDeltaTempLabel.setText("+18.5°C");
            popupDeltaTempLabel.setTextColor(COLOR_CRITICAL);
            popupLastInspectionLabel.setText("Last inspection: 14:02");
        } else if (statusColor == COLOR_WARNING) {
            popupStatusLabel.setText("WARNING");
            popupStatusLabel.setTextColor(COLOR_WARNING);
            popupDeltaTempLabel.setText("+9.2°C");
            popupDeltaTempLabel.setTextColor(COLOR_WARNING);
            popupLastInspectionLabel.setText("Last inspection: 13:45");
        } else if (statusColor == COLOR_HEALTHY) {
            popupStatusLabel.setText("HEALTHY");
            popupStatusLabel.setTextColor(COLOR_HEALTHY);
            popupDeltaTempLabel.setText("+2.1°C");
            popupDeltaTempLabel.setTextColor(COLOR_HEALTHY);
            popupLastInspectionLabel.setText("Last inspection: 12:30");
        } else {
            popupStatusLabel.setText("NOT INSPECTED");
            popupStatusLabel.setTextColor(COLOR_NOT_INSPECTED);
            popupDeltaTempLabel.setText("--");
            popupDeltaTempLabel.setTextColor(COLOR_NOT_INSPECTED);
            popupLastInspectionLabel.setText("Not yet inspected");
        }
    }
    
    private void setupButtonListeners() {
        findViewById(R.id.buttonMapChangeSite).setOnClickListener(v -> 
            Toast.makeText(this, "Change site dialog", Toast.LENGTH_SHORT).show());
        
        findViewById(R.id.buttonFilterFaults).setOnClickListener(v -> showFilterDialog());
        
        findViewById(R.id.buttonViewImage).setOnClickListener(v -> 
            Toast.makeText(this, "Opening thermal image", Toast.LENGTH_SHORT).show());
        
        findViewById(R.id.buttonNavigateToPanel).setOnClickListener(v -> 
            Toast.makeText(this, "GPS navigation to panel", Toast.LENGTH_SHORT).show());
        
        findViewById(R.id.buttonReinspect).setOnClickListener(v -> {
            Toast.makeText(this, "Starting reinspection", Toast.LENGTH_SHORT).show();
            finish(); // Return to inspection screen
        });
        
        findViewById(R.id.buttonClosePopup).setOnClickListener(v -> 
            panelDetailsPopup.setVisibility(View.GONE));
        
        findViewById(R.id.buttonZoomIn).setOnClickListener(v -> {
            zoomLevel = Math.min(zoomLevel + 0.2f, 2.0f);
            regenerateMap();
        });
        
        findViewById(R.id.buttonZoomOut).setOnClickListener(v -> {
            zoomLevel = Math.max(zoomLevel - 0.2f, 0.5f);
            regenerateMap();
        });
        
        findViewById(R.id.buttonCenterGPS).setOnClickListener(v -> 
            Toast.makeText(this, "Centering on current GPS location", Toast.LENGTH_SHORT).show());
        
        findViewById(R.id.buttonNextPanel).setOnClickListener(v -> 
            Toast.makeText(this, "Navigating to next unscanned panel", Toast.LENGTH_SHORT).show());
    }
    
    private void showFilterDialog() {
        String[] filters = {
            "Show All Panels",
            "Show Faults Only",
            "Show Warnings",
            "Show Uninspected"
        };
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("FILTER PANELS")
            .setItems(filters, (dialog, which) -> {
                // Update filter state based on selection
                switch (which) {
                    case 0:
                        currentFilter = "all";
                        break;
                    case 1:
                        currentFilter = "faults";
                        break;
                    case 2:
                        currentFilter = "warnings";
                        break;
                    case 3:
                        currentFilter = "uninspected";
                        break;
                }
                
                // Regenerate map with new filter
                regenerateMap();
                Toast.makeText(this, "Filter: " + filters[which], Toast.LENGTH_SHORT).show();
            })
            .show();
    }
    
    private void regenerateMap() {
        generateSolarFarmMap();
        Toast.makeText(this, "Zoom: " + String.format("%.1f", zoomLevel) + "x", 
            Toast.LENGTH_SHORT).show();
    }
}
