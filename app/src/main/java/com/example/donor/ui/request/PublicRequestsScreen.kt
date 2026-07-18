package com.example.donor.ui.request

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.donor.data.session.DeviceIdManager
import com.example.donor.data.session.SessionManager
import com.example.donor.service.FcmNotificationSender
import com.example.donor.ui.theme.RedPrimary
import com.example.donor.ui.theme.TextSecondary
import com.google.firebase.database.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

// ── Data ──────────────────────────────────────────────────────────────────────

data class PublicRequestItem(
    val id:             String = "",
    val requesterName:  String = "",
    val mobile:         String = "",
    val bloodGroup:     String = "",
    val hospital:       String = "",
    val district:       String = "",
    val city:           String = "",
    val urgency:        String = "normal",
    val requestDate:    String = "",
    val requiredUntil:  String = "",
    val responderCount: Int    = 0,
    val requesterFcmToken: String = ""
)

data class PublicRequestsUiState(
    val isLoading:        Boolean               = true,
    val requests:         List<PublicRequestItem> = emptyList(),
    val error:            String?               = null,
    // Which request is currently being acknowledged (shows loading on that card)
    val acknowledgingId:  String?               = null,
    // Which requestIds this device/user has already acknowledged
    val acknowledgedIds:  Set<String>           = emptySet()
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

class PublicRequestsViewModel(app: android.app.Application) : AndroidViewModel(app) {

    private val db             = FirebaseDatabase.getInstance().reference
    private val sessionManager = SessionManager(app)
    private val _uiState       = MutableStateFlow(PublicRequestsUiState())
    val uiState: StateFlow<PublicRequestsUiState> = _uiState

    private var listener: ValueEventListener? = null

    fun load() {
        listener?.let { db.child("bloodRequests").removeEventListener(it) }

        val query = db.child("bloodRequests")
            .orderByChild("status")
            .equalTo("pending")

        val newListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                viewModelScope.launch {
                    val sdf       = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    val today     = Date()
                    val myUid     = sessionManager.donorUid.first()
                    val deviceId  = DeviceIdManager.getOrCreateDeviceId(getApplication())

                    val acknowledgedIds = mutableSetOf<String>()

                    val items = snapshot.children.mapNotNull { child ->
                        val requiredUntilStr = child.child("requiredUntil")
                            .getValue(String::class.java) ?: ""

                        // Filter expired
                        if (requiredUntilStr.isNotBlank()) {
                            val expiry = runCatching { sdf.parse(requiredUntilStr) }.getOrNull()
                            if (expiry != null && expiry.before(sdf.parse(sdf.format(today)))) {
                                return@mapNotNull null
                            }
                        }

                        val requestId = child.key ?: return@mapNotNull null

                        // Check if this user already acknowledged
                        val responses = child.child("responses")
                        val alreadyAcked = responses.children.any { r ->
                            val key = r.key ?: ""
                            (myUid.isNotBlank() && key == myUid) || key == deviceId
                        }
                        if (alreadyAcked) acknowledgedIds.add(requestId)

                        PublicRequestItem(
                            id              = requestId,
                            requesterName   = child.child("requesterName").getValue(String::class.java) ?: "",
                            mobile          = child.child("mobile").getValue(String::class.java) ?: "",
                            bloodGroup      = child.child("bloodGroup").getValue(String::class.java) ?: "",
                            hospital        = child.child("hospital").getValue(String::class.java) ?: "",
                            district        = child.child("district").getValue(String::class.java) ?: "",
                            city            = child.child("city").getValue(String::class.java) ?: "",
                            urgency         = child.child("urgency").getValue(String::class.java) ?: "normal",
                            requestDate     = child.child("requestDate").getValue(String::class.java) ?: "",
                            requiredUntil   = requiredUntilStr,
                            responderCount  = child.child("responses").childrenCount.toInt(),
                            requesterFcmToken = child.child("requesterFcmToken").getValue(String::class.java) ?: ""
                        )
                    }
                        .sortedWith(
                            compareByDescending<PublicRequestItem> { it.urgency == "urgent" }
                                .thenByDescending { it.requestDate }
                        )

                    _uiState.update {
                        it.copy(
                            isLoading       = false,
                            requests        = items,
                            error           = null,
                            acknowledgedIds = acknowledgedIds
                        )
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                _uiState.update { it.copy(isLoading = false, error = error.message) }
            }
        }
        listener = newListener
        query.addValueEventListener(newListener)
    }

    /**
     * Acknowledge directly from the list.
     * For logged-in donors: no extra info needed.
     * For guests: caller passes name/phone/bloodGroup from the bottom sheet.
     */
    fun acknowledge(
        requestId:       String,
        requesterToken:  String,
        guestName:       String = "",
        guestPhone:      String = "",
        guestBloodGroup: String = ""
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(acknowledgingId = requestId) }

            val uid      = sessionManager.donorUid.first()
            val deviceId = DeviceIdManager.getOrCreateDeviceId(getApplication())

            val responseKey: String
            val name:        String
            val phone:       String
            val bloodGroup:  String

            if (uid.isNotBlank()) {
                responseKey = uid
                name        = sessionManager.donorName.first()
                phone       = sessionManager.donorPhone.first()
                bloodGroup  = sessionManager.bloodGroup.first()
            } else {
                responseKey = deviceId
                name        = guestName
                phone       = guestPhone
                bloodGroup  = guestBloodGroup
            }

            val timestamp = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())

            val responseData = mapOf(
                "donorName"  to name,
                "phone"      to phone,
                "bloodGroup" to bloodGroup,
                "timestamp"  to timestamp,
                "isGuest"    to (uid.isBlank())
            )

            try {
                db.child("bloodRequests").child(requestId)
                    .child("responses").child(responseKey)
                    .setValue(responseData)
                    .await()

                // Notify requester
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
                    }
                }

                _uiState.update {
                    it.copy(
                        acknowledgingId = null,
                        acknowledgedIds = it.acknowledgedIds + requestId
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(acknowledgingId = null, error = e.localizedMessage) }
            }
        }
    }

    fun withdrawAcknowledgement(requestId: String) {
        viewModelScope.launch {
            val uid      = sessionManager.donorUid.first()
            val deviceId = DeviceIdManager.getOrCreateDeviceId(getApplication())
            val key      = if (uid.isNotBlank()) uid else deviceId
            db.child("bloodRequests").child(requestId).child("responses").child(key).removeValue()
            _uiState.update { it.copy(acknowledgedIds = it.acknowledgedIds - requestId) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        listener?.let { db.child("bloodRequests").removeEventListener(it) }
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

private val BLOOD_GROUPS_LIST = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicRequestsScreen(
    onBack:        () -> Unit,
    onOpenRequest: (String) -> Unit,
    viewModel: PublicRequestsViewModel = viewModel(
        factory = androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory
            .getInstance(LocalContext.current.applicationContext as android.app.Application)
    )
) {
    val uiState    by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.load()
    }

    val context    = LocalContext.current
    val sessionMgr = remember { SessionManager(context.applicationContext as android.app.Application) }
    val isLoggedIn by sessionMgr.isLoggedIn.collectAsState(initial = false)

    // Guest sheet state
    val sheetState              = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showGuestSheet          by remember { mutableStateOf(false) }
    var pendingAckRequest       by remember { mutableStateOf<PublicRequestItem?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        // ── Header ──────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(RedPrimary)
                .padding(top = 40.dp, start = 4.dp, end = 16.dp, bottom = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                }
                Column {
                    Text(
                        "Active Blood Requests",
                        fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White
                    )
                    Text(
                        "Tap a card to see details · Donate directly from here",
                        fontSize = 12.sp, color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        }

        // ── Error banner ────────────────────────────────────────────────────
        uiState.error?.let { err ->
            Surface(color = Color(0xFFFFEBEE), modifier = Modifier.fillMaxWidth()) {
                Text(
                    err, color = Color(0xFFC62828), fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }

        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator(color = RedPrimary)
            }

            uiState.requests.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.CheckCircle, null,
                        tint = Color(0xFF2E8B57), modifier = Modifier.size(56.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("No active requests right now", color = TextSecondary)
                }
            }

            else -> LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.requests, key = { it.id }) { req ->
                    val isAcknowledged  = req.id in uiState.acknowledgedIds
                    val isAcknowledging = uiState.acknowledgingId == req.id

                    PublicRequestCard(
                        req             = req,
                        isAcknowledged  = isAcknowledged,
                        isAcknowledging = isAcknowledging,
                        onClick         = { onOpenRequest(req.id) },
                        onDonate        = {
                            if (isAcknowledged) {
                                viewModel.withdrawAcknowledgement(req.id)
                            } else if (isLoggedIn) {
                                viewModel.acknowledge(req.id, req.requesterFcmToken)
                            } else {
                                pendingAckRequest = req
                                showGuestSheet    = true
                            }
                        }
                    )
                }
            }
        }
    }

    // ── Guest info bottom sheet ──────────────────────────────────────────────
    if (showGuestSheet && pendingAckRequest != null) {
        val req = pendingAckRequest!!
        GuestDonateSheet(
            sheetState  = sheetState,
            accentColor = if (req.urgency == "urgent") Color(0xFFB71C1C) else RedPrimary,
            isLoading   = uiState.acknowledgingId == req.id,
            onDismiss   = {
                showGuestSheet    = false
                pendingAckRequest = null
            },
            onConfirm   = { name, phone, bloodGroup ->
                showGuestSheet = false
                viewModel.acknowledge(
                    requestId        = req.id,
                    requesterToken   = req.requesterFcmToken,
                    guestName        = name,
                    guestPhone       = phone,
                    guestBloodGroup  = bloodGroup
                )
                pendingAckRequest = null
            }
        )
    }
}

