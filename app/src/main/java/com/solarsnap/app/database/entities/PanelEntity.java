package com.solarsnap.app.database.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;
import androidx.annotation.NonNull;

@Entity(tableName = "panels")
public class PanelEntity {
    
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "panel_id")
    private String panelId;
    
    @ColumnInfo(name = "site_id")
    private String siteId;
    
    @ColumnInfo(name = "row_number")
    private int rowNumber;
    
    @ColumnInfo(name = "column_number")
    private int columnNumber;
    
    @ColumnInfo(name = "string_number")
    private int stringNumber;
    
    @ColumnInfo(name = "status")
    private String status; // HEALTHY, WARNING, CRITICAL, UNINSPECTED
    
    @ColumnInfo(name = "last_inspection")
    private long lastInspection;

    // Constructor
    public PanelEntity() {
        this.status = "UNINSPECTED";
        this.lastInspection = 0;
    }

    // Getters and Setters
    @NonNull
    public String getPanelId() { return panelId; }
    public void setPanelId(@NonNull String panelId) { this.panelId = panelId; }
    
    public String getSiteId() { return siteId; }
    public void setSiteId(String siteId) { this.siteId = siteId; }
    
    public int getRowNumber() { return rowNumber; }
    public void setRowNumber(int rowNumber) { this.rowNumber = rowNumber; }
    
    public int getColumnNumber() { return columnNumber; }
    public void setColumnNumber(int columnNumber) { this.columnNumber = columnNumber; }
    
    public int getStringNumber() { return stringNumber; }
    public void setStringNumber(int stringNumber) { this.stringNumber = stringNumber; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public long getLastInspection() { return lastInspection; }
    public void setLastInspection(long lastInspection) { this.lastInspection = lastInspection; }
}
