package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<User>>

    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserById(userId: String): User?

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): User?

    @Query("SELECT * FROM users WHERE phone = :phone LIMIT 1")
    suspend fun getUserByPhone(phone: String): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<User>)

    @Update
    suspend fun updateUser(user: User)

    @Delete
    suspend fun deleteUser(user: User)
}

@Dao
interface BranchDao {
    @Query("SELECT * FROM branches")
    fun getAllBranches(): Flow<List<Branch>>

    @Query("SELECT * FROM branches WHERE id = :branchId")
    suspend fun getBranchById(branchId: String): Branch?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBranch(branch: Branch)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBranches(branches: List<Branch>)
}

@Dao
interface ShiftDao {
    @Query("SELECT * FROM shifts ORDER BY createdAt DESC")
    fun getAllShifts(): Flow<List<Shift>>

    @Query("SELECT * FROM shifts WHERE employeeId = :employeeId ORDER BY createdAt DESC")
    fun getShiftsByEmployee(employeeId: String): Flow<List<Shift>>

    @Query("SELECT * FROM shifts WHERE branchId = :branchId ORDER BY createdAt DESC")
    fun getShiftsByBranch(branchId: String): Flow<List<Shift>>

    @Query("SELECT * FROM shifts WHERE id = :shiftId")
    suspend fun getShiftById(shiftId: String): Shift?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShift(shift: Shift)

    @Update
    suspend fun updateShift(shift: Shift)
}

@Dao
interface VoucherDao {
    @Query("SELECT * FROM voucher_inventory")
    fun getInventory(): Flow<List<VoucherInventory>>

    @Query("SELECT * FROM voucher_inventory WHERE employeeId = :employeeId")
    fun getInventoryByEmployee(employeeId: String): Flow<List<VoucherInventory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventory(items: List<VoucherInventory>)

    @Update
    suspend fun updateInventory(item: VoucherInventory)

    @Query("SELECT * FROM voucher_sales ORDER BY saleTime DESC")
    fun getAllSales(): Flow<List<VoucherSale>>

    @Query("SELECT * FROM voucher_sales WHERE employeeId = :employeeId ORDER BY saleTime DESC")
    fun getSalesByEmployee(employeeId: String): Flow<List<VoucherSale>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: VoucherSale)
}

@Dao
interface DebtDao {
    @Query("SELECT * FROM customer_debts ORDER BY recordedDate DESC")
    fun getAllDebts(): Flow<List<CustomerDebt>>

    @Query("SELECT * FROM customer_debts WHERE isPaid = 0")
    fun getUnpaidDebts(): Flow<List<CustomerDebt>>

    @Query("SELECT * FROM customer_debts WHERE id = :debtId")
    suspend fun getDebtById(debtId: String): CustomerDebt?

    @Query("SELECT * FROM customer_debts WHERE referenceCode = :refCode")
    suspend fun getDebtByReference(refCode: String): CustomerDebt?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebt(debt: CustomerDebt)

    @Update
    suspend fun updateDebt(debt: CustomerDebt)
}

@Dao
interface GamingDao {
    @Query("SELECT * FROM gaming_sessions ORDER BY createdAt DESC")
    fun getAllSessions(): Flow<List<GamingSession>>

    @Query("SELECT * FROM gaming_sessions WHERE employeeId = :employeeId ORDER BY createdAt DESC")
    fun getSessionsByEmployee(employeeId: String): Flow<List<GamingSession>>

    @Query("SELECT * FROM gaming_sessions WHERE id = :id")
    suspend fun getSessionById(id: String): GamingSession?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: GamingSession)

    @Update
    suspend fun updateSession(session: GamingSession)
}

@Dao
interface ReportDao {
    @Query("SELECT * FROM daily_reports ORDER BY submittedAt DESC")
    fun getAllReports(): Flow<List<DailyReport>>

    @Query("SELECT * FROM daily_reports WHERE employeeId = :employeeId ORDER BY submittedAt DESC")
    fun getReportsByEmployee(employeeId: String): Flow<List<DailyReport>>

    @Query("SELECT * FROM daily_reports WHERE status = 'SUBMITTED' OR status = 'UNDER_REVIEW'")
    fun getPendingReports(): Flow<List<DailyReport>>

    @Query("SELECT * FROM daily_reports WHERE id = :reportId")
    suspend fun getReportById(reportId: String): DailyReport?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: DailyReport)

    @Update
    suspend fun updateReport(report: DailyReport)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReportHistory(history: ReportHistory)

    @Query("SELECT * FROM report_history WHERE reportId = :reportId ORDER BY timestamp DESC")
    fun getHistoryForReport(reportId: String): Flow<List<ReportHistory>>
}

