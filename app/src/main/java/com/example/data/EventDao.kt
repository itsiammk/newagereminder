package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    // --- Event Queries ---
    @Query("SELECT * FROM events ORDER BY date ASC, time ASC")
    fun getAllEvents(): Flow<List<Event>>

    @Query("SELECT * FROM events WHERE id = :id")
    suspend fun getEventById(id: Int): Event?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: Event): Long

    @Update
    suspend fun updateEvent(event: Event)

    @Delete
    suspend fun deleteEvent(event: Event)

    // --- Reminder Queries ---
    @Query("SELECT * FROM reminders WHERE eventId = :eventId")
    fun getRemindersForEvent(eventId: Int): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE eventId = :eventId")
    suspend fun getRemindersForEventSync(eventId: Int): List<Reminder>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getReminderById(id: Int): Reminder?

    @Query("SELECT * FROM reminders WHERE isTriggered = 0 AND triggerTime >= :now")
    suspend fun getActiveRemindersSync(now: Long): List<Reminder>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: Reminder): Long

    @Update
    suspend fun updateReminder(reminder: Reminder)

    @Query("DELETE FROM reminders WHERE eventId = :eventId")
    suspend fun deleteRemindersByEventId(eventId: Int)

    @Delete
    suspend fun deleteReminder(reminder: Reminder)
}
