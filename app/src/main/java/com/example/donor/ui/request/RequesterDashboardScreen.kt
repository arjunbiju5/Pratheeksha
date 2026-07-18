package com.example.donor.ui.request

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.donor.data.session.DeviceIdManager
import com.google.firebase.database.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ── Palette ───────────────────────────────────────────────────────────────────
// A calmer, more "product" palette: deep slate ink, restrained accent red used
// sparingly, neutral surfaces with hairline borders instead of heavy fills.

private val PageBg        = Color(0xFFFAFAFB)
private val CardBg        = Color.White
private val CardBorder    = Color(0xFFE7E7EC)

private val PrimaryRed    = Color(0xFFC62828)
private val PrimaryRedDim = Color(0xFFB71C1C)
private val TintRed       = Color(0xFFFDECEC)
private val TintRedBorder = Color(0xFFF3CDCD)

private val GreenText     = Color(0xFF1B7A43)
private val TintGreen     = Color(0xFFEAF6EF)
private val TintGreenBorder = Color(0xFFC9E8D4)

private val AmberText     = Color(0xFFB05A00)
private val TintAmber     = Color(0xFFFFF4E5)
private val TintAmberBorder = Color(0xFFF3DCB0)

private val Ink           = Color(0xFF15161B)
private val InkSoft       = Color(0xFF52545C)
private val Muted         = Color(0xFF8A8C94)
private val Hairline      = Color(0xFFEDEDF1)

// ── Data ──────────────────────────────────────────────────────────────────────

data class MyRequest(
    val id:            String = "",
    val bloodGroup:    String = "",
    val hospital:      String = "",
    val district:      String = "",
    val status:        String = "pending",
    val urgency:       String = "normal",
    val requestDate:   String = "",
    val responseCount: Int    = 0
)

data class RequesterDashboardUiState(
    val requests:  List<MyRequest> = emptyList(),
    val isLoading: Boolean         = true,
    val deviceId:  String          = ""
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

class RequesterDashboardViewModel(
    app: android.app.Application
) : AndroidViewModel(app) {

    private val db = FirebaseDatabase.getInstance().reference
    private val _uiState = MutableStateFlow(RequesterDashboardUiState())
    val uiState: StateFlow<RequesterDashboardUiState> = _uiState
    private var listener: ValueEventListener? = null

    fun load() {
        viewModelScope.launch {
            val deviceId = DeviceIdManager.getOrCreateDeviceId(getApplication())
            _uiState.update { it.copy(deviceId = deviceId) }

            listener?.let { db.child("bloodRequests").removeEventListener(it) }

            val query = db.child("bloodRequests")
                .orderByChild("requestedByUid")
                .equalTo(deviceId)

            val newListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = snapshot.children.map { child ->
                        MyRequest(
                            id = child.key ?: "",
                            bloodGroup = child.child("bloodGroup").getValue(String::class.java)
                                ?: "",
                            hospital = child.child("hospital").getValue(String::class.java) ?: "",
                            district = child.child("district").getValue(String::class.java) ?: "",
                            status = child.child("status").getValue(String::class.java)
                                ?: "pending",
                            urgency = child.child("urgency").getValue(String::class.java)
                                ?: "normal",
                            requestDate = child.child("requestDate").getValue(String::class.java)
                                ?: "",
                            responseCount = child.child("responses").childrenCount.toInt()
                        )
                    }.sortedByDescending { it.requestDate }
                    _uiState.update { it.copy(requests = list, isLoading = false) }
                }

                override fun onCancelled(error: DatabaseError) {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
            listener = newListener
            query.addValueEventListener(newListener)
        }
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun RequesterDashboardScreen(
    onBack:        () -> Unit,
    onOpenRequest: (String) -> Unit,
    onNewRequest:  () -> Unit,
    viewModel: RequesterDashboardViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory
            .getInstance(LocalContext.current.applicationContext as android.app.Application)
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBg)
    ) {

        // ── Top bar ─────────────────────────────────────────────────────────
        Surface(
            color = PrimaryRed,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp, start = 8.dp, end = 16.dp, bottom = 8.dp)
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Spacer(Modifier.width(4.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "My Requests",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = (-0.2).sp
                        )
                        Text(
                            "Track donor responses in real time",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }

                    Button(
                        onClick = onNewRequest,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                    ) {
                        Icon(Icons.Default.Add, null, tint = PrimaryRed, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("New", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = PrimaryRed)
                    }
                }
            }
        }

        // ── Content ───────────────────────────────────────────────────────────
        when {
            uiState.isLoading -> LoadingState()
            uiState.requests.isEmpty() -> EmptyState(onNewRequest)
            else -> {
                val pending   = uiState.requests.filter { it.status != "fulfilled" }
                val fulfilled = uiState.requests.filter { it.status == "fulfilled" }

                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (pending.isNotEmpty()) {
                        item { SectionLabel("Active · ${pending.size}") }
                        items(pending, key = { it.id }) { req ->
                            RequestCard(req = req, onClick = { onOpenRequest(req.id) })
                        }
                    }

                    if (fulfilled.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(6.dp))
                            SectionLabel("Completed · ${fulfilled.size}")
                        }
                        items(fulfilled, key = { it.id }) { req ->
                            RequestCard(req = req, onClick = { onOpenRequest(req.id) })
                        }
                    }
                }
            }
        }
    }
}

