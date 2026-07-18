package com.example.donor.ui.search

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.donor.data.model.Donor
import com.example.donor.ui.theme.RedPrimary
import com.example.donor.ui.theme.TextSecondary

// Local accent tokens — mirrors the palette used on the dashboard so the two
// screens read as one product. Kept local so no edits to Theme.kt are needed.
private val MaroonDeep   = Color(0xFF6E1423)
private val PinkTint     = Color(0xFFFCE9EC)
private val SuccessGreen = Color(0xFF2E8B57)
private val SuccessTint  = Color(0xFFE3F3EA)
private val AmberWarn    = Color(0xFFC9700A)
private val AmberTint    = Color(0xFFFCF1E3)
private val Ink          = Color(0xFF231417)
private val Cloud        = Color(0xFFF7F2F1)

// District/City Map and Blood Groups
val districtCityMap = mapOf(
    "Thiruvananthapuram" to listOf("Thiruvananthapuram", "Attingal", "Neyyattinkara", "Varkala", "Nedumangad", "Kattakada", "Kazhakkoottam", "Kovalam", "Parassala", "Balaramapuram", "Pothencode", "Kilimanoor"),
    "Kollam" to listOf("Kollam", "Karunagappally", "Kottarakkara", "Punalur", "Pathanapuram", "Chadayamangalam", "Chathannoor", "Chavara", "Kundara", "Mynagappally", "Neduvathoor", "Anchal", "Paravur", "Sasthamcotta", "Eravipuram"),
    "Pathanamthitta" to listOf("Pathanamthitta", "Adoor", "Pandalam", "Konni", "Ranni", "Thiruvalla", "Kozhencherry", "Mallappally", "Pullad", "Aranmula", "Elanthoor"),
    "Alappuzha" to listOf("Alleppey", "Ambalapuzha", "Cherthala", "Chengannur", "Haripad", "Kayamkulam", "Kuttanad", "Mavelikara", "Mannar", "Thakazhi", "Aroor", "Thuravoor"),
    "Kottayam" to listOf("Kottayam", "Changanassery", "Pala", "Vaikom", "Ettumanoor", "Erattupetta", "Kanjirappally", "Ponkunnam", "Kuravilangad", "Kaduthuruthy"),
    "Idukki" to listOf("Idukki", "Thodupuzha", "Devikulam", "Peerumade", "Udumbanchola", "Munnar", "Adimali", "Kattappana", "Nedumkandam", "Kumily", "Vandiperiyar", "Cheruthoni"),
    "Ernakulam" to listOf("Ernakulam", "Kochi", "Mattancherry", "Palluruthy", "Tripunithura", "Piravom", "Perumbavoor", "Muvattupuzha", "Kothamangalam", "Aluva", "Angamaly", "North Paravur", "Narakkal", "Kakkanad", "Kalady", "Fort Kochi"),
    "Thrissur" to listOf("Thrissur", "Chalakudy", "Kodungallur", "Irinjalakuda", "Kunnamkulam", "Chavakkad", "Guruvayur", "Wadakkanchery", "Puthukkad", "Mala", "Ollur", "Pazhayannur"),
    "Palakkad" to listOf("Palakkad", "Ottapalam", "Shoranur", "Pattambi", "Mannarkkad", "Cherpulassery", "Alathur", "Chittur", "Nenmara", "Koduvayur", "Kongad", "Parali", "Thrithala", "Kuzhalmannam", "Vadakkenchery"),
    "Malappuram" to listOf("Malappuram", "Manjeri", "Perinthalmanna", "Nilambur", "Tirur", "Tirurangadi", "Ponnani", "Kondotty", "Kottakkal", "Valanchery", "Parappanangadi", "Tanur", "Edappal", "Areekode", "Wandoor"),
    "Kozhikode" to listOf("Kozhikode", "Vadakara", "Koyilandy", "Mukkam", "Feroke", "Ramanattukara", "Payyoli", "Balussery", "Nadapuram", "Thamarassery", "Perambra"),
    "Wayanad" to listOf("Kalpetta", "Mananthavady", "Sulthan Bathery", "Vythiri", "Meppadi", "Panamaram", "Pulpally", "Ambalavayal"),
    "Kannur" to listOf("Kannur", "Thalassery", "Payyannur", "Taliparamba", "Mattannur", "Iritty", "Kuthuparamba", "Peravoor", "Peringalam", "Edakkad", "Irikkur", "Dharmadam"),
    "Kasaragod" to listOf("Kasaragod", "Kanhangad", "Nileshwar", "Bekal", "Cheruvathur", "Uppala", "Manjeshwaram", "Vellarikundu", "Badiadka", "Trikaripur")
)
val allBloodGroups = listOf("All", "A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-", "A1+", "A1-", "A2+", "A2-", "A1B+", "A1B-", "A2B+", "A2B-", "Bombay Blood Group")

