package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class UserRole {
    CEO,
    MANAGER,
    HOTSPOT_EMPLOYEE,
    GAMING_EMPLOYEE,
    TECHNICIAN,
    BILLING_ADMIN
}

enum class Department {
    HOTSPOT,
    GAMING,
    TECHNICAL,
    BILLING,
    ALL,
    NONE
}

@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: String,
    val username: String,
    val fullName: String,
    val phone: String,
    val email: String,
    val role: UserRole,
    val department: Department,
    val branchId: String,
    val passwordHash: String,
    val status: String = "ACTIVE", // ACTIVE, SUSPENDED
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "branches")
data class Branch(
    @PrimaryKey val id: String,
    val name: String,
    val code: String,
    val location: String,
    val managerId: String,
    val phone: String,
    val activeRoutersCount: Int = 3,
    val activeGamingConsolesCount: Int = 4,
    val createdAt: Long = System.currentTimeMillis()
)

enum class ShiftStatus {
    REQUESTED,
    ACTIVE,
    COMPLETED,
    REJECTED
}

@Entity(tableName = "shifts")
data class Shift(
    @PrimaryKey val id: String,
    val employeeId: String,
    val employeeName: String,
    val department: Department,
    val branchId: String,
    val date: String, // e.g. "2026-10-14"
    val requestedStartTime: String, // e.g. "08:00 AM"
    val actualClockInTime: String? = null,
    val clockOutTime: String? = null,
    val uniformCompliant: Boolean = true,
    val status: ShiftStatus = ShiftStatus.REQUESTED,
    val commissionRatePercent: Double = 25.0, // Calculated based on arrival time & uniform
    val uniformDeductionPercent: Double = 0.0,
    val reviewerId: String? = null,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "voucher_inventory")
data class VoucherInventory(
    @PrimaryKey val id: String,
    val branchId: String,
    val employeeId: String,
    val denominationUgx: Long,
    val totalAssigned: Int,
    val soldCount: Int = 0,
    val returnedCount: Int = 0,
    val missingCount: Int = 0,
    val date: String
)

@Entity(tableName = "voucher_sales")
data class VoucherSale(
    @PrimaryKey val id: String,
    val branchId: String,
    val employeeId: String,
    val employeeName: String,
    val customerName: String,
    val customerPhone: String,
    val denominationUgx: Long,
    val quantity: Int,
    val totalAmountUgx: Long,
    val isPaid: Boolean,
    val debtReference: String? = null,
    val saleTime: Long = System.currentTimeMillis()
)

@Entity(tableName = "customer_debts")
data class CustomerDebt(
    @PrimaryKey val id: String,
    val referenceCode: String,
    val branchId: String,
    val customerName: String,
    val customerPhone: String,
    val department: Department,
    val amountUgx: Long,
    val amountPaidUgx: Long = 0,
    val isPaid: Boolean = false,
    val recordedByEmployeeId: String,
    val recordedDate: String,
    val settledDate: String? = null,
    val notes: String = ""
)

enum class ConsoleType {
    PS3,
    PS4
}

enum class GameCategory {
    PES_FIFA,
    OTHER_GAME
}

enum class SessionStatus {
    ACTIVE,
    COMPLETED,
    CANCELLED_REVERSED
}

