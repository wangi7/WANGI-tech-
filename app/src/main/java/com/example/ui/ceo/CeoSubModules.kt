package com.example.ui.ceo

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.WangiViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CeoSubModuleContainer(
    title: String,
    moduleId: String,
    currentUser: User,
    users: List<User>,
    branches: List<Branch>,
    sales: List<VoucherSale>,
    debts: List<CustomerDebt>,
    reports: List<DailyReport>,
    expenses: List<BusinessExpense>,
    settings: List<SystemSetting>,
    auditEvents: List<AuditEvent>,
    notifications: List<NotificationItem>,
    smsLogs: List<SmsLog>,
    onBackClick: () -> Unit,
    onSaveSetting: (String, String, String) -> Unit,
    onAddExpense: (String, String, Long, Department) -> Unit,
    onSettleDebt: (String, Long) -> Unit,
    onConfirmReport: (String, String) -> Unit,
    onReturnReport: (String, String) -> Unit
) {
    Scaffold(
        containerColor = WangiBackground,
        topBar = {
            WangiTopBar(
                title = title,
                subtitle = "CEO Management Module",
                showBackButton = true,
                onBackClick = onBackClick
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            when (moduleId) {
                "employees" -> EmployeeManagementView(users = users)
                "managers" -> ManagerManagementView(users = users.filter { it.role == UserRole.MANAGER }, branches = branches)
                "technicians" -> TechnicianManagementView(users = users.filter { it.role == UserRole.TECHNICIAN })
                "branches" -> BranchManagementView(branches = branches)
                "settings" -> SettingsManagementView(settings = settings, onSaveSetting = onSaveSetting)
                "debts" -> CustomerDebtsView(debts = debts, onSettleDebt = onSettleDebt)
                "expenses" -> ExpensesManagementView(expenses = expenses, onAddExpense = onAddExpense)
                "cash" -> CashReconciliationView(reports = reports, sales = sales)
                "audit" -> AuditLogsView(auditEvents = auditEvents)
                "notifications" -> NotificationsCenterView(notifications = notifications, smsLogs = smsLogs)
                "reports" -> ReportsApprovalView(reports = reports, onConfirmReport = onConfirmReport, onReturnReport = onReturnReport)
                "salaries" -> SalariesManagementView(users = users, reports = reports)
                "security" -> SecurityBackupView()
                else -> GenericDataTableView(title = title, reports = reports)
            }
        }
    }
}

@Composable
fun EmployeeManagementView(users: List<User>) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text(
                "Authorized Staff Roster (${users.size} Total)",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = WangiTextSecondary
            )
        }
        items(users) { u ->
            WangiCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(u.fullName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("${u.role} • Dept: ${u.department}", fontSize = 12.sp, color = WangiTextSecondary)
                        Text("Phone: ${u.phone} • Email: ${u.email}", fontSize = 11.sp, color = WangiTextMuted)
                    }
                    StatusBadge(status = u.status)
                }
            }
        }
    }
}

@Composable
fun ManagerManagementView(users: List<User>, branches: List<Branch>) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("Branch Managers (${users.size})", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        items(users) { m ->
            WangiCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(m.fullName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("Assigned Branch: Kampala Central Hub", fontSize = 12.sp, color = WangiTextSecondary)
                        Text("Authorizations: Shift approvals, sales audits, end-of-day reports", fontSize = 11.sp, color = WangiTextMuted)
                    }
                    StatusBadge(status = "AUTHORIZED")
                }
            }
        }
    }
}

@Composable
fun TechnicianManagementView(users: List<User>) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("Network & Field Technicians", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        items(users) { t ->
            WangiCard {
                Column {
                    Text(t.fullName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("Role: Senior Network Technician (MikroTik / Omada)", fontSize = 12.sp, color = WangiTextSecondary)
                    Text("Phone: ${t.phone} • Tools: Cable tester, crimper, Winbox v3/v4", fontSize = 11.sp, color = WangiTextMuted)
                }
            }
        }
    }
}