// ── Public Request Card ───────────────────────────────────────────────────────

@Composable
private fun PublicRequestCard(
    req:             PublicRequestItem,
    isAcknowledged:  Boolean,
    isAcknowledging: Boolean,
    onClick:         () -> Unit,
    onDonate:        () -> Unit
) {
    val accent = if (req.urgency == "urgent") Color(0xFFB71C1C) else RedPrimary

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        onClick   = onClick
    ) {
        Column(modifier = Modifier.padding(14.dp)) {

            // ── Blood group + urgency chips ──────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(shape = RoundedCornerShape(6.dp), color = accent.copy(alpha = 0.12f)) {
                    Text(
                        req.bloodGroup, color = accent, fontWeight = FontWeight.Bold, fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
                if (req.urgency == "urgent") {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFB71C1C).copy(alpha = 0.12f)
                    ) {
                        Text(
                            "URGENT", color = Color(0xFFB71C1C),
                            fontWeight = FontWeight.Bold, fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                // Responder count badge
                if (req.responderCount > 0) {
                    Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFE8F5E9)) {
                        Text(
                            "${req.responderCount} responded",
                            fontSize = 11.sp, color = Color(0xFF2E8B57),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Hospital & location ──────────────────────────────────────────
            Text(req.hospital, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text("${req.city}, ${req.district}", fontSize = 12.sp, color = TextSecondary)

            Spacer(Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Phone, null, tint = Color(0xFF2E8B57), modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    req.mobile, fontSize = 13.sp,
                    color = Color(0xFF2E8B57), fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.weight(1f))
                Text(req.requestDate, fontSize = 11.sp, color = TextSecondary)
            }

            Spacer(Modifier.height(12.dp))

            // ── I Can Donate / Withdraw button ───────────────────────────────
            if (isAcknowledged) {
                OutlinedButton(
                    onClick  = onDonate,
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                    shape    = RoundedCornerShape(10.dp),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                ) {
                    Icon(
                        Icons.Default.CheckCircle, null,
                        tint = Color(0xFF2E8B57), modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Donated — Tap to Withdraw",
                        fontSize = 13.sp, color = Color(0xFF2E8B57)
                    )
                }
            } else {
                Button(
                    onClick  = onDonate,
                    enabled  = !isAcknowledging,
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                    shape    = RoundedCornerShape(10.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = accent)
                ) {
                    if (isAcknowledging) {
                        CircularProgressIndicator(
                            color       = Color.White,
                            modifier    = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Default.Favorite, null,
                            tint     = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "I Can Donate",
                            fontSize   = 13.sp,
                            color      = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

// ── Guest Donate Bottom Sheet ─────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GuestDonateSheet(
    sheetState:  SheetState,
    accentColor: Color,
    isLoading:   Boolean,
    onDismiss:   () -> Unit,
    onConfirm:   (name: String, phone: String, bloodGroup: String) -> Unit
) {
    var name        by remember { mutableStateOf("") }
    var phone       by remember { mutableStateOf("") }
    var bloodGroup  by remember { mutableStateOf("") }
    var bgExpanded  by remember { mutableStateOf(false) }

    val canSubmit = name.isNotBlank() && phone.length >= 10 && bloodGroup.isNotBlank()

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
            Text("Your Details", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15161B))
            Text(
                "These will be shared with the requester so they can contact you.",
                fontSize = 13.sp, color = TextSecondary, lineHeight = 18.sp
            )

            OutlinedTextField(
                value         = name,
                onValueChange = { name = it },
                label         = { Text("Your Name") },
                leadingIcon   = { Icon(Icons.Default.Person, null) },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth(),
                shape         = RoundedCornerShape(10.dp)
            )

            OutlinedTextField(
                value         = phone,
                onValueChange = { phone = it.filter { c -> c.isDigit() }.take(10) },
                label         = { Text("Phone Number") },
                leadingIcon   = { Icon(Icons.Default.Phone, null) },
                singleLine    = true,
                isError       = phone.isNotBlank() && phone.length < 10,
                supportingText = if (phone.isNotBlank() && phone.length < 10)
                    ({ Text("Enter a valid 10-digit number") }) else null,
                modifier      = Modifier.fillMaxWidth(),
                shape         = RoundedCornerShape(10.dp)
            )


            // Proper dropdown using ExposedDropdownMenuBox
            ExposedDropdownMenuBoxBloodGroup(
                bloodGroup = bloodGroup,
                onSelect   = { bloodGroup = it }
            )

            Spacer(Modifier.height(4.dp))

            Button(
                onClick  = { if (canSubmit) onConfirm(name, phone, bloodGroup) },
                enabled  = canSubmit && !isLoading,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = accentColor)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Favorite, null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Confirm — I Can Donate",
                        fontSize = 15.sp, color = Color.White, fontWeight = FontWeight.SemiBold
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExposedDropdownMenuBoxBloodGroup(
    bloodGroup: String,
    onSelect:   (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded        = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value         = bloodGroup,
            onValueChange = {},
            readOnly      = true,
            label         = { Text("Blood Group") },
            trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier      = Modifier.fillMaxWidth().menuAnchor(),
            shape         = RoundedCornerShape(10.dp)
        )
        ExposedDropdownMenu(
            expanded         = expanded,
            onDismissRequest = { expanded = false }
        ) {
            BLOOD_GROUPS_LIST.forEach { group ->
                DropdownMenuItem(
                    text    = { Text(group) },
                    onClick = { onSelect(group); expanded = false }
                )
            }
        }
    }
}