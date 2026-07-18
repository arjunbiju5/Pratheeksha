@file:OptIn(ExperimentalFoundationApi::class)
package com.example.donor.ui.home


import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.donor.R
import com.example.donor.data.model.ContactPerson
import com.example.donor.ui.theme.RedDark
import com.example.donor.ui.theme.RedPrimary
import com.example.donor.ui.theme.TextSecondary
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

private val Cloud     = Color(0xFFFAF8F7)
private val Ink       = Color(0xFF1C1C1C)
private val LightGray = Color(0xFFF0EEEC)

private val avatarColors = listOf(
    Color(0xFFB71C1C),
    Color(0xFF1565C0),
    Color(0xFF2E7D32),
    Color(0xFF6A1B9A)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onRequestBlood:        () -> Unit = {},
    onFindDonor:           () -> Unit = {},
    onDonorLogin:          () -> Unit = {},
    onAdminLogin:          () -> Unit = {},
    onViewPendingRequests: () -> Unit = {}
) {
    var showContactSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var contacts by remember { mutableStateOf<List<ContactPerson>>(emptyList()) }

    DisposableEffect(Unit) {
        val ref = FirebaseDatabase.getInstance().getReference("contactPersons")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                contacts = snapshot.children.mapNotNull { child ->
                    child.getValue(ContactPerson::class.java)?.copy(uid = child.key ?: "")
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(listener)
        onDispose { ref.removeEventListener(listener) }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Cloud)
                .verticalScroll(rememberScrollState())
        ) {
            // ── Header ────────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                    .background(Brush.verticalGradient(listOf(RedPrimary, RedDark)))
                    .padding(top = 48.dp, bottom = 40.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    BloodDropLogo(size = 80.dp)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "PRATHEEKSHA",
                        fontSize   = 36.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color      = Color.White
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Save Lives. Donate Blood.",
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color      = Color.White.copy(alpha = 0.9f)
                    )
                }
            }

            // ── Action Cards ──────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 32.dp, bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Long press → Pending Requests
                HomeOptionCard(
                    icon         = Icons.Default.LocalHospital,
                    title        = "Request Blood",
                    description  = "Need blood urgently? Notify donors nearby",
                    primaryColor = Color(0xFFB71C1C),
                    onClick      = onRequestBlood,
                    onLongClick  = onViewPendingRequests
                )
                Spacer(Modifier.height(14.dp))
                HomeOptionCard(
                    icon         = Icons.Default.Search,
                    title        = "Find a Donor",
                    description  = "Search by blood group, district and city",
                    primaryColor = RedPrimary,
                    onClick      = onFindDonor
                )

                Spacer(Modifier.height(14.dp))
                HomeOptionCard(
                    icon         = Icons.Default.PersonAdd,
                    title        = "Register / Login as Donor",
                    description  = "Join as a donor or sign in to your account",
                    primaryColor = RedPrimary,
                    onClick      = onDonorLogin
                )
            }

            Spacer(Modifier.height(40.dp))
            Spacer(Modifier.height(96.dp))
        }

        // ── Floating "Contact Us" button ──────────────────────────────────────
        FloatingActionButton(
            onClick        = { showContactSheet = true },
            modifier       = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .shadow(elevation = 8.dp, shape = CircleShape, clip = false),
            shape          = CircleShape,
            containerColor = RedPrimary,
            contentColor   = Color.White
        ) {
            Icon(
                Icons.Default.SupportAgent,
                contentDescription = "Contact us",
                modifier           = Modifier.size(26.dp)
            )
        }
    }

    // ── Contact bottom sheet ──────────────────────────────────────────────────
    if (showContactSheet) {
        ModalBottomSheet(
            onDismissRequest = { showContactSheet = false },
            sheetState       = sheetState,
            containerColor   = Color.White
        ) {
            ContactSheetContent(
                contacts     = contacts,
                onAdminLogin = onAdminLogin
            )
        }
    }
}

