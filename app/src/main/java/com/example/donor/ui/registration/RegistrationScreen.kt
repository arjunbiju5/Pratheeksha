package com.example.donor.ui.registration

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
import com.example.donor.ui.search.districtCityMap
import com.example.donor.ui.theme.RedPrimary
import com.example.donor.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.*

val bloodGroups = listOf(
    "A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-",
    "A1+", "A1-", "A2+", "A2-", "A1B+", "A1B-", "A2B+", "A2B-", "Bombay Blood Group"
)

val keralaDistricts = listOf(
    "Thiruvananthapuram", "Kollam", "Pathanamthitta", "Alappuzha",
    "Kottayam", "Idukki", "Ernakulam", "Thrissur", "Palakkad",
    "Malappuram", "Kozhikode", "Wayanad", "Kannur", "Kasaragod"
)

private val weightOptions = listOf("<50 kg", "50–60 kg", "60–70 kg", "70–80 kg", "80–90 kg", "90+ kg")
private const val INELIGIBLE_WEIGHT = "<50 kg"
private val dobFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationScreen(
    onRegistered: () -> Unit,
    viewModel: RegistrationViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var fullName    by remember { mutableStateOf("") }
    var phone       by remember { mutableStateOf("") }          // ← NEW
    var bloodGroup  by remember { mutableStateOf("") }
    var district    by remember { mutableStateOf("") }
    var city        by remember { mutableStateOf("") }
    var dateOfBirth by remember { mutableStateOf("") }
    var weight      by remember { mutableStateOf("") }

    var bgDropdownOpen       by remember { mutableStateOf(false) }
    var districtDropdownOpen by remember { mutableStateOf(false) }
    var cityDropdownOpen     by remember { mutableStateOf(false) }
    var weightDropdownOpen   by remember { mutableStateOf(false) }
    var showDatePicker       by remember { mutableStateOf(false) }

    var fullNameError    by remember { mutableStateOf(false) }
    var phoneError       by remember { mutableStateOf(false) }  // ← NEW
    var bloodGroupError  by remember { mutableStateOf(false) }
    var districtError    by remember { mutableStateOf(false) }
    var cityError        by remember { mutableStateOf(false) }
    var dateOfBirthError by remember { mutableStateOf(false) }
    var weightError      by remember { mutableStateOf(false) }

    val isUnderweight = weight == INELIGIBLE_WEIGHT

    val availableCities = remember(district) {
        if (district.isBlank()) emptyList()
        else districtCityMap[district] ?: emptyList()
    }

    LaunchedEffect(district) { city = "" }
    LaunchedEffect(uiState.isSuccess) { if (uiState.isSuccess) onRegistered() }

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
                        dateOfBirthError = false
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
        Spacer(modifier = Modifier.height(16.dp))

        Text("🩸", fontSize = 40.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Create Donor Profile",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = RedPrimary
        )
        Text(
            "Fill in your details to register as a donor",
            fontSize = 14.sp,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(32.dp))

        // ── Full Name ──────────────────────────────────────────────────────────
        SectionLabel("Full Name *", isError = fullNameError)
        OutlinedTextField(
            value = fullName,
            onValueChange = { fullName = it; fullNameError = false },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Enter your full name") },
            leadingIcon = { Icon(Icons.Default.Person, null, tint = RedPrimary) },
            isError = fullNameError,
            supportingText = { if (fullNameError) Text("Full name is required") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = redOutlinedColors(isError = fullNameError)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // ── Phone Number ───────────────────────────────────────────────────────
        SectionLabel("Phone Number *", isError = phoneError)
        OutlinedTextField(
            value = phone,
            onValueChange = {
                if (it.length <= 10) {
                    phone = it.filter { c -> c.isDigit() }
                    phoneError = false
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

        // ── Blood Group ────────────────────────────────────────────────────────
        SectionLabel("Blood Group *", isError = bloodGroupError)
        ExposedDropdownMenuBox(
            expanded = bgDropdownOpen,
            onExpandedChange = { bgDropdownOpen = !bgDropdownOpen }
        ) {
            OutlinedTextField(
                value = bloodGroup.ifBlank { "Select blood group" },
                onValueChange = {},
                readOnly = true,
                isError = bloodGroupError,
                supportingText = { if (bloodGroupError) Text("Blood group is required") },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                leadingIcon = { Icon(Icons.Default.Favorite, null, tint = RedPrimary) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = bgDropdownOpen) },
                shape = RoundedCornerShape(12.dp),
                colors = redOutlinedColors(isError = bloodGroupError)
            )
            ExposedDropdownMenu(expanded = bgDropdownOpen, onDismissRequest = { bgDropdownOpen = false }) {
                bloodGroups.forEach { group ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                group,
                                fontWeight = if (group == bloodGroup) FontWeight.Bold else FontWeight.Normal,
                                color = if (group == bloodGroup) RedPrimary else Color.Unspecified
                            )
                        },
                        onClick = { bloodGroup = group; bloodGroupError = false; bgDropdownOpen = false }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── District ───────────────────────────────────────────────────────────
        SectionLabel("District *", isError = districtError)
        ExposedDropdownMenuBox(
            expanded = districtDropdownOpen,
            onExpandedChange = { districtDropdownOpen = !districtDropdownOpen }
        ) {
            OutlinedTextField(
                value = district.ifBlank { "Select district" },
                onValueChange = {},
                readOnly = true,
                isError = districtError,
                supportingText = { if (districtError) Text("District is required") },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                leadingIcon = { Icon(Icons.Default.LocationOn, null, tint = RedPrimary) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = districtDropdownOpen) },
                shape = RoundedCornerShape(12.dp),
                colors = redOutlinedColors(isError = districtError)
            )
            ExposedDropdownMenu(expanded = districtDropdownOpen, onDismissRequest = { districtDropdownOpen = false }) {
                keralaDistricts.forEach { d ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                d,
                                fontWeight = if (d == district) FontWeight.Bold else FontWeight.Normal,
                                color = if (d == district) RedPrimary else Color.Unspecified
                            )
                        },
                        onClick = { district = d; districtError = false; districtDropdownOpen = false }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── City ───────────────────────────────────────────────────────────────
        SectionLabel("City *", isError = cityError)
        ExposedDropdownMenuBox(
            expanded = cityDropdownOpen && district.isNotBlank(),
            onExpandedChange = { if (district.isNotBlank()) cityDropdownOpen = !cityDropdownOpen }
        ) {
            OutlinedTextField(
                value = city.ifBlank { if (district.isBlank()) "Select district first" else "Select city" },
                onValueChange = {},
                readOnly = true,
                enabled = district.isNotBlank(),
                isError = cityError,
                supportingText = { if (cityError) Text("City is required") },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                leadingIcon = {
                    Icon(Icons.Default.Place, null,
                        tint = if (district.isNotBlank()) RedPrimary else TextSecondary)
                },
                trailingIcon = {
                    if (district.isNotBlank())
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = cityDropdownOpen)
                },
                shape = RoundedCornerShape(12.dp),
                colors = redOutlinedColors(isError = cityError)
            )
            ExposedDropdownMenu(
                expanded = cityDropdownOpen && district.isNotBlank(),
                onDismissRequest = { cityDropdownOpen = false }
            ) {
                availableCities.forEach { c ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                c,
                                fontWeight = if (c == city) FontWeight.Bold else FontWeight.Normal,
                                color = if (c == city) RedPrimary else Color.Unspecified
                            )
                        },
                        onClick = { city = c; cityError = false; cityDropdownOpen = false }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── Date of Birth ──────────────────────────────────────────────────────
        SectionLabel("Date of Birth *", isError = dateOfBirthError)
        OutlinedTextField(
            value = dateOfBirth.ifBlank { "Tap to select date" },
            onValueChange = {},
            readOnly = true,
            isError = dateOfBirthError,
            supportingText = { if (dateOfBirthError) Text("Date of birth is required") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.DateRange, null, tint = RedPrimary) },
            trailingIcon = {
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(Icons.Default.Edit, "Pick date", tint = RedPrimary)
                }
            },
            shape = RoundedCornerShape(12.dp),
            colors = redOutlinedColors(isError = dateOfBirthError)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // ── Weight Category ────────────────────────────────────────────────────
        SectionLabel("Weight Category *", isError = weightError)
        ExposedDropdownMenuBox(
            expanded = weightDropdownOpen,
            onExpandedChange = { weightDropdownOpen = !weightDropdownOpen }
        ) {
            OutlinedTextField(
                value = weight.ifBlank { "Select weight category" },
                onValueChange = {},
                readOnly = true,
                isError = weightError || isUnderweight,
                supportingText = { if (weightError) Text("Weight category is required") },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                leadingIcon = {
                    Icon(Icons.Default.MonitorWeight, null,
                        tint = if (isUnderweight) Color(0xFFB71C1C) else RedPrimary)
                },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = weightDropdownOpen) },
                shape = RoundedCornerShape(12.dp),
                colors = redOutlinedColors(isError = weightError || isUnderweight)
            )
            ExposedDropdownMenu(expanded = weightDropdownOpen, onDismissRequest = { weightDropdownOpen = false }) {
                weightOptions.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    option,
                                    fontWeight = if (option == weight) FontWeight.Bold else FontWeight.Normal,
                                    color = when {
                                        option == INELIGIBLE_WEIGHT -> Color(0xFFB71C1C)
                                        option == weight -> RedPrimary
                                        else -> Color.Unspecified
                                    }
                                )
                                if (option == INELIGIBLE_WEIGHT) {
                                    Spacer(Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFFFFEBEE)
                                    ) {
                                        Text(
                                            "Not eligible",
                                            fontSize = 10.sp,
                                            color = Color(0xFFB71C1C),
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        },
                        onClick = { weight = option; weightError = false; weightDropdownOpen = false }
                    )
                }
            }
        }

        // ── Underweight banner ─────────────────────────────────────────────────
        if (isUnderweight) {
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFFFEBEE),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Default.Cancel,
                        contentDescription = null,
                        tint = Color(0xFFB71C1C),
                        modifier = Modifier.size(20.dp).padding(top = 1.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            "Not eligible to donate",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB71C1C)
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            "Donors must weigh at least 50 kg. " +
                                    "This is a standard medical requirement to ensure your safety during donation.",
                            fontSize = 12.sp,
                            color = Color(0xFF8B0000),
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // ── Error message ──────────────────────────────────────────────────────
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

        // ── Register Button ────────────────────────────────────────────────────
        Button(
            onClick = {
                fullNameError    = fullName.isBlank()
                phoneError       = phone.isBlank() || phone.length < 10
                bloodGroupError  = bloodGroup.isBlank()
                districtError    = district.isBlank()
                cityError        = city.isBlank()
                dateOfBirthError = dateOfBirth.isBlank()
                weightError      = weight.isBlank()

                if (isUnderweight) return@Button
                if (fullNameError || phoneError || bloodGroupError || districtError ||
                    cityError || dateOfBirthError || weightError) return@Button

                viewModel.saveDonor(
                    fullName     = fullName.trim(),
                    bloodGroup   = bloodGroup,
                    district     = district,
                    city         = city,
                    dateOfBirth  = dateOfBirth,
                    weight       = weight,
                    lastDonation = "",
                    mobile       = phone.trim()          // ← passed in now
                )
            },
            enabled = !uiState.isLoading && !isUnderweight,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isUnderweight) Color(0xFFBDBDBD) else RedPrimary,
                disabledContainerColor = Color(0xFFBDBDBD)
            )
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
            } else {
                Icon(
                    if (isUnderweight) Icons.Default.Block else Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (isUnderweight) "Not Eligible to Register" else "Register as Donor",
                    fontSize = 16.sp,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// ── Reusable composables ───────────────────────────────────────────────────────

@Composable
fun SectionLabel(text: String, isError: Boolean = false) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = if (isError) Color(0xFFB71C1C) else TextSecondary,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun redOutlinedColors(isError: Boolean = false) = OutlinedTextFieldDefaults.colors(
    focusedBorderColor   = if (isError) Color(0xFFB71C1C) else RedPrimary,
    unfocusedBorderColor = if (isError) Color(0xFFB71C1C).copy(alpha = 0.6f)
    else TextSecondary.copy(alpha = 0.4f),
    focusedLabelColor    = if (isError) Color(0xFFB71C1C) else RedPrimary,
    cursorColor          = RedPrimary,
    errorBorderColor     = Color(0xFFB71C1C)
)