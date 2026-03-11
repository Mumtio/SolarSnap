package com.solarsnap.app;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import com.solarsnap.app.repository.SiteRepository;
import com.solarsnap.app.repository.InspectionRepository;
import com.solarsnap.app.database.entities.PanelEntity;
import com.solarsnap.app.database.entities.InspectionEntity;
import java.util.List;
import java.util.ArrayList;

public class SiteMapActivity extends AppCompatActivity {
    
    private LinearLayout mapGridContainer;
    private LinearLayout panelDetailsPopup;
    private SiteRepository siteRepository;
    private List<PanelEntity> panels;
    private InspectionEntity currentInspection; // Store current inspection for image viewing
    private PanelEntity selectedPanel; // Store currently selected panel for reinspection
    private String currentSiteId; // Get from intent
    private String currentSiteName;
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
        
        siteRepository = new SiteRepository(this);
        
        // Get site ID from intent
        currentSiteId = getIntent().getStringExtra("siteId");
        currentSiteName = getIntent().getStringExtra("siteName");
        boolean shouldRefresh = getIntent().getBooleanExtra("refreshData", false);
        
        if (currentSiteId == null) {
            // No site passed, load the first available site
            Log.d("SiteMapActivity", "No site passed in intent, will load first available site");
            loadFirstAvailableSite();
        } else {
            Log.d("SiteMapActivity", "Site passed in intent: " + currentSiteId + " (" + currentSiteName + ")");
            mapGridContainer = findViewById(R.id.mapGridContainer);
            panelDetailsPopup = findViewById(R.id.panelDetailsPopup);
            
            initializeViews();
            loadPanelData(shouldRefresh); // Use refresh flag from intent
            setupButtonListeners();
            isInitialized = true; // Mark as fully initialized
        }
    }
    
    private boolean isInitialized = false;
    
    private void initializeViews() {
        TextView mapSiteNameLabel = findViewById(R.id.mapSiteNameLabel);
        mapSiteNameLabel.setText(currentSiteName != null ? currentSiteName : "Loading...");
        
        // Add click listener to site name for site selection
        mapSiteNameLabel.setOnClickListener(v -> showSiteSelectionDialog());
        
        TextView mapInspectedLabel = findViewById(R.id.mapInspectedLabel);
        mapInspectedLabel.setText("Inspected: Loading...");
        
        TextView mapRemainingLabel = findViewById(R.id.mapRemainingLabel);
        mapRemainingLabel.setText("Remaining: Loading...");
    }
    
    private void loadFirstAvailableSite() {
        siteRepository.getSites(new SiteRepository.SitesCallback() {
            @Override
            public void onSuccess(List<com.solarsnap.app.database.entities.SiteEntity> siteList) {
                runOnUiThread(() -> {
                    if (siteList != null && !siteList.isEmpty()) {
                        // Use first site as default
                        currentSiteId = siteList.get(0).getSiteId();
                        currentSiteName = siteList.get(0).getSiteName();
                        Log.d("SiteMapActivity", "Loaded first available site: " + currentSiteId);
                        
                        mapGridContainer = findViewById(R.id.mapGridContainer);
                        panelDetailsPopup = findViewById(R.id.panelDetailsPopup);
                        
                        initializeViews();
                        loadPanelData();
                        setupButtonListeners();
                        isInitialized = true; // Mark as fully initialized
                    } else {
                        // Fallback to hardcoded site
                        currentSiteId = "NV-Solar-04";
                        currentSiteName = "NV Solar Farm 04";
                        Log.d("SiteMapActivity", "No sites available, using fallback: " + currentSiteId);
                        
                        mapGridContainer = findViewById(R.id.mapGridContainer);
                        panelDetailsPopup = findViewById(R.id.panelDetailsPopup);
                        
                        initializeViews();
                        loadPanelData();
                        setupButtonListeners();
                        isInitialized = true; // Mark as fully initialized
                    }
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    // Fallback to hardcoded site on error
                    currentSiteId = "NV-Solar-04";
                    currentSiteName = "NV Solar Farm 04";
                    Log.e("SiteMapActivity", "Error loading sites, using fallback: " + error);
                    
                    mapGridContainer = findViewById(R.id.mapGridContainer);
                    panelDetailsPopup = findViewById(R.id.panelDetailsPopup);
                    
                    initializeViews();
                    loadPanelData();
                    setupButtonListeners();
                    isInitialized = true; // Mark as fully initialized
                });
            }
        });
    }
    
    private void loadPanelData() {
        loadPanelData(false);
    }
    
    private void loadPanelData(boolean forceRefresh) {
        Log.d("SiteMapActivity", "loadPanelData called for site: " + currentSiteId + ", forceRefresh: " + forceRefresh);
        
        // First load site details to get the correct layout
        siteRepository.getSiteDetails(currentSiteId, new SiteRepository.SiteDetailsCallback() {
            @Override
            public void onSuccess(com.solarsnap.app.database.entities.SiteEntity site) {
                Log.d("SiteMapActivity", "Site details loaded: " + site.getSiteName() + " (" + site.getRows() + "x" + site.getPanelsPerRow() + ")");
                
                // Update layout dimensions from site data
                totalRows = site.getRows();
                panelsPerRow = site.getPanelsPerRow();
                
                // Now load panels with correct layout
                siteRepository.getSitePanels(currentSiteId, new SiteRepository.PanelListCallback() {
                    @Override
                    public void onSuccess(List<PanelEntity> panelList) {
                        Log.d("SiteMapActivity", "Panels loaded: " + panelList.size() + " panels for site " + currentSiteId);
                        panels = panelList;
                        runOnUiThread(() -> {
                            updatePanelCounts();
                            generateSolarFarmMap();
                        });
                    }
                    
                    @Override
                    public void onError(String error) {
                        Log.e("SiteMapActivity", "Error loading panels: " + error);
                        panels = null;
                        runOnUiThread(() -> {
                            Toast.makeText(SiteMapActivity.this, "Error loading panels: " + error, 
                                Toast.LENGTH_SHORT).show();
                            // Don't generate mock data - show error state
                            TextView mapInspectedLabel = findViewById(R.id.mapInspectedLabel);
                            TextView mapRemainingLabel = findViewById(R.id.mapRemainingLabel);
                            mapInspectedLabel.setText("Error loading data");
                            mapRemainingLabel.setText("Please retry");
                        });
                    }
                }, forceRefresh);
            }
            
            @Override
            public void onError(String error) {
                Log.e("SiteMapActivity", "Error loading site details: " + error);
                runOnUiThread(() -> {
                    Toast.makeText(SiteMapActivity.this, "Error loading site details: " + error, 
                        Toast.LENGTH_SHORT).show();
                    // Use default layout if site details fail
                    totalRows = 15;
                    panelsPerRow = 80;
                    
                    // Still try to load panels
                    siteRepository.getSitePanels(currentSiteId, new SiteRepository.PanelListCallback() {
                        @Override
                        public void onSuccess(List<PanelEntity> panelList) {
                            panels = panelList;
                            runOnUiThread(() -> {
                                updatePanelCounts();
                                generateSolarFarmMap();
                            });
                        }
                        
                        @Override
                        public void onError(String error) {
                            panels = null;
                            runOnUiThread(() -> {
                                Toast.makeText(SiteMapActivity.this, "Error loading panels: " + error, 
                                    Toast.LENGTH_SHORT).show();
                                TextView mapInspectedLabel = findViewById(R.id.mapInspectedLabel);
                                TextView mapRemainingLabel = findViewById(R.id.mapRemainingLabel);
                                mapInspectedLabel.setText("Error loading data");
                                mapRemainingLabel.setText("Please retry");
                            });
                        }
                    }, forceRefresh);
                });
            }
        }, forceRefresh);
    }
    
    private void updatePanelCounts() {
        if (panels == null || panels.isEmpty()) {
            TextView mapInspectedLabel = findViewById(R.id.mapInspectedLabel);
            TextView mapRemainingLabel = findViewById(R.id.mapRemainingLabel);
            mapInspectedLabel.setText("Inspected: 0");
            mapRemainingLabel.setText("Remaining: 0");
            return;
        }
        
        // Calculate basic counts from local panel data
        int inspected = 0;
        for (PanelEntity panel : panels) {
            if (panel.getStatus() != null && !panel.getStatus().toLowerCase().equals("not_inspected")) {
                inspected++;
            }
        }
        
        int total = panels.size();
        int remaining = total - inspected;
        
        // Update basic counts
        TextView mapInspectedLabel = findViewById(R.id.mapInspectedLabel);
        TextView mapRemainingLabel = findViewById(R.id.mapRemainingLabel);
        
        mapInspectedLabel.setText("Inspected: " + inspected);
        mapRemainingLabel.setText("Remaining: " + remaining);
        
        // Fetch detailed statistics from backend
        fetchDetailedStatistics();
        
        // Only update grid dimensions if they weren't set from site details
        if (totalRows <= 0 || panelsPerRow <= 0) {
            if (!panels.isEmpty()) {
                int maxRow = 0, maxCol = 0;
                for (PanelEntity panel : panels) {
                    maxRow = Math.max(maxRow, panel.getRowNumber());
                    maxCol = Math.max(maxCol, panel.getColumnNumber());
                }
                totalRows = maxRow;
                panelsPerRow = maxCol;
            }
        }
        
        // Update next panel button
        updateNextPanelButton();
    }
    
    private void fetchDetailedStatistics() {
        if (currentSiteId == null) {
            Log.w("SiteMapActivity", "No site ID available for statistics");
            return;
        }
        
        // TODO: Replace with proper API service call
        // For now, calculate from local panel data as a fallback
        calculateLocalStatistics();
    }
    
    private void calculateLocalStatistics() {
        if (panels == null || panels.isEmpty()) {
            return;
        }
        
        int critical = 0;
        int warnings = 0;
        int healthy = 0;
        int uninspected = 0;
        
        for (PanelEntity panel : panels) {
            String status = panel.getStatus();
            if (status == null || status.toLowerCase().equals("not_inspected")) {
                uninspected++;
            } else {
                switch (status.toLowerCase()) {
                    case "critical":
                        critical++;
                        break;
                    case "warning":
                        warnings++;
                        break;
                    case "healthy":
                        healthy++;
                        break;
                    default:
                        uninspected++;
                        break;
                }
            }
        }
        
        // Update UI with detailed statistics
        updateDetailedStatisticsUI(critical, warnings, healthy, uninspected);
    }
    
    private void updateDetailedStatisticsUI(int critical, int warnings, int healthy, int uninspected) {
        // Log the statistics for now - in a real implementation, this would update UI elements
        Log.d("SiteMapActivity", String.format("Statistics - Critical: %d, Warnings: %d, Healthy: %d, Uninspected: %d", 
            critical, warnings, healthy, uninspected));
        
        // TODO: Update actual UI elements when detailed statistics view is implemented
        // This would update TextViews for critical faults, warnings, healthy panels, etc.
    }
    
    private void updateNextPanelButton() {
        Button nextPanelButton = findViewById(R.id.buttonNextPanel);
        
        if (panels == null || panels.isEmpty()) {
            nextPanelButton.setText("NEXT UNSCANNED PANEL\nNo data available");
            return;
        }
        
        PanelEntity nextPanel = findNextUnscannedPanel();
        
        if (nextPanel == null) {
            nextPanelButton.setText("ALL PANELS INSPECTED\n✓ Complete");
        } else {
            String status = nextPanel.getStatus().toLowerCase();
            String statusText;
            switch (status) {
                case "not_inspected":
                    statusText = "UNSCANNED";
                    break;
                case "critical":
                    statusText = "CRITICAL - NEEDS ATTENTION";
                    break;
                case "warning":
                    statusText = "WARNING - REVIEW NEEDED";
                    break;
                default:
                    statusText = "NEEDS INSPECTION";
                    break;
            }
            
            nextPanelButton.setText("NEXT " + statusText + " PANEL\nRow " + 
                nextPanel.getRowNumber() + " Panel " + nextPanel.getColumnNumber());
        }
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
        if (panels != null) {
            // Find the actual panel in our data by row and column
            for (PanelEntity panelEntity : panels) {
                if (panelEntity.getRowNumber() == row && panelEntity.getColumnNumber() == panel) {
                    String status = panelEntity.getStatus().toLowerCase();
                    switch (status) {
                        case "critical":
                            return COLOR_CRITICAL;
                        case "warning":
                            return COLOR_WARNING;
                        case "healthy":
                            return COLOR_HEALTHY;
                        case "not_inspected":
                        default:
                            return COLOR_NOT_INSPECTED;
                    }
                }
            }
        }
        
        // If panel not found in data, assume not inspected
        return COLOR_NOT_INSPECTED;
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
        
        // Get real panel data first
        PanelEntity panelEntity = null;
        if (panels != null) {
            for (PanelEntity p : panels) {
                if (p.getRowNumber() == row && p.getColumnNumber() == panel) {
                    panelEntity = p;
                    break;
                }
            }
        }
        
        // Use real panel ID if available, otherwise generate one
        String panelId;
        if (panelEntity != null) {
            panelId = panelEntity.getPanelId();
            selectedPanel = panelEntity; // Store the selected panel for reinspection
        } else {
            // Fallback to generated ID if panel not found in database
            panelId = String.format("PNL-A7-%04d", (row - 1) * panelsPerRow + panel);
            selectedPanel = null; // No real panel data available
        }
        
        TextView popupPanelIdLabel = findViewById(R.id.popupPanelIdLabel);
        TextView popupRowLabel = findViewById(R.id.popupRowLabel);
        TextView popupStringLabel = findViewById(R.id.popupStringLabel);
        TextView popupStatusLabel = findViewById(R.id.popupStatusLabel);
        TextView popupDeltaTempLabel = findViewById(R.id.popupDeltaTempLabel);
        TextView popupLastInspectionLabel = findViewById(R.id.popupLastInspectionLabel);
        
        popupPanelIdLabel.setText(panelId);
        
        // Use real panel data for Row and String if available
        if (panelEntity != null) {
            popupRowLabel.setText(String.valueOf(panelEntity.getRowNumber()));
            popupStringLabel.setText(String.valueOf(panelEntity.getStringNumber()));
        } else {
            // Fallback to calculated values if no real data
            popupRowLabel.setText(String.valueOf(row));
            popupStringLabel.setText(String.valueOf((panel - 1) / 10 + 1));
        }
        
        // Set status and temperature based on real data
        if (panelEntity != null) {
            String status = panelEntity.getStatus().toLowerCase();
            popupStatusLabel.setText(status.toUpperCase());
            popupStatusLabel.setTextColor(statusColor);
            popupLastInspectionLabel.setText("Last inspection: " + 
                (panelEntity.getLastInspection() != 0 ? panelEntity.getLastInspection() : "Never"));
            
            // Get real temperature data from inspections
            loadPanelInspectionData(panelEntity.getPanelId(), popupDeltaTempLabel);
        } else {
            // Panel not found in data
            popupStatusLabel.setText("NOT INSPECTED");
            popupStatusLabel.setTextColor(COLOR_NOT_INSPECTED);
            popupDeltaTempLabel.setText("--");
            popupDeltaTempLabel.setTextColor(COLOR_NOT_INSPECTED);
            popupLastInspectionLabel.setText("Not yet inspected");
        }
    }
    
    private void setupButtonListeners() {
        findViewById(R.id.buttonMapChangeSite).setOnClickListener(v -> {
            // Show site selection dialog instead of going back
            showSiteSelectionDialog();
        });
        
        findViewById(R.id.buttonFilterFaults).setOnClickListener(v -> showFilterDialog());
        
        findViewById(R.id.buttonViewImage).setOnClickListener(v -> {
            if (currentInspection != null) {
                showImageViewer(currentInspection);
            } else {
                Toast.makeText(this, "No inspection images available for this panel", Toast.LENGTH_SHORT).show();
            }
        });
        
        findViewById(R.id.buttonNavigateToPanel).setOnClickListener(v -> {
            // TODO: Implement GPS navigation to panel
            Toast.makeText(this, "GPS navigation not yet implemented", Toast.LENGTH_SHORT).show();
        });
        
        findViewById(R.id.buttonReinspect).setOnClickListener(v -> {
            // Navigate to inspection screen for this panel
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("siteId", currentSiteId);
            intent.putExtra("siteName", currentSiteName);
            
            // Pass the selected panel ID if available
            if (selectedPanel != null) {
                String panelId = selectedPanel.getPanelId();
                intent.putExtra("panelId", panelId);
                Log.d("SiteMapActivity", String.format("Launching inspection for panel: %s (Row: %d, Col: %d)", 
                    panelId, selectedPanel.getRowNumber(), selectedPanel.getColumnNumber()));
            } else {
                Log.w("SiteMapActivity", "No panel selected for reinspection - selectedPanel is null");
                Toast.makeText(this, "Error: No panel data available for reinspection", Toast.LENGTH_SHORT).show();
                return;
            }
            
            startActivity(intent);
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
        
        findViewById(R.id.buttonCenterGPS).setOnClickListener(v -> {
            // Center the map view
            zoomLevel = 1.0f;
            regenerateMap();
            Toast.makeText(this, "Map centered", Toast.LENGTH_SHORT).show();
        });
        
        findViewById(R.id.buttonNextPanel).setOnClickListener(v -> {
            navigateToNextUnscannedPanel();
        });
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
    
    private void showSiteSelectionDialog() {
        // Load available sites (API only to avoid duplicate dialogs)
        siteRepository.getSites(new SiteRepository.SitesCallback() {
            private boolean dialogShown = false; // Prevent multiple dialogs
            
            @Override
            public void onSuccess(List<com.solarsnap.app.database.entities.SiteEntity> siteList) {
                runOnUiThread(() -> {
                    // Prevent multiple dialogs from being shown
                    if (dialogShown) {
                        return;
                    }
                    dialogShown = true;
                    
                    if (siteList == null || siteList.isEmpty()) {
                        Toast.makeText(SiteMapActivity.this, "No sites available", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    // Filter out any test or invalid sites - be very specific
                    List<com.solarsnap.app.database.entities.SiteEntity> validSites = new ArrayList<>();
                    for (com.solarsnap.app.database.entities.SiteEntity site : siteList) {
                        // Only include the exact 3 sites we expect
                        if (site.getSiteId() != null && 
                            (site.getSiteId().equals("NV-Solar-04") || 
                             site.getSiteId().equals("NV-Solar-03") || 
                             site.getSiteId().equals("CA-Solar-01"))) {
                            validSites.add(site);
                        }
                    }
                    
                    if (validSites.isEmpty()) {
                        Toast.makeText(SiteMapActivity.this, "No valid sites available", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    String[] siteNames = new String[validSites.size()];
                    for (int i = 0; i < validSites.size(); i++) {
                        String siteName = validSites.get(i).getSiteName();
                        // Mark current site
                        if (validSites.get(i).getSiteId().equals(currentSiteId)) {
                            siteName += " (Current)";
                        }
                        siteNames[i] = siteName;
                    }
                    
                    AlertDialog.Builder builder = new AlertDialog.Builder(SiteMapActivity.this);
                    builder.setTitle("SELECT SITE")
                        .setItems(siteNames, (dialog, which) -> {
                            // Update current site
                            String oldSiteId = currentSiteId;
                            currentSiteId = validSites.get(which).getSiteId();
                            currentSiteName = validSites.get(which).getSiteName();
                            
                            Log.d("SiteMapActivity", "Site switching from " + oldSiteId + " to " + currentSiteId);
                            
                            // Update UI immediately
                            TextView mapSiteNameLabel = findViewById(R.id.mapSiteNameLabel);
                            mapSiteNameLabel.setText(currentSiteName);
                            
                            // Show loading state
                            TextView mapInspectedLabel = findViewById(R.id.mapInspectedLabel);
                            TextView mapRemainingLabel = findViewById(R.id.mapRemainingLabel);
                            mapInspectedLabel.setText("Loading...");
                            mapRemainingLabel.setText("Loading...");
                            
                            // Reload panel data for new site (force refresh to get fresh data)
                            loadPanelData(true);
                            
                            Toast.makeText(SiteMapActivity.this, "Site changed to " + currentSiteName, 
                                Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    if (!dialogShown) {
                        Toast.makeText(SiteMapActivity.this, "Error loading sites: " + error, 
                            Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
    
    private void navigateToNextUnscannedPanel() {
        if (panels == null || panels.isEmpty()) {
            Toast.makeText(this, "No panel data available", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Find next uninspected panel
        PanelEntity nextPanel = findNextUnscannedPanel();
        
        if (nextPanel == null) {
            Toast.makeText(this, "All panels have been inspected! 🎉", Toast.LENGTH_LONG).show();
            return;
        }
        
        // Highlight the panel on the map by showing its details
        int statusColor = getPanelStatus(nextPanel.getRowNumber(), nextPanel.getColumnNumber());
        showPanelDetails(nextPanel.getRowNumber(), nextPanel.getColumnNumber(), statusColor);
        
        // Show navigation options
        showNavigationDialog(nextPanel);
        
        // Update the button text
        updateNextPanelButton();
        
        Toast.makeText(this, "Found next panel: Row " + nextPanel.getRowNumber() + 
            ", Panel " + nextPanel.getColumnNumber(), Toast.LENGTH_SHORT).show();
    }
    
    private PanelEntity findNextUnscannedPanel() {
        if (panels == null || panels.isEmpty()) {
            return null;
        }
        
        // Sort panels by row, then by column for systematic navigation
        List<PanelEntity> sortedPanels = new ArrayList<>(panels);
        sortedPanels.sort((p1, p2) -> {
            int rowCompare = Integer.compare(p1.getRowNumber(), p2.getRowNumber());
            if (rowCompare != 0) {
                return rowCompare;
            }
            return Integer.compare(p1.getColumnNumber(), p2.getColumnNumber());
        });
        
        // Priority 1: Find uninspected panels first
        for (PanelEntity panel : sortedPanels) {
            String status = panel.getStatus().toLowerCase();
            if (status.equals("not_inspected")) {
                return panel;
            }
        }
        
        // Priority 2: Find critical panels that need re-inspection
        for (PanelEntity panel : sortedPanels) {
            String status = panel.getStatus().toLowerCase();
            if (status.equals("critical")) {
                return panel;
            }
        }
        
        // Priority 3: Find warning panels that might need attention
        for (PanelEntity panel : sortedPanels) {
            String status = panel.getStatus().toLowerCase();
            if (status.equals("warning")) {
                return panel;
            }
        }
        
        return null; // All panels are healthy
    }
    
    private void showNavigationDialog(PanelEntity panel) {
        String[] options = {
            "Inspect This Panel",
            "Mark as Inspected", 
            "Skip to Next Panel",
            "Cancel"
        };
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Panel Navigation")
            .setMessage("Panel " + panel.getPanelId() + " at Row " + panel.getRowNumber() + 
                       ", Column " + panel.getColumnNumber())
            .setItems(options, (dialog, which) -> {
                switch (which) {
                    case 0: // Inspect This Panel
                        startInspectionForPanel(panel);
                        break;
                    case 1: // Mark as Inspected
                        markPanelAsInspected(panel);
                        break;
                    case 2: // Skip to Next Panel
                        navigateToNextUnscannedPanel();
                        break;
                    case 3: // Cancel
                        dialog.dismiss();
                        break;
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    private void startInspectionForPanel(PanelEntity panel) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("siteId", currentSiteId);
        intent.putExtra("siteName", currentSiteName);
        intent.putExtra("panelId", panel.getPanelId());
        intent.putExtra("rowNumber", panel.getRowNumber());
        intent.putExtra("columnNumber", panel.getColumnNumber());
        intent.putExtra("mode", "inspect_panel");
        startActivity(intent);
    }
    
    private void markPanelAsInspected(PanelEntity panel) {
        // Update panel status to healthy (simulated inspection)
        panel.setStatus("HEALTHY");
        panel.setLastInspection(System.currentTimeMillis());
        
        // Update UI
        runOnUiThread(() -> {
            updatePanelCounts();
            generateSolarFarmMap();
            Toast.makeText(this, "Panel marked as inspected", Toast.LENGTH_SHORT).show();
            
            // Automatically find next panel
            navigateToNextUnscannedPanel();
        });
    }
    
    private void loadPanelInspectionData(String panelId, TextView deltaTempLabel) {
        InspectionRepository inspectionRepository = new InspectionRepository(this);
        
        inspectionRepository.getPanelInspections(panelId, new InspectionRepository.PanelInspectionCallback() {
            @Override
            public void onSuccess(InspectionEntity latestInspection, List<InspectionEntity> allInspections) {
                runOnUiThread(() -> {
                    if (latestInspection != null) {
                        // Show real temperature data
                        float deltaTemp = (float) latestInspection.getDeltaTemp();
                        float temperature = (float) latestInspection.getTemperature();
                        
                        String tempText = String.format("%.1f°C (Δ%.1f°C)", temperature, deltaTemp);
                        deltaTempLabel.setText(tempText);
                        
                        // Color based on delta temperature
                        if (deltaTemp > 15) {
                            deltaTempLabel.setTextColor(COLOR_CRITICAL);
                        } else if (deltaTemp > 8) {
                            deltaTempLabel.setTextColor(COLOR_WARNING);
                        } else {
                            deltaTempLabel.setTextColor(COLOR_HEALTHY);
                        }
                        
                        // Store inspection data for image viewing
                        currentInspection = latestInspection;
                    } else {
                        deltaTempLabel.setText("No inspection data");
                        deltaTempLabel.setTextColor(COLOR_NOT_INSPECTED);
                        currentInspection = null;
                    }
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    deltaTempLabel.setText("Error loading data");
                    deltaTempLabel.setTextColor(COLOR_NOT_INSPECTED);
                    currentInspection = null;
                });
            }
        });
    }
    
    private void showImageViewer(InspectionEntity inspection) {
        String[] imageOptions = {
            "View Thermal Image",
            "View Visual Image", 
            "View Both Images",
            "Cancel"
        };
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("View Inspection Images")
            .setMessage("Panel: " + inspection.getPanelId())
            .setItems(imageOptions, (dialog, which) -> {
                switch (which) {
                    case 0: // Thermal Image
                        if (inspection.getThermalImagePath() != null) {
                            openImageViewer(inspection.getThermalImagePath(), "Thermal Image");
                        } else {
                            Toast.makeText(this, "No thermal image available", Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case 1: // Visual Image
                        if (inspection.getVisualImagePath() != null) {
                            openImageViewer(inspection.getVisualImagePath(), "Visual Image");
                        } else {
                            Toast.makeText(this, "No visual image available", Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case 2: // Both Images
                        showBothImages(inspection);
                        break;
                    case 3: // Cancel
                        dialog.dismiss();
                        break;
                }
            })
            .show();
    }
    
    private void openImageViewer(String imagePath, String title) {
        // Create a simple image viewer dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        
        // Create a TextView to show image info (in a real app, this would be an ImageView)
        TextView imageInfo = new TextView(this);
        imageInfo.setPadding(40, 40, 40, 40);
        imageInfo.setTextSize(14);
        
        // For now, show image path and mock data (in production, load actual image)
        String imageContent = "Image Path: " + imagePath + "\n\n";
        imageContent += "📷 " + title + "\n";
        imageContent += "📅 Captured: " + new java.util.Date(currentInspection.getTimestamp()) + "\n";
        imageContent += "🌡️ Temperature: " + String.format("%.1f°C", currentInspection.getTemperature()) + "\n";
        imageContent += "⚠️ Delta Temp: " + String.format("%.1f°C", currentInspection.getDeltaTemp()) + "\n";
        imageContent += "📊 Status: " + currentInspection.getSeverity() + "\n";
        
        if (currentInspection.getIssueType() != null && !currentInspection.getIssueType().equals("none")) {
            imageContent += "🔍 Issue: " + currentInspection.getIssueType() + "\n";
        }
        
        imageContent += "\n[In production, this would display the actual thermal/visual image]";
        
        imageInfo.setText(imageContent);
        
        builder.setTitle(title)
            .setView(imageInfo)
            .setPositiveButton("Close", null)
            .setNeutralButton("Download", (dialog, which) -> {
                Toast.makeText(this, "Download functionality not implemented", Toast.LENGTH_SHORT).show();
            })
            .show();
    }
    
    private void showBothImages(InspectionEntity inspection) {
        // Create a dialog showing both images side by side
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(20, 20, 20, 20);
        
        // Thermal image section
        TextView thermalHeader = new TextView(this);
        thermalHeader.setText("🔥 THERMAL IMAGE");
        thermalHeader.setTextSize(16);
        thermalHeader.setPadding(0, 0, 0, 10);
        container.addView(thermalHeader);
        
        TextView thermalInfo = new TextView(this);
        String thermalContent = "Path: " + (inspection.getThermalImagePath() != null ? inspection.getThermalImagePath() : "Not available") + "\n";
        thermalContent += "Temperature: " + String.format("%.1f°C", inspection.getTemperature()) + "\n";
        thermalContent += "Delta Temp: " + String.format("%.1f°C", inspection.getDeltaTemp()) + "\n";
        thermalInfo.setText(thermalContent);
        thermalInfo.setPadding(0, 0, 0, 20);
        container.addView(thermalInfo);
        
        // Visual image section
        TextView visualHeader = new TextView(this);
        visualHeader.setText("📷 VISUAL IMAGE");
        visualHeader.setTextSize(16);
        visualHeader.setPadding(0, 0, 0, 10);
        container.addView(visualHeader);
        
        TextView visualInfo = new TextView(this);
        String visualContent = "Path: " + (inspection.getVisualImagePath() != null ? inspection.getVisualImagePath() : "Not available") + "\n";
        visualContent += "Status: " + inspection.getSeverity() + "\n";
        if (inspection.getIssueType() != null && !inspection.getIssueType().equals("none")) {
            visualContent += "Issue: " + inspection.getIssueType() + "\n";
        }
        visualInfo.setText(visualContent);
        container.addView(visualInfo);
        
        builder.setTitle("Inspection Images - " + inspection.getPanelId())
            .setView(container)
            .setPositiveButton("Close", null)
            .show();
    }
}
