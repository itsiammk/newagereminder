package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WishGeneratorScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Inputs States
    var recipientName by remember { mutableStateOf("") }
    var selectedVibe by remember { mutableStateOf("CYBERPUNK") }
    var selectedCategory by remember { mutableStateOf("BIRTHDAY") }

    // Generator Output States
    var craftedCardText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var apiStatusMsg by remember { mutableStateOf("") }

    // local fallback card engine
    fun generateLocalFallbackCard(name: String, vibe: String, cat: String): String {
        val target = if (name.trim().isEmpty()) "Chronos Traveler" else name.trim()
        return when (cat) {
            "BIRTHDAY" -> {
                when (vibe) {
                    "CYBERPUNK" -> "🌌 ALERT: SOLAR CYCLE ROTATION DETECTED 🌌\n\nTo: $target\n\nInitializing core systems... Happy Orbit Commencement Day! Your carbon construct has spanned another solar vector. Upgrade firmware, clear temporary caches, and calibrate sensors for maximum light travel. May your hyper-drive run cool and your latency remain sub-millisecond!"
                    "HEARTBEAT" -> "💖 Solar Cycles Alignment Certificate 💖\n\nDearest $target,\n\nOn this exact solar milestone, I want to declare how deeply appreciated your presence is in this sector. You are the gravity well that holds my universe in balance. May this rotation bring you absolute peace, starry skies, and boundless warmth."
                    "POETIC" -> "📜 Verse of the Chrono Orbit 📜\n\nTo $target,\n\nYears flow like cosmic dust across the galactic pane,\nYet your light shines brighter with each passing lane.\nOn this day when the stars aligned to let you breathe,\nMay the galaxies wrap you in flowers of peace beneath."
                    else -> "🎭 Chronos Anomaly Warning! 🎭\n\nTo: $target\n\nCongrats! You are officially one solar year closer to system breakdown. Recommended actions: Drink high-octane fuels, consume organic glucose sheets (cake), and pretend you are mature. Happy Birthday!"
                }
            }
            "ANNIVERSARY" -> {
                when (vibe) {
                    "CYBERPUNK" -> "📡 CHRONIC LINK SYNCHRONIZED 📡\n\nActive Nodes: $target and Central Core\n\nWe have completed another year of mutual orbit without system drift or structural collapse. Connection throughput is stable at 100%. Initiating loyalty routine. Happy Anniversary! Let us calibrate files and proceed into the next horizon."
                    "HEARTBEAT" -> "❤️ Infinite Love Transmission ❤️\n\nHappy Anniversary, $target.\n\nTime is infinite, but my heart chose you in every parallel dimension. Sharing this journey with you is the highlight of my physical existence. I love you beyond coordinates."
                    "POETIC" -> "🌌 Two Stars in Conjugation 🌌\n\nMy beloved $target,\n\nLike binary stars dancing in the cold expansion,\nWe found warmth in each other’s gravity and mansion.\nAnother year woven in the starry tapestry of creation,\nHappy Anniversary to our endless, beautiful duration."
                    else -> "💍 Alliance Node Active! 💍\n\nTo $target:\n\nYet another year of ignoring my bugs and accepting my updates. It is truly a miracle of engineering. Happy Anniversary, partner-in-orbit!"
                }
            }
            else -> { // SUCCESS / CUSTOM
                when (vibe) {
                    "CYBERPUNK" -> "⚙️ VECTOR COMPLETED: CRITICAL SUCCESS ⚙️\n\nTo: $target\n\nYour achievements have surged above standard index levels. Calibration reveals a 99.8% output efficiency. Calibrating praise protocols... Keep ascending the corporate/galactic hierarchy. The universe is yours!"
                    "HEARTBEAT" -> "💫 Pride Beacon Transmit 💫\n\nDear $target,\n\nYour incredible victory is a source of joy to everyone around you. Watching you shine is the most inspiring light-path in this galaxy. You deserve all the stars!"
                    "POETIC" -> "✨ Whispers of the High Victory ✨\n\nTo $target,\n\nYou scaled the sky and caught the lightning's flare,\nNow victory rides upon your flowing hair.\nLet all the worlds celebrate your name,\nAs you touch the peak and light the timeless flame."
                    else -> "🏆 System Achieved! 🏆\n\nTo $target,\n\nWow, you actually did it. I was 92% sure you would crash-land, but you pulled off a perfect orbit. Good job, carbon unit!"
                }
            }
        }
    }

    // Direct Rest Gemini integration using OkHttp
    suspend fun fetchGeminiCard(prompt: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            throw IllegalStateException("API Key missing or unresolved")
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()

        val payload = JSONObject().apply {
            put("contents", org.json.JSONArray().put(
                JSONObject().put("parts", org.json.JSONArray().put(
                    JSONObject().put("text", prompt)
                ))
            ))
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.85)
            })
        }

        val mediaType = "application/json".toMediaType()
        val requestBody = payload.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("HTTP ${response.code}")
            val body = response.body?.string() ?: throw Exception("Empty response payload")
            val json = JSONObject(body)
            val candidates = json.getJSONArray("candidates")
            val candidate = candidates.getJSONObject(0)
            val contentObj = candidate.getJSONObject("content")
            val parts = contentObj.getJSONArray("parts")
            parts.getJSONObject(0).getString("text")
        }
    }

    fun craftCard() {
        isLoading = true
        apiStatusMsg = "Modulating AI neural matrices..."

        val promptStr = "Write a premium, micro greeting card for a recipient named '$recipientName' " +
                "for their '$selectedCategory' event in a distinct '$selectedVibe' vibe. " +
                "Make it extremely creative, high-fidelity, and stylized. " +
                "Keep it under 100 words max, starting with a cool visual ASCII text banner like '[[COSMIC ALIGNMENT]]' or '[[CHRONO TRAVELER]]' aligned to the theme!"

        scope.launch {
            try {
                val res = fetchGeminiCard(promptStr)
                craftedCardText = res
                apiStatusMsg = "Capsule Crafted Successfully"
            } catch (e: Exception) {
                // Graceful Instant Fallback
                val fallbackText = generateLocalFallbackCard(recipientName, selectedVibe, selectedCategory)
                craftedCardText = fallbackText
                apiStatusMsg = "Local Backup Engine Deployed"
            } finally {
                isLoading = false
            }
        }
    }

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
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Wish Generator",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "AI-Powered Chronological Greetings",
                fontSize = 14.sp,
                color = Color(0xFF22D3EE),
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Section: Configuration
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = "Recipient Identity",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = recipientName,
                        onValueChange = { recipientName = it },
                        placeholder = { Text("e.g. Commander Alice", color = Color.White.copy(alpha = 0.4f)) },
                        textStyle = LocalTextStyle.current.copy(color = Color.White),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFC084FC),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                            focusedContainerColor = Color.White.copy(alpha = 0.02f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("wish_recipient_input"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Orbit Paradigm (Vibe)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("CYBERPUNK", "HEARTBEAT", "POETIC", "FUNNY").forEach { vibe ->
                            val isSel = selectedVibe == vibe
                            val blockClr = if (vibe == "CYBERPUNK" || vibe == "FUNNY") Color(0xFF22D3EE) else Color(0xFFC084FC)
                            Surface(
                                onClick = { selectedVibe = vibe },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSel) blockClr.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.04f),
                                border = BorderStroke(1.dp, if (isSel) blockClr else Color.Transparent),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("wish_vibe_$vibe")
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 10.dp)) {
                                    Text(
                                        text = vibe,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSel) blockClr else Color.White
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Trigger Event (Category)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("BIRTHDAY", "ANNIVERSARY", "SUCCESS").forEach { cat ->
                            val isSel = selectedCategory == cat
                            Surface(
                                onClick = { selectedCategory = cat },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSel) Color(0xFF34D399).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.04f),
                                border = BorderStroke(1.dp, if (isSel) Color(0xFF34D399) else Color.Transparent),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 10.dp)) {
                                    Text(
                                        text = cat,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSel) Color(0xFF34D399) else Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Generate Button
            Button(
                onClick = { craftCard() },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFC084FC),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("craft_card_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Star, null, modifier = Modifier.size(20.dp))
                    Text("CRAFT CAPSULE (GENERATE)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress/Status feedback
            if (isLoading) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF22D3EE),
                        modifier = Modifier
                            .size(18.dp)
                            .padding(end = 6.dp),
                        strokeWidth = 2.dp
                    )
                    Text(text = apiStatusMsg, fontSize = 12.sp, color = Color.White.copy(alpha = 0.5f))
                }
            } else if (apiStatusMsg.isNotEmpty()) {
                Text(
                    text = "◆ $apiStatusMsg ◆",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF22D3EE),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
            }

            // Results View Card
            AnimatedVisibility(
                visible = craftedCardText.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 48.dp)
                        .testTag("crafted_wish_card")
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFF22D3EE).copy(alpha = 0.3f), Color.Transparent)
                            ),
                            shape = RoundedCornerShape(24.dp)
                        )
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "AI GREETING TRANSLATION",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF22D3EE),
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Text(
                            text = craftedCardText,
                            fontSize = 15.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            lineHeight = 22.sp,
                            modifier = Modifier.padding(bottom = 20.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Copy button
                            Button(
                                onClick = {
                                    val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Cosmic Reminder Card", craftedCardText)
                                    manager.setPrimaryClip(clip)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White.copy(alpha = 0.08f),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Done, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copy", fontSize = 13.sp)
                            }

                            // Share button
                            Button(
                                onClick = {
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, craftedCardText)
                                        type = "text/plain"
                                    }
                                    val shareIntent = Intent.createChooser(sendIntent, "Egress Galactic Capsule")
                                    context.startActivity(shareIntent)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF22D3EE).copy(alpha = 0.2f),
                                    contentColor = Color(0xFF22D3EE)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Share, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Egress (Share)", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}
