package com.solarsnap.app.database.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;
import androidx.annotation.NonNull;

@Entity(tableName = "inspections")
public class InspectionEntity {
    
    @PrimaryKey(autoGenerate = true)
    @NonNull
    private int id;
    
    @ColumnInfo(name = "inspection_uuid")
    private String inspectionUuid;
    
    @ColumnInfo(name = "site_id")
    private String siteId;
    
    @ColumnInfo(name = "panel_id")
    private String panelId;
    
    @ColumnInfo(name = "temperature")
    private double temperature;
    
    @ColumnInfo(name = "delta_temp")
    private double deltaTemp;
    
    @ColumnInfo(name = "severity")
    private String severity; // HEALTHY, WARNING, CRITICAL
    
    @ColumnInfo(name = "issue_type")
    private String issueType; // hotspot, shading, soiling, cracking, disconnection
    
    @ColumnInfo(name = "latitude")
    private double latitude;
    
    @ColumnInfo(name = "longitude")
    private double longitude;
    
    @ColumnInfo(name = "timestamp")
    private long timestamp;
    
    @ColumnInfo(name = "thermal_image_path")
    private String thermalImagePath;
    
    @ColumnInfo(name = "visual_image_path")
    private String visualImagePath;
    
    @ColumnInfo(name = "uploaded")
    private boolean uploaded;
    
    @ColumnInfo(name = "created_at")
    private long createdAt;

    // Constructor
    public InspectionEntity() {
        this.createdAt = System.currentTimeMillis();
        this.uploaded = false;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getInspectionUuid() { return inspectionUuid; }
    public void setInspectionUuid(String inspectionUuid) { this.inspectionUuid = inspectionUuid; }
    
    public String getSiteId() { return siteId; }
    public void setSiteId(String siteId) { this.siteId = siteId; }
    
    public String getPanelId() { return panelId; }
    public void setPanelId(String panelId) { this.panelId = panelId; }
    
    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }
    
    public double getDeltaTemp() { return deltaTemp; }
    public void setDeltaTemp(double deltaTemp) { this.deltaTemp = deltaTemp; }
    
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    
    public String getIssueType() { return issueType; }
    public void setIssueType(String issueType) { this.issueType = issueType; }
    
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    
    public String getThermalImagePath() { return thermalImagePath; }
    public void setThermalImagePath(String thermalImagePath) { this.thermalImagePath = thermalImagePath; }
    
    public String getVisualImagePath() { return visualImagePath; }
    public void setVisualImagePath(String visualImagePath) { this.visualImagePath = visualImagePath; }
    
    public boolean isUploaded() { return uploaded; }
    public void setUploaded(boolean uploaded) { this.uploaded = uploaded; }
    
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
