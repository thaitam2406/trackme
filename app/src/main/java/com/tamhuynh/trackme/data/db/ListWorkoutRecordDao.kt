
package com.tamhuynh.trackme

import androidx.lifecycle.LiveData
import androidx.room.*

/**
 * Defines database operations.
 */
@Dao
interface ListWorkoutRecordDao {

    @Query("SELECT * FROM list_location_record_table ORDER BY date DESC")
    fun getListWorkoutRecordAll(): LiveData<List<ListWorkoutRecordEntity>>

    /**
     *  Because Room runs the two queries for us under the hood, add the @Transaction annotation, to ensure that this happens atomically.
     */
    @Transaction
    @Query("SELECT * FROM list_location_record_table ORDER BY date DESC LIMIT :limit OFFSET :offset")
    fun getListWorkoutRecord(limit: Int, offset: Int): LiveData<List<WorkoutRecordData>>

    @Insert
    fun addListWorkoutRecord(listWorkoutRecordEntity: ListWorkoutRecordEntity)

    @Update
    fun updateWorkoutRecord(listWorkoutRecordEntity: ListWorkoutRecordEntity)

    @Query("SELECT COUNT(workoutID) FROM list_location_record_table")
    fun getTotalWorkoutRecord() : LiveData<Integer>

}
