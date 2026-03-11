package com.solarsnap.app;

import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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
import com.solarsnap.app.repository.UploadRepository;
import com.solarsnap.app.database.entities.UploadQueueEntity;
import com.solarsnap.app.sync.UploadService;
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
    
    private List<UploadQueueEntity> uploadRecords;
    private UploadRepository uploadRepository;
    private UploadService uploadService;
    private Handler handler = new Handler();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_uploads);
        
        uploadRepository = new UploadRepository(this);
        uploadService = new UploadService(this);
        
        initializeViews();
        loadUploadData();
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
    
    private void loadUploadData() {
        uploadRepository.getAllUploads(new UploadRepository.UploadListCallback() {
            @Override
            public void onSuccess(List<UploadQueueEntity> uploads) {
                uploadRecords = uploads;
                runOnUiThread(() -> {
                    updateUploadStats();
                    displayUploadRecords();
                });
            }
            
            @Override
            public void onError(String error) {
                uploadRecords = new ArrayList<>();
                runOnUiThread(() -> {
                    updateUploadStats();
                    displayUploadRecords();
                    Toast.makeText(UploadsActivity.this, "Error loading uploads: " + error, 
                        Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    private void updateUploadStats() {
        if (uploadRecords == null) {
            uploadRecords = new ArrayList<>();
        }
        
        int pending = 0, uploading = 0, completed = 0, failed = 0;
        
        for (UploadQueueEntity record : uploadRecords) {
            switch (record.getStatus()) {
                case "pending":
                    pending++;
                    break;
                case "uploading":
                    uploading++;
                    break;
                case "completed":
                    completed++;
                    break;
                case "failed":
                    failed++;
                    break;
            }
        }
        
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
        boolean isOnline = isNetworkAvailable();
        
        if (isOnline) {
            networkStatusBar.setBackgroundColor(Color.parseColor("#4CAF50"));
            uploadNetworkStatus.setText("Connected");
            uploadNetworkStatus.setTextColor(Color.parseColor("#4CAF50"));
            uploadCloudIcon.setTextColor(Color.parseColor("#4CAF50"));
        } else {
            networkStatusBar.setBackgroundColor(Color.parseColor("#FF9800"));
            uploadNetworkStatus.setText("Offline");
            uploadNetworkStatus.setTextColor(Color.parseColor("#FF9800"));
            uploadCloudIcon.setTextColor(Color.parseColor("#757575"));
        }
    }
    
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = 
            (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
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
        
        for (UploadQueueEntity record : uploadRecords) {
            View recordCard = createUploadCard(record);
            uploadsListContainer.addView(recordCard);
        }
    }
    
    private View createUploadCard(UploadQueueEntity record) {
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
        switch (record.getStatus()) {
            case "pending":
                indicatorColor = Color.parseColor("#757575"); // Gray
                break;
            case "uploading":
                indicatorColor = Color.parseColor("#00D9FF"); // Blue
                break;
            case "completed":
                indicatorColor = Color.parseColor("#4CAF50"); // Green
                break;
            case "failed":
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
        panelIdText.setText("Panel: " + record.getPanelId());
        panelIdText.setTextColor(Color.WHITE);
        panelIdText.setTextSize(16);
        panelIdText.setTypeface(null, android.graphics.Typeface.BOLD);
        contentLayout.addView(panelIdText);
        
        // File type and path
        TextView fileInfoText = new TextView(this);
        fileInfoText.setText("Type: " + record.getFileType() + "  Path: " + record.getFilePath());
        fileInfoText.setTextColor(Color.parseColor("#C3C3C3"));
        fileInfoText.setTextSize(13);
        contentLayout.addView(fileInfoText);
        
        // Created time
        TextView createdText = new TextView(this);
        createdText.setText("Created: " + record.getCreatedAt());
        createdText.setTextColor(Color.parseColor("#C3C3C3"));
        createdText.setTextSize(12);
        contentLayout.addView(createdText);
        
        // Status and retry count
        LinearLayout statusLayout = new LinearLayout(this);
        statusLayout.setOrientation(LinearLayout.HORIZONTAL);
        
        TextView statusText = new TextView(this);
        statusText.setText("Status: " + record.getStatus().toUpperCase());
        statusText.setTextColor(indicatorColor);
        statusText.setTextSize(14);
        statusText.setTypeface(null, android.graphics.Typeface.BOLD);
        statusLayout.addView(statusText);
        
        if (record.getRetryCount() > 0) {
            TextView retryText = new TextView(this);
            retryText.setText("  •  Retries: " + record.getRetryCount());
            retryText.setTextColor(Color.parseColor("#C3C3C3"));
            retryText.setTextSize(12);
            statusLayout.addView(retryText);
        }
        
        contentLayout.addView(statusLayout);
        
        card.addView(contentLayout);
        
        // Click listener
        card.setOnClickListener(v -> showRecordActions(record));
        
        return card;
    }
    
    private void showRecordActions(UploadQueueEntity record) {
        String[] actions;
        
        if (record.getStatus().equals("failed")) {
            actions = new String[]{"View Record", "Retry Upload", "Delete Record", "Open on Map"};
        } else if (record.getStatus().equals("completed")) {
            actions = new String[]{"View Record", "Delete Record", "Open on Map"};
        } else {
            actions = new String[]{"View Record", "Cancel Upload", "Open on Map"};
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(record.getPanelId())
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
                        cancelUpload(record);
                        break;
                }
            })
            .show();
    }
    
    private void showRecordDetails(UploadQueueEntity record) {
        String details = "Panel ID: " + record.getPanelId() + "\n" +
                        "File Type: " + record.getFileType() + "\n" +
                        "File Path: " + record.getFilePath() + "\n" +
                        "Status: " + record.getStatus() + "\n" +
                        "Created: " + record.getCreatedAt() + "\n" +
                        "Retry Count: " + record.getRetryCount();
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Record Details")
            .setMessage(details)
            .setPositiveButton("Close", null)
            .show();
    }
    
    private void retryUpload(UploadQueueEntity record) {
        uploadRepository.retryUpload(record.getId(), new UploadRepository.UploadCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    Toast.makeText(UploadsActivity.this, "Retrying upload for " + record.getPanelId(), 
                        Toast.LENGTH_SHORT).show();
                    loadUploadData(); // Refresh the list
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(UploadsActivity.this, "Failed to retry upload: " + error, 
                        Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    private void deleteRecord(UploadQueueEntity record) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Record")
            .setMessage("Are you sure you want to delete this upload record?")
            .setPositiveButton("Delete", (dialog, which) -> {
                uploadRepository.deleteUpload(record.getId(), new UploadRepository.UploadCallback() {
                    @Override
                    public void onSuccess() {
                        runOnUiThread(() -> {
                            Toast.makeText(UploadsActivity.this, "Record deleted", Toast.LENGTH_SHORT).show();
                            loadUploadData(); // Refresh the list
                        });
                    }
                    
                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            Toast.makeText(UploadsActivity.this, "Failed to delete record: " + error, 
                                Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    private void cancelUpload(UploadQueueEntity record) {
        uploadRepository.cancelUpload(record.getId(), new UploadRepository.UploadCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    Toast.makeText(UploadsActivity.this, "Upload cancelled", Toast.LENGTH_SHORT).show();
                    loadUploadData(); // Refresh the list
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(UploadsActivity.this, "Failed to cancel upload: " + error, 
                        Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    private void openOnMap() {
        Intent intent = new Intent(this, SiteMapActivity.class);
        startActivity(intent);
    }
    
    private void setupButtonListeners() {
        findViewById(R.id.buttonSyncNow).setOnClickListener(v -> syncNow());
        
        findViewById(R.id.buttonRetryFailed).setOnClickListener(v -> retryFailedUploads());
        
        findViewById(R.id.buttonClearUploaded).setOnClickListener(v -> clearUploadedData());
        
        // Refresh network status periodically
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                updateNetworkStatus();
                handler.postDelayed(this, 5000); // Check every 5 seconds
            }
        }, 1000);
    }
    
    private void syncNow() {
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "Cannot sync: No network connection", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Toast.makeText(this, "Starting sync...", Toast.LENGTH_SHORT).show();
        
        // Use the background sync service for immediate sync
        uploadService.syncNow();
        
        // Refresh the list after a short delay
        handler.postDelayed(() -> {
            loadUploadData();
        }, 2000);
    }
    
    private void retryFailedUploads() {
        uploadRepository.retryFailedUploads(new UploadRepository.UploadCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    Toast.makeText(UploadsActivity.this, "Retrying failed uploads", Toast.LENGTH_SHORT).show();
                    loadUploadData(); // Refresh the list
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(UploadsActivity.this, "Failed to retry uploads: " + error, 
                        Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    private void clearUploadedData() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Clear Uploaded Data")
            .setMessage("This will delete all successfully uploaded records to free storage. Continue?")
            .setPositiveButton("Clear", (dialog, which) -> {
                uploadRepository.clearCompletedUploads(new UploadRepository.UploadCallback() {
                    @Override
                    public void onSuccess() {
                        runOnUiThread(() -> {
                            Toast.makeText(UploadsActivity.this, "Uploaded data cleared", Toast.LENGTH_SHORT).show();
                            loadUploadData(); // Refresh the list
                        });
                    }
                    
                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            Toast.makeText(UploadsActivity.this, "Failed to clear data: " + error, 
                                Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
}
