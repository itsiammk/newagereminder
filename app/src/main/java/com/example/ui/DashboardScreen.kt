package com.example.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Event
import java.util.*

@Composable
fun SoundwaveVisualizer() {
    val infiniteTransition = rememberInfiniteTransition(label = "soundwave")
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier
            .height(24.dp)
            .alpha(0.6f)
    ) {
        listOf(0.3f, 0.8f, 0.5f, 0.9f, 0.6f, 0.4f, 0.7f).forEachIndexed { index, baseHeight ->
            val animVal by infiniteTransition.animateFloat(
                initialValue = baseHeight * 0.2f,
                targetValue = baseHeight,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 650 + index * 90, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar_$index"
            )
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight(animVal)
                    .background(Color(0xFF22D3EE), RoundedCornerShape(2.dp))
            )
        }
    }
}

@Composable
fun HeroCard(
    event: Event,
    onToggleEnabled: (Boolean) -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.05f)
        ),
        shape = RoundedCornerShape(32.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hero_event_card_${event.id}")
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.12f),
                shape = RoundedCornerShape(32.dp)
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Header (Category is standard, Title highlighted)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "ACTIVE GUARD",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF22D3EE),
                            letterSpacing = 1.5.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = event.title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }

                    Switch(
                        checked = event.isEnabled,
                        onCheckedChange = { onToggleEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF22D3EE),
                            checkedTrackColor = Color(0xFF22D3EE).copy(alpha = 0.3f),
                            uncheckedThumbColor = Color.White.copy(alpha = 0.5f),
                            uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Large clock display
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = event.time,
                        fontSize = 46.sp,
                        fontWeight = FontWeight.Light,
                        color = Color.White,
                        letterSpacing = (-1).sp
                    )
                    Text(
                        text = "AM",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${event.date} • ${event.category} Mode",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.4f)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // soundwave visualizations and trigger actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SoundwaveVisualizer()

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(onClick = onEditClick) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Tracker",
                                tint = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(onClick = onDeleteClick) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Erase alarm",
                                tint = Color(0xFFFF007F).copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: EventsViewModel,
    modifier: Modifier = Modifier
) {
    val events by viewModel.allEvents.collectAsStateWithLifecycle()
    var showAddSheet by remember { mutableStateOf(false) }
    var eventToEdit by remember { mutableStateOf<Event?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header (Styled like Top App Bar)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Modern gradient circular emblem with cyan-to-purple shadow
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF22D3EE), Color(0xFFC084FC))
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Cosmic symbol",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Remindr",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                letterSpacing = (-0.5).sp
                            )
                            Text(
                                text = ".",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF22D3EE)
                            )
                        }
                        Text(
                            text = "ACTIVE CHRONO PATROL",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF22D3EE),
                            letterSpacing = 2.sp
                        )
                    }
                }
                
                // Right side - Account / settings shape mockup
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "User account",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Adaptive content grid
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                val columnsCount = when {
                    maxWidth < 600.dp -> 1
                    maxWidth < 900.dp -> 2
                    else -> 3
                }

                if (events.isEmpty()) {
                    EmptyCosmicState(onAddClick = { showAddSheet = true })
                } else {
                    val activeAlarms = events.filter { it.isEnabled }
                    val leadingAlarm = activeAlarms.firstOrNull() ?: events.firstOrNull()
                    val remainingAlarms = events.filter { it != leadingAlarm }

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columnsCount),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        leadingAlarm?.let { nextAlarm ->
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                HeroCard(
                                    event = nextAlarm,
                                    onToggleEnabled = { isEnabled ->
                                        viewModel.toggleEventEnabled(nextAlarm, isEnabled)
                                    },
                                    onEditClick = {
                                        eventToEdit = nextAlarm
                                        showAddSheet = true
                                    },
                                    onDeleteClick = {
                                        viewModel.deleteEvent(nextAlarm)
                                    }
                                )
                            }
                        }

                        if (remainingAlarms.isNotEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Text(
                                    text = "UPCOMING EVENT HORIZON",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.4f),
                                    letterSpacing = 1.5.sp,
                                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp, start = 4.dp)
                                )
                            }

                            items(remainingAlarms, key = { it.id }) { event ->
                                EventCard(
                                    event = event,
                                    onToggleEnabled = { isEnabled ->
                                        viewModel.toggleEventEnabled(event, isEnabled)
                                    },
                                    onEditClick = {
                                        eventToEdit = event
                                        showAddSheet = true
                                    },
                                    onDeleteClick = {
                                        viewModel.deleteEvent(event)
                                    }
                                )
                            }
                        }

                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
            }
        }

        // Beautiful Floating Action Button (FAB)
        FloatingActionButton(
            onClick = {
                eventToEdit = null
                showAddSheet = true
            },
            containerColor = Color(0xFF22D3EE),
            contentColor = Color.Black,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("add_reminder_fab")
                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Cosmic Alarm",
                modifier = Modifier.size(28.dp)
            )
        }

        // Trigger Editor Sheet
        if (showAddSheet) {
            EventBottomSheet(
                event = eventToEdit,
                onDismiss = {
                    showAddSheet = false
                    eventToEdit = null
                },
                onSave = { title, desc, date, time, cat, ringtone, vibrate, ex, min2, h4, d1 ->
                    viewModel.saveEvent(
                        id = eventToEdit?.id ?: 0,
                        title = title,
                        description = desc,
                        date = date,
                        time = time,
                        category = cat,
                        ringtone = ringtone,
                        vibrateStyle = vibrate,
                        alertExact = ex,
                        alert2Min = min2,
                        alert4Hour = h4,
                        alert1Day = d1,
                        onSuccess = {
                            showAddSheet = false
                            eventToEdit = null
                        }
                    )
                }
            )
        }
    }
}

