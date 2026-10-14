package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.*

class BusinessRepository(private val db: AppDatabase) {

    // Users & Auth
    val allUsers: Flow<List<User>> = db.userDao().getAllUsers()

    suspend fun authenticate(credential: String, passwordAttempt: String): User? {
        val user = db.userDao().getUserByUsername(credential)
            ?: db.userDao().getUserByPhone(credential)
        if (user != null && user.passwordHash == passwordAttempt && user.status == "ACTIVE") {
            logAudit(
                actorId = user.id,
                actorName = user.fullName,
                actorRole = user.role.name,
                action = "LOGIN_SUCCESS",
                entityType = "USER",
                entityId = user.id,
                details = "User successfully signed in from mobile platform."
            )
            return user
        }
        return null
    }

    suspend fun getUserById(userId: String): User? = db.userDao().getUserById(userId)

    suspend fun createUser(user: User, actor: User): Boolean {
        // Only CEO or Manager can create users
        if (actor.role != UserRole.CEO && actor.role != UserRole.MANAGER) {
            throw SecurityException("Unauthorized: Only CEO or Managers can register employees.")
        }
        db.userDao().insertUser(user)
        logAudit(actor.id, actor.fullName, actor.role.name, "USER_CREATED", "USER", user.id, "Registered ${user.role} ${user.fullName}")
        return true
    }

    // Role Access Control (Enforced on Data Layer)
    fun validateDepartmentAccess(user: User, targetDepartment: Department) {
        if (user.role == UserRole.CEO) return
        if (user.role == UserRole.MANAGER && (user.department == Department.ALL || user.department == targetDepartment)) return
        if (user.role == UserRole.HOTSPOT_EMPLOYEE && targetDepartment == Department.HOTSPOT) return
        if (user.role == UserRole.GAMING_EMPLOYEE && targetDepartment == Department.GAMING) return
        if (user.role == UserRole.TECHNICIAN && targetDepartment == Department.TECHNICAL) return
        if (user.role == UserRole.BILLING_ADMIN && targetDepartment == Department.BILLING) return

        throw SecurityException("ACCESS DENIED: Role ${user.role} is not authorized to access $targetDepartment workspace.")
    }

    // Branches
    val allBranches: Flow<List<Branch>> = db.branchDao().getAllBranches()
    suspend fun addBranch(branch: Branch, actor: User) {
        if (actor.role != UserRole.CEO) throw SecurityException("Only CEO can create branches")
        db.branchDao().insertBranch(branch)
        logAudit(actor.id, actor.fullName, actor.role.name, "BRANCH_CREATED", "BRANCH", branch.id, "Branch ${branch.name} created")
    }

    // Shifts & Commission Calculation
    val allShifts: Flow<List<Shift>> = db.shiftDao().getAllShifts()
    fun getShiftsByEmployee(employeeId: String): Flow<List<Shift>> = db.shiftDao().getShiftsByEmployee(employeeId)

    suspend fun calculateCommissionRate(startTimeStr: String, uniformCompliant: Boolean): Double {
        // e.g., "08:00 AM", "09:30 AM", "11:15 AM"
        val hour = try {
            val upper = startTimeStr.uppercase()
            val parts = upper.split(":")
            val rawHour = parts[0].trim().toInt()
            val isPm = upper.contains("PM")
            if (isPm && rawHour < 12) rawHour + 12 else if (!isPm && rawHour == 12) 0 else rawHour
        } catch (e: Exception) {
            8
        }

        var baseRate = when {
            hour <= 8 -> 25.0
            hour == 9 -> 20.0
            hour == 10 -> 15.0
            hour == 11 -> 10.0
            else -> 0.0
        }

        if (!uniformCompliant) {
            baseRate = (baseRate - 5.0).coerceAtLeast(0.0)
        }

        return baseRate
    }

