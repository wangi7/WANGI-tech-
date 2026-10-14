package com.example.ui.billing

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
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
fun BillingDashboardScreen(
    currentUser: User,
    packages: List<BillingPackage>,
    subscriptions: List<BillingSubscription>,
    vouchers: List<WifiVoucher>,
    transactions: List<PaymentTransaction>,
    devices: List<NetworkDevice>,
    notifications: List<NotificationItem>,
    liveTime: String,
    liveDate: String,
    onTestPaymentCallback: (String, String, String, String, BillingPackage, String) -> Unit,
    onGenerateVouchers: (BillingPackage, Int, String) -> Unit,
    onCreatePackage: (String, String, Long, Int, Long, Boolean, Boolean, String) -> Unit,
    onUpdatePackage: (BillingPackage) -> Unit,
    onDeletePackage: (BillingPackage) -> Unit,
    onDisconnectSubscriber: (BillingSubscription) -> Unit,
    onExtendSubscription: (BillingSubscription, Int) -> Unit,
    onToggleBlockMac: (BillingSubscription) -> Unit,
    onAddDevice: (String, String, String, String, Int, String) -> Unit,
    onDeleteDevice: (NetworkDevice) -> Unit,
    onSendVoucherSms: (String, String, String) -> Unit,
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val clipboardManager: ClipboardManager = LocalClipboardManager.current

    // Dialog States
    var showBatchDialog by remember { mutableStateOf(false) }
    var showCreatePackageDialog by remember { mutableStateOf(false) }
    var editingPackage by remember { mutableStateOf<BillingPackage?>(null) }
    var showAddDeviceDialog by remember { mutableStateOf(false) }
    var showCaptivePortalPreview by remember { mutableStateOf(false) }
    var showPrintDialog by remember { mutableStateOf(false) }
    var showRouterScriptDialog by remember { mutableStateOf(false) }
    var buyPackageDialog by remember { mutableStateOf<BillingPackage?>(null) }
    var smsVoucherDialog by remember { mutableStateOf<WifiVoucher?>(null) }
    var pingTestDevice by remember { mutableStateOf<NetworkDevice?>(null) }
    var pingResultText by remember { mutableStateOf<String?>(null) }
    var syncResultText by remember { mutableStateOf<String?>(null) }

    // Search and filter states
    var voucherSearch by remember { mutableStateOf("") }
    var voucherStatusFilter by remember { mutableStateOf("ALL") }
    var packageTypeFilter by remember { mutableStateOf("ALL") }
    var subscriberSearch by remember { mutableStateOf("") }

    // Customer checkout state
    var customerPhone by remember { mutableStateOf("0755927896") }
    var customerName by remember { mutableStateOf("Customer") }
    var selectedProvider by remember { mutableStateOf("AIRTEL_MONEY") }
    var simulateDuplicateCallback by remember { mutableStateOf(false) }

    val activeSubs = subscriptions.filter { it.status == "ACTIVE" }
    val totalBilled = transactions.filter { it.status == "VERIFIED" }.sumOf { it.amountUgx }
    val availableVouchers = vouchers.filter { it.status == "AVAILABLE" }

    Scaffold(
        containerColor = WangiBackground,
        topBar = {
            WangiTopBar(
                title = "WI-FI BILLING SYSTEM",
                subtitle = "Universal Hotspot, PPPoE & FreeRADIUS Engine",
                timeText = liveTime,
                unreadNotificationCount = notifications.count { !it.isRead },
                onLogoutClick = onLogout
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = WangiWhite,
                tonalElevation = 8.dp
            ) {
                listOf(
                    Triple(0, "Packages", Icons.Default.WifiTethering),
                    Triple(1, "Vouchers", Icons.Default.ConfirmationNumber),
                    Triple(2, "Subscribers", Icons.Default.People),
                    Triple(3, "Gateways", Icons.Default.Router),
                    Triple(4, "Payments", Icons.Default.Receipt)
                ).forEach { (tabIdx, label, icon) ->
                    NavigationBarItem(
                        selected = selectedTab == tabIdx,
                        onClick = { selectedTab = tabIdx },
                        icon = {
                            BadgedBox(badge = {
                                if (tabIdx == 1 && availableVouchers.isNotEmpty()) {
                                    Badge(containerColor = WangiRed) { Text("${availableVouchers.size}") }
                                } else if (tabIdx == 2 && activeSubs.isNotEmpty()) {
                                    Badge(containerColor = WangiSuccess) { Text("${activeSubs.size}") }
                                }
                            }) {
                                Icon(icon, contentDescription = label)
                            }
                        },
                        label = { Text(label, fontSize = 11.sp, fontWeight = if (selectedTab == tabIdx) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = WangiRed,
                            selectedTextColor = WangiRed,
                            indicatorColor = WangiLightRed,
                            unselectedIconColor = WangiTextMuted,
                            unselectedTextColor = WangiTextSecondary
                        )
                    )
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
            item {
                Spacer(modifier = Modifier.height(4.dp))

                // Network Engine Health Status Card
                WangiCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(WangiSuccess)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Universal Wi-Fi Billing Engine", fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
                            }
                            StatusBadge("ONLINE & SYNCED")
                        }

                        // Network protocol pills
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFE8F5E9))
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Text("RADIUS :1812", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = WangiSuccess)
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFE3F2FD))
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Text("MikroTik API :8728", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1976D2))
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFF3E5F5))
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Text("Omada Portal :8043", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7B1FA2))
                            }
                        }
                    }
                }
            }

            // High-Level KPI Summary (Airtel-style white cards with subtle shadow)
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    WangiCard(modifier = Modifier.weight(1f), backgroundColor = WangiWhite) {
                        Text("Online Revenue", fontSize = 11.sp, color = WangiTextSecondary)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("UGX ${WangiViewModel.formatUgx(totalBilled)}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = WangiCharcoal)
                        Text("Airtel + MTN MoMo", fontSize = 10.5.sp, color = WangiSuccess)
                    }

                    WangiCard(modifier = Modifier.weight(1f), backgroundColor = WangiWhite) {
                        Text("Active Hotspot Users", fontSize = 11.sp, color = WangiTextSecondary)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("${activeSubs.size} Online", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = WangiRed)
                        Text("Across 3 Hubs", fontSize = 10.5.sp, color = WangiTextMuted)
                    }

                    WangiCard(modifier = Modifier.weight(1f), backgroundColor = WangiWhite) {
                        Text("Voucher Stock", fontSize = 11.sp, color = WangiTextSecondary)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("${availableVouchers.size} Unused", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = WangiGoldDark)
                        Text("Total: ${vouchers.size}", fontSize = 10.5.sp, color = WangiTextMuted)
                    }
                }
            }

            // Quick Action Buttons Row (Airtel-style rounded square buttons)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = WangiWhite),
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("QUICK BILLING ACTIONS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = WangiTextSecondary)
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            QuickActionItem(icon = Icons.Default.ConfirmationNumber, label = "Batch Vouchers", color = WangiRed) {
                                showBatchDialog = true
                            }
                            QuickActionItem(icon = Icons.Default.AddCircleOutline, label = "New Package", color = Color(0xFF1976D2)) {
                                showCreatePackageDialog = true
                            }
                            QuickActionItem(icon = Icons.Default.Language, label = "Captive Portal", color = Color(0xFFE65100)) {
                                showCaptivePortalPreview = true
                            }
                            QuickActionItem(icon = Icons.Default.Print, label = "Print Sheet", color = Color(0xFF388E3C)) {
                                showPrintDialog = true
                            }
                            QuickActionItem(icon = Icons.Default.Terminal, label = "Router Script", color = WangiCharcoal) {
                                showRouterScriptDialog = true
                            }
                        }
                    }
                }
            }

            // Tabs Content
            when (selectedTab) {
                0 -> {
                    // TAB 0: INTERNET PACKAGES
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("INTERNET PACKAGES (${packages.size})", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = WangiTextSecondary)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf("ALL", "HOTSPOT", "PPPOE").forEach { filter ->
                                    FilterChip(
                                        selected = packageTypeFilter == filter,
                                        onClick = { packageTypeFilter = filter },
                                        label = { Text(filter, fontSize = 10.sp) }
                                    )
                                }
                            }
                        }
                    }

                    val filteredPackages = packages.filter {
                        when (packageTypeFilter) {
                            "HOTSPOT" -> it.isHotspot
                            "PPPOE" -> it.isPppoe
                            else -> true
                        }
                    }

                    items(filteredPackages) { pkg ->
                        WangiCard {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (pkg.isPppoe) Color(0xFFE8EAF6) else WangiLightRed),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                if (pkg.isPppoe) Icons.Default.SettingsEthernet else Icons.Default.Speed,
                                                contentDescription = null,
                                                tint = if (pkg.isPppoe) Color(0xFF3F51B5) else WangiRed
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(pkg.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                if (pkg.isPppoe) {
                                                    StatusBadge("PPPoE")
                                                }
                                            }
                                            Text("${pkg.validityPeriod} • Speed limit: ${pkg.speedLimitMbps} Mbps", fontSize = 12.sp, color = WangiTextSecondary)
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("UGX ${WangiViewModel.formatUgx(pkg.priceUgx)}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = WangiCharcoal)
                                        Text(if (pkg.quotaLimitMb == 0L) "Unlimited Quota" else "${pkg.quotaLimitMb} MB", fontSize = 11.sp, color = WangiTextMuted)
                                    }
                                }

                                if (pkg.description.isNotBlank()) {
                                    Text(pkg.description, fontSize = 11.5.sp, color = WangiTextSecondary)
                                }

                                Divider(color = WangiBorder)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        IconButton(onClick = { editingPackage = pkg }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = WangiTextSecondary, modifier = Modifier.size(18.dp))
                                        }
                                        IconButton(onClick = { onDeletePackage(pkg) }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = WangiRed, modifier = Modifier.size(18.dp))
                                        }
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(
                                            onClick = {
                                                onGenerateVouchers(pkg, 10, "WG-")
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                        ) {
                                            Icon(Icons.Default.ConfirmationNumber, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Gen 10", fontSize = 11.5.sp)
                                        }

                                        Button(
                                            onClick = { buyPackageDialog = pkg },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = WangiRed),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                                        ) {
                                            Text("Test Checkout", fontSize = 11.5.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // TAB 1: VOUCHERS & BATCH INVENTORY
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("VOUCHER INVENTORY (${vouchers.size})", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = WangiTextSecondary)
                                TextButton(onClick = { showPrintDialog = true }) {
                                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Print Cards", fontSize = 12.sp)
                                }
                            }

                            // Search bar
                            OutlinedTextField(
                                value = voucherSearch,
                                onValueChange = { voucherSearch = it },
                                placeholder = { Text("Search by code, PIN, or package...") },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )

                            // Status filters
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("ALL", "AVAILABLE", "ACTIVE", "EXPIRED").forEach { st ->
                                    FilterChip(
                                        selected = voucherStatusFilter == st,
                                        onClick = { voucherStatusFilter = st },
                                        label = { Text(st, fontSize = 11.sp) }
                                    )
                                }
                            }
                        }
                    }

                    val filteredVouchers = vouchers.filter { v ->
                        val matchesSearch = voucherSearch.isBlank() ||
                                v.code.contains(voucherSearch, ignoreCase = true) ||
                                v.pin.contains(voucherSearch, ignoreCase = true) ||
                                v.packageName.contains(voucherSearch, ignoreCase = true)
                        val matchesStatus = voucherStatusFilter == "ALL" || v.status.equals(voucherStatusFilter, ignoreCase = true)
                        matchesSearch && matchesStatus
                    }

                    if (filteredVouchers.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No vouchers found. Tap 'Batch Vouchers' to generate.", color = WangiTextMuted)
                            }
                        }
                    }

                    items(filteredVouchers) { v ->
                        WangiCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(v.code, fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = FontFamily.Monospace, color = WangiCharcoal)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        StatusBadge(status = v.status)
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("PIN: ${v.pin} • Package: ${v.packageName}", fontSize = 12.5.sp, color = WangiTextSecondary)
                                    Text("UGX ${WangiViewModel.formatUgx(v.priceUgx)} • ${v.validityPeriod}", fontSize = 12.sp, color = WangiGoldDark, fontWeight = FontWeight.SemiBold)
                                    if (v.usedByPhone != null) {
                                        Text("Used by: ${v.usedByName ?: v.usedByPhone}", fontSize = 11.sp, color = WangiTextMuted)
                                    }
                                }

                                Row {
                                    IconButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(v.code))
                                        }
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy Code", tint = WangiTextSecondary)
                                    }
                                    IconButton(
                                        onClick = { smsVoucherDialog = v }
                                    ) {
                                        Icon(Icons.Default.Sms, contentDescription = "Send SMS", tint = WangiRed)
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // TAB 2: ACTIVE SUBSCRIBERS & LIVE SESSIONS
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("ACTIVE HOTSPOT & PPPOE CLIENTS (${subscriptions.size})", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = WangiTextSecondary)
                            OutlinedTextField(
                                value = subscriberSearch,
                                onValueChange = { subscriberSearch = it },
                                placeholder = { Text("Search by name, phone, IP, or MAC...") },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }

                    val filteredSubs = subscriptions.filter { sub ->
                        subscriberSearch.isBlank() ||
                                sub.customerName.contains(subscriberSearch, ignoreCase = true) ||
                                sub.customerPhone.contains(subscriberSearch, ignoreCase = true) ||
                                sub.ipAddress.contains(subscriberSearch, ignoreCase = true) ||
                                sub.macAddress.contains(subscriberSearch, ignoreCase = true) ||
                                sub.voucherCode.contains(subscriberSearch, ignoreCase = true)
                    }

                    items(filteredSubs) { sub ->
                        WangiCard {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(sub.customerName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            if (sub.isBlocked) {
                                                StatusBadge("BLOCKED")
                                            } else {
                                                StatusBadge(status = sub.status)
                                            }
                                        }
                                        Text("Phone: ${sub.customerPhone} • Voucher: ${sub.voucherCode}", fontSize = 12.sp, color = WangiTextSecondary)
                                    }
                                    Text("UGX ${WangiViewModel.formatUgx(sub.priceUgx)}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }

                                // Technical details: IP, MAC, Bandwidth
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFF8F9FA))
                                        .padding(10.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("IP: ${sub.ipAddress}", fontSize = 11.5.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
                                            Text("MAC: ${sub.macAddress}", fontSize = 11.5.sp, fontFamily = FontFamily.Monospace, color = WangiTextSecondary)
                                        }
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Downloaded: ${sub.bytesDownloadedMb} MB", fontSize = 11.sp, color = WangiSuccess, fontWeight = FontWeight.SemiBold)
                                            Text("Uploaded: ${sub.bytesUploadedMb} MB", fontSize = 11.sp, color = WangiTextMuted)
                                        }
                                        Text(
                                            "Expires: ${SimpleDateFormat("dd MMM HH:mm", Locale.getDefault()).format(Date(sub.expiresAt))}",
                                            fontSize = 11.sp,
                                            color = WangiTextSecondary
                                        )
                                    }
                                }

                                // Action Buttons: Kick, Extend, Block
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = { onToggleBlockMac(sub) },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(if (sub.isBlocked) "Unblock MAC" else "Block MAC", fontSize = 11.sp)
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    OutlinedButton(
                                        onClick = { onExtendSubscription(sub, 24) },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("+24 Hours", fontSize = 11.sp)
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Button(
                                        onClick = { onDisconnectSubscriber(sub) },
                                        colors = ButtonDefaults.buttonColors(containerColor = WangiRed),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                    ) {
                                        Text("Disconnect", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                3 -> {
                    // TAB 3: ROUTERS & FREERADIUS GATEWAYS
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("NETWORK ROUTERS & CONTROLLERS (${devices.size})", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = WangiTextSecondary)
                            Button(
                                onClick = { showAddDeviceDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = WangiRed),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Gateway", fontSize = 11.5.sp)
                            }
                        }
                    }

                    items(devices) { dev ->
                        WangiCard {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (dev.vendor == "MIKROTIK") Color(0xFFE3F2FD) else Color(0xFFF3E5F5)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.Router,
                                                contentDescription = null,
                                                tint = if (dev.vendor == "MIKROTIK") Color(0xFF1976D2) else Color(0xFF7B1FA2)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(dev.name, fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
                                            Text("${dev.vendor} • ${dev.model}", fontSize = 12.sp, color = WangiTextSecondary)
                                            Text("IP: ${dev.ipAddress} • API Port: ${dev.adminPort}", fontSize = 11.5.sp, fontFamily = FontFamily.Monospace)
                                        }
                                    }
                                    StatusBadge(status = dev.status)
                                }

                                // Metrics: CPU & Hotspot users
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFFAFAFA))
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("OS: ${dev.routerOsVersion}", fontSize = 11.5.sp, color = WangiTextSecondary)
                                    Text("Active Users: ${dev.activeHotspotUsers}", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = WangiSuccess)
                                    Text("Backup: ${dev.configBackupDate}", fontSize = 11.sp, color = WangiTextMuted)
                                }

                                // Interactive Tools
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(onClick = { onDeleteDevice(dev) }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = WangiTextMuted)
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(
                                            onClick = {
                                                pingTestDevice = dev
                                                pingResultText = "Ping ${dev.ipAddress}: 2ms (Connected to ${dev.model} via API Port ${dev.adminPort}. CPU: 4%, Free RAM: 3.2 GB)"
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                        ) {
                                            Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Ping API", fontSize = 11.sp)
                                        }

                                        Button(
                                            onClick = {
                                                syncResultText = "Successfully synced ${activeSubs.size} active users & ${availableVouchers.size} vouchers to ${dev.name} (/ip hotspot user)."
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = WangiCharcoal),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                        ) {
                                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Sync Users", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // FreeRADIUS AAA Server Status Box
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("FreeRADIUS 3.2 Server Engine", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(WangiSuccess)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("ACTIVE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Text("• Authentication Port: UDP 1812 (Listening)", color = Color(0xFFA5D6A7), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                Text("• Accounting Port: UDP 1813 (Interim updates: 2 mins)", color = Color(0xFFA5D6A7), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                Text("• Shared Secret: 'WangiRadiusSecret2026'", color = Color(0xFFFFF59D), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                Text("• NAS Clients: 3 Active Gateways configured", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                            }
                        }
                    }
                }

                4 -> {
                    // TAB 4: PAYMENTS & IDEMPOTENT CALLBACK AUDIT
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("PAYMENT TRANSACTIONS (${transactions.size})", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = WangiTextSecondary)
                            Text("UGX ${WangiViewModel.formatUgx(totalBilled)} Total", fontWeight = FontWeight.Bold, color = WangiSuccess, fontSize = 13.sp)
                        }
                    }

                    items(transactions) { tx ->
                        WangiCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("TxID: ${tx.providerTxId}", fontWeight = FontWeight.Bold, fontSize = 13.5.sp, fontFamily = FontFamily.Monospace)
                                    Text("${tx.customerName} (${tx.customerPhone})", fontSize = 12.5.sp, color = WangiTextSecondary)
                                    Text("Package: ${tx.packageName} • Ref: ${tx.orderRef}", fontSize = 11.5.sp, color = WangiTextMuted)
                                    Text("Via: ${tx.provider}", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = if (tx.provider == "AIRTEL_MONEY") WangiRed else Color(0xFFF57F17))
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text("UGX ${WangiViewModel.formatUgx(tx.amountUgx)}", fontWeight = FontWeight.Bold, fontSize = 14.5.sp, color = WangiCharcoal)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    StatusBadge(status = tx.status)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // ==========================================
    // ALL DIALOGS & ACTION POPUPS
    // ==========================================

    if (showBatchDialog) {
        BatchGenerateVouchersDialog(
            packages = packages,
            onDismiss = { showBatchDialog = false },
            onGenerate = { pkg, qty, pfx ->
                onGenerateVouchers(pkg, qty, pfx)
            }
        )
    }

    if (showCreatePackageDialog) {
        CreateOrEditPackageDialog(
            initialPackage = null,
            onDismiss = { showCreatePackageDialog = false },
            onSave = { name, valPer, price, speed, quota, isHot, isPpp, desc ->
                onCreatePackage(name, valPer, price, speed, quota, isHot, isPpp, desc)
            }
        )
    }

    if (editingPackage != null) {
        CreateOrEditPackageDialog(
            initialPackage = editingPackage,
            onDismiss = { editingPackage = null },
            onSave = { name, valPer, price, speed, quota, isHot, isPpp, desc ->
                val updated = editingPackage!!.copy(
                    name = name,
                    validityPeriod = valPer,
                    priceUgx = price,
                    speedLimitMbps = speed,
                    quotaLimitMb = quota,
                    isHotspot = isHot,
                    isPppoe = isPpp,
                    description = desc
                )
                onUpdatePackage(updated)
                editingPackage = null
            }
        )
    }

    if (showAddDeviceDialog) {
        AddGatewayDialog(
            onDismiss = { showAddDeviceDialog = false },
            onAdd = { name, model, vendor, ip, port, branchId ->
                onAddDevice(name, model, vendor, ip, port, branchId)
            }
        )
    }

    if (showCaptivePortalPreview) {
        CaptivePortalPreviewDialog(
            packages = packages,
            onDismiss = { showCaptivePortalPreview = false }
        )
    }

    if (showPrintDialog) {
        PrintVouchersSheetDialog(
            vouchers = availableVouchers.ifEmpty { vouchers },
            onDismiss = { showPrintDialog = false }
        )
    }

    if (showRouterScriptDialog) {
        RouterScriptDialog(
            onDismiss = { showRouterScriptDialog = false }
        )
    }

    // SMS Voucher Dialog
    if (smsVoucherDialog != null) {
        val v = smsVoucherDialog!!
        var targetPhone by remember { mutableStateOf(v.usedByPhone ?: "0755927896") }

        AlertDialog(
            onDismissRequest = { smsVoucherDialog = null },
            title = { Text("Send Voucher via SMS", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Voucher Code: ${v.code} (PIN: ${v.pin})", fontWeight = FontWeight.Bold, color = WangiRed)
                    Text("Package: ${v.packageName} - UGX ${WangiViewModel.formatUgx(v.priceUgx)}")
                    OutlinedTextField(
                        value = targetPhone,
                        onValueChange = { targetPhone = it },
                        label = { Text("Recipient Phone Number") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSendVoucherSms(v.code, targetPhone, v.packageName)
                        smsVoucherDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WangiRed)
                ) {
                    Text("Send SMS")
                }
            },
            dismissButton = {
                TextButton(onClick = { smsVoucherDialog = null }) { Text("Cancel") }
            }
        )
    }

    // Test Checkout & Idempotent Callback Dialog
    if (buyPackageDialog != null) {
        val pkg = buyPackageDialog!!
        AlertDialog(
            onDismissRequest = { buyPackageDialog = null },
            title = { Text("Buy ${pkg.name}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Price: UGX ${WangiViewModel.formatUgx(pkg.priceUgx)} • Speed: ${pkg.speedLimitMbps} Mbps", fontWeight = FontWeight.Bold)
                    Text("Duration: ${pkg.validityPeriod}")
                    Divider()
                    OutlinedTextField(
                        value = customerPhone,
                        onValueChange = { customerPhone = it },
                        label = { Text("Customer Mobile Number") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = customerName,
                        onValueChange = { customerName = it },
                        label = { Text("Customer Name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Payment Method:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("AIRTEL_MONEY", "MTN_MOMO", "CASH").forEach { prov ->
                            FilterChip(
                                selected = selectedProvider == prov,
                                onClick = { selectedProvider = prov },
                                label = { Text(prov.replace("_", " "), fontSize = 11.sp) }
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = simulateDuplicateCallback,
                            onCheckedChange = { simulateDuplicateCallback = it }
                        )
                        Text("Simulate Duplicate Callback (Tests Protection)", fontSize = 11.5.sp, color = WangiRed)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val orderRef = "ORD-${System.currentTimeMillis() % 1000000}"
                        val txId = if (simulateDuplicateCallback) "DUP-TX-9999" else "TXN-${UUID.randomUUID().toString().take(8).uppercase()}"
                        onTestPaymentCallback(orderRef, txId, customerPhone, customerName, pkg, selectedProvider)
                        buyPackageDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WangiSuccess)
                ) {
                    Text("Process & Verify Payment")
                }
            },
            dismissButton = {
                TextButton(onClick = { buyPackageDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Ping Result Alert
    if (pingResultText != null) {
        AlertDialog(
            onDismissRequest = { pingResultText = null },
            icon = { Icon(Icons.Default.NetworkCheck, contentDescription = null, tint = WangiSuccess) },
            title = { Text("Router Connectivity Test", fontWeight = FontWeight.Bold) },
            text = { Text(pingResultText ?: "") },
            confirmButton = {
                Button(onClick = { pingResultText = null }, colors = ButtonDefaults.buttonColors(containerColor = WangiSuccess)) {
                    Text("OK")
                }
            }
        )
    }

    // Sync Result Alert
    if (syncResultText != null) {
        AlertDialog(
            onDismissRequest = { syncResultText = null },
            icon = { Icon(Icons.Default.Sync, contentDescription = null, tint = WangiCharcoal) },
            title = { Text("Hotspot Database Synced", fontWeight = FontWeight.Bold) },
            text = { Text(syncResultText ?: "") },
            confirmButton = {
                Button(onClick = { syncResultText = null }, colors = ButtonDefaults.buttonColors(containerColor = WangiCharcoal)) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun QuickActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(label, fontSize = 10.5.sp, fontWeight = FontWeight.Medium, color = WangiCharcoal, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}
