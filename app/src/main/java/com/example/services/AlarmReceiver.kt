package com.example.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.util.Log

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.example.ACTION_ALARM_TRIGGER") {
            val reminderId = intent.getIntExtra("EXTRA_REMINDER_ID", -1)
            val eventId = intent.getIntExtra("EXTRA_EVENT_ID", -1)

            Log.d("AlarmReceiver", "Received alarm trigger for Event: $eventId, Reminder: $reminderId")

            if (reminderId == -1 || eventId == -1) {
                Log.e("AlarmReceiver", "Invalid IDs in alarm trigger")
                return
            }

            // Acquire temporary WakeLock to guarantee execution
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "CosmicReminders::AlarmWakeLock"
            )
            wakeLock.acquire(10000) // Keep awake for up to 10 seconds to start service

            val serviceIntent = Intent(context, RingingForegroundService::class.java).apply {
                putExtra("EXTRA_REMINDER_ID", reminderId)
                putExtra("EXTRA_EVENT_ID", eventId)
            }

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
                Log.d("AlarmReceiver", "Foreground service started successfully from broadcast")
            } catch (e: Exception) {
                Log.e("AlarmReceiver", "Failed to start foreground service", e)
            }
        } else if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Re-schedule all active, non-triggered alarms after system boot
            Log.d("AlarmReceiver", "Device rebooted. Reminders rescheduled handled in MainActivity/Application startup")
            // We can also let the repository/app auto-schedule reminders when initialized
        }
    }
}
