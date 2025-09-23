package com.mobil80.posturely

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LaptopTrackingPage(
    onNavigateBack: () -> Unit = {},
    userEmail: String = ""
) {
    val pageBgDefault = Color(0xFFFED866)
    val textPrimary = Color(0xFF0F1931)
    val subText = Color(0xFF6B7280)
    val accentBrown = Color(0xFF7A4B00)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(pageBgDefault)
    ) {
        // Back button anchored and padded away from status bar
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 24.dp, top = 56.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Card(
                modifier = Modifier
                    .size(40.dp)
                    .clickable { onNavigateBack() },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "←",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                }
            }
        }

        // Centered main content
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Laptop Tracking",
                fontSize = 38.sp,
                fontWeight = FontWeight.ExtraBold,
                color = textPrimary
            )
        
            Spacer(modifier = Modifier.height(12.dp))
           

            Spacer(modifier = Modifier.height(36.dp))

            // Bullet points
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Bullet(text = "Download the desktop app at www.posturely.app", textPrimary = textPrimary)
                Spacer(Modifier.height(16.dp))
                Bullet(text = "Privacy First – Your posture data is stored securely and never shared.", textPrimary = textPrimary)
                Spacer(Modifier.height(16.dp))
                Bullet(text = "Seamless Sync – Whether you log in with QR or email, your data stays synced across devices.", textPrimary = textPrimary)
                Spacer(Modifier.height(16.dp))
                if (userEmail.isNotBlank()) {
                    Bullet(text = "Use email: $userEmail", textPrimary = textPrimary)
                }
            }
        }
    }
}

@Composable
private fun Bullet(text: String, textPrimary: Color) {
    Row(verticalAlignment = Alignment.Top) {
        Text("•", color = textPrimary, fontSize = 24.sp, modifier = Modifier.padding(end = 12.dp))
        Text(text, color = textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Medium)
    }
}
