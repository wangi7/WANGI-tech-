package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Department
import com.example.ui.animation.WangiStartupAnimation
import com.example.ui.auth.LoginScreen
import com.example.ui.billing.BillingDashboardScreen
import com.example.ui.ceo.CeoDashboardScreen
import com.example.ui.ceo.CeoSubModuleContainer
import com.example.ui.employee.gaming.GamingEmployeeScreen
import com.example.ui.employee.hotspot.HotspotEmployeeScreen
import com.example.ui.manager.ManagerDashboardScreen
import com.example.ui.portals.PortalLandingScreen
import com.example.ui.technician.TechnicianScreen
import com.example.ui.theme.WangiTechTheme
import com.example.ui.viewmodel.Portal
import com.example.ui.viewmodel.Screen
import com.example.ui.viewmodel.WangiViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: WangiViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            WangiTechTheme {
                val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
                val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
                val selectedBranchId by viewModel.selectedBranchId.collectAsStateWithLifecycle()
                val loginError by viewModel.loginError.collectAsStateWithLifecycle()
                val isAuthenticating by viewModel.isAuthenticating.collectAsStateWithLifecycle()
                val feedbackMessage by viewModel.feedbackMessage.collectAsStateWithLifecycle()

                val liveTime by viewModel.kampalaTime.collectAsStateWithLifecycle()
                val liveDate by viewModel.kampalaDate.collectAsStateWithLifecycle()

                val users by viewModel.users.collectAsStateWithLifecycle()
                val branches by viewModel.branches.collectAsStateWithLifecycle()
                val shifts by viewModel.shifts.collectAsStateWithLifecycle()
                val sales by viewModel.sales.collectAsStateWithLifecycle()
                val debts by viewModel.debts.collectAsStateWithLifecycle()
                val gamingSessions by viewModel.gamingSessions.collectAsStateWithLifecycle()
                val reports by viewModel.reports.collectAsStateWithLifecycle()
                val pendingReports by viewModel.pendingReports.collectAsStateWithLifecycle()
                val techJobs by viewModel.techJobs.collectAsStateWithLifecycle()
                val networkDevices by viewModel.networkDevices.collectAsStateWithLifecycle()
                val billingPackages by viewModel.billingPackages.collectAsStateWithLifecycle()
                val billingSubscriptions by viewModel.billingSubscriptions.collectAsStateWithLifecycle()
                val wifiVouchers by viewModel.wifiVouchers.collectAsStateWithLifecycle()
                val paymentTransactions by viewModel.paymentTransactions.collectAsStateWithLifecycle()
                val expenses by viewModel.expenses.collectAsStateWithLifecycle()
                val notifications by viewModel.notifications.collectAsStateWithLifecycle()
                val smsLogs by viewModel.smsLogs.collectAsStateWithLifecycle()
                val auditEvents by viewModel.auditEvents.collectAsStateWithLifecycle()
                val settings by viewModel.settings.collectAsStateWithLifecycle()

                val snackbarHostState = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()

                // Display snackbar feedback
                LaunchedEffect(feedbackMessage) {
                    feedbackMessage?.let { msg ->
                        scope.launch {
                            snackbarHostState.showSnackbar(msg)
                            viewModel.clearFeedback()
                        }
                    }
                }

                // Hardware / Predictive Back Handling
                BackHandler(enabled = currentScreen !is Screen.PortalSelection && currentScreen !is Screen.Splash) {
                    when (currentScreen) {
                        is Screen.Login -> viewModel.navigateTo(Screen.PortalSelection)
                        is Screen.CeoSubModule -> viewModel.navigateTo(Screen.CeoDashboard)
                        is Screen.CeoDashboard,
                        is Screen.ManagerDashboard,
                        is Screen.HotspotEmployeeDashboard,
                        is Screen.GamingEmployeeDashboard,
                        is Screen.TechnicianDashboard,
                        is Screen.BillingDashboard -> {
                            viewModel.logout()
                        }
                        else -> viewModel.navigateTo(Screen.PortalSelection)
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) { innerPadding ->
                    when (val screen = currentScreen) {
                        is Screen.Splash -> {
                            WangiStartupAnimation(
                                onAnimationComplete = {
                                    viewModel.navigateTo(Screen.PortalSelection)
                                }
                            )
                        }

                        is Screen.PortalSelection -> {
                            PortalLandingScreen(
                                liveTime = liveTime,
                                liveDate = liveDate,
                                onPortalSelected = { portal ->
                                    viewModel.onPortalSelected(portal)
                                }
                            )
                        }

                        is Screen.Login -> {
                            LoginScreen(
                                portal = screen.portal,
                                errorMessage = loginError,
                                isAuthenticating = isAuthenticating,
                                users = users,
                                onLoginClick = { u, p -> viewModel.login(screen.portal, u, p) },
                                onQuickLogin = { u -> viewModel.quickLoginAs(u, screen.portal) },
                                onBackClick = { viewModel.navigateTo(Screen.PortalSelection) }
                            )
                        }

                        is Screen.CeoDashboard -> {
                            currentUser?.let { user ->
                                CeoDashboardScreen(
                                    currentUser = user,
                                    branches = branches,
                                    selectedBranchId = selectedBranchId,
                                    onBranchSelected = { viewModel.selectBranch(it) },
                                    sales = sales,
                                    gamingSessions = gamingSessions,
                                    techJobs = techJobs,
                                    shifts = shifts,
                                    pendingReports = pendingReports,
                                    expenses = expenses,
                                    notifications = notifications,
                                    liveTime = liveTime,
                                    liveDate = liveDate,
                                    onNavigateToSubModule = { title, moduleId ->
                                        viewModel.navigateTo(Screen.CeoSubModule(title, moduleId))
                                    },
                                    onLogout = { viewModel.logout() },
                                    onConfirmReport = { repId, notes -> viewModel.confirmReport(repId, notes) },
                                    onReturnReport = { repId, reason -> viewModel.returnReport(repId, reason) }
                                )
                            }
                        }

                        is Screen.CeoSubModule -> {
                            currentUser?.let { user ->
                                CeoSubModuleContainer(
                                    title = screen.title,
                                    moduleId = screen.moduleId,
                                    currentUser = user,
                                    users = users,
                                    branches = branches,
                                    sales = sales,
                                    debts = debts,
                                    reports = reports,
                                    expenses = expenses,
                                    settings = settings,
                                    auditEvents = auditEvents,
                                    notifications = notifications,
                                    smsLogs = smsLogs,
                                    onBackClick = { viewModel.navigateTo(Screen.CeoDashboard) },
                                    onSaveSetting = { k, v, d -> viewModel.saveSetting(k, v, d) },
                                    onAddExpense = { c, d, a, dept -> viewModel.addExpense(c, d, a, dept) },
                                    onSettleDebt = { id, amt -> viewModel.settleDebt(id, amt) },
                                    onConfirmReport = { repId, notes -> viewModel.confirmReport(repId, notes) },
                                    onReturnReport = { repId, reason -> viewModel.returnReport(repId, reason) }
                                )
                            }
                        }

                        is Screen.ManagerDashboard -> {
                            currentUser?.let { user ->
                                val branch = branches.find { it.id == user.branchId }
                                ManagerDashboardScreen(
                                    currentUser = user,
                                    branch = branch,
                                    users = users,
                                    shifts = shifts,
                                    pendingReports = pendingReports,
                                    sales = sales,
                                    gamingSessions = gamingSessions,
                                    notifications = notifications,
                                    liveTime = liveTime,
                                    liveDate = liveDate,
                                    onConfirmReport = { repId, notes -> viewModel.confirmReport(repId, notes) },
                                    onReturnReport = { repId, reason -> viewModel.returnReport(repId, reason) },
                                    onLogout = { viewModel.logout() }
                                )
                            }
                        }

                        is Screen.HotspotEmployeeDashboard -> {
                            currentUser?.let { user ->
                                val branch = branches.find { it.id == user.branchId }
                                HotspotEmployeeScreen(
                                    currentUser = user,
                                    branch = branch,
                                    shifts = shifts,
                                    sales = sales,
                                    debts = debts,
                                    reports = reports,
                                    notifications = notifications,
                                    liveTime = liveTime,
                                    liveDate = liveDate,
                                    onClockIn = { time, uniform, notes -> viewModel.clockIn(time, uniform, notes) },
                                    onClockOut = { shfId -> viewModel.clockOut(shfId) },
                                    onRecordSale = { name, phone, denom, qty, isPaid, notes ->
                                        viewModel.recordSale(name, phone, denom, qty, isPaid, notes)
                                    },
                                    onSettleDebt = { id, amt -> viewModel.settleDebt(id, amt) },
                                    onSubmitReport = { start, end, total, paid, unpaid, expCash, actCash, exp, comm, notes ->
                                        viewModel.submitReport(start, end, total, paid, unpaid, expCash, actCash, exp, comm, notes)
                                    },
                                    onLogout = { viewModel.logout() }
                                )
                            }
                        }

                        is Screen.GamingEmployeeDashboard -> {
                            currentUser?.let { user ->
                                val branch = branches.find { it.id == user.branchId }
                                GamingEmployeeScreen(
                                    currentUser = user,
                                    branch = branch,
                                    shifts = shifts,
                                    sessions = gamingSessions,
                                    debts = debts,
                                    reports = reports,
                                    notifications = notifications,
                                    liveTime = liveTime,
                                    liveDate = liveDate,
                                    onStartGaming = { console, st, cat, title, dur, isPaid, cust ->
                                        viewModel.startGaming(console, st, cat, title, dur, isPaid, cust)
                                    },
                                    onCompleteGaming = { id -> viewModel.completeGaming(id) },
                                    onReverseGaming = { id, reason -> viewModel.reverseGaming(id, reason) },
                                    onClockIn = { time, uniform, notes -> viewModel.clockIn(time, uniform, notes) },
                                    onClockOut = { shfId -> viewModel.clockOut(shfId) },
                                    onSubmitReport = { start, end, total, paid, unpaid, expCash, actCash, exp, comm, notes ->
                                        viewModel.submitReport(start, end, total, paid, unpaid, expCash, actCash, exp, comm, notes)
                                    },
                                    onPlayAlertSound = { viewModel.playAlertSound() },
                                    onLogout = { viewModel.logout() }
                                )
                            }
                        }

                        is Screen.TechnicianDashboard -> {
                            currentUser?.let { user ->
                                TechnicianScreen(
                                    currentUser = user,
                                    jobs = techJobs,
                                    devices = networkDevices,
                                    notifications = notifications,
                                    liveTime = liveTime,
                                    liveDate = liveDate,
                                    onCreateJob = { title, cust, phone, addr, model, type, charge, notes ->
                                        viewModel.createTechJob(title, cust, phone, addr, model, type, charge, notes)
                                    },
                                    onUpdateJob = { id, status, charge, mat, notes, sign ->
                                        viewModel.updateTechJob(id, status, charge, mat, notes, sign)
                                    },
                                    onLogout = { viewModel.logout() }
                                )
                            }
                        }

                        is Screen.BillingDashboard -> {
                            currentUser?.let { user ->
                                BillingDashboardScreen(
                                    currentUser = user,
                                    packages = billingPackages,
                                    subscriptions = billingSubscriptions,
                                    vouchers = wifiVouchers,
                                    transactions = paymentTransactions,
                                    devices = networkDevices,
                                    notifications = notifications,
                                    liveTime = liveTime,
                                    liveDate = liveDate,
                                    onTestPaymentCallback = { orderRef, txId, phone, name, pkg, prov ->
                                        viewModel.testPaymentCallback(orderRef, txId, phone, name, pkg, prov)
                                    },
                                    onGenerateVouchers = { pkg, qty, pfx ->
                                        viewModel.generateVouchers(pkg, qty, pfx)
                                    },
                                    onCreatePackage = { name, valPer, price, speed, quota, isHot, isPpp, desc ->
                                        viewModel.createBillingPackage(name, valPer, price, speed, quota, isHot, isPpp, desc)
                                    },
                                    onUpdatePackage = { pkg ->
                                        viewModel.updateBillingPackage(pkg)
                                    },
                                    onDeletePackage = { pkg ->
                                        viewModel.deleteBillingPackage(pkg)
                                    },
                                    onDisconnectSubscriber = { sub ->
                                        viewModel.disconnectSubscriber(sub)
                                    },
                                    onExtendSubscription = { sub, hours ->
                                        viewModel.extendSubscription(sub, hours)
                                    },
                                    onToggleBlockMac = { sub ->
                                        viewModel.toggleBlockMac(sub)
                                    },
                                    onAddDevice = { name, model, vendor, ip, port, branchId ->
                                        viewModel.addNetworkDevice(name, model, vendor, ip, port, branchId)
                                    },
                                    onDeleteDevice = { dev ->
                                        viewModel.deleteNetworkDevice(dev)
                                    },
                                    onSendVoucherSms = { code, phone, pkgName ->
                                        viewModel.sendVoucherSms(code, phone, pkgName)
                                    },
                                    onLogout = { viewModel.logout() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
