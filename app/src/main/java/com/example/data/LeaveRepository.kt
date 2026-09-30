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
    private val supabaseSync = SupabaseSyncManager.getInstance(context)

    // Observable current active alert to show in Dialog
    private val _activePopupAlert = MutableStateFlow<AppAlert?>(null)
    val activePopupAlert: StateFlow<AppAlert?> = _activePopupAlert.asStateFlow()

    init {
        LocalNotificationHelper.createNotificationChannels(context)
        scope.launch {
            cleanDemoDataIfPresent()
            firestoreSync.startRealtimeListeners()
            // If Supabase is configured, trigger initial sync
            if (supabaseSync.isConfigured.value) {
                supabaseSync.syncAll()
            }
        }
    }

    /**
     * Supprime toutes les données d'exemples / de démonstration (demandes seed-*, sample_*, demo-*)
     * pour assurer un environnement d'entreprise totalement propre et prêt pour la production.
     */
    private suspend fun cleanDemoDataIfPresent() {
        try {
            leaveRequestDao.deleteDemoRequests()
            alertDao.deleteDemoAlerts()
        } catch (_: Exception) {
            // Ignore if tables are empty
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
        category: String = "Inconnu",
        startDateStr: String,
        endDateStr: String,
        reason: String,
        attachmentName: String? = null,
        attachmentUri: String? = null
    ): LeaveRequestEntity {
        val (startDay, endDay, month, year, count) = parseDateDetails(startDateStr, endDateStr)
        val requestId = UUID.randomUUID().toString()

        val request = LeaveRequestEntity(
            id = requestId,
            employeeEmail = user.email,
            employeeName = user.fullName,
            department = user.department,
            leaveType = leaveType,
            category = category,
            startDate = startDateStr,
            endDate = endDateStr,
            startDay = startDay,
            endDay = endDay,
            month = month,
            year = year,
            daysCount = count,
            reason = reason.ifBlank { "Demande de $leaveType" },
            attachmentName = attachmentName,
            attachmentUri = attachmentUri,
            status = "PENDING",
            createdAt = System.currentTimeMillis()
        )

        leaveRequestDao.insertRequest(request)
        
        // Push to Cloud Firestore & Supabase for instant real-time synchronization with Admin and other devices
        scope.launch {
            firestoreSync.uploadLeaveRequest(request)
            supabaseSync.uploadLeaveRequest(request)
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

        // Push status change to Firestore and Cloud Relay & Supabase
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
            supabaseSync.updateLeaveRequestStatus(
                requestId = requestId,
                status = "APPROVED",
                decisionAt = now,
                adminComment = adminComment
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

        // 1.5 Schedule reminder for the leave dates
        try {
            val patterns = listOf("dd/MM/yyyy", "yyyy-MM-dd", "d/M/yyyy", "dd-MM-yyyy")
            var startDate: java.util.Date? = null
            for (pattern in patterns) {
                if (startDate == null) {
                    try {
                        val sdf = java.text.SimpleDateFormat(pattern, java.util.Locale.getDefault()).apply { isLenient = false }
                        startDate = sdf.parse(req.startDate.trim())
                    } catch (_: Exception) {}
                }
            }
            if (startDate != null) {
                val cal = java.util.Calendar.getInstance()
                cal.time = startDate
                cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
                cal.set(java.util.Calendar.HOUR_OF_DAY, 9)
                cal.set(java.util.Calendar.MINUTE, 0)
                cal.set(java.util.Calendar.SECOND, 0)
                
                var triggerAt = cal.timeInMillis
                // Si la date calculée (veille à 9h) est déjà passée (ex: congé commence aujourd'hui),
                // on programme le rappel dans 15 secondes pour pouvoir le tester.
                if (triggerAt <= System.currentTimeMillis()) {
                    triggerAt = System.currentTimeMillis() + 15000
                }
                
                LocalNotificationHelper.scheduleLeaveReminder(
                    context = context,
                    requestId = requestId,
                    employeeName = req.employeeName,
                    leaveType = req.leaveType,
                    dates = "${req.startDate} - ${req.endDate}",
                    triggerAtMillis = triggerAt
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

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

        // Push status change to Firestore and Cloud Relay & Supabase
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
            supabaseSync.updateLeaveRequestStatus(
                requestId = requestId,
                status = "REJECTED",
                decisionAt = now,
                adminComment = adminComment
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

    suspend fun deleteLeaveRequest(request: LeaveRequestEntity) {
        leaveRequestDao.deleteRequest(request)
        scope.launch {
            firestoreSync.deleteLeaveRequest(request)
            supabaseSync.deleteLeaveRequest(request.id)
        }
    }


    fun getUnreadAlertsCountFlow(email: String): Flow<Int> {
        return alertDao.getUnreadCountFlow(email)
    }

    suspend fun markAlertRead(alertId: String) {
        alertDao.markAlertRead(alertId)
        scope.launch {
            firestoreSync.markAlertRead(alertId)
            supabaseSync.markAlertRead(alertId)
        }
    }

    suspend fun markAllAlertsRead(email: String) {
        alertDao.markAllAlertsRead(email)
    }

    suspend fun deleteAlert(alertId: String) {
        alertDao.deleteAlertById(alertId)
        scope.launch {
            firestoreSync.deleteAlert(alertId)
            supabaseSync.deleteAlert(alertId)
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
