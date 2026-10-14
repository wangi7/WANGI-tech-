package com.example.ui.employee.gaming

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.WangiViewModel
import kotlinx.coroutines.delay

@Composable
fun GamingEmployeeScreen(
    currentUser: User,
    branch: Branch?,
    shifts: List<Shift>,
    sessions: List<GamingSession>,
    debts: List<CustomerDebt>,
    reports: List<DailyReport>,
    notifications: List<NotificationItem>,
    liveTime: String,
    liveDate: String,
    onStartGaming: (ConsoleType, Int, GameCategory, String, Int, Boolean, String) -> Unit,
    onCompleteGaming: (String) -> Unit,
    onReverseGaming: (String, String) -> Unit,
    onClockIn: (String, Boolean, String) -> Unit,
    onClockOut: (String) -> Unit,
    onSubmitReport: (String, String, Long, Long, Long, Long, Long, Long, Long, String) -> Unit,
    onPlayAlertSound: () -> Unit,
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val myShifts = shifts.filter { it.employeeId == currentUser.id }
    val activeShift = myShifts.find { it.status == ShiftStatus.ACTIVE }
    val mySessions = sessions.filter { it.employeeId == currentUser.id }
    val myDebts = debts.filter { it.recordedByEmployeeId == currentUser.id }
    val myReports = reports.filter { it.employeeId == currentUser.id }

    var showStartSessionDialog by remember { mutableStateOf(false) }
    var showClockInDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var reversalSessionDialog by remember { mutableStateOf<GamingSession?>(null) }
    var reversalReason by remember { mutableStateOf("") }
    var shiftStartTimeStr by remember { mutableStateOf("08:00 AM") }
    var uniformStatus by remember { mutableStateOf(true) }

    // Session Form State
    var selectedConsole by remember { mutableStateOf(ConsoleType.PS4) }
    var stationNumber by remember { mutableIntStateOf(1) }
    var selectedCategory by remember { mutableStateOf(GameCategory.PES_FIFA) }
    var gameTitle by remember { mutableStateOf("FIFA 24") }
    var durationMinutes by remember { mutableIntStateOf(15) }
    var isCashPaid by remember { mutableStateOf(true) }
    var playerName by remember { mutableStateOf("") }

    // Report State
    var reportExpensesText by remember { mutableStateOf("0") }
    var reportActualCashText by remember { mutableStateOf("") }
    var reportNotes by remember { mutableStateOf("") }

    // Live Ticker for Active Session Countdowns
    var currentTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            currentTimeMillis = System.currentTimeMillis()
        }
    }

    val totalCash = mySessions.filter { it.isPaid && it.status != SessionStatus.CANCELLED_REVERSED }.sumOf { it.totalAmountUgx }
    val currentRate = activeShift?.commissionRatePercent ?: 25.0
    val earnedComm = (totalCash * (currentRate / 100.0)).toLong()

    Scaffold(
        containerColor = WangiBackground,
        topBar = {
            WangiTopBar(
                title = "GAMING WORKSPACE",
                subtitle = "${currentUser.fullName} • PS3 & PS4 Stations",
                timeText = liveTime,
                unreadNotificationCount = notifications.count { !it.isRead },
                onLogoutClick = onLogout
            )
        },
        bottomBar = {
            if (activeShift != null) {
                WangiBottomNavigation(
                    currentTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    tab1Label = "Stations",
                    tab2Label = "Shift & Clock",
                    tab3Label = "History",
                    tab4Label = "Daily Report",
                    tab1Icon = Icons.Default.SportsEsports,
                    tab2Icon = Icons.Default.AccessTime,
                    tab3Icon = Icons.Default.History,
                    tab4Icon = Icons.Default.Description
                )
            }
        },
        floatingActionButton = {
            if (selectedTab == 0 && activeShift != null) {
                FloatingActionButton(
                    onClick = { showStartSessionDialog = true },
                    containerColor = WangiRed,
                    contentColor = WangiWhite,
                    modifier = Modifier.testTag("fab_start_gaming_session")
                ) {
                    Icon(Icons.Default.Timer, contentDescription = "Start Console Timer")
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (activeShift == null) {
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    MandatoryClockInCard(
                        departmentName = "Gaming Station",
                        shiftStartTimeStr = shiftStartTimeStr,
                        onStartTimeSelected = { shiftStartTimeStr = it },
                        uniformStatus = uniformStatus,
                        onUniformStatusChanged = { uniformStatus = it },
                        onClockIn = {
                            onClockIn(shiftStartTimeStr, uniformStatus, "Shift clocked via mandatory lockout prompt")
                        }
                    )
                }
            } else {
                item {
                    Spacer(modifier = Modifier.height(4.dp))

                    // Shift Banner
                    WangiCard(
                        backgroundColor = WangiSuccessLight
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "GAMING SHIFT ACTIVE",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WangiSuccess
                                )
                                Text("Clocked: ${activeShift.actualClockInTime} • Rate: ${activeShift.commissionRatePercent}%", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }

                            OutlinedButton(
                                onClick = { onClockOut(activeShift.id) },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = WangiRed)
                            ) {
                                Text("Clock Out", fontSize = 12.sp)
                            }
                        }
                    }
                }

                // Gaming Revenue Card
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    WangiCard(
                        modifier = Modifier.weight(1f),
                        backgroundColor = WangiWhite
                    ) {
                        Text("Gaming Cash Collected", fontSize = 11.sp, color = WangiTextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "UGX ${WangiViewModel.formatUgx(totalCash)}",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = WangiCharcoal
                        )
                    }

                    WangiCard(
                        modifier = Modifier.weight(1f),
                        backgroundColor = WangiGoldLight
                    ) {
                        Text("Earned Commission", fontSize = 11.sp, color = WangiTextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "UGX ${WangiViewModel.formatUgx(earnedComm)}",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = WangiGoldDark
                        )
                    }
                }
            }

            when (selectedTab) {
                0 -> {
                    // Active Console Sessions with Live Countdown Timers
                    val activeSessions = mySessions.filter { it.status == SessionStatus.ACTIVE }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ACTIVE CONSOLE STATIONS (${activeSessions.size} RUNNING)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = WangiTextSecondary,
                                letterSpacing = 0.5.sp
                            )
                            Button(
                                onClick = { showStartSessionDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = WangiRed),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Start Session", fontSize = 11.5.sp)
                            }
                        }
                    }

                    if (activeSessions.isEmpty()) {
                        item {
                            WangiCard {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Default.SportsEsports, contentDescription = null, tint = WangiTextMuted, modifier = Modifier.size(40.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("No console timers running right now.", fontWeight = FontWeight.Bold, color = WangiCharcoal)
                                    Text("Click '+ Start Session' to start PS3/PS4 timers.", fontSize = 12.sp, color = WangiTextSecondary)
                                }
                            }
                        }
                    } else {
                        items(activeSessions) { session ->
                            val remainingMillis = (session.endTimeMillis - currentTimeMillis).coerceAtLeast(0L)
                            val remainingSec = (remainingMillis / 1000) % 60
                            val remainingMin = (remainingMillis / (1000 * 60))
                            val isExpired = remainingMillis == 0L

                            // Trigger sound alert when expired
                            LaunchedEffect(isExpired) {
                                if (isExpired) {
                                    onPlayAlertSound()
                                }
                            }

                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isExpired) WangiLightRed else WangiWhite
                                ),
                                border = CardDefaults.outlinedCardBorder().copy(
                                    brush = androidx.compose.ui.graphics.SolidColor(
                                        if (isExpired) WangiRed else WangiBorder
                                    )
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(if (session.consoleType == ConsoleType.PS4) Color(0xFF1D4ED8) else Color(0xFF6B21A8)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    session.consoleType.name,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    "Station #${session.consoleNumber} • ${session.gameTitle}",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                )
                                                Text("Player: ${session.customerName} (${session.durationMinutes} mins)", fontSize = 11.5.sp, color = WangiTextSecondary)
                                            }
                                        }

                                        StatusBadge(status = if (isExpired) "TIME EXPIRED!" else "RUNNING")
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Big visible countdown timer
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isExpired) WangiRed.copy(alpha = 0.1f) else WangiSurfaceVariant)
                                            .padding(vertical = 12.dp, horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("Remaining Time", fontSize = 10.5.sp, color = WangiTextSecondary)
                                            Text(
                                                text = if (isExpired) "00:00 - TIME IS UP!" else String.format("%02d:%02d", remainingMin, remainingSec),
                                                fontSize = 24.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (isExpired) WangiRed else WangiCharcoal
                                            )
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("Total Charge", fontSize = 10.5.sp, color = WangiTextSecondary)
                                            Text(
                                                "UGX ${WangiViewModel.formatUgx(session.totalAmountUgx)}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp,
                                                color = WangiGoldDark
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        OutlinedButton(
                                            onClick = { reversalSessionDialog = session },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = WangiRed)
                                        ) {
                                            Text("Reverse (Accidental)", fontSize = 11.sp)
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Button(
                                            onClick = { onCompleteGaming(session.id) },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = WangiSuccess)
                                        ) {
                                            Text("Complete & Save", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // Shift Details
                    item {
                        Text("MY GAMING SHIFTS", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    items(myShifts) { shf ->
                        WangiCard {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("Date: ${shf.date}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("Start: ${shf.actualClockInTime ?: "N/A"}", fontSize = 12.sp, color = WangiTextSecondary)
                                    Text("Commission Rate: ${shf.commissionRatePercent}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = WangiGoldDark)
                                }
                                StatusBadge(shf.status.name)
                            }
                        }
                    }
                }

                2 -> {
                    // Session History
                    item {
                        Text("RECENT SESSIONS HISTORY (${mySessions.size})", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    items(mySessions) { s ->
                        WangiCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("${s.consoleType} Station #${s.consoleNumber} • ${s.gameTitle}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("${s.durationMinutes} min session • Player: ${s.customerName}", fontSize = 11.5.sp, color = WangiTextSecondary)
                                    if (s.reversalReason != null) {
                                        Text("Reversal Reason: ${s.reversalReason}", fontSize = 11.sp, color = WangiRed)
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("UGX ${WangiViewModel.formatUgx(s.totalAmountUgx)}", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                                    StatusBadge(status = s.status.name)
                                }
                            }
                        }
                    }
                }

                3 -> {
                    // Daily Report
                    item {
                        WangiCard {
                            Text("Submit Gaming Shift Report", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Reconcile gaming station cash and commission for supervisor verification.", fontSize = 12.sp, color = WangiTextSecondary)
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    reportActualCashText = totalCash.toString()
                                    showReportDialog = true
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = WangiRed),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Prepare Daily Gaming Report", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    item {
                        Text("PREVIOUS GAMING REPORTS (${myReports.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = WangiTextSecondary)
                    }

                    items(myReports) { rep ->
                        WangiCard {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text(rep.referenceNumber, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("Date: ${rep.date} • Expected: UGX ${WangiViewModel.formatUgx(rep.expectedCashUgx)}", fontSize = 12.sp)
                                    Text("Actual: UGX ${WangiViewModel.formatUgx(rep.actualCashReceivedUgx)} • Comm: UGX ${WangiViewModel.formatUgx(rep.commissionEarnedUgx)}", fontSize = 12.sp, color = WangiGoldDark, fontWeight = FontWeight.Bold)
                                }
                                StatusBadge(rep.status.name)
                            }
                        }
                    }
                }
            }
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }

    // Start Session Dialog
    if (showStartSessionDialog) {
        AlertDialog(
            onDismissRequest = { showStartSessionDialog = false },
            title = { Text("Start Console Session", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select Console:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = selectedConsole == ConsoleType.PS4,
                            onClick = {
                                selectedConsole = ConsoleType.PS4
                                gameTitle = "FIFA 24"
                            },
                            label = { Text("PlayStation 4 (PS4)") }
                        )
                        FilterChip(
                            selected = selectedConsole == ConsoleType.PS3,
                            onClick = {
                                selectedConsole = ConsoleType.PS3
                                gameTitle = "PES 2024"
                            },
                            label = { Text("PlayStation 3 (PS3)") }
                        )
                    }

                    Text("Station Number (1-4):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (1..4).forEach { st ->
                            FilterChip(
                                selected = stationNumber == st,
                                onClick = { stationNumber = st },
                                label = { Text("St. $st") }
                            )
                        }
                    }

                    Text("Duration:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(5, 10, 15, 20, 30, 45, 60).take(4).forEach { dur ->
                            FilterChip(
                                selected = durationMinutes == dur,
                                onClick = { durationMinutes = dur },
                                label = { Text("${dur}m") }
                            )
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(5, 10, 15, 20, 30, 45, 60).drop(4).forEach { dur ->
                            FilterChip(
                                selected = durationMinutes == dur,
                                onClick = { durationMinutes = dur },
                                label = { Text("${dur}m") }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = playerName,
                        onValueChange = { playerName = it },
                        label = { Text("Customer / Player Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isCashPaid, onCheckedChange = { isCashPaid = it })
                        Text("Paid Cash (Uncheck if on credit/debt)", fontSize = 12.sp)
                    }

                    val rate = if (selectedConsole == ConsoleType.PS4) 1000L else 500L
                    val cost = (durationMinutes / 5).coerceAtLeast(1) * rate
                    Text(
                        "Estimated Charge: UGX ${WangiViewModel.formatUgx(cost)}",
                        fontWeight = FontWeight.Bold,
                        color = WangiCharcoal,
                        fontSize = 15.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onStartGaming(selectedConsole, stationNumber, selectedCategory, gameTitle, durationMinutes, isCashPaid, playerName)
                        showStartSessionDialog = false
                        playerName = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WangiRed)
                ) {
                    Text("Start Timer Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartSessionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Reversal Dialog
    if (reversalSessionDialog != null) {
        val s = reversalSessionDialog!!
        AlertDialog(
            onDismissRequest = { reversalSessionDialog = null },
            title = { Text("Reverse Accidental Start", fontWeight = FontWeight.Bold, color = WangiRed) },
            text = {
                Column {
                    Text("Enter audited reason for cancelling ${s.consoleType} Station #${s.consoleNumber}:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = reversalReason,
                        onValueChange = { reversalReason = it },
                        placeholder = { Text("e.g. Selected wrong console; customer switched to PS4") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (reversalReason.isNotBlank()) {
                            onReverseGaming(s.id, reversalReason)
                            reversalSessionDialog = null
                            reversalReason = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WangiRed),
                    enabled = reversalReason.isNotBlank()
                ) {
                    Text("Reverse Session")
                }
            },
            dismissButton = {
                TextButton(onClick = { reversalSessionDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Clock In Dialog
    if (showClockInDialog) {
        AlertDialog(
            onDismissRequest = { showClockInDialog = false },
            title = { Text("Clock In For Gaming Shift", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select shift start time:")
                    listOf("08:00 AM (25%)", "09:00 AM (20%)", "10:00 AM (15%)", "11:00 AM (10%)").forEach { opt ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(selected = true, onClick = {})
                            Text(opt, fontSize = 13.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onClockIn("08:00 AM", true, "Gaming shift clocked")
                        showClockInDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WangiSuccess)
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClockInDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Submit Report Dialog
    if (showReportDialog) {
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = { Text("Submit Gaming Daily Report", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Total Cash: UGX ${WangiViewModel.formatUgx(totalCash)}")
                    Text("Earned Commission: UGX ${WangiViewModel.formatUgx(earnedComm)}", color = WangiGoldDark, fontWeight = FontWeight.Bold)
                    Divider()
                    OutlinedTextField(
                        value = reportActualCashText,
                        onValueChange = { reportActualCashText = it },
                        label = { Text("Actual Cash Count (UGX)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = reportExpensesText,
                        onValueChange = { reportExpensesText = it },
                        label = { Text("Expenses (UGX)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = reportNotes,
                        onValueChange = { reportNotes = it },
                        label = { Text("Shift Notes") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val actual = reportActualCashText.toLongOrNull() ?: totalCash
                        val exp = reportExpensesText.toLongOrNull() ?: 0L
                        onSubmitReport("08:00 AM", liveTime, totalCash, totalCash, 0L, totalCash, actual, exp, earnedComm, reportNotes)
                        showReportDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WangiRed)
                ) {
                    Text("Submit to Manager")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
