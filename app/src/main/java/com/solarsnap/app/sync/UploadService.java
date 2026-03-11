package com.solarsnap.app.sync;

import android.content.Context;
import android.util.Log;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;
import java.util.concurrent.TimeUnit;

public class UploadService {
    
    private static final String TAG = "UploadService";
    private static final String PERIODIC_SYNC_WORK = "periodic_sync_work";
    private static final String IMMEDIATE_SYNC_WORK = "immediate_sync_work";
    
    private Context context;
    private WorkManager workManager;
    
    public UploadService(Context context) {
        this.context = context;
        this.workManager = WorkManager.getInstance(context);
    }
    
    /**
     * Start periodic background sync
     */
    public void startPeriodicSync() {
        Log.d(TAG, "Starting periodic sync service");
        
        // Create constraints - only run when network is available
        Constraints constraints = new Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build();
        
        // Create periodic work request - runs every 15 minutes
        PeriodicWorkRequest periodicWorkRequest = new PeriodicWorkRequest.Builder(
            SyncWorker.class, 15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .addTag("sync")
            .build();
        
        // Enqueue the work, replacing any existing periodic sync
        workManager.enqueueUniquePeriodicWork(
            PERIODIC_SYNC_WORK,
            ExistingPeriodicWorkPolicy.REPLACE,
            periodicWorkRequest
        );
        
        Log.d(TAG, "Periodic sync scheduled every 15 minutes");
    }
    
    /**
     * Stop periodic background sync
     */
    public void stopPeriodicSync() {
        Log.d(TAG, "Stopping periodic sync service");
        workManager.cancelUniqueWork(PERIODIC_SYNC_WORK);
    }
    
    /**
     * Trigger immediate sync
     */
    public void syncNow() {
        Log.d(TAG, "Triggering immediate sync");
        
        // Create constraints for immediate sync
        Constraints constraints = new Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build();
        
        // Create one-time work request
        OneTimeWorkRequest immediateWorkRequest = new OneTimeWorkRequest.Builder(SyncWorker.class)
            .setConstraints(constraints)
            .addTag("sync")
            .addTag("immediate")
            .build();
        
        // Enqueue the work
        workManager.enqueue(immediateWorkRequest);
        
        Log.d(TAG, "Immediate sync queued");
    }
    
    /**
     * Sync only when on WiFi (for large uploads)
     */
    public void syncOnWiFiOnly() {
        Log.d(TAG, "Triggering WiFi-only sync");
        
        // Create constraints - only WiFi
        Constraints constraints = new Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED) // WiFi or Ethernet
            .setRequiresBatteryNotLow(true)
            .build();
        
        // Create one-time work request
        OneTimeWorkRequest wifiWorkRequest = new OneTimeWorkRequest.Builder(SyncWorker.class)
            .setConstraints(constraints)
            .addTag("sync")
            .addTag("wifi-only")
            .build();
        
        // Enqueue the work
        workManager.enqueue(wifiWorkRequest);
        
        Log.d(TAG, "WiFi-only sync queued");
    }
    
    /**
     * Cancel all sync work
     */
    public void cancelAllSync() {
        Log.d(TAG, "Cancelling all sync work");
        workManager.cancelAllWorkByTag("sync");
    }
    
    /**
     * Get sync work status (commented out due to observeForever incompatibility)
     */
    public void getSyncStatus() {
        // Note: observeForever requires a LifecycleOwner context
        // This method should be called from an Activity/Fragment
        // workManager.getWorkInfosByTag("sync").observeForever(workInfos -> {
        //     Log.d(TAG, "Active sync jobs: " + workInfos.size());
        //     for (int i = 0; i < workInfos.size(); i++) {
        //         Log.d(TAG, "Job " + i + ": " + workInfos.get(i).getState());
        //     }
        // });
        Log.d(TAG, "getSyncStatus called - use WorkManager.getWorkInfosByTag from Activity");
    }
}