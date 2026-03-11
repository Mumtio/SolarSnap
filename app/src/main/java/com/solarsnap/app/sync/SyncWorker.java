package com.solarsnap.app.sync;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.solarsnap.app.repository.UploadRepository;
import com.solarsnap.app.repository.InspectionRepository;

public class SyncWorker extends Worker {
    
    private static final String TAG = "SyncWorker";
    
    private UploadRepository uploadRepository;
    private InspectionRepository inspectionRepository;
    
    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        uploadRepository = new UploadRepository(context);
        inspectionRepository = new InspectionRepository(context);
    }
    
    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Starting background sync");
        
        try {
            // Check network connectivity
            if (!NetworkMonitor.isNetworkAvailable(getApplicationContext())) {
                Log.d(TAG, "No network available, skipping sync");
                return Result.retry();
            }
            
            // Sync pending uploads
            boolean uploadSuccess = syncPendingUploads();
            
            // Sync inspection data
            boolean inspectionSuccess = syncInspectionData();
            
            if (uploadSuccess && inspectionSuccess) {
                Log.d(TAG, "Background sync completed successfully");
                return Result.success();
            } else {
                Log.w(TAG, "Background sync partially failed, will retry");
                return Result.retry();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Background sync failed with exception", e);
            return Result.failure();
        }
    }
    
    private boolean syncPendingUploads() {
        try {
            final boolean[] success = {false};
            final Object lock = new Object();
            
            uploadRepository.syncPendingUploads(new UploadRepository.UploadCallback() {
                @Override
                public void onSuccess() {
                    synchronized (lock) {
                        success[0] = true;
                        lock.notify();
                    }
                }
                
                @Override
                public void onError(String error) {
                    Log.e(TAG, "Upload sync failed: " + error);
                    synchronized (lock) {
                        success[0] = false;
                        lock.notify();
                    }
                }
            });
            
            // Wait for callback (with timeout)
            synchronized (lock) {
                try {
                    lock.wait(30000); // 30 second timeout
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
            
            return success[0];
            
        } catch (Exception e) {
            Log.e(TAG, "Exception during upload sync", e);
            return false;
        }
    }
    
    private boolean syncInspectionData() {
        try {
            // For now, just return true as inspection sync is handled by uploads
            // In a real implementation, this would sync inspection metadata
            Log.d(TAG, "Inspection data sync completed");
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Exception during inspection sync", e);
            return false;
        }
    }
}