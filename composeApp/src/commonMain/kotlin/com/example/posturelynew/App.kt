package com.example.posturelynew

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.painterResource

import posturelynew.composeapp.generated.resources.Res
import posturelynew.composeapp.generated.resources.compose_multiplatform
import posturelynew.composeapp.generated.resources.homebottom
import posturelynew.composeapp.generated.resources.scansbottom
import posturelynew.composeapp.generated.resources.exercisesbottom
import posturelynew.composeapp.generated.resources.profilebottom
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.time.TimeSource
import kotlin.time.Duration.Companion.seconds
import com.example.posturelynew.supabase.Supa

@Composable
fun App() {
    MaterialTheme {
        // Platform-specific storage (like Flutter's SharedPreferences)
        val storage = remember { PlatformStorage() }
        
        // State variables
        var currentScreen by remember { mutableStateOf("splash") }
        var isDarkTheme by remember { mutableStateOf(false) }
        var isLoggedIn by remember { mutableStateOf(false) }
        var userEmail by remember { mutableStateOf("") }
        var isAirPodsTracking by remember { mutableStateOf(false) }
        
        // Check if user is already logged in and restore the appropriate screen
        // This is like checking SharedPreferences.getBool("isLoggedIn") in Flutter
        LaunchedEffect(Unit) {
            val savedLoginState = storage.getBoolean("isLoggedIn", false)
            if (savedLoginState) {
                isLoggedIn = true
                // Also restore the user email
                userEmail = storage.getString("userEmail", "")
            }
        }
        
        // Check Supabase authentication state
        LaunchedEffect(Unit) {
            // Note: In a real app, you'd collect from Supa.sessionStatus Flow
            // For now, we'll just check the stored login state
        }
        
        when (currentScreen) {
            "splash" -> SplashScreen(
                onSplashComplete = {
                    // After splash, go to appropriate screen based on login state
                    currentScreen = if (isLoggedIn) "home" else "intro"
                }
            )
            "intro" -> IntroScreen(
                onNavigateToLogin = { currentScreen = "login" }
            )
            "login" -> LoginScreen(
                onNavigateToOTP = { email -> 
                    userEmail = email
                    currentScreen = "otp"
                }
            )
            "otp" -> OTPScreen(
                email = userEmail,
                onNavigateToHome = { 
                    // Like SharedPreferences.setBool("isLoggedIn", true) in Flutter
                    storage.saveBoolean("isLoggedIn", true)
                    storage.saveString("userEmail", userEmail)
                    currentScreen = "home"
                    isLoggedIn = true
                },
                onBackToLogin = { currentScreen = "login" }
            )
            "home" -> HomeScreen(
                onNavigateBack = { currentScreen = "intro" },
                onNavigateToLiveTracking = { isAirPodsTracking = false; currentScreen = "live_tracking" },
                onNavigateToAirPodsTracking = { isAirPodsTracking = true; currentScreen = "live_tracking" },
                onNavigateToLaptopTracking = { currentScreen = "laptop_tracking" },
                onLogout = { 
                    // Like SharedPreferences.setBool("isLoggedIn", false) in Flutter
                    storage.saveBoolean("isLoggedIn", false)
                    storage.saveString("userEmail", "")
                    userEmail = ""
                    currentScreen = "intro"
                    isLoggedIn = false
                    
                    // Sign out from Supabase
                    // Note: In a real app, you'd call Supa.client.auth.signOut() from a coroutine scope
                    // For now, we'll just update the local state
                },
                isDark = isDarkTheme,
                onToggleTheme = { isDarkTheme = !isDarkTheme }
            )
            "live_tracking" -> LiveTrackingScreen(
                onBackPressed = { currentScreen = "home" },
                isAirPodsTracking = isAirPodsTracking,
                userEmail = userEmail
            )
            "laptop_tracking" -> LaptopTrackingPage(
                onNavigateBack = { currentScreen = "home" },
                userEmail = userEmail
            )
            "main" -> MainScreen(
                onNavigateToPostureTracking = { currentScreen = "posture" },
                onNavigateToCalibration = { currentScreen = "calibration" },
                onNavigateToHomepage = { currentScreen = "home" },
                onNavigateToIntro = { currentScreen = "intro" }
            )
            "posture" -> PostureTrackingScreen(
                onBackPressed = { currentScreen = "main" }
            )
            "calibration" -> PostureCalibrationScreen(
                onBackPressed = { currentScreen = "main" },
                onStartCalibration = { currentScreen = "calibration_front" }
            )
            "calibration_front" -> PostureCalibrationActiveScreen(
                onNavigateBack = { currentScreen = "calibration" },
                onComplete = { currentScreen = "main" }
            )
        }
    }
}

