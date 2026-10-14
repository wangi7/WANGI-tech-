package com.example.ui.viewmodel

import android.app.Application
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.repository.BusinessRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

enum class Portal(val title: String, val description: String) {
    CEO("CEO PORTAL", "Manage the entire WANGI business, finances, users, branches, reports, and system settings."),
    MANAGER("MANAGER PORTAL", "Manage authorized employees, approve shifts, verify sales, and confirm daily reports."),
    EMPLOYEE("EMPLOYEE PORTAL", "Access the employee's assigned work section and daily tasks."),
    TECHNICIAN("TECHNICIAN PORTAL", "Manage network installations, maintenance, faults, equipment, and technical jobs."),
    BILLING("Wi-Fi BILLING SYSTEM", "Manage customer internet packages, MikroTik routers, Omada networks, vouchers, subscriptions, and billing.")
}

sealed class Screen {
    object Splash : Screen()
    object PortalSelection : Screen()
    data class Login(val portal: Portal) : Screen()
    object CeoDashboard : Screen()
    object ManagerDashboard : Screen()
    object HotspotEmployeeDashboard : Screen()
    object GamingEmployeeDashboard : Screen()
    object TechnicianDashboard : Screen()
    object BillingDashboard : Screen()
    data class CeoSubModule(val title: String, val moduleId: String) : Screen()
}

class WangiViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application, viewModelScope)
    val repository = BusinessRepository(db)

    // Current State
    private val _currentScreen = MutableStateFlow<Screen>(Screen.Splash)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _selectedBranchId = MutableStateFlow("br_kampala")
    val selectedBranchId: StateFlow<String> = _selectedBranchId.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    private val _isAuthenticating = MutableStateFlow(false)
    val isAuthenticating: StateFlow<Boolean> = _isAuthenticating.asStateFlow()

    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    // Live Kampala Time
    private val _kampalaTime = MutableStateFlow("")
    val kampalaTime: StateFlow<String> = _kampalaTime.asStateFlow()

    private val _kampalaDate = MutableStateFlow("")
    val kampalaDate: StateFlow<String> = _kampalaDate.asStateFlow()

    // Data streams
    val users = repository.allUsers.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val branches = repository.allBranches.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val shifts = repository.allShifts.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val sales = repository.allSales.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val debts = repository.allDebts.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val unpaidDebts = repository.unpaidDebts.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val gamingSessions = repository.allGamingSessions.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val reports = repository.allReports.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val pendingReports = repository.pendingReports.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val techJobs = repository.allTechJobs.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val networkDevices = repository.allNetworkDevices.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val billingPackages = repository.allBillingPackages.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val billingSubscriptions = repository.allSubscriptions.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val wifiVouchers = repository.allWifiVouchers.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val paymentTransactions = repository.allPaymentTransactions.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val expenses = repository.allExpenses.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val notifications = repository.allNotifications.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val smsLogs = repository.allSmsLogs.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val auditEvents = repository.allAuditEvents.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val settings = repository.allSettings.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Tone generator for gaming timer alert
    private var toneGenerator: ToneGenerator? = null

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90)
        } catch (e: Exception) {
            // Audio permission or stream unavailable
        }

        // Clock Coroutine: Updates live time in Africa/Kampala time zone
        viewModelScope.launch {
            val tz = TimeZone.getTimeZone("Africa/Kampala")
            val timeFmt = SimpleDateFormat("hh:mm:ss a", Locale.US).apply { timeZone = tz }
            val dateFmt = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.US).apply { timeZone = tz }

            while (true) {
                val now = Date()
                _kampalaTime.value = timeFmt.format(now)
                _kampalaDate.value = dateFmt.format(now)
                delay(1000)
            }
        }
    }

    fun playAlertSound() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 400)
        } catch (e: Exception) {
            // Safe fallback
        }
    }

    fun navigateTo(screen: Screen) {
        _loginError.value = null
        _currentScreen.value = screen
    }

    fun selectBranch(branchId: String) {
        _selectedBranchId.value = branchId
    }

    fun clearFeedback() {
        _feedbackMessage.value = null
    }

    fun setFeedback(msg: String) {
        _feedbackMessage.value = msg
    }

    // Portal Click handler
    fun onPortalSelected(portal: Portal) {
        // Direct to portal's login screen
        _loginError.value = null
        _currentScreen.value = Screen.Login(portal)
    }

    // Login logic with strict portal validation
    fun login(portal: Portal, username: String, pass: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isAuthenticating.value = true
            _loginError.value = null
            delay(500) // smooth transition

            val user = repository.authenticate(username.trim(), pass.trim())
            _isAuthenticating.value = false

            if (user == null) {
                _loginError.value = "Invalid credentials. Please verify your username/phone and password."
                return@launch
            }

            // Verify portal authorization
            when (portal) {
                Portal.CEO -> {
                    if (user.role != UserRole.CEO) {
                        _loginError.value = "Access Denied: You do not have CEO privileges."
                        return@launch
                    }
                    _currentUser.value = user
                    _currentScreen.value = Screen.CeoDashboard
                }
                Portal.MANAGER -> {
                    if (user.role != UserRole.MANAGER && user.role != UserRole.CEO) {
                        _loginError.value = "Access Denied: Only authorized managers can enter this portal."
                        return@launch
                    }
                    _currentUser.value = user
                    _currentScreen.value = Screen.ManagerDashboard
                }
                Portal.EMPLOYEE -> {
                    if (user.role != UserRole.HOTSPOT_EMPLOYEE && user.role != UserRole.GAMING_EMPLOYEE) {
                        _loginError.value = "Access Denied: Only active operators can access the Employee Portal."
                        return@launch
                    }
                    _currentUser.value = user
                    // Strictly route based on employee's assigned department
                    if (user.department == Department.HOTSPOT) {
                        _currentScreen.value = Screen.HotspotEmployeeDashboard
                    } else if (user.department == Department.GAMING) {
                        _currentScreen.value = Screen.GamingEmployeeDashboard
                    } else {
                        _loginError.value = "No specific department workspace assigned. Contact CEO."
                    }
                }
                Portal.TECHNICIAN -> {
                    if (user.role != UserRole.TECHNICIAN && user.role != UserRole.CEO) {
                        _loginError.value = "Access Denied: Technician credentials required."
                        return@launch
                    }
                    _currentUser.value = user
                    _currentScreen.value = Screen.TechnicianDashboard
                }
                Portal.BILLING -> {
                    if (user.role != UserRole.BILLING_ADMIN && user.role != UserRole.CEO) {
                        _loginError.value = "Access Denied: Billing Administrator authorization required."
                        return@launch
                    }
                    _currentUser.value = user
                    _currentScreen.value = Screen.BillingDashboard
                }
            }
        }
    }

    // Quick demo login switcher for testing convenience
    fun quickLoginAs(user: User, portal: Portal) {
        viewModelScope.launch(Dispatchers.IO) {
            _currentUser.value = user
            when (portal) {
                Portal.CEO -> _currentScreen.value = Screen.CeoDashboard
                Portal.MANAGER -> _currentScreen.value = Screen.ManagerDashboard
                Portal.EMPLOYEE -> {
                    if (user.department == Department.HOTSPOT) {
                        _currentScreen.value = Screen.HotspotEmployeeDashboard
                    } else {
                        _currentScreen.value = Screen.GamingEmployeeDashboard
                    }
                }
                Portal.TECHNICIAN -> _currentScreen.value = Screen.TechnicianDashboard
                Portal.BILLING -> _currentScreen.value = Screen.BillingDashboard
            }
        }
    }

    fun logout() {
        _currentUser.value = null
        _currentScreen.value = Screen.PortalSelection
    }

    // Shift Actions
    fun clockIn(startTime: String, uniformCompliant: Boolean, notes: String = "") {
        val user = _currentUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val shift = repository.clockInShift(user, startTime, uniformCompliant, notes)
                _feedbackMessage.value = "Shift started! Commission rate: ${shift.commissionRatePercent}%"
            } catch (e: Exception) {
                _feedbackMessage.value = "Error: ${e.message}"
            }
        }
    }

    fun clockOut(shiftId: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repository.clockOutShift(shiftId, user)
            _feedbackMessage.value = "Shift ended successfully."
        }
    }

    // Hotspot Voucher Sale Action
    fun recordSale(
        customerName: String,
        customerPhone: String,
        denominationUgx: Long,
        quantity: Int,
        isPaid: Boolean,
        notes: String
    ) {
        val user = _currentUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.recordVoucherSale(
                    employee = user,
                    customerName = customerName.ifBlank { "Walk-in Customer" },
                    customerPhone = customerPhone.ifBlank { "N/A" },
                    denominationUgx = denominationUgx,
                    quantity = quantity,
                    isPaid = isPaid,
                    notes = notes
                )
                _feedbackMessage.value = "Sale recorded: UGX ${formatUgx(denominationUgx * quantity)} ${if (!isPaid) "(Debt Logged)" else "(Cash)"}"
            } catch (e: Exception) {
                _feedbackMessage.value = "Error: ${e.message}"
            }
        }
    }

    // Gaming Session Actions
    fun startGaming(
        consoleType: ConsoleType,
        stationNumber: Int,
        gameCategory: GameCategory,
        gameTitle: String,
        durationMinutes: Int,
        isPaid: Boolean,
        customerName: String
    ) {
        val user = _currentUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val session = repository.startGamingSession(
                    employee = user,
                    consoleType = consoleType,
                    consoleNumber = stationNumber,
                    gameCategory = gameCategory,
                    gameTitle = gameTitle,
                    durationMinutes = durationMinutes,
                    isPaid = isPaid,
                    customerName = customerName.ifBlank { "Station $stationNumber Player" }
                )
                _feedbackMessage.value = "Timer started for $consoleType Station $stationNumber (${durationMinutes} mins) - UGX ${formatUgx(session.totalAmountUgx)}"
            } catch (e: Exception) {
                _feedbackMessage.value = "Error: ${e.message}"
            }
        }
    }

    fun completeGaming(sessionId: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repository.completeGamingSession(sessionId, user)
            _feedbackMessage.value = "Gaming session completed."
        }
    }

    fun reverseGaming(sessionId: String, reason: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repository.reverseGamingSession(sessionId, reason, user)
            _feedbackMessage.value = "Session reversed and audited."
        }
    }

    // Customer Debt Settle
    fun settleDebt(debtId: String, amountPaid: Long) {
        val user = _currentUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val ok = repository.settleDebt(debtId, amountPaid, user)
            if (ok) {
                _feedbackMessage.value = "Debt payment of UGX ${formatUgx(amountPaid)} recorded successfully."
            }
        }
    }

    // Daily Report Actions
    fun submitReport(
        startTime: String,
        endTime: String,
        totalSales: Long,
        paidSales: Long,
        unpaidDebts: Long,
        expectedCash: Long,
        actualCash: Long,
        expenses: Long,
        commission: Long,
        notes: String
    ) {
        val user = _currentUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val report = repository.submitDailyReport(
                    employee = user,
                    shiftStartTime = startTime,
                    shiftEndTime = endTime,
                    totalSalesUgx = totalSales,
                    paidSalesUgx = paidSales,
                    unpaidDebtsUgx = unpaidDebts,
                    expectedCashUgx = expectedCash,
                    actualCashReceivedUgx = actualCash,
                    expensesUgx = expenses,
                    commissionEarnedUgx = commission,
                    notes = notes
                )
                _feedbackMessage.value = "Daily report ${report.referenceNumber} submitted! Status: UNDER REVIEW."
            } catch (e: Exception) {
                _feedbackMessage.value = "Error: ${e.message}"
            }
        }
    }

    fun confirmReport(reportId: String, notes: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val rep = repository.confirmDailyReport(reportId, user, notes)
                _feedbackMessage.value = "Report ${rep.referenceNumber} CONFIRMED! Transactional SMS & notification dispatched."
            } catch (e: Exception) {
                _feedbackMessage.value = "Failed: ${e.message}"
            }
        }
    }

    fun returnReport(reportId: String, reason: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val rep = repository.returnDailyReportForCorrection(reportId, user, reason)
                _feedbackMessage.value = "Report ${rep.referenceNumber} returned for correction."
            } catch (e: Exception) {
                _feedbackMessage.value = "Failed: ${e.message}"
            }
        }
    }

    // Technician Job Actions
    fun createTechJob(
        title: String,
        customerName: String,
        phone: String,
        address: String,
        model: String,
        jobType: JobType,
        serviceCharge: Long,
        notes: String
    ) {
        val user = _currentUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val job = TechnicianJob(
                id = "job_${UUID.randomUUID()}",
                ticketNumber = "TKT-2026-${(100..999).random()}",
                title = title,
                customerName = customerName,
                customerPhone = phone,
                siteAddress = address,
                routerModel = model,
                deviceType = "Router / AP",
                jobType = jobType,
                status = JobStatus.NEW,
                branchId = user.branchId,
                serviceChargeUgx = serviceCharge,
                notes = notes
            )
            repository.createTechnicianJob(job, user)
            _feedbackMessage.value = "Ticket ${job.ticketNumber} created successfully."
        }
    }

    fun updateTechJob(
        jobId: String,
        status: JobStatus,
        serviceCharge: Long,
        materials: String,
        notes: String,
        signOff: Boolean
    ) {
        val user = _currentUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateJobStatus(jobId, status, serviceCharge, materials, notes, signOff, user)
            _feedbackMessage.value = "Job ticket updated."
        }
    }

    // Wi-Fi Billing Payment Callback Processing
    fun testPaymentCallback(
        orderRef: String,
        providerTxId: String,
        phone: String,
        name: String,
        pkg: BillingPackage,
        provider: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val (success, result) = repository.processPaymentCallback(
                orderRef = orderRef,
                providerTxId = providerTxId,
                customerPhone = phone,
                customerName = name,
                packageName = pkg.name,
                packageId = pkg.id,
                amountUgx = pkg.priceUgx,
                provider = provider
            )
            if (success) {
                _feedbackMessage.value = "Payment VERIFIED! Generated Voucher: $result (Subscription Active)"
            } else {
                _feedbackMessage.value = "Callback Notice: $result (Duplicate prevented!)"
            }
        }
    }

    // Wi-Fi Billing Operations
    fun generateVouchers(packageItem: BillingPackage, quantity: Int, prefix: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val vouchers = repository.generateVoucherBatch(packageItem, quantity, prefix, user)
                _feedbackMessage.value = "Successfully generated ${vouchers.size} vouchers for ${packageItem.name}!"
            } catch (e: Exception) {
                _feedbackMessage.value = "Voucher generation failed: ${e.message}"
            }
        }
    }

    fun createBillingPackage(
        name: String,
        validity: String,
        priceUgx: Long,
        speedMbps: Int,
        quotaMb: Long,
        isHotspot: Boolean,
        isPppoe: Boolean,
        desc: String
    ) {
        val user = _currentUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val pkg = BillingPackage(
                id = "pkg_${UUID.randomUUID().toString().take(8)}",
                name = name,
                validityPeriod = validity,
                priceUgx = priceUgx,
                speedLimitMbps = speedMbps,
                quotaLimitMb = quotaMb,
                isActive = true,
                isHotspot = isHotspot,
                isPppoe = isPppoe,
                description = desc
            )
            repository.createBillingPackage(pkg, user)
            _feedbackMessage.value = "Package '$name' created successfully."
        }
    }

    fun updateBillingPackage(pkg: BillingPackage) {
        val user = _currentUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateBillingPackage(pkg, user)
            _feedbackMessage.value = "Package '${pkg.name}' updated."
        }
    }

    fun deleteBillingPackage(pkg: BillingPackage) {
        val user = _currentUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteBillingPackage(pkg, user)
            _feedbackMessage.value = "Package '${pkg.name}' deleted."
        }
    }

    fun disconnectSubscriber(sub: BillingSubscription) {
        val user = _currentUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repository.disconnectSubscriber(sub, user)
            _feedbackMessage.value = "Subscriber ${sub.customerName} disconnected."
        }
    }

    fun extendSubscription(sub: BillingSubscription, hours: Int) {
        val user = _currentUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repository.extendSubscription(sub, hours, user)
            _feedbackMessage.value = "Extended ${sub.customerName} by $hours hours."
        }
    }

    fun toggleBlockMac(sub: BillingSubscription) {
        val user = _currentUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repository.toggleBlockMac(sub, user)
            _feedbackMessage.value = "MAC ${sub.macAddress} block status updated."
        }
    }

    fun addNetworkDevice(name: String, model: String, vendor: String, ip: String, port: Int, branchId: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val dev = NetworkDevice(
                id = "dev_${UUID.randomUUID().toString().take(6)}",
                name = name,
                model = model,
                vendor = vendor,
                serialNumber = "SN-${(10000..99999).random()}",
                macAddress = "%02X:%02X:%02X:%02X:%02X:%02X".format(
                    (0..255).random(), (0..255).random(), (0..255).random(),
                    (0..255).random(), (0..255).random(), (0..255).random()
                ),
                ipAddress = ip,
                routerOsVersion = if (vendor == "MIKROTIK") "RouterOS v7.14.3" else "Omada Controller v5.13",
                siteLocation = "Branch Gateway Rack",
                branchId = branchId,
                status = "ONLINE",
                adminPort = port,
                configBackupDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                activeHotspotUsers = (1..20).random()
            )
            repository.addNetworkDevice(dev, user)
            _feedbackMessage.value = "Gateway '${dev.name}' registered."
        }
    }

    fun deleteNetworkDevice(device: NetworkDevice) {
        val user = _currentUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteNetworkDevice(device, user)
            _feedbackMessage.value = "Gateway '${device.name}' removed."
        }
    }

    fun sendVoucherSms(voucherCode: String, phone: String, packageName: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repository.sendVoucherSms(voucherCode, phone, packageName, user)
            _feedbackMessage.value = "Voucher sent via SMS to $phone."
        }
    }

    // Expenses
    fun addExpense(category: String, description: String, amountUgx: Long, department: Department) {
        val user = _currentUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repository.recordExpense(
                BusinessExpense(
                    id = "exp_${UUID.randomUUID()}",
                    branchId = user.branchId,
                    department = department,
                    category = category,
                    description = description,
                    amountUgx = amountUgx,
                    recordedBy = user.fullName,
                    date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                ),
                user
            )
            _feedbackMessage.value = "Expense UGX ${formatUgx(amountUgx)} saved."
        }
    }

    // Settings update
    fun saveSetting(key: String, value: String, description: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.updateSetting(key, value, description, user)
                _feedbackMessage.value = "Setting updated."
            } catch (e: Exception) {
                _feedbackMessage.value = "Error: ${e.message}"
            }
        }
    }

    fun registerEmployee(
        fullName: String,
        phone: String,
        username: String,
        department: Department,
        branchId: String,
        password: String,
        onSuccess: (User) -> Unit = {}
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val newUser = repository.registerEmployee(fullName, phone, username, department, branchId, password)
                _feedbackMessage.value = "Employee registered! Welcome SMS dispatched via Pandora Uganda Gateway to ${newUser.phone}."
                onSuccess(newUser)
            } catch (e: Exception) {
                _feedbackMessage.value = "Registration failed: ${e.message}"
            }
        }
    }

    fun sendPandoraSms(recipientPhone: String, message: String, reference: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.sendPandoraSms(recipientPhone, message, reference, user)
                _feedbackMessage.value = "SMS sent via Pandora Uganda Gateway to $recipientPhone!"
            } catch (e: Exception) {
                _feedbackMessage.value = "SMS error: ${e.message}"
            }
        }
    }

    fun resetPassword(
        identifier: String,
        newPassword: String,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val success = repository.resetPassword(identifier, newPassword)
                if (success) {
                    _feedbackMessage.value = "Password reset! Confirmation SMS sent via Pandora Uganda Gateway."
                    onResult(true, "Password reset successfully! You can now log in.")
                } else {
                    onResult(false, "User with identifier '$identifier' not found.")
                }
            } catch (e: Exception) {
                onResult(false, "Reset error: ${e.message}")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            toneGenerator?.release()
        } catch (e: Exception) {
        }
    }

    companion object {
        fun formatUgx(amount: Long): String {
            return NumberFormat.getNumberInstance(Locale.US).format(amount)
        }
    }
}
