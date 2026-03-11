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
    
    // Add item to upload queue
    public void addToQueue(int inspectionId, double fileSize, UploadCallback callback) {
        executor.execute(() -> {
            try {
                UploadQueueEntity upload = new UploadQueueEntity();
                upload.setInspectionId(inspectionId);
                upload.setFileSize(fileSize);
                upload.setStatus("pending");
                
                long id = uploadQueueDao.insert(upload);
                upload.setId((int) id);
                
                Log.d(TAG, "Added to upload queue: " + id);
                callback.onSuccess();
                
            } catch (Exception e) {
                callback.onError("Failed to add to queue: " + e.getMessage());
            }
        });
    }
    
    // Get all uploads
    public void getAllUploads(UploadListCallback callback) {
        executor.execute(() -> {
            List<UploadQueueEntity> uploads = uploadQueueDao.getAllUploads();
            callback.onSuccess(uploads);
        });
    }
    
    // Get uploads by status
    public void getUploadsByStatus(String status, UploadListCallback callback) {
        executor.execute(() -> {
            List<UploadQueueEntity> uploads = uploadQueueDao.getUploadsByStatus(status);
            callback.onSuccess(uploads);
        });
    }
    
    // Get sync status
    public void getSyncStatus(SyncStatusCallback callback) {
        executor.execute(() -> {
            try {
                int pending = uploadQueueDao.getUploadCountByStatus("pending");
                int uploading = uploadQueueDao.getUploadCountByStatus("uploading");
                int completed = uploadQueueDao.getUploadCountByStatus("uploaded");
                int failed = uploadQueueDao.getUploadCountByStatus("failed");
                
                callback.onSuccess(pending, uploading, completed, failed);
            } catch (Exception e) {
                callback.onError("Failed to get sync status: " + e.getMessage());
            }
        });
    }
    
    // Retry upload
    public void retryUpload(int uploadId, UploadCallback callback) {
        executor.execute(() -> {
            try {
                UploadQueueEntity upload = uploadQueueDao.getUploadById(uploadId);
                if (upload != null) {
                    upload.setStatus("pending");
                    upload.setErrorMessage(null);
                    uploadQueueDao.update(upload);
                    
                    Log.d(TAG, "Upload queued for retry: " + uploadId);
                    callback.onSuccess();
                } else {
                    callback.onError("Upload not found");
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
    
    // Clear completed uploads
    public void clearCompletedUploads(UploadCallback callback) {
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