@Composable
private fun KeepAliveTab(visible: Boolean, content: @Composable () -> Unit) {
    // IMPORTANT: Call content() exactly once at a stable position to preserve state.
    // Only change modifiers (alpha/offset) so the subtree remains mounted.
    val showModifier = Modifier.graphicsLayer(alpha = 1f)
    val hiddenModifier = Modifier
        .graphicsLayer(alpha = 0f)
        .offset(x = 10000.dp) // move offscreen to avoid interactions

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(if (visible) showModifier else hiddenModifier)
    ) {
        content()
    }
}

@Composable
fun MainScreen(
    onNavigateToPostureTracking: () -> Unit,
    onNavigateToCalibration: () -> Unit,
    onNavigateToHomepage: () -> Unit,
    onNavigateToIntro: () -> Unit
) {
    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.primaryContainer)
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App Logo/Icon
        Image(
            painter = painterResource(Res.drawable.compose_multiplatform),
            contentDescription = "Posturely Logo",
            modifier = Modifier.size(120.dp)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // App Title
        Text(
            text = "Posturely",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        
        Text(
            text = "Monitor and correct your posture",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
            modifier = Modifier.padding(top = 8.dp)
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // Main Action Button
        Button(
            onClick = onNavigateToPostureTracking,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = "Start Posture Tracking",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Calibrate Posture Button
        Button(
            onClick = onNavigateToCalibration,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary
            )
        ) {
            Text(
                text = "Calibrate Posture",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Homepage Button
        Button(
            onClick = onNavigateToHomepage,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "Homepage",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Intro Button
        Button(
            onClick = onNavigateToIntro,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "Intro",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "How it works",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• Uses your device's camera to detect your pose\n• Analyzes your posture in real-time\n• Provides feedback to improve your posture\n• Works with MediaPipe pose detection",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun HomeScreen(
    onNavigateBack: () -> Unit,
    onNavigateToLiveTracking: () -> Unit,
    onNavigateToAirPodsTracking: () -> Unit,
    onNavigateToLaptopTracking: () -> Unit,
    onLogout: () -> Unit,
    isDark: Boolean,
    onToggleTheme: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    // Persist bottom tab selection across navigations
    val storage = remember { PlatformStorage() }
    LaunchedEffect(Unit) {
        // Force default to Track tab on app open
        selectedTab = 0
        storage.saveString("home_selected_tab", "0")
    }
    var isTrackingActive by remember { mutableStateOf(false) }
    val navBarBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val statusBarTopPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    // App-wide background color (match screenshot)
    val backgroundLight = Color(0xFFFED867)
    val backgroundDark = Color(0xFFFED867)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) backgroundDark else backgroundLight)
    ) {
        // Content area above nav bar
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = statusBarTopPadding, bottom = 72.dp + navBarBottomPadding)
        ) {
            // Keep all tabs mounted; just toggle visibility. This preserves state and coroutines.
            KeepAliveTab(visible = selectedTab == 0) {
                com.example.posturelynew.home.HomeDashboardPage(
                    isDark = isDark,
                    onToggleTheme = onToggleTheme,
                    onNavigateToLiveTracking = onNavigateToLiveTracking,
                    onNavigateToAirPodsTracking = onNavigateToAirPodsTracking,
                    onNavigateToLaptopTracking = onNavigateToLaptopTracking,
                    onTrackingStateChange = { active -> isTrackingActive = active }
                )
            }
            KeepAliveTab(visible = selectedTab == 1) { ScanScreen() }
            KeepAliveTab(visible = selectedTab == 2) { StatsScreen() }
            KeepAliveTab(visible = selectedTab == 3) { ProfileScreen(onLogout = onLogout) }
        }

        // Fixed bottom navigation bar like screenshot
        val barBg = Color(0xFFFFF0C0)
        val selectedColor = Color(0xFF5A3A00) // darker brown selected
        val unselected = Color(0xFFB8860B) // lighter brown unselected

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(barBg)
                .padding(bottom = navBarBottomPadding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(88.dp)
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("Track", "Scan", "Stats", "Profile").forEachIndexed { index, label ->
                    val isSelected = selectedTab == index
                    val iconTint = if (isSelected) selectedColor else unselected
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(
                                interactionSource = null,
                                indication = null,
                                enabled = true,
                                onClick = {
                                    selectedTab = index
                                    storage.saveString("home_selected_tab", index.toString())
                                }
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        when (index) {
                            0 -> {
                                // Track icon - smaller filled circle (less filled)
                                Canvas(modifier = Modifier.size(28.dp)) {
                                    drawCircle(
                                        color = iconTint,
                                        radius = size.minDimension * 0.4f
                                    )
                                }
                            }
                            1 -> {
                                // Scan icon - simple magnifying glass
                                Canvas(modifier = Modifier.size(28.dp)) {
                                    val w = size.width
                                    val h = size.height
                                    val radius = h * 0.28f
                                    val center = androidx.compose.ui.geometry.Offset(w * 0.45f, h * 0.45f)
                                    drawCircle(color = iconTint, radius = radius, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 5f), center = center)
                                    val handleStart = androidx.compose.ui.geometry.Offset(center.x + radius * 0.7f, center.y + radius * 0.7f)
                                    val handleEnd = androidx.compose.ui.geometry.Offset(w * 0.9f, h * 0.9f)
                                    drawLine(color = iconTint, start = handleStart, end = handleEnd, strokeWidth = 5f)
                                }
                            }
                            2 -> {
                                // Stats icon - three slimmer filled bars (less filled)
                                Canvas(modifier = Modifier.size(28.dp)) {
                                    val w = size.width
                                    val h = size.height
                                    val slot = w / 5f
                                    val barWidth = slot * 0.6f
                                    // left bar
                                    drawRoundRect(
                                        color = iconTint,
                                        topLeft = androidx.compose.ui.geometry.Offset(1f * slot + (slot - barWidth) / 2f, h * 0.60f),
                                        size = androidx.compose.ui.geometry.Size(barWidth, h * 0.30f),
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f)
                                    )
                                    // middle bar (tallest)
                                    drawRoundRect(
                                        color = iconTint,
                                        topLeft = androidx.compose.ui.geometry.Offset(2f * slot + (slot - barWidth) / 2f, h * 0.30f),
                                        size = androidx.compose.ui.geometry.Size(barWidth, h * 0.60f),
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f)
                                    )
                                    // right bar
                                    drawRoundRect(
                                        color = iconTint,
                                        topLeft = androidx.compose.ui.geometry.Offset(3f * slot + (slot - barWidth) / 2f, h * 0.50f),
                                        size = androidx.compose.ui.geometry.Size(barWidth, h * 0.40f),
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f)
                                    )
                                }
                            }
                            3 -> {
                                // Profile icon - smaller head and slimmer shoulders (less filled)
                                Canvas(modifier = Modifier.size(28.dp)) {
                                    val w = size.width
                                    val h = size.height
                                    // head
                                    drawCircle(
                                        color = iconTint,
                                        radius = h * 0.18f,
                                        center = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.34f)
                                    )
                                    // shoulders/body
                                    drawRoundRect(
                                        color = iconTint,
                                        topLeft = androidx.compose.ui.geometry.Offset(w * 0.28f, h * 0.58f),
                                        size = androidx.compose.ui.geometry.Size(w * 0.44f, h * 0.26f),
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(7f, 7f)
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            label,
                            color = if (isSelected) selectedColor else unselected,
                            fontSize = 16.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

/* Deprecated: replaced by dashboard/DashboardScreen.kt
@Composable
private fun DashboardContentLight() {
    val pageBg = Color(0xFFEFF3F7)
    val cardBg = Color.White
    val textPrimary = Color(0xFF111418)
    val textSecondary = Color(0xFF667085)
    val chipBg = Color(0xFFF1F5FB)

    val postureInterface = rememberPostureTrackingInterface()
    var isMonitoring by remember { mutableStateOf(false) }
    var currentRatio by remember { mutableStateOf(0f) }
    val recentRatios = remember { mutableStateListOf<Float>() }

    // Stable good/bad with 1s threshold
    var stableIsGood by remember { mutableStateOf(true) }
    var candidateState: Boolean? by remember { mutableStateOf(null) }
    var candidateSince by remember { mutableStateOf(TimeSource.Monotonic.markNow()) }

    LaunchedEffect(isMonitoring) {
        while (isMonitoring) {
            val data = postureInterface.getPoseData()
            val ratio = parseRatioFromPoseData(data)
            currentRatio = ratio
            if (ratio > 0f) {
                recentRatios.add(ratio)
                if (recentRatios.size > 30) recentRatios.removeFirst()
            }

            val avg = if (recentRatios.isNotEmpty()) recentRatios.sum() / recentRatios.size else 0f
            val immediateIsGood = if (avg <= 0f) true else ratio >= (avg - 0.05f)
            if (immediateIsGood != stableIsGood) {
                if (candidateState == immediateIsGood) {
                    if (candidateSince.elapsedNow() >= 1.seconds) {
                        stableIsGood = immediateIsGood
                    }
                } else {
                    candidateState = immediateIsGood
                    candidateSince = TimeSource.Monotonic.markNow()
                }
            } else {
                candidateState = null
            }

            delay(250)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(pageBg)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Header row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Logo circle
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFFBFD6E6), CircleShape)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Hello, Fred", color = textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Text(" 44B", fontSize = 20.sp)
            }
            // Avatar circle
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(Color(0xFFFFE0B2), CircleShape)
            )
        }

        Spacer(Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Dashboard", color = textPrimary, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(Color(0xFF1F2328), CircleShape)
            )
        }

        Spacer(Modifier.height(16.dp))

        // Stats row (Calories / Weight)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            SmallStatCard(
                modifier = Modifier.weight(1f),
                title = "Calories",
                subtitle = "Great physical\nactivity",
                value = "1290/2340Kcal",
                chip = "1d",
                iconBg = Color(0xFFFFE6DB),
                iconDot = Color(0xFFEF6C00)
            )
            SmallStatCard(
                modifier = Modifier.weight(1f),
                title = "Weight",
                subtitle = "Healthy weight is\n72-82kg",
                value = "198lbs",
                chip = "6'0\"",
                iconBg = Color(0xFFE7F3D7),
                iconDot = Color(0xFF7CB342)
            )
        }

        Spacer(Modifier.height(16.dp))

        // Monitoring card (replacing heart rate)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color(0xFFAED581), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    // inner ring
                    Canvas(Modifier.size(22.dp)) {
                        drawCircle(Color(0xFF2E7D32), style = Stroke(width = 4f))
                        drawCircle(Color(0xFF2E7D32), radius = size.minDimension * 0.12f)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = if (isMonitoring) "Monitoring…" else "Not monitoring", color = textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(text = if (isMonitoring) "Currently active" else "Choose a source to start", color = textSecondary, fontSize = 14.sp)
                }
                if (isMonitoring) {
                    TextButton(onClick = {
                        isMonitoring = false
                        postureInterface.stopTracking()
                    }) { Text("Stop") }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        if (isMonitoring) {
            // Posture status
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Posture", color = textPrimary, fontWeight = FontWeight.SemiBold)
                        Text(if (stableIsGood) "Good" else "Bad", color = if (stableIsGood) Color(0xFF2E7D32) else Color(0xFFC62828), fontSize = 14.sp)
                    }
                    Text("Ratio ${formatThreeDecimals(currentRatio)}", color = textSecondary, fontSize = 12.sp)
                }
            }
        } else {
            // Start monitoring options
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        postureInterface.startTracking()
                        isMonitoring = true
                    },
                    shape = RoundedCornerShape(16.dp)
                ) { Text("Use phone camera") }

                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        // Placeholder for laptop start — for now behave same way
                        postureInterface.startTracking()
                        isMonitoring = true
                    },
                    shape = RoundedCornerShape(16.dp)
                ) { Text("Use laptop") }
            }
        }
    }
}
*/

@Composable
private fun BadgeCard(
    modifier: Modifier = Modifier,
    gradient: Brush,
    icon: @Composable () -> Unit,
    titleTop: String,
    titleBottom: String
) {
    Card(
        modifier = modifier.height(120.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .background(gradient)
                .fillMaxSize()
                .padding(14.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                icon()
                Spacer(modifier = Modifier.height(10.dp))
                Text(titleTop, color = Color.White, textAlign = TextAlign.Center, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Text(titleBottom, color = Color.White, textAlign = TextAlign.Center, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun SimpleCircleIcon() {
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(Color.White, shape = CircleShape)
    )
}

@Composable
private fun RingIcon() {
    Canvas(modifier = Modifier.size(36.dp)) {
        val stroke = 6.dp.toPx()
        drawCircle(
            brush = Brush.sweepGradient(listOf(Color(0xFF00E676), Color(0xFF40C4FF), Color(0xFF00E676))),
            style = Stroke(width = stroke)
        )
    }
}

@Composable
private fun DummyScreenDark(title: String) {
    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFFEFF3F7)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(Color(0xFFBFD6E6), shape = CircleShape)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color(0xFF111418))
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "This is a placeholder screen.", style = MaterialTheme.typography.bodyLarge, color = Color(0xFF667085))
    }
}

@Composable
private fun DummyScreenLight(title: String) {
    DummyScreenDark(title)
}

@Composable
private fun SmallStatCard(
    title: String,
    subtitle: String,
    value: String,
    chip: String,
    iconBg: Color,
    iconDot: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(iconBg, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(modifier = Modifier.size(10.dp).background(iconDot, CircleShape))
                }
                Box(
                    modifier = Modifier
                        .background(Color(0xFFF1F5FB), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) { Text(chip, color = Color(0xFF667085), fontSize = 12.sp) }
            }
            Spacer(Modifier.height(8.dp))
            Text(title, color = Color(0xFF111418), fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, color = Color(0xFF667085), fontSize = 12.sp)
            Spacer(Modifier.height(10.dp))
            Text(value, color = Color(0xFF111418), fontWeight = FontWeight.Bold)
        }
    }
}

// Helpers used by dashboard monitoring
private fun parseRatioFromPoseData(data: String): Float {
    return try {
        val lines = data.split("\n")
        for (line in lines) {
            if (line.contains("Nose to Shoulder Center:")) {
                val ratioStr = line.split(":").lastOrNull()?.trim()
                return ratioStr?.toFloatOrNull() ?: 0f
            }
        }
        0f
    } catch (_: Exception) {
        0f
    }
}

private fun formatThreeDecimals(value: Float): String {
    val thousandths = kotlin.math.round(value * 1000f).toInt()
    val sign = if (thousandths < 0) "-" else ""
    val absThousandths = kotlin.math.abs(thousandths)
    val whole = absThousandths / 1000
    val frac = absThousandths % 1000
    val fracStr = frac.toString().padStart(3, '0')
    return "$sign$whole.$fracStr"
}

// --- Minimal vector-like icons (placeholder shapes) ---
@Composable
private fun HomeIcon(tint: Color) {
    Box(Modifier.size(24.dp)) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height
            drawLine(tint, Offset(w*0.15f, h*0.55f), Offset(w*0.5f, h*0.2f), strokeWidth = 4f)
            drawLine(tint, Offset(w*0.85f, h*0.55f), Offset(w*0.5f, h*0.2f), strokeWidth = 4f)
            drawRect(color = tint, topLeft = Offset(w*0.25f, h*0.5f), size = Size(w*0.5f, h*0.35f), style = Stroke(width = 4f))
        }
    }
}

@Composable
private fun BookmarkIcon(tint: Color) {
    Canvas(Modifier.size(22.dp)) {
        val stroke = 4f
        drawRoundRect(color = tint, style = Stroke(width = stroke))
        drawLine(tint, Offset(size.width/2, size.height - stroke/2), Offset(size.width/2, size.height*0.65f), strokeWidth = stroke)
    }
}

@Composable
private fun BellIcon(tint: Color) {
    Canvas(Modifier.size(22.dp)) {
        drawArc(color = tint, startAngle = 200f, sweepAngle = 140f, useCenter = false, style = Stroke(width = 4f))
        drawLine(tint, Offset(size.width*0.25f, size.height*0.7f), Offset(size.width*0.75f, size.height*0.7f), strokeWidth = 4f)
    }
}

@Composable
private fun GearIcon(tint: Color) {
    Canvas(Modifier.size(22.dp)) {
        drawCircle(color = tint, style = Stroke(width = 4f))
        // simple teeth
        drawLine(tint, Offset(size.width/2, 0f), Offset(size.width/2, size.height*0.2f), strokeWidth = 4f)
        drawLine(tint, Offset(size.width, size.height/2), Offset(size.width*0.8f, size.height/2), strokeWidth = 4f)
        drawLine(tint, Offset(size.width/2, size.height), Offset(size.width/2, size.height*0.8f), strokeWidth = 4f)
        drawLine(tint, Offset(0f, size.height/2), Offset(size.width*0.2f, size.height/2), strokeWidth = 4f)
    }
}
