package com.solarsnap.app.network;

import com.solarsnap.app.network.models.*;
import com.google.gson.JsonObject;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.*;

public interface SolarSnapApiService {
    
    // ==================== Authentication Endpoints ====================
    
    @POST("auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);
    
    @POST("auth/register")
    Call<JsonObject> register(@Body JsonObject request);
    
    @POST("auth/refresh")
    Call<JsonObject> refreshToken(@Body JsonObject request);
    
    @POST("auth/logout")
    Call<JsonObject> logout();
    
    @GET("auth/me")
    Call<JsonObject> getCurrentUser();
    
    // ==================== Sites Endpoints ====================
    
    @GET("sites")
    Call<SitesResponse> getSites();
    
    @GET("sites/{siteId}")
    Call<JsonObject> getSiteDetails(@Path("siteId") String siteId);
    
    @GET("sites/{siteId}/panels")
    Call<JsonObject> getSitePanels(@Path("siteId") String siteId, @Query("page") int page, @Query("per_page") int perPage);
    
    @POST("sites")
    Call<JsonObject> createSite(@Body JsonObject request);
    
    // ==================== Inspections Endpoints ====================
    
    @POST("inspections")
    Call<JsonObject> createInspection(@Body InspectionRequest request);
    
    @GET("inspections")
    Call<JsonObject> getInspections(
        @Query("siteId") String siteId,
        @Query("severity") String severity,
        @Query("limit") Integer limit,
        @Query("offset") Integer offset
    );
    
    @GET("inspections/{id}")
    Call<JsonObject> getInspectionDetails(@Path("id") String id);
    
    @GET("inspections/panel/{panelId}")
    Call<JsonObject> getPanelInspections(@Path("panelId") String panelId);
    
    @DELETE("inspections/{id}")
    Call<JsonObject> deleteInspection(@Path("id") String id);
    
    @GET("inspections/statistics")
    Call<JsonObject> getInspectionStatistics();
    
    // ==================== Upload Endpoints ====================
    
    @Multipart
    @POST("upload/thermal")
    Call<ResponseBody> uploadThermalImage(
        @Part("panelId") RequestBody panelId,
        @Part MultipartBody.Part image
    );
    
    @Multipart
    @POST("upload/visual")
    Call<ResponseBody> uploadVisualImage(
        @Part("panelId") RequestBody panelId,
        @Part MultipartBody.Part image
    );
    
    @POST("upload/batch")
    Call<JsonObject> uploadBatch(@Body JsonObject request);
    
    // ==================== Sync Endpoints ====================
    
    @GET("sync/status")
    Call<JsonObject> getSyncStatus();
    
    @GET("sync/queue")
    Call<JsonObject> getSyncQueue(@Query("status") String status);
    
    @POST("sync/retry/{uploadId}")
    Call<JsonObject> retryUpload(@Path("uploadId") int uploadId);
    
    @POST("sync/create")
    Call<JsonObject> createUploadQueue(@Body JsonObject request);
    
    @POST("sync/clear-completed")
    Call<JsonObject> clearCompletedUploads();
    
    @GET("sync/device-storage")
    Call<JsonObject> getDeviceStorage();
    
    // ==================== Reports Endpoints ====================
    
    @GET("reports/site/{siteId}")
    Call<JsonObject> getSiteReport(
        @Path("siteId") String siteId,
        @Query("startDate") Long startDate,
        @Query("endDate") Long endDate
    );
    
    @GET("reports/fault")
    Call<JsonObject> getFaultReport(
        @Query("siteId") String siteId,
        @Query("severity") String severity
    );
    
    @GET("reports/maintenance")
    Call<JsonObject> getMaintenanceReport(@Query("siteId") String siteId);
    
    @POST("reports/export")
    Call<JsonObject> exportReport(@Body JsonObject request);
    
    @POST("reports/generate")
    Call<JsonObject> generateReport(@Body JsonObject request);
    
    @POST("reports/export-data")
    Call<JsonObject> exportData(@Body JsonObject request);
    
    @POST("reports/cloud-sync")
    Call<JsonObject> syncToCloud(@Body JsonObject request);
    
    @POST("reports/export-history")
    Call<JsonObject> exportHistory(@Body JsonObject request);
    
    @DELETE("reports/delete-record")
    Call<JsonObject> deleteInspectionRecord(@Body JsonObject request);
    
    @GET("reports/temperature-distribution")
    Call<JsonObject> getTemperatureDistribution(@Query("siteId") String siteId);
    
    // ==================== Settings Endpoints ====================
    
    @GET("settings/user")
    Call<JsonObject> getUserSettings();
    
    @PUT("settings/user")
    Call<JsonObject> updateUserSettings(@Body JsonObject request);
    
    @GET("settings/company/{companyId}")
    Call<JsonObject> getCompanySettings(@Path("companyId") String companyId);
}
