package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reminders",
    foreignKeys = [
        ForeignKey(
            entity = Event::class,
            parentColumns = ["id"],
            childColumns = ["eventId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["eventId"])]
)
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val eventId: Int,
    val triggerTime: Long, // Epoch Milliseconds
    val minutesBefore: Int, // 0 for exact, 2 for 2 min before, 240 for 4 hours, 1440 for 1 day
    val isTriggered: Boolean = false,
    val snoozeCount: Int = 0
)
