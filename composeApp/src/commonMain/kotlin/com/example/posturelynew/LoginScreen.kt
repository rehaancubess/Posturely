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
fun LoginScreen(
    onNavigateToOTP: (String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var isEmailValid by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    
    val pageBg = Color(0xFFFFF9EA)
    val textPrimary = Color(0xFF0F1931)
    val subText = Color(0xFF6B7280)
    val accentGreen = Color(0xFF2ECC71)
    val accentRed = Color(0xFFEF4444)
    
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
    
    // Handle submit
    fun onSubmit() {
        if (isEmailValid) {
            isLoading = true
            // Real Supabase authentication
        } else {
            showError = true
        }
    }
    
    // Handle the loading and navigation
    LaunchedEffect(isLoading) {
        if (isLoading) {
            try {
                try {
                    Supa.sendEmailOtp(email)
                    isLoading = false
                    onNavigateToOTP(email)
                } catch (e: Exception) {
                    isLoading = false
                    errorMessage = e.message ?: "Failed to send OTP"
                    showError = true
                }
            } catch (e: Exception) {
                isLoading = false
                errorMessage = e.message ?: "An unexpected error occurred"
                showError = true
            }
        }
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
        
        // Logo/App Title
        Text(
            text = "Posturely",
            color = textPrimary,
            fontSize = 36.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Text(
            text = "Monitor and correct your posture",
            color = subText,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 48.dp)
        )
        
        // Login Card
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
                
                // Email Input
                OutlinedTextField(
                    value = email,
                    onValueChange = { onEmailChange(it) },
                    label = { Text("Email Address") },
                    placeholder = { Text("Enter your email") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (isEmailValid) accentGreen else accentRed,
                        unfocusedBorderColor = if (showError) accentRed else subText,
                        focusedLabelColor = if (isEmailValid) accentGreen else accentRed
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
                        color = accentGreen,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .align(Alignment.Start)
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Submit Button
                Button(
                    onClick = { onSubmit() },
                    enabled = isEmailValid && !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isEmailValid) accentGreen else subText
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
                            text = "Continue",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Info text
                Text(
                    text = "We'll send you a verification code",
                    color = subText,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Footer
        Text(
            text = "By continuing, you agree to our Terms of Service and Privacy Policy",
            color = subText,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 32.dp)
        )
    }
}
