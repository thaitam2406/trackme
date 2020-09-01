
package com.tamhuynh.trackme.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.tamhuynh.trackme.data.LocationRepository
import com.tamhuynh.trackme.ListWorkoutRecordEntity
import java.util.concurrent.Executors

/**
 * Allows Fragment to observer {@link MyLocation} database, follow the state of location updates,
 * and start/stop receiving location updates.
 */
class LocationUpdateViewModel(application: Application) : AndroidViewModel(application) {

    private val locationRepository = LocationRepository.getInstance(
        application.applicationContext,
        Executors.newSingleThreadExecutor()
    )

    val receivingLocationUpdates: LiveData<Boolean> = locationRepository.receivingLocationUpdates

    val locationListLiveData = locationRepository.getLocations()

    val getTotalWorkoutRecord = locationRepository.getTotalWorkoutRecord()

    fun getListWorkoutRecord(limit: Int, offset: Int) = locationRepository.getListWorkoutRecord(limit, offset)

    fun addNewWorkoutRecord(listWorkoutRecordEntity: ListWorkoutRecordEntity) = locationRepository.addWorkoutRecord(listWorkoutRecordEntity)

    fun updateWorkoutRecord(listWorkoutRecordEntity: ListWorkoutRecordEntity) = locationRepository.updateWorkoutRecord(listWorkoutRecordEntity)

    val locationChangedFrequently = locationRepository.locationChangedFrequently()

    fun startLocationUpdates(workoutID : Int) = locationRepository.startLocationUpdates(workoutID)

    fun stopLocationUpdates() = locationRepository.stopLocationUpdates()
}