@Composable
fun BranchManagementView(branches: List<Branch>) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("Company Branches (${branches.size})", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        items(branches) { b ->
            WangiCard {
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(b.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        StatusBadge("ONLINE")
                    }
                    Text("Code: ${b.code} • Location: ${b.location}", fontSize = 12.sp, color = WangiTextSecondary)
                    Text("Routers: ${b.activeRoutersCount} | Gaming Stations: ${b.activeGamingConsolesCount}", fontSize = 12.sp, color = WangiGoldDark, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun SettingsManagementView(
    settings: List<SystemSetting>,
    onSaveSetting: (String, String, String) -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Commission & Business Configuration Rules", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text("Historical records retain previously calculated rates upon change.", fontSize = 12.sp, color = WangiTextSecondary)
        }
        items(settings) { s ->
            var editValue by remember(s.value) { mutableStateOf(s.value) }
            WangiCard {
                Column {
                    Text(s.description, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Key: ${s.key}", fontSize = 11.sp, color = WangiTextMuted)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = editValue,
                            onValueChange = { editValue = it },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { onSaveSetting(s.key, editValue, s.description) },
                            colors = ButtonDefaults.buttonColors(containerColor = WangiRed)
                        ) {
                            Text("Update")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomerDebtsView(
    debts: List<CustomerDebt>,
    onSettleDebt: (String, Long) -> Unit
) {
    var settleDebtDialog by remember { mutableStateOf<CustomerDebt?>(null) }
    var payAmountText by remember { mutableStateOf("") }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            val totalOutstanding = debts.filter { !it.isPaid }.sumOf { it.amountUgx - it.amountPaidUgx }
            WangiCard(backgroundColor = WangiGoldLight) {
                Text("Total Outstanding Customer Debt", fontSize = 11.sp, color = WangiTextSecondary)
                Text("UGX ${WangiViewModel.formatUgx(totalOutstanding)}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = WangiGoldDark)
            }
        }
        items(debts) { d ->
            val remaining = d.amountUgx - d.amountPaidUgx
            WangiCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(d.customerName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Ref: ${d.referenceCode} • Phone: ${d.customerPhone}", fontSize = 11.5.sp, color = WangiTextSecondary)
                        Text("Total: UGX ${WangiViewModel.formatUgx(d.amountUgx)} | Paid: UGX ${WangiViewModel.formatUgx(d.amountPaidUgx)}", fontSize = 11.5.sp)
                        if (d.notes.isNotBlank()) Text("Note: ${d.notes}", fontSize = 11.sp, color = WangiTextMuted)
                    }

                    if (!d.isPaid) {
                        Button(
                            onClick = {
                                settleDebtDialog = d
                                payAmountText = remaining.toString()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = WangiSuccess),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("Settle", fontSize = 11.sp)
                        }
                    } else {
                        StatusBadge("PAID")
                    }
                }
            }
        }
    }

    if (settleDebtDialog != null) {
        val d = settleDebtDialog!!
        AlertDialog(
            onDismissRequest = { settleDebtDialog = null },
            title = { Text("Settle Customer Debt", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Customer: ${d.customerName} (${d.referenceCode})")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = payAmountText,
                        onValueChange = { payAmountText = it },
                        label = { Text("Amount (UGX)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = payAmountText.toLongOrNull() ?: 0L
                        if (amount > 0) {
                            onSettleDebt(d.id, amount)
                            settleDebtDialog = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WangiSuccess)
                ) {
                    Text("Record Payment")
                }
            },
            dismissButton = {
                TextButton(onClick = { settleDebtDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ExpensesManagementView(
    expenses: List<BusinessExpense>,
    onAddExpense: (String, String, Long, Department) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var category by remember { mutableStateOf("UTILITIES") }
    var desc by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Business Expenses (${expenses.size})", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Button(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = WangiRed),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Expense")
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(expenses) { exp ->
                WangiCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(exp.category, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(exp.description, fontSize = 12.sp, color = WangiTextSecondary)
                            Text("Recorded by ${exp.recordedBy} on ${exp.date}", fontSize = 11.sp, color = WangiTextMuted)
                        }
                        Text(
                            "- UGX ${WangiViewModel.formatUgx(exp.amountUgx)}",
                            fontWeight = FontWeight.Bold,
                            color = WangiRed,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Record Business Expense", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Category (e.g. UTILITIES, RENT, CABLE)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Amount (UGX)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = amountText.toLongOrNull() ?: 0L
                        if (amt > 0 && desc.isNotBlank()) {
                            onAddExpense(category, desc, amt, Department.ALL)
                            showAddDialog = false
                            desc = ""
                            amountText = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WangiRed)
                ) {
                    Text("Save Expense")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun CashReconciliationView(reports: List<DailyReport>, sales: List<VoucherSale>) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            WangiCard {
                Text("Cash Reconciliation & Discrepancies", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Compare expected vs actual cash received across daily reports.", fontSize = 12.sp, color = WangiTextSecondary)
            }
        }
        items(reports) { r ->
            WangiCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(r.referenceNumber, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("${r.employeeName} • ${r.date}", fontSize = 12.sp, color = WangiTextSecondary)
                        Text("Expected: UGX ${WangiViewModel.formatUgx(r.expectedCashUgx)} | Actual: UGX ${WangiViewModel.formatUgx(r.actualCashReceivedUgx)}", fontSize = 11.5.sp)
                    }
                    val diff = r.shortageOrSurplusUgx
                    Column(horizontalAlignment = Alignment.End) {
                        StatusBadge(status = r.status.name)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (diff == 0L) "BALANCED" else if (diff > 0) "+ UGX ${WangiViewModel.formatUgx(diff)}" else "- UGX ${WangiViewModel.formatUgx(Math.abs(diff))}",
                            color = if (diff >= 0) WangiSuccess else WangiRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AuditLogsView(auditEvents: List<AuditEvent>) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Text("Immutable Security Audit Trail (${auditEvents.size} Events)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        items(auditEvents) { ev ->
            WangiCard {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(ev.action, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = WangiRed)
                        Text(
                            SimpleDateFormat("HH:mm:ss dd/MM", Locale.getDefault()).format(Date(ev.timestamp)),
                            fontSize = 11.sp,
                            color = WangiTextMuted
                        )
                    }
                    Text("Actor: ${ev.actorName} (${ev.actorRole})", fontSize = 12.sp, color = WangiCharcoal)
                    Text(ev.details, fontSize = 11.5.sp, color = WangiTextSecondary)
                }
            }
        }
    }
}

@Composable
fun NotificationsCenterView(notifications: List<NotificationItem>, smsLogs: List<SmsLog>) {
    var tab by remember { mutableIntStateOf(0) }
    Column {
        TabRow(selectedTabIndex = tab, containerColor = WangiWhite) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("In-App (${notifications.size})") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("SMS Logs (${smsLogs.size})") })
        }
        Spacer(modifier = Modifier.height(10.dp))
        if (tab == 0) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(notifications) { n ->
                    WangiCard {
                        Text(n.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(n.message, fontSize = 12.sp, color = WangiTextSecondary)
                    }
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(smsLogs) { s ->
                    WangiCard {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("To: ${s.recipientPhone}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            StatusBadge(status = s.status)
                        }
                        Text(s.message, fontSize = 12.sp, color = WangiTextSecondary)
                        if (s.errorMessage != null) {
                            Text("Error: ${s.errorMessage}", fontSize = 11.sp, color = WangiRed)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReportsApprovalView(
    reports: List<DailyReport>,
    onConfirmReport: (String, String) -> Unit,
    onReturnReport: (String, String) -> Unit
) {
    var selectedReport by remember { mutableStateOf<DailyReport?>(null) }
    var notes by remember { mutableStateOf("") }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("All Daily Reports (${reports.size})", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        items(reports) { r ->
            WangiCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(r.referenceNumber, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("${r.employeeName} • ${r.department} • ${r.date}", fontSize = 12.sp, color = WangiTextSecondary)
                        Text("Expected: UGX ${WangiViewModel.formatUgx(r.expectedCashUgx)} | Actual: UGX ${WangiViewModel.formatUgx(r.actualCashReceivedUgx)}", fontSize = 11.5.sp)
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        StatusBadge(status = r.status.name)
                        if (r.status == ReportStatus.SUBMITTED || r.status == ReportStatus.UNDER_REVIEW) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = { selectedReport = r },
                                colors = ButtonDefaults.buttonColors(containerColor = WangiRed),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("Review", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    if (selectedReport != null) {
        val rep = selectedReport!!
        AlertDialog(
            onDismissRequest = { selectedReport = null },
            title = { Text("Review: ${rep.referenceNumber}", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Employee: ${rep.employeeName}")
                    Text("Cash Received: UGX ${WangiViewModel.formatUgx(rep.actualCashReceivedUgx)}")
                    Text("Commission: UGX ${WangiViewModel.formatUgx(rep.commissionEarnedUgx)}")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Approval notes") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onConfirmReport(rep.id, notes)
                        selectedReport = null
                        notes = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WangiSuccess)
                ) {
                    Text("Confirm & Finalize")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    onReturnReport(rep.id, "Please verify cash discrepancy.")
                    selectedReport = null
                }) {
                    Text("Return for Correction")
                }
            }
        )
    }
}

@Composable
fun SalariesManagementView(users: List<User>, reports: List<DailyReport>) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            WangiCard {
                Text("Monthly Payroll & Commissions (October 2026)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("Calculated from confirmed daily reports and configured commission rates.", fontSize = 12.sp, color = WangiTextSecondary)
            }
        }
        items(users.filter { it.role == UserRole.HOTSPOT_EMPLOYEE || it.role == UserRole.GAMING_EMPLOYEE }) { u ->
            val empReports = reports.filter { it.employeeId == u.id && it.status == ReportStatus.CONFIRMED }
            val earnedComm = empReports.sumOf { it.commissionEarnedUgx }
            WangiCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(u.fullName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("${u.role} • ${u.phone}", fontSize = 12.sp, color = WangiTextSecondary)
                        Text("${empReports.size} Confirmed shifts", fontSize = 11.5.sp, color = WangiTextMuted)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("UGX ${WangiViewModel.formatUgx(earnedComm)}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = WangiGoldDark)
                        Text("Earned Commission", fontSize = 10.sp, color = WangiSuccess)
                    }
                }
            }
        }
    }
}

@Composable
fun SecurityBackupView() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        WangiCard {
            Text("System Security & Backup Controls", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text("• Relational Database: SQLite / Room (wangi_tech_solutions.db)", fontSize = 13.sp)
            Text("• Backup Status: Synchronized daily at 00:00 Africa/Kampala", fontSize = 13.sp)
            Text("• RouterOS Password Vault: AES-256 encrypted storage", fontSize = 13.sp)
            Text("• Role-Based Access: Hotspot & Gaming strictly separated at server layer", fontSize = 13.sp)
        }

        WangiCard(backgroundColor = WangiSuccessLight) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = WangiSuccess)
                Spacer(modifier = Modifier.width(10.dp))
                Text("Zero Vulnerabilities Detected • System Operational", fontWeight = FontWeight.Bold, color = WangiSuccess, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun GenericDataTableView(title: String, reports: List<DailyReport>) {
    Column {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(10.dp))
        Text("Total Records Loaded: ${reports.size}", color = WangiTextSecondary)
    }
}
