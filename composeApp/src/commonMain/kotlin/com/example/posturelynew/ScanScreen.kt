package com.example.posturelynew

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
import com.example.posturelynew.scan.ScanImagesBridge
import com.example.posturelynew.scan.postPostureReport
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.Image
import com.example.posturelynew.scan.decodeBase64ToImageBitmap
import androidx.compose.ui.draw.rotate

@Composable
fun ScanScreen() {
    // Reuse app colors
    val pageBg = Color(0xFFFED867)
    val textPrimary = Color(0xFF0F1931)
    val accentBrown = Color(0xFF7A4B00)
    val cardBg = Color(0xFFFFF0C0)

    val scope = rememberCoroutineScope()
    var showPreview by remember { mutableStateOf(false) }
    var frontB64 by remember { mutableStateOf<String?>(null) }
    var sideB64 by remember { mutableStateOf<String?>(null) }

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
                // Listen once for images from native (iOS) and then post to edge
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
                .clickable { /* dummy */ },
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
        val fImg = frontB64?.let { decodeBase64ToImageBitmap(it) }
        val sImg = sideB64?.let { decodeBase64ToImageBitmap(it) }
        AlertDialog(
            onDismissRequest = { showPreview = false },
            confirmButton = {
                TextButton(onClick = { showPreview = false }) { Text("Close") }
            },
            title = { Text("Captured Images", color = textPrimary) },
            text = {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Front", color = textPrimary, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        if (fImg != null) {
                            Image(bitmap = fImg, contentDescription = "Front", modifier = Modifier.fillMaxWidth().height(180.dp).rotate(90f))
                        } else {
                            Text("Front image unavailable", color = textPrimary)
                        }
                    }
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Side", color = textPrimary, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        if (sImg != null) {
                            Image(bitmap = sImg, contentDescription = "Side", modifier = Modifier.fillMaxWidth().height(180.dp).rotate(90f))
                        } else {
                            Text("Side image unavailable", color = textPrimary)
                        }
                    }
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


