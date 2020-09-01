
package com.tamhuynh.trackme.data

import android.content.Context
import androidx.annotation.MainThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.tamhuynh.trackme.WorkoutRecordData
import com.tamhuynh.trackme.ListWorkoutRecordEntity
import com.tamhuynh.trackme.MyLocationDatabase
import com.tamhuynh.trackme.MyLocationEntity
import java.util.*
import java.util.concurrent.ExecutorService

private const val TAG = "LocationRepository"

/**
 * Access point for database (MyLocation data) and location APIs (start/stop location updates and
 * checking location update status).
 */
class LocationRepository private constructor(
    private val myLocationDatabase: MyLocationDatabase,
    private val myLocationManager: MyLocationManager,
    private val executor: ExecutorService
) {

    val changeLocation : MutableLiveData<MyLocationEntity> = MutableLiveData()

    // Database related fields/methods:
    private val locationDao = myLocationDatabase.locationDao()
    private val listWorkoutRecordDao = myLocationDatabase.listWorkoutDao()

    /**
     * Returns all recorded locations from database.
     */
    fun getLocations(): LiveData<List<MyLocationEntity>> = locationDao.getLocations()

    /**
     * Return all workout record from database
     */
    fun getListWorkout() : LiveData<List<ListWorkoutRecordEntity>> = listWorkoutRecordDao.getListWorkoutRecordAll()

    fun getTotalWorkoutRecord() : LiveData<Integer> = listWorkoutRecordDao.getTotalWorkoutRecord()

    fun getListWorkoutRecord(limit: Int, offset: Int) : LiveData<List<WorkoutRecordData>>
            = listWorkoutRecordDao.getListWorkoutRecord(limit, offset)

    fun locationChangedFrequently(): LiveData<MyLocationEntity> = changeLocation

    // Not being used now but could in future versions.
    /**
     * Returns specific location in database.
     */
    fun getLocation(id: UUID): LiveData<MyLocationEntity> = locationDao.getLocation(id)

    // Not being used now but could in future versions.
    /**
     * Updates location in database.
     */
    fun updateLocation(myLocationEntity: MyLocationEntity) {
        executor.execute {
            locationDao.updateLocation(myLocationEntity)
        }
    }

    /**
     * Adds location to the database.
     */
    fun addLocation(myLocationEntity: MyLocationEntity) {
        executor.execute {
            locationDao.addLocation(myLocationEntity)
        }
    }

    /**
     * Adds list of locations to the database.
     */
    fun addLocations(myLocationEntities: List<MyLocationEntity>) {
        executor.execute {
            locationDao.addLocations(myLocationEntities)
        }
        changeLocation.value = myLocationEntities[0]
    }

    /**
     * Add workout record session
     */
    fun addWorkoutRecord(listWorkoutRecordEntity: ListWorkoutRecordEntity){
        executor.execute {
            listWorkoutRecordDao.addListWorkoutRecord(listWorkoutRecordEntity)
        }
    }

    /**
     * Add workout record session
     */
    fun updateWorkoutRecord(listWorkoutRecordEntity: ListWorkoutRecordEntity){
        executor.execute {
            listWorkoutRecordDao.updateWorkoutRecord(listWorkoutRecordEntity)
        }
    }

    // Location related fields/methods:
    /**
     * Status of whether the app is actively subscribed to location changes.
     */
    val receivingLocationUpdates: LiveData<Boolean> = myLocationManager.receivingLocationUpdates

    /**
     * Subscribes to location updates.
     */
    @MainThread
    fun startLocationUpdates(workoutRecordID : Int) = myLocationManager.startLocationUpdates(workoutRecordID)

    /**
     * Un-subscribes from location updates.
     */
    @MainThread
    fun stopLocationUpdates() = myLocationManager.stopLocationUpdates()

    companion object {
        @Volatile private var INSTANCE: LocationRepository? = null

        fun getInstance(context: Context, executor: ExecutorService): LocationRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LocationRepository(
                    MyLocationDatabase.getInstance(context),
                    MyLocationManager.getInstance(context),
                    executor)
                    .also { INSTANCE = it }
            }
        }
    }
}
