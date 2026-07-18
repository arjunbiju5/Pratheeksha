package com.example.donor.ui.dashboard

import android.app.DatePickerDialog
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.donor.ui.theme.RedPrimary
import com.example.donor.ui.theme.TextSecondary
import java.util.*

private val MaroonDeep   = Color(0xFF6E1423)
private val PinkTint     = Color(0xFFFCE9EC)
private val SuccessGreen = Color(0xFF2E8B57)
private val SuccessTint  = Color(0xFFE3F3EA)
private val Ink          = Color(0xFF231417)
private val Cloud        = Color(0xFFF7F2F1)
private val NeutralRing  = Color(0xFFD8D2D1)
private val NeutralTrack = Color(0xFFEDE8E7)

@Composable
fun DashboardScreen(
    onLogout: () -> Unit,
    viewModel: DashboardViewModel = viewModel()
) {
    val uiState = viewModel.uiState.collectAsState().value
    val context = LocalContext.current

    var showDeleteDialog by remember { mutableStateOf(false) }
    var hasDonatedBefore by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedDonationDate by remember { mutableStateOf("") }

    if (showDatePicker) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            context,
            { _, year, month, day ->
                selectedDonationDate = "%02d/%02d/%04d".format(day, month + 1, year)
                showDatePicker       = false
                viewModel.saveDonationDate(selectedDonationDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).also { dialog ->
            dialog.datePicker.maxDate = calendar.timeInMillis
            dialog.setOnCancelListener {
                hasDonatedBefore = false
                showDatePicker   = false
            }
        }.show()
    }

    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) onLogout()
    }

    LaunchedEffect(uiState.donor) {
        uiState.donor?.let { donor ->
            if (donor.lastDonation.isNotBlank() && donor.lastDonation != "Never") {
                hasDonatedBefore     = true
                selectedDonationDate = donor.lastDonation
            }
        }
    }

    if (showDeleteDialog) {
        ConfirmDialog(
            title        = "Delete profile?",
            message      = "This permanently removes your donor profile and signs you out. This can't be undone.",
            confirmText  = "Delete",
            confirmColor = Color(0xFFB71C1C),
            onConfirm    = { viewModel.deleteProfile(); showDeleteDialog = false },
            onDismiss    = { showDeleteDialog = false }
        )
    }

    if (uiState.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = RedPrimary)
        }
        return
    }

    val donor = uiState.donor ?: return

    // ── Eligibility derivation ──────────────────────────────────────────────
    // rawDaysLeft can be negative if nextEligible has already passed — that sign
    // is exactly what tells us whether the donor is still "recovering" or has
    // become eligible again. daysLeft (clamped) is only for display purposes.
    val rawDaysLeft      = if (donor.nextEligible.isNotBlank()) viewModel.daysRemaining(donor.nextEligible) else 0
    val daysLeft         = rawDaysLeft.coerceAtLeast(0)
    val recoveryProgress = ((90 - daysLeft) / 90f).coerceIn(0f, 1f)
    val isNeverDonated   = donor.lastDonation.isBlank() || donor.lastDonation == "Never"
    val isRecovering     = !isNeverDonated && donor.nextEligible.isNotBlank() && rawDaysLeft > 0
    val isEligibleNow    = !isNeverDonated && !isRecovering

    val ringColor = when {
        isEligibleNow -> SuccessGreen
        isRecovering  -> RedPrimary
        else          -> NeutralRing
    }
    val ringTrack = when {
        isEligibleNow -> SuccessTint
        isRecovering  -> PinkTint
        else          -> NeutralTrack
    }
    val ringProgress = if (isRecovering) recoveryProgress else 1f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(Cloud)
    ) {

        // ── Header ────────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                .background(Brush.verticalGradient(listOf(RedPrimary, MaroonDeep)))
                .padding(top = 28.dp, bottom = 28.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier            = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                StatusRing(
                    ringSize    = 96.dp,
                    strokeWidth = 5.dp,
                    progress    = ringProgress,
                    ringColor   = ringColor,
                    trackColor  = ringTrack
                ) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text       = donor.fullName.firstOrNull()?.uppercase() ?: "D",
                            fontSize   = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color      = RedPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text       = donor.fullName,
                    fontSize   = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White.copy(alpha = 0.18f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier          = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                        ) {
                            Icon(Icons.Default.Favorite, null, tint = Color.White, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(donor.bloodGroup, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isEligibleNow) SuccessGreen else Color.White.copy(alpha = 0.18f)
                    ) {
                        Text(
                            text = when {
                                isEligibleNow -> "Eligible now"
                                isRecovering  -> "Recovering · ${daysLeft}d"
                                else          -> "New donor"
                            },
                            color      = Color.White,
                            fontSize   = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier   = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, null, tint = Color.White.copy(alpha = 0.75f), modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${donor.city}, ${donor.district}", color = Color.White.copy(alpha = 0.75f), fontSize = 13.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // ── Stats Row ─────────────────────────────────────────────────────────
        Row(
            modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                icon     = Icons.Default.DateRange,
                label    = "Last donated",
                value    = if (isNeverDonated) "-" else donor.lastDonation,
                accent   = RedPrimary
            )
            StatCard(
                modifier = Modifier.weight(1f),
                icon     = Icons.Default.Refresh,
                label    = "Next eligible",
                value    = if (donor.nextEligible.isBlank()) "-" else donor.nextEligible,
                accent   = if (isEligibleNow) SuccessGreen else RedPrimary
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ── Donated Before Card ───────────────────────────────────────────────
        DashboardCard {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier          = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (!hasDonatedBefore) {
                                hasDonatedBefore = true
                                showDatePicker   = true
                            } else {
                                hasDonatedBefore     = false
                                selectedDonationDate = ""
                                viewModel.clearDonationDate()
                            }
                        }
                ) {
                    Checkbox(
                        checked         = hasDonatedBefore,
                        onCheckedChange = null,
                        colors          = CheckboxDefaults.colors(checkedColor = RedPrimary, uncheckedColor = TextSecondary)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            "I've donated blood before",
                            fontSize   = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color      = if (hasDonatedBefore) RedPrimary else Ink
                        )
                        Text("Tap to record your last donation date", fontSize = 12.sp, color = TextSecondary)
                    }
                }

                if (hasDonatedBefore && selectedDonationDate.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color(0xFFEFEAE9))
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier              = Modifier.fillMaxWidth().clickable { showDatePicker = true },
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(32.dp).clip(CircleShape).background(PinkTint),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.DateRange, null, tint = RedPrimary, modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Last donation", fontSize = 12.sp, color = TextSecondary)
                                Text(selectedDonationDate, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = RedPrimary)
                            }
                        }
                        Text("Change", fontSize = 12.sp, color = RedPrimary, fontWeight = FontWeight.Medium)
                    }

                    if (donor.nextEligible.isNotBlank()) {
                        Spacer(modifier = Modifier.height(14.dp))

                        val animatedProgress by animateFloatAsState(
                            targetValue   = recoveryProgress,
                            animationSpec = tween(durationMillis = 900),
                            label         = "progress"
                        )

                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Text("Recovery period", fontSize = 13.sp, color = TextSecondary)
                            Text("$daysLeft days left", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = RedPrimary)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        LinearProgressIndicator(
                            progress   = { animatedProgress },
                            modifier   = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color      = RedPrimary,
                            trackColor = PinkTint,
                            strokeCap  = StrokeCap.Round
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Eligible again on ${donor.nextEligible}", fontSize = 12.sp, color = TextSecondary)

                    } else {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier          = Modifier.fillMaxWidth().background(SuccessTint, RoundedCornerShape(10.dp)).padding(10.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("You're eligible to donate now", fontSize = 13.sp, color = SuccessGreen, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ── Availability Toggle ───────────────────────────────────────────────
        DashboardCard {
            val isCurrentlyLocked = isRecovering
            Row(
                modifier              = Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (donor.available && !isCurrentlyLocked) SuccessTint else Color(0xFFEDE8E7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (donor.available) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            null,
                            tint     = if (donor.available && !isCurrentlyLocked) SuccessGreen else TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "Show me as available",
                            fontSize   = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = if (isCurrentlyLocked) TextSecondary else Ink
                        )
                        Text(
                            if (isCurrentlyLocked) "Unavailable during recovery" else "Others can find and contact you",
                            fontSize = 12.sp,
                            color    = TextSecondary
                        )
                    }
                }
                Switch(
                    checked         = if (isCurrentlyLocked) false else donor.available,
                    onCheckedChange = { viewModel.toggleAvailability(it) },
                    enabled         = !isCurrentlyLocked,
                    colors          = SwitchDefaults.colors(
                        checkedThumbColor   = Color.White,
                        checkedTrackColor   = SuccessGreen,
                        uncheckedTrackColor = Color(0xFFBDBDBD)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ── Donor Info Card ───────────────────────────────────────────────────
        DashboardCard {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Your details", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = RedPrimary)
                HorizontalDivider(color = Color(0xFFEFEAE9))
                InfoRow(Icons.Default.Person,        "Full name",     donor.fullName)
                InfoRow(Icons.Default.Phone,         "Mobile",        donor.mobile)
                InfoRow(Icons.Default.Favorite,      "Blood group",   donor.bloodGroup)
                InfoRow(Icons.Default.Cake, "Date of birth", donor.dateOfBirth.ifBlank { "Not provided" })
                InfoRow(Icons.Default.MonitorWeight, "Weight", donor.weight.ifBlank { "Not provided" })
                InfoRow(Icons.Default.Place, "City", donor.city)
                InfoRow(Icons.Default.LocationOn, "District", donor.district)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ── Delete Profile ────────────────────────────────────────────────────
        OutlinedButton(
            onClick  = { showDeleteDialog = true },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(50.dp),
            shape    = RoundedCornerShape(12.dp),
            colors   = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFB71C1C)),
            border   = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFB71C1C).copy(alpha = 0.4f))
        ) {
            Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Remove my profile", fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// ── Reusable composables ──────────────────────────────────────────────────────

@Composable
fun StatusRing(
    ringSize: Dp,
    strokeWidth: Dp,
    progress: Float,
    ringColor: Color,
    trackColor: Color,
    content: @Composable BoxScope.() -> Unit
) {
    val animated by animateFloatAsState(
        targetValue   = progress,
        animationSpec = tween(durationMillis = 900),
        label         = "ring"
    )
    Box(modifier = Modifier.size(ringSize), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val stroke  = strokeWidth.toPx()
            val topLeft = Offset(stroke / 2, stroke / 2)
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(color = trackColor, startAngle = -90f, sweepAngle = 360f, useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round), topLeft = topLeft, size = arcSize)
            drawArc(color = ringColor, startAngle = -90f, sweepAngle = 360f * animated, useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round), topLeft = topLeft, size = arcSize)
        }
        content()
    }
}

@Composable
fun DashboardCard(content: @Composable () -> Unit) {
    Card(
        modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape     = RoundedCornerShape(18.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Box(modifier = Modifier.padding(16.dp)) { content() }
    }
}

@Composable
fun StatCard(modifier: Modifier = Modifier, icon: ImageVector, label: String, value: String, accent: Color) {
    Card(
        modifier  = modifier,
        shape     = RoundedCornerShape(18.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier            = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape).background(accent.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = accent, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = accent)
            Spacer(modifier = Modifier.height(2.dp))
            Text(label, fontSize = 11.sp, color = TextSecondary, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = TextSecondary, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Text(label, fontSize = 13.sp, color = TextSecondary, modifier = Modifier.weight(1f))
        Text(value.ifBlank { "—" }, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Ink)
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    confirmColor: Color,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink)
                Spacer(modifier = Modifier.height(12.dp))
                Text(message, fontSize = 14.sp, color = TextSecondary, lineHeight = 20.sp)
                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) {
                        Text("Cancel")
                    }
                    Button(
                        onClick  = onConfirm,
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(10.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = confirmColor)
                    ) {
                        Text(confirmText, color = Color.White)
                    }
                }
            }
        }
    }
}