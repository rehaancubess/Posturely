package com.mobil80.posturely.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DashboardScreen() {
    // Background gradient similar to screenshot
    // Diagonal gradient (bottom-left -> top-right) for the whole content area
    val bgGradient = Brush.linearGradient(
        colors = listOf(Color(0xFF0D1730), Color(0xFF081024)),
        start = Offset(0f, Float.POSITIVE_INFINITY),
        end = Offset(Float.POSITIVE_INFINITY, 0f)
    )
    // Glassmorphic card tone (translucent dark)
    val glass = Color(0xFF0A1224).copy(alpha = 0.6f)
    val glassBorder = Color.White.copy(alpha = 0.08f)
    val textPrimary = Color.White
    val textSecondary = Color(0xFF9FB1C7)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgGradient)
            .drawBehind {
                // Soft blue glow from bottom-left
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF2B5BDE).copy(alpha = 0.35f), Color.Transparent)
                    ),
                    radius = size.minDimension * 0.9f,
                    center = Offset(size.width * 0.25f, size.height * 0.95f)
                )
                // Subtle purple glow from top-right
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF6C3AE3).copy(alpha = 0.25f), Color.Transparent)
                    ),
                    radius = size.minDimension * 0.7f,
                    center = Offset(size.width * 0.9f, size.height * 0.2f)
                )
            }
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .wrapContentWidth(Alignment.CenterHorizontally)
            .widthIn(max = 420.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Hello, Posturely",
                color = textPrimary,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color(0xFF2C3550), CircleShape)
            )
        }

        Spacer(Modifier.height(12.dp))

        // Monitoring On indicator
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(Color(0xFF37D483), CircleShape)
            )
            Spacer(Modifier.width(8.dp))
            Text("Monitoring On", color = Color(0xFF37D483), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(16.dp))

        // Posture Score Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = glass)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, glassBorder, RoundedCornerShape(28.dp))
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Rings
                Box(
                    modifier = Modifier
                        .size(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        val strokeOuter = 20.dp.toPx()
                        val strokeMid = 16.dp.toPx()
                        val strokeInner = 12.dp.toPx()

                        // Outer red ring
                        drawArc(
                            color = Color(0xFFFF2E57),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = strokeOuter)
                        )
                        // Middle green ring
                        val insetMid = (strokeOuter - strokeMid) / 2 + strokeOuter / 2
                        val sizeMid = Size(size.width - insetMid * 2, size.height - insetMid * 2)
                        drawArc(
                            color = Color(0xFF32D671),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = Offset(insetMid, insetMid),
                            size = sizeMid,
                            style = Stroke(width = strokeMid)
                        )
                        // Inner blue ring
                        val insetInner = insetMid + (strokeMid - strokeInner)
                        val sizeInner = Size(size.width - insetInner * 2, size.height - insetInner * 2)
                        drawArc(
                            color = Color(0xFF2FB4FF),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = Offset(insetInner, insetInner),
                            size = sizeInner,
                            style = Stroke(width = strokeInner)
                        )
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Posture\nScore", color = textPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("82", color = textPrimary, fontSize = 56.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Lower two cards (equal size)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // Session card
            Card(
                modifier = Modifier.weight(1f).height(190.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = glass)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(1.dp, glassBorder, RoundedCornerShape(28.dp))
                        .padding(20.dp)
                ) {
                    Text("Session", color = Color(0xFF5EE2A0), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("34", color = textPrimary, fontSize = 56.sp, fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.width(6.dp))
                        Text("m", color = textPrimary, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }

            // Live Angle card
            Card(
                modifier = Modifier.weight(1f).height(190.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = glass)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(1.dp, glassBorder, RoundedCornerShape(28.dp))
                        .padding(20.dp)
                ) {
                    Text("Live Angle", color = Color(0xFF7B74FF), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    // Silhouette placeholder
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(Modifier.size(60.dp)) {
                            // head
                            drawCircle(Color(0xFF2FB4FF), radius = size.minDimension * 0.18f, center = Offset(size.width * 0.55f, size.height * 0.25f))
                            // body
                            drawRoundRect(
                                color = Color(0xFF2FB4FF),
                                topLeft = Offset(size.width * 0.35f, size.height * 0.35f),
                                size = Size(size.width * 0.3f, size.height * 0.45f),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f)
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("12°", color = textPrimary, fontSize = 40.sp, fontWeight = FontWeight.ExtraBold)
                    Text("forward", color = textSecondary, fontSize = 14.sp)
                }
            }
        }
    }
}

