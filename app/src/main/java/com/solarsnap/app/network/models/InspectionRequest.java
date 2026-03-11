package com.solarsnap.app.network.models;

import com.google.gson.annotations.SerializedName;

public class InspectionRequest {
    @SerializedName("siteId")
    private String siteId;
    
    @SerializedName("panelId")
    private String panelId;
    
    private double temperature;
    
    @SerializedName("deltaTemp")
    private double deltaTemp;
    
    private String severity;
    
    @SerializedName("issueType")
    private String issueType;
    
    private double latitude;
    private double longitude;
    private long timestamp;
    
    @SerializedName("thermalImageUrl")
    private String thermalImageUrl;
    
    @SerializedName("visualImageUrl")
    private String visualImageUrl;
    
    // Constructor
    public InspectionRequest() {}
    
    // Getters and Setters
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
    
    public String getThermalImageUrl() { return thermalImageUrl; }
    public void setThermalImageUrl(String thermalImageUrl) { this.thermalImageUrl = thermalImageUrl; }
    
    public String getVisualImageUrl() { return visualImageUrl; }
    public void setVisualImageUrl(String visualImageUrl) { this.visualImageUrl = visualImageUrl; }
}
