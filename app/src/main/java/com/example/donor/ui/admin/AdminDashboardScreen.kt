package com.example.donor.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.verticalScroll
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.donor.data.model.Donor
import com.example.donor.ui.theme.RedPrimary
import com.example.donor.ui.theme.TextSecondary
import kotlin.Boolean
import kotlin.OptIn
import kotlin.String
import kotlin.collections.listOf
import kotlin.collections.mapOf

// Local palette tokens — kept here to avoid touching Theme.kt
private val SuccessGreen = Color(0xFF2E8B57)
private val WarningAmber = Color(0xFFFF9800)
private val DangerRed    = Color(0xFFB71C1C)
private val SurfaceGray  = Color(0xFFF7F2F1)
private val CardBorder   = Color(0xFFEEEEEE)

@Composable
fun AdminDashboardScreen(
    onLogout: () -> Unit,
    onEditDonor: (Donor) -> Unit,
    onAddDonor: () -> Unit,
    onViewRequests: () -> Unit,
    onManageContacts: () -> Unit,
    viewModel: AdminViewModel = viewModel()
) {
    val uiState by viewModel.adminUiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showDeleteConfirm by remember { mutableStateOf<Donor?>(null) }

    val listState = rememberLazyListState()
    var previousIndex  by remember { mutableStateOf(0) }
    var previousOffset by remember { mutableStateOf(0) }
    var searchBarVisible by remember { mutableStateOf(true) }

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect { (index, offset) ->
                val scrolledDown = index > previousIndex ||
                        (index == previousIndex && offset > previousOffset)
                val scrolledUp = index < previousIndex ||
                        (index == previousIndex && offset < previousOffset)
                if (scrolledDown && (index > 0 || offset > 24)) searchBarVisible = false
                else if (scrolledUp) searchBarVisible = true
                previousIndex  = index
                previousOffset = offset
            }
    }

    LaunchedEffect(Unit) { viewModel.loadAllDonors() }

    val filteredDonors = uiState.allDonors.filter { donor ->
        donor.fullName.contains(searchQuery,   ignoreCase = true) ||
                donor.bloodGroup.contains(searchQuery, ignoreCase = true) ||
                donor.mobile.contains(searchQuery,     ignoreCase = true) ||
                donor.city.contains(searchQuery,       ignoreCase = true)
    }

    // ── Delete confirmation dialog ──────────────────────────────────────────
    showDeleteConfirm?.let { donor ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = {
                Text("Delete Donor Profile", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            },
            text = {
                Text(
                    "This will permanently remove ${donor.fullName}'s profile and all " +
                            "associated data. This action cannot be undone.",
                    fontSize   = 14.sp,
                    lineHeight = 20.sp,
                    color      = Color(0xFF424242)
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteDonor(donor.uid); showDeleteConfirm = null },
                    colors  = ButtonDefaults.buttonColors(containerColor = DangerRed),
                    shape   = RoundedCornerShape(8.dp)
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirm = null },
                    shape   = RoundedCornerShape(8.dp)
                ) {
                    Text("Cancel", color = RedPrimary, fontWeight = FontWeight.SemiBold)
                }
            },
            shape = RoundedCornerShape(12.dp)
        )
    }

    Scaffold(
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                ExtendedFloatingActionButton(
                    onClick        = onManageContacts,
                    containerColor = Color.White,
                    contentColor   = RedPrimary,
                    icon           = { Icon(Icons.Default.ContactPhone, contentDescription = null) },
                    text           = { Text("Manage Contacts", fontWeight = FontWeight.SemiBold) }
                )
                Spacer(modifier = Modifier.height(12.dp))
                ExtendedFloatingActionButton(
                    onClick        = onViewRequests,
                    containerColor = Color.White,
                    contentColor   = RedPrimary,
                    icon           = { Icon(Icons.Default.Bloodtype, contentDescription = null) },
                    text           = { Text("Blood Requests", fontWeight = FontWeight.SemiBold) }
                )
                Spacer(modifier = Modifier.height(12.dp))
                ExtendedFloatingActionButton(
                    onClick        = onAddDonor,
                    containerColor = RedPrimary,
                    contentColor   = Color.White,
                    icon           = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                    text           = { Text("Add Donor", fontWeight = FontWeight.SemiBold) }
                )
            }
        }
    ) { scaffoldPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(scaffoldPadding)
        ) {

            // ── Header ───────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(RedPrimary)
                    .padding(16.dp)
            ) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Donor Management",
                            fontSize   = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color      = Color.White
                        )
                        if (uiState.allDonors.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "${uiState.allDonors.size} donors registered",
                                fontSize   = 13.sp,
                                color      = Color.White.copy(alpha = 0.85f),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    IconButton(
                        onClick  = onLogout,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.Logout,
                            contentDescription = "Logout",
                            tint               = Color.White,
                            modifier           = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // ── Collapsible search bar ────────────────────────────────────────
            AnimatedVisibility(
                visible = searchBarVisible,
                enter   = expandVertically() + fadeIn(),
                exit    = shrinkVertically() + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFAFAFA))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    OutlinedTextField(
                        value         = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier      = Modifier.fillMaxWidth(),
                        placeholder   = {
                            Text(
                                "Search by name, blood group, mobile or city",
                                fontSize = 13.sp,
                                color    = Color.Gray
                            )
                        },
                        leadingIcon  = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint     = RedPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick  = { searchQuery = "" },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Clear,
                                        contentDescription = null,
                                        tint     = Color.Gray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        shape      = RoundedCornerShape(10.dp),
                        singleLine = true,
                        colors     = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = RedPrimary,
                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.2f),
                            cursorColor          = RedPrimary
                        )
                    )
                }
            }

            // ── Content states ────────────────────────────────────────────────
            when {
                uiState.isLoading && uiState.allDonors.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color       = RedPrimary,
                                modifier    = Modifier.size(48.dp),
                                strokeWidth = 4.dp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Loading donors",
                                fontSize   = 15.sp,
                                color      = TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                uiState.errorMessage != null -> {
                    Box(
                        Modifier.fillMaxSize().padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(12.dp))
                                .padding(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint     = RedPrimary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Error Loading Data",
                                fontWeight = FontWeight.SemiBold,
                                fontSize   = 16.sp,
                                color      = Color(0xFF424242)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                uiState.errorMessage ?: "Unknown error",
                                color     = TextSecondary,
                                fontSize  = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                uiState.allDonors.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier            = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                Icons.Default.GroupOff,
                                contentDescription = null,
                                tint     = Color.Gray,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "No Donors Yet",
                                fontSize   = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color      = Color(0xFF1C1C1C)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Tap \"Add Donor\" below to register your first donor",
                                fontSize  = 13.sp,
                                color     = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                filteredDonors.isEmpty() && searchQuery.isNotEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier            = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                Icons.Default.SearchOff,
                                contentDescription = null,
                                tint     = Color.Gray,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "No Results Found",
                                fontSize   = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color      = Color(0xFF1C1C1C)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Try adjusting your search criteria",
                                fontSize = 13.sp,
                                color    = TextSecondary
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        state               = listState,
                        contentPadding      = PaddingValues(
                            start  = 12.dp, end = 12.dp,
                            top    = 12.dp, bottom = 88.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier            = Modifier.fillMaxWidth()
                    ) {
                        items(filteredDonors, key = { it.uid }) { donor ->
                            AdminDonorCard(
                                donor    = donor,
                                onEdit   = { onEditDonor(donor) },
                                onDelete = { showDeleteConfirm = donor }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Donor Card ────────────────────────────────────────────────────────────────

@Composable
fun AdminDonorCard(
    donor: Donor,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isAvailable  = donor.isCurrentlyAvailable()
    val borderColor  = if (isAvailable) SuccessGreen else WarningAmber
    val statusColor  = if (isAvailable) SuccessGreen else WarningAmber
    val statusText   = if (isAvailable) "Available" else "Unavailable"
    val isUnderweight = donor.weight == "<50 kg"

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border    = BorderStroke(2.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .background(Color.White)
                .padding(14.dp)
        ) {

            // ── Row 1: Avatar + Name + Action Buttons ─────────────────────────
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar circle
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(RedPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text       = donor.fullName.firstOrNull()?.uppercase() ?: "D",
                        fontSize   = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color      = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text       = donor.fullName,
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = Color(0xFF1C1C1C),
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text       = donor.city,
                        fontSize   = 12.sp,
                        color      = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Edit + Delete buttons
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    IconButton(
                        onClick  = onEdit,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(RedPrimary.copy(alpha = 0.10f))
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit ${donor.fullName}",
                            tint     = RedPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick  = onDelete,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(DangerRed.copy(alpha = 0.10f))
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete ${donor.fullName}",
                            tint     = DangerRed,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ── Row 2: Blood group chip + Status chip + Weight chip ───────────
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Blood group
                Surface(
                    shape  = RoundedCornerShape(6.dp),
                    color  = Color.White,
                    border = BorderStroke(1.dp, RedPrimary.copy(alpha = 0.35f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier          = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = null,
                            tint     = RedPrimary,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text       = donor.bloodGroup,
                            fontSize   = 12.sp,
                            color      = RedPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Availability status
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = statusColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text       = statusText,
                        fontSize   = 11.sp,
                        color      = statusColor,
                        fontWeight = FontWeight.Bold,
                        modifier   = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Weight chip — red-tinted if underweight
                if (donor.weight.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isUnderweight)
                            DangerRed.copy(alpha = 0.10f)
                        else
                            SurfaceGray
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier          = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                Icons.Default.MonitorWeight,
                                contentDescription = null,
                                tint     = if (isUnderweight) DangerRed else TextSecondary,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text       = donor.weight,
                                fontSize   = 11.sp,
                                color      = if (isUnderweight) DangerRed else TextSecondary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Underweight warning — compact inline banner, not a full card
            if (isUnderweight) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFFEBEE), RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint     = DangerRed,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text       = "Below minimum weight — not eligible to donate",
                        fontSize   = 11.sp,
                        color      = DangerRed,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = CardBorder, thickness = 1.dp)
            Spacer(modifier = Modifier.height(10.dp))

            // ── Row 3: Detail grid — mobile, DOB, last donated, next eligible ─
            // Split into two rows of two so values don't crowd on narrow screens.
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                CardDetailItem(
                    icon  = Icons.Default.Phone,
                    label = "Mobile",
                    value = donor.mobile.ifBlank { "—" },
                    modifier = Modifier.weight(1f)
                )
                CardDetailItem(
                    icon  = Icons.Default.Cake,
                    label = "Date of Birth",
                    value = donor.dateOfBirth.ifBlank { "—" },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                CardDetailItem(
                    icon  = Icons.Default.DateRange,
                    label = "Last Donated",
                    value = donor.lastDonation.ifBlank { "Never" },
                    modifier = Modifier.weight(1f)
                )
                CardDetailItem(
                    icon  = Icons.Default.EventAvailable,
                    label = "Next Eligible",
                    value = donor.nextEligible.ifBlank { "Now" },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// ── Reusable detail cell ───────────────────────────────────────────────────────

@Composable
fun CardDetailItem(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier          = modifier,
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint     = TextSecondary,
            modifier = Modifier
                .size(13.dp)
                .padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Column {
            Text(
                text       = label,
                fontSize   = 11.sp,
                color      = TextSecondary,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text       = value,
                fontSize   = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color      = Color(0xFF1C1C1C),
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis
            )
        }
    }
}