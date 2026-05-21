package com.example.data

import kotlinx.coroutines.flow.Flow

class EventRepository(private val eventDao: EventDao) {

    val allEvents: Flow<List<Event>> = eventDao.getAllEvents()

    suspend fun getEventById(id: Int): Event? {
        return eventDao.getEventById(id)
    }

    suspend fun insertEvent(event: Event): Long {
        return eventDao.insertEvent(event)
    }

    suspend fun updateEvent(event: Event) {
        eventDao.updateEvent(event)
    }

    suspend fun deleteEventAndReminders(event: Event) {
        eventDao.deleteRemindersByEventId(event.id)
        eventDao.deleteEvent(event)
    }

    suspend fun getRemindersForEvent(eventId: Int): Flow<List<Reminder>> {
        return eventDao.getRemindersForEvent(eventId)
    }

    suspend fun getRemindersForEventSync(eventId: Int): List<Reminder> {
        return eventDao.getRemindersForEventSync(eventId)
    }

    suspend fun getReminderById(id: Int): Reminder? {
        return eventDao.getReminderById(id)
    }

    suspend fun getActiveRemindersSync(now: Long): List<Reminder> {
        return eventDao.getActiveRemindersSync(now)
    }

    suspend fun insertReminder(reminder: Reminder): Long {
        return eventDao.insertReminder(reminder)
    }

    suspend fun updateReminder(reminder: Reminder) {
        eventDao.updateReminder(reminder)
    }

    suspend fun deleteReminder(reminder: Reminder) {
        eventDao.deleteReminder(reminder)
    }
}
