package com.mobil80.posturely

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import posturelynew.composeapp.generated.resources.Res
import posturelynew.composeapp.generated.resources.giffygood

// Color definitions at file level
private val pageBg = Color(0xFFFFF9EA)
private val textPrimary = Color(0xFF0F1931)
private val subText = Color(0xFF6B7280)
private val accentGreen = Color(0xFF28A745)
private val accentOrange = Color(0xFFFFA000)

// TEMPORARILY DISABLED - Replaced with Stats tab
@OptIn(ExperimentalResourceApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ScansScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(pageBg)
            .padding(horizontal = 24.dp, vertical = 24.dp)
    ) {
        // Title Section - matching home tab sizing
        Text(
            text = "Scans",
            color = textPrimary, 
            fontSize = 32.sp, 
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Text(
            text = "Perform a full body scan to analyze your posture.",
            style = MaterialTheme.typography.bodyLarge,
            color = subText,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        // Hero Section: Description + Giraffe with Magnifying Glass
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Description text is already above
            }
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(Res.drawable.giffygood),
                    contentDescription = "Giraffe with magnifying glass",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Start Full Body Scan Button - matching image styling
        Button(
            onClick = { /* TODO: Start scan */ },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = accentGreen)
        ) {
            Text(
                "Start Full Body Scan",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(40.dp))
        
        // Last Scan Section - matching image layout
        Text(
            text = "Last Scan",
            color = textPrimary, 
            fontSize = 22.sp, 
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(bottom = 20.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Score Circle
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(accentOrange, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "86",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            
            // Posture Issues
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    "• Forward head posture",
                    color = accentGreen,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    "• Rounded shoulders",
                    color = accentGreen,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // View Report Button and Graph
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // View Report Button
            OutlinedButton(
                onClick = { /* TODO: View report */ },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color(0xFFF3F4F6)
                )
            ) {
                Text(
                    "View Report",
                    color = textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            
            // Simple Graph (placeholder)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(60.dp)
                    .background(Color(0xFFE8F5E8), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "📈 Progress Graph",
                    color = accentGreen,
                    fontSize = 14.sp
                )
            }
        }
    }
} 