@Composable
private fun ContactSheetContent(
    contacts:     List<ContactPerson>,
    onAdminLogin: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp)
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(4.dp, 20.dp)
                    .background(RedPrimary, RoundedCornerShape(2.dp))
            )
            Text(
                "Contact Us",
                fontSize   = 18.sp,
                fontWeight = FontWeight.Bold,
                color      = Ink
            )
        }

        Spacer(Modifier.height(4.dp))
        Text("Reach out for help", fontSize = 13.sp, color = TextSecondary)
        Spacer(Modifier.height(18.dp))

        if (contacts.isEmpty()) {
            Text(
                "No contact details available right now.",
                fontSize = 13.sp,
                color    = TextSecondary,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                contacts.forEachIndexed { index, contact ->
                    val avatarColor = avatarColors[index % avatarColors.size]
                    val initials    = contact.name.firstOrNull()?.uppercase() ?: "?"

                    Surface(
                        shape    = RoundedCornerShape(16.dp),
                        color    = LightGray,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier          = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(avatarColor.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    initials,
                                    fontSize   = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color      = avatarColor
                                )
                            }

                            Spacer(Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(contact.name,  fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                                Text(contact.phone, fontSize = 12.sp, color = TextSecondary)
                            }

                            Spacer(Modifier.width(10.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                                IconButton(
                                    onClick  = {
                                        context.startActivity(
                                            Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contact.phone}"))
                                        )
                                    },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE8F5E9))
                                ) {
                                    Icon(
                                        Icons.Default.Call,
                                        contentDescription = "Call ${contact.name}",
                                        tint     = Color(0xFF2E7D32),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                IconButton(
                                    onClick  = {
                                        context.startActivity(
                                            Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/91${contact.phone}"))
                                        )
                                    },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE3F2FD))
                                ) {
                                    Icon(
                                        Icons.Default.Message,
                                        contentDescription = "WhatsApp ${contact.name}",
                                        tint     = Color(0xFF1565C0),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── NSS credit + hidden admin access via long press on SSN logo ───────
        Spacer(Modifier.height(20.dp))
        Divider(color = LightGray, thickness = 1.dp)
        Spacer(Modifier.height(12.dp))
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier              = Modifier.fillMaxWidth()
        ) {
            Image(
                painter            = painterResource(id = R.drawable.ssn),
                contentDescription = null,
                contentScale       = ContentScale.Fit,
                modifier           = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .combinedClickable(
                        onClick     = {},
                        onLongClick = onAdminLogin
                    )
            )
            Spacer(Modifier.width(4.dp))
            Text(
                "An initiative by NSS Units 128 & 198",
                fontSize = 11.sp,
                color    = TextSecondary
            )
        }
    }
}

// ── Blood drop logo ───────────────────────────────────────────────────────────
@Composable
private fun BloodDropLogo(size: androidx.compose.ui.unit.Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w          = this.size.width
        val h          = this.size.height
        val bodyCenter = Offset(w * 0.5f, h * 0.60f)
        val bodyRadius = h * 0.34f
        val gradient   = Brush.verticalGradient(
            colors = listOf(RedPrimary, RedPrimary.copy(alpha = 0.92f))
        )
        drawCircle(brush = gradient, radius = bodyRadius, center = bodyCenter)
        val tipPath = Path().apply {
            moveTo(w * 0.5f, h * 0.04f)
            cubicTo(w * 0.70f, h * 0.26f, w * 0.82f, h * 0.42f, w * 0.80f, h * 0.58f)
            lineTo(w * 0.20f, h * 0.58f)
            cubicTo(w * 0.18f, h * 0.42f, w * 0.30f, h * 0.26f, w * 0.5f, h * 0.04f)
            close()
        }
        drawPath(path = tipPath, brush = gradient)
        drawOval(
            color   = Color.White.copy(alpha = 0.25f),
            topLeft = Offset(bodyCenter.x + bodyRadius * 0.05f, bodyCenter.y - bodyRadius * 0.15f),
            size    = androidx.compose.ui.geometry.Size(bodyRadius * 0.55f, bodyRadius * 0.65f)
        )
    }
}

// ── Action card ───────────────────────────────────────────────────────────────
@Composable
private fun HomeOptionCard(
    icon:         androidx.compose.ui.graphics.vector.ImageVector,
    title:        String,
    description:  String,
    primaryColor: Color,
    onClick:      () -> Unit,
    onLongClick:  (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(16.dp), clip = true)
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick     = onClick,
                onLongClick = onLongClick
            ),
        shape  = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, primaryColor.copy(alpha = 0.2f))
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier         = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(primaryColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = primaryColor, modifier = Modifier.size(28.dp))
            }
            Column(
                modifier            = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(title,       fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                Text(description, fontSize = 13.sp, color = TextSecondary, lineHeight = 18.sp)
            }
            Icon(Icons.Default.ChevronRight, null, tint = primaryColor, modifier = Modifier.size(24.dp))
        }
    }
}