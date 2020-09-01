
package com.tamhuynh.trackme

import androidx.room.TypeConverter
import java.util.Date
import java.util.UUID

/**
 * Converts non-standard objects in the {@link WorkoutRecord} data class into and out of the database.
 */
class WorkoutRecordTypeConverters {

    @TypeConverter
    fun fromDate(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun toDate(millisSinceEpoch: Long?): Date? {
        return millisSinceEpoch?.let {
            Date(it)
        }
    }
}
