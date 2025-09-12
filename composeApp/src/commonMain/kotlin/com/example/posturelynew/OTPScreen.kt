package com.example.posturelynew

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.example.posturelynew.supabase.Supa

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OTPScreen(
    email: String,
    onNavigateToHome: () -> Unit,
    onBackToLogin: () -> Unit
) {
    var otp by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var countdown by remember { mutableStateOf(30) }
    
    val pageBg = Color(0xFFFFF9EA)
    val textPrimary = Color(0xFF0F1931)
    val subText = Color(0xFF6B7280)
    val accentGreen = Color(0xFF2ECC71)
    val accentRed = Color(0xFFEF4444)
    
    // Countdown timer
    LaunchedEffect(Unit) {
        while (countdown > 0) {
            delay(1000)
            countdown--
        }
    }
    
    // Handle OTP verification
    fun verifyOTP() {
        if (otp.length == 6) {
            isLoading = true
            // Real Supabase OTP verification
        } else {
            showError = true
        }
    }
    
    // Handle the loading and navigation
    LaunchedEffect(isLoading) {
        if (isLoading) {
            try {
                // Note: We need to pass the email from the previous screen
                // For now, we'll use a placeholder - in a real app, you'd pass this as a parameter
                Supa.verifyEmailOtp(email, otp)
                isLoading = false
                onNavigateToHome()
            } catch (e: Exception) {
                isLoading = false
                errorMessage = e.message ?: "An unexpected error occurred"
                showError = true
            }
        }
    }
    
    // Resend OTP
    fun resendOTP() {
        countdown = 30
        showError = false
        // In real app, this would trigger a new OTP
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(pageBg)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Add safe space for status bar
        Spacer(modifier = Modifier.height(48.dp))
        
        // Back button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            TextButton(
                onClick = onBackToLogin,
                colors = ButtonDefaults.textButtonColors(contentColor = textPrimary)
            ) {
                Text("← Back", fontSize = 16.sp)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Title
        Text(
            text = "Verify OTP",
            color = textPrimary,
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Text(
            text = "Enter the 6-digit code sent to your email",
            color = subText,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 48.dp)
        )
        
        // OTP Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // OTP Input
                OutlinedTextField(
                    value = otp,
                    onValueChange = { 
                        if (it.length <= 6) {
                            otp = it
                            showError = false
                        }
                    },
                    label = { Text("Enter OTP") },
                    placeholder = { Text("000000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (otp.length == 6) accentGreen else accentRed,
                        unfocusedBorderColor = if (showError) accentRed else subText,
                        focusedLabelColor = if (otp.length == 6) accentGreen else accentRed
                    ),
                    isError = showError && otp.length != 6
                )
                
                // Error message
                if (showError) {
                    Text(
                        text = if (otp.length != 6) "Please enter a valid 6-digit OTP" else errorMessage,
                        color = accentRed,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .align(Alignment.Start)
                    )
                }
                
                // Success message
                if (otp.length == 6) {
                    Text(
                        text = "✓ Valid OTP format",
                        color = accentGreen,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .align(Alignment.Start)
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Verify Button
                Button(
                    onClick = { verifyOTP() },
                    enabled = otp.length == 6 && !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (otp.length == 6) accentGreen else subText
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Verify OTP",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Resend OTP section
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Didn't receive the code?",
                        color = subText,
                        fontSize = 14.sp
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (countdown > 0) {
                        Text(
                            text = "Resend in ${countdown}s",
                            color = subText,
                            fontSize = 14.sp
                        )
                    } else {
                        TextButton(
                            onClick = { resendOTP() },
                            colors = ButtonDefaults.textButtonColors(contentColor = accentGreen)
                        ) {
                            Text(
                                text = "Resend OTP",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Footer
        Text(
            text = "The OTP will expire in 5 minutes",
            color = subText,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 32.dp)
        )
    }
}
