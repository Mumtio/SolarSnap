package com.solarsnap.app.repository;

import android.content.Context;
import android.util.Log;

import com.solarsnap.app.database.AppDatabase;
import com.solarsnap.app.database.dao.SiteDao;
import com.solarsnap.app.database.dao.PanelDao;
import com.solarsnap.app.database.entities.SiteEntity;
import com.solarsnap.app.database.entities.PanelEntity;
import com.solarsnap.app.network.ApiClient;
import com.solarsnap.app.network.SolarSnapApiService;
import com.solarsnap.app.network.models.SitesResponse;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SiteRepository {
    
    private static final String TAG = "SiteRepository";
    private final SolarSnapApiService apiService;
    private final SiteDao siteDao;
    private final PanelDao panelDao;
    private final Executor executor;
    
    public SiteRepository(Context context) {
        this.apiService = ApiClient.getApiService(context);
        this.siteDao = AppDatabase.getInstance(context).siteDao();
        this.panelDao = AppDatabase.getInstance(context).panelDao();
        this.executor = Executors.newSingleThreadExecutor();
    }
    
    // Callback interfaces
    public interface SitesCallback {
        void onSuccess(List<SiteEntity> sites);
        void onError(String error);
    }
    
    public interface SiteDetailsCallback {
        void onSuccess(SiteEntity site);
        void onError(String error);
    }
    
    public interface PanelListCallback {
        void onSuccess(List<PanelEntity> panels);
        void onError(String error);
    }
    
    // Get sites from API and cache in database
    public void getSites(SitesCallback callback) {
        // Clear any old cached data first to prevent stale entries
        executor.execute(() -> {
            siteDao.deleteAll();
            Log.d(TAG, "Cleared cached sites");
        });
        
        // Fetch fresh data from API
        apiService.getSites().enqueue(new Callback<SitesResponse>() {
            @Override
            public void onResponse(Call<SitesResponse> call, Response<SitesResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    SitesResponse sitesResponse = response.body();
                    
                    if (sitesResponse.isSuccess()) {
                        // Convert to entities
                        List<SiteEntity> siteEntities = new ArrayList<>();
                        for (SitesResponse.SiteData siteData : sitesResponse.getSites()) {
                            SiteEntity entity = new SiteEntity();
                            entity.setSiteId(siteData.getSiteId());
                            entity.setSiteName(siteData.getSiteName());
                            entity.setTotalPanels(siteData.getTotalPanels());
                            entity.setRows(siteData.getRows());
                            entity.setPanelsPerRow(siteData.getPanelsPerRow());
                            entity.setLatitude(siteData.getLatitude());
                            entity.setLongitude(siteData.getLongitude());
                            entity.setStatus(siteData.getStatus());
                            entity.setLastSynced(System.currentTimeMillis());
                            siteEntities.add(entity);
                        }
                        
                        // Save to database
                        executor.execute(() -> {
                            siteDao.insertAll(siteEntities);
                            Log.d(TAG, "Sites cached: " + siteEntities.size());
                        });
                        
                        callback.onSuccess(siteEntities);
                    } else {
                        callback.onError("Failed to fetch sites");
                    }
                } else {
                    callback.onError("API error: " + response.code());
                }
            }
            
            @Override
            public void onFailure(Call<SitesResponse> call, Throwable t) {
                Log.e(TAG, "Error fetching sites: " + t.getMessage());
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }
    
    // Get sites from database only (offline mode)
    public void getSitesFromDatabase(SitesCallback callback) {
        executor.execute(() -> {
            List<SiteEntity> sites = siteDao.getAllSites();
            callback.onSuccess(sites);
        });
    }
    
    // Get single site by ID
    public void getSiteById(String siteId, SitesCallback callback) {
        executor.execute(() -> {
            SiteEntity site = siteDao.getSiteById(siteId);
            if (site != null) {
                List<SiteEntity> sites = new ArrayList<>();
                sites.add(site);
                callback.onSuccess(sites);
            } else {
                callback.onError("Site not found");
            }
        });
    }
    
    // Get site details from API (force fresh data for site switching)
    public void getSiteDetails(String siteId, SiteDetailsCallback callback) {
        getSiteDetails(siteId, callback, false);
    }
    
    public void getSiteDetails(String siteId, SiteDetailsCallback callback, boolean forceRefresh) {
        if (!forceRefresh) {
            // First, try to get from database
            executor.execute(() -> {
                SiteEntity cachedSite = siteDao.getSiteById(siteId);
                if (cachedSite != null) {
                    Log.d(TAG, "Returning cached site details for: " + siteId);
                    callback.onSuccess(cachedSite);
                }
            });
        }
        
        // Fetch from API
        apiService.getSiteDetails(siteId).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject jsonResponse = response.body();
                    
                    if (jsonResponse.has("success") && jsonResponse.get("success").getAsBoolean()) {
                        JsonObject siteData = jsonResponse.getAsJsonObject("site");
                        
                        SiteEntity entity = new SiteEntity();
                        entity.setSiteId(siteData.get("siteId").getAsString());
                        entity.setSiteName(siteData.get("siteName").getAsString());
                        entity.setTotalPanels(siteData.get("totalPanels").getAsInt());
                        entity.setRows(siteData.get("rows").getAsInt());
                        entity.setPanelsPerRow(siteData.get("panelsPerRow").getAsInt());
                        entity.setLatitude(siteData.has("latitude") && !siteData.get("latitude").isJsonNull() ? 
                            siteData.get("latitude").getAsDouble() : 0.0);
                        entity.setLongitude(siteData.has("longitude") && !siteData.get("longitude").isJsonNull() ? 
                            siteData.get("longitude").getAsDouble() : 0.0);
                        entity.setStatus(siteData.get("status").getAsString());
                        entity.setLastSynced(System.currentTimeMillis());
                        
                        // Save to database
                        executor.execute(() -> {
                            siteDao.insert(entity);
                            Log.d(TAG, "Site details cached: " + siteId);
                        });
                        
                        callback.onSuccess(entity);
                    } else {
                        callback.onError("Failed to fetch site details");
                    }
                } else {
                    callback.onError("API error: " + response.code());
                }
            }
            
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "Error fetching site details: " + t.getMessage());
                
                if (!forceRefresh) {
                    // Return cached data on network error
                    executor.execute(() -> {
                        SiteEntity cachedSite = siteDao.getSiteById(siteId);
                        if (cachedSite != null) {
                            callback.onSuccess(cachedSite);
                        } else {
                            callback.onError("Network error: " + t.getMessage());
                        }
                    });
                } else {
                    callback.onError("Network error: " + t.getMessage());
                }
            }
        });
    }
    
    // Get site panels from API and cache in database (with pagination support)
    public void getSitePanels(String siteId, PanelListCallback callback) {
        getSitePanels(siteId, callback, false);
    }
    
    public void getSitePanels(String siteId, PanelListCallback callback, boolean forceRefresh) {
        if (!forceRefresh) {
            // First, try to get from database
            executor.execute(() -> {
                List<PanelEntity> cachedPanels = panelDao.getPanelsBySite(siteId);
                if (!cachedPanels.isEmpty()) {
                    Log.d(TAG, "Returning cached panels for site " + siteId + ": " + cachedPanels.size());
                    callback.onSuccess(cachedPanels);
                }
            });
        }
        
        // Fetch all pages from API
        fetchAllPanelPages(siteId, 1, new ArrayList<>(), callback);
    }
    
    private void fetchAllPanelPages(String siteId, int page, List<PanelEntity> allPanels, PanelListCallback callback) {
        apiService.getSitePanels(siteId, page, 100).enqueue(new Callback<JsonObject>() {  // Reduced to 100 per page
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject jsonResponse = response.body();
                    
                    if (jsonResponse.has("success") && jsonResponse.get("success").getAsBoolean()) {
                        // Parse panels from response
                        List<PanelEntity> panelEntities = new ArrayList<>();
                        
                        if (jsonResponse.has("panels") && jsonResponse.get("panels").isJsonArray()) {
                            jsonResponse.getAsJsonArray("panels").forEach(element -> {
                                JsonObject panelObj = element.getAsJsonObject();
                                PanelEntity entity = new PanelEntity();
                                entity.setPanelId(panelObj.get("panel_id").getAsString());
                                entity.setSiteId(siteId);
                                entity.setRowNumber(panelObj.get("row_number").getAsInt());
                                entity.setColumnNumber(panelObj.get("column_number").getAsInt());
                                
                                // Handle optional fields
                                if (panelObj.has("string_number")) {
                                    entity.setStringNumber(panelObj.get("string_number").getAsInt());
                                }
                                entity.setStatus(panelObj.get("status").getAsString());
                                if (panelObj.has("last_inspection") && !panelObj.get("last_inspection").isJsonNull()) {
                                    // Parse ISO date string to timestamp
                                    try {
                                        String dateStr = panelObj.get("last_inspection").getAsString();
                                        // Simple timestamp for now
                                        entity.setLastInspection(System.currentTimeMillis());
                                    } catch (Exception e) {
                                        entity.setLastInspection(0);
                                    }
                                }
                                panelEntities.add(entity);
                            });
                        }
                        
                        allPanels.addAll(panelEntities);
                        
                        // Check if there are more pages
                        boolean hasNext = false;
                        if (jsonResponse.has("pagination")) {
                            JsonObject pagination = jsonResponse.getAsJsonObject("pagination");
                            hasNext = pagination.has("has_next") && pagination.get("has_next").getAsBoolean();
                        }
                        
                        if (hasNext) {
                            // Fetch next page
                            fetchAllPanelPages(siteId, page + 1, allPanels, callback);
                        } else {
                            // All pages fetched, save to database and return
                            executor.execute(() -> {
                                panelDao.insertAll(allPanels);
                                Log.d(TAG, "All panels cached for site " + siteId + ": " + allPanels.size());
                            });
                            
                            callback.onSuccess(allPanels);
                        }
                    } else {
                        callback.onError("Failed to fetch panels for site " + siteId);
                    }
                } else {
                    callback.onError("API error: " + response.code());
                }
            }
            
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "Error fetching panels page " + page + " for site " + siteId + ": " + t.getMessage());
                
                // Retry once for network issues
                if (page == 1 && t.getMessage() != null && t.getMessage().contains("unexpected end of stream")) {
                    Log.d(TAG, "Retrying first page due to stream error...");
                    new Thread(() -> {
                        try {
                            Thread.sleep(1000); // Wait 1 second before retry
                            fetchAllPanelPages(siteId, page, allPanels, callback);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }).start();
                    return;
                }
                
                // Return cached data on network error
                executor.execute(() -> {
                    List<PanelEntity> cachedPanels = panelDao.getPanelsBySite(siteId);
                    if (!cachedPanels.isEmpty()) {
                        callback.onSuccess(cachedPanels);
                    } else {
                        callback.onError("Network error: " + t.getMessage());
                    }
                });
            }
        });
    }
}
