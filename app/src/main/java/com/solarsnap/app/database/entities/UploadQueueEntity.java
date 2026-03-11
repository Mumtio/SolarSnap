package com.solarsnap.app.database.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;
import androidx.annotation.NonNull;

@Entity(tableName = "upload_queue")
public class UploadQueueEntity {
    
    @PrimaryKey(autoGenerate = true)
    @NonNull
    private int id;
    
    @ColumnInfo(name = "inspection_id")
    private int inspectionId;
    
    @ColumnInfo(name = "status")
    private String status; // pending, uploading, uploaded, failed
    
    @ColumnInfo(name = "file_size")
    private double fileSize;
    
    @ColumnInfo(name = "retry_count")
    private int retryCount;
    
    @ColumnInfo(name = "error_message")
    private String errorMessage;
    
    @ColumnInfo(name = "created_at")
    private long createdAt;
    
    @ColumnInfo(name = "last_attempt_at")
    private long lastAttemptAt;

    // Constructor
    public UploadQueueEntity() {
        this.status = "pending";
        this.retryCount = 0;
        this.createdAt = System.currentTimeMillis();
        this.lastAttemptAt = 0;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getInspectionId() { return inspectionId; }
    public void setInspectionId(int inspectionId) { this.inspectionId = inspectionId; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public double getFileSize() { return fileSize; }
    public void setFileSize(double fileSize) { this.fileSize = fileSize; }
    
    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }
    
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    
    public long getLastAttemptAt() { return lastAttemptAt; }
    public void setLastAttemptAt(long lastAttemptAt) { this.lastAttemptAt = lastAttemptAt; }
    
    // Additional getters for compatibility
    public String getPanelId() { 
        // This would need to be fetched from the related inspection
        return "N/A"; 
    }
    
    public String getFileType() { 
        return "thermal_image"; 
    }
    
    public String getFilePath() { 
        return ""; 
    }
}
