package com.solarsnap.app.repository;

import android.content.Context;
import android.util.Log;

import com.solarsnap.app.database.AppDatabase;
import com.solarsnap.app.database.dao.UploadQueueDao;
import com.solarsnap.app.database.entities.UploadQueueEntity;
import com.solarsnap.app.network.ApiClient;
import com.solarsnap.app.network.SolarSnapApiService;
import com.google.gson.JsonObject;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UploadRepository {
    
    private static final String TAG = "UploadRepository";
    private final SolarSnapApiService apiService;
    private final UploadQueueDao uploadQueueDao;
    private final Executor executor;
    
    public UploadRepository(Context context) {
        this.apiService = ApiClient.getApiService(context);
        this.uploadQueueDao = AppDatabase.getInstance(context).uploadQueueDao();
        this.executor = Executors.newSingleThreadExecutor();
    }
    
    // Callback interfaces
    public interface UploadCallback {
        void onSuccess();
        void onError(String error);
    }
    
    public interface UploadListCallback {
        void onSuccess(List<UploadQueueEntity> uploads);
        void onError(String error);
    }
    
    public interface SyncStatusCallback {
        void onSuccess(int pending, int uploading, int completed, int failed);
        void onError(String error);
    }
    
    // Add item to upload queue with inspection details
    public void addToQueue(String inspectionUuid, String panelId, String siteId, 
                          String fileType, String filePath, double fileSize, UploadCallback callback) {
        executor.execute(() -> {
            try {
                UploadQueueEntity upload = new UploadQueueEntity();
                upload.setInspectionUuid(inspectionUuid);
                upload.setPanelId(panelId);
                upload.setSiteId(siteId);
                upload.setFileType(fileType);
                upload.setFilePath(filePath);
                upload.setFileSize(fileSize);
                upload.setStatus("pending");
                
                long id = uploadQueueDao.insert(upload);
                upload.setId((int) id);
                
                // Also create on backend
                createBackendUploadQueue(upload, callback);
                
            } catch (Exception e) {
                callback.onError("Failed to add to queue: " + e.getMessage());
            }
        });
    }
    
    private void createBackendUploadQueue(UploadQueueEntity upload, UploadCallback callback) {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("inspectionId", upload.getInspectionUuid());
        requestBody.addProperty("fileSize", upload.getFileSize());
        
        apiService.createUploadQueue(requestBody).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject result = response.body();
                    if (result.has("success") && result.get("success").getAsBoolean()) {
                        // Update local record with backend ID
                        executor.execute(() -> {
                            upload.setBackendUploadId(result.get("uploadId").getAsString());
                            uploadQueueDao.update(upload);
                        });
                        Log.d(TAG, "Upload queue created on backend: " + result.get("uploadId").getAsString());
                        callback.onSuccess();
                    } else {
                        Log.w(TAG, "Backend upload queue creation failed, keeping local only");
                        callback.onSuccess(); // Still success for local
                    }
                } else {
                    Log.w(TAG, "Backend upload queue creation failed, keeping local only");
                    callback.onSuccess(); // Still success for local
                }
            }
            
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.w(TAG, "Backend upload queue creation failed: " + t.getMessage());
                callback.onSuccess(); // Still success for local
            }
        });
    }
    
    // Get all uploads with backend sync
    public void getAllUploads(UploadListCallback callback) {
        // First get local data
        executor.execute(() -> {
            List<UploadQueueEntity> localUploads = uploadQueueDao.getAllUploads();
            
            // Sync with backend to get latest status
            syncWithBackend(new UploadCallback() {
                @Override
                public void onSuccess() {
                    // Get updated local data after sync
                    List<UploadQueueEntity> updatedUploads = uploadQueueDao.getAllUploads();
                    callback.onSuccess(updatedUploads);
                }
                
                @Override
                public void onError(String error) {
                    // Return local data even if sync fails
                    Log.w(TAG, "Backend sync failed, using local data: " + error);
                    callback.onSuccess(localUploads);
                }
            });
        });
    }
    
    // Sync with backend
    private void syncWithBackend(UploadCallback callback) {
        apiService.getSyncQueue("all").enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject result = response.body();
                    if (result.has("success") && result.get("success").getAsBoolean()) {
                        // Update local database with backend data
                        updateLocalFromBackend(result, callback);
                    } else {
                        callback.onError("Backend sync failed");
                    }
                } else {
                    callback.onError("Backend sync error: " + response.code());
                }
            }
            
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }
    
    private void updateLocalFromBackend(JsonObject result, UploadCallback callback) {
        executor.execute(() -> {
            try {
                if (result.has("queue")) {
                    // Process backend queue data and update local database
                    // This would involve parsing the JSON and updating local records
                    Log.d(TAG, "Backend sync completed");
                }
                callback.onSuccess();
            } catch (Exception e) {
                callback.onError("Failed to update local data: " + e.getMessage());
            }
        });
    }
    
    // Get uploads by status
    public void getUploadsByStatus(String status, UploadListCallback callback) {
        executor.execute(() -> {
            List<UploadQueueEntity> uploads = uploadQueueDao.getUploadsByStatus(status);
            callback.onSuccess(uploads);
        });
    }
    
    // Get sync status from backend
    public void getSyncStatusFromBackend(SyncStatusCallback callback) {
        apiService.getSyncStatus().enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject result = response.body();
                    if (result.has("success") && result.get("success").getAsBoolean()) {
                        JsonObject syncStatus = result.getAsJsonObject("syncStatus");
                        
                        int pending = syncStatus.get("pending").getAsInt();
                        int uploading = syncStatus.get("uploading").getAsInt();
                        int completed = syncStatus.get("completed").getAsInt();
                        int failed = syncStatus.get("failed").getAsInt();
                        
                        callback.onSuccess(pending, uploading, completed, failed);
                    } else {
                        callback.onError("Backend sync status failed");
                    }
                } else {
                    callback.onError("Server error: " + response.code());
                }
            }
            
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                // Fallback to local status
                getSyncStatus(callback);
            }
        });
    }
    
    // Get sync status (local fallback)
    public void getSyncStatus(SyncStatusCallback callback) {
        executor.execute(() -> {
            try {
                int pending = uploadQueueDao.getUploadCountByStatus("pending");
                int uploading = uploadQueueDao.getUploadCountByStatus("uploading");
                int completed = uploadQueueDao.getUploadCountByStatus("completed");
                int failed = uploadQueueDao.getUploadCountByStatus("failed");
                
                callback.onSuccess(pending, uploading, completed, failed);
            } catch (Exception e) {
                callback.onError("Failed to get sync status: " + e.getMessage());
            }
        });
    }
    
    // Retry upload with backend API
    public void retryUpload(int uploadId, UploadCallback callback) {
        executor.execute(() -> {
            try {
                UploadQueueEntity upload = uploadQueueDao.getUploadById(uploadId);
                if (upload != null && upload.getBackendUploadId() != null) {
                    // Call backend retry API
                    String backendId = upload.getBackendUploadId().replace("upload_", "");
                    
                    apiService.retryUpload(Integer.parseInt(backendId)).enqueue(new Callback<JsonObject>() {
                        @Override
                        public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                JsonObject result = response.body();
                                if (result.has("success") && result.get("success").getAsBoolean()) {
                                    // Update local status
                                    executor.execute(() -> {
                                        upload.setStatus("uploading");
                                        upload.setErrorMessage(null);
                                        upload.setLastAttemptAt(System.currentTimeMillis());
                                        uploadQueueDao.update(upload);
                                    });
                                    callback.onSuccess();
                                } else {
                                    callback.onError("Backend retry failed");
                                }
                            } else {
                                callback.onError("Server error: " + response.code());
                            }
                        }
                        
                        @Override
                        public void onFailure(Call<JsonObject> call, Throwable t) {
                            callback.onError("Network error: " + t.getMessage());
                        }
                    });
                } else {
                    // Fallback to local retry
                    if (upload != null) {
                        upload.setStatus("pending");
                        upload.setErrorMessage(null);
                        uploadQueueDao.update(upload);
                        Log.d(TAG, "Upload queued for retry (local): " + uploadId);
                        callback.onSuccess();
                    } else {
                        callback.onError("Upload not found");
                    }
                }
            } catch (Exception e) {
                callback.onError("Failed to retry upload: " + e.getMessage());
            }
        });
    }
    
    // Delete upload
    public void deleteUpload(int uploadId, UploadCallback callback) {
        executor.execute(() -> {
            try {
                UploadQueueEntity entity = uploadQueueDao.getUploadById(uploadId);
                if (entity != null) {
                    uploadQueueDao.delete(entity);
                }
                Log.d(TAG, "Upload deleted: " + uploadId);
                callback.onSuccess();
            } catch (Exception e) {
                callback.onError("Failed to delete upload: " + e.getMessage());
            }
        });
    }
    
    // Cancel upload
    public void cancelUpload(int uploadId, UploadCallback callback) {
        executor.execute(() -> {
            try {
                UploadQueueEntity upload = uploadQueueDao.getUploadById(uploadId);
                if (upload != null) {
                    upload.setStatus("cancelled");
                    uploadQueueDao.update(upload);
                    
                    Log.d(TAG, "Upload cancelled: " + uploadId);
                    callback.onSuccess();
                } else {
                    callback.onError("Upload not found");
                }
            } catch (Exception e) {
                callback.onError("Failed to cancel upload: " + e.getMessage());
            }
        });
    }
    
    // Retry failed uploads
    public void retryFailedUploads(UploadCallback callback) {
        executor.execute(() -> {
            try {
                List<UploadQueueEntity> failedUploads = uploadQueueDao.getUploadsByStatus("failed");
                for (UploadQueueEntity upload : failedUploads) {
                    upload.setStatus("pending");
                    upload.setErrorMessage(null);
                    uploadQueueDao.update(upload);
                }
                
                Log.d(TAG, "Retrying " + failedUploads.size() + " failed uploads");
                callback.onSuccess();
            } catch (Exception e) {
                callback.onError("Failed to retry failed uploads: " + e.getMessage());
            }
        });
    }
    
    // Clear completed uploads with backend sync
    public void clearCompletedUploads(UploadCallback callback) {
        // Call backend API first
        apiService.clearCompletedUploads().enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject result = response.body();
                    if (result.has("success") && result.get("success").getAsBoolean()) {
                        // Clear local completed uploads
                        executor.execute(() -> {
                            try {
                                uploadQueueDao.deleteCompletedUploads();
                                Log.d(TAG, "Completed uploads cleared (backend + local)");
                                callback.onSuccess();
                            } catch (Exception e) {
                                callback.onError("Failed to clear local uploads: " + e.getMessage());
                            }
                        });
                    } else {
                        callback.onError("Backend clear failed");
                    }
                } else {
                    // Fallback to local clear only
                    executor.execute(() -> {
                        try {
                            uploadQueueDao.deleteCompletedUploads();
                            Log.d(TAG, "Completed uploads cleared (local only)");
                            callback.onSuccess();
                        } catch (Exception e) {
                            callback.onError("Failed to clear uploads: " + e.getMessage());
                        }
                    });
                }
            }
            
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                // Fallback to local clear only
                executor.execute(() -> {
                    try {
                        uploadQueueDao.deleteCompletedUploads();
                        Log.d(TAG, "Completed uploads cleared (local only, network failed)");
                        callback.onSuccess();
                    } catch (Exception e) {
                        callback.onError("Failed to clear uploads: " + e.getMessage());
                    }
                });
            }
        });
    }
    
    // Sync pending uploads (called by SyncWorker)
    public void syncPendingUploads(UploadCallback callback) {
        executor.execute(() -> {
            try {
                List<UploadQueueEntity> pendingUploads = uploadQueueDao.getUploadsByStatus("pending");
                
                if (pendingUploads.isEmpty()) {
                    Log.d(TAG, "No pending uploads to sync");
                    callback.onSuccess();
                    return;
                }
                
                Log.d(TAG, "Syncing " + pendingUploads.size() + " pending uploads");
                
                // Update status to uploading
                for (UploadQueueEntity upload : pendingUploads) {
                    upload.setStatus("uploading");
                    uploadQueueDao.update(upload);
                }
                
                // TODO: Implement actual upload logic here
                // For now, simulate successful upload
                for (UploadQueueEntity upload : pendingUploads) {
                    upload.setStatus("completed");
                    upload.setLastAttemptAt(System.currentTimeMillis());
                    uploadQueueDao.update(upload);
                }
                
                callback.onSuccess();
                
            } catch (Exception e) {
                callback.onError("Failed to sync uploads: " + e.getMessage());
            }
        });
    }
    
    // Clear completed uploads
    public void clearCompleted(UploadCallback callback) {
        executor.execute(() -> {
            try {
                uploadQueueDao.deleteCompletedUploads();
                Log.d(TAG, "Completed uploads cleared");
                callback.onSuccess();
            } catch (Exception e) {
                callback.onError("Failed to clear uploads: " + e.getMessage());
            }
        });
    }
    
    // Update upload status
    public void updateUploadStatus(int uploadId, String status, String errorMessage) {
        executor.execute(() -> {
            try {
                UploadQueueEntity upload = uploadQueueDao.getUploadById(uploadId);
                if (upload != null) {
                    upload.setStatus(status);
                    upload.setErrorMessage(errorMessage);
                    upload.setLastAttemptAt(System.currentTimeMillis());
                    
                    if ("failed".equals(status)) {
                        upload.setRetryCount(upload.getRetryCount() + 1);
                    }
                    
                    uploadQueueDao.update(upload);
                    Log.d(TAG, "Upload status updated: " + uploadId + " -> " + status);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to update upload status: " + e.getMessage());
            }
        });
    }
}