package com.solarsnap.app.database.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;
import androidx.annotation.NonNull;

@Entity(tableName = "sites")
public class SiteEntity {
    
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "site_id")
    private String siteId;
    
    @ColumnInfo(name = "site_name")
    private String siteName;
    
    @ColumnInfo(name = "total_panels")
    private int totalPanels;
    
    @ColumnInfo(name = "rows")
    private int rows;
    
    @ColumnInfo(name = "panels_per_row")
    private int panelsPerRow;
    
    @ColumnInfo(name = "latitude")
    private double latitude;
    
    @ColumnInfo(name = "longitude")
    private double longitude;
    
    @ColumnInfo(name = "status")
    private String status;
    
    @ColumnInfo(name = "last_synced")
    private long lastSynced;

    // Constructor
    public SiteEntity() {
        this.lastSynced = 0;
    }

    // Getters and Setters
    @NonNull
    public String getSiteId() { return siteId; }
    public void setSiteId(@NonNull String siteId) { this.siteId = siteId; }
    
    public String getSiteName() { return siteName; }
    public void setSiteName(String siteName) { this.siteName = siteName; }
    
    public int getTotalPanels() { return totalPanels; }
    public void setTotalPanels(int totalPanels) { this.totalPanels = totalPanels; }
    
    public int getRows() { return rows; }
    public void setRows(int rows) { this.rows = rows; }
    
    public int getPanelsPerRow() { return panelsPerRow; }
    public void setPanelsPerRow(int panelsPerRow) { this.panelsPerRow = panelsPerRow; }
    
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public long getLastSynced() { return lastSynced; }
    public void setLastSynced(long lastSynced) { this.lastSynced = lastSynced; }
}