@Dao
interface TechnicianDao {
    @Query("SELECT * FROM technician_jobs ORDER BY createdAt DESC")
    fun getAllJobs(): Flow<List<TechnicianJob>>

    @Query("SELECT * FROM technician_jobs WHERE technicianId = :techId ORDER BY createdAt DESC")
    fun getJobsByTechnician(techId: String): Flow<List<TechnicianJob>>

    @Query("SELECT * FROM technician_jobs WHERE id = :id")
    suspend fun getJobById(id: String): TechnicianJob?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJob(job: TechnicianJob)

    @Update
    suspend fun updateJob(job: TechnicianJob)
}

@Dao
interface DeviceDao {
    @Query("SELECT * FROM network_devices")
    fun getAllDevices(): Flow<List<NetworkDevice>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: NetworkDevice)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevices(devices: List<NetworkDevice>)

    @Update
    suspend fun updateDevice(device: NetworkDevice)

    @Delete
    suspend fun deleteDevice(device: NetworkDevice)
}

@Dao
interface BillingDao {
    @Query("SELECT * FROM billing_packages")
    fun getAllPackages(): Flow<List<BillingPackage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPackages(packages: List<BillingPackage>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPackage(pkg: BillingPackage)

    @Update
    suspend fun updatePackage(pkg: BillingPackage)

    @Delete
    suspend fun deletePackage(pkg: BillingPackage)

    @Query("SELECT * FROM wifi_vouchers ORDER BY generatedAt DESC")
    fun getAllWifiVouchers(): Flow<List<WifiVoucher>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWifiVouchers(vouchers: List<WifiVoucher>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWifiVoucher(voucher: WifiVoucher)

    @Update
    suspend fun updateWifiVoucher(voucher: WifiVoucher)

    @Delete
    suspend fun deleteWifiVoucher(voucher: WifiVoucher)

    @Query("SELECT * FROM billing_subscriptions ORDER BY startedAt DESC")
    fun getAllSubscriptions(): Flow<List<BillingSubscription>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscription(sub: BillingSubscription)

    @Update
    suspend fun updateSubscription(sub: BillingSubscription)

    @Delete
    suspend fun deleteSubscription(sub: BillingSubscription)

    @Query("SELECT * FROM payment_transactions ORDER BY createdAt DESC")
    fun getAllTransactions(): Flow<List<PaymentTransaction>>

    @Query("SELECT * FROM payment_transactions WHERE orderRef = :orderRef LIMIT 1")
    suspend fun getTransactionByOrderRef(orderRef: String): PaymentTransaction?

    @Query("SELECT * FROM payment_transactions WHERE providerTxId = :providerTxId LIMIT 1")
    suspend fun getTransactionByProviderTxId(providerTxId: String): PaymentTransaction?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(tx: PaymentTransaction)

    @Update
    suspend fun updateTransaction(tx: PaymentTransaction)
}

@Dao
interface FinanceDao {
    @Query("SELECT * FROM business_expenses ORDER BY createdAt DESC")
    fun getAllExpenses(): Flow<List<BusinessExpense>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: BusinessExpense)

    @Query("SELECT * FROM salary_records")
    fun getAllSalaryRecords(): Flow<List<SalaryRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSalaryRecords(records: List<SalaryRecord>)

    @Update
    suspend fun updateSalaryRecord(record: SalaryRecord)
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications ORDER BY createdAt DESC")
    fun getAllNotifications(): Flow<List<NotificationItem>>

    @Query("SELECT * FROM notifications WHERE targetUserId = :userId OR targetRole = :role ORDER BY createdAt DESC")
    fun getNotificationsForUser(userId: String, role: UserRole): Flow<List<NotificationItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationItem)

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: String)

    @Query("SELECT * FROM sms_logs ORDER BY sentAt DESC")
    fun getAllSmsLogs(): Flow<List<SmsLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSmsLog(log: SmsLog)
}

@Dao
interface AuditDao {
    @Query("SELECT * FROM audit_events ORDER BY timestamp DESC")
    fun getAllAuditEvents(): Flow<List<AuditEvent>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditEvent(event: AuditEvent)

    @Query("SELECT * FROM system_settings")
    fun getAllSettings(): Flow<List<SystemSetting>>

    @Query("SELECT * FROM system_settings WHERE key = :key LIMIT 1")
    suspend fun getSetting(key: String): SystemSetting?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setSetting(setting: SystemSetting)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: List<SystemSetting>)
}
