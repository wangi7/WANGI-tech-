package com.example.ui.billing

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.*
import com.example.ui.components.StatusBadge
import com.example.ui.components.WangiCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.WangiViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun BatchGenerateVouchersDialog(
    packages: List<BillingPackage>,
    onDismiss: () -> Unit,
    onGenerate: (BillingPackage, Int, String) -> Unit
) {
    var selectedPackage by remember { mutableStateOf(packages.firstOrNull()) }
    var quantityText by remember { mutableStateOf("25") }
    var prefix by remember { mutableStateOf("WG-") }
    var includePin by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ConfirmationNumber, contentDescription = null, tint = WangiRed)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Batch Generate Wi-Fi Vouchers", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Select Internet Package:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                packages.forEach { pkg ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selectedPackage?.id == pkg.id) WangiLightRed else WangiLightGray)
                            .clickable { selectedPackage = pkg }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(pkg.name, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                            Text("${pkg.validityPeriod} • ${pkg.speedLimitMbps} Mbps", fontSize = 11.5.sp, color = WangiTextSecondary)
                        }
                        Text("UGX ${WangiViewModel.formatUgx(pkg.priceUgx)}", fontWeight = FontWeight.Bold, color = WangiRed, fontSize = 13.sp)
                    }
                }

                Text("Batch Quantity:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("10", "25", "50", "100").forEach { q ->
                        FilterChip(
                            selected = quantityText == q,
                            onClick = { quantityText = q },
                            label = { Text("$q Pcs") }
                        )
                    }
                }

                OutlinedTextField(
                    value = prefix,
                    onValueChange = { prefix = it },
                    label = { Text("Voucher Code Prefix (e.g. WG-, KLA-)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = includePin, onCheckedChange = { includePin = it })
                    Text("Generate 4-Digit Security PIN for each voucher", fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val pkg = selectedPackage ?: return@Button
                    val qty = quantityText.toIntOrNull() ?: 10
                    onGenerate(pkg, qty, prefix)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = WangiRed)
            ) {
                Text("Generate Vouchers")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun CreateOrEditPackageDialog(
    initialPackage: BillingPackage? = null,
    onDismiss: () -> Unit,
    onSave: (String, String, Long, Int, Long, Boolean, Boolean, String) -> Unit
) {
    var name by remember { mutableStateOf(initialPackage?.name ?: "") }
    var validity by remember { mutableStateOf(initialPackage?.validityPeriod ?: "24 Hours") }
    var priceText by remember { mutableStateOf(initialPackage?.priceUgx?.toString() ?: "1000") }
    var speedText by remember { mutableStateOf(initialPackage?.speedLimitMbps?.toString() ?: "10") }
    var quotaText by remember { mutableStateOf(initialPackage?.quotaLimitMb?.toString() ?: "0") }
    var isHotspot by remember { mutableStateOf(initialPackage?.isHotspot ?: true) }
    var isPppoe by remember { mutableStateOf(initialPackage?.isPppoe ?: false) }
    var description by remember { mutableStateOf(initialPackage?.description ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (initialPackage == null) "New Internet Package" else "Edit Package", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Package Name (e.g. 12-Hour Night Owl)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = validity,
                    onValueChange = { validity = it },
                    label = { Text("Validity Period (e.g. 12 Hours, 7 Days)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it },
                        label = { Text("Price (UGX)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = speedText,
                        onValueChange = { speedText = it },
                        label = { Text("Speed (Mbps)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = quotaText,
                    onValueChange = { quotaText = it },
                    label = { Text("Data Quota (MB, 0 for Unlimited)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isHotspot, onCheckedChange = { isHotspot = it })
                    Text("Hotspot Captive Portal", fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(16.dp))
                    Checkbox(checked = isPppoe, onCheckedChange = { isPppoe = it })
                    Text("PPPoE Dedicated", fontSize = 12.sp)
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description / Profile Notes") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val price = priceText.toLongOrNull() ?: 1000L
                    val speed = speedText.toIntOrNull() ?: 10
                    val quota = quotaText.toLongOrNull() ?: 0L
                    if (name.isNotBlank()) {
                        onSave(name, validity, price, speed, quota, isHotspot, isPppoe, description)
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = WangiRed)
            ) {
                Text("Save Package")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AddGatewayDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, String, String, Int, String) -> Unit
) {
    var name by remember { mutableStateOf("New Hotspot Gateway") }
    var vendor by remember { mutableStateOf("MIKROTIK") }
    var model by remember { mutableStateOf("CCR2004-16G-2S+") }
    var ip by remember { mutableStateOf("192.168.88.1") }
    var portText by remember { mutableStateOf("8728") }
    var branchId by remember { mutableStateOf("br_kampala") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Router, contentDescription = null, tint = WangiRed)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Router / Controller Gateway", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Gateway Name (e.g. Downtown Hotspot)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Hardware Vendor:", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("MIKROTIK", "TP_LINK_OMADA").forEach { v ->
                        FilterChip(
                            selected = vendor == v,
                            onClick = {
                                vendor = v
                                if (v == "TP_LINK_OMADA") {
                                    model = "ER7206 / OC200"
                                    portText = "8043"
                                } else {
                                    model = "CCR2004-16G-2S+"
                                    portText = "8728"
                                }
                            },
                            label = { Text(if (v == "MIKROTIK") "MikroTik RouterOS" else "TP-Link Omada") }
                        )
                    }
                }

                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text("Model / Controller Version") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = ip,
                        onValueChange = { ip = it },
                        label = { Text("IP Address / Host") },
                        modifier = Modifier.weight(1.5f)
                    )
                    OutlinedTextField(
                        value = portText,
                        onValueChange = { portText = it },
                        label = { Text("API Port") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val port = portText.toIntOrNull() ?: 8728
                    onAdd(name, model, vendor, ip, port, branchId)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = WangiRed)
            ) {
                Text("Register Gateway")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun CaptivePortalPreviewDialog(
    packages: List<BillingPackage>,
    onDismiss: () -> Unit
) {
    var voucherInput by remember { mutableStateOf("") }
    var selectedPackage by remember { mutableStateOf(packages.firstOrNull()) }
    var customerPhone by remember { mutableStateOf("0755123456") }
    var portalState by remember { mutableStateOf("LOGIN") } // LOGIN, SUCCESS, BUY
    var connectedCode by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(20.dp)),
            color = WangiWhite,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Top phone notch preview bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(WangiCharcoal)
                        .padding(vertical = 10.dp, horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("http://wifi.wangi.net/login", color = Color.White.copy(alpha = 0.8f), fontSize = 11.5.sp, fontFamily = FontFamily.Monospace)
                        IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                }

                // Branded Captive Portal Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(WangiRed)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Wifi, contentDescription = null, tint = WangiRed, modifier = Modifier.size(34.dp))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("WANGI TECH WI-FI", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text("Smart Networks. Superfast Internet.", color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp)
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (portalState == "SUCCESS") {
                        // Connected Screen
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = WangiSuccess, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("CONNECTED TO INTERNET!", fontWeight = FontWeight.Bold, color = WangiSuccess, fontSize = 16.sp)
                                Text("Voucher: $connectedCode", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text("Speed: Up to 15 Mbps • No Limit", fontSize = 12.sp, color = WangiTextSecondary)
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { portalState = "LOGIN" },
                                    colors = ButtonDefaults.buttonColors(containerColor = WangiSuccess)
                                ) {
                                    Text("Disconnect / Re-test")
                                }
                            }
                        }
                    } else {
                        // Switch between Voucher Login and Instant Mobile Money Buy
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { portalState = "LOGIN" },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (portalState == "LOGIN") WangiRed else WangiLightGray,
                                    contentColor = if (portalState == "LOGIN") Color.White else WangiCharcoal
                                )
                            ) {
                                Text("Voucher Code", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { portalState = "BUY" },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (portalState == "BUY") WangiRed else WangiLightGray,
                                    contentColor = if (portalState == "BUY") Color.White else WangiCharcoal
                                )
                            ) {
                                Text("Buy with MoMo", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (portalState == "LOGIN") {
                            Text("Enter your Wi-Fi Voucher code from your scratch card or SMS:", fontSize = 13.sp)
                            OutlinedTextField(
                                value = voucherInput,
                                onValueChange = { voucherInput = it.uppercase() },
                                label = { Text("Voucher Code (e.g. WG-849201)") },
                                leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Button(
                                onClick = {
                                    if (voucherInput.isNotBlank()) {
                                        connectedCode = voucherInput
                                        portalState = "SUCCESS"
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = WangiRed),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("CONNECT TO WI-FI", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    connectedCode = "FREE-TRIAL-15MIN"
                                    portalState = "SUCCESS"
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("🎁 Free 15-Minute Trial Pass", fontWeight = FontWeight.SemiBold)
                            }
                        } else {
                            // Instant Mobile Money Checkout
                            Text("Select a Package to buy immediately:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            packages.forEach { pkg ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selectedPackage?.id == pkg.id) WangiLightRed else WangiLightGray)
                                        .clickable { selectedPackage = pkg }
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(pkg.name, fontWeight = FontWeight.Bold)
                                    Text("UGX ${WangiViewModel.formatUgx(pkg.priceUgx)}", color = WangiRed, fontWeight = FontWeight.Bold)
                                }
                            }

                            OutlinedTextField(
                                value = customerPhone,
                                onValueChange = { customerPhone = it },
                                label = { Text("Airtel Money or MTN Mobile Number") },
                                leadingIcon = { Icon(Icons.Default.PhoneAndroid, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Button(
                                onClick = {
                                    connectedCode = "AUTO-MOMO-" + (1000..9999).random()
                                    portalState = "SUCCESS"
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = WangiSuccess),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Pay & Connect Instantly", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Captive Portal Help Footer
                    Divider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Need help? Call +256 701 234 567", fontSize = 11.5.sp, color = WangiTextSecondary)
                        Text("Powered by WANGI", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = WangiCharcoal)
                    }
                }
            }
        }
    }
}

@Composable
fun PrintVouchersSheetDialog(
    vouchers: List<WifiVoucher>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Print, contentDescription = null, tint = WangiCharcoal)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Printable Voucher Tickets (${vouchers.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text("Thermal & Retail Card Format. Connect to 'WANGI TECH WI-FI' SSID:", fontSize = 12.sp, color = WangiTextSecondary)
                }

                items(vouchers.take(50)) { v ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, WangiBorder, RoundedCornerShape(8.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("WANGI TECH SOLUTIONS", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = WangiRed)
                                Text("SSID: WANGI_WIFI", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Divider(modifier = Modifier.padding(vertical = 6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("CODE: ${v.code}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = WangiCharcoal, fontFamily = FontFamily.Monospace)
                                    Text("PIN: ${v.pin} • ${v.packageName}", fontSize = 12.sp, color = WangiTextSecondary)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("UGX ${WangiViewModel.formatUgx(v.priceUgx)}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = WangiGoldDark)
                                    Text(v.validityPeriod, fontSize = 11.sp, color = WangiTextSecondary)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = WangiCharcoal)
            ) {
                Text("Done")
            }
        }
    )
}

@Composable
fun RouterScriptDialog(
    onDismiss: () -> Unit
) {
    val script = """
# ===================================================
# WANGI TECH SOLUTIONS - MIKROTIK ROUTEROS SETUP
# FreeRADIUS & Hotspot Captive Portal Integration
# ===================================================

# 1. Configure FreeRADIUS AAA Client
/radius add service=hotspot,login address=127.0.0.1 secret="WangiRadiusSecret2026" authentication-port=1812 accounting-port=1813 timeout=3s comment="WANGI FreeRADIUS"

# 2. Configure Hotspot Server Profile
/ip hotspot profile set [find default=yes] use-radius=yes radius-accounting=yes radius-interim-update=2m login-by=http-chap,http-pap,mac-cookie split-user-domain=no html-directory=hotspot

# 3. Enable WANGI Captive Portal Login URL
/ip hotspot profile set [find default=yes] hotspot-address=192.168.88.1 dns-name="wifi.wangi.net"

# 4. Bandwidth Rate-Limit Profiles
/ip hotspot user profile add name="Chap_Chap" rate-limit="5M/5M" shared-users=1 transparent-proxy=no
/ip hotspot user profile add name="Daily_Pass" rate-limit="10M/10M" shared-users=1 transparent-proxy=no
/ip hotspot user profile add name="Weekly_Unlimited" rate-limit="15M/15M" shared-users=1 transparent-proxy=no
/ip hotspot user profile add name="Monthly_Premium" rate-limit="20M/20M" shared-users=2 transparent-proxy=no

# 5. Enable API Service for Real-time Monitoring
/ip service set api port=8728 disabled=no
/ip service set api-ssl port=8729 disabled=no
    """.trimIndent()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Terminal, contentDescription = null, tint = WangiCharcoal)
                Spacer(modifier = Modifier.width(8.dp))
                Text("RouterOS CLI Configuration", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Copy & paste this into your MikroTik Terminal to configure RADIUS, Hotspot profiles, and API access:", fontSize = 12.sp, color = WangiTextSecondary)
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .verticalScroll(rememberScrollState()),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = script,
                        color = Color(0xFF81C784),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = WangiRed)
            ) {
                Text("Close")
            }
        }
    )
}
