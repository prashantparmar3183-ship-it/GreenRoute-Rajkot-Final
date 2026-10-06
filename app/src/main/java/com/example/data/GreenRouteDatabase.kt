package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        GarbagePointEntity::class,
        VehicleEntity::class,
        CollectionLogEntity::class,
        RouteMetadataEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class GreenRouteDatabase : RoomDatabase() {
    abstract fun dao(): GreenRouteDao

    companion object {
        @Volatile
        private var INSTANCE: GreenRouteDatabase? = null

        fun getDatabase(context: Context): GreenRouteDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GreenRouteDatabase::class.java,
                    "greenroute_rajkot.db"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
