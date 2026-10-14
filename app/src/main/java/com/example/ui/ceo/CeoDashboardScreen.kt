package com.example.ui.ceo

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.Screen
import com.example.ui.viewmodel.WangiViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CeoDashboardScreen(
    currentUser: User,
    branches: List<Branch>,
    selectedBranchId: String,
    onBranchSelected: (String) -> Unit,
    sales: List<VoucherSale>,
    gamingSessions: List<GamingSession>,
    techJobs: List<TechnicianJob>,
    shifts: List<Shift>,
    pendingReports: List<DailyReport>,
    expenses: List<BusinessExpense>,
    notifications: List<NotificationItem>,
    liveTime: String,
    liveDate: String,
    onNavigateToSubModule: (String, String) -> Unit,
    onLogout: () -> Unit,
    onConfirmReport: (String, String) -> Unit,
    onReturnReport: (String, String) -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }
    var showReportConfirmDialog by remember { mutableStateOf<DailyReport?>(null) }
    var reviewNotes by remember { mutableStateOf("") }
    var showReturnDialog by remember { mutableStateOf<DailyReport?>(null) }
    var returnReason by remember { mutableStateOf("") }
    var branchMenuExpanded by remember { mutableStateOf(false) }

    // Aggregate Financials
    val hotspotRevenue = sales.filter { it.isPaid }.sumOf { it.totalAmountUgx }
    val gamingRevenue = gamingSessions.filter { it.isPaid && it.status != SessionStatus.CANCELLED_REVERSED }.sumOf { it.totalAmountUgx }
    val techRevenue = techJobs.filter { it.status == JobStatus.COMPLETED }.sumOf { it.serviceChargeUgx }
    val totalRevenue = hotspotRevenue + gamingRevenue + techRevenue
    val totalExpenses = expenses.sumOf { it.amountUgx }
    val netProfit = totalRevenue - totalExpenses

    val activeShifts = shifts.filter { it.status == ShiftStatus.ACTIVE }
    val unreadNotifs = notifications.count { !it.isRead }

    val activeBranch = branches.find { it.id == selectedBranchId } ?: branches.firstOrNull()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            CeoDrawerContent(
                currentUser = currentUser,
                onClose = { scope.launch { drawerState.close() } },
                onNavigate = { title, moduleId ->
                    scope.launch { drawerState.close() }
                    onNavigateToSubModule(title, moduleId)
                },
                onLogout = onLogout
            )
        }
    ) {
        Scaffold(
            containerColor = WangiBackground,
            topBar = {
                WangiTopBar(
                    title = "CEO EXECUTIVE",
                    subtitle = activeBranch?.name ?: "All Branches",
                    timeText = liveTime,
                    unreadNotificationCount = unreadNotifs,
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onNotificationClick = { onNavigateToSubModule("Notifications Center", "notifications") },
                    onLogoutClick = onLogout
                )
            },
            bottomBar = {
                WangiBottomNavigation(
                    currentTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    tab1Label = "Executive",
                    tab2Label = "Operations",
                    tab3Label = "Reports",
                    tab4Label = "Menu",
                    tab1Icon = Icons.Default.Dashboard,
                    tab2Icon = Icons.Default.Hub,
                    tab3Icon = Icons.Default.Assessment,
                    tab4Icon = Icons.Default.MenuOpen
                )
            }
        ) { innerPadding ->
            // Open drawer if Menu tab selected
            LaunchedEffect(selectedTab) {
                if (selectedTab == 3) {
                    drawerState.open()
                    selectedTab = 0
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))

                    // Airtel-Inspired Greeting Card with Branch Selector
                    WangiCard(
                        backgroundColor = WangiWhite
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Welcome Back,",
                                    fontSize = 12.sp,
                                    color = WangiTextSecondary
                                )
                                Text(
                                    text = currentUser.fullName,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WangiCharcoal,
                                    maxLines = 1
                                )
                                Text(
                                    text = liveDate,
                                    fontSize = 11.sp,
                                    color = WangiTextMuted
                                )
                            }

                            // Branch Selector Button
                            Box {
                                OutlinedButton(
                                    onClick = { branchMenuExpanded = true },
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("btn_branch_selector")
                                ) {
                                    Icon(
                                        Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = WangiRed,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = activeBranch?.code ?: "BRANCH",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = WangiCharcoal
                                    )
                                    Icon(
                                        Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = WangiCharcoal
                                    )
                                }

                                DropdownMenu(
                                    expanded = branchMenuExpanded,
                                    onDismissRequest = { branchMenuExpanded = false }
                                ) {
                                    branches.forEach { br ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(br.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                    Text(br.location, fontSize = 11.sp, color = WangiTextSecondary)
                                                }
                                            },
                                            onClick = {
                                                onBranchSelected(br.id)
                                                branchMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    // Main Revenue Hero Card (Airtel-Inspired Red/Gold Highlight)
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = WangiCharcoal),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(WangiGold)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "TOTAL BUSINESS REVENUE",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White.copy(alpha = 0.7f),
                                        letterSpacing = 1.sp
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.White.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = "October 2026",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = WangiGold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "UGX",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WangiGold,
                                    modifier = Modifier.padding(bottom = 4.dp, end = 6.dp)
                                )
                                Text(
                                    text = WangiViewModel.formatUgx(totalRevenue),
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Mini breakdown chips
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color.White.copy(alpha = 0.08f),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("Expenses", fontSize = 10.sp, color = Color.White.copy(alpha = 0.6f))
                                        Text(
                                            "UGX ${WangiViewModel.formatUgx(totalExpenses)}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = WangiRed
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color.White.copy(alpha = 0.08f),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("Net Profit", fontSize = 10.sp, color = Color.White.copy(alpha = 0.6f))
                                        Text(
                                            "UGX ${WangiViewModel.formatUgx(netProfit)}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = WangiSuccess
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Pending Reports Action Banner (Mandatory Workflow)
                if (pendingReports.isNotEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = WangiGoldLight,
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = Brush.linearGradient(listOf(WangiGold, WangiGoldDark))
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(WangiGoldDark),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PriorityHigh,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "${pendingReports.size} Pending Daily Report${if (pendingReports.size > 1) "s" else ""}",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = WangiCharcoal
                                        )
                                        Text(
                                            text = "Awaiting CEO/Manager confirmation",
                                            fontSize = 11.5.sp,
                                            color = WangiTextSecondary
                                        )
                                    }
                                }

                                Button(
                                    onClick = { showReportConfirmDialog = pendingReports.first() },
                                    colors = ButtonDefaults.buttonColors(containerColor = WangiRed),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("Review", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                item {
                    // Quick Action Grid (Inspired by Airtel Quick Actions 4x2)
                    Text(
                        text = "EXECUTIVE QUICK ACTIONS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = WangiTextSecondary,
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    WangiCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            QuickActionItem(
                                icon = Icons.Default.People,
                                label = "Employees",
                                onClick = { onNavigateToSubModule("Employee Management", "employees") }
                            )
                            QuickActionItem(
                                icon = Icons.Default.Wifi,
                                label = "Hotspot",
                                onClick = { onNavigateToSubModule("Hotspot Management", "hotspot") }
                            )
                            QuickActionItem(
                                icon = Icons.Default.SportsEsports,
                                label = "Gaming",
                                onClick = { onNavigateToSubModule("Gaming Station", "gaming") }
                            )
                            QuickActionItem(
                                icon = Icons.Default.Router,
                                label = "Billing",
                                onClick = { onNavigateToSubModule("Wi-Fi Billing", "billing") }
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            QuickActionItem(
                                icon = Icons.Default.Build,
                                label = "Technicians",
                                onClick = { onNavigateToSubModule("Technician Management", "technicians") }
                            )
                            QuickActionItem(
                                icon = Icons.Default.Assessment,
                                label = "Reports",
                                badgeText = if (pendingReports.isNotEmpty()) "${pendingReports.size}" else null,
                                onClick = { onNavigateToSubModule("Daily Reports & Approvals", "reports") }
                            )
                            QuickActionItem(
                                icon = Icons.Default.AccountBalanceWallet,
                                label = "Finances",
                                onClick = { onNavigateToSubModule("Revenue & Profit", "finances") }
                            )
                            QuickActionItem(
                                icon = Icons.Default.Tune,
                                label = "Settings",
                                onClick = { onNavigateToSubModule("Pricing & Commissions", "settings") }
                            )
                        }
                    }
                }

                item {
                    // Three Departments Breakdown Cards
                    Text(
                        text = "DEPARTMENT PERFORMANCE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = WangiTextSecondary,
                        letterSpacing = 0.5.sp
                    )
                }

                item {
                    // Hotspot Summary Card
                    WangiCard(
                        onClick = { onNavigateToSubModule("Hotspot Management", "hotspot") }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(WangiLightRed),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Wifi, contentDescription = null, tint = WangiRed)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Hotspot Business", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("${sales.size} Voucher sales recorded", fontSize = 11.5.sp, color = WangiTextSecondary)
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "UGX ${WangiViewModel.formatUgx(hotspotRevenue)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = WangiCharcoal
                                )
                                Text("Paid revenue", fontSize = 10.sp, color = WangiSuccess)
                            }
                        }
                    }
                }

                item {
                    // Gaming Station Summary Card
                    WangiCard(
                        onClick = { onNavigateToSubModule("Gaming Station", "gaming") }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFFEFF6FF)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.SportsEsports, contentDescription = null, tint = Color(0xFF2563EB))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Gaming Station", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("${gamingSessions.size} Console sessions (PS3/PS4)", fontSize = 11.5.sp, color = WangiTextSecondary)
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "UGX ${WangiViewModel.formatUgx(gamingRevenue)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = WangiCharcoal
                                )
                                Text("Collected cash", fontSize = 10.sp, color = WangiSuccess)
                            }
                        }
                    }
                }

                item {
                    // Technician Summary Card
                    WangiCard(
                        onClick = { onNavigateToSubModule("Technician Management", "technicians") }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFFF0FDFA)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Build, contentDescription = null, tint = Color(0xFF0D9488))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Technical & Network Jobs", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("${techJobs.size} Active tickets & installs", fontSize = 11.5.sp, color = WangiTextSecondary)
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "UGX ${WangiViewModel.formatUgx(techRevenue)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = WangiCharcoal
                                )
                                Text("Service charges", fontSize = 10.sp, color = Color(0xFF0D9488))
                            }
                        }
                    }
                }

                item {
                    // Compact Revenue Trend Chart
                    WangiCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("7-Day Revenue Trend", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("+18.4% vs last week", fontSize = 11.sp, color = WangiSuccess, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp)
                        ) {
                            val points = listOf(0.35f, 0.45f, 0.4f, 0.65f, 0.55f, 0.85f, 1.0f)
                            val stepX = size.width / (points.size - 1)
                            val path = Path()

                            points.forEachIndexed { i, p ->
                                val x = i * stepX
                                val y = size.height - (p * size.height * 0.85f)
                                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            }

                            drawPath(
                                path = path,
                                color = WangiRed,
                                style = Stroke(width = 3.dp.toPx())
                            )

                            // Point circles
                            points.forEachIndexed { i, p ->
                                val x = i * stepX
                                val y = size.height - (p * size.height * 0.85f)
                                drawCircle(
                                    color = WangiGold,
                                    radius = 4.dp.toPx(),
                                    center = Offset(x, y)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            listOf("Thu", "Fri", "Sat", "Sun", "Mon", "Tue", "Today").forEach {
                                Text(it, fontSize = 10.sp, color = WangiTextMuted)
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }

    // Review & Confirm Report Dialog
    if (showReportConfirmDialog != null) {
        val rep = showReportConfirmDialog!!
        AlertDialog(
            onDismissRequest = { showReportConfirmDialog = null },
            title = {
                Text("Confirm Daily Report: ${rep.referenceNumber}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Employee: ${rep.employeeName} (${rep.department})", fontWeight = FontWeight.SemiBold)
                    Text("Date: ${rep.date} • Hours: ${rep.shiftStartTime} - ${rep.shiftEndTime}")
                    Divider()
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Sales:")
                        Text("UGX ${WangiViewModel.formatUgx(rep.totalSalesUgx)}", fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Expected Cash:")
                        Text("UGX ${WangiViewModel.formatUgx(rep.expectedCashUgx)}", fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Actual Cash Received:")
                        Text("UGX ${WangiViewModel.formatUgx(rep.actualCashReceivedUgx)}", fontWeight = FontWeight.Bold, color = WangiSuccess)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Commission Earned:")
                        Text("UGX ${WangiViewModel.formatUgx(rep.commissionEarnedUgx)}", fontWeight = FontWeight.Bold, color = WangiGoldDark)
                    }
                    if (rep.notes.isNotBlank()) {
                        Text("Employee Notes: ${rep.notes}", fontSize = 12.sp, color = WangiTextSecondary)
                    }
                    Divider()
                    Text("Reviewer Notes / Feedback:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = reviewNotes,
                        onValueChange = { reviewNotes = it },
                        placeholder = { Text("e.g. Cash reconciled with physical drawer.") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onConfirmReport(rep.id, reviewNotes)
                        showReportConfirmDialog = null
                        reviewNotes = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WangiSuccess)
                ) {
                    Text("Confirm & Dispatch SMS")
                }
            },
            dismissButton = {
                Row {
                    OutlinedButton(
                        onClick = {
                            showReturnDialog = rep
                            showReportConfirmDialog = null
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = WangiRed)
                    ) {
                        Text("Return")
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    TextButton(onClick = { showReportConfirmDialog = null }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }

    // Return Report Dialog with Mandatory Reason
    if (showReturnDialog != null) {
        val rep = showReturnDialog!!
        AlertDialog(
            onDismissRequest = { showReturnDialog = null },
            title = {
                Text("Return Report for Correction", fontWeight = FontWeight.Bold, color = WangiRed)
            },
            text = {
                Column {
                    Text("Enter the specific discrepancy or reason for the employee to correct:")
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = returnReason,
                        onValueChange = { returnReason = it },
                        placeholder = { Text("e.g. Missing UGX 2,000 voucher count. Please re-check drawer.") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (returnReason.isNotBlank()) {
                            onReturnReport(rep.id, returnReason)
                            showReturnDialog = null
                            returnReason = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WangiRed),
                    enabled = returnReason.isNotBlank()
                ) {
                    Text("Send Return Notice")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReturnDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun CeoDrawerContent(
    currentUser: User,
    onClose: () -> Unit,
    onNavigate: (String, String) -> Unit,
    onLogout: () -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = WangiWhite,
        modifier = Modifier.width(320.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Drawer Header
            Surface(
                color = WangiCharcoal,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(WangiGold),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.MilitaryTech, contentDescription = null, tint = WangiCharcoal)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(currentUser.fullName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Chief Executive Officer", color = WangiGold, fontSize = 11.5.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "WANGI TECH SOLUTIONS",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Scrollable Menu Items (All 18 modules from the requirements)
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp)
            ) {
                val menuItems = listOf(
                    Triple("Employee Management", "employees", Icons.Default.People),
                    Triple("Manager Management", "managers", Icons.Default.SupervisorAccount),
                    Triple("Technician Management", "technicians", Icons.Default.Build),
                    Triple("Branch Management", "branches", Icons.Default.Store),
                    Triple("Hotspot Management", "hotspot", Icons.Default.Wifi),
                    Triple("Gaming Management", "gaming", Icons.Default.SportsEsports),
                    Triple("Wi-Fi Billing System", "billing", Icons.Default.Router),
                    Triple("Pricing & Commissions", "settings", Icons.Default.PriceChange),
                    Triple("Daily Reports & Approvals", "reports", Icons.Default.Assessment),
                    Triple("Cash Reconciliation", "cash", Icons.Default.PointOfSale),
                    Triple("Customer Debts", "debts", Icons.Default.MoneyOff),
                    Triple("Salaries & Advances", "salaries", Icons.Default.Payments),
                    Triple("Expenses", "expenses", Icons.Default.ReceiptLong),
                    Triple("Revenue & Profit", "finances", Icons.Default.TrendingUp),
                    Triple("Voucher Inventory", "vouchers", Icons.Default.ConfirmationNumber),
                    Triple("Notifications Center", "notifications", Icons.Default.Notifications),
                    Triple("Audit Logs", "audit", Icons.Default.History),
                    Triple("Security & Backup", "security", Icons.Default.Security)
                )

                items(menuItems) { (title, id, icon) ->
                    NavigationDrawerItem(
                        icon = { Icon(icon, contentDescription = null, tint = WangiCharcoal) },
                        label = { Text(title, fontSize = 13.5.sp, fontWeight = FontWeight.Medium) },
                        selected = false,
                        onClick = { onNavigate(title, id) },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                    )
                }
            }

            Divider()

            // Logout at bottom
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.ExitToApp, contentDescription = null, tint = WangiRed) },
                label = { Text("Sign Out", color = WangiRed, fontWeight = FontWeight.Bold) },
                selected = false,
                onClick = onLogout,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}
