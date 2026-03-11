package com.solarsnap.app.models;

public class SolarSite {
    private String siteId;
    private String siteName;
    private int totalPanels;
    private int inspectedPanels;
    private double latitude;
    private double longitude;

    public SolarSite(String siteId, String siteName, int totalPanels) {
        this.siteId = siteId;
        this.siteName = siteName;
        this.totalPanels = totalPanels;
        this.inspectedPanels = 0;
    }

    public String getSiteId() { return siteId; }
    public String getSiteName() { return siteName; }
    public int getTotalPanels() { return totalPanels; }
    public int getInspectedPanels() { return inspectedPanels; }
    public void setInspectedPanels(int count) { this.inspectedPanels = count; }
    public int getProgressPercentage() { 
        return totalPanels > 0 ? (inspectedPanels * 100) / totalPanels : 0; 
    }
}
