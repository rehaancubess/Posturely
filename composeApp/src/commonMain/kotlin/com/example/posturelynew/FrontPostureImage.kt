package com.mobil80.posturely

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

@Composable
fun FrontPostureImage(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        
        // Calculate proportions
        val headRadius = width * 0.08f
        val neckWidth = width * 0.06f
        val shoulderWidth = width * 0.4f
        val torsoHeight = height * 0.3f
        val armLength = height * 0.25f
        val legLength = height * 0.35f
        
        // Colors
        val primaryColor = Color(0xFF2196F3) // Blue
        val secondaryColor = Color(0xFF1976D2) // Darker blue
        val strokeColor = Color(0xFF0D47A1) // Dark blue for outlines
        
        // Center position
        val centerX = width / 2
        val startY = height * 0.1f
        
        // Draw head
        drawCircle(
            color = primaryColor,
            radius = headRadius,
            center = Offset(centerX, startY + headRadius)
        )
        
        // Draw neck
        val neckY = startY + headRadius * 2
        drawRect(
            color = primaryColor,
            topLeft = Offset(centerX - neckWidth / 2, neckY),
            size = androidx.compose.ui.geometry.Size(neckWidth, headRadius)
        )
        
        // Draw shoulders
        val shoulderY = neckY + headRadius
        drawLine(
            color = primaryColor,
            start = Offset(centerX - shoulderWidth / 2, shoulderY),
            end = Offset(centerX + shoulderWidth / 2, shoulderY),
            strokeWidth = width * 0.04f
        )
        
        // Draw torso
        val torsoY = shoulderY
        val torsoRect = androidx.compose.ui.geometry.Rect(
            left = centerX - shoulderWidth / 2,
            top = torsoY,
            right = centerX + shoulderWidth / 2,
            bottom = torsoY + torsoHeight
        )
        drawRect(
            color = primaryColor,
            topLeft = torsoRect.topLeft,
            size = torsoRect.size
        )
        
        // Draw arms
        val armY = shoulderY + width * 0.02f
        val leftArmX = centerX - shoulderWidth / 2
        val rightArmX = centerX + shoulderWidth / 2
        
        // Left arm
        drawLine(
            color = primaryColor,
            start = Offset(leftArmX, armY),
            end = Offset(leftArmX - armLength * 0.3f, armY + armLength * 0.4f),
            strokeWidth = width * 0.03f
        )
        
        // Right arm
        drawLine(
            color = primaryColor,
            start = Offset(rightArmX, armY),
            end = Offset(rightArmX + armLength * 0.3f, armY + armLength * 0.4f),
            strokeWidth = width * 0.03f
        )
        
        // Draw legs
        val legY = torsoY + torsoHeight
        val legWidth = width * 0.08f
        
        // Left leg
        drawRect(
            color = primaryColor,
            topLeft = Offset(centerX - legWidth - width * 0.02f, legY),
            size = androidx.compose.ui.geometry.Size(legWidth, legLength)
        )
        
        // Right leg
        drawRect(
            color = primaryColor,
            topLeft = Offset(centerX + width * 0.02f, legY),
            size = androidx.compose.ui.geometry.Size(legWidth, legLength)
        )
        
        // Draw posture guide lines
        val guideColor = Color(0xFF4CAF50) // Green for guides
        
        // Vertical center line
        drawLine(
            color = guideColor,
            start = Offset(centerX, startY),
            end = Offset(centerX, legY + legLength),
            strokeWidth = 2f
        )
        
        // Horizontal shoulder line
        drawLine(
            color = guideColor,
            start = Offset(centerX - shoulderWidth / 2 - 20f, shoulderY),
            end = Offset(centerX + shoulderWidth / 2 + 20f, shoulderY),
            strokeWidth = 2f
        )
        
        // Add some posture indicators
        drawPostureIndicators(centerX, startY, shoulderY, guideColor)
    }
}

private fun DrawScope.drawPostureIndicators(centerX: Float, startY: Float, shoulderY: Float, color: Color) {
    // Head alignment indicator
    drawCircle(
        color = color,
        radius = 8f,
        center = Offset(centerX, startY + 40f)
    )
    
    // Shoulder alignment indicators
    drawCircle(
        color = color,
        radius = 6f,
        center = Offset(centerX - 60f, shoulderY)
    )
    drawCircle(
        color = color,
        radius = 6f,
        center = Offset(centerX + 60f, shoulderY)
    )
} 