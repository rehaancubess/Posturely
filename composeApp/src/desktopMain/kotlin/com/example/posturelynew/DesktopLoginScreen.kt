package com.example.posturelynew

import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
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
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.ExperimentalResourceApi
import com.example.posturelynew.supabase.DesktopSupa
import posturelynew.composeapp.generated.resources.Res
import posturelynew.composeapp.generated.resources.giraffeenew

@OptIn(ExperimentalMaterial3Api::class, ExperimentalResourceApi::class)
@Composable
fun DesktopLoginScreen(
    onNavigateToOTP: (String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var isEmailValid by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var showOTPInput by remember { mutableStateOf(false) }
    var otp by remember { mutableStateOf("") }
    var otpLoading by remember { mutableStateOf(false) }
    
    // Desktop main page color theme
    val pageBg = Color(0xFFFED867)
    val textPrimary = Color(0xFF0F1931)
    val subText = Color(0xFF6B7280)
    val accentPrimary = Color(0xFFD69C2F) // matches main screen accent brown
    val accentRed = Color(0xFFEF4444)
    val cardBg = Color(0xFFFFF0C0)
    
    // Email validation function
    fun validateEmail(email: String): Boolean {
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$"
        return email.matches(emailRegex.toRegex())
    }
    
    // Handle email changes
    fun onEmailChange(newEmail: String) {
        email = newEmail
        isEmailValid = validateEmail(newEmail)
        showError = false
        errorMessage = ""
    }
    
    // Handle email submit
    fun onSubmitEmail() {
        if (isEmailValid) {
            isLoading = true
        } else {
            showError = true
        }
    }
    
    // Handle OTP verification
    fun verifyOTP() {
        if (otp.length == 6) {
            otpLoading = true
        } else {
            showError = true
        }
    }
    
    // Handle email OTP sending
    LaunchedEffect(isLoading) {
        if (isLoading) {
            try {
                DesktopSupa.sendEmailOtp(email)
                isLoading = false
                showOTPInput = true
            } catch (e: Exception) {
                isLoading = false
                errorMessage = e.message ?: "Failed to send OTP"
                showError = true
            }
        }
    }
    
    // Handle OTP verification
    LaunchedEffect(otpLoading) {
        if (otpLoading) {
            try {
                DesktopSupa.verifyEmailOtp(email, otp)
                otpLoading = false
                onNavigateToOTP(email)
            } catch (e: Exception) {
                otpLoading = false
                errorMessage = e.message ?: "Invalid OTP"
                showError = true
            }
        }
    }
    
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(pageBg)
    ) {
        val isCompact = maxWidth < 800.dp
        val horizontalPadding = if (isCompact) 24.dp else 60.dp
        val logoSize = if (isCompact) 90.dp else 120.dp
        val titleSize = if (isCompact) 36.sp else 48.sp
        val subTitleSize = if (isCompact) 16.sp else 18.sp
        val topSpacer = if (isCompact) 24.dp else 60.dp
        val cardPadding = if (isCompact) 20.dp else 32.dp
        val cardWidthFraction = if (isCompact) 1f else 0.6f

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horizontalPadding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(topSpacer))

            // Logo and Title
            Image(
                painter = painterResource(Res.drawable.giraffeenew),
                contentDescription = "Posturely Logo",
                modifier = Modifier
                    .size(logoSize)
                    .padding(bottom = 24.dp)
            )
            
            Text(
                text = "Posturely",
                color = textPrimary,
                fontSize = titleSize,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = "Monitor and correct your posture",
                color = subText,
                fontSize = subTitleSize,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = if (isCompact) 32.dp else 60.dp)
            )
            
            // Login Card
            Card(
                modifier = Modifier
                    .fillMaxWidth(cardWidthFraction)
                    .widthIn(max = 720.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(cardPadding),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                Text(
                    text = "Welcome Back",
                    color = textPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Text(
                    text = "Sign in to continue tracking your posture",
                    color = subText,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 32.dp)
                )
                
                if (!showOTPInput) {
                    // Email Input
                    OutlinedTextField(
                        value = email,
                        onValueChange = { newValue ->
                            println("Email input changed: '$newValue'")
                            onEmailChange(newValue)
                        },
                        label = { Text("Email Address", color = textPrimary) },
                        placeholder = { Text("Enter your email", color = subText) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (showError && !isEmailValid) accentRed else accentPrimary,
                            unfocusedBorderColor = if (showError && !isEmailValid) accentRed else subText,
                            focusedLabelColor = if (showError && !isEmailValid) accentRed else accentPrimary,
                            unfocusedLabelColor = textPrimary,
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary,
                            cursorColor = accentPrimary
                        ),
                        isError = showError && !isEmailValid
                    )
                    
                    // Error message
                    if (showError) {
                        Text(
                            text = if (!isEmailValid) "Please enter a valid email address" else errorMessage,
                            color = accentRed,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .align(Alignment.Start)
                        )
                    }
                    
                    // Success message
                    if (isEmailValid && email.isNotEmpty()) {
                        Text(
                            text = "✓ Valid email address",
                            color = accentPrimary,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .align(Alignment.Start)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // Submit Button
                    Button(
                        onClick = { onSubmitEmail() },
                        enabled = isEmailValid && !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isEmailValid) accentPrimary else subText
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
                                text = "Send OTP",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                } else {
                    // OTP Input
                    Text(
                        text = "Enter the 6-digit code sent to",
                        color = subText,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Text(
                        text = email,
                        color = textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                    
                    OutlinedTextField(
                        value = otp,
                        onValueChange = { newValue ->
                            println("OTP input changed: '$newValue'")
                            if (newValue.length <= 6) {
                                otp = newValue
                                showError = false
                            }
                        },
                        label = { Text("Enter OTP", color = textPrimary) },
                        placeholder = { Text("000000", color = subText) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (showError && otp.length != 6) accentRed else accentPrimary,
                            unfocusedBorderColor = if (showError && otp.length != 6) accentRed else subText,
                            focusedLabelColor = if (showError && otp.length != 6) accentRed else accentPrimary,
                            unfocusedLabelColor = textPrimary,
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary,
                            cursorColor = accentPrimary
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
                            color = accentPrimary,
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
                        enabled = otp.length == 6 && !otpLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (otp.length == 6) accentPrimary else subText
                        )
                    ) {
                        if (otpLoading) {
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
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Back to email
                    TextButton(
                        onClick = { 
                            showOTPInput = false
                            otp = ""
                            showError = false
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = subText)
                    ) {
                        Text("← Back to email", fontSize = 14.sp)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Info text
                Text(
                    text = if (!showOTPInput) "We'll send you a verification code" else "The OTP will expire in 5 minutes",
                    color = subText,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
            }
        }
    }
}