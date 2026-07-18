package com.example.donor.ui.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.donor.R
import com.example.donor.ui.theme.RedDark
import com.example.donor.ui.theme.White
import kotlinx.coroutines.delay

@Composable
fun LogoDisplay(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id = R.drawable.logo),
        contentDescription = "App Logo",
        modifier = modifier.size(180.dp)
    )
}

@Composable
fun SplashScreen(
    onFinished:            () -> Unit,
    onDeepLink:            (String, Boolean) -> Unit = { _, _ -> },
    onDeepLinkPending:     () -> Unit       = {},
    deepLinkRequestId:     String?          = null,
    deepLinkPending:       Boolean          = false,
    fromNotification:      Boolean          = false
){
    var startAnimation by remember { mutableStateOf(false) }

    val alpha by animateFloatAsState(
        targetValue   = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label         = "alpha"
    )

    val scale by animateFloatAsState(
        targetValue   = if (startAnimation) 1f else 0.8f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessLow
        ),
        label = "scale"
    )

    val offsetY by animateDpAsState(
        targetValue   = if (startAnimation) 0.dp else 40.dp,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label         = "offsetY"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
        // Shorter delay when launched from a notification
        val isDeepLink = !deepLinkRequestId.isNullOrBlank() || deepLinkPending
        delay(if (isDeepLink) 1200L else 2500L)

        when {
            !deepLinkRequestId.isNullOrBlank() -> onDeepLink(deepLinkRequestId,fromNotification)
            deepLinkPending                    -> onDeepLinkPending()
            else                               -> onFinished()
        }
    }

    Box(
        modifier         = Modifier
            .fillMaxSize()
            .background(White),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier            = Modifier
                .padding(24.dp)
                .offset(y = offsetY)
                .alpha(alpha)
                .scale(scale)
        ) {
            LogoDisplay()

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text  = "PRATHEEKSHA",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize      = 42.sp,
                    letterSpacing = 2.sp,
                    fontWeight    = FontWeight.ExtraBold
                ),
                color     = RedDark,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text  = "Connecting Donors, Saving Lives",
                style = MaterialTheme.typography.bodyMedium.copy(
                    letterSpacing = 1.sp,
                    lineHeight    = 20.sp
                ),
                color      = RedDark.copy(alpha = 0.7f),
                textAlign  = TextAlign.Center,
                fontWeight = FontWeight.Medium
            )
        }
    }
}