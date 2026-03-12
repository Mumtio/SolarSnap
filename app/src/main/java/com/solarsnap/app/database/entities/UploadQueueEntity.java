package com.solarsnap.app.database.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;
import androidx.annotation.NonNull;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

@Entity(tableName = "upload_queue")
public class UploadQueueEntity {
    
    @PrimaryKey(autoGenerate = true)
    @NonNull
    private int id;
    
    @ColumnInfo(name = "inspection_id")
    private int inspectionId;
    
    @ColumnInfo(name = "inspection_uuid")
    private String inspectionUuid;
    
    @ColumnInfo(name = "panel_id")
    private String panelId;
    
    @ColumnInfo(name = "site_id")
    private String siteId;
    
    @ColumnInfo(name = "status")
    private String status; // pending, uploading, uploaded, failed
    
    @ColumnInfo(name = "file_size")
    private double fileSize;
    
    @ColumnInfo(name = "file_type")
    private String fileType; // thermal_image, visual_image, metadata
    
    @ColumnInfo(name = "file_path")
    private String filePath;
    
    @ColumnInfo(name = "retry_count")
    private int retryCount;
    
    @ColumnInfo(name = "error_message")
    private String errorMessage;
    
    @ColumnInfo(name = "created_at")
    private long createdAt;
    
    @ColumnInfo(name = "last_attempt_at")
    private long lastAttemptAt;
    
    @ColumnInfo(name = "backend_upload_id")
    private String backendUploadId;

    // Constructor
    public UploadQueueEntity() {
        this.status = "pending";
        this.retryCount = 0;
        this.createdAt = System.currentTimeMillis();
        this.lastAttemptAt = 0;
        this.fileType = "thermal_image";
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getInspectionId() { return inspectionId; }
    public void setInspectionId(int inspectionId) { this.inspectionId = inspectionId; }
    
    public String getInspectionUuid() { return inspectionUuid; }
    public void setInspectionUuid(String inspectionUuid) { this.inspectionUuid = inspectionUuid; }
    
    public String getPanelId() { return panelId; }
    public void setPanelId(String panelId) { this.panelId = panelId; }
    
    public String getSiteId() { return siteId; }
    public void setSiteId(String siteId) { this.siteId = siteId; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public double getFileSize() { return fileSize; }
    public void setFileSize(double fileSize) { this.fileSize = fileSize; }
    
    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }
    
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    
    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }
    
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    
    public long getLastAttemptAt() { return lastAttemptAt; }
    public void setLastAttemptAt(long lastAttemptAt) { this.lastAttemptAt = lastAttemptAt; }
    
    public String getBackendUploadId() { return backendUploadId; }
    public void setBackendUploadId(String backendUploadId) { this.backendUploadId = backendUploadId; }
    
    // Helper method to get formatted created date
    public String getFormattedCreatedAt() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, HH:mm", Locale.getDefault());
        return sdf.format(new Date(createdAt));
    }
    
    // Helper method to get file size in readable format
    public String getFormattedFileSize() {
        if (fileSize < 1) {
            return String.format("%.1f KB", fileSize * 1024);
        } else {
            return String.format("%.1f MB", fileSize);
        }
    }
}
