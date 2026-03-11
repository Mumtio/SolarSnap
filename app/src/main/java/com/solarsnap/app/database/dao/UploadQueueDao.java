package com.solarsnap.app.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;

import com.solarsnap.app.database.entities.UploadQueueEntity;

import java.util.List;

@Dao
public interface UploadQueueDao {
    
    @Insert
    long insert(UploadQueueEntity uploadQueue);
    
    @Update
    void update(UploadQueueEntity uploadQueue);
    
    @Delete
    void delete(UploadQueueEntity uploadQueue);
    
    @Query("SELECT * FROM upload_queue ORDER BY created_at DESC")
    List<UploadQueueEntity> getAllUploads();
    
    @Query("SELECT * FROM upload_queue WHERE status = :status ORDER BY created_at DESC")
    List<UploadQueueEntity> getUploadsByStatus(String status);
    
    @Query("SELECT * FROM upload_queue WHERE id = :id")
    UploadQueueEntity getUploadById(int id);
    
    @Query("SELECT COUNT(*) FROM upload_queue WHERE status = :status")
    int getUploadCountByStatus(String status);
    
    @Query("UPDATE upload_queue SET status = :status, last_attempt_at = :timestamp WHERE id = :id")
    void updateUploadStatus(int id, String status, long timestamp);
    
    @Query("UPDATE upload_queue SET retry_count = retry_count + 1, last_attempt_at = :timestamp WHERE id = :id")
    void incrementRetryCount(int id, long timestamp);
    
    @Query("DELETE FROM upload_queue WHERE status = 'uploaded'")
    void deleteCompletedUploads();
    
    @Query("DELETE FROM upload_queue WHERE status = 'uploaded' AND created_at < :olderThan")
    void deleteOldCompletedUploads(long olderThan);
}
