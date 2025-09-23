package com.mobil80.posturely

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.painterResource
import posturelynew.composeapp.generated.resources.Res
import posturelynew.composeapp.generated.resources.scans
import com.mobil80.posturely.scan.ScanImagesBridge
import com.mobil80.posturely.scan.postPostureReport
import com.mobil80.posturely.openNativeScanCamera
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.ContentScale
import com.mobil80.posturely.scan.Base64Image
import kotlinx.coroutines.delay

@Composable
fun ScanScreen(
    onNavigateToReports: () -> Unit = {}
) {
    // Reuse app colors
    val pageBg = Color(0xFFFED867)
    val textPrimary = Color(0xFF0F1931)
    val accentBrown = Color(0xFF7A4B00)
    val cardBg = Color(0xFFFFF0C0)

    val scope = rememberCoroutineScope()
    var showPreview by remember { mutableStateOf(false) }
    var frontB64 by remember { mutableStateOf<String?>(null) }
    var sideB64 by remember { mutableStateOf<String?>(null) }
    var showAnalyzing by remember { mutableStateOf(false) }

    // Ensure listener survives navigation and updates state when native posts images
    LaunchedEffect(Unit) {
        ScanImagesBridge.onImagesReady = { _, f, s ->
            frontB64 = f
            sideB64 = s
            showPreview = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(pageBg)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Scans",
                color = textPrimary,
                fontSize = 40.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
        }

        // Illustration above the points
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(Res.drawable.scans),
                contentDescription = "Scans illustration",
                modifier = Modifier.fillMaxHeight(),
                contentScale = androidx.compose.ui.layout.ContentScale.Fit
            )
        }

        Spacer(Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardBg, RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Bullet(text = "Posture analysis", textPrimary = textPrimary)
            Spacer(Modifier.height(12.dp))
            Bullet(text = "Body measurements", textPrimary = textPrimary)
            Spacer(Modifier.height(12.dp))
            Bullet(text = "Symmetry assessment", textPrimary = textPrimary)
            Spacer(Modifier.height(12.dp))
            Bullet(text = "Balance evaluation", textPrimary = textPrimary)
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                // Use cross-platform bridge to open native scan camera
                openNativeScanCamera()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = accentBrown)
        ) {
            Text(
                text = "Take Full Body Scan",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(cardBg, RoundedCornerShape(20.dp))
                .clickable { onNavigateToReports() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "View Previous Scans",
                color = textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }

    if (showPreview) {
        AlertDialog(
            onDismissRequest = { showPreview = false },
            confirmButton = {
                Button(
                    onClick = {
                        // Start analyzing flow
                        showPreview = false
                        showAnalyzing = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentBrown)
                ) { Text("Analyze", color = Color.White, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showPreview = false }) { Text("Cancel") }
            },
            title = { Text("Captured Images", color = textPrimary) },
            text = {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Front", color = textPrimary, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        frontB64?.let { front ->
                            Base64Image(
                                base64String = front,
                                modifier = Modifier
                                    .width(160.dp)
                                    .height(240.dp)
                                    .rotate(90f),
                                contentScale = ContentScale.Fit
                            )
                        } ?: Text("Front image unavailable", color = textPrimary)
                    }
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Side", color = textPrimary, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        sideB64?.let { side ->
                            Base64Image(
                                base64String = side,
                                modifier = Modifier
                                    .width(160.dp)
                                    .height(240.dp)
                                    .rotate(90f),
                                contentScale = ContentScale.Fit
                            )
                        } ?: Text("Side image unavailable", color = textPrimary)
                    }
                }
            },
            containerColor = cardBg
        )
    }

    if (showAnalyzing) {
        // Auto-close after ~5 seconds and navigate to reports
        LaunchedEffect(Unit) {
            delay(5000)
            showAnalyzing = false
            onNavigateToReports()
        }
        AlertDialog(
            onDismissRequest = { /* block dismiss while analyzing */ },
            confirmButton = {},
            title = { Text("Analyzing your posture", color = textPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    // Reuse the scans illustration image
                    Image(
                        painter = painterResource(Res.drawable.scans),
                        contentDescription = "Analyzing",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(Modifier.height(16.dp))
                    CircularProgressIndicator(color = accentBrown)
                }
            },
            containerColor = cardBg
        )
    }
}

@Composable
private fun Bullet(text: String, textPrimary: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("•", color = textPrimary, fontSize = 22.sp, modifier = Modifier.padding(end = 12.dp))
        Text(text, color = textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Medium)
    }
}


