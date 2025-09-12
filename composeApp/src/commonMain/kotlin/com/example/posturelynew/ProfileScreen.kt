package com.example.posturelynew

import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import posturelynew.composeapp.generated.resources.Res
import posturelynew.composeapp.generated.resources.giraffeenew
import androidx.compose.ui.text.style.TextAlign
// import kwebview.KWebView // Temporarily disabled due to iOS compilation issues

@Composable
fun ProfileMenuItem(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(cardBg, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Text(text, color = textPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
    }
}

// Color definitions matching the design
private val pageBg = Color(0xFFFED867)
private val textPrimary = Color(0xFF0F1931)
private val cardBg = Color(0xFFFFF0C0)
private val accentBrown = Color(0xFF7A4B00)
private val accentYellow = Color(0xFFFFC233)

@OptIn(ExperimentalResourceApi::class)
@Composable
fun ProfileScreen(
    onLogout: () -> Unit
) {
    val storage = remember { PlatformStorage() }
    val email = remember { storage.getString("userEmail", "") }
    val userName = remember(email) {
        if (email.isNotBlank() && email.contains("@")) {
            val base = email.substringBefore('@')
            base.split('.', '_', '-').joinToString(" ") { part ->
                part.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
        } else "Emily Smith"
    }

    var showLogoutDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(pageBg)
    ) {
        // Separate horizontal/vertical scales (keep 15 Plus look as baseline)
        val hScale = (maxWidth / 390.dp).coerceIn(0.85f, 1.2f)
        val vScale = (maxHeight / 932.dp).coerceIn(0.85f, 1.2f)

        val pad = 24.dp * hScale
        val titleSize = (40.sp.value * hScale).sp
        val avatarSize = 140.dp * hScale
        val innerAvatar = 120.dp * hScale
        val nameSize = (32.sp.value * hScale).sp
        val emailSize = (12.sp.value * hScale).sp
        val logoutSize = (20.sp.value * hScale).sp

        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = maxHeight)
                .verticalScroll(scrollState)
                .padding(horizontal = pad, vertical = pad),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Text(
                text = "Profile",
                color = textPrimary,
                fontSize = titleSize,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(bottom = 16.dp * vScale)
            )

            // Avatar
            Box(
                modifier = Modifier
                    .size(avatarSize)
                    .background(cardBg, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(Res.drawable.giraffeenew),
                    contentDescription = "Avatar",
                    modifier = Modifier.size(innerAvatar),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(Modifier.height(16.dp * vScale))

            Text(
                text = userName,
                color = textPrimary,
                fontSize = nameSize,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 20.dp * vScale)
            )

            // Email chip
            if (email.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .background(cardBg, RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp * hScale, vertical = 6.dp * vScale)
                ) {
                    Text(email, color = textPrimary.copy(alpha = 0.8f), fontSize = emailSize, fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.height(20.dp * vScale))
            }

            // Menu items
            Column(verticalArrangement = Arrangement.spacedBy(12.dp * vScale)) {
                // ProfileMenuItem("Go Premium") { }
                ProfileMenuItem("Contact Us") {
                    openEmailClient("rehaancubes@gmail.com", subject = "Posturely Support", body = "")
                }
                ProfileMenuItem("Privacy Policy") {
                    // TODO: Implement webview - temporarily disabled due to iOS compilation issues
                    // For now, this could open external browser
                }
                ProfileMenuItem("Delete Account") {
                    showDeleteAccountDialog = true
                }
            }

            Spacer(Modifier.height(12.dp * vScale))

            // Logout button (brown)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(accentBrown, RoundedCornerShape(16.dp))
                    .clickable { showLogoutDialog = true }
                    .padding(vertical = 16.dp * vScale),
                contentAlignment = Alignment.Center
            ) {
                Text("Log Out", color = Color.White, fontSize = logoutSize, fontWeight = FontWeight.Bold)
            }

            // Logout Confirmation Dialog
            if (showLogoutDialog) {
                AlertDialog(
                    onDismissRequest = { showLogoutDialog = false },
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Are you sure you want to log out?",
                                color = textPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Spacer(modifier = Modifier.height(6.dp * vScale))
                            Box(
                                modifier = Modifier
                                    .height(4.dp)
                                    .width(56.dp)
                                    .background(accentYellow, shape = RoundedCornerShape(2.dp))
                            )
                        }
                    },
                    text = {
                        Text(
                            "You can log back in anytime to continue tracking.",
                            color = textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showLogoutDialog = false
                                onLogout()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = accentBrown)
                        ) {
                            Text("Log Out", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showLogoutDialog = false }) {
                            Text("Cancel", color = textPrimary, fontWeight = FontWeight.Bold)
                        }
                    },
                    shape = RoundedCornerShape(20.dp),
                    containerColor = cardBg
                )
            }

            // Delete Account Confirmation Dialog
            if (showDeleteAccountDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteAccountDialog = false },
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Delete Account",
                                color = textPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Spacer(modifier = Modifier.height(6.dp * vScale))
                            Box(
                                modifier = Modifier
                                    .height(4.dp)
                                    .width(56.dp)
                                    .background(accentYellow, shape = RoundedCornerShape(2.dp))
                            )
                        }
                    },
                    text = {
                        Text(
                            "Are you sure you want to delete your account? This action cannot be undone and all your data will be permanently lost.",
                            color = textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showDeleteAccountDialog = false
                                onLogout() // For now, just logout instead of actual deletion
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC3545)) // Red color for delete
                        ) {
                            Text("Delete Account", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteAccountDialog = false }) {
                            Text("Cancel", color = textPrimary, fontWeight = FontWeight.Bold)
                        }
                    },
                    shape = RoundedCornerShape(20.dp),
                    containerColor = cardBg
                )
            }
        }
    }
}