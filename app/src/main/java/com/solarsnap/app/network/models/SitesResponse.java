package com.solarsnap.app.network.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class SitesResponse {
    private boolean success;
    private List<SiteData> sites;
    
    public static class SiteData {
        @SerializedName("siteId")
        private String siteId;
        
        @SerializedName("siteName")
        private String siteName;
        
        @SerializedName("totalPanels")
        private int totalPanels;
        
        private int rows;
        
        @SerializedName("panelsPerRow")
        private int panelsPerRow;
        
        private double latitude;
        private double longitude;
        private String status;
        
        @SerializedName("inspectedPanels")
        private int inspectedPanels;
        
        @SerializedName("criticalCount")
        private int criticalCount;
        
        @SerializedName("warningCount")
        private int warningCount;
        
        // Getters
        public String getSiteId() { return siteId; }
        public String getSiteName() { return siteName; }
        public int getTotalPanels() { return totalPanels; }
        public int getRows() { return rows; }
        public int getPanelsPerRow() { return panelsPerRow; }
        public double getLatitude() { return latitude; }
        public double getLongitude() { return longitude; }
        public String getStatus() { return status; }
        public int getInspectedPanels() { return inspectedPanels; }
        public int getCriticalCount() { return criticalCount; }
        public int getWarningCount() { return warningCount; }
    }
    
    // Getters
    public boolean isSuccess() { return success; }
    public List<SiteData> getSites() { return sites; }
}
