package com.solarsnap.app.repository;

import android.content.Context;
import android.util.Log;

import com.solarsnap.app.database.AppDatabase;
import com.solarsnap.app.database.dao.InspectionDao;
import com.solarsnap.app.database.entities.InspectionEntity;
import com.solarsnap.app.network.ApiClient;
import com.solarsnap.app.network.SolarSnapApiService;
import com.solarsnap.app.network.models.InspectionRequest;
import com.google.gson.JsonObject;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class InspectionRepository {
    
    private static final String TAG = "InspectionRepository";
    private final SolarSnapApiService apiService;
    private final InspectionDao inspectionDao;
    private final Executor executor;
    
    public InspectionRepository(Context context) {
        this.apiService = ApiClient.getApiService(context);
        this.inspectionDao = AppDatabase.getInstance(context).inspectionDao();
        this.executor = Executors.newSingleThreadExecutor();
    }
    
    // Callback interfaces
    public interface InspectionCallback {
        void onSuccess(InspectionEntity inspection);
        void onError(String error);
    }
    
    public interface InspectionsCallback {
        void onSuccess(List<InspectionEntity> inspections);
        void onError(String error);
    }
    
    public interface InspectionListCallback {
        void onSuccess(List<InspectionEntity> inspections);
        void onError(String error);
    }
    
    public interface PanelInspectionCallback {
        void onSuccess(InspectionEntity latestInspection, List<InspectionEntity> allInspections);
        void onError(String error);
    }
    
    // Save inspection locally and queue for upload
    public void saveInspection(InspectionEntity inspection, InspectionCallback callback) {
        executor.execute(() -> {
            try {
                // Generate UUID if not set
                if (inspection.getInspectionUuid() == null) {
                    inspection.setInspectionUuid("insp_" + System.currentTimeMillis() + "_" + 
                        UUID.randomUUID().toString().substring(0, 8));
                }
                
                // Save to local database
                long id = inspectionDao.insert(inspection);
                inspection.setId((int) id);
                
                Log.d(TAG, "Inspection saved locally: " + inspection.getInspectionUuid());
                callback.onSuccess(inspection);
                
                // Try to upload immediately if online
                uploadInspection(inspection);
                
            } catch (Exception e) {
                Log.e(TAG, "Error saving inspection: " + e.getMessage());
                callback.onError("Failed to save inspection: " + e.getMessage());
            }
        });
    }
    
    // Upload inspection to server
    private void uploadInspection(InspectionEntity inspection) {
        InspectionRequest request = new InspectionRequest();
        request.setSiteId(inspection.getSiteId());
        request.setPanelId(inspection.getPanelId());
        request.setTemperature(inspection.getTemperature());
        request.setDeltaTemp(inspection.getDeltaTemp());
        request.setSeverity(inspection.getSeverity());
        request.setIssueType(inspection.getIssueType());
        request.setLatitude(inspection.getLatitude());
        request.setLongitude(inspection.getLongitude());
        request.setTimestamp(inspection.getTimestamp());
        request.setThermalImageUrl(inspection.getThermalImagePath());
        request.setVisualImageUrl(inspection.getVisualImagePath());
        
        apiService.createInspection(request).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful()) {
                    // Mark as uploaded
                    executor.execute(() -> {
                        inspectionDao.markAsUploaded(inspection.getId());
                        Log.d(TAG, "Inspection uploaded: " + inspection.getInspectionUuid());
                    });
                } else {
                    Log.w(TAG, "Failed to upload inspection: " + response.code());
                }
            }
            
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.w(TAG, "Upload failed, will retry later: " + t.getMessage());
            }
        });
    }
    
    // Get all inspections from database
    public void getAllInspections(InspectionsCallback callback) {
        executor.execute(() -> {
            List<InspectionEntity> inspections = inspectionDao.getAllInspections();
            callback.onSuccess(inspections);
        });
    }
    
    // Get inspections by site
    public void getInspectionsBySite(String siteId, InspectionsCallback callback) {
        executor.execute(() -> {
            List<InspectionEntity> inspections = inspectionDao.getInspectionsBySite(siteId);
            callback.onSuccess(inspections);
        });
    }
    
    // Get inspections by severity
    public void getInspectionsBySeverity(String severity, InspectionsCallback callback) {
        executor.execute(() -> {
            List<InspectionEntity> inspections = inspectionDao.getInspectionsBySeverity(severity);
            callback.onSuccess(inspections);
        });
    }
    
    // Get pending inspections (not uploaded)
    public void getPendingInspections(InspectionsCallback callback) {
        executor.execute(() -> {
            List<InspectionEntity> inspections = inspectionDao.getPendingInspections();
            callback.onSuccess(inspections);
        });
    }
    
    // Delete inspection
    public void deleteInspection(InspectionEntity inspection, InspectionCallback callback) {
        executor.execute(() -> {
            try {
                inspectionDao.delete(inspection);
                Log.d(TAG, "Inspection deleted: " + inspection.getInspectionUuid());
                callback.onSuccess(inspection);
            } catch (Exception e) {
                callback.onError("Failed to delete inspection: " + e.getMessage());
            }
        });
    }
    
    // Get inspection statistics
    public interface StatisticsCallback {
        void onSuccess(int total, int critical, int warning, int healthy);
        void onError(String error);
    }
    
    public void getStatistics(String siteId, StatisticsCallback callback) {
        executor.execute(() -> {
            try {
                int total = inspectionDao.getInspectionCountBySite(siteId);
                int critical = inspectionDao.getInspectionCountBySeverity("CRITICAL");
                int warning = inspectionDao.getInspectionCountBySeverity("WARNING");
                int healthy = inspectionDao.getInspectionCountBySeverity("HEALTHY");
                
                callback.onSuccess(total, critical, warning, healthy);
            } catch (Exception e) {
                callback.onError("Failed to get statistics: " + e.getMessage());
            }
        });
    }
    
    // Get inspection data for a specific panel
    public void getPanelInspections(String panelId, PanelInspectionCallback callback) {
        // First try to get from local database
        executor.execute(() -> {
            List<InspectionEntity> localInspections = inspectionDao.getInspectionsByPanel(panelId);
            if (!localInspections.isEmpty()) {
                InspectionEntity latest = localInspections.get(0); // Assuming ordered by timestamp desc
                callback.onSuccess(latest, localInspections);
            }
        });
        
        // Then fetch from API
        apiService.getPanelInspections(panelId).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject jsonResponse = response.body();
                    
                    if (jsonResponse.has("success") && jsonResponse.get("success").getAsBoolean()) {
                        try {
                            // Parse latest inspection
                            InspectionEntity latestInspection = null;
                            if (jsonResponse.has("latest") && !jsonResponse.get("latest").isJsonNull()) {
                                JsonObject latestData = jsonResponse.getAsJsonObject("latest");
                                latestInspection = parseInspectionFromJson(latestData);
                            }
                            
                            // Parse all inspections
                            List<InspectionEntity> allInspections = new java.util.ArrayList<>();
                            if (jsonResponse.has("inspections") && jsonResponse.get("inspections").isJsonArray()) {
                                jsonResponse.getAsJsonArray("inspections").forEach(element -> {
                                    JsonObject inspectionObj = element.getAsJsonObject();
                                    InspectionEntity inspection = parseInspectionFromJson(inspectionObj);
                                    allInspections.add(inspection);
                                });
                            }
                            
                            // Cache in local database
                            executor.execute(() -> {
                                for (InspectionEntity inspection : allInspections) {
                                    inspectionDao.insertOrUpdate(inspection);
                                }
                            });
                            
                            callback.onSuccess(latestInspection, allInspections);
                            
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing panel inspections: " + e.getMessage());
                            callback.onError("Error parsing inspection data");
                        }
                    } else {
                        callback.onError("Failed to fetch panel inspections");
                    }
                } else {
                    callback.onError("API error: " + response.code());
                }
            }
            
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "Error fetching panel inspections: " + t.getMessage());
                
                // Return cached data on network error
                executor.execute(() -> {
                    List<InspectionEntity> localInspections = inspectionDao.getInspectionsByPanel(panelId);
                    if (!localInspections.isEmpty()) {
                        InspectionEntity latest = localInspections.get(0);
                        callback.onSuccess(latest, localInspections);
                    } else {
                        callback.onError("Network error: " + t.getMessage());
                    }
                });
            }
        });
    }
    
    private InspectionEntity parseInspectionFromJson(JsonObject jsonData) {
        InspectionEntity inspection = new InspectionEntity();
        
        if (jsonData.has("inspectionId")) {
            inspection.setInspectionUuid(jsonData.get("inspectionId").getAsString());
        }
        if (jsonData.has("panelId")) {
            inspection.setPanelId(jsonData.get("panelId").getAsString());
        }
        if (jsonData.has("siteId")) {
            inspection.setSiteId(jsonData.get("siteId").getAsString());
        }
        if (jsonData.has("temperature")) {
            inspection.setTemperature(jsonData.get("temperature").getAsFloat());
        }
        if (jsonData.has("deltaTemp")) {
            inspection.setDeltaTemp(jsonData.get("deltaTemp").getAsFloat());
        }
        if (jsonData.has("severity")) {
            inspection.setSeverity(jsonData.get("severity").getAsString());
        }
        if (jsonData.has("issueType")) {
            inspection.setIssueType(jsonData.get("issueType").getAsString());
        }
        if (jsonData.has("thermalImageUrl")) {
            inspection.setThermalImagePath(jsonData.get("thermalImageUrl").getAsString());
        }
        if (jsonData.has("visualImageUrl")) {
            inspection.setVisualImagePath(jsonData.get("visualImageUrl").getAsString());
        }
        if (jsonData.has("timestamp")) {
            inspection.setTimestamp(jsonData.get("timestamp").getAsLong());
        }
        if (jsonData.has("latitude")) {
            inspection.setLatitude(jsonData.get("latitude").getAsDouble());
        }
        if (jsonData.has("longitude")) {
            inspection.setLongitude(jsonData.get("longitude").getAsDouble());
        }
        
        return inspection;
    }
}