private val rareGroups = listOf("AB-", "O-", "B-", "Bombay Blood Group")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FindDonorScreen(onBack: () -> Unit, viewModel: FindDonorViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var selectedBloodGroup by remember { mutableStateOf("All") }
    var selectedDistrict by remember { mutableStateOf("All") }
    var selectedCity by remember { mutableStateOf("All") }

    LaunchedEffect(selectedDistrict) { selectedCity = "All" }
    val availableCities = listOf("All") + (districtCityMap[selectedDistrict] ?: emptyList())

    // ── Scroll-aware filter card ──
    // Scrolling down hides the filter card to give results more room;
    // scrolling back up (or being at the very top) brings it back.
    val listState = rememberLazyListState()
    var filterVisible by remember { mutableStateOf(true) }
    var lastScrollIndex by remember { mutableStateOf(0) }
    var lastScrollOffset by remember { mutableStateOf(0) }

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect { (index, offset) ->
                val scrollingDown = index > lastScrollIndex ||
                        (index == lastScrollIndex && offset > lastScrollOffset)
                filterVisible = when {
                    index == 0 && offset < 10 -> true
                    scrollingDown             -> false
                    else                      -> true
                }
                lastScrollIndex  = index
                lastScrollOffset = offset
            }
    }

    Column(modifier = Modifier.fillMaxSize().background(Cloud)) {

        // ── Header ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                .background(Brush.verticalGradient(listOf(RedPrimary, MaroonDeep)))
                .padding(vertical = 18.dp)
        ) {
            IconButton(
                onClick  = onBack,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 8.dp)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(
                text       = "Find a donor",
                color      = Color.White,
                fontSize   = 19.sp,
                fontWeight = FontWeight.Bold,
                modifier   = Modifier.align(Alignment.Center)
            )
        }

        // ── Filter card (collapses on scroll-down) ──
        AnimatedVisibility(
            visible = filterVisible,
            enter   = expandVertically() + fadeIn(),
            exit    = shrinkVertically() + fadeOut()
        ) {
            Card(
                modifier  = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                shape     = RoundedCornerShape(18.dp),
                colors    = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    FilterDropdown(Icons.Default.Favorite, "Blood group", selectedBloodGroup, allBloodGroups) { selectedBloodGroup = it }
                    Spacer(modifier = Modifier.height(14.dp))
                    FilterDropdown(Icons.Default.LocationOn, "District", selectedDistrict, listOf("All") + districtCityMap.keys.toList()) { selectedDistrict = it }
                    Spacer(modifier = Modifier.height(14.dp))
                    FilterDropdown(Icons.Default.LocationOn, "City", selectedCity, availableCities) { selectedCity = it }
                    Spacer(modifier = Modifier.height(18.dp))
                    Button(
                        onClick  = { viewModel.searchDonors(selectedBloodGroup, selectedDistrict, selectedCity, false) },
                        enabled  = !uiState.isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape  = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier    = Modifier.size(20.dp),
                                color       = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Search donors", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // ── Error banner ──
        uiState.errorMessage?.let { message ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 8.dp),
                shape = RoundedCornerShape(12.dp),
                color = AmberTint
            ) {
                Row(
                    modifier          = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = AmberWarn, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(message, fontSize = 13.sp, color = AmberWarn, modifier = Modifier.weight(1f))
                    IconButton(onClick = { viewModel.clearError() }, modifier = Modifier.size(20.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = AmberWarn, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // ── Results ──
        when {
            !uiState.searched -> EmptyState(
                icon    = Icons.Default.Search,
                title   = "Search for a donor",
                message = "Choose a blood group and location, then tap Search donors to see who's nearby."
            )
            uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = RedPrimary)
            }
            uiState.donors.isEmpty() -> EmptyState(
                icon    = Icons.Default.Info,
                title   = "No donors found",
                message = "Try a wider area or a different blood group."
            )
            else -> Column {
                Text(
                    text     = "${uiState.donors.size} donor${if (uiState.donors.size == 1) "" else "s"} found",
                    fontSize = 13.sp,
                    color    = TextSecondary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
                LazyColumn(
                    state                 = listState,
                    contentPadding        = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement   = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.donors) { donor ->
                        DonorCard(donor) {
                            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${donor.mobile}")))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, message: String) {
    Column(
        modifier             = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment  = Alignment.CenterHorizontally,
        verticalArrangement  = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(PinkTint),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = RedPrimary, modifier = Modifier.size(28.dp))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink)
        Spacer(modifier = Modifier.height(6.dp))
        Text(message, fontSize = 13.sp, color = TextSecondary, textAlign = TextAlign.Center)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterDropdown(icon: ImageVector, label: String, selected: String, options: List<String>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(label, fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
        }
        Spacer(modifier = Modifier.height(6.dp))
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            OutlinedTextField(
                value          = selected,
                onValueChange  = {},
                readOnly       = true,
                modifier       = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                shape          = RoundedCornerShape(12.dp),
                trailingIcon   = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                colors         = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = RedPrimary,
                    unfocusedBorderColor = Color(0xFFE0DADA)
                )
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(text = { Text(option) }, onClick = { onSelect(option); expanded = false })
                }
            }
        }
    }
}

@Composable
fun DonorCard(donor: Donor, onCall: () -> Unit) {
    val isRare = donor.bloodGroup in rareGroups
    val isAvailable = donor.isCurrentlyAvailable()

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(18.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (isRare) MaroonDeep else RedPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text       = donor.fullName.firstOrNull()?.uppercase() ?: "D",
                        color      = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 18.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(donor.fullName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Ink)
                    Text(
                        text     = "${donor.bloodGroup} · ${donor.city}, ${donor.district}",
                        fontSize = 12.sp,
                        color    = TextSecondary
                    )
                }
                IconButton(
                    onClick  = onCall,
                    modifier = Modifier
                        .size(42.dp)
                        .background(SuccessTint, CircleShape)
                ) {
                    Icon(Icons.Default.Phone, contentDescription = "Call ${donor.fullName}", tint = SuccessGreen)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isAvailable) SuccessTint else AmberTint
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier          = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (isAvailable) SuccessGreen else AmberWarn)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text       = if (isAvailable) "Available" else "Unavailable until ${donor.nextEligible}",
                        fontSize   = 11.sp,
                        color      = if (isAvailable) SuccessGreen else AmberWarn,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}