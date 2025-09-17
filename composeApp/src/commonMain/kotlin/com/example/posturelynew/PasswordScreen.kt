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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.posturelynew.supabase.Supa

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordScreen(
    email: String,
    onNavigateToHome: () -> Unit,
    onBackToLogin: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val pageBg = Color(0xFFFED867)
    val textPrimary = Color(0xFF0F1931)
    val subText = Color(0xFF6B7280)
    val accentBrown = Color(0xFF7A4B00)
    val accentRed = Color(0xFFEF4444)
    val lightBrown = Color(0xFFD2B48C)

    fun submit() {
        if (password.isNotEmpty()) {
            isLoading = true
        } else {
            showError = true
            errorMessage = "Please enter password"
        }
    }

    LaunchedEffect(isLoading) {
        if (isLoading) {
            try {
                Supa.signInWithEmailPassword(email, password)
                isLoading = false
                onNavigateToHome()
            } catch (e: Exception) {
                isLoading = false
                errorMessage = e.message ?: "Invalid credentials"
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
        Spacer(modifier = Modifier.height(60.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
            TextButton(onClick = onBackToLogin, colors = ButtonDefaults.textButtonColors(contentColor = textPrimary)) {
                Text("← Back", fontSize = 16.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "ENTER PASSWORD",
            color = textPrimary,
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = "Sign in as $email",
            color = textPrimary,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 48.dp)
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it; showError = false },
            placeholder = { Text("Enter password", color = subText) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
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

        if (showError && errorMessage.isNotEmpty()) {
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

        Button(
            onClick = { submit() },
            enabled = password.isNotEmpty() && !isLoading,
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
                    text = "SIGN IN",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "Posture insights are wellness guidance, not medical advice.",
            color = subText,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 32.dp)
        )
    }
}


