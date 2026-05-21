package com.example

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppDatabase
import com.example.data.Event
import com.example.data.EventRepository
import com.example.data.Reminder
import com.example.services.RingingForegroundService
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.random.Random

class RingingActivity : ComponentActivity() {

    private val closeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Setup over lockscreen flags
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        // Register receiver for when alarm is stopped via notification actions
        registerReceiver(closeReceiver, IntentFilter("com.example.services.CLOSE_RINGING_ACTIVITY"))

        val reminderId = intent.getIntExtra("EXTRA_REMINDER_ID", -1)
        val eventId = intent.getIntExtra("EXTRA_EVENT_ID", -1)

        setContent {
            MyApplicationTheme {
                RingingScreen(
                    reminderId = reminderId,
                    eventId = eventId,
                    onDismiss = {
                        val serviceIntent = Intent(this, RingingForegroundService::class.java).apply {
                            action = RingingForegroundService.ACTION_DISMISS
                            putExtra("EXTRA_REMINDER_ID", reminderId)
                            putExtra("EXTRA_EVENT_ID", eventId)
                        }
                        startService(serviceIntent)
                        finish()
                    },
                    onSnooze = {
                        val serviceIntent = Intent(this, RingingForegroundService::class.java).apply {
                            action = RingingForegroundService.ACTION_SNOOZE
                            putExtra("EXTRA_REMINDER_ID", reminderId)
                            putExtra("EXTRA_EVENT_ID", eventId)
                        }
                        startService(serviceIntent)
                        finish()
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(closeReceiver)
        } catch (_: Exception) {}
    }
}

// Particle class for the animated confetti physics model
data class ConfettiParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val color: Color,
    val size: Float,
    var rotation: Float,
    val rotationSpeed: Float
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RingingScreen(
    reminderId: Int,
    eventId: Int,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit
) {
    // Collect context references
    var eventState by remember { mutableStateOf<Event?>(null) }
    val db = AppDatabase.getDatabase(androidx.compose.ui.platform.LocalContext.current)
    val repo = EventRepository(db.eventDao())

    LaunchedEffect(eventId) {
        if (eventId != -1) {
            eventState = repo.getEventById(eventId)
        }
    }

    // Interactive Swipe States
    var dragOffset by remember { mutableStateOf(0f) }
    val screenWidthPx = with(LocalDensity.current) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
    val swipeThreshold = screenWidthPx * 0.35f

    // Confetti Engine triggers
    val particles = remember {
        mutableStateListOf<ConfettiParticle>().apply {
            repeat(60) {
                add(
                    ConfettiParticle(
                        x = Random.nextFloat() * 1000f,
                        y = -Random.nextFloat() * 400f,
                        vx = (Random.nextFloat() * 4f) - 2f,
                        vy = (Random.nextFloat() * 6f) + 4f,
                        color = listOf(
                            Color(0xFFFF007F), // Neon Pink
                            Color(0xFF00F0FF), // Neon Blue
                            Color(0xFF39FF14), // Neon Green
                            Color(0xFFFFD700), // Electric Gold
                            Color(0xFFB026FF)  // Electric Violet
                        ).random(),
                        size = Random.nextFloat() * 12f + 8f,
                        rotation = Random.nextFloat() * 360f,
                        rotationSpeed = (Random.nextFloat() * 5f) - 2.5f
                    )
                )
            }
        }
    }

    // Particles animator frame loop
    val infiniteTransition = rememberInfiniteTransition(label = "ConfettiLoop")
    val frameTrigger by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "frameTrigger"
    )

    LaunchedEffect(frameTrigger) {
        particles.forEach { p ->
            p.y += p.vy
            p.x += p.vx
            p.rotation += p.rotationSpeed
            // Recolor and reset if fall past screen
            if (p.y > 2500f) {
                p.y = -50f
                p.x = Random.nextFloat() * 1000f
                p.vy = (Random.nextFloat() * 6f) + 4f
            }
        }
    }

    // Micro pulsing and gradients
    val pulseScale by animateFloatAsState(
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("ringing_screen")
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF13092A), // Deep Cosmic Violet
                        Color(0xFF06030D)  // Void Black
                    )
                )
            )
    ) {
        // Draw Confetti Layer on Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            particles.forEach { p ->
                drawRect(
                    color = p.color,
                    topLeft = androidx.compose.ui.geometry.Offset(p.x, p.y),
                    size = androidx.compose.ui.geometry.Size(p.size, p.size),
                    alpha = 0.85f
                )
            }
        }

        // Center Content Container
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Info
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(top = 48.dp)
                    .fillMaxWidth()
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.08f),
                    modifier = Modifier
                        .size(80.dp * pulseScale)
                        .border(1.5.dp, Color(0xFF00F0FF).copy(alpha = 0.4f), CircleShape)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Alarm icon",
                            tint = Color(0xFF00F0FF),
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "COSMIC REMINDER",
                    fontSize = 13.sp,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00F0FF),
                    letterSpacing = 4.sp
                )
            }

            // Central Glass-morphic Information Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.05f)
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.18f),
                                Color.Transparent
                            )
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(24.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = eventState?.title ?: "Cosmic Wakeup Call",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        lineHeight = 36.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        text = eventState?.description?.ifEmpty { "It is time for your custom set notification!" }
                            ?: "Synthesized rhythm looping...",
                        fontSize = 16.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Category: ${eventState?.category ?: "URGENT"}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFFF007F),
                        modifier = Modifier
                            .background(Color(0xFFFF007F).copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }

            // Interactive "Swipe to Dismiss/Snooze" visual track
            Column(
                modifier = Modifier
                    .padding(bottom = 56.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "◀ SWIPE LEFT SNOOZE   |   SWIPE RIGHT DISMISS ▶",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.4f),
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // The Slider Container
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(72.dp)
                        .clip(RoundedCornerShape(36.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(36.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    // Left and Right icons inside the track
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Snooze Icons",
                            tint = Color(0xFFFF007F).copy(alpha = 0.5f)
                        )
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss Icons",
                            tint = Color(0xFF00F0FF).copy(alpha = 0.5f)
                        )
                    }

                    // Tappable and draggable glowing action pill
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(dragOffset.roundToInt(), 0) }
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFFFF007F), // Neon Pink/Purple
                                        Color(0xFF00F0FF)  // Neon Blue
                                    )
                                )
                            )
                            .pointerInput(Unit) {
                                detectHorizontalDragGestures(
                                    onDragEnd = {
                                        if (dragOffset > swipeThreshold) {
                                            // Swiped right past threshold -> Dismiss Alarm
                                            onDismiss()
                                        } else if (dragOffset < -swipeThreshold) {
                                            // Swiped left past threshold -> Snooze Alarm
                                            onSnooze()
                                        } else {
                                            // Back to center
                                            dragOffset = 0f
                                        }
                                    },
                                    onDragCancel = { dragOffset = 0f },
                                    onHorizontalDrag = { change, dragAmount ->
                                        change.consume()
                                        val nextValue = dragOffset + dragAmount
                                        // Bound the movement inside the track
                                        val bound = screenWidthPx * 0.45f
                                        dragOffset = nextValue.coerceIn(-bound, bound)
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Drag control button",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}
