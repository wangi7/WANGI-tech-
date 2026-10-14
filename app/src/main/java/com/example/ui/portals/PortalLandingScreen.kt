package com.example.ui.portals

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.Portal
import kotlinx.coroutines.delay

data class PortalCardData(
    val portal: Portal,
    val icon: ImageVector,
    val accentColor: Color,
    val lightBg: Color,
    val badge: String
)

@Composable
fun PortalLandingScreen(
    liveTime: String,
    liveDate: String,
    onPortalSelected: (Portal) -> Unit
) {
    val portals = listOf(
        PortalCardData(
            portal = Portal.CEO,
            icon = Icons.Default.MilitaryTech, // Executive / Crown
            accentColor = WangiGoldDark,
            lightBg = WangiGoldLight,
            badge = "Executive"
        ),
        PortalCardData(
            portal = Portal.MANAGER,
            icon = Icons.Default.AssignmentInd, // Clipboard / Management
            accentColor = WangiRed,
            lightBg = WangiLightRed,
            badge = "Supervisory"
        ),
        PortalCardData(
            portal = Portal.EMPLOYEE,
            icon = Icons.Default.Badge, // Work badge
            accentColor = Color(0xFF2563EB),
            lightBg = Color(0xFFEFF6FF),
            badge = "Hotspot / Gaming"
        ),
        PortalCardData(
            portal = Portal.TECHNICIAN,
            icon = Icons.Default.Build, // Router / Network tools
            accentColor = Color(0xFF0D9488),
            lightBg = Color(0xFFF0FDFA),
            badge = "MikroTik & Omada"
        ),
        PortalCardData(
            portal = Portal.BILLING,
            icon = Icons.Default.WifiTethering, // Wi-Fi Billing router
            accentColor = Color(0xFF7C3AED),
            lightBg = Color(0xFFF5F3FF),
            badge = "RADIUS & Subscriptions"
        )
    )

    // Pulsing animation for the Wi-Fi signal icon
    val infiniteTransition = rememberInfiniteTransition(label = "portal_signal")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Scaffold(
        containerColor = WangiBackground
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))

                // Branded Header with glowing dark surface
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = WangiCharcoal,
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .graphicsLayer(scaleX = pulseScale, scaleY = pulseScale)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Brush.linearGradient(listOf(WangiRed, WangiDarkRed))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Wifi,
                                        contentDescription = "Logo",
                                        tint = Color.White,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "WANGI TECH",
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        text = "SOLUTIONS",
                                        color = WangiGold,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 2.sp
                                    )
                                }
                            }

                            // Kampala Clock Pill
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color.White.copy(alpha = 0.12f)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.End,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = liveTime,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Africa/Kampala",
                                        color = WangiGold,
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = liveDate,
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 12.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Universal Business Management Platform",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Select an authorized portal below to log in.",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "BUSINESS PORTALS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = WangiTextSecondary,
                        letterSpacing = 1.sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(WangiSuccess)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "5 Systems Connected",
                            fontSize = 11.sp,
                            color = WangiSuccess,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Animated Portal Cards (Staggered entrance + touch feedback)
            itemsIndexed(portals) { index, item ->
                AnimatedPortalCard(
                    data = item,
                    index = index,
                    onClick = { onPortalSelected(item.portal) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                // Footer
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "WANGI TECH SOLUTIONS • Smart Networks. Better Business.",
                        fontSize = 11.sp,
                        color = WangiTextMuted,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Kampala, Uganda • Version 2.5 Enterprise",
                        fontSize = 10.sp,
                        color = WangiTextMuted
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
fun AnimatedPortalCard(
    data: PortalCardData,
    index: Int,
    onClick: () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(index * 75L) // Staggered entrance animation
        isVisible = true
    }

    val alphaAnim by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "portal_alpha"
    )

    val translationAnim by animateFloatAsState(
        targetValue = if (isVisible) 0f else 35f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "portal_translation"
    )

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scaleAnim by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "portal_press_scale"
    )

    // Continuous elegant floating and pulsing animation on the landing page cards
    val infiniteTransition = rememberInfiniteTransition(label = "portal_float")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200 + (index * 300), easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatOffset"
    )
    val pulseElevation by infiniteTransition.animateFloat(
        initialValue = 2f,
        targetValue = 5.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000 + (index * 250), easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseElevation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = alphaAnim
                translationY = translationAnim + (if (isVisible) floatOffset else 0f)
                scaleX = scaleAnim
                scaleY = scaleAnim
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .testTag("portal_card_${data.portal.name}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = WangiWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isPressed) 1.dp else pulseElevation.dp),
        border = BorderStroke(
            1.dp,
            if (isPressed) data.accentColor else WangiBorder
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(data.lightBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = data.icon,
                            contentDescription = data.portal.title,
                            tint = data.accentColor,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = data.portal.title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = WangiCharcoal
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = data.lightBg,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text(
                                text = data.badge,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = data.accentColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Arrow Circle with subtle background
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(if (isPressed) data.lightBg else WangiSurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Enter",
                        tint = if (isPressed) data.accentColor else WangiCharcoal,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = data.portal.description,
                fontSize = 12.5.sp,
                color = WangiTextSecondary,
                lineHeight = 17.sp
            )
        }
    }
}
