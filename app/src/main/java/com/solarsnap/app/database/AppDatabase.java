package com.solarsnap.app.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.solarsnap.app.database.dao.InspectionDao;
import com.solarsnap.app.database.dao.PanelDao;
import com.solarsnap.app.database.dao.SiteDao;
import com.solarsnap.app.database.dao.UploadQueueDao;
import com.solarsnap.app.database.entities.InspectionEntity;
import com.solarsnap.app.database.entities.PanelEntity;
import com.solarsnap.app.database.entities.SiteEntity;
import com.solarsnap.app.database.entities.UploadQueueEntity;

@Database(
    entities = {
        InspectionEntity.class,
        SiteEntity.class,
        PanelEntity.class,
        UploadQueueEntity.class
    },
    version = 2,
    exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {
    
    private static final String DATABASE_NAME = "solarsnap_db";
    private static AppDatabase instance;
    
    // Abstract methods for DAOs
    public abstract InspectionDao inspectionDao();
    public abstract SiteDao siteDao();
    public abstract PanelDao panelDao();
    public abstract UploadQueueDao uploadQueueDao();
    
    // Singleton pattern
    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                context.getApplicationContext(),
                AppDatabase.class,
                DATABASE_NAME
            )
            .fallbackToDestructiveMigration() // For development only
            .build();
        }
        return instance;
    }
    
    // For testing purposes
    public static void destroyInstance() {
        instance = null;
    }
}
