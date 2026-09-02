package com.example.data

import android.content.Context
import com.example.service.EmailNotificationService
import com.example.service.LocalNotificationHelper
import com.example.ui.NotificationSystem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

class LeaveRepository private constructor(private val context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val leaveRequestDao = db.leaveRequestDao()
    private val alertDao = db.alertDao()
    private val scope = CoroutineScope(Dispatchers.IO)
    private val firestoreSync = FirestoreSyncManager.getInstance(context)

    // Observable current active alert to show in Dialog
    private val _activePopupAlert = MutableStateFlow<AppAlert?>(null)
    val activePopupAlert: StateFlow<AppAlert?> = _activePopupAlert.asStateFlow()

    init {
        LocalNotificationHelper.createNotificationChannels(context)
        scope.launch {
            seedDefaultDataIfEmpty()
            firestoreSync.startRealtimeListeners()
        }
    }

    private suspend fun seedDefaultDataIfEmpty() {
        val existing = leaveRequestDao.getAllRequests()
        if (existing.isEmpty()) {
            val defaults = listOf(
                LeaveRequestEntity(
                    id = "seed-1",
                    employeeEmail = "aya@gmail.com",
                    employeeName = "aya",
                    department = "Ingénierie & IT",
                    leaveType = "Congés payés",
                    startDate = "12/10/2026",
                    endDate = "14/10/2026",
                    startDay = 12,
                    endDay = 14,
                    month = Calendar.OCTOBER,
                    year = 2026,
                    daysCount = 3,
                    reason = "Congés annuels d'automne en famille",
                    status = "PENDING",
                    createdAt = System.currentTimeMillis() - 86400000L * 2
                ),
                LeaveRequestEntity(
                    id = "seed-2",
                    employeeEmail = "mouad@gmail.com",
                    employeeName = "mouad elmzabite",
                    department = "Marketing & Com",
                    leaveType = "Maladie",
                    startDate = "15/10/2026",
                    endDate = "15/10/2026",
                    startDay = 15,
                    endDay = 15,
                    month = Calendar.OCTOBER,
                    year = 2026,
                    daysCount = 1,
                    reason = "Arrêt maladie - Rendez-vous médical",
                    status = "PENDING",
                    createdAt = System.currentTimeMillis() - 86400000L
                ),
                LeaveRequestEntity(
                    id = "seed-3",
                    employeeEmail = "walid@gmail.com",
                    employeeName = "walid elmzabite",
                    department = "Ingénierie & IT",
                    leaveType = "Télétravail",
                    startDate = "18/10/2026",
                    endDate = "20/10/2026",
                    startDay = 18,
                    endDay = 20,
                    month = Calendar.OCTOBER,
                    year = 2026,
                    daysCount = 2,
                    reason = "Travail à distance / concentration sprint",
                    status = "PENDING",
                    createdAt = System.currentTimeMillis() - 43200000L
                ),
                LeaveRequestEntity(
                    id = "seed-4",
                    employeeEmail = "yassine@gmail.com",
                    employeeName = "mohamed yassine elmzabite",
                    department = "Ingénierie & IT",
                    leaveType = "Congés payés",
                    startDate = "13/10/2026",
                    endDate = "16/10/2026",
                    startDay = 13,
                    endDay = 16,
                    month = Calendar.OCTOBER,
                    year = 2026,
                    daysCount = 4,
                    reason = "Repos annuel",
                    status = "PENDING",
                    createdAt = System.currentTimeMillis() - 21600000L
                ),
                LeaveRequestEntity(
                    id = "seed-5",
                    employeeEmail = "khadija@gmail.com",
                    employeeName = "khadija el ferrouni",
                    department = "Ingénierie & IT",
                    leaveType = "RTT",
                    startDate = "02/09/2026",
                    endDate = "04/09/2026",
                    startDay = 2,
                    endDay = 4,
                    month = Calendar.SEPTEMBER,
                    year = 2026,
                    daysCount = 3,
                    reason = "Récupération temps de travail",
                    status = "APPROVED",
                    createdAt = System.currentTimeMillis() - 86400000L * 30,
                    decisionAt = System.currentTimeMillis() - 86400000L * 29
                )
            )
            leaveRequestDao.insertAll(defaults)
        }
    }

    fun getAllRequestsFlow(): Flow<List<LeaveRequestEntity>> = leaveRequestDao.getAllRequestsFlow()

    fun getPendingRequestsFlow(): Flow<List<LeaveRequestEntity>> = leaveRequestDao.getPendingRequestsFlow()

    fun getAllApprovedRequestsFlow(): Flow<List<LeaveRequestEntity>> = leaveRequestDao.getAllApprovedRequestsFlow()

    suspend fun getApprovedRequestsByMonth(month: Int, year: Int): List<LeaveRequestEntity> =
        leaveRequestDao.getApprovedRequestsByMonth(month, year)

    fun getEmployeeRequestsFlow(email: String): Flow<List<LeaveRequestEntity>> =
        leaveRequestDao.getRequestsByEmployeeFlow(email)

    fun getAlertsForUserFlow(email: String): Flow<List<AppAlert>> =
        alertDao.getAlertsForUserFlow(email)

    suspend fun submitLeaveRequest(
        user: User,
        leaveType: String,
        startDateStr: String,
        endDateStr: String,
        reason: String,
        attachmentName: String? = null
    ): LeaveRequestEntity {
        val (startDay, endDay, month, year, count) = parseDateDetails(startDateStr, endDateStr)
        val requestId = UUID.randomUUID().toString()

        val request = LeaveRequestEntity(
            id = requestId,
            employeeEmail = user.email,
            employeeName = user.fullName,
            department = user.department,
            leaveType = leaveType,
            startDate = startDateStr,
            endDate = endDateStr,
            startDay = startDay,
            endDay = endDay,
            month = month,
            year = year,
            daysCount = count,
            reason = reason.ifBlank { "Demande de $leaveType" },
            attachmentName = attachmentName,
            status = "PENDING",
            createdAt = System.currentTimeMillis()
        )

        leaveRequestDao.insertRequest(request)
        
        // Push to Cloud Firestore for instant real-time synchronization with Admin and other devices
        scope.launch {
            firestoreSync.uploadLeaveRequest(request)
        }

        // Notification for admin and user
        NotificationSystem.sendNotification(
            title = "Demande envoyée à l'administrateur",
            message = "Votre demande de $leaveType ($startDateStr au $endDateStr) a été transmise pour validation.",
            isApproved = true
        )

        // Send a notification email to the manager
        EmailNotificationService.sendNewRequestEmailToManager(
            employeeName = user.fullName,
            leaveType = leaveType,
            dates = "$startDateStr au $endDateStr",
            reason = reason
        )

        return request
    }

    suspend fun approveRequest(requestId: String, adminComment: String = "") {
        val req = leaveRequestDao.getRequestById(requestId) ?: return
        val now = System.currentTimeMillis()
        leaveRequestDao.updateStatus(requestId, "APPROVED", now, adminComment)

        // Push status change to Firestore and Cloud Relay
        scope.launch {
            firestoreSync.updateLeaveRequestStatus(
                requestId = requestId,
                status = "APPROVED",
                decisionAt = now,
                adminComment = adminComment,
                targetUserEmail = req.employeeEmail,
                employeeName = req.employeeName,
                leaveType = req.leaveType,
                dates = "${req.startDate} - ${req.endDate}"
            )
        }

        // 1. Send Android System Local Notification to alert employee immediately
        LocalNotificationHelper.sendLeaveStatusNotification(
            context = context,
            requestId = requestId,
            employeeName = req.employeeName,
            leaveType = req.leaveType,
            dates = "${req.startDate} - ${req.endDate}",
            isApproved = true,
            adminComment = adminComment
        )

        // 2. Send / Log formal Email
        EmailNotificationService.sendLeaveStatusEmail(
            employeeName = req.employeeName,
            employeeEmail = req.employeeEmail,
            leaveType = req.leaveType,
            dates = "${req.startDate} - ${req.endDate}",
            isApproved = true,
            adminComment = adminComment
        )

        // 3. Create in-app alert for employee notification center
        val alert = AppAlert(
            id = UUID.randomUUID().toString(),
            targetUserEmail = req.employeeEmail,
            title = "Demande de congé VALIDÉE",
            message = "Votre demande de ${req.leaveType} pour la période du ${req.startDate} au ${req.endDate} a été VALIDÉE par l'administrateur." +
                    if (adminComment.isNotBlank()) "\nCommentaire : $adminComment" else "",
            type = "APPROVED",
            timestamp = now,
            isRead = false,
            isPopupShown = false
        )
        alertDao.insertAlert(alert)
        _activePopupAlert.value = alert

        // Push alert to Firestore
        scope.launch {
            firestoreSync.uploadAlert(alert)
        }

        // 4. Emit notification system toast / snackbar
        NotificationSystem.sendNotification(
            title = "Demande Validée",
            message = "${req.employeeName} : Demande pour ${req.leaveType} (${req.startDate} - ${req.endDate}) a été approuvée.",
            isApproved = true
        )
    }

    suspend fun rejectRequest(requestId: String, adminComment: String = "") {
        val req = leaveRequestDao.getRequestById(requestId) ?: return
        val now = System.currentTimeMillis()
        leaveRequestDao.updateStatus(requestId, "REJECTED", now, adminComment)

        // Push status change to Firestore and Cloud Relay
        scope.launch {
            firestoreSync.updateLeaveRequestStatus(
                requestId = requestId,
                status = "REJECTED",
                decisionAt = now,
                adminComment = adminComment,
                targetUserEmail = req.employeeEmail,
                employeeName = req.employeeName,
                leaveType = req.leaveType,
                dates = "${req.startDate} - ${req.endDate}"
            )
        }

        // 1. Send Android System Local Notification to alert employee immediately
        LocalNotificationHelper.sendLeaveStatusNotification(
            context = context,
            requestId = requestId,
            employeeName = req.employeeName,
            leaveType = req.leaveType,
            dates = "${req.startDate} - ${req.endDate}",
            isApproved = false,
            adminComment = adminComment
        )

        // 2. Send / Log formal Email
        EmailNotificationService.sendLeaveStatusEmail(
            employeeName = req.employeeName,
            employeeEmail = req.employeeEmail,
            leaveType = req.leaveType,
            dates = "${req.startDate} - ${req.endDate}",
            isApproved = false,
            adminComment = adminComment
        )

        // 3. Create in-app alert for employee notification center
        val alert = AppAlert(
            id = UUID.randomUUID().toString(),
            targetUserEmail = req.employeeEmail,
            title = "Demande de congé REFUSÉE",
            message = "Votre demande de ${req.leaveType} pour la période du ${req.startDate} au ${req.endDate} a été REFUSÉE par l'administrateur." +
                    if (adminComment.isNotBlank()) "\nMotif : $adminComment" else "",
            type = "REJECTED",
            timestamp = now,
            isRead = false,
            isPopupShown = false
        )
        alertDao.insertAlert(alert)
        _activePopupAlert.value = alert

        // Push alert to Firestore
        scope.launch {
            firestoreSync.uploadAlert(alert)
        }

        // 4. Emit notification system toast / snackbar
        NotificationSystem.sendNotification(
            title = "Demande Refusée",
            message = "${req.employeeName} : Demande pour ${req.leaveType} (${req.startDate} - ${req.endDate}) a été refusée.",
            isApproved = false
        )
    }


    fun getUnreadAlertsCountFlow(email: String): Flow<Int> {
        return alertDao.getUnreadCountFlow(email)
    }

    suspend fun markAlertRead(alertId: String) {
        alertDao.markAlertRead(alertId)
        scope.launch {
            firestoreSync.markAlertRead(alertId)
        }
    }

    suspend fun markAllAlertsRead(email: String) {
        alertDao.markAllAlertsRead(email)
    }

    suspend fun deleteAlert(alertId: String) {
        alertDao.deleteAlertById(alertId)
        scope.launch {
            firestoreSync.deleteAlert(alertId)
        }
    }

    suspend fun clearAllAlerts(email: String) {
        alertDao.deleteAllAlertsForUser(email)
    }

    suspend fun triggerTestLeaveNotification(targetEmail: String, isApproved: Boolean) {
        val now = System.currentTimeMillis()
        val leaveType = if (isApproved) "Congés payés" else "RTT"
        val dates = "15/10/2026 - 22/10/2026"
        val comment = if (isApproved) "Demande test validée avec succès." else "Demande test refusée pour contrôle des alertes."

        // 1. Android System Local Notification
        LocalNotificationHelper.sendLeaveStatusNotification(
            context = context,
            requestId = "test_$now",
            employeeName = targetEmail.substringBefore("@").replace(".", " ").replaceFirstChar { it.uppercase() },
            leaveType = leaveType,
            dates = dates,
            isApproved = isApproved,
            adminComment = comment
        )

        // 2. Email log
        EmailNotificationService.sendLeaveStatusEmail(
            employeeName = targetEmail.substringBefore("@"),
            employeeEmail = targetEmail,
            leaveType = leaveType,
            dates = dates,
            isApproved = isApproved,
            adminComment = comment
        )

        // 3. In-App alert
        val alert = AppAlert(
            id = UUID.randomUUID().toString(),
            targetUserEmail = targetEmail,
            title = if (isApproved) "Demande de $leaveType VALIDÉE" else "Demande de $leaveType REFUSÉE",
            message = "Votre demande de $leaveType pour la période du $dates a été ${if (isApproved) "VALIDÉE" else "REFUSÉE"} par l'administrateur.\n$comment",
            type = if (isApproved) "APPROVED" else "REJECTED",
            timestamp = now,
            isRead = false,
            isPopupShown = false
        )
        alertDao.insertAlert(alert)
        _activePopupAlert.value = alert

        NotificationSystem.sendNotification(
            title = if (isApproved) "Demande Validée" else "Demande Refusée",
            message = "Alerte de test générée pour $targetEmail.",
            isApproved = isApproved
        )
    }

    suspend fun checkForUnshownAlerts(email: String) {
        val unshown = alertDao.getPendingPopupsForUser(email)
        if (unshown.isNotEmpty()) {
            _activePopupAlert.value = unshown.first()
        }
    }

    suspend fun dismissActiveAlert(alert: AppAlert) {
        alertDao.markAlertPopupShown(alert.id)
        alertDao.markAlertRead(alert.id)
        if (_activePopupAlert.value?.id == alert.id) {
            _activePopupAlert.value = null
        }
    }

    fun clearActiveAlert() {
        _activePopupAlert.value = null
    }

    private fun parseDateDetails(startDateStr: String, endDateStr: String): DateCalcResult {
        val patterns = listOf("dd/MM/yyyy", "yyyy-MM-dd", "d/M/yyyy", "dd-MM-yyyy")
        var startDate: Date? = null
        var endDate: Date? = null

        for (pattern in patterns) {
            if (startDate == null) {
                try {
                    val sdf = SimpleDateFormat(pattern, Locale.getDefault()).apply { isLenient = false }
                    startDate = sdf.parse(startDateStr.trim())
                } catch (_: Exception) {}
            }
            if (endDate == null) {
                try {
                    val sdf = SimpleDateFormat(pattern, Locale.getDefault()).apply { isLenient = false }
                    endDate = sdf.parse(endDateStr.trim())
                } catch (_: Exception) {}
            }
        }

        val calStart = Calendar.getInstance()
        if (startDate != null) {
            calStart.time = startDate
        } else {
            calStart.add(Calendar.DAY_OF_YEAR, 1)
        }

        val calEnd = Calendar.getInstance()
        if (endDate != null) {
            calEnd.time = endDate
        } else {
            calEnd.time = calStart.time
        }

        val startDay = calStart.get(Calendar.DAY_OF_MONTH)
        val endDay = calEnd.get(Calendar.DAY_OF_MONTH)
        val month = calStart.get(Calendar.MONTH)
        val year = calStart.get(Calendar.YEAR)

        val diffMillis = calEnd.timeInMillis - calStart.timeInMillis
        val days = maxOf(1, ((diffMillis / (1000 * 60 * 60 * 24)) + 1).toInt())

        return DateCalcResult(startDay, endDay, month, year, days)
    }

    private data class DateCalcResult(
        val startDay: Int,
        val endDay: Int,
        val month: Int,
        val year: Int,
        val daysCount: Int
    )

    companion object {
        @Volatile
        private var INSTANCE: LeaveRepository? = null

        fun getInstance(context: Context): LeaveRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = LeaveRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
