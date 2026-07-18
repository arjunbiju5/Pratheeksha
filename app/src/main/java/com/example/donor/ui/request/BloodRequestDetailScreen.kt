package com.example.donor.ui.request

import android.content.Intent
import android.net.Uri
import com.example.donor.data.session.DeviceIdManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.donor.data.session.SessionManager
import com.example.donor.service.FcmNotificationSender
import com.example.donor.ui.theme.RedPrimary
import com.example.donor.ui.theme.TextSecondary
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// ── Data ──────────────────────────────────────────────────────────────────────

data class DonorResponse(
    val donorUid:  String = "",
    val donorName: String = "",
    val phone:     String = "",
    val bloodGroup:String = "",
    val city:      String = "",
    val timestamp: String = ""
)

data class RequestDetailUiState(
    val requesterName: String              = "",
    val mobile:        String              = "",
    val bloodGroup:    String              = "",
    val hospital:      String              = "",
    val district:      String              = "",
    val city:          String              = "",
    val urgency:       String              = "normal",
    val note:          String              = "",
    val requestDate:   String              = "",
    val requiredUntil: String              = "",
    val status:        String              = "pending",
    val responses:     List<DonorResponse> = emptyList(),
    val hasAcknowledged: Boolean           = false,
    val isRequester:     Boolean           = false,
    val isLoading:     Boolean             = false,
    val errorMessage:  String?             = null
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

class BloodRequestDetailViewModel(
    private val context: android.app.Application
) : androidx.lifecycle.AndroidViewModel(context) {

    private val db             = FirebaseDatabase.getInstance().reference
    private val sessionManager = SessionManager(context)

    private val _uiState = MutableStateFlow(RequestDetailUiState())
    val uiState: StateFlow<RequestDetailUiState> = _uiState

    private var requestListener: ValueEventListener? = null
    private var currentRequestId: String = ""

    fun loadRequest(requestId: String) {
        currentRequestId = requestId
        requestListener?.let { db.child("bloodRequests").child(requestId).removeEventListener(it) }

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) return

                val responses = mutableListOf<DonorResponse>()
                snapshot.child("responses").children.forEach { child ->
                    responses.add(
                        DonorResponse(
                            donorUid   = child.key ?: "",
                            donorName  = child.child("donorName").getValue(String::class.java)  ?: "",
                            phone      = child.child("phone").getValue(String::class.java)      ?: "",
                            bloodGroup = child.child("bloodGroup").getValue(String::class.java) ?: "",
                            city       = child.child("city").getValue(String::class.java)       ?: "",
                            timestamp  = child.child("timestamp").getValue(String::class.java)  ?: ""
                        )
                    )
                }

                viewModelScope.launch {
                    val myUid             = sessionManager.donorUid.first()
                    val myDeviceId        = DeviceIdManager.getOrCreateDeviceId(context)
                    val requesterDeviceId = snapshot.child("requestedByUid").getValue(String::class.java) ?: ""
                    val requesterDonorUid = snapshot.child("requestedByDonorUid").getValue(String::class.java) ?: ""

                    val isRequester = if (myUid.isNotBlank()) {
                        requesterDonorUid.isNotBlank() && myUid == requesterDonorUid
                    } else {
                        requesterDeviceId.isNotBlank() && myDeviceId == requesterDeviceId
                    }

                    // hasAcknowledged: check both donorUid (logged-in) and deviceId (guest)
                    val hasAcknowledged = responses.any { r ->
                        (myUid.isNotBlank() && r.donorUid == myUid) ||
                                r.donorUid == myDeviceId
                    }

                    _uiState.update { s ->
                        s.copy(
                            requesterName    = snapshot.child("requesterName").getValue(String::class.java) ?: "",
                            mobile           = snapshot.child("mobile").getValue(String::class.java)        ?: "",
                            bloodGroup       = snapshot.child("bloodGroup").getValue(String::class.java)    ?: "",
                            hospital         = snapshot.child("hospital").getValue(String::class.java)      ?: "",
                            district         = snapshot.child("district").getValue(String::class.java)      ?: "",
                            city             = snapshot.child("city").getValue(String::class.java)          ?: "",
                            urgency          = snapshot.child("urgency").getValue(String::class.java)       ?: "normal",
                            note             = snapshot.child("note").getValue(String::class.java)          ?: "",
                            requestDate      = snapshot.child("requestDate").getValue(String::class.java)   ?: "",
                            requiredUntil    = snapshot.child("requiredUntil").getValue(String::class.java) ?: "",
                            status           = snapshot.child("status").getValue(String::class.java)        ?: "pending",
                            responses        = responses,
                            hasAcknowledged  = hasAcknowledged,
                            isRequester      = isRequester
                        )
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                _uiState.update { it.copy(errorMessage = error.message) }
            }
        }

        requestListener = listener
        db.child("bloodRequests").child(requestId).addValueEventListener(listener)
    }

    /**
     * Acknowledge a blood request.
     *
     * - If the user is logged in  → uses their donorUid as the Firebase key
     *   and pulls name/phone/bloodGroup from SessionManager.
     * - If the user is a guest    → uses deviceId as the Firebase key
     *   and uses the name/phone/bloodGroup they typed in the bottom-sheet form.
     */
    fun acknowledge(
        requestId:  String,
        // Guest fields — only used when uid is blank
        guestName:       String = "",
        guestPhone:      String = "",
        guestBloodGroup: String = ""
    ) {
        viewModelScope.launch {
            val uid        = sessionManager.donorUid.first()
            val deviceId   = DeviceIdManager.getOrCreateDeviceId(context)

            // Decide which key to use in Firebase and which profile data to store
            val responseKey: String
            val name:        String
            val phone:       String
            val bloodGroup:  String

            if (uid.isNotBlank()) {
                // Logged-in donor: use their saved profile
                responseKey = uid
                name        = sessionManager.donorName.first()
                phone       = sessionManager.donorPhone.first()
                bloodGroup  = sessionManager.bloodGroup.first()
            } else {
                // Guest: use deviceId as key, guest-supplied info as profile
                responseKey = deviceId
                name        = guestName
                phone       = guestPhone
                bloodGroup  = guestBloodGroup
            }

            _uiState.update { it.copy(isLoading = true) }

            val timestamp = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
                .format(java.util.Date())

            val responseData = mapOf(
                "donorName"  to name,
                "phone"      to phone,
                "bloodGroup" to bloodGroup,
                "timestamp"  to timestamp,
                "isGuest"    to (uid.isBlank())   // handy flag for admin panel
            )

            try {
                db.child("bloodRequests").child(requestId)
                    .child("responses").child(responseKey)
                    .setValue(responseData)
                    .await()

                val snapshot = db.child("bloodRequests").child(requestId)
                    .child("requesterFcmToken")
                    .get()
                    .await()

                val requesterToken = snapshot.getValue(String::class.java) ?: ""

                if (requesterToken.isNotBlank()) {
                    runCatching {
                        FcmNotificationSender.sendToOne(
                            context = getApplication(),
                            token   = requesterToken,
                            title   = "🩸 A donor has responded!",
                            body    = "$name ($bloodGroup) can help. Tap to see their contact.",
                            data    = mapOf(
                                "requestId"  to requestId,
                                "type"       to "donor_acknowledged",
                                "donorName"  to name,
                                "donorPhone" to phone
                            )
                        )
                    }.onFailure { e ->
                        android.util.Log.w("BloodLink", "FCM ack notify failed: ${e.message}")
                    }
                }

                _uiState.update { it.copy(isLoading = false) }

            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.localizedMessage) }
            }
        }
    }

    fun withdrawAcknowledgement(requestId: String) {
        viewModelScope.launch {
            val uid      = sessionManager.donorUid.first()
            val deviceId = DeviceIdManager.getOrCreateDeviceId(context)
            // Use whichever key was used when acknowledging
            val key = if (uid.isNotBlank()) uid else deviceId
            db.child("bloodRequests").child(requestId).child("responses").child(key).removeValue()
        }
    }

    fun setStatus(requestId: String, status: String) {
        viewModelScope.launch {
            try {
                db.child("bloodRequests").child(requestId).child("status").setValue(status).await()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to update status: ${e.localizedMessage}") }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        requestListener?.let {
            db.child("bloodRequests").child(currentRequestId).removeEventListener(it)
        }
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BloodRequestDetailScreen(
    requestId: String,
    isAdmin: Boolean = false,
    onBack: () -> Unit,
    viewModel: BloodRequestDetailViewModel = viewModel(
        factory = androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(
            androidx.compose.ui.platform.LocalContext.current.applicationContext as android.app.Application
        )
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    // Collect isLoggedIn from SessionManager so we know whether to show the sheet
    val sessionManager = remember { SessionManager(context.applicationContext as android.app.Application) }
    val isLoggedIn by sessionManager.isLoggedIn.collectAsState(initial = false)

    LaunchedEffect(requestId) { viewModel.loadRequest(requestId) }

    val isUrgent    = uiState.urgency == "urgent"
    val accentColor = if (isUrgent) Color(0xFFB71C1C) else RedPrimary
    val isFulfilled = uiState.status == "fulfilled"

    // Bottom-sheet state
    val sheetState        = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope             = rememberCoroutineScope()
    var showGuestSheet    by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {

        // ── Header ─────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(accentColor)
                .padding(top = 40.dp, start = 4.dp, end = 16.dp, bottom = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                }
                Column {
                    Text(
                        if (isUrgent) "🚨 Urgent Blood Request" else "🩸 Blood Request",
                        fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White
                    )
                    Text(
                        uiState.requestDate,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // ── Error banner ────────────────────────────────────────────────────
        uiState.errorMessage?.let { msg ->
            Surface(
                color = Color(0xFFFFEBEE),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    msg,
                    color = Color(0xFFC62828),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ── Status chip ────────────────────────────────────────────────
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusChip(
                        label = if (isFulfilled) "✓ Fulfilled" else "Pending",
                        color = if (isFulfilled) Color(0xFF2E8B57) else Color(0xFFFF9800)
                    )
                }
            }

            // ── Requester details card ─────────────────────────────────────
            item {
                DetailCard(title = "Requester Details") {
                    DetailRow(Icons.Default.Person, "Name", uiState.requesterName)
                    DetailRow(Icons.Default.Phone, "Mobile", uiState.mobile)
                    DetailRow(Icons.Default.LocalHospital, "Hospital", uiState.hospital)
                    DetailRow(
                        Icons.Default.LocationOn,
                        "Location",
                        "${uiState.city}, ${uiState.district}"
                    )
                    if (uiState.requiredUntil.isNotBlank()) {
                        DetailRow(Icons.Default.CalendarToday, "Required Until", uiState.requiredUntil)
                    }
                    if (uiState.note.isNotBlank()) {
                        DetailRow(Icons.Default.Notes, "Note", uiState.note)
                    }
                }
            }

            // ── Requester Actions (Mark as Fulfilled) ──────────────────────
            if (uiState.isRequester && !isFulfilled) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Requester Management",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF1976D2)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Did you receive the blood? Marking this as fulfilled will remove it from public lists.",
                                fontSize = 12.sp,
                                color = Color(0xFF1976D2).copy(alpha = 0.8f)
                            )
                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = { viewModel.setStatus(requestId, "fulfilled") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E8B57)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Mark as Fulfilled")
                            }
                        }
                    }
                }
            }

            // ── Acknowledge button (donors only, not fulfilled) ─────────────
            if (!isAdmin && !isFulfilled && !uiState.isRequester) {
                item {
                    if (uiState.hasAcknowledged) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFE8F5E9),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle, null,
                                        tint = Color(0xFF2E8B57), modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            "You've acknowledged this request",
                                            fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                                            color = Color(0xFF2E8B57)
                                        )
                                        Text(
                                            "The requester has your contact details.",
                                            fontSize = 12.sp, color = TextSecondary
                                        )
                                    }
                                }
                            }
                            OutlinedButton(
                                onClick = { viewModel.withdrawAcknowledgement(requestId) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Withdraw", color = TextSecondary)
                            }
                        }
                    } else {
                        Button(
                            onClick = {
                                if (isLoggedIn) {
                                    // Logged-in: acknowledge directly, no sheet needed
                                    viewModel.acknowledge(requestId)
                                } else {
                                    // Guest: open the info-collection sheet
                                    showGuestSheet = true
                                }
                            },
                            enabled = !uiState.isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(22.dp), strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.Favorite, null, tint = Color.White)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "I Can Help — Acknowledge",
                                    fontSize = 15.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // ── Responses ──────────────────────────────────────────────────
            if (uiState.responses.isNotEmpty()) {
                if (isAdmin || uiState.isRequester) {
                    item {
                        Text(
                            "Donors Who Responded (${uiState.responses.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(uiState.responses) { response ->
                        ResponseCard(response) {
                            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${response.phone}"))
                            context.startActivity(dialIntent)
                        }
                    }
                } else {
                    item {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFE8F5E9),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Text(
                                "${uiState.responses.size} donor(s) have offered to help.",
                                fontSize = 13.sp,
                                color = Color(0xFF2E8B57),
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                }
            } else if (isAdmin || uiState.isRequester) {
                item {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF5F5F5),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            "No donors have responded yet.",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    // ── Guest Info Bottom Sheet ────────────────────────────────────────────────
    if (showGuestSheet) {
        GuestAcknowledgeSheet(
            sheetState  = sheetState,
            accentColor = accentColor,
            isLoading   = uiState.isLoading,
            onDismiss   = { showGuestSheet = false },
            onConfirm   = { name, phone, bloodGroup ->
                showGuestSheet = false
                viewModel.acknowledge(
                    requestId        = requestId,
                    guestName        = name,
                    guestPhone       = phone,
                    guestBloodGroup  = bloodGroup
                )
            }
        )
    }
}

// ── Guest Acknowledge Bottom Sheet ────────────────────────────────────────────

private val BLOOD_GROUPS = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GuestAcknowledgeSheet(
    sheetState:  androidx.compose.material3.SheetState,
    accentColor: Color,
    isLoading:   Boolean,
    onDismiss:   () -> Unit,
    onConfirm:   (name: String, phone: String, bloodGroup: String) -> Unit
) {
    var name       by remember { mutableStateOf("") }
    var phone      by remember { mutableStateOf("") }
    var bloodGroup by remember { mutableStateOf("") }
    var bgExpanded by remember { mutableStateOf(false) }

    val nameError  = name.isBlank()
    val phoneError = phone.length < 10
    val bgError    = bloodGroup.isBlank()
    val canSubmit  = !nameError && !phoneError && !bgError

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = Color.White,
        shape            = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            Text(
                "Your Details",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF15161B)
            )
            Text(
                "These will be shared with the requester so they can contact you.",
                fontSize = 13.sp,
                color = TextSecondary,
                lineHeight = 18.sp
            )

            // Name
            OutlinedTextField(
                value         = name,
                onValueChange = { name = it },
                label         = { Text("Your Name") },
                leadingIcon   = { Icon(Icons.Default.Person, null) },
                singleLine    = true,
                isError       = name.isNotBlank() && nameError,
                modifier      = Modifier.fillMaxWidth(),
                shape         = RoundedCornerShape(10.dp)
            )

            // Phone
            OutlinedTextField(
                value         = phone,
                onValueChange = { phone = it.filter { c -> c.isDigit() } },
                label         = { Text("Phone Number") },
                leadingIcon   = { Icon(Icons.Default.Phone, null) },
                singleLine    = true,
                isError       = phone.isNotBlank() && phoneError,
                supportingText = if (phone.isNotBlank() && phoneError)
                { { Text("Enter a valid 10-digit number") } } else null,
                modifier      = Modifier.fillMaxWidth(),
                shape         = RoundedCornerShape(10.dp)
            )

            // Blood Group dropdown
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value         = bloodGroup,
                    onValueChange = {},
                    readOnly      = true,
                    label         = { Text("Blood Group") },
                    trailingIcon  = {
                        Icon(
                            Icons.Default.ArrowDropDown, null,
                            modifier = Modifier.clickable { bgExpanded = true }
                        )
                    },
                    isError  = bloodGroup.isNotEmpty() && bgError,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { bgExpanded = true },
                    shape = RoundedCornerShape(10.dp)
                )
                DropdownMenu(
                    expanded         = bgExpanded,
                    onDismissRequest = { bgExpanded = false }
                ) {
                    BLOOD_GROUPS.forEach { group ->
                        DropdownMenuItem(
                            text    = { Text(group) },
                            onClick = {
                                bloodGroup = group
                                bgExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            Button(
                onClick  = { if (canSubmit) onConfirm(name, phone, bloodGroup) },
                enabled  = canSubmit && !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape  = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color    = Color.White,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Favorite, null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Confirm — I Can Help",
                        fontSize     = 15.sp,
                        color        = Color.White,
                        fontWeight   = FontWeight.SemiBold
                    )
                }
            }

            OutlinedButton(
                onClick  = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(12.dp)
            ) {
                Text("Cancel", color = TextSecondary)
            }
        }
    }
}

// ── Response card ─────────────────────────────────────────────────────────────

@Composable
private fun ResponseCard(response: DonorResponse, onCall: () -> Unit) {
    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .clickable { onCall() },
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(RedPrimary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    response.donorName.firstOrNull()?.uppercase() ?: "D",
                    fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(response.donorName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(
                    "${response.bloodGroup} · ${response.city}",
                    fontSize = 12.sp, color = TextSecondary
                )
                Text(response.timestamp, fontSize = 11.sp, color = TextSecondary)
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFE8F5E9)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Phone, null,
                        tint = Color(0xFF2E8B57), modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        response.phone, fontSize = 12.sp,
                        color = Color(0xFF2E8B57), fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

@Composable
private fun DetailCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                title, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                color = TextSecondary, modifier = Modifier.padding(bottom = 10.dp)
            )
            content()
        }
    }
}

@Composable
private fun DetailRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(icon, null, tint = RedPrimary, modifier = Modifier.size(16.dp).padding(top = 2.dp))
        Spacer(Modifier.width(10.dp))
        Column {
            Text(label, fontSize = 11.sp, color = TextSecondary)
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun StatusChip(label: String, color: Color) {
    Surface(shape = RoundedCornerShape(6.dp), color = color.copy(alpha = 0.12f)) {
        Text(
            label, fontSize = 12.sp, color = color, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}