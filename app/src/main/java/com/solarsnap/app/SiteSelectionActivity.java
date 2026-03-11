package com.solarsnap.app;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import com.solarsnap.app.database.entities.SiteEntity;
import com.solarsnap.app.repository.SiteRepository;
import com.solarsnap.app.repository.InspectionRepository;
import com.solarsnap.app.network.ApiClient;
import com.solarsnap.app.network.SolarSnapApiService;
import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SiteSelectionActivity extends AppCompatActivity {
    private SiteEntity currentSite;
    private SiteRepository siteRepository;
    private InspectionRepository inspectionRepository;
    private SolarSnapApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_site_selection);

        // Initialize repositories
        siteRepository = new SiteRepository(this);
        inspectionRepository = new InspectionRepository(this);
        apiService = ApiClient.getApiService(this);
        
        // Load sites from API/database
        loadSites();
        
        setupButtonListeners();
    }
    
    private void loadSites() {
        siteRepository.getSites(new SiteRepository.SitesCallback() {
            @Override
            public void onSuccess(List<SiteEntity> sites) {
                runOnUiThread(() -> {
                    if (!sites.isEmpty()) {
                        // If we don't have a current site, use the first one
                        // Otherwise, keep the current site (useful after registration)
                        if (currentSite == null) {
                            currentSite = sites.get(0);
                            initializeViews();
                        }
                        Log.d("SiteSelectionActivity", "Loaded " + sites.size() + " sites from API");
                    } else {
                        Toast.makeText(SiteSelectionActivity.this, 
                            "No sites available", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(SiteSelectionActivity.this, 
                        "Error loading sites: " + error, Toast.LENGTH_LONG).show();
                    // Create a default site for testing only if no current site exists
                    if (currentSite == null) {
                        createDefaultSite();
                    }
                });
            }
        });
    }
    
    private void createDefaultSite() {
        currentSite = new SiteEntity();
        currentSite.setSiteId("NV-Solar-04");
        currentSite.setSiteName("NV Solar Farm 04");
        currentSite.setTotalPanels(400);
        currentSite.setRows(20);
        currentSite.setPanelsPerRow(20);
        initializeViews();
    }

    private void initializeViews() {
        if (currentSite == null) return;
        
        // Set current time
        TextView timeLabel = findViewById(R.id.timeLabel);
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        timeLabel.setText("Time: " + sdf.format(new Date()));

        // Site info
        TextView siteNameLabel = findViewById(R.id.siteNameLabel);
        siteNameLabel.setText(currentSite.getSiteName());

        // Get inspection statistics
        inspectionRepository.getStatistics(currentSite.getSiteId(), 
            new InspectionRepository.StatisticsCallback() {
                @Override
                public void onSuccess(int total, int critical, int warning, int healthy) {
                    runOnUiThread(() -> {
                        // Calculate progress
                        int progress = currentSite.getTotalPanels() > 0 ? 
                            (total * 100) / currentSite.getTotalPanels() : 0;
                        
                        TextView progressPercentLabel = findViewById(R.id.progressPercentLabel);
                        progressPercentLabel.setText("Progress today: " + progress + "%");

                        ProgressBar siteProgressBar = findViewById(R.id.siteProgressBar);
                        siteProgressBar.setProgress(progress);

                        // Inspection progress
                        TextView scannedCountLabel = findViewById(R.id.scannedCountLabel);
                        scannedCountLabel.setText(String.valueOf(total));

                        TextView remainingCountLabel = findViewById(R.id.remainingCountLabel);
                        int remaining = currentSite.getTotalPanels() - total;
                        remainingCountLabel.setText(String.valueOf(remaining));

                        // Fault counts
                        TextView criticalCountLabel = findViewById(R.id.criticalCountLabel);
                        criticalCountLabel.setText(critical + " " + "●".repeat(Math.min(critical, 10)));

                        TextView warningCountLabel = findViewById(R.id.warningCountLabel);
                        warningCountLabel.setText(warning + " " + "●".repeat(Math.min(warning, 15)));

                        TextView healthyCountLabel = findViewById(R.id.healthyCountLabel);
                        healthyCountLabel.setText(String.valueOf(healthy));
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        // Set default values on error
                        TextView progressPercentLabel = findViewById(R.id.progressPercentLabel);
                        progressPercentLabel.setText("Progress today: 0%");

                        ProgressBar siteProgressBar = findViewById(R.id.siteProgressBar);
                        siteProgressBar.setProgress(0);

                        TextView scannedCountLabel = findViewById(R.id.scannedCountLabel);
                        scannedCountLabel.setText("0");

                        TextView remainingCountLabel = findViewById(R.id.remainingCountLabel);
                        remainingCountLabel.setText(String.valueOf(currentSite.getTotalPanels()));

                        TextView criticalCountLabel = findViewById(R.id.criticalCountLabel);
                        criticalCountLabel.setText("0");

                        TextView warningCountLabel = findViewById(R.id.warningCountLabel);
                        warningCountLabel.setText("0");

                        TextView healthyCountLabel = findViewById(R.id.healthyCountLabel);
                        healthyCountLabel.setText("0");
                    });
                }
            });
    }

    private void setupButtonListeners() {
        // Settings icon
        findViewById(R.id.settingsIcon).setOnClickListener(v -> openSettings());
        
        findViewById(R.id.buttonStartInspection).setOnClickListener(v -> startInspection());
        findViewById(R.id.buttonContinueInspection).setOnClickListener(v -> continueInspection());
        findViewById(R.id.buttonRegisterSite).setOnClickListener(v -> registerNewSite());
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

    private void registerNewSite() {
        // Show dialog to register a new site
        showRegisterSiteDialog();
    }
    
    private void showRegisterSiteDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Register New Site");
        
        // Create form layout
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 40);
        
        // Site ID field
        final EditText siteIdInput = new EditText(this);
        siteIdInput.setHint("Site ID (e.g., NV-Solar-05)");
        siteIdInput.setInputType(InputType.TYPE_CLASS_TEXT);
        layout.addView(siteIdInput);
        
        // Site Name field
        final EditText siteNameInput = new EditText(this);
        siteNameInput.setHint("Site Name (e.g., Nevada Solar Farm 05)");
        siteNameInput.setInputType(InputType.TYPE_CLASS_TEXT);
        layout.addView(siteNameInput);
        
        // Total Panels field
        final EditText totalPanelsInput = new EditText(this);
        totalPanelsInput.setHint("Total Panels (e.g., 1200)");
        totalPanelsInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        layout.addView(totalPanelsInput);
        
        // Rows field
        final EditText rowsInput = new EditText(this);
        rowsInput.setHint("Number of Rows (e.g., 15)");
        rowsInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        layout.addView(rowsInput);
        
        // Panels per Row field
        final EditText panelsPerRowInput = new EditText(this);
        panelsPerRowInput.setHint("Panels per Row (e.g., 80)");
        panelsPerRowInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        layout.addView(panelsPerRowInput);
        
        builder.setView(layout);
        
        builder.setPositiveButton("Register Site", (dialog, which) -> {
            String siteId = siteIdInput.getText().toString().trim();
            String siteName = siteNameInput.getText().toString().trim();
            String totalPanelsStr = totalPanelsInput.getText().toString().trim();
            String rowsStr = rowsInput.getText().toString().trim();
            String panelsPerRowStr = panelsPerRowInput.getText().toString().trim();
            
            // Validate required fields
            if (siteId.isEmpty()) {
                Toast.makeText(this, "Site ID is required", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (siteName.isEmpty()) {
                Toast.makeText(this, "Site Name is required", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (totalPanelsStr.isEmpty()) {
                Toast.makeText(this, "Total Panels is required", Toast.LENGTH_SHORT).show();
                return;
            }
            
            try {
                int totalPanels = Integer.parseInt(totalPanelsStr);
                if (totalPanels <= 0) {
                    Toast.makeText(this, "Total panels must be greater than 0", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                int rows = rowsStr.isEmpty() ? 0 : Integer.parseInt(rowsStr);
                int panelsPerRow = panelsPerRowStr.isEmpty() ? 0 : Integer.parseInt(panelsPerRowStr);
                
                // Validate layout consistency
                if (rows > 0 && panelsPerRow > 0) {
                    int calculatedPanels = rows * panelsPerRow;
                    if (calculatedPanels != totalPanels) {
                        Toast.makeText(this, 
                            String.format("Layout mismatch: %d rows × %d panels = %d, but total is %d", 
                                rows, panelsPerRow, calculatedPanels, totalPanels), 
                            Toast.LENGTH_LONG).show();
                        return;
                    }
                }
                
                // Validate site ID format (basic check)
                if (!siteId.matches("^[A-Z0-9-]+$")) {
                    Toast.makeText(this, "Site ID should contain only uppercase letters, numbers, and hyphens", Toast.LENGTH_LONG).show();
                    return;
                }
                
                // Call backend API to register the site
                registerSiteWithBackend(siteId, siteName, totalPanels, rows, panelsPerRow);
                
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Please enter valid numbers for panel counts", Toast.LENGTH_SHORT).show();
            }
        });
        
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    private void registerSiteWithBackend(String siteId, String siteName, int totalPanels, int rows, int panelsPerRow) {
        // Show loading dialog
        android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(this);
        progressDialog.setTitle("Registering Site");
        progressDialog.setMessage("Creating new site...");
        progressDialog.setCancelable(false);
        progressDialog.show();
        
        // Prepare API request
        JsonObject request = new JsonObject();
        request.addProperty("siteId", siteId);
        request.addProperty("siteName", siteName);
        request.addProperty("totalPanels", totalPanels);
        request.addProperty("rows", rows);
        request.addProperty("panelsPerRow", panelsPerRow);
        request.addProperty("status", "active");
        
        Log.d("SiteSelectionActivity", String.format("Registering site: %s (%s) - %d panels, %dx%d layout", 
            siteId, siteName, totalPanels, rows, panelsPerRow));
        
        // Make API call
        apiService.createSite(request).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    
                    if (response.isSuccessful() && response.body() != null) {
                        JsonObject responseBody = response.body();
                        boolean success = responseBody.has("success") && responseBody.get("success").getAsBoolean();
                        
                        if (success) {
                            Toast.makeText(SiteSelectionActivity.this, 
                                "✅ Site registered successfully: " + siteName, 
                                Toast.LENGTH_LONG).show();
                            
                            // Create a SiteEntity for the newly registered site
                            SiteEntity newSite = new SiteEntity();
                            newSite.setSiteId(siteId);
                            newSite.setSiteName(siteName);
                            newSite.setTotalPanels(totalPanels);
                            newSite.setRows(rows);
                            newSite.setPanelsPerRow(panelsPerRow);
                            newSite.setStatus("active");
                            newSite.setLastSynced(System.currentTimeMillis());
                            
                            // Set the newly registered site as current site
                            currentSite = newSite;
                            
                            // Refresh the UI with the new site
                            initializeViews();
                            
                            // Also refresh the sites list for future "Change Site" operations
                            loadSites();
                            
                            Log.d("SiteSelectionActivity", "Site registration successful: " + siteId);
                        } else {
                            String errorMessage = "Site registration failed";
                            if (responseBody.has("error") && responseBody.get("error").isJsonObject()) {
                                JsonObject error = responseBody.getAsJsonObject("error");
                                if (error.has("message")) {
                                    errorMessage = error.get("message").getAsString();
                                }
                            }
                            Toast.makeText(SiteSelectionActivity.this, 
                                "❌ " + errorMessage, Toast.LENGTH_LONG).show();
                            Log.e("SiteSelectionActivity", "Site registration failed: " + errorMessage);
                        }
                    } else {
                        String errorMessage = "Registration failed";
                        try {
                            if (response.errorBody() != null) {
                                String errorBody = response.errorBody().string();
                                com.google.gson.JsonObject errorJson = com.google.gson.JsonParser.parseString(errorBody).getAsJsonObject();
                                if (errorJson.has("error") && errorJson.get("error").isJsonObject()) {
                                    com.google.gson.JsonObject error = errorJson.getAsJsonObject("error");
                                    if (error.has("message")) {
                                        String fullError = error.get("message").getAsString();
                                        // Extract user-friendly error message
                                        if (fullError.contains("UNIQUE constraint failed: panels.panel_id")) {
                                            errorMessage = "Panel ID conflict detected. Please try again.";
                                        } else if (fullError.contains("UNIQUE constraint failed")) {
                                            errorMessage = "Site ID already exists";
                                        } else {
                                            errorMessage = fullError.length() > 100 ? 
                                                fullError.substring(0, 100) + "..." : fullError;
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            Log.w("SiteSelectionActivity", "Error parsing error response", e);
                        }
                        
                        if (response.code() == 409) {
                            errorMessage = "Site ID already exists";
                        } else if (response.code() == 400) {
                            errorMessage = "Invalid site data provided";
                        } else if (response.code() == 401) {
                            errorMessage = "Authentication required";
                        } else if (response.code() == 500) {
                            errorMessage = "Server error occurred. Please try again.";
                        }
                        
                        Toast.makeText(SiteSelectionActivity.this, 
                            "❌ " + errorMessage, 
                            Toast.LENGTH_LONG).show();
                        Log.e("SiteSelectionActivity", "Site registration failed with code: " + response.code() + 
                            ", message: " + errorMessage);
                    }
                });
            }
            
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    String errorMessage = "Network error: " + t.getMessage();
                    Toast.makeText(SiteSelectionActivity.this, 
                        "❌ " + errorMessage, Toast.LENGTH_LONG).show();
                    Log.e("SiteSelectionActivity", "Site registration network error", t);
                });
            }
        });
    }

    private void changeSite() {
        // Load all sites and show selection dialog
        Log.d("SiteSelectionActivity", "Loading sites for site selection dialog...");
        
        siteRepository.getSites(new SiteRepository.SitesCallback() {
            @Override
            public void onSuccess(List<SiteEntity> sites) {
                runOnUiThread(() -> {
                    Log.d("SiteSelectionActivity", "Loaded " + sites.size() + " sites for selection");
                    
                    if (sites.isEmpty()) {
                        Toast.makeText(SiteSelectionActivity.this, 
                            "No sites available", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(SiteSelectionActivity.this);
                    builder.setTitle("Select Solar Site (" + sites.size() + " available)");
                    
                    String[] siteNames = new String[sites.size()];
                    for (int i = 0; i < sites.size(); i++) {
                        SiteEntity site = sites.get(i);
                        String name = site.getSiteName() + " (" + site.getSiteId() + ")";
                        if (currentSite != null && site.getSiteId().equals(currentSite.getSiteId())) {
                            name += " ✓ Current";
                        }
                        siteNames[i] = name;
                        Log.d("SiteSelectionActivity", "Site option " + i + ": " + name);
                    }
                    
                    builder.setItems(siteNames, (dialog, which) -> {
                        SiteEntity selectedSite = sites.get(which);
                        Log.d("SiteSelectionActivity", "User selected site: " + selectedSite.getSiteId());
                        
                        if (currentSite == null || !selectedSite.getSiteId().equals(currentSite.getSiteId())) {
                            currentSite = selectedSite;
                            initializeViews();
                            Toast.makeText(SiteSelectionActivity.this, 
                                "Site changed to: " + selectedSite.getSiteName(), 
                                Toast.LENGTH_SHORT).show();
                            Log.d("SiteSelectionActivity", "Site successfully changed to: " + selectedSite.getSiteId());
                        } else {
                            Toast.makeText(SiteSelectionActivity.this, 
                                "Already on this site", Toast.LENGTH_SHORT).show();
                        }
                    });
                    
                    builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
                    builder.show();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Log.e("SiteSelectionActivity", "Error loading sites for selection: " + error);
                    Toast.makeText(SiteSelectionActivity.this, 
                        "Error loading sites: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void openMap() {
        Intent intent = new Intent(this, SiteMapActivity.class);
        if (currentSite != null) {
            intent.putExtra("siteId", currentSite.getSiteId());
            intent.putExtra("siteName", currentSite.getSiteName());
        }
        startActivity(intent);
    }

    private void openSiteMap() {
        Intent intent = new Intent(this, SiteMapActivity.class);
        if (currentSite != null) {
            intent.putExtra("siteId", currentSite.getSiteId());
            intent.putExtra("siteName", currentSite.getSiteName());
        }
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
