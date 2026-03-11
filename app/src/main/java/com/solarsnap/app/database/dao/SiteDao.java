package com.solarsnap.app.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;
import androidx.room.OnConflictStrategy;

import com.solarsnap.app.database.entities.SiteEntity;

import java.util.List;

@Dao
public interface SiteDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(SiteEntity site);
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<SiteEntity> sites);
    
    @Update
    void update(SiteEntity site);
    
    @Delete
    void delete(SiteEntity site);
    
    @Query("SELECT * FROM sites ORDER BY site_name ASC")
    List<SiteEntity> getAllSites();
    
    @Query("SELECT * FROM sites WHERE site_id = :siteId")
    SiteEntity getSiteById(String siteId);
    
    @Query("SELECT COUNT(*) FROM sites")
    int getSiteCount();
    
    @Query("UPDATE sites SET last_synced = :timestamp WHERE site_id = :siteId")
    void updateLastSynced(String siteId, long timestamp);
    
    @Query("DELETE FROM sites")
    void deleteAll();
}
