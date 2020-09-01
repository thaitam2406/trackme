package com.tamhuynh.trackme

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Relation
import java.io.Serializable
import java.util.*


data class WorkoutRecordData (
        @Embedded val listWorkoutRecordEntity: ListWorkoutRecordEntity,
        @Relation(
                parentColumn = "workoutID",
                entityColumn = "workoutRecordId"
        )
        val locations: List<MyLocationEntity>
) : Serializable