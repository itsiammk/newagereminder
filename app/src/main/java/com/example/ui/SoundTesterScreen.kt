package com.example.ui

import android.app.Application
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundTesterScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val vibratorService = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    // Audio Test State
    var activeTestingTone by remember { mutableStateOf<String?>(null) }
    var testAudioTrack by remember { mutableStateOf<AudioTrack?>(null) }
    var testPlaying by remember { mutableStateOf(false) }

    // Settings States
    var dndBypass by remember { mutableStateOf(true) }
    var dozeOverride by remember { mutableStateOf(true) }
    var selectedTestVibration by remember { mutableStateOf("STANDARD") }

    // Tone Synth function
    fun stopTestingTone() {
        testPlaying = false
        try {
            testAudioTrack?.apply {
                stop()
                release()
            }
        } catch (_: Exception) {}
        testAudioTrack = null
        activeTestingTone = null
    }

    fun playTestingTone(toneName: String) {
        if (activeTestingTone == toneName) {
            stopTestingTone()
            return
        }
        stopTestingTone()
        activeTestingTone = toneName
        testPlaying = true

        Thread {
            val sampleRate = 44100
            val numSamples = sampleRate // 1 sec
            val buffer = ShortArray(numSamples)
            val minBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            try {
                val track = AudioTrack.Builder()
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

                testAudioTrack = track
                track.play()

                var phase = 0.0
                while (testPlaying && activeTestingTone == toneName) {
                    for (i in 0 until numSamples) {
                        // Match synth frequencies from foreground service
                        val frequency = when (toneName) {
                            "CHIPTUNE" -> {
                                val tick = (i / 4000) % 4
                                when (tick) {
                                    0 -> 523.25
                                    1 -> 659.25
                                    2 -> 783.99
                                    else -> 1046.50
                                }
                            }
                            "CHIME" -> 1000.0 + 200.0 * sin(i * 0.003)
                            "SIREN" -> 500.0 + 400.0 * sin(2.0 * PI * i / sampleRate)
                            "PULSE" -> 350.0 + (if ((i % 12000) < 6000) 150.0 else 0.0)
                            else -> 440.0
                        }

                        val genValue = when (toneName) {
                            "CHIPTUNE" -> if (sin(phase) >= 0) 0.3 else -0.3
                            "SIREN" -> sin(phase)
                            "CHIME" -> 0.7 * sin(phase) + 0.3 * sin(phase * 2)
                            "PULSE" -> if (sin(phase) >= 0) 0.5 * sin(phase) else 0.0
                            else -> sin(phase)
                        }

                        buffer[i] = (genValue * 32767).toInt().toShort()
                        phase += 2.0 * PI * frequency / sampleRate
                        if (phase > 2.0 * PI) phase -= 2.0 * PI
                    }
                    track.write(buffer, 0, numSamples)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    // Trigger local test vibration
    fun triggerTestVibration(style: String) {
        vibratorService?.cancel()
        val pattern = when (style) {
            "NONE" -> null
            "STANDARD" -> longArrayOf(0, 300, 200)
            "HEARTBEAT" -> longArrayOf(0, 150, 100, 150, 400)
            "SOS" -> longArrayOf(0, 80, 80, 80, 80, 80, 200, 100, 200, 100, 200, 100, 80, 80, 80, 80, 80, 300)
            else -> null
        }
        if (pattern != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibratorService?.vibrate(VibrationEffect.createWaveform(pattern, -1)) // No loop
            } else {
                @Suppress("DEPRECATION")
                vibratorService?.vibrate(pattern, -1)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            stopTestingTone()
            vibratorService?.cancel()
        }
    }

    // Neon scale animation
    val infiniteTransition = rememberInfiniteTransition(label = "NeonGlow")
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = SineWaveEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BorderAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Soundboard Settings",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Modulate Acoustic Waveforms & Pulses",
                fontSize = 14.sp,
                color = Color(0xFF22D3EE),
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Section 1: Sound Board (Tones preview)
            Text(
                text = "SOUND GENERATORS TESTER",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.6f),
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            val tones = listOf(
                Triple("CHIPTUNE", "Retro synth bits, fast 8-bit sweep orbits", Color(0xFF22D3EE)),
                Triple("CHIME", "Metallic glass bell, modern cyber chime pulse", Color(0xFF34D399)),
                Triple("SIREN", "Continuous sweeps of emergency alerting sirens", Color(0xFFC084FC)),
                Triple("PULSE", "Subtle electronic beacon ping wave loop", Color(0xFFFFB020))
            )

            tones.forEach { (name, desc, clr) ->
                val isTestingThis = activeTestingTone == name
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .testTag("soundboard_$name")
                        .border(
                            width = 1.dp,
                            color = if (isTestingThis) clr.copy(alpha = borderAlpha) else Color.White.copy(alpha = 0.06f),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = clr
                            )
                            Text(
                                text = desc,
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        // Play Test Button with interactive layout icon
                        IconButton(
                            onClick = { playTestingTone(name) },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(clr.copy(alpha = 0.15f))
                        ) {
                            Icon(
                                imageVector = if (isTestingThis) Icons.Default.Close else Icons.Default.PlayArrow,
                                contentDescription = "Test play audio synthesizer",
                                tint = clr,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section 2: Vibrations tester
            Text(
                text = "VIBRATOR FEEDBACK SIMULATION",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.6f),
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
                    .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("STANDARD", "HEARTBEAT", "SOS").forEach { style ->
                            val isSel = selectedTestVibration == style
                            Surface(
                                onClick = {
                                    selectedTestVibration = style
                                    triggerTestVibration(style)
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSel) Color(0xFF22D3EE) else Color.White.copy(alpha = 0.05f),
                                border = BorderStroke(1.dp, if (isSel) Color(0xFF22D3EE) else Color.Transparent)
                            ) {
                                Text(
                                    text = style,
                                    fontSize = 12.sp,
                                    color = if (isSel) Color(0xFF22D3EE) else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Touch any vibration chip to trigger a localized sensor impulse simulation.",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            }

            // Section 3: Priority Override Configuration Custom Switch Compose Panels
            Text(
                text = "PRIORITY OVERRIDE ENGINE",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.6f),
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Direct custom switch card toggling bypassing dnd
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Bypass Silent / DND Mode",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Routes through the Alarm System audio stream to guarantee sound even in complete DND states.",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    // Large touch target Switch
                    Switch(
                        checked = dndBypass,
                        onCheckedChange = { dndBypass = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF22D3EE),
                            checkedTrackColor = Color(0xFF22D3EE).copy(alpha = 0.3f)
                        ),
                        modifier = Modifier
                            .size(56.dp)
                            .testTag("priority_switch_dnd")
                    )
                }
            }

            // Direct custom switch card toggling waking screen
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
                    .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Doze Mode Screen Wakeup",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Deploys a Full-Screen Intent notification to forcefully wake lockscreen and panels mid sleeping schedules.",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Switch(
                        checked = dozeOverride,
                        onCheckedChange = { dozeOverride = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF22D3EE),
                            checkedTrackColor = Color(0xFF22D3EE).copy(alpha = 0.3f)
                        ),
                        modifier = Modifier
                            .size(56.dp)
                            .testTag("priority_switch_doze")
                    )
                }
            }

            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

val SineWaveEasing = Easing { x ->
    ((sin(x * PI - PI / 2) + 1.0) / 2.0).toFloat()
}
