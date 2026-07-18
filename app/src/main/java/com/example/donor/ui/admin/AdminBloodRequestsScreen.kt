package com.example.donor.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.donor.ui.request.DonorResponse
import com.example.donor.ui.theme.RedPrimary
import com.example.donor.ui.theme.TextSecondary
import com.google.firebase.database.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

// ── Model ─────────────────────────────────────────────────────────────────

data class AdminRequestItem(
    val id:             String = "",
    val requesterName:  String = "",
    val mobile:         String = "",
    val bloodGroup:     String = "",
    val hospital:       String = "",
    val district:       String = "",
    val city:           String = "",
    val urgency:        String = "normal",
    val status:         String = "pending",
    val requestDate:    String = "",
    val responses:      List<DonorResponse> = emptyList()
)

data class AdminRequestsUiState(
    val isLoading: Boolean = true,
    val all:       List<AdminRequestItem> = emptyList(),
    val error:     String? = null
)

// ── ViewModel ─────────────────────────────────────────────────────────────

class AdminBloodRequestsViewModel(app: android.app.Application) : AndroidViewModel(app) {

    private val db = FirebaseDatabase.getInstance().reference
    private val _uiState = MutableStateFlow(AdminRequestsUiState())
    val uiState: StateFlow<AdminRequestsUiState> = _uiState

    private var listener: ValueEventListener? = null

    fun load() {
        listener?.let { db.child("bloodRequests").removeEventListener(it) }

        val newListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val items = snapshot.children.mapNotNull { child ->
                    val responses = child.child("responses").children.map { r ->
                        DonorResponse(
                            donorUid   = r.key ?: "",
                            donorName  = r.child("donorName").getValue(String::class.java) ?: "",
                            phone      = r.child("phone").getValue(String::class.java) ?: "",
                            bloodGroup = r.child("bloodGroup").getValue(String::class.java) ?: "",
                            timestamp  = r.child("timestamp").getValue(String::class.java) ?: ""
                        )
                    }
                    AdminRequestItem(
                        id            = child.key ?: return@mapNotNull null,
                        requesterName = child.child("requesterName").getValue(String::class.java) ?: "",
                        mobile        = child.child("mobile").getValue(String::class.java) ?: "",
                        bloodGroup    = child.child("bloodGroup").getValue(String::class.java) ?: "",
                        hospital      = child.child("hospital").getValue(String::class.java) ?: "",
                        district      = child.child("district").getValue(String::class.java) ?: "",
                        city          = child.child("city").getValue(String::class.java) ?: "",
                        urgency       = child.child("urgency").getValue(String::class.java) ?: "normal",
                        status        = child.child("status").getValue(String::class.java) ?: "pending",
                        requestDate   = child.child("requestDate").getValue(String::class.java) ?: "",
                        responses     = responses
                    )
                }.sortedByDescending { it.requestDate }

                _uiState.update { it.copy(isLoading = false, all = items, error = null) }
            }
            override fun onCancelled(error: DatabaseError) {
                _uiState.update { it.copy(isLoading = false, error = error.message) }
            }
        }
        listener = newListener
        db.child("bloodRequests").addValueEventListener(newListener)
    }

    fun markFulfilled(requestId: String) {
        db.child("bloodRequests").child(requestId).child("status").setValue("fulfilled")
    }

    fun deleteRequest(requestId: String) {
        db.child("bloodRequests").child(requestId).removeValue()
    }

    override fun onCleared() {
        super.onCleared()
        listener?.let { db.child("bloodRequests").removeEventListener(it) }
    }
}

// ── Screen ────────────────────────────────────────────────────────────────

@Composable
fun AdminBloodRequestsScreen(
    onBack: () -> Unit,
    onOpenRequest: (String) -> Unit,
    viewModel: AdminBloodRequestsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    var deleteTarget by remember { mutableStateOf<AdminRequestItem?>(null) }

    LaunchedEffect(Unit) { viewModel.load() }

    val pending   = uiState.all.filter { it.status != "fulfilled" }
    val fulfilled = uiState.all.filter { it.status == "fulfilled" }

    deleteTarget?.let { req ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete Request", fontWeight = FontWeight.Bold) },
            text = { Text("Permanently delete this request from ${req.requesterName.ifBlank { "Unknown" }}? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteRequest(req.id); deleteTarget = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C)),
                    shape = RoundedCornerShape(8.dp)
                ) { Text("Delete", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel", color = RedPrimary) }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    Scaffold(
        topBar = {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(RedPrimary)
                        .statusBarsPadding()
                        .padding(vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                        }
                        Text(
                            "Blood Requests",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.White,
                    contentColor = RedPrimary,
                    indicator = { tabPositions ->
                        SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = RedPrimary
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Pending (${pending.size})", fontWeight = FontWeight.SemiBold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Fulfilled (${fulfilled.size})", fontWeight = FontWeight.SemiBold) }
                    )
                }
            }
        },
        containerColor = Color(0xFFF4F4F6)
    ) { padding ->
        val list = if (selectedTab == 0) pending else fulfilled

        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                uiState.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = RedPrimary)
                }
                uiState.error != null -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text(uiState.error ?: "Error", color = TextSecondary)
                }
                list.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Inbox, null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            if (selectedTab == 0) "No pending requests" else "No fulfilled requests yet",
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(list, key = { it.id }) { req ->
                        AdminRequestCard(
                            req = req,
                            onClick = { onOpenRequest(req.id) },
                            onMarkFulfilled = { viewModel.markFulfilled(req.id) },
                            onDelete = { deleteTarget = req }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminRequestCard(
    req: AdminRequestItem,
    onClick: () -> Unit,
    onMarkFulfilled: () -> Unit,
    onDelete: () -> Unit
) {
    val isFulfilled = req.status == "fulfilled"
    val accent = if (req.urgency == "urgent") Color(0xFFB71C1C) else RedPrimary

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Blood Group Badge
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = accent.copy(alpha = 0.1f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            req.bloodGroup,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = accent
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        req.hospital,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A2E),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "${req.city}, ${req.district}",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!isFulfilled) {
                        IconButton(onClick = onMarkFulfilled) {
                            Icon(Icons.Default.CheckCircle, "Fulfilled", tint = Color(0xFF2E8B57))
                        }
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, "Delete", tint = Color(0xFFB71C1C))
                    }
                }
            }

            if (req.urgency == "urgent") {
                Spacer(Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFFFEBEE)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, null, tint = Color(0xFFB71C1C), modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("URGENT", color = Color(0xFFB71C1C), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFE8E8EC), thickness = 1.dp)
            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        req.requesterName.ifBlank { "Unknown Requester" },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1A1A2E)
                    )
                    Text(req.mobile, fontSize = 12.sp, color = TextSecondary)
                }
                Text(req.requestDate, fontSize = 11.sp, color = TextSecondary)
            }

            if (req.responses.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFF1F8F1)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Favorite, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(14.dp))
                        Text(
                            "${req.responses.size} donor(s) responded",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
            }
        }
    }
}