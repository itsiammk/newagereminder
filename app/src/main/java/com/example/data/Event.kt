package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "events")
data class Event(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val date: String, // format "YYYY-MM-DD"
    val time: String, // format "HH:mm"
    val category: String, // BIRTHDAY, ANNIVERSARY, SHORT_TERM, LONG_TERM, CUSTOM
    val ringtone: String = "CHIPTUNE", // CHIPTUNE, CHIME, SIREN, PULSE
    val vibrateStyle: String = "STANDARD", // NONE, STANDARD, HEARTBEAT, SOS
    val isEnabled: Boolean = true,
    val isCompleted: Boolean = false
)
