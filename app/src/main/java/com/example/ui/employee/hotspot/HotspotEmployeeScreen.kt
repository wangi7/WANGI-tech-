package com.example.ui.employee.hotspot

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

@Composable
fun HotspotEmployeeScreen(
    currentUser: User,
    branch: Branch?,
    shifts: List<Shift>,
    sales: List<VoucherSale>,
    debts: List<CustomerDebt>,
    reports: List<DailyReport>,
    notifications: List<NotificationItem>,
    liveTime: String,
    liveDate: String,
    onClockIn: (String, Boolean, String) -> Unit,
    onClockOut: (String) -> Unit,
    onRecordSale: (String, String, Long, Int, Boolean, String) -> Unit,
    onSettleDebt: (String, Long) -> Unit,
    onSubmitReport: (String, String, Long, Long, Long, Long, Long, Long, Long, String) -> Unit,
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val myShifts = shifts.filter { it.employeeId == currentUser.id }
    val activeShift = myShifts.find { it.status == ShiftStatus.ACTIVE }
    val mySales = sales.filter { it.employeeId == currentUser.id }
    val myDebts = debts.filter { it.recordedByEmployeeId == currentUser.id }
    val myReports = reports.filter { it.employeeId == currentUser.id }

    var showNewSaleDialog by remember { mutableStateOf(false) }
    var showClockInDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }

    // Sale Form State
    var custName by remember { mutableStateOf("") }
    var custPhone by remember { mutableStateOf("") }
    var selectedDenom by remember { mutableLongStateOf(1000L) }
    var saleQty by remember { mutableIntStateOf(1) }
    var isCashPaid by remember { mutableStateOf(true) }
    var saleNotes by remember { mutableStateOf("") }

    // Clock In Form State
    var shiftStartTimeStr by remember { mutableStateOf("08:00 AM") }
    var uniformStatus by remember { mutableStateOf(true) }

    // Report Form State
    var reportExpensesText by remember { mutableStateOf("0") }
    var reportActualCashText by remember { mutableStateOf("") }
    var reportNotes by remember { mutableStateOf("") }

    val totalSoldCash = mySales.filter { it.isPaid }.sumOf { it.totalAmountUgx }
    val totalUnpaidCredit = mySales.filter { !it.isPaid }.sumOf { it.totalAmountUgx }
    val currentRate = activeShift?.commissionRatePercent ?: 25.0
    val earnedComm = (totalSoldCash * (currentRate / 100.0)).toLong()

    Scaffold(
        containerColor = WangiBackground,
        topBar = {
            WangiTopBar(
                title = "HOTSPOT WORKSPACE",
                subtitle = "${currentUser.fullName} • ${branch?.name ?: "Kampala"}",
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
                    tab1Label = "Vouchers",
                    tab2Label = "Shift & Clock",
                    tab3Label = "Credit/Debts",
                    tab4Label = "Daily Report",
                    tab1Icon = Icons.Default.ConfirmationNumber,
                    tab2Icon = Icons.Default.AccessTime,
                    tab3Icon = Icons.Default.MoneyOff,
                    tab4Icon = Icons.Default.Description
                )
            }
        },
        floatingActionButton = {
            if (selectedTab == 0 && activeShift != null) {
                FloatingActionButton(
                    onClick = { showNewSaleDialog = true },
                    containerColor = WangiRed,
                    contentColor = WangiWhite,
                    modifier = Modifier.testTag("fab_new_voucher_sale")
                ) {
                    Icon(Icons.Default.AddShoppingCart, contentDescription = "New Voucher Sale")
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
                        departmentName = "Wi-Fi Hotspot",
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
                                    text = "ACTIVE SHIFT • ON DUTY",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WangiSuccess
                                )
                                Text("Clocked in at ${activeShift.actualClockInTime} • Rate: ${activeShift.commissionRatePercent}%", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                if (!activeShift.uniformCompliant) {
                                    Text("5% Uniform deduction applied", fontSize = 11.sp, color = WangiWarning)
                                }
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

                // Financial Summary Metrics
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        WangiCard(
                            modifier = Modifier.weight(1f),
                            backgroundColor = WangiWhite
                        ) {
                            Text("Today's Paid Cash", fontSize = 11.sp, color = WangiTextSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "UGX ${WangiViewModel.formatUgx(totalSoldCash)}",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = WangiCharcoal
                            )
                        }

                        WangiCard(
                            modifier = Modifier.weight(1f),
                            backgroundColor = WangiGoldLight
                        ) {
                            Text("My Commission", fontSize = 11.sp, color = WangiTextSecondary)
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
                    // Voucher Denominations & Sales Log
                    item {
                        Text(
                            text = "AVAILABLE VOUCHER PACKAGES",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = WangiTextSecondary,
                            letterSpacing = 0.5.sp
                        )
                    }

                    item {
                        // Quick Package Pills
                        val denominations = listOf(500L, 1000L, 2000L, 3000L, 5000L, 9000L, 20000L, 50000L)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            denominations.take(4).forEach { denom ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = WangiWhite,
                                    border = CardDefaults.outlinedCardBorder(),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            selectedDenom = denom
                                            showNewSaleDialog = true
                                        }
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("${denom / 1000}k", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = WangiRed)
                                        Text("UGX $denom", fontSize = 10.sp, color = WangiTextSecondary)
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "RECENT VOUCHER SALES (${mySales.size})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = WangiTextSecondary
                            )
                            Text("Total: UGX ${WangiViewModel.formatUgx(totalSoldCash + totalUnpaidCredit)}", fontSize = 11.sp, color = WangiCharcoal, fontWeight = FontWeight.Bold)
                        }
                    }

                    items(mySales) { sale ->
                        WangiCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("${sale.quantity}x UGX ${WangiViewModel.formatUgx(sale.denominationUgx)} Voucher", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("Customer: ${sale.customerName} (${sale.customerPhone})", fontSize = 12.sp, color = WangiTextSecondary)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        "UGX ${WangiViewModel.formatUgx(sale.totalAmountUgx)}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (sale.isPaid) WangiCharcoal else WangiRed
                                    )
                                    StatusBadge(status = if (sale.isPaid) "PAID CASH" else "UNPAID DEBT")
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // Shift Details & History
                    item {
                        Text("MY SHIFT RECORDS", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    items(myShifts) { shf ->
                        WangiCard {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("Date: ${shf.date}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("Requested: ${shf.requestedStartTime} • Actual: ${shf.actualClockInTime ?: "N/A"}", fontSize = 12.sp, color = WangiTextSecondary)
                                    Text("Commission Rate: ${shf.commissionRatePercent}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = WangiGoldDark)
                                }
                                StatusBadge(status = shf.status.name)
                            }
                        }
                    }
                }

                2 -> {
                    // Customer Debts & Credit Logs
                    item {
                        Text("CUSTOMER DEBTS & CREDIT SALES (${myDebts.size})", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    items(myDebts) { d ->
                        WangiCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(d.customerName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("Ref: ${d.referenceCode} • ${d.customerPhone}", fontSize = 11.5.sp, color = WangiTextSecondary)
                                    Text("Due: UGX ${WangiViewModel.formatUgx(d.amountUgx - d.amountPaidUgx)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = WangiRed)
                                }
                                if (!d.isPaid) {
                                    Button(
                                        onClick = { onSettleDebt(d.id, d.amountUgx - d.amountPaidUgx) },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = WangiSuccess),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text("Collect", fontSize = 11.sp)
                                    }
                                } else {
                                    StatusBadge("SETTLED")
                                }
                            }
                        }
                    }
                }

                3 -> {
                    // Daily Report Submission & Status
                    item {
                        WangiCard {
                            Text("Submit Shift Daily Report", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Reconcile sales, credit, expenses, and submit to Manager/CEO for confirmation.", fontSize = 12.sp, color = WangiTextSecondary)
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    reportActualCashText = totalSoldCash.toString()
                                    showReportDialog = true
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("btn_open_report_dialog"),
                                colors = ButtonDefaults.buttonColors(containerColor = WangiRed),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Prepare & Submit Daily Report", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    item {
                        Text("SUBMITTED REPORTS HISTORY (${myReports.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = WangiTextSecondary)
                    }

                    items(myReports) { rep ->
                        WangiCard {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(rep.referenceNumber, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    StatusBadge(status = rep.status.name)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Date: ${rep.date} • Expected: UGX ${WangiViewModel.formatUgx(rep.expectedCashUgx)}", fontSize = 12.sp)
                                Text("Actual Cash: UGX ${WangiViewModel.formatUgx(rep.actualCashReceivedUgx)} • Comm: UGX ${WangiViewModel.formatUgx(rep.commissionEarnedUgx)}", fontSize = 12.sp, color = WangiGoldDark, fontWeight = FontWeight.SemiBold)
                                if (rep.reviewerNotes != null) {
                                    Text("Reviewer: ${rep.reviewerNotes}", fontSize = 11.5.sp, color = WangiSuccess)
                                }
                                if (rep.reviewReason != null) {
                                    Text("Returned Reason: ${rep.reviewReason}", fontSize = 11.5.sp, color = WangiRed)
                                }
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

    // New Voucher Sale Dialog
    if (showNewSaleDialog) {
        AlertDialog(
            onDismissRequest = { showNewSaleDialog = false },
            title = { Text("Record Voucher Sale", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select Denomination (UGX):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    val denoms = listOf(500L, 1000L, 2000L, 3000L, 5000L, 9000L, 20000L)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        denoms.take(4).forEach { d ->
                            FilterChip(
                                selected = selectedDenom == d,
                                onClick = { selectedDenom = d },
                                label = { Text("${d / 1000}k", fontSize = 11.sp) }
                            )
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        denoms.drop(4).forEach { d ->
                            FilterChip(
                                selected = selectedDenom == d,
                                onClick = { selectedDenom = d },
                                label = { Text("${d / 1000}k", fontSize = 11.sp) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = custName,
                        onValueChange = { custName = it },
                        label = { Text("Customer Name (or Walk-in)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = custPhone,
                        onValueChange = { custPhone = it },
                        label = { Text("Customer Phone") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isCashPaid, onCheckedChange = { isCashPaid = it })
                        Text("Paid Cash Immediately (Uncheck if Credit/Debt)", fontSize = 12.5.sp)
                    }

                    Text(
                        "Total Amount: UGX ${WangiViewModel.formatUgx(selectedDenom * saleQty)}",
                        fontWeight = FontWeight.Bold,
                        color = WangiCharcoal,
                        fontSize = 15.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRecordSale(custName, custPhone, selectedDenom, saleQty, isCashPaid, saleNotes)
                        showNewSaleDialog = false
                        custName = ""
                        custPhone = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WangiRed)
                ) {
                    Text("Save Sale")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewSaleDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Clock In Dialog
    if (showClockInDialog) {
        AlertDialog(
            onDismissRequest = { showClockInDialog = false },
            title = { Text("Clock In For Shift", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Select your arrival time to calculate commission:", fontSize = 12.sp)
                    listOf("08:00 AM (25%)", "09:00 AM (20%)", "10:00 AM (15%)", "11:00 AM (10%)").forEach { opt ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { shiftStartTimeStr = opt.substringBefore(" (") }
                        ) {
                            RadioButton(selected = shiftStartTimeStr == opt.substringBefore(" ("), onClick = { shiftStartTimeStr = opt.substringBefore(" (") })
                            Text(opt, fontSize = 13.sp)
                        }
                    }
                    Divider()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = uniformStatus, onCheckedChange = { uniformStatus = it })
                        Text("Uniform Compliant (-5% if not worn)", fontSize = 12.5.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onClockIn(shiftStartTimeStr, uniformStatus, "Shift clocked via mobile app")
                        showClockInDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WangiSuccess)
                ) {
                    Text("Confirm Clock In")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClockInDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Submit Daily Report Dialog
    if (showReportDialog) {
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = { Text("Submit Daily Report", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Total Sales: UGX ${WangiViewModel.formatUgx(totalSoldCash + totalUnpaidCredit)}")
                    Text("Paid Cash: UGX ${WangiViewModel.formatUgx(totalSoldCash)}")
                    Text("Unpaid Debts: UGX ${WangiViewModel.formatUgx(totalUnpaidCredit)}")
                    Text("Commission: UGX ${WangiViewModel.formatUgx(earnedComm)}", color = WangiGoldDark, fontWeight = FontWeight.Bold)
                    Divider()
                    OutlinedTextField(
                        value = reportActualCashText,
                        onValueChange = { reportActualCashText = it },
                        label = { Text("Actual Cash Received (UGX)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = reportExpensesText,
                        onValueChange = { reportExpensesText = it },
                        label = { Text("Deductible Expenses (UGX)") },
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
                        val actual = reportActualCashText.toLongOrNull() ?: totalSoldCash
                        val exp = reportExpensesText.toLongOrNull() ?: 0L
                        onSubmitReport(
                            activeShift?.requestedStartTime ?: "08:00 AM",
                            liveTime,
                            totalSoldCash + totalUnpaidCredit,
                            totalSoldCash,
                            totalUnpaidCredit,
                            totalSoldCash,
                            actual,
                            exp,
                            earnedComm,
                            reportNotes
                        )
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
