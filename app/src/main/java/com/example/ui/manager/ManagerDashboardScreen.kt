package com.example.ui.manager

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
fun ManagerDashboardScreen(
    currentUser: User,
    branch: Branch?,
    users: List<User>,
    shifts: List<Shift>,
    pendingReports: List<DailyReport>,
    sales: List<VoucherSale>,
    gamingSessions: List<GamingSession>,
    notifications: List<NotificationItem>,
    liveTime: String,
    liveDate: String,
    onConfirmReport: (String, String) -> Unit,
    onReturnReport: (String, String) -> Unit,
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var reviewReportDialog by remember { mutableStateOf<DailyReport?>(null) }
    var reviewNotes by remember { mutableStateOf("") }
    var returnReason by remember { mutableStateOf("") }
    var showReturnDialog by remember { mutableStateOf<DailyReport?>(null) }

    val activeShifts = shifts.filter { it.status == ShiftStatus.ACTIVE }
    val totalPendingCash = pendingReports.sumOf { it.actualCashReceivedUgx }
    val unreadNotifs = notifications.count { !it.isRead }

    Scaffold(
        containerColor = WangiBackground,
        topBar = {
            WangiTopBar(
                title = "MANAGER PORTAL",
                subtitle = "${branch?.name ?: "Kampala Central"} (${branch?.code ?: "KLA-01"})",
                timeText = liveTime,
                unreadNotificationCount = unreadNotifs,
                onLogoutClick = onLogout
            )
        },
        bottomBar = {
            WangiBottomNavigation(
                currentTab = selectedTab,
                onTabSelected = { selectedTab = it },
                tab1Label = "Dashboard",
                tab2Label = "Staff Shifts",
                tab3Label = "Approvals",
                tab4Label = "Sales Audit",
                tab1Icon = Icons.Default.Dashboard,
                tab2Icon = Icons.Default.Schedule,
                tab3Icon = Icons.Default.FactCheck,
                tab4Icon = Icons.Default.PointOfSale
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))

                // Manager Greeting Card
                WangiCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Manager On Duty:", fontSize = 11.5.sp, color = WangiTextSecondary)
                            Text(currentUser.fullName, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = WangiCharcoal)
                            Text(liveDate, fontSize = 11.sp, color = WangiTextMuted)
                        }
                        StatusBadge("SUPERVISOR")
                    }
                }
            }

            // Quick Stats Banner
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    WangiCard(
                        modifier = Modifier.weight(1f),
                        backgroundColor = WangiGoldLight
                    ) {
                        Text("Cash Awaiting Audit", fontSize = 11.sp, color = WangiTextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "UGX ${WangiViewModel.formatUgx(totalPendingCash)}",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = WangiGoldDark
                        )
                    }

                    WangiCard(
                        modifier = Modifier.weight(1f),
                        backgroundColor = WangiSuccessLight
                    ) {
                        Text("Active On-Shift", fontSize = 11.sp, color = WangiTextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "${activeShifts.size} Staff",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = WangiSuccess
                        )
                    }
                }
            }

            // Pending Daily Reports Requiring Action
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "REPORTS REQUIRING CONFIRMATION",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = WangiTextSecondary,
                        letterSpacing = 0.5.sp
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (pendingReports.isNotEmpty()) WangiLightRed else WangiSuccessLight
                    ) {
                        Text(
                            text = "${pendingReports.size} Pending",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (pendingReports.isNotEmpty()) WangiRed else WangiSuccess,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            if (pendingReports.isEmpty()) {
                item {
                    WangiCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = WangiSuccess)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("All submitted employee reports are confirmed and up to date.", fontSize = 13.sp, color = WangiTextSecondary)
                        }
                    }
                }
            } else {
                items(pendingReports) { rep ->
                    WangiCard {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(rep.referenceNumber, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("${rep.employeeName} (${rep.department})", fontSize = 12.sp, color = WangiTextSecondary)
                                }
                                StatusBadge(status = rep.status.name)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Expected: UGX ${WangiViewModel.formatUgx(rep.expectedCashUgx)}", fontSize = 12.sp)
                                Text("Actual: UGX ${WangiViewModel.formatUgx(rep.actualCashReceivedUgx)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = WangiSuccess)
                            }

                            val diff = rep.shortageOrSurplusUgx
                            if (diff != 0L) {
                                Text(
                                    text = "Discrepancy: ${if (diff > 0) "+ " else ""}${WangiViewModel.formatUgx(diff)} UGX",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (diff > 0) WangiGoldDark else WangiRed
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                OutlinedButton(
                                    onClick = { showReturnDialog = rep },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = WangiRed)
                                ) {
                                    Text("Return for Correction", fontSize = 11.sp)
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Button(
                                    onClick = { reviewReportDialog = rep },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = WangiSuccess)
                                ) {
                                    Text("Verify & Confirm", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Active Employees & Shift Details
            item {
                Text(
                    text = "BRANCH EMPLOYEES & SHIFT STATUS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = WangiTextSecondary,
                    letterSpacing = 0.5.sp
                )
            }

            items(users.filter { it.role == UserRole.HOTSPOT_EMPLOYEE || it.role == UserRole.GAMING_EMPLOYEE }) { emp ->
                val currentShift = shifts.find { it.employeeId == emp.id && it.status == ShiftStatus.ACTIVE }
                WangiCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(if (currentShift != null) WangiSuccessLight else WangiSurfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (emp.department == Department.HOTSPOT) Icons.Default.Wifi else Icons.Default.SportsEsports,
                                    contentDescription = null,
                                    tint = if (currentShift != null) WangiSuccess else WangiTextMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(emp.fullName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Dept: ${emp.department} • Phone: ${emp.phone}", fontSize = 11.5.sp, color = WangiTextSecondary)
                                if (currentShift != null) {
                                    Text("Clocked in: ${currentShift.actualClockInTime ?: "Today"} (Rate: ${currentShift.commissionRatePercent}%)", fontSize = 11.sp, color = WangiSuccess)
                                }
                            }
                        }

                        StatusBadge(status = if (currentShift != null) "ON SHIFT" else "OFF DUTY")
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Verify & Confirm Report Dialog
    if (reviewReportDialog != null) {
        val rep = reviewReportDialog!!
        AlertDialog(
            onDismissRequest = { reviewReportDialog = null },
            title = { Text("Manager Confirmation: ${rep.referenceNumber}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Staff: ${rep.employeeName} (${rep.department})")
                    Text("Date: ${rep.date} • Period: ${rep.shiftStartTime} - ${rep.shiftEndTime}")
                    Divider()
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Expected Cash:")
                        Text("UGX ${WangiViewModel.formatUgx(rep.expectedCashUgx)}", fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Actual Cash:")
                        Text("UGX ${WangiViewModel.formatUgx(rep.actualCashReceivedUgx)}", fontWeight = FontWeight.Bold, color = WangiSuccess)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Commission to Credit:")
                        Text("UGX ${WangiViewModel.formatUgx(rep.commissionEarnedUgx)}", fontWeight = FontWeight.Bold, color = WangiGoldDark)
                    }
                    Divider()
                    Text("Manager Comments & Instructions:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = reviewNotes,
                        onValueChange = { reviewNotes = it },
                        placeholder = { Text("Physical drawer reconciled. Approved.") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onConfirmReport(rep.id, reviewNotes)
                        reviewReportDialog = null
                        reviewNotes = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WangiSuccess)
                ) {
                    Text("Confirm & Send SMS")
                }
            },
            dismissButton = {
                TextButton(onClick = { reviewReportDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Return for Correction Dialog
    if (showReturnDialog != null) {
        val rep = showReturnDialog!!
        AlertDialog(
            onDismissRequest = { showReturnDialog = null },
            title = { Text("Return Report for Correction", fontWeight = FontWeight.Bold, color = WangiRed) },
            text = {
                Column {
                    Text("Enter required corrections for ${rep.employeeName}:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = returnReason,
                        onValueChange = { returnReason = it },
                        placeholder = { Text("e.g. Unreconciled UGX 5,000 voucher. Verify returning inventory.") },
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
                    Text("Return with Feedback")
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
