package com.solarsnap.app.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;
import androidx.room.OnConflictStrategy;

import com.solarsnap.app.database.entities.PanelEntity;

import java.util.List;

@Dao
public interface PanelDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(PanelEntity panel);
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<PanelEntity> panels);
    
    @Update
    void update(PanelEntity panel);
    
    @Delete
    void delete(PanelEntity panel);
    
    @Query("SELECT * FROM panels WHERE site_id = :siteId ORDER BY row_number, column_number")
    List<PanelEntity> getPanelsBySite(String siteId);
    
    @Query("SELECT * FROM panels WHERE panel_id = :panelId")
    PanelEntity getPanelById(String panelId);
    
    @Query("SELECT * FROM panels WHERE site_id = :siteId AND status = :status")
    List<PanelEntity> getPanelsByStatus(String siteId, String status);
    
    @Query("SELECT COUNT(*) FROM panels WHERE site_id = :siteId")
    int getPanelCountBySite(String siteId);
    
    @Query("SELECT COUNT(*) FROM panels WHERE site_id = :siteId AND status = :status")
    int getPanelCountByStatus(String siteId, String status);
    
    @Query("UPDATE panels SET status = :status, last_inspection = :timestamp WHERE panel_id = :panelId")
    void updatePanelStatus(String panelId, String status, long timestamp);
    
    @Query("DELETE FROM panels WHERE site_id = :siteId")
    void deletePanelsBySite(String siteId);
}
