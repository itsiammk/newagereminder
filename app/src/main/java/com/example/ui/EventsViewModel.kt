package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.Event
import com.example.data.EventRepository
import com.example.data.Reminder
import com.example.services.AlarmScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class EventsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: EventRepository
    private val scheduler: AlarmScheduler
    val allEvents: StateFlow<List<Event>>

    init {
        val db = AppDatabase.getDatabase(application)
        repository = EventRepository(db.eventDao())
        scheduler = AlarmScheduler(application)

        allEvents = repository.allEvents.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    fun saveEvent(
        id: Int = 0,
        title: String,
        description: String,
        date: String,
        time: String,
        category: String,
        ringtone: String,
        vibrateStyle: String,
        alertExact: Boolean,
        alert2Min: Boolean,
        alert4Hour: Boolean,
        alert1Day: Boolean,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val event = Event(
                    id = id,
                    title = title,
                    description = description,
                    date = date,
                    time = time,
                    category = category,
                    ringtone = ringtone,
                    vibrateStyle = vibrateStyle,
                    isEnabled = true
                )

                val eventId = if (id == 0) {
                    repository.insertEvent(event).toInt()
                } else {
                    repository.updateEvent(event)
                    // If editing, cancel existing reminder alarms first
                    val existingReminders = repository.getRemindersForEventSync(id)
                    existingReminders.forEach { scheduler.cancel(it) }
                    repository.deleteEventAndReminders(event) // Clean up database entries
                    repository.insertEvent(event.copy(id = id)) // reinsert
                    id
                }

                // Calculate triggers
                val baseTime = parseDateTime(date, time)

                // 4 alerts supported (multi-tier)
                val alertsToCreate = mutableListOf<Pair<Long, Int>>()
                if (alertExact) alertsToCreate.add(Pair(baseTime, 0))
                if (alert2Min) alertsToCreate.add(Pair(baseTime - 2 * 60 * 1000L, 2))
                if (alert4Hour) alertsToCreate.add(Pair(baseTime - 4 * 60 * 60 * 1000L, 240))
                if (alert1Day) alertsToCreate.add(Pair(baseTime - 24 * 60 * 60 * 1000L, 1440))

                alertsToCreate.forEach { (triggerTime, minutesBefore) ->
                    if (triggerTime > System.currentTimeMillis()) {
                        val reminder = Reminder(
                            eventId = eventId,
                            triggerTime = triggerTime,
                            minutesBefore = minutesBefore,
                            isTriggered = false
                        )
                        val remId = repository.insertReminder(reminder)
                        scheduler.schedule(reminder.copy(id = remId.toInt()))
                    }
                }

                viewModelScope.launch(Dispatchers.Main) {
                    onSuccess()
                }
            } catch (e: Exception) {
                Log.e("ViewModel", "Failed to save event/reminders", e)
            }
        }
    }

    fun deleteEvent(event: Event) {
        viewModelScope.launch(Dispatchers.IO) {
            val existingReminders = repository.getRemindersForEventSync(event.id)
            existingReminders.forEach { scheduler.cancel(it) }
            repository.deleteEventAndReminders(event)
        }
    }

    fun toggleEventEnabled(event: Event, isEnabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val updatedEvent = event.copy(isEnabled = isEnabled)
            repository.updateEvent(updatedEvent)

            val existingReminders = repository.getRemindersForEventSync(event.id)
            if (isEnabled) {
                // Re-schedule alarms that are in the future
                existingReminders.forEach { rem ->
                    if (rem.triggerTime > System.currentTimeMillis() && !rem.isTriggered) {
                        scheduler.schedule(rem)
                    }
                }
            } else {
                // Cancel active alarms in Android system
                existingReminders.forEach { scheduler.cancel(it) }
            }
        }
    }

    private fun parseDateTime(date: String, time: String): Long {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            format.parse("$date $time")?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
}
