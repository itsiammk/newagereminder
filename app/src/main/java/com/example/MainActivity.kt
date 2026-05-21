package com.example

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.DashboardScreen
import com.example.ui.EventsViewModel
import com.example.ui.SoundTesterScreen
import com.example.ui.WishGeneratorScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    // Notification permission launcher
    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d("MainActivity", "Notification permission granted")
        } else {
            Toast.makeText(this, "Notification permission is required for alarms", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Immerse edge-to-edge layout
        enableEdgeToEdge()

        // Check and prompt permissions
        checkPermissions()

        setContent {
            MyApplicationTheme {
                val viewModel: EventsViewModel = viewModel()
                var currentTab by remember { mutableStateOf("DASHBOARD") }

                val configuration = LocalConfiguration.current
                val isWideScreen = configuration.screenWidthDp >= 600

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF050508))
                        .drawBehind {
                            // Top-left purple/violet cosmic glow
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color(0x227E22CE), // Purple-700 translucent
                                        Color.Transparent
                                    ),
                                    center = Offset(0f, 0f),
                                    radius = size.minDimension * 1.1f
                                ),
                                radius = size.minDimension * 1.1f,
                                center = Offset(0f, 0f)
                            )
                            // Bottom-right cyan cosmic glow
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color(0x1806B6D4), // Cyan-500 translucent
                                        Color.Transparent
                                    ),
                                    center = Offset(size.width, size.height),
                                    radius = size.minDimension * 1.1f
                                ),
                                radius = size.minDimension * 1.1f,
                                center = Offset(size.width, size.height)
                            )
                        }
                ) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = Color.Transparent,
                        bottomBar = {
                            if (!isWideScreen) {
                                GlassyBottomBar(
                                    selectedTab = currentTab,
                                    onTabSelect = { currentTab = it }
                                )
                            }
                        }
                    ) { innerPadding ->
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(
                                    bottom = if (!isWideScreen) innerPadding.calculateBottomPadding() else 0.dp,
                                    top = innerPadding.calculateTopPadding()
                                )
                        ) {
                            if (isWideScreen) {
                                AdaptiveNavigationRail(
                                    selectedTab = currentTab,
                                    onTabSelect = { currentTab = it }
                                )
                            }

                            // Rendering selected tab
                            Box(modifier = Modifier.weight(1f)) {
                                when (currentTab) {
                                    "DASHBOARD" -> DashboardScreen(viewModel = viewModel)
                                    "SOUNDS" -> SoundTesterScreen()
                                    "WISH" -> WishGeneratorScreen()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun checkPermissions() {
        // 1. Post Notification Permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // 2. Exact Alarm Permission (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                try {
                    val intent = Intent().apply {
                        action = Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                        data = Uri.fromParts("package", packageName, null)
                    }
                    startActivity(intent)
                    Toast.makeText(this, "Please enable exact alarm permission for bulletproof alarms", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Log.e("MainActivity", "Failed exact alarm request routing", e)
                }
            }
        }
    }
}

@Composable
fun GlassyBottomBar(
    selectedTab: String,
    onTabSelect: (String) -> Unit
) {
    NavigationBar(
        containerColor = Color(0xFF0A0A0F),
        modifier = Modifier
            .testTag("bottom_nav_bar")
            .drawBehind {
                drawLine(
                    color = Color.White.copy(alpha = 0.08f),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx()
                )
            }
    ) {
        NavigationBarItem(
            selected = selectedTab == "DASHBOARD",
            onClick = { onTabSelect("DASHBOARD") },
            icon = { Icon(Icons.Default.Home, "Dashboard") },
            label = { Text("Core", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF22D3EE),
                selectedTextColor = Color(0xFF22D3EE),
                indicatorColor = Color(0xFF22D3EE).copy(alpha = 0.15f),
                unselectedIconColor = Color.White.copy(alpha = 0.4f),
                unselectedTextColor = Color.White.copy(alpha = 0.4f)
            )
        )

        NavigationBarItem(
            selected = selectedTab == "SOUNDS",
            onClick = { onTabSelect("SOUNDS") },
            icon = { Icon(Icons.Default.Notifications, "Sound tester board") },
            label = { Text("Soundboard", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFFC084FC),
                selectedTextColor = Color(0xFFC084FC),
                indicatorColor = Color(0xFFC084FC).copy(alpha = 0.15f),
                unselectedIconColor = Color.White.copy(alpha = 0.4f),
                unselectedTextColor = Color.White.copy(alpha = 0.4f)
            )
        )

        NavigationBarItem(
            selected = selectedTab == "WISH",
            onClick = { onTabSelect("WISH") },
            icon = { Icon(Icons.Default.Star, "AI wish generator") },
            label = { Text("AI Generator", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF34D399),
                selectedTextColor = Color(0xFF34D399),
                indicatorColor = Color(0xFF34D399).copy(alpha = 0.15f),
                unselectedIconColor = Color.White.copy(alpha = 0.4f),
                unselectedTextColor = Color.White.copy(alpha = 0.4f)
            )
        )
    }
}

@Composable
fun AdaptiveNavigationRail(
    selectedTab: String,
    onTabSelect: (String) -> Unit
) {
    NavigationRail(
        containerColor = Color(0xFF0A0A0F),
        modifier = Modifier
            .testTag("side_navigation_rail")
            .drawBehind {
                drawLine(
                    color = Color.White.copy(alpha = 0.08f),
                    start = Offset(size.width, 0f),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            }
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        
        NavigationRailItem(
            selected = selectedTab == "DASHBOARD",
            onClick = { onTabSelect("DASHBOARD") },
            icon = { Icon(Icons.Default.Home, "Dashboard") },
            label = { Text("Core") },
            colors = NavigationRailItemDefaults.colors(
                selectedIconColor = Color(0xFF22D3EE),
                selectedTextColor = Color(0xFF22D3EE),
                indicatorColor = Color(0xFF22D3EE).copy(alpha = 0.15f),
                unselectedIconColor = Color.White.copy(alpha = 0.4f),
                unselectedTextColor = Color.White.copy(alpha = 0.4f)
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        NavigationRailItem(
            selected = selectedTab == "SOUNDS",
            onClick = { onTabSelect("SOUNDS") },
            icon = { Icon(Icons.Default.Notifications, "Sounds preview") },
            label = { Text("Soundboard") },
            colors = NavigationRailItemDefaults.colors(
                selectedIconColor = Color(0xFFC084FC),
                selectedTextColor = Color(0xFFC084FC),
                indicatorColor = Color(0xFFC084FC).copy(alpha = 0.15f),
                unselectedIconColor = Color.White.copy(alpha = 0.4f),
                unselectedTextColor = Color.White.copy(alpha = 0.4f)
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        NavigationRailItem(
            selected = selectedTab == "WISH",
            onClick = { onTabSelect("WISH") },
            icon = { Icon(Icons.Default.Star, "Wishes generator") },
            label = { Text("AI Generator") },
            colors = NavigationRailItemDefaults.colors(
                selectedIconColor = Color(0xFF34D399),
                selectedTextColor = Color(0xFF34D399),
                indicatorColor = Color(0xFF34D399).copy(alpha = 0.15f),
                unselectedIconColor = Color.White.copy(alpha = 0.4f),
                unselectedTextColor = Color.White.copy(alpha = 0.4f)
            )
        )
    }
}