// ── Section Label ─────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = Muted,
        letterSpacing = 1.2.sp,
        modifier = Modifier.padding(start = 4.dp, bottom = 6.dp, top = 2.dp)
    )
}

// ── Request Card ──────────────────────────────────────────────────────────────

@Composable
private fun RequestCard(req: MyRequest, onClick: () -> Unit) {
    val isFulfilled = req.status == "fulfilled"
    val isUrgent    = req.urgency == "urgent"

    val accentColor = if (isFulfilled) GreenText else PrimaryRedDim
    val avatarBg     = if (isFulfilled) TintGreen else TintRed

    Surface(
        modifier  = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape     = RoundedCornerShape(16.dp),
        color     = CardBg,
        border    = BorderStroke(1.dp, CardBorder),
        shadowElevation = 0.dp
    ) {
        Column(Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .background(if (isFulfilled) GreenText else PrimaryRed)
            )

            // ── Card top: blood group + info + status ─────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(avatarBg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        req.bloodGroup,
                        fontSize = if (req.bloodGroup.length > 2) 13.sp else 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }

                Spacer(Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        req.hospital,
                        fontSize = 16.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(3.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.LocationOn, null,
                            tint = Muted,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(req.district, fontSize = 13.sp, color = InkSoft)
                    }
                }

                Spacer(Modifier.width(10.dp))

                StatusBadge(isFulfilled = isFulfilled, isUrgent = isUrgent)
            }

            HorizontalDivider(color = Hairline, thickness = 1.dp, modifier = Modifier.padding(horizontal = 18.dp))

            // ── Card bottom: date + donor count ───────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        Icons.Default.CalendarToday, null,
                        tint = Muted,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(req.requestDate, fontSize = 12.5.sp, color = Muted)
                }

                DonorResponseRow(count = req.responseCount, isFulfilled = isFulfilled)
            }
        }
    }
}

// ── Status Badge ──────────────────────────────────────────────────────────────

private data class BadgeStyle(val bg: Color, val border: Color, val fg: Color, val label: String)

@Composable
private fun StatusBadge(isFulfilled: Boolean, isUrgent: Boolean) {
    val style = when {
        isFulfilled -> BadgeStyle(TintGreen, TintGreenBorder, GreenText, "Fulfilled")
        isUrgent    -> BadgeStyle(TintRed,   TintRedBorder,   PrimaryRedDim, "Urgent")
        else        -> BadgeStyle(TintAmber, TintAmberBorder, AmberText, "Pending")
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(style.bg)
            .border(BorderStroke(1.dp, style.border), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(style.label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = style.fg)
    }
}

// ── Donor Response Row ────────────────────────────────────────────────────────

@Composable
private fun DonorResponseRow(count: Int, isFulfilled: Boolean) {
    val hasResponses = count > 0
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        when {
            hasResponses -> {
                repeat(minOf(count, 3)) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(TintGreen)
                            .border(BorderStroke(1.dp, CardBg), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Person, null,
                            tint = GreenText,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
                if (count > 3) {
                    Text("+${count - 3}", fontSize = 10.sp, color = GreenText, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(2.dp))
                Text(
                    if (count == 1) "1 donor ready" else "$count donors ready",
                    fontSize = 12.sp,
                    color = GreenText,
                    fontWeight = FontWeight.SemiBold
                )
            }
            isFulfilled -> {
                Icon(
                    Icons.Default.CheckCircle, null,
                    tint = GreenText,
                    modifier = Modifier.size(13.dp)
                )
                Text("Donation completed", fontSize = 12.sp, color = GreenText, fontWeight = FontWeight.SemiBold)
            }
            else -> {
                Icon(
                    Icons.Default.HourglassTop, null,
                    tint = PrimaryRedDim,
                    modifier = Modifier.size(12.dp)
                )
                Text("Awaiting donors", fontSize = 12.sp, color = PrimaryRedDim)
            }
        }
    }
}

// ── Loading ───────────────────────────────────────────────────────────────────

@Composable
private fun LoadingState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = PrimaryRed,
                strokeWidth = 2.dp,
                modifier = Modifier.size(30.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text("Loading requests…", fontSize = 13.sp, color = Muted)
        }
    }
}

// ── Empty ─────────────────────────────────────────────────────────────────────

@Composable
private fun EmptyState(onNewRequest: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 36.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(TintRed)
                    .border(BorderStroke(1.dp, TintRedBorder), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.WaterDrop, null,
                    tint = PrimaryRed,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(
                "No requests yet",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Ink
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Submit a blood request and track\nwho responds — in real time.",
                fontSize = 13.sp,
                color = Muted,
                textAlign = TextAlign.Center,
                lineHeight = 19.sp
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onNewRequest,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed)
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(8.dp))
                Text("Request Blood", fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}