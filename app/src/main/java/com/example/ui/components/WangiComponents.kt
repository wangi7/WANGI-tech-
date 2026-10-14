package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WangiTopBar(
    title: String,
    subtitle: String? = null,
    timeText: String? = null,
    showBackButton: Boolean = false,
    onBackClick: () -> Unit = {},
    unreadNotificationCount: Int = 0,
    onNotificationClick: () -> Unit = {},
    onLogoutClick: (() -> Unit)? = null,
    onMenuClick: (() -> Unit)? = null
) {
    Surface(
        color = WangiWhite,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (showBackButton) {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier
                                .testTag("btn_back")
                                .size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = WangiCharcoal
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    } else if (onMenuClick != null) {
                        IconButton(
                            onClick = onMenuClick,
                            modifier = Modifier
                                .testTag("btn_menu")
                                .size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu",
                                tint = WangiCharcoal
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    // WANGI Logo Mark & Title
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Brush.linearGradient(listOf(WangiRed, WangiDarkRed))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Wifi,
                                contentDescription = "WANGI Logo",
                                tint = WangiWhite,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = WangiCharcoal
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (subtitle != null) {
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 12.sp,
                                        color = WangiTextSecondary
                                    ),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Kampala Clock pill
                    if (timeText != null) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = WangiSurfaceVariant,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(WangiSuccess)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = timeText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = WangiTextPrimary
                                )
                            }
                        }
                    }

                    // Notification Icon with Badge
                    Box {
                        IconButton(
                            onClick = onNotificationClick,
                            modifier = Modifier
                                .testTag("btn_notifications")
                                .size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint = WangiCharcoal
                            )
                        }
                        if (unreadNotificationCount > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = (-4).dp, y = 4.dp)
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(WangiRed),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (unreadNotificationCount > 9) "9+" else "$unreadNotificationCount",
                                    color = WangiWhite,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Logout Action
                    if (onLogoutClick != null) {
                        IconButton(
                            onClick = onLogoutClick,
                            modifier = Modifier
                                .testTag("btn_logout")
                                .size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ExitToApp,
                                contentDescription = "Logout",
                                tint = WangiRed
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WangiCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = WangiWhite,
    borderColor: Color = WangiBorder,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(borderColor, borderColor))),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            content = content
        )
    }
}

@Composable
fun QuickActionItem(
    icon: ImageVector,
    label: String,
    iconTint: Color = WangiRed,
    iconBgColor: Color = WangiLightRed,
    badgeText: String? = null,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(80.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp)
            .testTag("action_$label")
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(iconBgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(26.dp)
            )

            if (badgeText != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = WangiGold,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-4).dp)
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = WangiCharcoal,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = WangiCharcoal,
            textAlign = TextAlign.Center,
            maxLines = 2,
            lineHeight = 13.sp
        )
    }
}

@Composable
fun StatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor) = when (status.uppercase()) {
        "ACTIVE", "CONFIRMED", "VERIFIED", "ONLINE", "COMPLETED" -> WangiSuccessLight to WangiSuccess
        "PENDING", "SUBMITTED", "UNDER_REVIEW", "IN_PROGRESS", "ASSIGNED" -> WangiWarningLight to WangiWarning
        "RETURNED_FOR_CORRECTION", "REJECTED", "OFFLINE", "CANCELLED", "CANCELLED_REVERSED" -> WangiLightRed to WangiRed
        else -> WangiSurfaceVariant to WangiTextSecondary
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        modifier = modifier
    ) {
        Text(
            text = status.replace("_", " "),
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun WangiBottomNavigation(
    currentTab: Int,
    onTabSelected: (Int) -> Unit,
    tab1Label: String = "Home",
    tab2Label: String = "Operations",
    tab3Label: String = "Reports",
    tab4Label: String = "More",
    tab1Icon: ImageVector = Icons.Default.Home,
    tab2Icon: ImageVector = Icons.Default.Wifi,
    tab3Icon: ImageVector = Icons.Default.Assessment,
    tab4Icon: ImageVector = Icons.Default.MoreHoriz
) {
    Surface(
        color = WangiWhite,
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tabs = listOf(
                Triple(0, tab1Label, tab1Icon),
                Triple(1, tab2Label, tab2Icon),
                Triple(2, tab3Label, tab3Icon),
                Triple(3, tab4Label, tab4Icon)
            )

            tabs.forEach { (index, label, icon) ->
                val selected = currentTab == index
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { onTabSelected(index) }
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .testTag("tab_$label")
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (selected) WangiRed else WangiTextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected) WangiRed else WangiTextSecondary
                    )
                    // Active Pill indicator
                    if (selected) {
                        Box(
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .width(16.dp)
                                .height(2.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(WangiRed)
                        )
                    } else {
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun MandatoryClockInCard(
    departmentName: String,
    shiftStartTimeStr: String,
    onStartTimeSelected: (String) -> Unit,
    uniformStatus: Boolean,
    onUniformStatusChanged: (Boolean) -> Unit,
    onClockIn: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_mandatory_clock_in"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = WangiWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        border = BorderStroke(1.5.dp, WangiRed)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(WangiLightRed),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = WangiRed,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "CLOCK-IN REQUIRED",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = WangiRed
                    )
                    Text(
                        text = "$departmentName Workspace Locked",
                        fontSize = 12.sp,
                        color = WangiTextSecondary
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = WangiBackground
            ) {
                Text(
                    text = "Company policy requires all staff to clock in, record arrival time, and verify uniform compliance before using the system.",
                    fontSize = 12.sp,
                    color = WangiTextSecondary,
                    modifier = Modifier.padding(10.dp),
                    lineHeight = 16.sp
                )
            }

            // Arrival Time Picker
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Select Actual Arrival Time:",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = WangiCharcoal
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("08:00 AM" to "25%", "09:00 AM" to "20%", "10:00 AM" to "15%", "11:00 AM" to "10%").forEach { (time, rate) ->
                        FilterChip(
                            selected = shiftStartTimeStr == time,
                            onClick = { onStartTimeSelected(time) },
                            label = {
                                Text(
                                    "$time ($rate)",
                                    fontSize = 10.5.sp,
                                    fontWeight = if (shiftStartTimeStr == time) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = WangiLightRed,
                                selectedLabelColor = WangiRed
                            )
                        )
                    }
                }
            }

            // Uniform Compliance Checkbox
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (uniformStatus) Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = uniformStatus,
                        onCheckedChange = onUniformStatusChanged,
                        colors = CheckboxDefaults.colors(checkedColor = WangiSuccess)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = if (uniformStatus) "Wearing Full WANGI Uniform" else "Uniform Incomplete (-5% penalty)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (uniformStatus) WangiSuccess else WangiWarning
                        )
                        Text(
                            text = if (uniformStatus) "Standard arrival commission applies" else "5 percentage points deducted from daily rate",
                            fontSize = 10.5.sp,
                            color = WangiTextSecondary
                        )
                    }
                }
            }

            // Clock In Action Button
            Button(
                onClick = onClockIn,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_mandatory_clock_in_now"),
                colors = ButtonDefaults.buttonColors(containerColor = WangiRed),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "CLOCK IN & UNLOCK WORKSPACE",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp
                )
            }
        }
    }
}
