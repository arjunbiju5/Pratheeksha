package com.example.donor.ui.login

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.donor.ui.registration.SectionLabel
import com.example.donor.ui.registration.redOutlinedColors
import com.example.donor.ui.theme.RedPrimary
import com.example.donor.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.*

private val dobFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoggedIn: () -> Unit,
    onNavigateToRegister: () -> Unit,
    viewModel: LoginViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var phone        by remember { mutableStateOf("") }
    var dateOfBirth  by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }

    var phoneError  by remember { mutableStateOf(false) }
    var dobError    by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) onLoggedIn()
    }

    // ── Date Picker ────────────────────────────────────────────────────────────
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    utcTimeMillis <= System.currentTimeMillis()
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        dateOfBirth = dobFormat.format(Date(millis))
                        dobError = false
                    }
                    showDatePicker = false
                }) { Text("OK", color = RedPrimary) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        ) { DatePicker(state = datePickerState) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        // ── Header ─────────────────────────────────────────────────────────────
        Text("🩸", fontSize = 40.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Welcome Back",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = RedPrimary
        )
        Text(
            "Sign in with your phone number and date of birth",
            fontSize = 14.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(40.dp))

        // ── Phone Number ───────────────────────────────────────────────────────
        SectionLabel("Phone Number *", isError = phoneError)
        OutlinedTextField(
            value = phone,
            onValueChange = {
                if (it.length <= 10) {
                    phone = it.filter { c -> c.isDigit() }
                    phoneError = false
                    viewModel.clearError()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("10-digit mobile number") },
            leadingIcon = { Icon(Icons.Default.Phone, null, tint = RedPrimary) },
            isError = phoneError,
            supportingText = { if (phoneError) Text("Enter a valid 10-digit phone number") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(12.dp),
            colors = redOutlinedColors(isError = phoneError)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // ── Date of Birth ──────────────────────────────────────────────────────
        SectionLabel("Date of Birth *", isError = dobError)
        OutlinedTextField(
            value = dateOfBirth.ifBlank { "Tap to select date" },
            onValueChange = {},
            readOnly = true,
            isError = dobError,
            supportingText = { if (dobError) Text("Date of birth is required") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.DateRange, null, tint = RedPrimary) },
            trailingIcon = {
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(Icons.Default.Edit, "Pick date", tint = RedPrimary)
                }
            },
            shape = RoundedCornerShape(12.dp),
            colors = redOutlinedColors(isError = dobError)
        )

        Spacer(modifier = Modifier.height(28.dp))

        // ── Error Banner ───────────────────────────────────────────────────────
        uiState.errorMessage?.let { error ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, null, tint = RedPrimary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(error, color = RedPrimary, fontSize = 13.sp)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // ── Login Button ───────────────────────────────────────────────────────
        Button(
            onClick = {
                phoneError = phone.isBlank() || phone.length < 10
                dobError   = dateOfBirth.isBlank()
                if (phoneError || dobError) return@Button
                viewModel.login(phone, dateOfBirth)
            },
            enabled = !uiState.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Default.Login, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("Sign In", fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Register CTA ───────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Not registered yet?", color = TextSecondary, fontSize = 14.sp)
            Spacer(Modifier.width(4.dp))
            TextButton(onClick = onNavigateToRegister, contentPadding = PaddingValues(0.dp)) {
                Text(
                    "Register as donor",
                    color = RedPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}