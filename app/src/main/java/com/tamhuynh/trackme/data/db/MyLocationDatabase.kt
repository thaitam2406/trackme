
package com.tamhuynh.trackme

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

private const val DATABASE_NAME = "my-location-database"

/**
 * Database for storing all location data.
 */
@Database(entities = [MyLocationEntity::class, ListWorkoutRecordEntity::class], version = 2)
@TypeConverters(MyLocationTypeConverters::class)
abstract class MyLocationDatabase : RoomDatabase() {
    abstract fun locationDao(): MyLocationDao
    abstract fun listWorkoutDao(): ListWorkoutRecordDao

    companion object {
        // For Singleton instantiation
        @Volatile private var INSTANCE: MyLocationDatabase? = null

        fun getInstance(context: Context): MyLocationDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): MyLocationDatabase {
            return Room.databaseBuilder(
                    context,
                    MyLocationDatabase::class.java,
                    DATABASE_NAME
                ).build()
        }
    }
}
