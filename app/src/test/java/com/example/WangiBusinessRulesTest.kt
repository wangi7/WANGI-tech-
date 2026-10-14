package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.repository.BusinessRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class WangiBusinessRulesTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: BusinessRepository

    private val ceoUser = User(
        id = "usr_ceo",
        username = "ceo",
        fullName = "Mudhaffar Wangi Ssebiraalo",
        phone = "+256755927896",
        email = "ceo@wangitech.com",
        role = UserRole.CEO,
        department = Department.ALL,
        branchId = "br_kampala",
        passwordHash = "wangi@2026"
    )

    private val managerUser = User(
        id = "usr_mgr",
        username = "manager",
        fullName = "Sarah Nanteza",
        phone = "+256701234567",
        email = "manager@wangitech.com",
        role = UserRole.MANAGER,
        department = Department.ALL,
        branchId = "br_kampala",
        passwordHash = "manager123"
    )

    private val hotspotEmployee = User(
        id = "usr_hotspot",
        username = "hotspot_emp",
        fullName = "Brian Mukasa",
        phone = "+256782345678",
        email = "brian@wangitech.com",
        role = UserRole.HOTSPOT_EMPLOYEE,
        department = Department.HOTSPOT,
        branchId = "br_kampala",
        passwordHash = "hotspot123"
    )

    private val gamingEmployee = User(
        id = "usr_gaming",
        username = "gaming_emp",
        fullName = "David Kato",
        phone = "+256773456789",
        email = "david@wangitech.com",
        role = UserRole.GAMING_EMPLOYEE,
        department = Department.GAMING,
        branchId = "br_kampala",
        passwordHash = "gaming123"
    )

    private val technicianUser = User(
        id = "usr_tech",
        username = "tech_john",
        fullName = "John Semwogerere",
        phone = "+256754567890",
        email = "john.tech@wangitech.com",
        role = UserRole.TECHNICIAN,
        department = Department.TECHNICAL,
        branchId = "br_kampala",
        passwordHash = "tech123"
    )

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = BusinessRepository(db)

        runTest {
            db.userDao().insertUsers(listOf(ceoUser, managerUser, hotspotEmployee, gamingEmployee, technicianUser))
        }
    }

    @After
    fun tearDown() {
        db.close()
    }

    // Rule 1: A Hotspot employee cannot read or modify Gaming records.
    @Test
    fun testRule1_hotspotEmployeeCannotAccessGaming() = runTest {
        try {
            repository.startGamingSession(
                employee = hotspotEmployee,
                consoleType = ConsoleType.PS4,
                consoleNumber = 1,
                gameCategory = GameCategory.PES_FIFA,
                gameTitle = "FIFA 24",
                durationMinutes = 15,
                isPaid = true,
                customerName = "Test"
            )
            fail("Expected SecurityException when Hotspot employee attempts to access Gaming records")
        } catch (e: SecurityException) {
            assertTrue(e.message?.contains("ACCESS DENIED") == true)
        }
    }

    // Rule 2: A Gaming employee cannot read or modify Hotspot records.
    @Test
    fun testRule2_gamingEmployeeCannotAccessHotspot() = runTest {
        try {
            repository.recordVoucherSale(
                employee = gamingEmployee,
                customerName = "Customer A",
                customerPhone = "0700112233",
                denominationUgx = 1000,
                quantity = 1,
                isPaid = true
            )
            fail("Expected SecurityException when Gaming employee attempts to access Hotspot records")
        } catch (e: SecurityException) {
            assertTrue(e.message?.contains("ACCESS DENIED") == true)
        }
    }

    // Rule 3: An employee cannot confirm their own report.
    @Test
    fun testRule3_employeeCannotConfirmOwnReport() = runTest {
        val report = repository.submitDailyReport(
            employee = hotspotEmployee,
            shiftStartTime = "08:00 AM",
            shiftEndTime = "05:00 PM",
            totalSalesUgx = 50000,
            paidSalesUgx = 50000,
            unpaidDebtsUgx = 0,
            expectedCashUgx = 50000,
            actualCashReceivedUgx = 50000,
            expensesUgx = 0,
            commissionEarnedUgx = 12500,
            notes = "Test daily shift"
        )

        try {
            repository.confirmDailyReport(report.id, hotspotEmployee, "Self approval attempt")
            fail("Expected SecurityException when employee tries to confirm their own report")
        } catch (e: SecurityException) {
            assertTrue(e.message?.contains("cannot confirm their own") == true)
        }
    }

    // Rule 4: An unconfirmed report is not finalized.
    @Test
    fun testRule4_unconfirmedReportIsNotFinalized() = runTest {
        val report = repository.submitDailyReport(
            employee = hotspotEmployee,
            shiftStartTime = "08:00 AM",
            shiftEndTime = "05:00 PM",
            totalSalesUgx = 30000,
            paidSalesUgx = 30000,
            unpaidDebtsUgx = 0,
            expectedCashUgx = 30000,
            actualCashReceivedUgx = 30000,
            expensesUgx = 0,
            commissionEarnedUgx = 7500,
            notes = "Draft shift"
        )

        val retrieved = db.reportDao().getReportById(report.id)
        assertNotNull(retrieved)
        assertEquals(ReportStatus.SUBMITTED, retrieved?.status)
        assertNull(retrieved?.reviewedAt)
        assertNull(retrieved?.reviewerId)
    }

    // Rule 5: Only authorized managers or the CEO can confirm reports.
    @Test
    fun testRule5_onlyManagerOrCeoCanConfirmReports() = runTest {
        val report = repository.submitDailyReport(
            employee = hotspotEmployee,
            shiftStartTime = "08:00 AM",
            shiftEndTime = "05:00 PM",
            totalSalesUgx = 40000,
            paidSalesUgx = 40000,
            unpaidDebtsUgx = 0,
            expectedCashUgx = 40000,
            actualCashReceivedUgx = 40000,
            expensesUgx = 0,
            commissionEarnedUgx = 10000,
            notes = "Shift notes"
        )

        // Technician cannot confirm
        try {
            repository.confirmDailyReport(report.id, technicianUser, "Tech unauthorized confirm")
            fail("Technician should not be authorized to confirm daily reports")
        } catch (e: SecurityException) {
            assertTrue(e.message?.contains("Only CEO or Branch Managers") == true)
        }

        // Manager CAN confirm
        val confirmed = repository.confirmDailyReport(report.id, managerUser, "Cash verified by Sarah")
        assertEquals(ReportStatus.CONFIRMED, confirmed.status)
        assertEquals(managerUser.id, confirmed.reviewerId)
    }

    // Rule 6: Report confirmation generates the required notification workflow.
    @Test
    fun testRule6_reportConfirmationGeneratesNotifications() = runTest {
        val report = repository.submitDailyReport(
            employee = hotspotEmployee,
            shiftStartTime = "08:00 AM",
            shiftEndTime = "05:00 PM",
            totalSalesUgx = 25000,
            paidSalesUgx = 25000,
            unpaidDebtsUgx = 0,
            expectedCashUgx = 25000,
            actualCashReceivedUgx = 25000,
            expensesUgx = 0,
            commissionEarnedUgx = 6250,
            notes = "Completed shift"
        )

        repository.confirmDailyReport(report.id, ceoUser, "CEO Confirmed")

        val employeeNotifs = repository.getNotificationsForUser(hotspotEmployee.id, hotspotEmployee.role).first()
        assertTrue(employeeNotifs.any { it.title.contains("CONFIRMED") && it.relatedEntityId == report.id })
    }

    // Rule 7: SMS failures are recorded and do not falsely appear delivered.
    @Test
    fun testRule7_smsStatusAccuratelyRecorded() = runTest {
        val report = repository.submitDailyReport(
            employee = hotspotEmployee,
            shiftStartTime = "08:00 AM",
            shiftEndTime = "05:00 PM",
            totalSalesUgx = 15000,
            paidSalesUgx = 15000,
            unpaidDebtsUgx = 0,
            expectedCashUgx = 15000,
            actualCashReceivedUgx = 15000,
            expensesUgx = 0,
            commissionEarnedUgx = 3750,
            notes = "Check SMS log"
        )

        repository.confirmDailyReport(report.id, managerUser, "Confirmed")

        val smsLogs = repository.allSmsLogs.first()
        val log = smsLogs.find { it.reference == report.referenceNumber }
        assertNotNull(log)
        // Since SMS provider is unconfigured in base settings, status must be NO_PROVIDER, never falsely DELIVERED!
        assertEquals("NO_PROVIDER", log?.status)
        assertNotNull(log?.errorMessage)
    }

    // Rule 8: Duplicate payment callbacks do not duplicate billing actions.
    @Test
    fun testRule8_duplicatePaymentCallbacksPrevented() = runTest {
        val orderRef = "ORD-TEST-1001"
        val providerTxId = "TXN-AIRTEL-987654"

        val pkg = BillingPackage("pkg_test", "1 Day Pass", "24 Hours", 1000, 10, 0, true)

        val (firstSuccess, voucher1) = repository.processPaymentCallback(
            orderRef = orderRef,
            providerTxId = providerTxId,
            customerPhone = "0755927896",
            customerName = "Mudhaffar",
            packageName = pkg.name,
            packageId = pkg.id,
            amountUgx = pkg.priceUgx,
            provider = "AIRTEL_MONEY"
        )

        assertTrue(firstSuccess)
        assertTrue(voucher1.startsWith("WG-"))

        // Duplicate callback simulation
        val (secondSuccess, result2) = repository.processPaymentCallback(
            orderRef = orderRef,
            providerTxId = providerTxId,
            customerPhone = "0755927896",
            customerName = "Mudhaffar",
            packageName = pkg.name,
            packageId = pkg.id,
            amountUgx = pkg.priceUgx,
            provider = "AIRTEL_MONEY"
        )

        assertFalse(secondSuccess)
        assertEquals("DUPLICATE_CALLBACK_PREVENTED", result2)

        // Verify only 1 subscription created
        val allSubs = repository.allSubscriptions.first()
        val matching = allSubs.filter { it.paymentTxRef == providerTxId }
        assertEquals(1, matching.size)
    }

    // Rule 9: A technician cannot access CEO financial controls without permission.
    @Test
    fun testRule9_technicianCannotAccessCeoSettings() = runTest {
        try {
            repository.updateSetting("commission_8am", "30", "Changed by tech", technicianUser)
            fail("Technician should be denied access to CEO settings")
        } catch (e: SecurityException) {
            assertTrue(e.message?.contains("Only CEO") == true)
        }
    }

    // Rule 10: Unauthorized direct API requests are denied.
    @Test
    fun testRule10_unauthorizedDepartmentValidation() = runTest {
        try {
            repository.validateDepartmentAccess(technicianUser, Department.HOTSPOT)
            fail("Technician accessing Hotspot department directly should throw SecurityException")
        } catch (e: SecurityException) {
            assertTrue(e.message?.contains("ACCESS DENIED") == true)
        }

        try {
            repository.validateDepartmentAccess(hotspotEmployee, Department.GAMING)
            fail("Hotspot employee accessing Gaming department directly should throw SecurityException")
        } catch (e: SecurityException) {
            assertTrue(e.message?.contains("ACCESS DENIED") == true)
        }
    }

    // Wi-Fi Billing Tests: Batch Voucher Generation
    @Test
    fun testWifiBilling_batchVoucherGeneration() = runTest {
        val pkg = BillingPackage("pkg_test_daily", "Test Daily", "24 Hours", 1000, 10, 0, true, true, false, "Test package")
        val vouchers = repository.generateVoucherBatch(pkg, 15, "TEST-", ceoUser)

        assertEquals(15, vouchers.size)
        vouchers.forEach { v ->
            assertTrue(v.code.startsWith("TEST-"))
            assertEquals("AVAILABLE", v.status)
            assertEquals(1000L, v.priceUgx)
            assertEquals("Test Daily", v.packageName)
            assertTrue(v.pin.length == 4)
        }

        val allVouchers = repository.allWifiVouchers.first()
        assertEquals(15, allVouchers.size)
    }

    // Wi-Fi Billing Tests: Subscriber Disconnect and Time Extension
    @Test
    fun testWifiBilling_subscriberDisconnectAndExtend() = runTest {
        val now = System.currentTimeMillis()
        val sub = BillingSubscription(
            id = "sub_test_1",
            customerName = "Test Client",
            customerPhone = "0755112233",
            packageName = "Test Daily",
            packageId = "pkg_test_daily",
            voucherCode = "WG-123456",
            routerId = "dev_1",
            priceUgx = 1000,
            startedAt = now,
            expiresAt = now + 3600000,
            status = "ACTIVE",
            paymentTxRef = "TXN-TEST-1"
        )
        db.billingDao().insertSubscription(sub)

        // Extend by 24 hours
        repository.extendSubscription(sub, 24, ceoUser)
        val extendedSub = repository.allSubscriptions.first().first { it.id == "sub_test_1" }
        assertEquals(now + 3600000 + (24 * 3600 * 1000L), extendedSub.expiresAt)

        // Disconnect / kick
        repository.disconnectSubscriber(extendedSub, ceoUser)
        val kickedSub = repository.allSubscriptions.first().first { it.id == "sub_test_1" }
        assertEquals("REVOKED", kickedSub.status)
    }

    // Wi-Fi Billing Tests: Package CRUD
    @Test
    fun testWifiBilling_packageManagement() = runTest {
        val pkg = BillingPackage("pkg_custom", "Special Event", "3 Days", 3000, 20, 5000, true, true, false, "VIP Event")
        repository.createBillingPackage(pkg, ceoUser)

        var packages = repository.allBillingPackages.first()
        assertTrue(packages.any { it.id == "pkg_custom" })

        val updated = pkg.copy(priceUgx = 2500)
        repository.updateBillingPackage(updated, ceoUser)
        packages = repository.allBillingPackages.first()
        assertEquals(2500L, packages.first { it.id == "pkg_custom" }.priceUgx)

        repository.deleteBillingPackage(updated, ceoUser)
        packages = repository.allBillingPackages.first()
        assertFalse(packages.any { it.id == "pkg_custom" })
    }
}