@Entity(tableName = "gaming_sessions")
data class GamingSession(
    @PrimaryKey val id: String,
    val branchId: String,
    val employeeId: String,
    val consoleType: ConsoleType,
    val consoleNumber: Int, // e.g. Station 1, 2, 3, 4
    val gameCategory: GameCategory,
    val gameTitle: String, // e.g. "PES 2024", "FIFA 24", "Mortal Kombat"
    val durationMinutes: Int,
    val rateUgx: Long,
    val totalAmountUgx: Long,
    val isPaid: Boolean,
    val customerName: String,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val status: SessionStatus = SessionStatus.ACTIVE,
    val reversalReason: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

enum class ReportStatus {
    DRAFT,
    SUBMITTED,
    UNDER_REVIEW,
    CONFIRMED,
    RETURNED_FOR_CORRECTION
}

@Entity(tableName = "daily_reports")
data class DailyReport(
    @PrimaryKey val id: String,
    val referenceNumber: String,
    val employeeId: String,
    val employeeName: String,
    val department: Department,
    val branchId: String,
    val date: String,
    val shiftStartTime: String,
    val shiftEndTime: String,
    val totalSalesUgx: Long,
    val paidSalesUgx: Long,
    val unpaidDebtsUgx: Long,
    val expectedCashUgx: Long,
    val actualCashReceivedUgx: Long,
    val expensesUgx: Long,
    val commissionEarnedUgx: Long,
    val shortageOrSurplusUgx: Long, // actualCash - expectedCash (after expenses)
    val notes: String,
    val status: ReportStatus = ReportStatus.DRAFT,
    val reviewerId: String? = null,
    val reviewerName: String? = null,
    val reviewReason: String? = null,
    val reviewerNotes: String? = null,
    val submittedAt: Long? = null,
    val reviewedAt: Long? = null,
    val versionNumber: Int = 1
)

@Entity(tableName = "report_history")
data class ReportHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val reportId: String,
    val versionNumber: Int,
    val figuresSummary: String,
    val actionTaken: String,
    val actionBy: String,
    val notes: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class JobStatus {
    NEW,
    ASSIGNED,
    IN_PROGRESS,
    WAITING,
    COMPLETED,
    CANCELLED
}

enum class JobType {
    INSTALLATION,
    TROUBLESHOOTING,
    MAINTENANCE,
    EQUIPMENT_REPAIR
}

@Entity(tableName = "technician_jobs")
data class TechnicianJob(
    @PrimaryKey val id: String,
    val ticketNumber: String,
    val title: String,
    val customerName: String,
    val customerPhone: String,
    val siteAddress: String,
    val routerModel: String,
    val deviceType: String,
    val jobType: JobType,
    val status: JobStatus = JobStatus.NEW,
    val technicianId: String? = null,
    val technicianName: String? = null,
    val branchId: String,
    val serviceChargeUgx: Long = 0,
    val materialsUsed: String = "",
    val notes: String = "",
    val customerSignOff: Boolean = false,
    val completionProofUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)

@Entity(tableName = "network_devices")
data class NetworkDevice(
    @PrimaryKey val id: String,
    val name: String,
    val model: String,
    val vendor: String, // "MIKROTIK", "TP_LINK_OMADA", "OTHER"
    val serialNumber: String,
    val macAddress: String,
    val ipAddress: String,
    val routerOsVersion: String, // e.g. "RouterOS v7.14.3" or "Omada Controller v5.13"
    val siteLocation: String,
    val branchId: String,
    val status: String = "ONLINE", // ONLINE, OFFLINE, MAINTENANCE
    val adminPort: Int = 8728,
    val configBackupDate: String = "2026-10-12",
    val activeHotspotUsers: Int = 28
)

@Entity(tableName = "billing_packages")
data class BillingPackage(
    @PrimaryKey val id: String,
    val name: String,
    val validityPeriod: String, // e.g. "1 Day", "3 Days", "1 Week", "1 Month"
    val priceUgx: Long,
    val speedLimitMbps: Int, // e.g. 5, 10, 20
    val quotaLimitMb: Long = 0, // 0 for unlimited
    val isActive: Boolean = true,
    val isHotspot: Boolean = true,
    val isPppoe: Boolean = false,
    val description: String = ""
)

@Entity(tableName = "billing_subscriptions")
data class BillingSubscription(
    @PrimaryKey val id: String,
    val customerName: String,
    val customerPhone: String,
    val packageName: String,
    val packageId: String,
    val voucherCode: String,
    val routerId: String,
    val priceUgx: Long,
    val startedAt: Long,
    val expiresAt: Long,
    val status: String = "ACTIVE", // ACTIVE, EXPIRED, REVOKED
    val paymentTxRef: String,
    val ipAddress: String = "192.168.88.50",
    val macAddress: String = "A4:B2:39:10:4F:92",
    val bytesDownloadedMb: Long = 145,
    val bytesUploadedMb: Long = 32,
    val isBlocked: Boolean = false
)

@Entity(tableName = "wifi_vouchers")
data class WifiVoucher(
    @PrimaryKey val id: String,
    val batchCode: String,
    val code: String,
    val pin: String,
    val packageName: String,
    val packageId: String,
    val priceUgx: Long,
    val validityPeriod: String,
    val speedLimitMbps: Int,
    val status: String = "AVAILABLE", // AVAILABLE, ACTIVE, EXPIRED
    val usedByPhone: String? = null,
    val usedByName: String? = null,
    val activatedAt: Long? = null,
    val expiresAt: Long? = null,
    val generatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "payment_transactions")
data class PaymentTransaction(
    @PrimaryKey val id: String,
    val orderRef: String,
    val providerTxId: String,
    val customerPhone: String,
    val customerName: String,
    val packageName: String,
    val amountUgx: Long,
    val provider: String, // "AIRTEL_MONEY", "MTN_MOMO", "CASH"
    val status: String, // "PENDING", "VERIFIED", "FAILED", "DUPLICATE_PREVENTED"
    val callbackProcessedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "business_expenses")
data class BusinessExpense(
    @PrimaryKey val id: String,
    val branchId: String,
    val department: Department,
    val category: String, // "UTILITIES", "RENT", "EQUIPMENT", "STATIONERY", "TRANSPORT", "MAINTENANCE"
    val description: String,
    val amountUgx: Long,
    val recordedBy: String,
    val date: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "salary_records")
data class SalaryRecord(
    @PrimaryKey val id: String,
    val employeeId: String,
    val employeeName: String,
    val branchId: String,
    val department: Department,
    val baseSalaryUgx: Long,
    val totalCommissionsUgx: Long,
    val salaryAdvancesUgx: Long,
    val deductionsUgx: Long,
    val netPayableUgx: Long,
    val monthYear: String, // "October 2026"
    val status: String = "PENDING" // PENDING, APPROVED, PAID
)

@Entity(tableName = "notifications")
data class NotificationItem(
    @PrimaryKey val id: String,
    val targetUserId: String?,
    val targetRole: UserRole?,
    val title: String,
    val message: String,
    val type: String, // "SHIFT", "REPORT", "CASH_DISCREPANCY", "TECH_JOB", "BILLING", "SYSTEM"
    val relatedEntityId: String? = null,
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "sms_logs")
data class SmsLog(
    @PrimaryKey val id: String,
    val recipientPhone: String,
    val message: String,
    val status: String, // "DELIVERED", "PENDING", "FAILED", "NO_PROVIDER"
    val providerName: String, // "AFRICAS_TALKING", "TWILIO", "UNCONFIGURED"
    val errorMessage: String? = null,
    val reference: String,
    val sentAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "audit_events")
data class AuditEvent(
    @PrimaryKey val id: String,
    val actorId: String,
    val actorName: String,
    val actorRole: String,
    val action: String,
    val entityType: String,
    val entityId: String,
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "system_settings")
data class SystemSetting(
    @PrimaryKey val key: String,
    val value: String,
    val description: String,
    val updatedAt: Long = System.currentTimeMillis()
)
