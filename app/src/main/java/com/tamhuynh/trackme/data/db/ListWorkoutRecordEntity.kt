
package com.tamhuynh.trackme

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.DateFormat
import java.util.Date
import java.util.UUID

/**
 * Data class for Location related data (only takes what's needed from
 * {@link android.location.Location} class).
 */
@Entity(tableName = "list_location_record_table")
data class ListWorkoutRecordEntity(
        @PrimaryKey(autoGenerate = true) var workoutID: Int = 0,
        var distance : Int = 0,
        var v : Float = 0.0f,
        var totalTime : String = "",
        var date: Date = Date()
)
