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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
    
    // Focus requester for autofocus
    val focusRequester = remember { FocusRequester() }
    
    // Colors matching your app's yellow and brown theme
    val pageBg = Color(0xFFFED867) // Your app's yellow background
    val textPrimary = Color(0xFF0F1931) // Dark text on yellow background
    val subText = Color(0xFF6B7280)
    val accentBrown = Color(0xFF7A4B00) // Your brown theme
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
        } else {
            showError = true
        }
    }
    
    // Handle the loading and navigation
    LaunchedEffect(isLoading) {
        if (isLoading) {
            try {
                Supa.sendEmailOtp(email)
                isLoading = false
                onNavigateToOTP(email)
            } catch (e: Exception) {
                isLoading = false
                errorMessage = e.message ?: "Failed to send OTP"
                showError = true
            }
        }
    }
    
    // Autofocus the text field when screen loads to open keyboard
    LaunchedEffect(Unit) {
        delay(300) // Delay to ensure the screen is fully loaded and rendered
        focusRequester.requestFocus()
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
        
        // Title - matching the image
        Text(
            text = "REGISTER",
            color = textPrimary,
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Description text
        Text(
            text = "Please enter your valid email address. We will send you a 6-digit code to verify your account.",
            color = textPrimary,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
            modifier = Modifier.padding(bottom = 48.dp)
        )
        
        // Email Input Field - with your app's color theme
        OutlinedTextField(
            value = email,
            onValueChange = { onEmailChange(it) },
            placeholder = { Text("Enter your email", color = subText) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
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
        if (showError && !isEmailValid) {
            Text(
                text = "Please enter a valid email address",
                color = accentRed,
                fontSize = 14.sp,
                modifier = Modifier
                    .padding(top = 12.dp)
                    .align(Alignment.Start)
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Submit Button - using your brown theme
        Button(
            onClick = { onSubmit() },
            enabled = isEmailValid && !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = accentBrown,
                disabledContainerColor = subText
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
                    text = "SUBMIT",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Footer text
        Text(
            text = "Posture insights are wellness guidance, not medical advice.",
            color = subText,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 32.dp)
        )
    }
}
