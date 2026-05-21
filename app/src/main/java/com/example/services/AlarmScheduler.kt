package com.example.services

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.Reminder

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    @SuppressLint("MissingPermission")
    fun schedule(reminder: Reminder) {
        if (reminder.triggerTime <= System.currentTimeMillis()) {
            Log.d("AlarmScheduler", "Skipping reminder ${reminder.id} as trigger time is in the past")
            return
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("EXTRA_REMINDER_ID", reminder.id)
            putExtra("EXTRA_EVENT_ID", reminder.eventId)
            action = "com.example.ACTION_ALARM_TRIGGER"
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id,
            intent,
            flags
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    setExactAlarm(reminder.triggerTime, pendingIntent)
                } else {
                    // Fallback to inexact if permission is missing
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        reminder.triggerTime,
                        pendingIntent
                    )
                }
            } else {
                setExactAlarm(reminder.triggerTime, pendingIntent)
            }
            Log.d("AlarmScheduler", "Scheduled reminder ${reminder.id} at ${reminder.triggerTime}")
        } catch (e: Exception) {
            Log.e("AlarmScheduler", "Failed to schedule alarm for reminder ${reminder.id}", e)
        }
    }

    private fun setExactAlarm(triggerTime: Long, pendingIntent: PendingIntent) {
        // setAlarmClock is the most bulletproof way to wake the device in Doze mode
        val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTime, pendingIntent)
        alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
    }

    fun cancel(reminder: Reminder) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.example.ACTION_ALARM_TRIGGER"
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_NO_CREATE
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id,
            intent,
            flags
        )

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d("AlarmScheduler", "Cancelled reminder ${reminder.id}")
        }
    }
}
