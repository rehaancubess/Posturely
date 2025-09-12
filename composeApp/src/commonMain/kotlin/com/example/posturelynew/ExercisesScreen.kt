package com.example.posturelynew

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
private val lightOrange = Color(0xFFFFE4B3)
private val lightGreen = Color(0xFFE8F5E8)
private val lightPurple = Color(0xFFE8E0FF)
private val lightBlue = Color(0xFFE0F2FF)

// TEMPORARILY DISABLED - Replaced with Stats tab
@OptIn(ExperimentalResourceApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ExercisesScreen() {
    var selectedTab by remember { mutableStateOf(0) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(pageBg)
            .padding(horizontal = 24.dp, vertical = 24.dp)
    ) {
        // Header Section - matching home tab sizing
        Text(
            text = "Exercises",
            color = textPrimary, 
            fontSize = 32.sp, 
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(bottom = 20.dp)
        )
        
        // Tab Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TabButton(
                text = "Today",
                isSelected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                modifier = Modifier.weight(1f)
            )
            TabButton(
                text = "Library",
                isSelected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // Streak Section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(lightOrange, RoundedCornerShape(20.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🔥", fontSize = 20.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "3-day streak",
                    color = textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Today's Routine Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left side: Progress and content
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    // Progress Circle
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(accentGreen, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "75%",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        "Today's Routine",
                        color = textPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    // Exercise list
                    Text(
                        "• Chin Tucks",
                        color = textPrimary,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        "• Shoulder Blade Squeezes",
                        color = textPrimary,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        "• Chin Nods",
                        color = textPrimary,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Text(
                        "Estimated 8 min",
                        color = subText,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    // Start Session Button
                    Button(
                        onClick = { /* TODO: Start session */ },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accentOrange)
                    ) {
                        Text(
                            "Start Session",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                // Right side: Giraffe illustration
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(Res.drawable.giffygood),
                        contentDescription = "Giraffe illustration",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Categories Section
        Text(
            text = "Categories",
            color = textPrimary, 
            fontSize = 22.sp, 
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(bottom = 20.dp)
        )
        
        // Categories Grid (2x2)
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top row
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CategoryCard(
                    title = "Neck",
                    subtitle = "Stretches",
                    details = "5 min • No equipment",
                    backgroundColor = lightGreen,
                    modifier = Modifier.weight(1f)
                )
                CategoryCard(
                    title = "Back",
                    subtitle = "Thoracic Extensions",
                    details = "5 min • No equipment",
                    backgroundColor = lightBlue,
                    modifier = Modifier.weight(1f)
                )
            }
            
            // Bottom row
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CategoryCard(
                    title = "Shoulders",
                    subtitle = "Retractions",
                    details = "5 min • No equipment",
                    backgroundColor = lightOrange,
                    modifier = Modifier.weight(1f)
                )
                CategoryCard(
                    title = "Shoulder",
                    subtitle = "Retractions",
                    details = "5 min • No equipment",
                    backgroundColor = lightPurple,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun TabButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                if (isSelected) lightOrange else Color(0xFFF3F4F6),
                RoundedCornerShape(20.dp)
            )
            .clickable(
                interactionSource = null,
                indication = null,
                enabled = true,
                onClick = onClick
            )
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isSelected) textPrimary else subText,
            fontSize = 16.sp,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
        )
    }
}

@Composable
private fun CategoryCard(
    title: String,
    subtitle: String,
    details: String,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                color = textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            
            Text(
                text = subtitle,
                color = textPrimary,
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = details,
                color = subText,
                fontSize = 12.sp
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Play button icon (placeholder)
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("▶", color = accentGreen, fontSize = 16.sp)
            }
        }
    }
} 