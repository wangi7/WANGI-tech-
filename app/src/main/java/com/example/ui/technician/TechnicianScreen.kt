package com.example.ui.technician

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
fun TechnicianScreen(
    currentUser: User,
    jobs: List<TechnicianJob>,
    devices: List<NetworkDevice>,
    notifications: List<NotificationItem>,
    liveTime: String,
    liveDate: String,
    onCreateJob: (String, String, String, String, String, JobType, Long, String) -> Unit,
    onUpdateJob: (String, JobStatus, Long, String, String, Boolean) -> Unit,
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showNewJobDialog by remember { mutableStateOf(false) }
    var selectedJobForUpdate by remember { mutableStateOf<TechnicianJob?>(null) }

    // Job Form State
    var newTitle by remember { mutableStateOf("") }
    var newCustomer by remember { mutableStateOf("") }
    var newPhone by remember { mutableStateOf("") }
    var newAddress by remember { mutableStateOf("") }
    var newRouterModel by remember { mutableStateOf("MikroTik hAP ax3") }
    var newJobType by remember { mutableStateOf(JobType.INSTALLATION) }
    var newChargeText by remember { mutableStateOf("100000") }
    var newNotes by remember { mutableStateOf("") }

    // Update Form State
    var editStatus by remember { mutableStateOf(JobStatus.IN_PROGRESS) }
    var editMaterials by remember { mutableStateOf("") }
    var editChargeText by remember { mutableStateOf("") }
    var editNotes by remember { mutableStateOf("") }
    var editCustomerSignOff by remember { mutableStateOf(false) }

    val completedJobs = jobs.filter { it.status == JobStatus.COMPLETED }
    val inProgressJobs = jobs.filter { it.status == JobStatus.IN_PROGRESS || it.status == JobStatus.ASSIGNED }

    Scaffold(
        containerColor = WangiBackground,
        topBar = {
            WangiTopBar(
                title = "TECHNICIAN PORTAL",
                subtitle = "${currentUser.fullName} • Senior Network Tech",
                timeText = liveTime,
                unreadNotificationCount = notifications.count { !it.isRead },
                onLogoutClick = onLogout
            )
        },
        bottomBar = {
            WangiBottomNavigation(
                currentTab = selectedTab,
                onTabSelected = { selectedTab = it },
                tab1Label = "Jobs & Tickets",
                tab2Label = "Hardware",
                tab3Label = "Completed",
                tab4Label = "Performance",
                tab1Icon = Icons.Default.Construction,
                tab2Icon = Icons.Default.Router,
                tab3Icon = Icons.Default.DoneAll,
                tab4Icon = Icons.Default.TrendingUp
            )
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = { showNewJobDialog = true },
                    containerColor = WangiRed,
                    contentColor = WangiWhite,
                    modifier = Modifier.testTag("fab_new_tech_ticket")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "New Job Ticket")
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

                // Technician Profile Header
                WangiCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(currentUser.fullName, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("Field Engineer • Certified MikroTik (MTCNA/MTCRE)", fontSize = 12.sp, color = WangiTextSecondary)
                            Text("Assigned: ${inProgressJobs.size} Active Jobs • ${devices.size} Network Devices", fontSize = 11.5.sp, color = WangiGoldDark, fontWeight = FontWeight.SemiBold)
                        }
                        StatusBadge("FIELD READY")
                    }
                }
            }

            when (selectedTab) {
                0 -> {
                    // Active Job Tickets
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("ASSIGNED NETWORK TICKETS (${jobs.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = WangiTextSecondary)
                            Button(
                                onClick = { showNewJobDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = WangiRed),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("+ New Ticket", fontSize = 11.sp)
                            }
                        }
                    }

                    items(jobs) { job ->
                        WangiCard(
                            onClick = {
                                selectedJobForUpdate = job
                                editStatus = job.status
                                editMaterials = job.materialsUsed
                                editChargeText = job.serviceChargeUgx.toString()
                                editNotes = job.notes
                                editCustomerSignOff = job.customerSignOff
                            }
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(job.ticketNumber, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = WangiRed)
                                        Text(job.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    }
                                    StatusBadge(job.status.name)
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Customer: ${job.customerName} (${job.customerPhone})", fontSize = 12.sp, color = WangiTextSecondary)
                                Text("Site: ${job.siteAddress} • Equipment: ${job.routerModel}", fontSize = 11.5.sp, color = WangiCharcoal)

                                if (job.materialsUsed.isNotBlank()) {
                                    Text("Materials: ${job.materialsUsed}", fontSize = 11.sp, color = WangiTextMuted)
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Charge: UGX ${WangiViewModel.formatUgx(job.serviceChargeUgx)}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = WangiGoldDark)

                                    if (job.customerSignOff) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = WangiSuccessLight
                                        ) {
                                            Row(modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = WangiSuccess, modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text("Customer Signed", fontSize = 10.sp, color = WangiSuccess, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // Network Devices Inventory (MikroTik / TP-Link Omada)
                    item {
                        Text("ROUTER & ACCESS POINT INVENTORY (${devices.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = WangiTextSecondary)
                    }

                    items(devices) { dev ->
                        WangiCard {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (dev.vendor == "MIKROTIK") WangiLightRed else Color(0xFFF0FDFA)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Router,
                                                contentDescription = null,
                                                tint = if (dev.vendor == "MIKROTIK") WangiRed else Color(0xFF0D9488)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(dev.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text("${dev.vendor} • ${dev.model}", fontSize = 11.5.sp, color = WangiTextSecondary)
                                        }
                                    }
                                    StatusBadge(dev.status)
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text("OS/Firmware: ${dev.routerOsVersion} • Port: ${dev.adminPort}", fontSize = 11.5.sp)
                                Text("IP: ${dev.ipAddress} • MAC: ${dev.macAddress}", fontSize = 11.sp, color = WangiTextSecondary)
                                Text("Site: ${dev.siteLocation} • Active Hotspot Clients: ${dev.activeHotspotUsers}", fontSize = 11.sp, color = WangiSuccess, fontWeight = FontWeight.SemiBold)
                                Text("Last Config Backup: ${dev.configBackupDate}", fontSize = 10.sp, color = WangiTextMuted)
                            }
                        }
                    }
                }

                2 -> {
                    // Completed Jobs
                    item {
                        Text("COMPLETED INSTALLATIONS & REPAIRS (${completedJobs.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = WangiTextSecondary)
                    }
                    items(completedJobs) { cj ->
                        WangiCard {
                            Column {
                                Text(cj.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Client: ${cj.customerName} • Site: ${cj.siteAddress}", fontSize = 12.sp, color = WangiTextSecondary)
                                Text("Service Charge: UGX ${WangiViewModel.formatUgx(cj.serviceChargeUgx)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = WangiSuccess)
                                if (cj.materialsUsed.isNotBlank()) {
                                    Text("Materials: ${cj.materialsUsed}", fontSize = 11.sp, color = WangiTextMuted)
                                }
                            }
                        }
                    }
                }

                3 -> {
                    // Technical Performance Metrics
                    item {
                        WangiCard {
                            Text("Technician Efficiency Score", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("• Total Service Value Generated: UGX ${WangiViewModel.formatUgx(completedJobs.sumOf { it.serviceChargeUgx })}", fontSize = 13.sp)
                            Text("• First-Time Fix Rate: 94.2%", fontSize = 13.sp)
                            Text("• Average Resolution Time: 2.4 Hours", fontSize = 13.sp)
                            Text("• Customer Sign-Off Rate: 100%", fontSize = 13.sp)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // New Job Ticket Dialog
    if (showNewJobDialog) {
        AlertDialog(
            onDismissRequest = { showNewJobDialog = false },
            title = { Text("Create Network Ticket", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Job Title (e.g. 5G AP Installation)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newCustomer,
                        onValueChange = { newCustomer = it },
                        label = { Text("Customer / Site Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newPhone,
                        onValueChange = { newPhone = it },
                        label = { Text("Phone Number") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newAddress,
                        onValueChange = { newAddress = it },
                        label = { Text("Site Address / Floor") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newRouterModel,
                        onValueChange = { newRouterModel = it },
                        label = { Text("Hardware (MikroTik / Omada Model)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newChargeText,
                        onValueChange = { newChargeText = it },
                        label = { Text("Service Charge (UGX)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val charge = newChargeText.toLongOrNull() ?: 0L
                        if (newTitle.isNotBlank() && newCustomer.isNotBlank()) {
                            onCreateJob(newTitle, newCustomer, newPhone, newAddress, newRouterModel, newJobType, charge, newNotes)
                            showNewJobDialog = false
                            newTitle = ""
                            newCustomer = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WangiRed)
                ) {
                    Text("Create Ticket")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewJobDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Update Job Status Dialog
    if (selectedJobForUpdate != null) {
        val j = selectedJobForUpdate!!
        AlertDialog(
            onDismissRequest = { selectedJobForUpdate = null },
            title = { Text("Update Ticket: ${j.ticketNumber}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Site: ${j.siteAddress}")
                    Text("Status:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(JobStatus.ASSIGNED, JobStatus.IN_PROGRESS, JobStatus.COMPLETED).forEach { st ->
                            FilterChip(
                                selected = editStatus == st,
                                onClick = { editStatus = st },
                                label = { Text(st.name.replace("_", " "), fontSize = 10.sp) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = editMaterials,
                        onValueChange = { editMaterials = it },
                        label = { Text("Materials Used (e.g. 40m Cat6, RJ45)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editChargeText,
                        onValueChange = { editChargeText = it },
                        label = { Text("Service Charge (UGX)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editNotes,
                        onValueChange = { editNotes = it },
                        label = { Text("Technician Notes") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = editCustomerSignOff, onCheckedChange = { editCustomerSignOff = it })
                        Text("Customer Verbal / Signed Approval", fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val charge = editChargeText.toLongOrNull() ?: j.serviceChargeUgx
                        onUpdateJob(j.id, editStatus, charge, editMaterials, editNotes, editCustomerSignOff)
                        selectedJobForUpdate = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WangiSuccess)
                ) {
                    Text("Save Ticket")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedJobForUpdate = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