private fun Modifier.fillSomeWeight(): Modifier = this.fillMaxHeight().fillMaxWidth()

@Composable
fun EmptyCosmicState(onAddClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.05f),
            modifier = Modifier
                .size(120.dp)
                .border(1.dp, Color(0xFF00F0FF).copy(alpha = 0.3f), CircleShape)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Explore deep space logo",
                    tint = Color(0xFF00F0FF).copy(alpha = 0.6f),
                    modifier = Modifier.size(56.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "No Alarms in Orbit",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            fontFamily = FontFamily.SansSerif
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Your galactic reminder core is vacant. Touch the Add button to initiate a chronological tracker.",
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.5f),
            textAlign = TextAlign.Center,
            fontFamily = FontFamily.SansSerif,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
    }
}

@Composable
fun EventCard(
    event: Event,
    onToggleEnabled: (Boolean) -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.04f)
        ),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("event_card_${event.id}")
            .border(
                width = 1.dp,
                color = if (event.isEnabled) Color(0xFF00F0FF).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f),
                shape = RoundedCornerShape(20.dp)
            )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header Info & Status Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Tag
                val isUrgent = event.category == "SHORT_TERM" || event.category == "CUSTOM"
                val blockColor = if (isUrgent) Color(0xFFFF007F) else Color(0xFF00F0FF)
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(blockColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    val icon = when (event.category) {
                        "BIRTHDAY" -> Icons.Default.Favorite
                        "ANNIVERSARY" -> Icons.Default.Star
                        "SHORT_TERM" -> Icons.Default.PlayArrow
                        "LONG_TERM" -> Icons.Default.Notifications
                        else -> Icons.Default.Info
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = event.category,
                        tint = blockColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = event.category,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = blockColor,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Enabled Toggle Switch
                Switch(
                    checked = event.isEnabled,
                    onCheckedChange = { onToggleEnabled(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF00F0FF),
                        checkedTrackColor = Color(0xFF00F0FF).copy(alpha = 0.3f),
                        uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                        uncheckedTrackColor = Color.White.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.scale(0.85f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Event Title
            Text(
                text = event.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = if (event.isEnabled) Color.White else Color.White.copy(alpha = 0.4f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Event Description
            if (event.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = event.description,
                    fontSize = 13.sp,
                    color = if (event.isEnabled) Color.White.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.3f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Horizontal Split Divider
            Divider(color = Color.White.copy(alpha = 0.08f))

            Spacer(modifier = Modifier.height(12.dp))

            // Footer (Time / Date & Quick Actions)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Trigger detail
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Chronicle moment",
                        tint = Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${event.date} | ${event.time}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (event.isEnabled) Color(0xFF00F0FF) else Color.White.copy(alpha = 0.3f),
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Action buttons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(onClick = onEditClick) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Tracker",
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onDeleteClick) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Erase alarm orbit",
                            tint = Color(0xFFFF007F).copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Modifier.scale(scale: Float): Modifier = this.size((this.hashCode() % 10 + 40).dp * scale)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventBottomSheet(
    event: Event?,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        description: String,
        date: String,
        time: String,
        category: String,
        ringtone: String,
        vibrateStyle: String,
        exactAlert: Boolean,
        alert2Min: Boolean,
        alert4Hour: Boolean,
        alert1Day: Boolean
    ) -> Unit
) {
    var title by remember { mutableStateOf(event?.title ?: "") }
    var description by remember { mutableStateOf(event?.description ?: "") }
    var date by remember { mutableStateOf(event?.date ?: "") }
    var time by remember { mutableStateOf(event?.time ?: "") }
    var category by remember { mutableStateOf(event?.category ?: "BIRTHDAY") }
    var ringtone by remember { mutableStateOf(event?.ringtone ?: "CHIPTUNE") }
    var vibrateStyle by remember { mutableStateOf(event?.vibrateStyle ?: "STANDARD") }

    // Multi-tier Alert Schedules Checkboxes
    var alertExact by remember { mutableStateOf(true) }
    var alert2Min by remember { mutableStateOf(false) }
    var alert4Hour by remember { mutableStateOf(false) }
    var alert1Day by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0A0A0F), // Elegant Dark surface base
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = if (event == null) "Orbit A New Alarm" else "Refit Alarm Parameters",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Title Input
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Alarm Name", color = Color.White.copy(alpha = 0.6f)) },
                textStyle = LocalTextStyle.current.copy(color = Color.White),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF22D3EE),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                    focusedContainerColor = Color.White.copy(alpha = 0.03f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("event_title_input"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Description Input
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Details (Optional)", color = Color.White.copy(alpha = 0.6f)) },
                textStyle = LocalTextStyle.current.copy(color = Color.White),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF22D3EE),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                    focusedContainerColor = Color.White.copy(alpha = 0.03f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Row for Date & Time picker text fields
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Date picker trigger button
                Surface(
                    onClick = {
                        DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                calendar.set(Calendar.YEAR, year)
                                calendar.set(Calendar.MONTH, month)
                                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                date = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        ).apply {
                            // Enable dark-themes styling
                        }.show()
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.04f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Favorite, "Calendar Select", tint = Color(0xFF22D3EE))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = date.ifEmpty { "Select Date" },
                            color = if (date.isEmpty()) Color.White.copy(alpha = 0.5f) else Color.White,
                            fontSize = 14.sp
                        )
                    }
                }

                // Time picker trigger button
                Surface(
                    onClick = {
                        TimePickerDialog(
                            context,
                            { _, hourOfDay, minute ->
                                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                                calendar.set(Calendar.MINUTE, minute)
                                time = String.format("%02d:%02d", hourOfDay, minute)
                            },
                            calendar.get(Calendar.HOUR_OF_DAY),
                            calendar.get(Calendar.MINUTE),
                            true
                        ).show()
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.04f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Notifications, "Clock Select", tint = Color(0xFF22D3EE))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = time.ifEmpty { "Select Time" },
                            color = if (time.isEmpty()) Color.White.copy(alpha = 0.5f) else Color.White,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Category Selection Row pills
            Text("Orbit Class (Category)", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("BIRTHDAY", "ANNIVERSARY", "SHORT_TERM", "LONG_TERM", "CUSTOM").forEach { cat ->
                    val isSelected = category == cat
                    val col = if (cat == "SHORT_TERM" || cat == "CUSTOM") Color(0xFFC084FC) else Color(0xFF22D3EE)
                    
                    Surface(
                        onClick = { category = cat },
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) col.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f),
                        border = BorderStroke(1.dp, if (isSelected) col else Color.Transparent),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = cat,
                            color = if (isSelected) col else Color.White.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Multi-tier Alarms Checklist
            Text("Multi-Tier Chrono alerts", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = alertExact,
                        onCheckedChange = { alertExact = it },
                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFF22D3EE))
                    )
                    Text("Exact trigger moment (0h/0m offset)", color = Color.White, fontSize = 13.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = alert2Min,
                        onCheckedChange = { alert2Min = it },
                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFF22D3EE))
                    )
                    Text("2 minutes ahead (pre-alignment beacon)", color = Color.White, fontSize = 13.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = alert4Hour,
                        onCheckedChange = { alert4Hour = it },
                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFF22D3EE))
                    )
                    Text("4 hours ahead (pre-entry scan)", color = Color.White, fontSize = 13.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = alert1Day,
                        onCheckedChange = { alert1Day = it },
                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFF22D3EE))
                    )
                    Text("1 solar cycle before (24 hour early beacon)", color = Color.White, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Soundboard & Vibration options standard M3
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Ringtone SELECT
                Column(modifier = Modifier.weight(1f)) {
                    Text("Acoustic wave (Ringtone)", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    val ringtones = listOf("CHIPTUNE", "CHIME", "SIREN", "PULSE")
                    var expandedRing by remember { mutableStateOf(false) }
                    
                    Box {
                        Surface(
                            onClick = { expandedRing = true },
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White.copy(alpha = 0.05f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(ringtone, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Icon(Icons.Default.ArrowDropDown, null, tint = Color.White)
                            }
                        }
                        
                        DropdownMenu(
                            expanded = expandedRing,
                            onDismissRequest = { expandedRing = false },
                            modifier = Modifier.background(Color(0xFF0A0A0F))
                        ) {
                            ringtones.forEach { rt ->
                                DropdownMenuItem(
                                    text = { Text(rt, color = Color.White) },
                                    onClick = {
                                        ringtone = rt
                                        expandedRing = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Vibration SELECT
                Column(modifier = Modifier.weight(1f)) {
                    Text("Vibration sync pattern", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    val vibrations = listOf("NONE", "STANDARD", "HEARTBEAT", "SOS")
                    var expandedVib by remember { mutableStateOf(false) }
                    
                    Box {
                        Surface(
                            onClick = { expandedVib = true },
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White.copy(alpha = 0.05f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(vibrateStyle, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Icon(Icons.Default.ArrowDropDown, null, tint = Color.White)
                            }
                        }
                        
                        DropdownMenu(
                            expanded = expandedVib,
                            onDismissRequest = { expandedVib = false },
                            modifier = Modifier.background(Color(0xFF0A0A0F))
                        ) {
                            vibrations.forEach { vb ->
                                DropdownMenuItem(
                                    text = { Text(vb, color = Color.White) },
                                    onClick = {
                                        vibrateStyle = vb
                                        expandedVib = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Save Action Button
            val isFormValid = title.isNotEmpty() && date.isNotEmpty() && time.isNotEmpty()
            Button(
                onClick = {
                    if (isFormValid) {
                        onSave(title, description, date, time, category, ringtone, vibrateStyle, alertExact, alert2Min, alert4Hour, alert1Day)
                    }
                },
                enabled = isFormValid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF22D3EE),
                    contentColor = Color.Black,
                    disabledContainerColor = Color.White.copy(alpha = 0.08f),
                    disabledContentColor = Color.White.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("save_event_button")
            ) {
                Text("Align Coordinates (Save Task)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}
