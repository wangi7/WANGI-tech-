package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        User::class,
        Branch::class,
        Shift::class,
        VoucherInventory::class,
        VoucherSale::class,
        CustomerDebt::class,
        GamingSession::class,
        DailyReport::class,
        ReportHistory::class,
        TechnicianJob::class,
        NetworkDevice::class,
        BillingPackage::class,
        BillingSubscription::class,
        WifiVoucher::class,
        PaymentTransaction::class,
        BusinessExpense::class,
        SalaryRecord::class,
        NotificationItem::class,
        SmsLog::class,
        AuditEvent::class,
        SystemSetting::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun branchDao(): BranchDao
    abstract fun shiftDao(): ShiftDao
    abstract fun voucherDao(): VoucherDao
    abstract fun debtDao(): DebtDao
    abstract fun gamingDao(): GamingDao
    abstract fun reportDao(): ReportDao
    abstract fun technicianDao(): TechnicianDao
    abstract fun deviceDao(): DeviceDao
    abstract fun billingDao(): BillingDao
    abstract fun financeDao(): FinanceDao
    abstract fun notificationDao(): NotificationDao
    abstract fun auditDao(): AuditDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "wangi_tech_solutions.db"
                )
                    .addCallback(AppDatabaseCallback(scope))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class AppDatabaseCallback(
        private val scope: CoroutineScope
    ) : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateInitialData(database)
                }
            }
        }

        suspend fun populateInitialData(db: AppDatabase) {
            // Seed Users
            val users = listOf(
                User(
                    id = "usr_ceo",
                    username = "ceo",
                    fullName = "Mudhaffar Wangi Ssebiraalo",
                    phone = "+256755927896",
                    email = "ceo@wangitech.com",
                    role = UserRole.CEO,
                    department = Department.ALL,
                    branchId = "br_kampala",
                    passwordHash = "wangi@2026"
                ),
                User(
                    id = "usr_mgr",
                    username = "manager",
                    fullName = "Sarah Nanteza",
                    phone = "+256701234567",
                    email = "manager@wangitech.com",
                    role = UserRole.MANAGER,
                    department = Department.ALL,
                    branchId = "br_kampala",
                    passwordHash = "manager123"
                ),
                User(
                    id = "usr_hotspot",
                    username = "hotspot_emp",
                    fullName = "Brian Mukasa",
                    phone = "+256782345678",
                    email = "brian@wangitech.com",
                    role = UserRole.HOTSPOT_EMPLOYEE,
                    department = Department.HOTSPOT,
                    branchId = "br_kampala",
                    passwordHash = "hotspot123"
                ),
                User(
                    id = "usr_gaming",
                    username = "gaming_emp",
                    fullName = "David Kato",
                    phone = "+256773456789",
                    email = "david@wangitech.com",
                    role = UserRole.GAMING_EMPLOYEE,
                    department = Department.GAMING,
                    branchId = "br_kampala",
                    passwordHash = "gaming123"
                ),
                User(
                    id = "usr_tech",
                    username = "tech_john",
                    fullName = "John Semwogerere",
                    phone = "+256754567890",
                    email = "john.tech@wangitech.com",
                    role = UserRole.TECHNICIAN,
                    department = Department.TECHNICAL,
                    branchId = "br_kampala",
                    passwordHash = "tech123"
                ),
                User(
                    id = "usr_billing",
                    username = "billing_admin",
                    fullName = "Amina Namubiru",
                    phone = "+256705678901",
                    email = "billing@wangitech.com",
                    role = UserRole.BILLING_ADMIN,
                    department = Department.BILLING,
                    branchId = "br_kampala",
                    passwordHash = "billing123"
                )
            )
            db.userDao().insertUsers(users)

            // Seed Branches
            val branches = listOf(
                Branch(
                    id = "br_kampala",
                    name = "Kampala Main Hub",
                    code = "KLA-01",
                    location = "Kampala Road, City Centre",
                    managerId = "usr_mgr",
                    phone = "+256701234567",
                    activeRoutersCount = 4,
                    activeGamingConsolesCount = 6
                ),
                Branch(
                    id = "br_entebbe",
                    name = "Entebbe Branch",
                    code = "EBB-02",
                    location = "Airport Road, Entebbe",
                    managerId = "usr_mgr",
                    phone = "+256702345678",
                    activeRoutersCount = 2,
                    activeGamingConsolesCount = 4
                ),
                Branch(
                    id = "br_mukono",
                    name = "Mukono Branch",
                    code = "MKN-03",
                    location = "Jinja Highway, Mukono",
                    managerId = "usr_mgr",
                    phone = "+256703456789",
                    activeRoutersCount = 2,
                    activeGamingConsolesCount = 3
                )
            )
            db.branchDao().insertBranches(branches)

            // Seed Internet Packages (UGX)
            val packages = listOf(
                BillingPackage("pkg_chap", "Chap Chap", "1 Hour", 500, 5, 0, true, true, false, "Fast ultra-short pass for emergencies"),
                BillingPackage("pkg_daily", "Daily Pass", "24 Hours", 1000, 10, 0, true, true, false, "24-Hour full speed access"),
                BillingPackage("pkg_3days", "Three Days", "72 Hours", 2000, 10, 0, true, true, false, "3 Days non-stop internet"),
                BillingPackage("pkg_weekly", "Weekly Unlimited", "7 Days", 5000, 15, 0, true, true, false, "Best value 7-day connection"),
                BillingPackage("pkg_monthly", "Monthly Premium", "30 Days", 20000, 20, 0, true, true, true, "30 Days high-speed bandwidth")
            )
            db.billingDao().insertPackages(packages)

            // Seed Initial Wi-Fi Vouchers Batch
            val initialVouchers = listOf(
                WifiVoucher("vc_1", "BATCH-2026-B1", "WG-849201", "7712", "Chap Chap", "pkg_chap", 500, "1 Hour", 5, "AVAILABLE"),
                WifiVoucher("vc_2", "BATCH-2026-B1", "WG-849202", "8823", "Daily Pass", "pkg_daily", 1000, "24 Hours", 10, "AVAILABLE"),
                WifiVoucher("vc_3", "BATCH-2026-B1", "WG-849203", "3341", "Daily Pass", "pkg_daily", 1000, "24 Hours", 10, "AVAILABLE"),
                WifiVoucher("vc_4", "BATCH-2026-B1", "WG-849204", "9904", "Three Days", "pkg_3days", 2000, "72 Hours", 10, "AVAILABLE"),
                WifiVoucher("vc_5", "BATCH-2026-B1", "WG-849205", "1256", "Weekly Unlimited", "pkg_weekly", 5000, "7 Days", 15, "AVAILABLE"),
                WifiVoucher("vc_6", "BATCH-2026-B1", "WG-849206", "4412", "Monthly Premium", "pkg_monthly", 20000, "30 Days", 20, "AVAILABLE"),
                WifiVoucher("vc_7", "BATCH-2026-B1", "WG-731001", "5512", "Daily Pass", "pkg_daily", 1000, "24 Hours", 10, "ACTIVE", "0755927896", "Derrick Katamba", System.currentTimeMillis() - 7200000, System.currentTimeMillis() + 79200000),
                WifiVoucher("vc_8", "BATCH-2026-B1", "WG-731002", "6623", "Weekly Unlimited", "pkg_weekly", 5000, "7 Days", 15, "ACTIVE", "0788114422", "Sarah Kyomugisha", System.currentTimeMillis() - 86400000, System.currentTimeMillis() + (6 * 86400000L))
            )
            db.billingDao().insertWifiVouchers(initialVouchers)

            // Seed Initial Active Hotspot & PPPoE Subscriptions
            val initialSubs = listOf(
                BillingSubscription(
                    id = "sub_seed_1",
                    customerName = "Derrick Katamba",
                    customerPhone = "0755927896",
                    packageName = "Daily Pass",
                    packageId = "pkg_daily",
                    voucherCode = "WG-731001",
                    routerId = "dev_1",
                    priceUgx = 1000,
                    startedAt = System.currentTimeMillis() - 7200000,
                    expiresAt = System.currentTimeMillis() + 79200000,
                    status = "ACTIVE",
                    paymentTxRef = "TXN-AIRTEL-4819",
                    ipAddress = "192.168.88.114",
                    macAddress = "B4:86:55:A1:C9:22",
                    bytesDownloadedMb = 342,
                    bytesUploadedMb = 58
                ),
                BillingSubscription(
                    id = "sub_seed_2",
                    customerName = "Sarah Kyomugisha",
                    customerPhone = "0788114422",
                    packageName = "Weekly Unlimited",
                    packageId = "pkg_weekly",
                    voucherCode = "WG-731002",
                    routerId = "dev_1",
                    priceUgx = 5000,
                    startedAt = System.currentTimeMillis() - 86400000,
                    expiresAt = System.currentTimeMillis() + (6 * 86400000L),
                    status = "ACTIVE",
                    paymentTxRef = "TXN-MTN-9932",
                    ipAddress = "192.168.88.145",
                    macAddress = "3C:52:82:78:E1:90",
                    bytesDownloadedMb = 1420,
                    bytesUploadedMb = 230
                ),
                BillingSubscription(
                    id = "sub_seed_3",
                    customerName = "Apex Digital Studios",
                    customerPhone = "0701992211",
                    packageName = "Monthly Premium",
                    packageId = "pkg_monthly",
                    voucherCode = "PPPOE-APEX-01",
                    routerId = "dev_2",
                    priceUgx = 20000,
                    startedAt = System.currentTimeMillis() - (5 * 86400000L),
                    expiresAt = System.currentTimeMillis() + (25 * 86400000L),
                    status = "ACTIVE",
                    paymentTxRef = "TXN-AIRTEL-8120",
                    ipAddress = "192.168.89.50",
                    macAddress = "E0:D5:5E:33:44:11",
                    bytesDownloadedMb = 12450,
                    bytesUploadedMb = 4300
                )
            )
            initialSubs.forEach { db.billingDao().insertSubscription(it) }

            // Seed Initial Payment Transactions
            val initialTransactions = listOf(
                PaymentTransaction(
                    id = "tx_seed_1",
                    orderRef = "ORD-894101",
                    providerTxId = "TXN-AIRTEL-4819",
                    customerPhone = "0755927896",
                    customerName = "Derrick Katamba",
                    packageName = "Daily Pass",
                    amountUgx = 1000,
                    provider = "AIRTEL_MONEY",
                    status = "VERIFIED",
                    callbackProcessedAt = System.currentTimeMillis() - 7200000
                ),
                PaymentTransaction(
                    id = "tx_seed_2",
                    orderRef = "ORD-894102",
                    providerTxId = "TXN-MTN-9932",
                    customerPhone = "0788114422",
                    customerName = "Sarah Kyomugisha",
                    packageName = "Weekly Unlimited",
                    amountUgx = 5000,
                    provider = "MTN_MOMO",
                    status = "VERIFIED",
                    callbackProcessedAt = System.currentTimeMillis() - 86400000
                ),
                PaymentTransaction(
                    id = "tx_seed_3",
                    orderRef = "ORD-894103",
                    providerTxId = "TXN-AIRTEL-8120",
                    customerPhone = "0701992211",
                    customerName = "Apex Digital Studios",
                    packageName = "Monthly Premium",
                    amountUgx = 20000,
                    provider = "AIRTEL_MONEY",
                    status = "VERIFIED",
                    callbackProcessedAt = System.currentTimeMillis() - (5 * 86400000L)
                )
            )
            initialTransactions.forEach { db.billingDao().insertTransaction(it) }

            // Seed Voucher Inventory for Brian (Hotspot Employee)
            val inventory = listOf(
                VoucherInventory("inv_500", "br_kampala", "usr_hotspot", 500, 50, 12, 38, 0, "2026-10-14"),
                VoucherInventory("inv_1000", "br_kampala", "usr_hotspot", 1000, 40, 18, 22, 0, "2026-10-14"),
                VoucherInventory("inv_2000", "br_kampala", "usr_hotspot", 2000, 30, 8, 22, 0, "2026-10-14"),
                VoucherInventory("inv_5000", "br_kampala", "usr_hotspot", 5000, 20, 5, 15, 0, "2026-10-14"),
                VoucherInventory("inv_20000", "br_kampala", "usr_hotspot", 20000, 10, 2, 8, 0, "2026-10-14")
            )
            db.voucherDao().insertInventory(inventory)

            // Seed Initial Sales
            val sales = listOf(
                VoucherSale("sale_1", "br_kampala", "usr_hotspot", "Brian Mukasa", "Alex Musoke", "0788112233", 1000, 1, 1000, true, null, System.currentTimeMillis() - 3600000),
                VoucherSale("sale_2", "br_kampala", "usr_hotspot", "Brian Mukasa", "Juliet Nakato", "0755334455", 5000, 1, 5000, true, null, System.currentTimeMillis() - 7200000),
                VoucherSale("sale_3", "br_kampala", "usr_hotspot", "Brian Mukasa", "Tom Byaruhanga", "0700998877", 2000, 1, 2000, false, "DEBT-2026-001", System.currentTimeMillis() - 10800000)
            )
            sales.forEach { db.voucherDao().insertSale(it) }

            // Seed Initial Customer Debt
            db.debtDao().insertDebt(
                CustomerDebt(
                    id = "debt_1",
                    referenceCode = "DEBT-2026-001",
                    branchId = "br_kampala",
                    customerName = "Tom Byaruhanga",
                    customerPhone = "0700998877",
                    department = Department.HOTSPOT,
                    amountUgx = 2000,
                    amountPaidUgx = 0,
                    isPaid = false,
                    recordedByEmployeeId = "usr_hotspot",
                    recordedDate = "2026-10-14",
                    notes = "Promised to pay tomorrow morning"
                )
            )

            // Seed Initial Gaming Sessions
            val gamingSessions = listOf(
                GamingSession(
                    id = "gs_1",
                    branchId = "br_kampala",
                    employeeId = "usr_gaming",
                    consoleType = ConsoleType.PS4,
                    consoleNumber = 1,
                    gameCategory = GameCategory.PES_FIFA,
                    gameTitle = "FIFA 24",
                    durationMinutes = 20,
                    rateUgx = 4000,
                    totalAmountUgx = 4000,
                    isPaid = true,
                    customerName = "Ronald",
                    startTimeMillis = System.currentTimeMillis() - 3600000,
                    endTimeMillis = System.currentTimeMillis() - 2400000,
                    status = SessionStatus.COMPLETED
                ),
                GamingSession(
                    id = "gs_2",
                    branchId = "br_kampala",
                    employeeId = "usr_gaming",
                    consoleType = ConsoleType.PS3,
                    consoleNumber = 2,
                    gameCategory = GameCategory.PES_FIFA,
                    gameTitle = "PES 2024",
                    durationMinutes = 15,
                    rateUgx = 1500,
                    totalAmountUgx = 1500,
                    isPaid = true,
                    customerName = "Grace",
                    startTimeMillis = System.currentTimeMillis() - 1800000,
                    endTimeMillis = System.currentTimeMillis() - 900000,
                    status = SessionStatus.COMPLETED
                )
            )
            gamingSessions.forEach { db.gamingDao().insertSession(it) }

            // Seed Network Devices
            val devices = listOf(
                NetworkDevice(
                    id = "dev_1",
                    name = "Kampala Main Gateway",
                    model = "CCR2004-16G-2S+",
                    vendor = "MIKROTIK",
                    serialNumber = "HE3049B7A21",
                    macAddress = "48:8F:5A:12:34:56",
                    ipAddress = "192.168.88.1",
                    routerOsVersion = "RouterOS v7.14.3",
                    siteLocation = "Kampala Main Hub Rack A",
                    branchId = "br_kampala",
                    status = "ONLINE",
                    adminPort = 8728,
                    configBackupDate = "2026-10-13",
                    activeHotspotUsers = 34
                ),
                NetworkDevice(
                    id = "dev_2",
                    name = "Entebbe Hub Router",
                    model = "RB5009UG+S+IN",
                    vendor = "MIKROTIK",
                    serialNumber = "EBB890123AB",
                    macAddress = "DC:2C:6E:98:76:54",
                    ipAddress = "192.168.89.1",
                    routerOsVersion = "RouterOS v7.13.2",
                    siteLocation = "Entebbe Server Room",
                    branchId = "br_entebbe",
                    status = "ONLINE",
                    adminPort = 8728,
                    configBackupDate = "2026-10-10",
                    activeHotspotUsers = 19
                ),
                NetworkDevice(
                    id = "dev_3",
                    name = "Mukono Omada Controller",
                    model = "ER7206 / OC200",
                    vendor = "TP_LINK_OMADA",
                    serialNumber = "TPOM778899A",
                    macAddress = "B0:BE:76:33:44:55",
                    ipAddress = "192.168.0.1",
                    routerOsVersion = "Omada Controller v5.13.30",
                    siteLocation = "Mukono Branch Office",
                    branchId = "br_mukono",
                    status = "ONLINE",
                    adminPort = 8043,
                    configBackupDate = "2026-10-11",
                    activeHotspotUsers = 14
                )
            )
            db.deviceDao().insertDevices(devices)

            // Seed Technician Jobs
            val techJobs = listOf(
                TechnicianJob(
                    id = "job_1",
                    ticketNumber = "TKT-2026-089",
                    title = "New 5G Outdoor AP Installation",
                    customerName = "Kigozi Plaza Complex",
                    customerPhone = "0772556677",
                    siteAddress = "Kigozi Plaza, Floor 2, Kampala",
                    routerModel = "MikroTik hAP ax3 + cAP ax",
                    deviceType = "Access Point & Gateway",
                    jobType = JobType.INSTALLATION,
                    status = JobStatus.IN_PROGRESS,
                    technicianId = "usr_tech",
                    technicianName = "John Semwogerere",
                    branchId = "br_kampala",
                    serviceChargeUgx = 150000,
                    materialsUsed = "Cat6 Cable 60m, RJ45 8pcs, Mounting clips",
                    notes = "Mounted outdoor AP on mast; configuring VLANs."
                ),
                TechnicianJob(
                    id = "job_2",
                    ticketNumber = "TKT-2026-090",
                    title = "Bandwidth Fluctuation & DHCP Lease Expiry",
                    customerName = "Prime Lounge & Sports Bar",
                    customerPhone = "0701889900",
                    siteAddress = "Plot 14 Ggaba Road",
                    routerModel = "TP-Link Omada EAP650",
                    deviceType = "Omada AP",
                    jobType = JobType.TROUBLESHOOTING,
                    status = JobStatus.ASSIGNED,
                    technicianId = "usr_tech",
                    technicianName = "John Semwogerere",
                    branchId = "br_kampala",
                    serviceChargeUgx = 50000,
                    notes = "Clients report disconnection every 30 minutes."
                )
            )
            techJobs.forEach { db.technicianDao().insertJob(it) }

            // Seed System Settings
            val settings = listOf(
                SystemSetting("commission_8am", "25", "Commission rate (%) for shift starting at 08:00 AM or earlier"),
                SystemSetting("commission_9am", "20", "Commission rate (%) for shift starting at 09:00 AM"),
                SystemSetting("commission_10am", "15", "Commission rate (%) for shift starting at 10:00 AM"),
                SystemSetting("commission_11am", "10", "Commission rate (%) for shift starting at 11:00 AM or later"),
                SystemSetting("commission_12pm", "0", "Commission rate (%) for shift starting at 12:00 PM or later (if enabled)"),
                SystemSetting("uniform_deduction", "5", "Commission deduction percentage points for missing uniform"),
                SystemSetting("sms_provider", "UNCONFIGURED", "Configured SMS Provider: AFRICAS_TALKING / TWILIO / UNCONFIGURED"),
                SystemSetting("sms_sender_id", "WANGI_TECH", "Alphanumeric Sender ID for automated transactional SMS"),
                SystemSetting("ps3_unit_rate", "500", "PS3 PES/FIFA rate (UGX) per 5-minute unit"),
                SystemSetting("ps4_unit_rate", "1000", "PS4 PES/FIFA rate (UGX) per 5-minute unit")
            )
            db.auditDao().insertSettings(settings)

            // Seed sample pending report for demonstration
            val sampleReport = DailyReport(
                id = "rep_sample_01",
                referenceNumber = "REP-2026-1014-01",
                employeeId = "usr_hotspot",
                employeeName = "Brian Mukasa",
                department = Department.HOTSPOT,
                branchId = "br_kampala",
                date = "2026-10-14",
                shiftStartTime = "08:00 AM",
                shiftEndTime = "05:00 PM",
                totalSalesUgx = 65000,
                paidSalesUgx = 63000,
                unpaidDebtsUgx = 2000,
                expectedCashUgx = 63000,
                actualCashReceivedUgx = 63000,
                expensesUgx = 0,
                commissionEarnedUgx = 15750, // 25% of 63000
                shortageOrSurplusUgx = 0,
                notes = "Smooth day, busy afternoon session. 1 credit customer recorded.",
                status = ReportStatus.SUBMITTED,
                submittedAt = System.currentTimeMillis() - 7200000,
                versionNumber = 1
            )
            db.reportDao().insertReport(sampleReport)

            // Seed Initial Notification for Manager & CEO
            db.notificationDao().insertNotification(
                NotificationItem(
                    id = "notif_1",
                    targetUserId = null,
                    targetRole = UserRole.MANAGER,
                    title = "Pending Daily Report Submitted",
                    message = "Brian Mukasa submitted Daily Report REP-2026-1014-01 (UGX 63,000 cash) awaiting your confirmation.",
                    type = "REPORT",
                    relatedEntityId = "rep_sample_01"
                )
            )
            db.notificationDao().insertNotification(
                NotificationItem(
                    id = "notif_2",
                    targetUserId = null,
                    targetRole = UserRole.CEO,
                    title = "System Alert: All Gateways Online",
                    message = "Kampala Main Hub (CCR2004) and Entebbe Hub (RB5009) reporting optimal health.",
                    type = "SYSTEM",
                    relatedEntityId = null
                )
            )

            // Seed Audit Event
            db.auditDao().insertAuditEvent(
                AuditEvent(
                    id = "audit_init",
                    actorId = "system",
                    actorName = "System Initializer",
                    actorRole = "SYSTEM",
                    action = "SYSTEM_INITIALIZED",
                    entityType = "DATABASE",
                    entityId = "wangi_tech_solutions.db",
                    details = "Platform initialized with secure roles, branches, initial pricing, and network profiles."
                )
            )
        }
    }
}
