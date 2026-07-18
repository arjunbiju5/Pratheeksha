package com.example.donor.ui.admin

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.example.donor.data.model.Donor
import com.example.donor.ui.theme.RedPrimary
import com.example.donor.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.*

// ── Constants shared across admin screens ──────────────────────────────────────

private val bloodGroups = listOf(
    "A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-",
    "A1+", "A1-", "A2+", "A2-", "A1B+", "A1B-", "A2B+", "A2B-",
    "Bombay Blood Group"
)

private val weightOptions = listOf(
    "<50 kg", "50–60 kg", "60–70 kg", "70–80 kg", "80–90 kg", "90+ kg"
)
private const val INELIGIBLE_WEIGHT = "<50 kg"

private val districtCityMap = mapOf(
    "Thiruvananthapuram" to listOf("Thiruvananthapuram", "Attingal", "Neyyattinkara", "Varkala", "Nedumangad", "Kattakada", "Kazhakkoottam", "Kovalam", "Parassala", "Balaramapuram", "Pothencode", "Kilimanoor"),
    "Kollam"             to listOf("Kollam", "Karunagappally", "Kottarakkara", "Punalur", "Pathanapuram", "Chadayamangalam", "Chathannoor", "Chavara", "Kundara", "Mynagappally", "Neduvathoor", "Anchal", "Paravur", "Sasthamcotta", "Eravipuram"),
    "Pathanamthitta"     to listOf("Pathanamthitta", "Adoor", "Pandalam", "Konni", "Ranni", "Thiruvalla", "Kozhencherry", "Mallappally", "Pullad", "Aranmula", "Elanthoor"),
    "Alappuzha"          to listOf("Alleppey", "Ambalapuzha", "Cherthala", "Chengannur", "Haripad", "Kayamkulam", "Kuttanad", "Mavelikara", "Mannar", "Thakazhi", "Aroor", "Thuravoor"),
    "Kottayam"           to listOf("Kottayam", "Changanassery", "Pala", "Vaikom", "Ettumanoor", "Erattupetta", "Kanjirappally", "Ponkunnam", "Kuravilangad", "Kaduthuruthy"),
    "Idukki"             to listOf("Idukki", "Thodupuzha", "Devikulam", "Peerumade", "Udumbanchola", "Munnar", "Adimali", "Kattappana", "Nedumkandam", "Kumily", "Vandiperiyar", "Cheruthoni"),
    "Ernakulam"          to listOf("Ernakulam", "Kochi", "Mattancherry", "Palluruthy", "Tripunithura", "Piravom", "Perumbavoor", "Muvattupuzha", "Kothamangalam", "Aluva", "Angamaly", "North Paravur", "Narakkal", "Kakkanad", "Kalady", "Fort Kochi"),
    "Thrissur"           to listOf("Thrissur", "Chalakudy", "Kodungallur", "Irinjalakuda", "Kunnamkulam", "Chavakkad", "Guruvayur", "Wadakkanchery", "Puthukkad", "Mala", "Ollur", "Pazhayannur"),
    "Palakkad"           to listOf("Palakkad", "Ottapalam", "Shoranur", "Pattambi", "Mannarkkad", "Cherpulassery", "Alathur", "Chittur", "Nenmara", "Koduvayur", "Kongad", "Parali", "Thrithala", "Kuzhalmannam", "Vadakkenchery"),
    "Malappuram"         to listOf("Malappuram", "Manjeri", "Perinthalmanna", "Nilambur", "Tirur", "Tirurangadi", "Ponnani", "Kondotty", "Kottakkal", "Valanchery", "Parappanangadi", "Tanur", "Edappal", "Areekode", "Wandoor"),
    "Kozhikode"          to listOf("Kozhikode", "Vadakara", "Koyilandy", "Mukkam", "Feroke", "Ramanattukara", "Payyoli", "Balussery", "Nadapuram", "Thamarassery", "Perambra"),
    "Wayanad"            to listOf("Kalpetta", "Mananthavady", "Sulthan Bathery", "Vythiri", "Meppadi", "Panamaram", "Pulpally", "Ambalavayal"),
    "Kannur"             to listOf("Kannur", "Thalassery", "Payyannur", "Taliparamba", "Mattannur", "Iritty", "Kuthuparamba", "Peravoor", "Peringalam", "Edakkad", "Irikkur", "Dharmadam"),
    "Kasaragod"          to listOf("Kasaragod", "Kanhangad", "Nileshwar", "Bekal", "Cheruvathur", "Uppala", "Manjeshwaram", "Vellarikundu", "Badiadka", "Trikaripur")
)

