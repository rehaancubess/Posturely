package com.mobil80.posturely

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun DesktopSettingsMenu(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onLogout: () -> Unit
) {
    val pageBg = Color(0xFFFED867)
    val cardBg = Color(0xFFFFF0C0)
    val textPrimary = Color(0xFF0F1931)
    val accentBrown = Color(0xFF7A4B00)
    val accentYellow = Color(0xFFFFC233)
    
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    
    if (isOpen) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .width(320.dp)
                        .heightIn(min = 400.dp, max = 600.dp)
                        .clickable { /* Prevent dismiss when clicking menu */ },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Header
                        Text(
                            text = "Settings",
                            color = textPrimary,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        // Menu items
                        SettingsMenuItem(
                            text = "Contact Us",
                            onClick = {
                                openEmailClient("rehaancubes@gmail.com", subject = "Posturely Support", body = "")
                            }
                        )
                        
                        SettingsMenuItem(
                            text = "Privacy Policy",
                            onClick = {
                                openUrl("https://www.posturely.app/privacypolicy")
                            }
                        )
                        
                        SettingsMenuItem(
                            text = "Delete Account",
                            onClick = {
                                showDeleteAccountDialog = true
                            }
                        )
                        
                        Spacer(modifier = Modifier.weight(1f))
                        
                        // Logout button
                        Button(
                            onClick = { showLogoutDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = accentBrown),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "Log Out",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
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
                    Spacer(modifier = Modifier.height(6.dp))
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
                    Spacer(modifier = Modifier.height(6.dp))
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

@Composable
private fun SettingsMenuItem(
    text: String,
    onClick: () -> Unit
) {
    val cardBg = Color(0xFFFFF0C0)
    val textPrimary = Color(0xFF0F1931)
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(cardBg, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Text(
            text = text,
            color = textPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
