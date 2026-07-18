package com.example.donor.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.donor.ui.theme.RedPrimary
import com.example.donor.ui.theme.TextSecondary

private val GreenSuccess = Color(0xFF2E7D32)
private val GreenSuccessBg = Color(0xFFE8F5E9)

@Composable
fun AdminLoginScreen(
    onLoginSuccess: () -> Unit,
    onBackToHome: () -> Unit,
    viewModel: AdminViewModel = viewModel()
) {
    val uiState by viewModel.adminUiState.collectAsState()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    // Navigate on success
    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) onLoginSuccess()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // Back button
        IconButton(
            onClick = onBackToHome,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(bottom = 16.dp)
        ) {
            Icon(
                Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = RedPrimary
            )
        }

        Text(text = "🔐", fontSize = 48.sp)

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text       = "Admin Login",
            fontSize   = 26.sp,
            fontWeight = FontWeight.Bold,
            color      = RedPrimary
        )

        Text(
            text     = "Manage donor database",
            fontSize = 14.sp,
            color    = TextSecondary
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Email field
        OutlinedTextField(
            value         = email,
            onValueChange = { email = it },
            modifier      = Modifier.fillMaxWidth(),
            label         = { Text("Email") },
            placeholder   = { Text("") },
            leadingIcon   = {
                Icon(Icons.Default.Email, contentDescription = null, tint = RedPrimary)
            },
            singleLine    = true,
            shape         = RoundedCornerShape(12.dp),
            colors        = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = RedPrimary,
                focusedLabelColor    = RedPrimary,
                cursorColor          = RedPrimary,
                unfocusedBorderColor = TextSecondary.copy(alpha = 0.4f)
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Password field
        OutlinedTextField(
            value         = password,
            onValueChange = { password = it },
            modifier      = Modifier.fillMaxWidth(),
            label         = { Text("Password") },
            placeholder   = { Text("Enter password") },
            leadingIcon   = {
                Icon(Icons.Default.Lock, contentDescription = null, tint = RedPrimary)
            },
            trailingIcon  = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(
                        if (showPassword) Icons.Default.Visibility
                        else Icons.Default.VisibilityOff,
                        contentDescription = null,
                        tint = RedPrimary
                    )
                }
            },
            visualTransformation = if (showPassword) VisualTransformation.None
            else PasswordVisualTransformation(),
            singleLine    = true,
            shape         = RoundedCornerShape(12.dp),
            colors        = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = RedPrimary,
                focusedLabelColor    = RedPrimary,
                cursorColor          = RedPrimary,
                unfocusedBorderColor = TextSecondary.copy(alpha = 0.4f)
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Forgot password link — sends a Firebase reset email to whatever
        // address is currently typed in the Email field above.
        TextButton(
            onClick = {
                if (email.isBlank()) {
                    viewModel.setError("Enter your email above first, then tap this again")
                } else {
                    viewModel.sendPasswordReset(email)
                }
            },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text(
                text = "Forgot password?",
                fontSize = 13.sp,
                color = RedPrimary,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Error message
        uiState.errorMessage?.let {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                shape  = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier          = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint     = RedPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = it, color = RedPrimary, fontSize = 13.sp)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Info / success message (e.g. "Password reset link sent")
        uiState.infoMessage?.let {
            Card(
                colors = CardDefaults.cardColors(containerColor = GreenSuccessBg),
                shape  = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier          = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint     = GreenSuccess,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = it, color = GreenSuccess, fontSize = 13.sp)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Login button
        Button(
            onClick  = {
                if (email.isBlank() || password.isBlank()) {
                    viewModel.setError("Please fill all fields")
                } else {
                    viewModel.adminLogin(email, password)
                }
            },
            enabled  = !uiState.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape    = RoundedCornerShape(12.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = RedPrimary)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    color    = Color.White,
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(Icons.Default.Login, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text     = "Admin Login",
                    fontSize = 16.sp,
                    color    = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}