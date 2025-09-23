package com.mobil80.posturely

import androidx.compose.runtime.*
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.mobil80.posturely.DesktopLiveTrackingScreen

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Posturely - Posture Monitoring",
        state = rememberWindowState(size = DpSize(800.dp, 600.dp)),
        resizable = true
    ) {
        DesktopApp()
    }
}

@Composable
fun DesktopApp() {
    var currentScreen by remember { mutableStateOf("login") }
    var userEmail by remember { mutableStateOf("") }
    var isLoggedIn by remember { mutableStateOf(false) }
    
    // Check if user is already logged in and restore the appropriate screen
    LaunchedEffect(Unit) {
        val storage = PlatformStorage()
        val savedLoginState = storage.getBoolean("isLoggedIn", false)
        val savedUserEmail = storage.getString("userEmail", "")
        
        if (savedLoginState && savedUserEmail.isNotEmpty()) {
            isLoggedIn = true
            userEmail = savedUserEmail
            currentScreen = "main"
            println("🔧 [DESKTOP] Restored login state for user: $savedUserEmail")
        } else {
            println("🔧 [DESKTOP] No saved login state, starting with login screen")
        }
    }
    
    when (currentScreen) {
        "login" -> DesktopLoginScreen(
            onNavigateToOTP = { email ->
                userEmail = email
                if (email.equals("rehaan@mobil80.com", ignoreCase = true)) {
                    currentScreen = "password"
                } else {
                    // For desktop we treat OTP as direct login for now
                    isLoggedIn = true
                    currentScreen = "main"
                    val storage = PlatformStorage()
                    storage.saveBoolean("isLoggedIn", true)
                    storage.saveString("userEmail", email)
                    println("🔧 [DESKTOP] Saved login state for user: $email")
                }
            }
        )
        "password" -> PasswordScreen(
            email = userEmail,
            onNavigateToHome = {
                val storage = PlatformStorage()
                storage.saveBoolean("isLoggedIn", true)
                storage.saveString("userEmail", userEmail)
                isLoggedIn = true
                currentScreen = "main"
            },
            onBackToLogin = { currentScreen = "login" }
        )
        "main" -> DesktopMainScreen(
            onNavigateToLiveTracking = { currentScreen = "live_tracking" },
            onLogout = {
                // Clear login state
                val storage = PlatformStorage()
                storage.clear()
                isLoggedIn = false
                userEmail = ""
                currentScreen = "login"
                println("🔧 [DESKTOP] Logged out and cleared saved state")
            },
            onStartTracking = { currentScreen = "live_tracking_auto" }
        )
        "live_tracking" -> DesktopLiveTrackingScreen(
            onBackPressed = { currentScreen = "main" },
            userEmail = userEmail
        )
        "live_tracking_auto" -> DesktopLiveTrackingScreen(
            onBackPressed = { currentScreen = "main" },
            userEmail = userEmail,
            autoStart = true
        )
    }
}