// ── Screen ─────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminEditDonorScreen(
    donor: Donor?,
    onBack: () -> Unit,
    viewModel: AdminViewModel = viewModel()
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val isEditMode = donor != null
    val uiState   by viewModel.adminUiState.collectAsState()

    // ── Form state ──
    var fullName     by remember { mutableStateOf(donor?.fullName     ?: "") }
    var bloodGroup   by remember { mutableStateOf(donor?.bloodGroup   ?: "") }
    var mobile       by remember { mutableStateOf(donor?.mobile       ?: "") }
    var district     by remember { mutableStateOf(donor?.district     ?: "") }
    var city         by remember { mutableStateOf(donor?.city         ?: "") }
    var lastDonation by remember { mutableStateOf(donor?.lastDonation ?: "") }
    var nextEligible by remember { mutableStateOf(donor?.nextEligible ?: "") }
    var dateOfBirth  by remember { mutableStateOf(donor?.dateOfBirth  ?: "") }
    var weight       by remember { mutableStateOf(donor?.weight       ?: "") }

    // ── Dropdown expanded state ──
    var bloodGroupExpanded by remember { mutableStateOf(false) }
    var districtExpanded   by remember { mutableStateOf(false) }
    var cityExpanded       by remember { mutableStateOf(false) }
    var weightExpanded     by remember { mutableStateOf(false) }

    // ── Date pickers ──
    var showLastDonationPicker by remember { mutableStateOf(false) }
    var showDobPicker          by remember { mutableStateOf(false) }

    // ── Validation error flags ──
    var nameError        by remember { mutableStateOf(false) }
    var mobileError      by remember { mutableStateOf(false) }
    var bloodGroupError  by remember { mutableStateOf(false) }
    var districtError    by remember { mutableStateOf(false) }
    var cityError        by remember { mutableStateOf(false) }
    var dobError         by remember { mutableStateOf(false) }
    var weightError      by remember { mutableStateOf(false) }

    // Cities list updates when district changes
    val availableCities = remember(district) {
        if (district.isBlank()) emptyList()
        else districtCityMap[district] ?: emptyList()
    }


    // Key derived state
    val isUnderweight    = weight == INELIGIBLE_WEIGHT
    val isAvailable      = Donor(
        lastDonation = lastDonation,
        nextEligible = nextEligible,
        available    = true
    ).isCurrentlyAvailable()
    val borderColor      = if (isAvailable) Color(0xFF4CAF50) else Color(0xFFFF9800)

    // Auto-calculate next eligible date whenever last donation changes
    LaunchedEffect(lastDonation) {
        if (lastDonation.isNotBlank() && lastDonation != "Never") {
            try {
                val parsed = dateFormat.parse(lastDonation) ?: return@LaunchedEffect
                val cal    = Calendar.getInstance().also {
                    it.time = parsed
                    it.add(Calendar.DAY_OF_YEAR, 90)
                }
                nextEligible = dateFormat.format(cal.time)
            } catch (_: Exception) { /* leave as-is for invalid dates */ }
        } else {
            nextEligible = ""
        }
    }

    // Navigate back on successful save
    var saveTriggered by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.isLoading, uiState.errorMessage) {
        if (saveTriggered && !uiState.isLoading && uiState.errorMessage == null) onBack()
    }

    // ── Validation + save ──
    fun validateAndSave() {
        nameError       = fullName.isBlank()
        mobileError     = mobile.isBlank() || mobile.length < 10
        bloodGroupError = bloodGroup.isBlank()
        districtError   = district.isBlank()
        cityError       = city.isBlank()
        dobError        = dateOfBirth.isBlank()
        weightError     = weight.isBlank()

        if (isUnderweight || nameError || mobileError || bloodGroupError ||
            districtError || cityError || dobError || weightError) return

        val result = Donor(
            uid          = donor?.uid ?: "",
            fullName     = fullName.trim(),
            bloodGroup   = bloodGroup.trim(),
            mobile       = mobile.trim(),
            district     = district.trim(),
            city         = city.trim(),
            regDate      = donor?.regDate ?: dateFormat.format(Date()),
            lastDonation = if (lastDonation.isBlank()) "Never" else lastDonation.trim(),
            nextEligible = nextEligible.trim(),
            dateOfBirth  = dateOfBirth.trim(),
            weight       = weight.trim()
        )
        saveTriggered = true
        if (isEditMode) viewModel.updateDonor(result) else viewModel.addDonor(result)
    }

    // ── Last Donation Date Picker ──
    if (showLastDonationPicker) {
        val initialMillis = remember(lastDonation) {
            if (lastDonation.isNotBlank() && lastDonation != "Never") {
                try { dateFormat.parse(lastDonation)?.time } catch (_: Exception) { null }
            } else null
        } ?: System.currentTimeMillis()

        val state = rememberDatePickerState(
            initialSelectedDateMillis = initialMillis,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) =
                    utcTimeMillis <= System.currentTimeMillis()
            }
        )
        DatePickerDialog(
            onDismissRequest = { showLastDonationPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { lastDonation = dateFormat.format(Date(it)) }
                    showLastDonationPicker = false
                }) { Text("OK", color = RedPrimary) }
            },
            dismissButton = {
                TextButton(onClick = { showLastDonationPicker = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        ) { DatePicker(state = state) }
    }

    // ── Date of Birth Picker ──
    if (showDobPicker) {
        val initialMillis = remember(dateOfBirth) {
            if (dateOfBirth.isNotBlank()) {
                try { dateFormat.parse(dateOfBirth)?.time } catch (_: Exception) { null }
            } else null
        } ?: System.currentTimeMillis()

        val dobState = rememberDatePickerState(
            initialSelectedDateMillis = initialMillis,
            selectableDates = object : SelectableDates {
                // DOB can't be in the future and shouldn't be unrealistically old
                override fun isSelectableDate(utcTimeMillis: Long) =
                    utcTimeMillis <= System.currentTimeMillis()
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDobPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dobState.selectedDateMillis?.let {
                        dateOfBirth = dateFormat.format(Date(it))
                        dobError    = false
                    }
                    showDobPicker = false
                }) { Text("OK", color = RedPrimary) }
            },
            dismissButton = {
                TextButton(onClick = { showDobPicker = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        ) { DatePicker(state = dobState) }
    }

    // ── Root layout — outer border reflects availability state ──
    Column(
        modifier = Modifier
            .fillMaxSize()
            .border(
                2.dp, borderColor,
                RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 12.dp, bottomEnd = 12.dp)
            )
    ) {

        // ── Header ──────────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(RedPrimary)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text       = if (isEditMode) "Edit Donor" else "Add New Donor",
                fontSize   = 20.sp,
                fontWeight = FontWeight.Bold,
                color      = Color.White
            )
        }

        // ── Scrollable form body ──────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {

            // Firebase error banner
            uiState.errorMessage?.let { error ->
                Surface(
                    shape    = RoundedCornerShape(8.dp),
                    color    = Color(0xFFFFEBEE),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Text(
                        text     = error,
                        color    = Color(0xFFB71C1C),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // ── Full Name ──────────────────────────────────────────────────────
            AdminFieldLabel("Full Name *", isError = nameError)
            OutlinedTextField(
                value          = fullName,
                onValueChange  = { fullName = it; nameError = false },
                modifier       = Modifier.fillMaxWidth(),
                placeholder    = { Text("Enter full name") },
                leadingIcon    = { Icon(Icons.Default.Person, null, tint = if (nameError) Color(0xFFB71C1C) else RedPrimary) },
                isError        = nameError,
                supportingText = { if (nameError) Text("Full name is required") },
                singleLine     = true,
                shape          = RoundedCornerShape(10.dp),
                colors         = adminFieldColors(isError = nameError)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // ── Blood Group ────────────────────────────────────────────────────
            AdminFieldLabel("Blood Group *", isError = bloodGroupError)
            ExposedDropdownMenuBox(
                expanded        = bloodGroupExpanded,
                onExpandedChange = { bloodGroupExpanded = !bloodGroupExpanded }
            ) {
                OutlinedTextField(
                    value          = bloodGroup.ifBlank { "Select blood group" },
                    onValueChange  = {},
                    readOnly       = true,
                    isError        = bloodGroupError,
                    supportingText = { if (bloodGroupError) Text("Blood group is required") },
                    leadingIcon    = { Icon(Icons.Default.Favorite, null, tint = if (bloodGroupError) Color(0xFFB71C1C) else RedPrimary) },
                    trailingIcon   = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = bloodGroupExpanded) },
                    shape          = RoundedCornerShape(10.dp),
                    colors         = adminFieldColors(isError = bloodGroupError),
                    modifier       = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded        = bloodGroupExpanded,
                    onDismissRequest = { bloodGroupExpanded = false }
                ) {
                    bloodGroups.forEach { group ->
                        DropdownMenuItem(
                            text  = {
                                Text(
                                    group,
                                    fontWeight = if (group == bloodGroup) FontWeight.Bold else FontWeight.Normal,
                                    color      = if (group == bloodGroup) RedPrimary else Color.Unspecified
                                )
                            },
                            onClick = {
                                bloodGroup      = group
                                bloodGroupError = false
                                bloodGroupExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ── Mobile Number ─────────────────────────────────────────────────
            AdminFieldLabel("Mobile Number *", isError = mobileError)
            OutlinedTextField(
                value          = mobile,
                onValueChange  = { if (it.length <= 10) { mobile = it; mobileError = false } },
                modifier       = Modifier.fillMaxWidth(),
                placeholder    = { Text("10-digit number") },
                leadingIcon    = { Icon(Icons.Default.Phone, null, tint = if (mobileError) Color(0xFFB71C1C) else RedPrimary) },
                isError        = mobileError,
                supportingText = { if (mobileError) Text("Valid 10-digit number required") },
                singleLine     = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                shape          = RoundedCornerShape(10.dp),
                colors         = adminFieldColors(isError = mobileError)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // ── District ──────────────────────────────────────────────────────
            AdminFieldLabel("District *", isError = districtError)
            ExposedDropdownMenuBox(
                expanded        = districtExpanded,
                onExpandedChange = { districtExpanded = !districtExpanded }
            ) {
                OutlinedTextField(
                    value          = district.ifBlank { "Select district" },
                    onValueChange  = {},
                    readOnly       = true,
                    isError        = districtError,
                    supportingText = { if (districtError) Text("District is required") },
                    leadingIcon    = { Icon(Icons.Default.LocationOn, null, tint = if (districtError) Color(0xFFB71C1C) else RedPrimary) },
                    trailingIcon   = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = districtExpanded) },
                    shape          = RoundedCornerShape(10.dp),
                    colors         = adminFieldColors(isError = districtError),
                    modifier       = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded        = districtExpanded,
                    onDismissRequest = { districtExpanded = false }
                ) {
                    districtCityMap.keys.sorted().forEach { d ->
                        DropdownMenuItem(
                            text  = {
                                Text(
                                    d,
                                    fontWeight = if (d == district) FontWeight.Bold else FontWeight.Normal,
                                    color      = if (d == district) RedPrimary else Color.Unspecified
                                )
                            },
                            onClick = {
                                if (district != d) {
                                    district = d
                                    city = ""
                                }
                                districtError = false
                                districtExpanded = false
                            }

                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ── City (depends on District) ─────────────────────────────────────
            AdminFieldLabel("City *", isError = cityError)
            ExposedDropdownMenuBox(
                expanded        = cityExpanded && district.isNotBlank(),
                onExpandedChange = { if (district.isNotBlank()) cityExpanded = !cityExpanded }
            ) {
                OutlinedTextField(
                    value          = city.ifBlank { if (district.isBlank()) "Select district first" else "Select city" },
                    onValueChange  = {},
                    readOnly       = true,
                    enabled        = district.isNotBlank(),
                    isError        = cityError,
                    supportingText = { if (cityError) Text("City is required") },
                    leadingIcon    = {
                        Icon(
                            Icons.Default.Place, null,
                            tint = when {
                                cityError         -> Color(0xFFB71C1C)
                                district.isBlank() -> TextSecondary
                                else              -> RedPrimary
                            }
                        )
                    },
                    trailingIcon = {
                        if (district.isNotBlank())
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = cityExpanded)
                    },
                    shape          = RoundedCornerShape(10.dp),
                    colors         = adminFieldColors(isError = cityError),
                    modifier       = Modifier.menuAnchor().fillMaxWidth()
                )
                if (district.isNotBlank()) {
                    ExposedDropdownMenu(
                        expanded        = cityExpanded,
                        onDismissRequest = { cityExpanded = false }
                    ) {
                        availableCities.forEach { c ->
                            DropdownMenuItem(
                                text  = {
                                    Text(
                                        c,
                                        fontWeight = if (c == city) FontWeight.Bold else FontWeight.Normal,
                                        color      = if (c == city) RedPrimary else Color.Unspecified
                                    )
                                },
                                onClick = {
                                    city      = c
                                    cityError = false
                                    cityExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ── Date of Birth (date picker, mandatory) ────────────────────────
            AdminFieldLabel("Date of Birth *", isError = dobError)
            val dobInteraction = remember { MutableInteractionSource() }
            LaunchedEffect(dobInteraction) {
                dobInteraction.interactions.collect { interaction ->
                    if (interaction is PressInteraction.Release) showDobPicker = true
                }
            }
            OutlinedTextField(
                value             = dateOfBirth.ifBlank { "Tap to select date" },
                onValueChange     = {},
                readOnly          = true,
                interactionSource = dobInteraction,
                isError           = dobError,
                supportingText    = { if (dobError) Text("Date of birth is required") },
                modifier          = Modifier.fillMaxWidth(),
                leadingIcon       = { Icon(Icons.Default.Cake, null, tint = if (dobError) Color(0xFFB71C1C) else RedPrimary) },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (dateOfBirth.isNotBlank()) {
                            IconButton(onClick = { dateOfBirth = ""; dobError = false }) {
                                Icon(Icons.Default.Close, "Clear DOB", tint = TextSecondary)
                            }
                        }
                        IconButton(onClick = { showDobPicker = true }) {
                            Icon(Icons.Default.DateRange, "Pick DOB", tint = RedPrimary)
                        }
                    }
                },
                singleLine = true,
                shape      = RoundedCornerShape(10.dp),
                colors     = adminFieldColors(isError = dobError)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // ── Weight Category (mandatory, guards submission if <50 kg) ─────
            AdminFieldLabel("Weight Category *", isError = weightError || isUnderweight)
            ExposedDropdownMenuBox(
                expanded        = weightExpanded,
                onExpandedChange = { weightExpanded = !weightExpanded }
            ) {
                OutlinedTextField(
                    value          = weight.ifBlank { "Select weight category" },
                    onValueChange  = {},
                    readOnly       = true,
                    isError        = weightError || isUnderweight,
                    supportingText = { if (weightError) Text("Weight category is required") },
                    leadingIcon    = {
                        Icon(
                            Icons.Default.MonitorWeight, null,
                            tint = when {
                                isUnderweight -> Color(0xFFB71C1C)
                                weightError   -> Color(0xFFB71C1C)
                                else          -> RedPrimary
                            }
                        )
                    },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = weightExpanded) },
                    shape    = RoundedCornerShape(10.dp),
                    colors   = adminFieldColors(isError = weightError || isUnderweight),
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded        = weightExpanded,
                    onDismissRequest = { weightExpanded = false }
                ) {
                    weightOptions.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        option,
                                        fontWeight = if (option == weight) FontWeight.Bold else FontWeight.Normal,
                                        color = when {
                                            option == INELIGIBLE_WEIGHT -> Color(0xFFB71C1C)
                                            option == weight            -> RedPrimary
                                            else                        -> Color.Unspecified
                                        }
                                    )
                                    if (option == INELIGIBLE_WEIGHT) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xFFFFEBEE)
                                        ) {
                                            Text(
                                                "Not eligible",
                                                fontSize   = 10.sp,
                                                color      = Color(0xFFB71C1C),
                                                fontWeight = FontWeight.SemiBold,
                                                modifier   = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            },
                            onClick = {
                                weight      = option
                                weightError = false
                                weightExpanded = false
                            }
                        )
                    }
                }
            }

            // ── Underweight ineligibility banner ──────────────────────────────
            if (isUnderweight) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape    = RoundedCornerShape(10.dp),
                    color    = Color(0xFFFFEBEE),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier          = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Default.Cancel,
                            contentDescription = null,
                            tint     = Color(0xFFB71C1C),
                            modifier = Modifier.size(20.dp).padding(top = 1.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                "Not eligible to donate",
                                fontSize   = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color      = Color(0xFFB71C1C)
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                "Donors must weigh at least 50 kg. This is a standard " +
                                        "medical requirement to ensure the donor's safety.",
                                fontSize   = 12.sp,
                                color      = Color(0xFF8B0000),
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ── Last Donation Date (optional, date picker) ────────────────────
            AdminFieldLabel("Last Donation Date (optional)")
            val lastDonationInteraction = remember { MutableInteractionSource() }
            LaunchedEffect(lastDonationInteraction) {
                lastDonationInteraction.interactions.collect { interaction ->
                    if (interaction is PressInteraction.Release) showLastDonationPicker = true
                }
            }
            OutlinedTextField(
                value             = lastDonation.ifBlank { "Tap to select date" },
                onValueChange     = {},
                readOnly          = true,
                interactionSource = lastDonationInteraction,
                modifier          = Modifier.fillMaxWidth(),
                leadingIcon       = { Icon(Icons.Default.DateRange, null, tint = RedPrimary) },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (lastDonation.isNotBlank()) {
                            IconButton(onClick = { lastDonation = "" }) {
                                Icon(Icons.Default.Close, "Clear date", tint = TextSecondary)
                            }
                        }
                        IconButton(onClick = { showLastDonationPicker = true }) {
                            Icon(Icons.Default.DateRange, "Pick date", tint = RedPrimary)
                        }
                    }
                },
                singleLine = true,
                shape      = RoundedCornerShape(10.dp),
                colors     = adminFieldColors()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // ── Next Eligible (auto-calculated, read-only) ────────────────────
            AdminFieldLabel("Next Eligible Date ")
            OutlinedTextField(
                value         = nextEligible.ifBlank { "—" },
                onValueChange = {},
                readOnly      = true,
                enabled       = false,
                modifier      = Modifier.fillMaxWidth(),
                leadingIcon   = { Icon(Icons.Default.EventAvailable, null, tint = TextSecondary) },
                singleLine    = true,
                shape         = RoundedCornerShape(10.dp),
                colors        = OutlinedTextFieldDefaults.colors(
                    disabledBorderColor = Color.Gray.copy(alpha = 0.3f),
                    disabledTextColor   = TextSecondary
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // ── Availability status pill ──────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isAvailable) Color(0xFF4CAF50).copy(alpha = 0.10f)
                        else Color(0xFFFF9800).copy(alpha = 0.10f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            if (isAvailable) Color(0xFF4CAF50) else Color(0xFFFF9800),
                            CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isAvailable) "Available to donate now"
                    else "Unavailable until $nextEligible",
                    fontSize   = 12.sp,
                    color      = if (isAvailable) Color(0xFF4CAF50) else Color(0xFFFF9800),
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ── Save Button ──────────────────────────────────────────────────
            Button(
                onClick  = { validateAndSave() },
                enabled  = !uiState.isLoading && !isUnderweight,
                shape    = RoundedCornerShape(10.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor        = if (isUnderweight) Color(0xFFBDBDBD) else RedPrimary,
                    disabledContainerColor = Color(0xFFBDBDBD)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        color       = Color.White,
                        strokeWidth = 2.dp,
                        modifier    = Modifier.size(22.dp)
                    )
                } else {
                    Icon(
                        if (isUnderweight) Icons.Default.Block else Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text       = when {
                            isUnderweight -> "Not Eligible — Cannot Save"
                            isEditMode    -> "Save Changes"
                            else          -> "Add Donor"
                        },
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ── Reusable field helpers ────────────────────────────────────────────────────

@Composable
private fun AdminFieldLabel(text: String, isError: Boolean = false) {
    Text(
        text     = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color    = if (isError) Color(0xFFB71C1C) else TextSecondary,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun adminFieldColors(isError: Boolean = false) = OutlinedTextFieldDefaults.colors(
    focusedBorderColor   = if (isError) Color(0xFFB71C1C) else RedPrimary,
    unfocusedBorderColor = if (isError) Color(0xFFB71C1C).copy(alpha = 0.6f)
    else TextSecondary.copy(alpha = 0.4f),
    focusedLabelColor    = if (isError) Color(0xFFB71C1C) else RedPrimary,
    cursorColor          = RedPrimary,
    errorBorderColor     = Color(0xFFB71C1C)
)