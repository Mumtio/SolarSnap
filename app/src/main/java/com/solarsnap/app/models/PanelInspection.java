package com.solarsnap.app.models;

public class PanelInspection {
    private String assetId;
    private double temperature;
    private double deltaTemp;
    private String severity; // "HEALTHY", "WARNING", "CRITICAL"
    private String issueType; // "hotspot", "diode", "shading", "connection"
    private double latitude;
    private double longitude;
    private long timestamp;
    private String thermalImagePath;
    private String visualImagePath;
    private boolean uploaded;

    public PanelInspection(String assetId) {
        this.assetId = assetId;
        this.timestamp = System.currentTimeMillis();
        this.uploaded = false;
    }

    public String getAssetId() { return assetId; }
    public void setAssetId(String assetId) { this.assetId = assetId; }
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
    public String getThermalImagePath() { return thermalImagePath; }
    public void setThermalImagePath(String path) { this.thermalImagePath = path; }
    public String getVisualImagePath() { return visualImagePath; }
    public void setVisualImagePath(String path) { this.visualImagePath = path; }
    public boolean isUploaded() { return uploaded; }
    public void setUploaded(boolean uploaded) { this.uploaded = uploaded; }
}
