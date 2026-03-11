package com.solarsnap.app.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;

import com.solarsnap.app.database.entities.InspectionEntity;

import java.util.List;

@Dao
public interface InspectionDao {
    
    @Insert
    long insert(InspectionEntity inspection);
    
    @Update
    void update(InspectionEntity inspection);
    
    @Delete
    void delete(InspectionEntity inspection);
    
    @Query("SELECT * FROM inspections ORDER BY timestamp DESC")
    List<InspectionEntity> getAllInspections();
    
    @Query("SELECT * FROM inspections WHERE site_id = :siteId ORDER BY timestamp DESC")
    List<InspectionEntity> getInspectionsBySite(String siteId);
    
    @Query("SELECT * FROM inspections WHERE severity = :severity ORDER BY timestamp DESC")
    List<InspectionEntity> getInspectionsBySeverity(String severity);
    
    @Query("SELECT * FROM inspections WHERE uploaded = 0 ORDER BY timestamp DESC")
    List<InspectionEntity> getPendingInspections();
    
    @Query("SELECT * FROM inspections WHERE id = :id")
    InspectionEntity getInspectionById(int id);
    
    @Query("SELECT * FROM inspections WHERE inspection_uuid = :uuid")
    InspectionEntity getInspectionByUuid(String uuid);
    
    @Query("SELECT * FROM inspections WHERE site_id = :siteId AND timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    List<InspectionEntity> getInspectionsByDateRange(String siteId, long startTime, long endTime);
    
    @Query("SELECT COUNT(*) FROM inspections WHERE site_id = :siteId")
    int getInspectionCountBySite(String siteId);
    
    @Query("SELECT COUNT(*) FROM inspections WHERE severity = :severity")
    int getInspectionCountBySeverity(String severity);
    
    @Query("UPDATE inspections SET uploaded = 1 WHERE id = :id")
    void markAsUploaded(int id);
    
    @Query("SELECT * FROM inspections WHERE panel_id = :panelId ORDER BY timestamp DESC")
    List<InspectionEntity> getInspectionsByPanel(String panelId);
    
    @Insert
    void insertOrUpdate(InspectionEntity inspection);
    
    @Query("DELETE FROM inspections WHERE uploaded = 1 AND timestamp < :olderThan")
    void deleteOldUploadedInspections(long olderThan);
}
