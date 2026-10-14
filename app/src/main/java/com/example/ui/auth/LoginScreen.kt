package com.example.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Department
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.ui.theme.*
import com.example.ui.viewmodel.Portal

@Composable
fun LoginScreen(
    portal: Portal,
    errorMessage: String?,
    isAuthenticating: Boolean,
    users: List<User>,
    onLoginClick: (String, String) -> Unit,
    onQuickLogin: (User) -> Unit,
    onRegisterEmployee: (String, String, String, Department, String, String) -> Unit = { _, _, _, _, _, _ -> },
    onResetPassword: (String, String, (Boolean, String) -> Unit) -> Unit = { _, _, _ -> },
    onBackClick: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // Dialog States
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var showRegisterEmployeeDialog by remember { mutableStateOf(false) }

    // Forgot Password Flow States
    var forgotStep by remember { mutableIntStateOf(1) } // 1 = identifier & request OTP, 2 = enter OTP & new password
    var forgotIdentifier by remember { mutableStateOf("") }
    var generatedOtp by remember { mutableStateOf("") }
    var enteredOtp by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmNewPassword by remember { mutableStateOf("") }
    var forgotMessage by remember { mutableStateOf<String?>(null) }
    var forgotIsError by remember { mutableStateOf(false) }

    // Employee Registration States
    var regFullName by remember { mutableStateOf("") }
    var regPhone by remember { mutableStateOf("+256") }
    var regUsername by remember { mutableStateOf("") }
    var regDept by remember { mutableStateOf(Department.HOTSPOT) }
    var regBranchId by remember { mutableStateOf("br_kampala") }
    var regPassword by remember { mutableStateOf("") }

    // Pre-populate with typical credentials when clicking a quick demo chip
    val portalDefaultUser = when (portal) {
        Portal.CEO -> users.find { it.username == "ceo" }
        Portal.MANAGER -> users.find { it.username == "manager" }
        Portal.EMPLOYEE -> users.find { it.username == "hotspot_emp" }
        Portal.TECHNICIAN -> users.find { it.username == "tech_john" }
        Portal.BILLING -> users.find { it.username == "billing_admin" }
    }

    val employeeUsers = users.filter {
        it.role == UserRole.HOTSPOT_EMPLOYEE || it.role == UserRole.GAMING_EMPLOYEE
    }

    Scaffold(
        containerColor = WangiBackground,
        topBar = {
            Surface(
                color = WangiWhite,
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .testTag("btn_back_to_portals")
                            .size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = WangiCharcoal
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = portal.title,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = WangiCharcoal
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Branded Icon Header
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(WangiRed, WangiDarkRed)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (portal) {
                        Portal.CEO -> Icons.Default.MilitaryTech
                        Portal.MANAGER -> Icons.Default.AssignmentInd
                        Portal.EMPLOYEE -> Icons.Default.Badge
                        Portal.TECHNICIAN -> Icons.Default.Build
                        Portal.BILLING -> Icons.Default.WifiTethering
                    },
                    contentDescription = null,
                    tint = WangiWhite,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "${portal.title} Access",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = WangiCharcoal
            )

            Text(
                text = "Authenticate with your registered WANGI credentials.",
                fontSize = 12.5.sp,
                color = WangiTextSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Error Banner
            AnimatedVisibility(visible = errorMessage != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = WangiLightRed,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    border = BorderStroke(1.dp, WangiRed.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = "Error",
                            tint = WangiRed
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = errorMessage ?: "",
                            color = WangiDarkRed,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Login Card (Airtel style: Clean white card, rounded corners, subtle border)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = WangiWhite,
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 2.dp,
                border = BorderStroke(1.dp, WangiBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Sign In",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = WangiCharcoal
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Username / Phone field
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username or Registered Phone") },
                        placeholder = { Text("e.g. brian_mukasa or 0755927896") },
                        leadingIcon = {
                            Icon(Icons.Default.PersonOutline, contentDescription = null, tint = WangiTextSecondary)
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_username"),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Password field
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = WangiTextSecondary)
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                    tint = WangiTextSecondary
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { onLoginClick(username, password) }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_password"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = "Forgot Password?",
                            color = WangiRed,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clickable {
                                    forgotStep = 1
                                    forgotIdentifier = username
                                    forgotMessage = null
                                    showForgotPasswordDialog = true
                                }
                                .padding(4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Sign In Button
                    Button(
                        onClick = { onLoginClick(username, password) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("btn_sign_in"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WangiRed,
                            contentColor = WangiWhite
                        ),
                        enabled = !isAuthenticating
                    ) {
                        if (isAuthenticating) {
                            CircularProgressIndicator(
                                color = WangiWhite,
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Text(
                                text = "Sign In to ${portal.name}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Employee Portal: Register New Staff & Accounts List
            if (portal == Portal.EMPLOYEE) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = WangiWhite,
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, WangiBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "STAFF ACCOUNTS (${employeeUsers.size})",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WangiTextSecondary,
                                    letterSpacing = 0.5.sp
                                )
                                Text("Registered employees appear here automatically", fontSize = 10.5.sp, color = WangiTextMuted)
                            }

                            Button(
                                onClick = { showRegisterEmployeeDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = WangiRed),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Register Staff", fontSize = 11.5.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Chips for all employees (including newly created ones!)
                        employeeUsers.forEach { emp ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (username == emp.username) WangiLightRed else WangiBackground,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        username = emp.username
                                        password = emp.passwordHash
                                        onQuickLogin(emp)
                                    },
                                border = BorderStroke(1.dp, if (username == emp.username) WangiRed else WangiBorder)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(if (emp.department == Department.HOTSPOT) WangiLightRed else Color(0xFFEFF6FF)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                if (emp.department == Department.HOTSPOT) Icons.Default.Wifi else Icons.Default.SportsEsports,
                                                contentDescription = null,
                                                tint = if (emp.department == Department.HOTSPOT) WangiRed else Color(0xFF2563EB),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(emp.fullName, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                                            Text("@${emp.username} • ${emp.department} • ${emp.phone}", fontSize = 11.5.sp, color = WangiTextSecondary)
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = WangiGoldLight
                                    ) {
                                        Text(
                                            text = "1-Tap Enter",
                                            color = WangiGoldDark,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Quick 1-Click Test Login for other portals
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = WangiWhite,
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, WangiBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "AUTHORIZED TEST PROFILE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = WangiTextSecondary,
                                letterSpacing = 0.5.sp
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = WangiGoldLight
                            ) {
                                Text(
                                    text = "Instant Enter",
                                    color = WangiGoldDark,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (portalDefaultUser != null) {
                            FilledTonalButton(
                                onClick = {
                                    username = portalDefaultUser.username
                                    password = portalDefaultUser.passwordHash
                                    onQuickLogin(portalDefaultUser)
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("btn_quick_fill_authorized")
                            ) {
                                Icon(Icons.Default.FlashOn, contentDescription = null, tint = WangiGoldDark)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Enter as ${portalDefaultUser.fullName} (${portalDefaultUser.role})",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // ==============================================================
    // FORGOT PASSWORD FLOW (2-STEP RESET WITH PANDORA SMS OTP)
    // ==============================================================
    if (showForgotPasswordDialog) {
        AlertDialog(
            onDismissRequest = {
                showForgotPasswordDialog = false
                forgotMessage = null
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LockReset, contentDescription = null, tint = WangiRed)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Password Recovery & Reset", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (forgotStep == 1) {
                        Text(
                            "Enter your username or registered mobile number. We will dispatch a 4-digit verification code via Pandora Uganda SMS Gateway:",
                            fontSize = 12.5.sp,
                            color = WangiTextSecondary
                        )

                        OutlinedTextField(
                            value = forgotIdentifier,
                            onValueChange = { forgotIdentifier = it },
                            label = { Text("Username or Phone") },
                            placeholder = { Text("e.g. brian_mukasa or 0755927896") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (forgotMessage != null) {
                            Text(
                                text = forgotMessage!!,
                                color = if (forgotIsError) WangiRed else WangiSuccess,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else {
                        // Step 2: Enter OTP & Set New Password
                        Text(
                            "Verification code dispatched to your registered phone via Pandora SMS. Enter the code and your new password:",
                            fontSize = 12.5.sp,
                            color = WangiTextSecondary
                        )

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFE8F5E9),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Pandora SMS OTP Dispatched: $generatedOtp",
                                color = WangiSuccess,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(8.dp)
                            )
                        }

                        OutlinedTextField(
                            value = enteredOtp,
                            onValueChange = { enteredOtp = it },
                            label = { Text("Enter 4-Digit OTP") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )

                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = { newPassword = it },
                            label = { Text("New Password") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = PasswordVisualTransformation()
                        )

                        OutlinedTextField(
                            value = confirmNewPassword,
                            onValueChange = { confirmNewPassword = it },
                            label = { Text("Confirm New Password") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = PasswordVisualTransformation()
                        )

                        if (forgotMessage != null) {
                            Text(
                                text = forgotMessage!!,
                                color = if (forgotIsError) WangiRed else WangiSuccess,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            },
            confirmButton = {
                if (forgotStep == 1) {
                    Button(
                        onClick = {
                            if (forgotIdentifier.isNotBlank()) {
                                val otp = (1000..9999).random().toString()
                                generatedOtp = otp
                                forgotStep = 2
                                forgotMessage = "OTP $otp dispatched via Pandora Uganda SMS Gateway."
                                forgotIsError = false
                            } else {
                                forgotMessage = "Please enter your username or phone."
                                forgotIsError = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = WangiRed)
                    ) {
                        Text("Send OTP via Pandora SMS")
                    }
                } else {
                    Button(
                        onClick = {
                            if (enteredOtp != generatedOtp) {
                                forgotMessage = "Incorrect OTP code. Please enter $generatedOtp."
                                forgotIsError = true
                            } else if (newPassword.isBlank() || newPassword != confirmNewPassword) {
                                forgotMessage = "Passwords do not match or are empty."
                                forgotIsError = true
                            } else {
                                onResetPassword(forgotIdentifier, newPassword) { success, msg ->
                                    if (success) {
                                        password = newPassword
                                        showForgotPasswordDialog = false
                                    } else {
                                        forgotMessage = msg
                                        forgotIsError = true
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = WangiSuccess)
                    ) {
                        Text("Reset & Save Password")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showForgotPasswordDialog = false
                    forgotMessage = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    // ==============================================================
    // REGISTER NEW EMPLOYEE DIALOG (WELCOME SMS VIA PANDORA)
    // ==============================================================
    if (showRegisterEmployeeDialog) {
        AlertDialog(
            onDismissRequest = { showRegisterEmployeeDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, tint = WangiRed)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Register Staff Member", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "New employee will automatically receive welcome SMS credentials via Pandora Uganda SMS Gateway.",
                        fontSize = 12.sp,
                        color = WangiTextSecondary
                    )

                    OutlinedTextField(
                        value = regFullName,
                        onValueChange = { regFullName = it },
                        label = { Text("Full Name (e.g. Julius Sempala)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = regPhone,
                        onValueChange = { regPhone = it },
                        label = { Text("Mobile Phone Number (+256...)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = regUsername,
                        onValueChange = { regUsername = it.lowercase().replace(" ", "_") },
                        label = { Text("Username") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Assigned Department:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = regDept == Department.HOTSPOT,
                            onClick = { regDept = Department.HOTSPOT },
                            label = { Text("Wi-Fi Hotspot") }
                        )
                        FilterChip(
                            selected = regDept == Department.GAMING,
                            onClick = { regDept = Department.GAMING },
                            label = { Text("Gaming Station") }
                        )
                    }

                    Text("Branch Location:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            "br_kampala" to "Kampala",
                            "br_entebbe" to "Entebbe",
                            "br_mukono" to "Mukono"
                        ).forEach { (id, name) ->
                            FilterChip(
                                selected = regBranchId == id,
                                onClick = { regBranchId = id },
                                label = { Text(name) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = regPassword,
                        onValueChange = { regPassword = it },
                        label = { Text("Initial Password") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (regFullName.isNotBlank() && regUsername.isNotBlank() && regPassword.isNotBlank()) {
                            onRegisterEmployee(
                                regFullName,
                                regPhone,
                                regUsername,
                                regDept,
                                regBranchId,
                                regPassword
                            )
                            username = regUsername
                            password = regPassword
                            showRegisterEmployeeDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WangiRed)
                ) {
                    Text("Register & Send SMS")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRegisterEmployeeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
