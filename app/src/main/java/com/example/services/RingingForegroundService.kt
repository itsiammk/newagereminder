package com.example.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.RingingActivity
import com.example.data.AppDatabase
import com.example.data.Event
import com.example.data.EventRepository
import com.example.data.Reminder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

class RingingForegroundService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null
    private var vibrator: Vibrator? = null
    private var isPlaying = false
    private var audioTrack: AudioTrack? = null

    private val database by lazy { AppDatabase.getDatabase(applicationContext) }
    private val repository by lazy { EventRepository(database.eventDao()) }

    companion object {
        const val CHANNEL_ID = "cosmic_reminders_alarm_channel"
        const val NOTIFICATION_ID = 8808
        const val ACTION_START = "com.example.services.RingingForegroundService.START"
        const val ACTION_DISMISS = "com.example.services.RingingForegroundService.DISMISS"
        const val ACTION_SNOOZE = "com.example.services.RingingForegroundService.SNOOZE"

        // Store active service state for UI connection
        var currentEvent: Event? = null
        var currentReminder: Reminder? = null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d("RingingService", "Service onCreate")
        
        // Setup Vibrator
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        // Acquire wake lock
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "CosmicReminders::RingingServiceWakeLock"
        )
        wakeLock?.acquire(10 * 60 * 1000L) // 10 minutes timeout
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val reminderId = intent?.getIntExtra("EXTRA_REMINDER_ID", -1) ?: -1
        val eventId = intent?.getIntExtra("EXTRA_EVENT_ID", -1) ?: -1
        val action = intent?.action ?: ACTION_START

        Log.d("RingingService", "onStartCommand action: $action, eventId: $eventId, reminderId: $reminderId")

        if (action == ACTION_DISMISS) {
            dismissAlarm(reminderId, eventId)
            return START_NOT_STICKY
        } else if (action == ACTION_SNOOZE) {
            snoozeAlarm(reminderId, eventId)
            return START_NOT_STICKY
        }

        if (reminderId == -1 || eventId == -1) {
            stopSelf()
            return START_NOT_STICKY
        }

        // Load Event and Reminder from DB asynchronously
        CoroutineScope(Dispatchers.IO).launch {
            val event = repository.getEventById(eventId)
            val reminder = repository.getReminderById(reminderId)

            if (event != null && reminder != null) {
                currentEvent = event
                currentReminder = reminder

                // Start Foreground with Notification
                showForegroundNotification(event, reminder)

                // Vibration pattern
                triggerVibration(event.vibrateStyle)

                // Tone play
                playSynthesizedTone(event.ringtone)
                
                // Launch Ringing Screen directly as user requested Screen Wake / Over-lock behavior
                val ringingIntent = Intent(applicationContext, RingingActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    putExtra("EXTRA_REMINDER_ID", reminderId)
                    putExtra("EXTRA_EVENT_ID", eventId)
                }
                startActivity(ringingIntent)
            } else {
                stopSelf()
            }
        }

        return START_STICKY
    }

    private fun showForegroundNotification(event: Event, reminder: Reminder) {
        val channelName = "Cosmic Reminders Alarms"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical reminder alarm triggers"
                enableLights(true)
                setBypassDnd(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        // Full-screen intent to wake lockscreen and display RingingActivity
        val fullScreenIntent = Intent(this, RingingActivity::class.java).apply {
            putExtra("EXTRA_REMINDER_ID", reminder.id)
            putExtra("EXTRA_EVENT_ID", event.id)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            reminder.id,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        // Notification buttons
        val dismissIntent = Intent(this, RingingForegroundService::class.java).apply {
            action = ACTION_DISMISS
            putExtra("EXTRA_REMINDER_ID", reminder.id)
            putExtra("EXTRA_EVENT_ID", event.id)
        }
        val dismissPendingIntent = PendingIntent.getService(
            this,
            reminder.id + 100000,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val snoozeIntent = Intent(this, RingingForegroundService::class.java).apply {
            action = ACTION_SNOOZE
            putExtra("EXTRA_REMINDER_ID", reminder.id)
            putExtra("EXTRA_EVENT_ID", event.id)
        }
        val snoozePendingIntent = PendingIntent.getService(
            this,
            reminder.id + 200000,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(event.title)
            .setContentText(event.description.ifEmpty { "Critical Reminder Alarm Triggered!" })
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setOngoing(true)
            .setAutoCancel(false)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", dismissPendingIntent)
            .addAction(android.R.drawable.ic_popup_sync, "Snooze", snoozePendingIntent)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun triggerVibration(vibrateStyle: String) {
        val pattern = when (vibrateStyle) {
            "NONE" -> null
            "STANDARD" -> longArrayOf(0, 500, 500)
            "HEARTBEAT" -> longArrayOf(0, 200, 150, 200, 600)
            "SOS" -> longArrayOf(0, 100, 100, 100, 100, 100, 300, 200, 300, 200, 300, 200, 100, 100, 100, 100, 100, 500)
            else -> longArrayOf(0, 100, 100)
        }

        if (pattern != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0)) // Loop at 0
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        }
    }

    private fun playSynthesizedTone(tone: String) {
        stopTone()
        isPlaying = true
        Thread {
            val sampleRate = 44100
            val numSamples = sampleRate // 1 second buffer
            val buffer = ShortArray(numSamples)

            val minBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            try {
                audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(maxOf(minBufferSize, numSamples * 2))
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                audioTrack?.play()

                var phase = 0.0
                while (isPlaying) {
                    for (i in 0 until numSamples) {
                        val frequency = getFrequencyForTone(tone, i, sampleRate)
                        val value = generateWave(tone, phase)
                        buffer[i] = (value * 32767).toInt().toShort()
                        phase += 2.0 * Math.PI * frequency / sampleRate
                        if (phase > 2.0 * Math.PI) {
                            phase -= 2.0 * Math.PI
                        }
                    }
                    audioTrack?.write(buffer, 0, numSamples)
                }
            } catch (e: Exception) {
                Log.e("RingingService", "Failed synthesizer track creation", e)
            }
        }.start()
    }

    private fun getFrequencyForTone(tone: String, index: Int, sampleRate: Int): Double {
        return when (tone) {
            "CHIPTUNE" -> {
                val tick = (index / 4000) % 4
                when (tick) {
                    0 -> 523.25 // C5
                    1 -> 659.25 // E5
                    2 -> 783.99 // G5
                    else -> 1046.50 // C6
                }
            }
            "CHIME" -> {
                1000.0 + 200.0 * sin(index * 0.003)
            }
            "SIREN" -> {
                500.0 + 400.0 * sin(2.0 * PI * index / sampleRate)
            }
            "PULSE" -> {
                350.0 + (if ((index % 12000) < 6000) 150.0 else 0.0)
            }
            else -> 440.0
        }
    }

    private fun generateWave(tone: String, phase: Double): Double {
        return when (tone) {
            "CHIPTUNE" -> {
                if (sin(phase) >= 0) 0.3 else -0.3
            }
            "SIREN" -> sin(phase)
            "CHIME" -> 0.7 * sin(phase) + 0.3 * sin(phase * 2)
            "PULSE" -> if (sin(phase) >= 0) 0.5 * sin(phase) else 0.0
            else -> sin(phase)
        }
    }

    private fun stopTone() {
        isPlaying = false
        try {
            audioTrack?.apply {
                stop()
                release()
            }
        } catch (_: Exception) {}
        audioTrack = null
    }

    private fun dismissAlarm(reminderId: Int, eventId: Int) {
        Log.d("RingingService", "Dismissing alarm for Reminder: $reminderId, Event: $eventId")
        val ringId = if (reminderId != -1) reminderId else currentReminder?.id ?: -1
        val evId = if (eventId != -1) eventId else currentEvent?.id ?: -1

        CoroutineScope(Dispatchers.IO).launch {
            if (ringId != -1) {
                val rem = repository.getReminderById(ringId)
                if (rem != null) {
                    repository.updateReminder(rem.copy(isTriggered = true))
                }
            }
            cleanUpAndStop()
        }
    }

    private fun snoozeAlarm(reminderId: Int, eventId: Int) {
        val ringId = if (reminderId != -1) reminderId else currentReminder?.id ?: -1
        val evId = if (eventId != -1) eventId else currentEvent?.id ?: -1

        Log.d("RingingService", "Snoozing alarm for Reminder: $ringId, Event: $evId")

        if (evId == -1) {
            cleanUpAndStop()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            if (ringId != -1) {
                val rem = repository.getReminderById(ringId)
                if (rem != null) {
                    // Mark old as triggered
                    repository.updateReminder(rem.copy(isTriggered = true))

                    // Schedule snooze alarm (e.g., 2 minutes in future for easier demo, or 10 min)
                    val snoozeInterval = 2 * 60 * 1000L // 2 minutes for testing, extremely friendly!
                    val newTriggerTime = System.currentTimeMillis() + snoozeInterval
                    
                    val snoozeReminder = Reminder(
                        eventId = evId,
                        triggerTime = newTriggerTime,
                        minutesBefore = rem.minutesBefore,
                        isTriggered = false,
                        snoozeCount = rem.snoozeCount + 1
                    )
                    
                    val newId = repository.insertReminder(snoozeReminder)
                    val scheduledRem = snoozeReminder.copy(id = newId.toInt())
                    
                    AlarmScheduler(applicationContext).schedule(scheduledRem)
                    Log.d("RingingService", "Snooze alarm successfully scheduled for 2 minutes from now")
                }
            }
            cleanUpAndStop()
        }
    }

    private fun cleanUpAndStop() {
        stopTone()
        vibrator?.cancel()
        currentEvent = null
        currentReminder = null
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) {}

        // Broadcast to trigger UI closes in RingingActivity
        val closeIntent = Intent("com.example.services.CLOSE_RINGING_ACTIVITY")
        sendBroadcast(closeIntent)

        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        cleanUpAndStop()
        Log.d("RingingService", "Service onDestroy")
    }
}