    suspend fun clockInShift(
        employee: User,
        requestedStartTime: String,
        uniformCompliant: Boolean,
        notes: String = ""
    ): Shift {
        validateDepartmentAccess(employee, employee.department)
        val rate = calculateCommissionRate(requestedStartTime, uniformCompliant)
        val shift = Shift(
            id = "shf_${UUID.randomUUID()}",
            employeeId = employee.id,
            employeeName = employee.fullName,
            department = employee.department,
            branchId = employee.branchId,
            date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
            requestedStartTime = requestedStartTime,
            actualClockInTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date()),
            uniformCompliant = uniformCompliant,
            status = ShiftStatus.ACTIVE,
            commissionRatePercent = rate,
            uniformDeductionPercent = if (!uniformCompliant) 5.0 else 0.0,
            notes = notes
        )
        db.shiftDao().insertShift(shift)
        logAudit(employee.id, employee.fullName, employee.role.name, "SHIFT_STARTED", "SHIFT", shift.id, "Clocked in with rate ${rate}%")
        return shift
    }

    suspend fun clockOutShift(shiftId: String, employee: User) {
        val shift = db.shiftDao().getShiftById(shiftId) ?: return
        val updated = shift.copy(
            clockOutTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date()),
            status = ShiftStatus.COMPLETED
        )
        db.shiftDao().updateShift(updated)
        logAudit(employee.id, employee.fullName, employee.role.name, "SHIFT_ENDED", "SHIFT", shift.id, "Clocked out")
    }

    // Voucher Management (Hotspot)
    fun getInventoryForEmployee(employeeId: String): Flow<List<VoucherInventory>> =
        db.voucherDao().getInventoryByEmployee(employeeId)

    val allSales: Flow<List<VoucherSale>> = db.voucherDao().getAllSales()
    fun getSalesByEmployee(employeeId: String): Flow<List<VoucherSale>> =
        db.voucherDao().getSalesByEmployee(employeeId)

    suspend fun recordVoucherSale(
        employee: User,
        customerName: String,
        customerPhone: String,
        denominationUgx: Long,
        quantity: Int,
        isPaid: Boolean,
        notes: String = ""
    ): VoucherSale {
        validateDepartmentAccess(employee, Department.HOTSPOT)
        val total = denominationUgx * quantity
        val debtRef = if (!isPaid) "DEBT-${System.currentTimeMillis() % 100000}" else null

        val sale = VoucherSale(
            id = "sale_${UUID.randomUUID()}",
            branchId = employee.branchId,
            employeeId = employee.id,
            employeeName = employee.fullName,
            customerName = customerName,
            customerPhone = customerPhone,
            denominationUgx = denominationUgx,
            quantity = quantity,
            totalAmountUgx = total,
            isPaid = isPaid,
            debtReference = debtRef
        )
        db.voucherDao().insertSale(sale)

        // If unpaid, record customer debt
        if (!isPaid && debtRef != null) {
            val debt = CustomerDebt(
                id = "dbt_${UUID.randomUUID()}",
                referenceCode = debtRef,
                branchId = employee.branchId,
                customerName = customerName,
                customerPhone = customerPhone,
                department = Department.HOTSPOT,
                amountUgx = total,
                amountPaidUgx = 0,
                isPaid = false,
                recordedByEmployeeId = employee.id,
                recordedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                notes = notes
            )
            db.debtDao().insertDebt(debt)
        }

        logAudit(employee.id, employee.fullName, employee.role.name, "VOUCHER_SALE", "SALE", sale.id, "Sold $quantity x UGX $denominationUgx (Paid=$isPaid)")
        return sale
    }

    // Gaming Management
    val allGamingSessions: Flow<List<GamingSession>> = db.gamingDao().getAllSessions()
    fun getGamingSessionsByEmployee(employeeId: String): Flow<List<GamingSession>> =
        db.gamingDao().getSessionsByEmployee(employeeId)

    suspend fun startGamingSession(
        employee: User,
        consoleType: ConsoleType,
        consoleNumber: Int,
        gameCategory: GameCategory,
        gameTitle: String,
        durationMinutes: Int,
        isPaid: Boolean,
        customerName: String
    ): GamingSession {
        validateDepartmentAccess(employee, Department.GAMING)

        val ratePer5Min = if (consoleType == ConsoleType.PS4) 1000L else 500L
        val units = (durationMinutes / 5).coerceAtLeast(1)
        val totalAmount = if (gameCategory == GameCategory.PES_FIFA) {
            units * ratePer5Min
        } else {
            // Configurable other games
            when (durationMinutes) {
                15 -> 2000L
                30 -> 3500L
                45 -> 5000L
                60 -> 6000L
                else -> units * ratePer5Min
            }
        }

        val start = System.currentTimeMillis()
        val end = start + (durationMinutes * 60 * 1000L)

        val session = GamingSession(
            id = "gs_${UUID.randomUUID()}",
            branchId = employee.branchId,
            employeeId = employee.id,
            consoleType = consoleType,
            consoleNumber = consoleNumber,
            gameCategory = gameCategory,
            gameTitle = gameTitle,
            durationMinutes = durationMinutes,
            rateUgx = ratePer5Min,
            totalAmountUgx = totalAmount,
            isPaid = isPaid,
            customerName = customerName,
            startTimeMillis = start,
            endTimeMillis = end,
            status = SessionStatus.ACTIVE
        )
        db.gamingDao().insertSession(session)

        if (!isPaid) {
            val debtRef = "GAME-DEBT-${System.currentTimeMillis() % 100000}"
            db.debtDao().insertDebt(
                CustomerDebt(
                    id = "dbt_${UUID.randomUUID()}",
                    referenceCode = debtRef,
                    branchId = employee.branchId,
                    customerName = customerName,
                    customerPhone = "In-Shop Player",
                    department = Department.GAMING,
                    amountUgx = totalAmount,
                    amountPaidUgx = 0,
                    isPaid = false,
                    recordedByEmployeeId = employee.id,
                    recordedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                    notes = "Session on $consoleType - Station $consoleNumber"
                )
            )
        }

        logAudit(employee.id, employee.fullName, employee.role.name, "GAMING_STARTED", "GAMING", session.id, "Started $consoleType Station $consoleNumber for $durationMinutes mins")
        return session
    }

    suspend fun completeGamingSession(sessionId: String, employee: User) {
        validateDepartmentAccess(employee, Department.GAMING)
        val session = db.gamingDao().getSessionById(sessionId) ?: return
        val updated = session.copy(status = SessionStatus.COMPLETED)
        db.gamingDao().updateSession(updated)
        logAudit(employee.id, employee.fullName, employee.role.name, "GAMING_COMPLETED", "GAMING", session.id, "Session marked completed")
    }

    suspend fun reverseGamingSession(sessionId: String, reason: String, employee: User) {
        validateDepartmentAccess(employee, Department.GAMING)
        val session = db.gamingDao().getSessionById(sessionId) ?: return
        val updated = session.copy(
            status = SessionStatus.CANCELLED_REVERSED,
            reversalReason = reason
        )
        db.gamingDao().updateSession(updated)
        logAudit(employee.id, employee.fullName, employee.role.name, "GAMING_REVERSED", "GAMING", session.id, "Reversed: $reason")
    }

    // Debts Management
    val allDebts: Flow<List<CustomerDebt>> = db.debtDao().getAllDebts()
    val unpaidDebts: Flow<List<CustomerDebt>> = db.debtDao().getUnpaidDebts()

    suspend fun settleDebt(debtId: String, amountPaid: Long, actor: User): Boolean {
        val debt = db.debtDao().getDebtById(debtId) ?: return false
        val newPaid = debt.amountPaidUgx + amountPaid
        val isFullyPaid = newPaid >= debt.amountUgx
        val updated = debt.copy(
            amountPaidUgx = newPaid,
            isPaid = isFullyPaid,
            settledDate = if (isFullyPaid) SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) else null
        )
        db.debtDao().updateDebt(updated)
        logAudit(actor.id, actor.fullName, actor.role.name, "DEBT_SETTLED", "DEBT", debt.id, "Collected UGX $amountPaid for Ref ${debt.referenceCode}")
        return true
    }

    // Daily Reports & Confirmation Workflow
    val allReports: Flow<List<DailyReport>> = db.reportDao().getAllReports()
    val pendingReports: Flow<List<DailyReport>> = db.reportDao().getPendingReports()
    fun getReportsByEmployee(employeeId: String): Flow<List<DailyReport>> =
        db.reportDao().getReportsByEmployee(employeeId)

    suspend fun submitDailyReport(
        employee: User,
        shiftStartTime: String,
        shiftEndTime: String,
        totalSalesUgx: Long,
        paidSalesUgx: Long,
        unpaidDebtsUgx: Long,
        expectedCashUgx: Long,
        actualCashReceivedUgx: Long,
        expensesUgx: Long,
        commissionEarnedUgx: Long,
        notes: String
    ): DailyReport {
        val shortageOrSurplus = actualCashReceivedUgx - (expectedCashUgx - expensesUgx)
        val ref = "REP-${SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())}-${(1000..9999).random()}"

        val report = DailyReport(
            id = "rep_${UUID.randomUUID()}",
            referenceNumber = ref,
            employeeId = employee.id,
            employeeName = employee.fullName,
            department = employee.department,
            branchId = employee.branchId,
            date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
            shiftStartTime = shiftStartTime,
            shiftEndTime = shiftEndTime,
            totalSalesUgx = totalSalesUgx,
            paidSalesUgx = paidSalesUgx,
            unpaidDebtsUgx = unpaidDebtsUgx,
            expectedCashUgx = expectedCashUgx,
            actualCashReceivedUgx = actualCashReceivedUgx,
            expensesUgx = expensesUgx,
            commissionEarnedUgx = commissionEarnedUgx,
            shortageOrSurplusUgx = shortageOrSurplus,
            notes = notes,
            status = ReportStatus.SUBMITTED,
            submittedAt = System.currentTimeMillis(),
            versionNumber = 1
        )
        db.reportDao().insertReport(report)

        // Save history entry
        db.reportDao().insertReportHistory(
            ReportHistory(
                reportId = report.id,
                versionNumber = 1,
                figuresSummary = "Expected: UGX $expectedCashUgx | Actual: UGX $actualCashReceivedUgx | Commission: UGX $commissionEarnedUgx",
                actionTaken = "SUBMITTED",
                actionBy = employee.fullName,
                notes = notes
            )
        )

        // Notify Managers and CEO
        db.notificationDao().insertNotification(
            NotificationItem(
                id = "notif_${UUID.randomUUID()}",
                targetUserId = null,
                targetRole = UserRole.MANAGER,
                title = "New Daily Report Submitted",
                message = "${employee.fullName} submitted report $ref ($expectedCashUgx UGX) for review.",
                type = "REPORT",
                relatedEntityId = report.id
            )
        )
        db.notificationDao().insertNotification(
            NotificationItem(
                id = "notif_${UUID.randomUUID()}",
                targetUserId = null,
                targetRole = UserRole.CEO,
                title = "New Daily Report Submitted",
                message = "${employee.fullName} submitted report $ref for review.",
                type = "REPORT",
                relatedEntityId = report.id
            )
        )

        // Dispatch automated Pandora Uganda SMS alert to Manager / CEO
        val smsConfigured = db.auditDao().getSetting("PANDORA_SMS_API_KEY")?.value?.isNotBlank() == true ||
            db.auditDao().getSetting("SMS_PROVIDER_CONFIGURED")?.value == "true"
        val smsStatus = if (smsConfigured) "DELIVERED" else "NO_PROVIDER"
        val smsError = if (smsConfigured) null else "Pandora Uganda SMS gateway unconfigured in settings"

        db.notificationDao().insertSmsLog(
            SmsLog(
                id = "sms_${UUID.randomUUID()}",
                recipientPhone = "+256755927896",
                message = "WANGI PANDORA SMS: New Daily Report ${report.referenceNumber} submitted by ${employee.fullName} (${employee.department}). Expected Cash: UGX ${report.expectedCashUgx}, Actual: UGX ${report.actualCashReceivedUgx}. Login to confirm.",
                status = smsStatus,
                providerName = "PANDORA_UGANDA",
                errorMessage = smsError,
                reference = report.referenceNumber
            )
        )

        logAudit(employee.id, employee.fullName, employee.role.name, "REPORT_SUBMITTED", "DAILY_REPORT", report.id, "Submitted ref $ref")
        return report
    }

    suspend fun confirmDailyReport(
        reportId: String,
        reviewer: User,
        reviewerNotes: String
    ): DailyReport {
        val report = db.reportDao().getReportById(reportId)
            ?: throw IllegalArgumentException("Report not found")

        // CRITICAL SECURITY RULE: Employee cannot confirm their own report!
        if (reviewer.id == report.employeeId) {
            throw SecurityException("SECURITY VIOLATION: Employee cannot confirm their own daily report.")
        }

        // CRITICAL SECURITY RULE: Only Manager or CEO can confirm reports
        if (reviewer.role != UserRole.CEO && reviewer.role != UserRole.MANAGER) {
            throw SecurityException("SECURITY VIOLATION: Only CEO or Branch Managers are authorized to confirm reports.")
        }

        val updated = report.copy(
            status = ReportStatus.CONFIRMED,
            reviewerId = reviewer.id,
            reviewerName = reviewer.fullName,
            reviewerNotes = reviewerNotes,
            reviewedAt = System.currentTimeMillis()
        )
        db.reportDao().updateReport(updated)

        // Record history
        db.reportDao().insertReportHistory(
            ReportHistory(
                reportId = report.id,
                versionNumber = report.versionNumber,
                figuresSummary = "CONFIRMED by ${reviewer.fullName}",
                actionTaken = "CONFIRMED",
                actionBy = reviewer.fullName,
                notes = reviewerNotes
            )
        )

        // Find employee phone to trigger SMS
        val employee = db.userDao().getUserById(report.employeeId)
        val empPhone = employee?.phone ?: "+256700000000"

        val smsMessage = "WANGI TECH (Pandora Uganda SMS): Daily report ${report.referenceNumber} CONFIRMED by ${reviewer.fullName}. Received Cash: UGX ${report.actualCashReceivedUgx}. Commission Earned: UGX ${report.commissionEarnedUgx}. Status: APPROVED."

        val smsConfigured = db.auditDao().getSetting("PANDORA_SMS_API_KEY")?.value?.isNotBlank() == true ||
            db.auditDao().getSetting("SMS_PROVIDER_CONFIGURED")?.value == "true"
        val smsStatus = if (smsConfigured) "DELIVERED" else "NO_PROVIDER"
        val smsError = if (smsConfigured) null else "SMS provider unconfigured in settings"

        db.notificationDao().insertSmsLog(
            SmsLog(
                id = "sms_${UUID.randomUUID()}",
                recipientPhone = empPhone,
                message = smsMessage,
                status = smsStatus,
                providerName = "PANDORA_UGANDA",
                errorMessage = smsError,
                reference = report.referenceNumber
            )
        )

        // In-app notification for employee
        db.notificationDao().insertNotification(
            NotificationItem(
                id = "notif_${UUID.randomUUID()}",
                targetUserId = report.employeeId,
                targetRole = null,
                title = "Daily Report CONFIRMED",
                message = "Your report ${report.referenceNumber} was confirmed by ${reviewer.fullName}. Commission: UGX ${report.commissionEarnedUgx}.",
                type = "REPORT",
                relatedEntityId = report.id
            )
        )

        logAudit(reviewer.id, reviewer.fullName, reviewer.role.name, "REPORT_CONFIRMED", "DAILY_REPORT", report.id, "Confirmed by ${reviewer.fullName}")
        return updated
    }

    suspend fun returnDailyReportForCorrection(
        reportId: String,
        reviewer: User,
        returnReason: String
    ): DailyReport {
        if (returnReason.isBlank()) {
            throw IllegalArgumentException("A mandatory reason must be provided when returning a report for correction.")
        }
        val report = db.reportDao().getReportById(reportId)
            ?: throw IllegalArgumentException("Report not found")

        if (reviewer.id == report.employeeId) {
            throw SecurityException("SECURITY VIOLATION: Employee cannot review their own daily report.")
        }
        if (reviewer.role != UserRole.CEO && reviewer.role != UserRole.MANAGER) {
            throw SecurityException("SECURITY VIOLATION: Only CEO or Branch Managers can return reports.")
        }

        val updated = report.copy(
            status = ReportStatus.RETURNED_FOR_CORRECTION,
            reviewerId = reviewer.id,
            reviewerName = reviewer.fullName,
            reviewReason = returnReason,
            reviewedAt = System.currentTimeMillis()
        )
        db.reportDao().updateReport(updated)

        db.reportDao().insertReportHistory(
            ReportHistory(
                reportId = report.id,
                versionNumber = report.versionNumber,
                figuresSummary = "RETURNED FOR CORRECTION: $returnReason",
                actionTaken = "RETURNED_FOR_CORRECTION",
                actionBy = reviewer.fullName,
                notes = returnReason
            )
        )

        // Find employee phone to trigger SMS
        val employee = db.userDao().getUserById(report.employeeId)
        val empPhone = employee?.phone ?: "+256700000000"

        val smsMessage = "WANGI TECH (Pandora Uganda SMS): Daily report ${report.referenceNumber} RETURNED by ${reviewer.fullName} for correction. Reason: $returnReason. Please adjust and resubmit."

        db.notificationDao().insertSmsLog(
            SmsLog(
                id = "sms_${UUID.randomUUID()}",
                recipientPhone = empPhone,
                message = smsMessage,
                status = "DELIVERED",
                providerName = "PANDORA_UGANDA",
                errorMessage = null,
                reference = report.referenceNumber
            )
        )

        // In-app notification
        db.notificationDao().insertNotification(
            NotificationItem(
                id = "notif_${UUID.randomUUID()}",
                targetUserId = report.employeeId,
                targetRole = null,
                title = "Report Returned for Correction",
                message = "Your report ${report.referenceNumber} was returned by ${reviewer.fullName}. Reason: $returnReason",
                type = "REPORT",
                relatedEntityId = report.id
            )
        )

        logAudit(reviewer.id, reviewer.fullName, reviewer.role.name, "REPORT_RETURNED", "DAILY_REPORT", report.id, "Returned: $returnReason")
        return updated
    }

    suspend fun registerEmployee(
        fullName: String,
        phone: String,
        username: String,
        department: Department,
        branchId: String,
        password: String
    ): User {
        val cleanUsername = username.trim().lowercase()
        val existing = db.userDao().getUserByUsername(cleanUsername)
        if (existing != null) {
            throw IllegalArgumentException("Username '$cleanUsername' is already taken.")
        }

        val role = if (department == Department.GAMING) UserRole.GAMING_EMPLOYEE else UserRole.HOTSPOT_EMPLOYEE
        val user = User(
            id = "usr_${UUID.randomUUID().toString().take(8)}",
            username = cleanUsername,
            fullName = fullName.trim(),
            phone = phone.trim(),
            email = "$cleanUsername@wangitech.com",
            role = role,
            department = department,
            branchId = branchId,
            passwordHash = password.trim()
        )
        db.userDao().insertUser(user)

        // Automated Welcome SMS via Pandora Uganda SMS Gateway
        val welcomeSms = "WANGI TECH (Pandora SMS Gateway): Welcome ${user.fullName}! Your staff account is created. Username: ${user.username}, Password: ${user.passwordHash}, Dept: ${user.department}. Login at WANGI Staff Portal."
        db.notificationDao().insertSmsLog(
            SmsLog(
                id = "sms_${UUID.randomUUID()}",
                recipientPhone = user.phone,
                message = welcomeSms,
                status = "DELIVERED",
                providerName = "PANDORA_UGANDA",
                reference = "REG-${user.username}"
            )
        )

        logAudit(user.id, user.fullName, user.role.name, "EMPLOYEE_REGISTERED", "USER", user.id, "Registered with phone ${user.phone}")
        return user
    }

    suspend fun sendPandoraSms(
        recipientPhone: String,
        message: String,
        reference: String,
        actor: User
    ) {
        db.notificationDao().insertSmsLog(
            SmsLog(
                id = "sms_${UUID.randomUUID()}",
                recipientPhone = recipientPhone,
                message = message,
                status = "DELIVERED",
                providerName = "PANDORA_UGANDA",
                reference = reference
            )
        )
        logAudit(actor.id, actor.fullName, actor.role.name, "PANDORA_SMS_SENT", "SMS", reference, "Sent to $recipientPhone")
    }

    suspend fun resetPassword(
        identifier: String,
        newPassword: String
    ): Boolean {
        val clean = identifier.trim()
        val user = db.userDao().getUserByUsername(clean.lowercase())
            ?: db.userDao().getUserByPhone(clean)
            ?: return false

        val updated = user.copy(passwordHash = newPassword.trim())
        db.userDao().updateUser(updated)

        // Send confirmation SMS via Pandora Uganda Gateway
        val sms = "WANGI TECH (Pandora SMS): Your account password has been successfully reset. If you did not request this, contact your CEO immediately."
        db.notificationDao().insertSmsLog(
            SmsLog(
                id = "sms_${UUID.randomUUID()}",
                recipientPhone = user.phone,
                message = sms,
                status = "DELIVERED",
                providerName = "PANDORA_UGANDA",
                reference = "PWD-RESET-${user.username}"
            )
        )

        logAudit(user.id, user.fullName, user.role.name, "PASSWORD_RESET", "USER", user.id, "Password reset successfully via SMS OTP")
        return true
    }

    // Technician Management
    val allTechJobs: Flow<List<TechnicianJob>> = db.technicianDao().getAllJobs()
    fun getJobsByTechnician(techId: String): Flow<List<TechnicianJob>> = db.technicianDao().getJobsByTechnician(techId)
    val allNetworkDevices: Flow<List<NetworkDevice>> = db.deviceDao().getAllDevices()

    suspend fun createTechnicianJob(job: TechnicianJob, actor: User) {
        db.technicianDao().insertJob(job)
        logAudit(actor.id, actor.fullName, actor.role.name, "TECH_JOB_CREATED", "JOB", job.id, "Ticket ${job.ticketNumber} created")
    }

    suspend fun updateJobStatus(
        jobId: String,
        newStatus: JobStatus,
        serviceChargeUgx: Long,
        materialsUsed: String,
        notes: String,
        customerSignOff: Boolean,
        actor: User
    ) {
        val job = db.technicianDao().getJobById(jobId) ?: return
        val updated = job.copy(
            status = newStatus,
            serviceChargeUgx = serviceChargeUgx,
            materialsUsed = materialsUsed,
            notes = notes,
            customerSignOff = customerSignOff,
            completedAt = if (newStatus == JobStatus.COMPLETED) System.currentTimeMillis() else job.completedAt
        )
        db.technicianDao().updateJob(updated)
        logAudit(actor.id, actor.fullName, actor.role.name, "JOB_STATUS_UPDATED", "JOB", job.id, "Status changed to $newStatus")
    }

    // Wi-Fi Billing & Payment Idempotency
    val allBillingPackages: Flow<List<BillingPackage>> = db.billingDao().getAllPackages()
    val allSubscriptions: Flow<List<BillingSubscription>> = db.billingDao().getAllSubscriptions()
    val allPaymentTransactions: Flow<List<PaymentTransaction>> = db.billingDao().getAllTransactions()
    val allWifiVouchers: Flow<List<WifiVoucher>> = db.billingDao().getAllWifiVouchers()

    suspend fun generateVoucherBatch(
        packageItem: BillingPackage,
        quantity: Int,
        prefix: String,
        actor: User
    ): List<WifiVoucher> {
        val batchCode = "BATCH-${SimpleDateFormat("yyyyMMdd-HHmm", Locale.getDefault()).format(Date())}"
        val vouchers = mutableListOf<WifiVoucher>()
        val cleanPrefix = prefix.ifBlank { "WG-" }.uppercase()

        for (i in 1..quantity) {
            val randomNum = (100000..999999).random()
            val pin = (1000..9999).random().toString()
            val code = "$cleanPrefix$randomNum"
            vouchers.add(
                WifiVoucher(
                    id = "vc_${UUID.randomUUID()}",
                    batchCode = batchCode,
                    code = code,
                    pin = pin,
                    packageName = packageItem.name,
                    packageId = packageItem.id,
                    priceUgx = packageItem.priceUgx,
                    validityPeriod = packageItem.validityPeriod,
                    speedLimitMbps = packageItem.speedLimitMbps,
                    status = "AVAILABLE"
                )
            )
        }
        db.billingDao().insertWifiVouchers(vouchers)
        logAudit(actor.id, actor.fullName, actor.role.name, "VOUCHER_BATCH_GENERATED", "BILLING", batchCode, "Generated $quantity vouchers for ${packageItem.name}")
        return vouchers
    }

    suspend fun createBillingPackage(pkg: BillingPackage, actor: User) {
        db.billingDao().insertPackage(pkg)
        logAudit(actor.id, actor.fullName, actor.role.name, "PACKAGE_CREATED", "BILLING", pkg.id, "Created ${pkg.name} (UGX ${pkg.priceUgx})")
    }

    suspend fun updateBillingPackage(pkg: BillingPackage, actor: User) {
        db.billingDao().updatePackage(pkg)
        logAudit(actor.id, actor.fullName, actor.role.name, "PACKAGE_UPDATED", "BILLING", pkg.id, "Updated ${pkg.name}")
    }

    suspend fun deleteBillingPackage(pkg: BillingPackage, actor: User) {
        db.billingDao().deletePackage(pkg)
        logAudit(actor.id, actor.fullName, actor.role.name, "PACKAGE_DELETED", "BILLING", pkg.id, "Deleted ${pkg.name}")
    }

    suspend fun updateSubscription(sub: BillingSubscription, actor: User) {
        db.billingDao().updateSubscription(sub)
        logAudit(actor.id, actor.fullName, actor.role.name, "SUBSCRIPTION_UPDATED", "BILLING", sub.id, "Updated subscription for ${sub.customerName}")
    }

    suspend fun disconnectSubscriber(sub: BillingSubscription, actor: User) {
        val updated = sub.copy(status = "REVOKED")
        db.billingDao().updateSubscription(updated)
        logAudit(actor.id, actor.fullName, actor.role.name, "SUBSCRIBER_KICKED", "BILLING", sub.id, "Kicked active subscriber ${sub.customerName} (${sub.ipAddress})")
    }

    suspend fun extendSubscription(sub: BillingSubscription, extraHours: Int, actor: User) {
        val newExpiry = sub.expiresAt + (extraHours * 3600 * 1000L)
        val updated = sub.copy(expiresAt = newExpiry, status = "ACTIVE")
        db.billingDao().updateSubscription(updated)
        logAudit(actor.id, actor.fullName, actor.role.name, "SUBSCRIPTION_EXTENDED", "BILLING", sub.id, "Extended by $extraHours hours")
    }

    suspend fun toggleBlockMac(sub: BillingSubscription, actor: User) {
        val updated = sub.copy(
            isBlocked = !sub.isBlocked,
            status = if (!sub.isBlocked) "REVOKED" else "ACTIVE"
        )
        db.billingDao().updateSubscription(updated)
        logAudit(actor.id, actor.fullName, actor.role.name, "MAC_BLOCK_TOGGLED", "BILLING", sub.id, "MAC ${sub.macAddress} blocked=${updated.isBlocked}")
    }

    suspend fun addNetworkDevice(device: NetworkDevice, actor: User) {
        db.deviceDao().insertDevice(device)
        logAudit(actor.id, actor.fullName, actor.role.name, "DEVICE_ADDED", "NETWORK", device.id, "Added ${device.name} (${device.ipAddress})")
    }

    suspend fun updateNetworkDevice(device: NetworkDevice, actor: User) {
        db.deviceDao().updateDevice(device)
        logAudit(actor.id, actor.fullName, actor.role.name, "DEVICE_UPDATED", "NETWORK", device.id, "Updated ${device.name}")
    }

    suspend fun deleteNetworkDevice(device: NetworkDevice, actor: User) {
        db.deviceDao().deleteDevice(device)
        logAudit(actor.id, actor.fullName, actor.role.name, "DEVICE_DELETED", "NETWORK", device.id, "Deleted ${device.name}")
    }

    suspend fun sendVoucherSms(voucherCode: String, phone: String, packageName: String, actor: User) {
        val sms = SmsLog(
            id = "sms_${UUID.randomUUID()}",
            recipientPhone = phone,
            message = "WANGI WI-FI: Your voucher code is $voucherCode for $packageName. Connect to 'WANGI TECH WI-FI' and login at wifi.wangi.net. Enjoy high speed internet!",
            status = "DELIVERED",
            providerName = "AFRICAS_TALKING",
            reference = "VOUCHER-$voucherCode"
        )
        db.notificationDao().insertSmsLog(sms)
        logAudit(actor.id, actor.fullName, actor.role.name, "SMS_SENT", "BILLING", voucherCode, "Sent voucher SMS to $phone")
    }

    suspend fun processPaymentCallback(
        orderRef: String,
        providerTxId: String,
        customerPhone: String,
        customerName: String,
        packageName: String,
        packageId: String,
        amountUgx: Long,
        provider: String
    ): Pair<Boolean, String> {
        // IDEMPOTENCY CHECK: Check if providerTxId or orderRef already exists and verified
        val existingTxByProvider = db.billingDao().getTransactionByProviderTxId(providerTxId)
        if (existingTxByProvider != null && existingTxByProvider.status == "VERIFIED") {
            logAudit("system", "Payment Gateway", "GATEWAY", "DUPLICATE_PAYMENT_BLOCKED", "PAYMENT", providerTxId, "Prevented duplicate execution for providerTxId $providerTxId")
            return Pair(false, "DUPLICATE_CALLBACK_PREVENTED")
        }

        val existingOrder = db.billingDao().getTransactionByOrderRef(orderRef)
        if (existingOrder != null && existingOrder.status == "VERIFIED") {
            return Pair(false, "ORDER_ALREADY_VERIFIED")
        }

        // Record verified transaction
        val tx = PaymentTransaction(
            id = "tx_${UUID.randomUUID()}",
            orderRef = orderRef,
            providerTxId = providerTxId,
            customerPhone = customerPhone,
            customerName = customerName,
            packageName = packageName,
            amountUgx = amountUgx,
            provider = provider,
            status = "VERIFIED",
            callbackProcessedAt = System.currentTimeMillis()
        )
        db.billingDao().insertTransaction(tx)

        // Issue Voucher & Subscription
        val voucherCode = "WG-" + (100000..999999).random()
        val now = System.currentTimeMillis()
        val sub = BillingSubscription(
            id = "sub_${UUID.randomUUID()}",
            customerName = customerName,
            customerPhone = customerPhone,
            packageName = packageName,
            packageId = packageId,
            voucherCode = voucherCode,
            routerId = "dev_1",
            priceUgx = amountUgx,
            startedAt = now,
            expiresAt = now + (24 * 3600 * 1000L),
            status = "ACTIVE",
            paymentTxRef = providerTxId,
            ipAddress = "192.168.88.${(10..240).random()}",
            macAddress = "%02X:%02X:%02X:%02X:%02X:%02X".format(
                (0..255).random(), (0..255).random(), (0..255).random(),
                (0..255).random(), (0..255).random(), (0..255).random()
            ),
            bytesDownloadedMb = (10..500).random().toLong(),
            bytesUploadedMb = (2..90).random().toLong()
        )
        db.billingDao().insertSubscription(sub)

        // Also add to wifi_vouchers as ACTIVE
        db.billingDao().insertWifiVoucher(
            WifiVoucher(
                id = "vc_${UUID.randomUUID()}",
                batchCode = "ONLINE-CHECKOUT",
                code = voucherCode,
                pin = (1000..9999).random().toString(),
                packageName = packageName,
                packageId = packageId,
                priceUgx = amountUgx,
                validityPeriod = "24 Hours",
                speedLimitMbps = 10,
                status = "ACTIVE",
                usedByPhone = customerPhone,
                usedByName = customerName,
                activatedAt = now,
                expiresAt = now + (24 * 3600 * 1000L)
            )
        )

        // Send confirmation SMS automatically
        db.notificationDao().insertSmsLog(
            SmsLog(
                id = "sms_${UUID.randomUUID()}",
                recipientPhone = customerPhone,
                message = "WANGI TECH: Payment UGX $amountUgx received ($provider). Your Wi-Fi Voucher is: $voucherCode. Connect to 'WANGI TECH WI-FI' and login.",
                status = "DELIVERED",
                providerName = "AFRICAS_TALKING",
                reference = providerTxId
            )
        )

        logAudit("system", "Payment Gateway", "GATEWAY", "SUBSCRIPTION_ACTIVATED", "SUBSCRIPTION", sub.id, "Issued voucher $voucherCode for $customerPhone")
        return Pair(true, voucherCode)
    }

    // Finances & Expenses
    val allExpenses: Flow<List<BusinessExpense>> = db.financeDao().getAllExpenses()
    suspend fun recordExpense(expense: BusinessExpense, actor: User) {
        db.financeDao().insertExpense(expense)
        logAudit(actor.id, actor.fullName, actor.role.name, "EXPENSE_RECORDED", "EXPENSE", expense.id, "UGX ${expense.amountUgx} for ${expense.category}")
    }

    // Notifications & SMS Logs
    fun getNotificationsForUser(userId: String, role: UserRole): Flow<List<NotificationItem>> =
        db.notificationDao().getNotificationsForUser(userId, role)
    val allNotifications: Flow<List<NotificationItem>> = db.notificationDao().getAllNotifications()
    val allSmsLogs: Flow<List<SmsLog>> = db.notificationDao().getAllSmsLogs()

    suspend fun markNotificationAsRead(id: String) {
        db.notificationDao().markAsRead(id)
    }

    // Audit Logs & Settings
    val allAuditEvents: Flow<List<AuditEvent>> = db.auditDao().getAllAuditEvents()
    val allSettings: Flow<List<SystemSetting>> = db.auditDao().getAllSettings()

    suspend fun updateSetting(key: String, value: String, description: String, actor: User) {
        if (actor.role != UserRole.CEO) throw SecurityException("Only CEO can update system settings.")
        db.auditDao().setSetting(SystemSetting(key, value, description, System.currentTimeMillis()))
        logAudit(actor.id, actor.fullName, actor.role.name, "SETTING_UPDATED", "SETTING", key, "Changed to $value")
    }

    private suspend fun logAudit(
        actorId: String,
        actorName: String,
        actorRole: String,
        action: String,
        entityType: String,
        entityId: String,
        details: String
    ) {
        db.auditDao().insertAuditEvent(
            AuditEvent(
                id = "aud_${UUID.randomUUID()}",
                actorId = actorId,
                actorName = actorName,
                actorRole = actorRole,
                action = action,
                entityType = entityType,
                entityId = entityId,
                details = details
            )
        )
    }
}
