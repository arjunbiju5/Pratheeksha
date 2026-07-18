package com.example.donor.ui.request


import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.donor.ui.registration.SectionLabel
import com.example.donor.ui.registration.bloodGroups
import com.example.donor.ui.registration.keralaDistricts
import com.example.donor.ui.registration.redOutlinedColors
import com.example.donor.ui.theme.RedPrimary
import com.example.donor.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestBloodScreen(
    onBack: () -> Unit,
    onSubmitted: () -> Unit,
    onTrackRequest: (String) -> Unit,
    viewModel: RequestBloodViewModel = viewModel(
        factory = androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory
            .getInstance(androidx.compose.ui.platform.LocalContext.current.applicationContext
                    as android.app.Application)

    )
) {
    val uiState by viewModel.uiState.collectAsState()

    var patientName by remember { mutableStateOf("") }
    var bystanderName by remember { mutableStateOf("") }
    var contactPhone by remember { mutableStateOf("") }
    var bloodGroup by remember { mutableStateOf("") }
    var units by remember { mutableStateOf("1") }
    var hospital by remember { mutableStateOf("") }
    var district by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var medicalCase by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var urgency by remember { mutableStateOf("normal") }
    var requiredUntil by remember { mutableStateOf("") }

    var bgExpanded by remember { mutableStateOf(false) }
    var districtExpanded by remember { mutableStateOf(false) }
    var unitsExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    var patientNameError by remember { mutableStateOf(false) }
    var phoneError by remember { mutableStateOf(false) }
    var bgError by remember { mutableStateOf(false) }
    var hospitalError by remember { mutableStateOf(false) }
    var districtError by remember { mutableStateOf(false) }
    var cityError by remember { mutableStateOf(false) }
    var medicalCaseError by remember { mutableStateOf(false) }
    var requiredUntilError by remember { mutableStateOf(false) }

    val isUrgent = urgency == "urgent"
    val accentColor = if (isUrgent) Color(0xFFB71C1C) else RedPrimary

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val fmt = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                        requiredUntil = fmt.format(java.util.Date(millis))
                        requiredUntilError = false
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    LaunchedEffect(uiState.isSuccess, uiState.requestId) {
        if (uiState.isSuccess && uiState.requestId != null) {
            onTrackRequest(uiState.requestId!!)
        }
    }

    fun validate(): Boolean {
        patientNameError = patientName.isBlank()
        phoneError = contactPhone.length != 10
        bgError = bloodGroup.isBlank()
        hospitalError = hospital.isBlank()
        districtError = district.isBlank()
        cityError = city.isBlank()
        medicalCaseError = medicalCase.isBlank()
        requiredUntilError = requiredUntil.isBlank()
        return !patientNameError && !phoneError && !bgError &&
                !hospitalError && !districtError && !cityError && !medicalCaseError && !requiredUntilError
    }

    Column(modifier = Modifier.fillMaxSize()) {

        // ── Top Bar ──────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(accentColor, accentColor.copy(alpha = 0.85f))
                    )
                )
                .padding(top = 44.dp, start = 4.dp, end = 16.dp, bottom = 20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Column {
                    Text(
                        "Request Blood",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        "Donors in your district will be notified",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        }

        // ── Form Body ─────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {

            // ── 1. Urgency ────────────────────────────────────────────────────
            FormSection(title = "Urgency Level", icon = Icons.Default.PriorityHigh) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("normal" to "Normal", "urgent" to "🚨 Urgent").forEach { (key, label) ->
                        val selected = urgency == key
                        FilterChip(
                            selected = selected,
                            onClick = { urgency = key },
                            label = {
                                Text(
                                    label,
                                    fontSize = 13.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (key == "urgent") Color(0xFFB71C1C) else RedPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            // ── 2. Patient & Contact ──────────────────────────────────────────
            FormSection(title = "Patient & Contact Details", icon = Icons.Default.Person) {

                SectionLabel("Patient Name *", isError = patientNameError)
                OutlinedTextField(
                    value = patientName,
                    onValueChange = { patientName = it; patientNameError = false },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Full name of the patient") },
                    leadingIcon = { Icon(Icons.Default.PersonOutline, null, tint = accentColor) },
                    isError = patientNameError,
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = redOutlinedColors(patientNameError)
                )

                Spacer(Modifier.height(4.dp))

                SectionLabel("Bystander / Requester Name", isError = false)
                OutlinedTextField(
                    value = bystanderName,
                    onValueChange = { bystanderName = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Name of person making request") },
                    leadingIcon = { Icon(Icons.Default.PersonOutline, null, tint = accentColor) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = redOutlinedColors(false)
                )

                Spacer(Modifier.height(4.dp))

                SectionLabel("Contact Number *", isError = phoneError)
                OutlinedTextField(
                    value = contactPhone,
                    onValueChange = {
                        if (it.length <= 10) {
                            contactPhone = it.filter { c -> c.isDigit() }
                            phoneError = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("10-digit mobile number") },
                    leadingIcon = { Icon(Icons.Default.Phone, null, tint = accentColor) },
                    isError = phoneError,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    colors = redOutlinedColors(phoneError)
                )
                if (phoneError) {
                    Text("Enter a valid 10-digit number", color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                }
            }

            // ── 3. Blood Requirement ──────────────────────────────────────────
            FormSection(title = "Blood Requirement", icon = Icons.Default.Favorite) {

                SectionLabel("Blood Group *", isError = bgError)
                ExposedDropdownMenuBox(
                    expanded = bgExpanded,
                    onExpandedChange = { bgExpanded = !bgExpanded }
                ) {
                    OutlinedTextField(
                        value = bloodGroup,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        placeholder = { Text("Select blood group") },
                        leadingIcon = { Icon(Icons.Default.Bloodtype, null, tint = accentColor) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = bgExpanded) },
                        isError = bgError,
                        shape = RoundedCornerShape(12.dp),
                        colors = redOutlinedColors(bgError)
                    )
                    ExposedDropdownMenu(
                        expanded = bgExpanded,
                        onDismissRequest = { bgExpanded = false }
                    ) {
                        bloodGroups.forEach { group ->
                            DropdownMenuItem(
                                text = { Text(group) },
                                onClick = {
                                    bloodGroup = group; bgExpanded = false; bgError = false
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                SectionLabel("Units Required", isError = false)
                OutlinedTextField(
                    value = units,
                    onValueChange = { input ->
                        units = input.filter { it.isDigit() }.take(3) // up to 999 units
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g. 2") },
                    leadingIcon = { Icon(Icons.Default.Numbers, null, tint = accentColor) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    colors = redOutlinedColors(false)
                )

                Spacer(Modifier.height(4.dp))

                SectionLabel("Required Until *", isError = requiredUntilError)
                OutlinedTextField(
                    value = requiredUntil,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("When is the blood needed by?") },
                    leadingIcon = { Icon(Icons.Default.CalendarToday, null, tint = accentColor) },
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.DateRange, null, tint = accentColor)
                        }
                    },
                    isError = requiredUntilError,
                    shape = RoundedCornerShape(12.dp),
                    colors = redOutlinedColors(requiredUntilError)
                )
            }
            // ── 4. Hospital & Location ────────────────────────────────────────
            FormSection(title = "Hospital & Location", icon = Icons.Default.LocalHospital) {

                SectionLabel("Hospital Name *", isError = hospitalError)
                OutlinedTextField(
                    value = hospital,
                    onValueChange = { hospital = it; hospitalError = false },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Name of the hospital") },
                    leadingIcon = { Icon(Icons.Default.MedicalServices, null, tint = accentColor) },
                    isError = hospitalError,
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = redOutlinedColors(hospitalError)
                )

                Spacer(Modifier.height(4.dp))

                SectionLabel("District *", isError = districtError)
                ExposedDropdownMenuBox(
                    expanded = districtExpanded,
                    onExpandedChange = { districtExpanded = !districtExpanded }
                ) {
                    OutlinedTextField(
                        value = district,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        placeholder = { Text("Select district") },
                        leadingIcon = { Icon(Icons.Default.LocationOn, null, tint = accentColor) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = districtExpanded) },
                        isError = districtError,
                        shape = RoundedCornerShape(12.dp),
                        colors = redOutlinedColors(districtError)
                    )
                    ExposedDropdownMenu(
                        expanded = districtExpanded,
                        onDismissRequest = { districtExpanded = false }
                    ) {
                        keralaDistricts.forEach { d ->
                            DropdownMenuItem(
                                text = { Text(d) },
                                onClick = { district = d; districtExpanded = false; districtError = false }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                SectionLabel("City / Town *", isError = cityError)
                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it; cityError = false },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Nearest city or town") },
                    leadingIcon = { Icon(Icons.Default.LocationCity, null, tint = accentColor) },
                    isError = cityError,
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = redOutlinedColors(cityError)
                )
            }

            // ── 5. Medical Info ───────────────────────────────────────────────
            FormSection(title = "Medical Information", icon = Icons.Default.Info) {

                SectionLabel("Medical Case / Reason *", isError = medicalCaseError)
                OutlinedTextField(
                    value = medicalCase,
                    onValueChange = { medicalCase = it; medicalCaseError = false },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g. Surgery, Accident, Thalassemia…") },
                    leadingIcon = { Icon(Icons.Default.Description, null, tint = accentColor) },
                    isError = medicalCaseError,
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = redOutlinedColors(medicalCaseError)
                )

                Spacer(Modifier.height(4.dp))

                SectionLabel("Additional Notes (optional)", isError = false)
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 90.dp),
                    placeholder = { Text("Any other details donors should know") },
                    maxLines = 4,
                    shape = RoundedCornerShape(12.dp),
                    colors = redOutlinedColors(false)
                )
            }

            // ── Error banner ──────────────────────────────────────────────────
            if (uiState.errorMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ErrorOutline, null, tint = Color(0xFFB71C1C))
                        Text(uiState.errorMessage!!, color = Color(0xFFB71C1C), fontSize = 13.sp)
                    }
                }
            }

            // ── Submit Button ─────────────────────────────────────────────────
            Button(
                onClick = {
                    if (validate()) {
                        viewModel.submitRequest(
                            patientName = patientName,
                            bystanderName = bystanderName,
                            contactPhone = contactPhone,
                            bloodGroup = bloodGroup,
                            units = units.toIntOrNull() ?: 1,
                            hospital = hospital,
                            district = district,
                            city = city,
                            medicalCase = medicalCase,
                            note = note,
                            urgency = urgency,
                            requiredUntil = requiredUntil
                        )
                    }
                },
                enabled = !uiState.isLoading && !uiState.isSendingNotifications,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
            ) {
                if (uiState.isLoading || uiState.isSendingNotifications) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = if (uiState.isSendingNotifications) "Notifying donors..." else "Submitting...",
                            fontSize = 15.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    Text(
                        if (isUrgent) "Send Urgent Request" else "Send Blood Request",
                        fontSize = 16.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ── Reusable section card ─────────────────────────────────────────────────────
@Composable
private fun FormSection(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 14.dp)
            ) {
                Icon(icon, null, tint = RedPrimary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1C1C1C))
            }
            content()
        }
    }
}