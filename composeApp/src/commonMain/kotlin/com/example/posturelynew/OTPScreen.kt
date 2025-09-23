package com.mobil80.posturely

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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import kotlinx.coroutines.delay
import com.mobil80.posturely.supabase.Supa

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
    
    // Focus requester for autofocus
    val focusRequester = remember { FocusRequester() }
    
    // Colors matching your app's yellow and brown theme
    val pageBg = Color(0xFFFED867) // Your app's yellow background
    val textPrimary = Color(0xFF0F1931) // Dark text on yellow background
    val subText = Color(0xFF6B7280)
    val accentBrown = Color(0xFF7A4B00) // Your brown theme
    val accentRed = Color(0xFFEF4444)
    val lightBrown = Color(0xFFD2B48C)
    
    // Countdown timer
    LaunchedEffect(Unit) {
        while (countdown > 0) {
            delay(1000)
            countdown--
        }
    }
    
    // Autofocus the text field when screen loads to open keyboard
    LaunchedEffect(Unit) {
        delay(300) // Delay to ensure the screen is fully loaded and rendered
        focusRequester.requestFocus()
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
        Spacer(modifier = Modifier.height(60.dp))
        
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
        
        // Title - matching the email screen style
        Text(
            text = "VERIFY OTP",
            color = textPrimary,
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Description text - matching email screen style
        Text(
            text = "Enter the 6-digit code sent to your email address to verify your account.",
            color = textPrimary,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
            modifier = Modifier.padding(bottom = 48.dp)
        )
        
        // OTP Input Field - with your app's color theme
        OutlinedTextField(
            value = otp,
            onValueChange = { 
                if (it.length <= 6) {
                    otp = it
                    showError = false
                }
            },
            placeholder = { Text("Enter 6-digit code", color = subText) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accentBrown,
                unfocusedBorderColor = accentBrown,
                focusedTextColor = textPrimary,
                unfocusedTextColor = textPrimary,
                cursorColor = accentBrown,
                focusedPlaceholderColor = subText,
                unfocusedPlaceholderColor = subText,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp)
        )
        
        // Error message
        if (showError && otp.length != 6) {
            Text(
                text = "Please enter a valid 6-digit OTP",
                color = accentRed,
                fontSize = 14.sp,
                modifier = Modifier
                    .padding(top = 12.dp)
                    .align(Alignment.Start)
            )
        }
        
        // Error message for verification failure
        if (showError && otp.length == 6 && errorMessage.isNotEmpty()) {
            Text(
                text = errorMessage,
                color = accentRed,
                fontSize = 14.sp,
                modifier = Modifier
                    .padding(top = 12.dp)
                    .align(Alignment.Start)
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Verify Button - using your brown theme
        Button(
            onClick = { verifyOTP() },
            enabled = otp.length == 6 && !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = accentBrown,
                disabledContainerColor = lightBrown
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
                    text = "VERIFY",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
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
                    colors = ButtonDefaults.textButtonColors(contentColor = accentBrown)
                ) {
                    Text(
                        text = "Resend OTP",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Footer text - matching email screen style
        Text(
            text = "Posture insights are wellness guidance, not medical advice.",
            color = subText,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 32.dp)
        )
    }
}
