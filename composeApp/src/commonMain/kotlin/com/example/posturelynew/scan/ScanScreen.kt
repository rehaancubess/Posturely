package com.example.posturelynew.scan

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ScanScreen() {
    val bgGradient = Brush.verticalGradient(listOf(Color(0xFF0D1730), Color(0xFF081024)))
    val cardDark = Color(0xFF0E1D34)
    val cardLight = Color(0xFFE9F0F7).copy(alpha = 0.12f)
    val textPrimary = Color.White
    val accentGreen = Color(0xFF37D483)
    val primaryBlue = Color(0xFF1C3B7A)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgGradient)
            .padding(16.dp)
            .wrapContentWidth(Alignment.CenterHorizontally)
            .widthIn(max = 420.dp)
    ) {
        // Title
        Text("Scan", color = textPrimary, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(8.dp))

        // Monitoring On
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(12.dp).background(accentGreen, CircleShape))
            Spacer(Modifier.width(8.dp))
            Text("Monitoring On", color = accentGreen, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(16.dp))

        // Top row: Start Scan button and ring card
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = primaryBlue)
            ) {
                Box(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                    Button(
                        onClick = { /* TODO start scan */ },
                        modifier = Modifier.fillMaxWidth().height(64.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF223A85))
                    ) { Text("Start Scan", fontSize = 22.sp, fontWeight = FontWeight.Bold) }
                }
            }

            Card(
                modifier = Modifier.size(150.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.fillMaxSize().padding(16.dp)) {
                        // Background ring
                        drawArc(
                            brush = Brush.sweepGradient(listOf(Color(0xFF1A2140), Color(0xFF1A2140))),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = 18.dp.toPx())
                        )
                        // Green progress arc
                        drawArc(
                            color = accentGreen,
                            startAngle = -90f,
                            sweepAngle = 90f,
                            useCenter = false,
                            style = Stroke(width = 18.dp.toPx())
                        )
                    }
                    // Silhouette placeholder
                    Canvas(Modifier.size(64.dp)) {
                        drawCircle(Color(0xFF2FB4FF), radius = size.minDimension * 0.18f, center = Offset(size.width * 0.5f, size.height * 0.25f))
                        drawRoundRect(
                            color = Color(0xFF2FB4FF),
                            topLeft = Offset(size.width * 0.35f, size.height * 0.35f),
                            size = Size(size.width * 0.3f, size.height * 0.45f),
                            cornerRadius = CornerRadius(12f, 12f)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Middle grid
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // Left tall silhouette card
            Card(
                modifier = Modifier.weight(1f).height(260.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = cardDark)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    // Full body silhouette placeholder
                    Canvas(Modifier.fillMaxSize().padding(24.dp)) {
                        // trunk
                        drawRoundRect(color = Color(0xFF2FB4FF), topLeft = Offset(size.width*0.45f, size.height*0.25f), size = Size(size.width*0.1f, size.height*0.45f), cornerRadius = CornerRadius(8f,8f))
                        // head
                        drawCircle(Color(0xFF2FB4FF), radius = size.minDimension*0.06f, center = Offset(size.width*0.5f, size.height*0.20f))
                        // legs
                        drawRoundRect(color = Color(0xFF2FB4FF), topLeft = Offset(size.width*0.43f, size.height*0.70f), size = Size(size.width*0.06f, size.height*0.22f), cornerRadius = CornerRadius(8f,8f))
                        drawRoundRect(color = Color(0xFF2FB4FF), topLeft = Offset(size.width*0.51f, size.height*0.70f), size = Size(size.width*0.06f, size.height*0.22f), cornerRadius = CornerRadius(8f,8f))
                    }
                }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricPill(title = "Symmetry", value = "93")
                MetricCard(title = "Shoulder\nAlignment", value = "74", ringColor = Color(0xFF32D671))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SmallMetric(title = "58\nHead\nTilt", modifier = Modifier.weight(1f))
                    SmallMetric(title = "Spine\nCurve\n93", modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SmallMetric(title = "Pelvis\nTilt\n63", modifier = Modifier.weight(1f))
                    SmallMetric(title = "Knee\nAlignm\n", modifier = Modifier.weight(1f))
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Report card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Score chip
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFD5F6EA))) {
                    Box(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Text("93", color = Color(0xFF0C8E63), fontWeight = FontWeight.ExtraBold)
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("You've great symmetry!", color = Color(0xFF0B152D), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Your posture is generally well-\nbalanced, but you may benefit from\nfocusing on your pelvis and\nknee alignment.",
                        color = Color(0xFF415167),
                        fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Bottom segmented filter
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SegPill("Full body", selected = true)
            SegPill("Front", selected = false)
            SegPill("Side", selected = false)
        }
    }
}

@Composable
private fun MetricPill(title: String, value: String) {
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f))) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = Color.White, fontSize = 14.sp)
            Text(value, color = Color.White, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun MetricCard(title: String, value: String, ringColor: Color) {
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.12f))) {
        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.SemiBold)
            }
            Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    drawArc(color = ringColor, startAngle = -90f, sweepAngle = 240f, useCenter = false, style = Stroke(width = 6.dp.toPx()))
                }
            }
            Spacer(Modifier.width(8.dp))
            Text(value, color = Color.White, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun SmallMetric(title: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.12f))) {
        Box(Modifier.padding(14.dp)) {
            Text(title, color = Color.White)
        }
    }
}

@Composable
private fun SegPill(label: String, selected: Boolean) {
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = if (selected) Color.White.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.06f))) {
        Box(Modifier.padding(horizontal = 18.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
            Text(label, color = Color.White, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
        }
    }
}

