package com.flir.atlassdk.acecamerasample;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;

public class UploadsActivity extends AppCompatActivity {
    
    private LinearLayout uploadsListContainer;
    private LinearLayout networkStatusBar;
    private TextView uploadNetworkStatus;
    private TextView uploadCloudIcon;
    private TextView pendingCountLabel;
    private TextView uploadingCountLabel;
    private TextView completedCountLabel;
    private TextView failedCountLabel;
    private ProgressBar uploadProgressBar;
    private TextView uploadProgressLabel;
    
    private List<UploadRecord> uploadRecords;
    private boolean isOnline = true;
    private Handler handler = new Handler();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_uploads);
        
        initializeViews();
        generateMockData();
        updateNetworkStatus();
        displayUploadRecords();
        setupButtonListeners();
    }
    
    private void initializeViews() {
        uploadsListContainer = findViewById(R.id.uploadsListContainer);
        networkStatusBar = findViewById(R.id.networkStatusBar);
        uploadNetworkStatus = findViewById(R.id.uploadNetworkStatus);
        uploadCloudIcon = findViewById(R.id.uploadCloudIcon);
        pendingCountLabel = findViewById(R.id.pendingCountLabel);
        uploadingCountLabel = findViewById(R.id.uploadingCountLabel);
        completedCountLabel = findViewById(R.id.completedCountLabel);
        failedCountLabel = findViewById(R.id.failedCountLabel);
        uploadProgressBar = findViewById(R.id.uploadProgressBar);
        uploadProgressLabel = findViewById(R.id.uploadProgressLabel);
    }
    
    private void generateMockData() {
        uploadRecords = new ArrayList<>();
        
        // Mock upload records with different statuses
        uploadRecords.add(new UploadRecord("PNL-A7-4402", 12, 8, "Hotspot", 
            18.5, "Aug 7 14:02", "Pending", 3.2));
        uploadRecords.add(new UploadRecord("PNL-A7-4403", 12, 9, "None", 
            1.2, "Aug 7 14:05", "Uploading", 2.8));
        uploadRecords.add(new UploadRecord("PNL-A7-4126", 11, 5, "Diode Failure", 
            22.3, "Aug 7 13:45", "Pending", 3.5));
        uploadRecords.add(new UploadRecord("PNL-A7-3850", 10, 2, "Cell Crack", 
            9.8, "Aug 7 13:20", "Failed", 3.1));
        uploadRecords.add(new UploadRecord("PNL-A7-2450", 8, 1, "Connection Fault", 
            11.2, "Aug 7 12:50", "Uploaded", 2.9));
        
        updateUploadStats();
    }
    
    private void updateUploadStats() {
        int pending = 0, uploading = 0, completed = 0, failed = 0;
        
        for (UploadRecord record : uploadRecords) {
            switch (record.status) {
                case "Pending":
                    pending++;
                    break;
                case "Uploading":
                    uploading++;
                    break;
                case "Uploaded":
                    completed++;
                    break;
                case "Failed":
                    failed++;
                    break;
            }
        }
        
        // Add mock completed uploads
        completed += 42;
        
        pendingCountLabel.setText(String.valueOf(pending));
        uploadingCountLabel.setText(String.valueOf(uploading));
        completedCountLabel.setText(String.valueOf(completed));
        failedCountLabel.setText(String.valueOf(failed));
        
        // Calculate progress
        int total = pending + uploading + completed + failed;
        int progress = total > 0 ? (completed * 100) / total : 0;
        uploadProgressBar.setProgress(progress);
        uploadProgressLabel.setText(progress + "% complete");
    }
    
    private void updateNetworkStatus() {
        if (isOnline) {
            networkStatusBar.setBackgroundColor(Color.parseColor("#4CAF50"));
            uploadNetworkStatus.setText("LTE");
            uploadNetworkStatus.setTextColor(Color.parseColor("#4CAF50"));
            uploadCloudIcon.setTextColor(Color.parseColor("#4CAF50"));
            
            TextView statusTitle = ((LinearLayout) networkStatusBar).getChildAt(0) instanceof TextView ? 
                (TextView) ((LinearLayout) networkStatusBar).getChildAt(0) : null;
            if (statusTitle != null) {
                statusTitle.setText("Connected");
            }
        } else {
            networkStatusBar.setBackgroundColor(Color.parseColor("#FF9800"));
            uploadNetworkStatus.setText("Offline");
            uploadNetworkStatus.setTextColor(Color.parseColor("#FF9800"));
            uploadCloudIcon.setTextColor(Color.parseColor("#757575"));
            
            TextView statusTitle = ((LinearLayout) networkStatusBar).getChildAt(0) instanceof TextView ? 
                (TextView) ((LinearLayout) networkStatusBar).getChildAt(0) : null;
            if (statusTitle != null) {
                statusTitle.setText("Offline");
            }
        }
    }
    
    private void displayUploadRecords() {
        uploadsListContainer.removeAllViews();
        
        if (uploadRecords.isEmpty()) {
            TextView emptyView = new TextView(this);
            emptyView.setText("No pending uploads");
            emptyView.setTextColor(Color.parseColor("#C3C3C3"));
            emptyView.setTextSize(16);
            emptyView.setGravity(Gravity.CENTER);
            emptyView.setPadding(0, 40, 0, 40);
            uploadsListContainer.addView(emptyView);
            return;
        }
        
        for (UploadRecord record : uploadRecords) {
            View recordCard = createUploadCard(record);
            uploadsListContainer.addView(recordCard);
        }
    }
    
    private View createUploadCard(UploadRecord record) {
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
        switch (record.status) {
            case "Pending":
                indicatorColor = Color.parseColor("#757575"); // Gray
                break;
            case "Uploading":
                indicatorColor = Color.parseColor("#00D9FF"); // Blue
                break;
            case "Uploaded":
                indicatorColor = Color.parseColor("#4CAF50"); // Green
                break;
            case "Failed":
                indicatorColor = Color.parseColor("#F44336"); // Red
                break;
            default:
                indicatorColor = Color.parseColor("#757575");
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
        panelIdText.setText("Panel: " + record.panelId);
        panelIdText.setTextColor(Color.WHITE);
        panelIdText.setTextSize(16);
        panelIdText.setTypeface(null, android.graphics.Typeface.BOLD);
        contentLayout.addView(panelIdText);
        
        // Row and String
        TextView locationText = new TextView(this);
        locationText.setText("Row: " + record.row + "  String: " + record.string);
        locationText.setTextColor(Color.parseColor("#C3C3C3"));
        locationText.setTextSize(13);
        contentLayout.addView(locationText);
        
        // Issue and Delta T
        TextView issueText = new TextView(this);
        issueText.setText("Issue: " + record.issue + "  ΔT: +" + record.deltaTemp + "°C");
        issueText.setTextColor(Color.parseColor("#C3C3C3"));
        issueText.setTextSize(13);
        contentLayout.addView(issueText);
        
        // Captured time
        TextView capturedText = new TextView(this);
        capturedText.setText("Captured: " + record.captured);
        capturedText.setTextColor(Color.parseColor("#C3C3C3"));
        capturedText.setTextSize(12);
        contentLayout.addView(capturedText);
        
        // Status and file size
        LinearLayout statusLayout = new LinearLayout(this);
        statusLayout.setOrientation(LinearLayout.HORIZONTAL);
        
        TextView statusText = new TextView(this);
        statusText.setText("Status: " + record.status);
        statusText.setTextColor(indicatorColor);
        statusText.setTextSize(14);
        statusText.setTypeface(null, android.graphics.Typeface.BOLD);
        statusLayout.addView(statusText);
        
        TextView fileSizeText = new TextView(this);
        fileSizeText.setText("  •  " + record.fileSize + " MB");
        fileSizeText.setTextColor(Color.parseColor("#C3C3C3"));
        fileSizeText.setTextSize(12);
        statusLayout.addView(fileSizeText);
        
        contentLayout.addView(statusLayout);
        
        card.addView(contentLayout);
        
        // Click listener
        card.setOnClickListener(v -> showRecordActions(record));
        
        return card;
    }
    
    private void showRecordActions(UploadRecord record) {
        String[] actions;
        
        if (record.status.equals("Failed")) {
            actions = new String[]{"View Record", "Retry Upload", "Delete Record", "Open on Map"};
        } else if (record.status.equals("Uploaded")) {
            actions = new String[]{"View Record", "Delete Record", "Open on Map"};
        } else {
            actions = new String[]{"View Record", "Cancel Upload", "Open on Map"};
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(record.panelId)
            .setItems(actions, (dialog, which) -> {
                String action = actions[which];
                switch (action) {
                    case "View Record":
                        showRecordDetails(record);
                        break;
                    case "Retry Upload":
                        retryUpload(record);
                        break;
                    case "Delete Record":
                        deleteRecord(record);
                        break;
                    case "Open on Map":
                        openOnMap();
                        break;
                    case "Cancel Upload":
                        Toast.makeText(this, "Upload cancelled", Toast.LENGTH_SHORT).show();
                        break;
                }
            })
            .show();
    }
    
    private void showRecordDetails(UploadRecord record) {
        String details = "Panel ID: " + record.panelId + "\n" +
                        "Captured: " + record.captured + "\n" +
                        "File size: " + record.fileSize + " MB\n" +
                        "Status: " + record.status;
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Record Details")
            .setMessage(details)
            .setPositiveButton("Close", null)
            .show();
    }
    
    private void retryUpload(UploadRecord record) {
        record.status = "Uploading";
        displayUploadRecords();
        updateUploadStats();
        
        Toast.makeText(this, "Retrying upload for " + record.panelId, Toast.LENGTH_SHORT).show();
        
        // Simulate upload completion after 2 seconds
        handler.postDelayed(() -> {
            record.status = "Uploaded";
            displayUploadRecords();
            updateUploadStats();
            Toast.makeText(this, "Upload completed", Toast.LENGTH_SHORT).show();
        }, 2000);
    }
    
    private void deleteRecord(UploadRecord record) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Record")
            .setMessage("Are you sure you want to delete this upload record?")
            .setPositiveButton("Delete", (dialog, which) -> {
                uploadRecords.remove(record);
                displayUploadRecords();
                updateUploadStats();
                Toast.makeText(this, "Record deleted", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    private void openOnMap() {
        Intent intent = new Intent(this, SiteMapActivity.class);
        startActivity(intent);
    }
    
    private void setupButtonListeners() {
        findViewById(R.id.buttonSyncNow).setOnClickListener(v -> syncNow());
        
        findViewById(R.id.buttonRetryFailed).setOnClickListener(v -> retryFailedUploads());
        
        findViewById(R.id.buttonClearUploaded).setOnClickListener(v -> clearUploadedData());
        
        // Toggle network status for demo
        networkStatusBar.setOnClickListener(v -> {
            isOnline = !isOnline;
            updateNetworkStatus();
            Toast.makeText(this, isOnline ? "Network connected" : "Network offline", 
                Toast.LENGTH_SHORT).show();
        });
    }
    
    private void syncNow() {
        if (!isOnline) {
            Toast.makeText(this, "Cannot sync: No network connection", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Toast.makeText(this, "Starting sync...", Toast.LENGTH_SHORT).show();
        
        // Simulate syncing all pending records
        for (UploadRecord record : uploadRecords) {
            if (record.status.equals("Pending")) {
                record.status = "Uploading";
            }
        }
        
        displayUploadRecords();
        updateUploadStats();
        
        // Simulate completion
        handler.postDelayed(() -> {
            for (UploadRecord record : uploadRecords) {
                if (record.status.equals("Uploading")) {
                    record.status = "Uploaded";
                }
            }
            displayUploadRecords();
            updateUploadStats();
            Toast.makeText(this, "Sync completed", Toast.LENGTH_SHORT).show();
        }, 3000);
    }
    
    private void retryFailedUploads() {
        int failedCount = 0;
        for (UploadRecord record : uploadRecords) {
            if (record.status.equals("Failed")) {
                record.status = "Uploading";
                failedCount++;
            }
        }
        
        if (failedCount == 0) {
            Toast.makeText(this, "No failed uploads to retry", Toast.LENGTH_SHORT).show();
            return;
        }
        
        displayUploadRecords();
        updateUploadStats();
        Toast.makeText(this, "Retrying " + failedCount + " failed uploads", Toast.LENGTH_SHORT).show();
        
        // Simulate completion
        handler.postDelayed(() -> {
            for (UploadRecord record : uploadRecords) {
                if (record.status.equals("Uploading")) {
                    record.status = "Uploaded";
                }
            }
            displayUploadRecords();
            updateUploadStats();
        }, 2000);
    }
    
    private void clearUploadedData() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Clear Uploaded Data")
            .setMessage("This will delete all successfully uploaded records to free storage. Continue?")
            .setPositiveButton("Clear", (dialog, which) -> {
                uploadRecords.removeIf(record -> record.status.equals("Uploaded"));
                displayUploadRecords();
                updateUploadStats();
                Toast.makeText(this, "Uploaded data cleared", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    // Inner class for upload records
    private static class UploadRecord {
        String panelId;
        int row;
        int string;
        String issue;
        double deltaTemp;
        String captured;
        String status;
        double fileSize;
        
        UploadRecord(String panelId, int row, int string, String issue, 
                    double deltaTemp, String captured, String status, double fileSize) {
            this.panelId = panelId;
            this.row = row;
            this.string = string;
            this.issue = issue;
            this.deltaTemp = deltaTemp;
            this.captured = captured;
            this.status = status;
            this.fileSize = fileSize;
        }
    }
}
