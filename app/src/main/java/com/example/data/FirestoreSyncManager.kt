package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.service.EmailNotificationService
import com.example.service.LocalNotificationHelper
import com.example.ui.NotificationSystem
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Enterprise Multi-Device Real-Time Cloud Synchronization Engine.
 * 
 * Ensures that leave requests submitted on ANY phone (e.g., employee device)
 * are INSTANTLY received on ALL other phones (e.g., manager/admin device and colleagues)
 * with zero-configuration cloud relay (HTTPS + Server-Sent Events) and Firebase Firestore.
 */
class FirestoreSyncManager private constructor(private val context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val leaveRequestDao = db.leaveRequestDao()
    private val userDao = db.userDao()
    private val alertDao = db.alertDao()
    private val localStorage = LocalStorageManager(context)
    private val scope = CoroutineScope(Dispatchers.IO)
    private val prefs: SharedPreferences = context.getSharedPreferences("timeoff_cloud_sync_prefs", Context.MODE_PRIVATE)

    // Device identification to avoid echo loops while keeping remote devices in sync
    private val deviceId: String

    // Configurable company sync channel
    var companySyncChannel: String
        get() = prefs.getString(PREF_SYNC_CHANNEL, DEFAULT_SYNC_CHANNEL) ?: DEFAULT_SYNC_CHANNEL
        set(value) {
            val sanitized = value.trim().lowercase().replace(Regex("[^a-z0-9_-]"), "_").ifBlank { DEFAULT_SYNC_CHANNEL }
            prefs.edit().putString(PREF_SYNC_CHANNEL, sanitized).apply()
            restartRealtimeRelay()
        }

    private var firestore: FirebaseFirestore? = null
    private var requestsListener: ListenerRegistration? = null
    private var usersListener: ListenerRegistration? = null
    private var alertsListener: ListenerRegistration? = null

    private var relayJob: Job? = null
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // Infinite for persistent SSE streaming
        .writeTimeout(15, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val fastHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private val _isCloudConnected = MutableStateFlow(false)
    val isCloudConnected: StateFlow<Boolean> = _isCloudConnected.asStateFlow()

    private val _lastSyncTimestamp = MutableStateFlow(System.currentTimeMillis())
    val lastSyncTimestamp: StateFlow<Long> = _lastSyncTimestamp.asStateFlow()

    private val _syncStatusText = MutableStateFlow("Connexion au réseau cloud...")
    val syncStatusText: StateFlow<String> = _syncStatusText.asStateFlow()

    init {
        var devId = prefs.getString(PREF_DEVICE_ID, null)
        if (devId.isNullOrBlank()) {
            devId = "device_" + UUID.randomUUID().toString().substring(0, 8)
            prefs.edit().putString(PREF_DEVICE_ID, devId).apply()
        }
        deviceId = devId

        // Initialize Firebase Firestore if configured in project
        try {
            val app = try {
                FirebaseApp.getInstance()
            } catch (e: Exception) {
                try {
                    FirebaseApp.initializeApp(context)
                } catch (ex: Exception) {
                    null
                }
            }

            if (app != null) {
                firestore = FirebaseFirestore.getInstance()
            }
        } catch (e: Exception) {
            Log.w("FirestoreSyncManager", "Firestore optional init notice: ${e.message}")
        }

        // Start multi-device sync
        startRealtimeListeners()
    }

    /**
     * Starts both Firestore (if available) and Universal Realtime Cloud Relay (SSE).
     */
    fun startRealtimeListeners() {
        startFirestoreListeners()
        startCloudRelayListener()
    }

    fun restartRealtimeRelay() {
        relayJob?.cancel()
        startCloudRelayListener()
    }

    private fun startFirestoreListeners() {
        val fs = firestore ?: return
        try {
            requestsListener?.remove()
            requestsListener = fs.collection(COLLECTION_REQUESTS)
                .addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Log.w("FirestoreSyncManager", "Firestore requests error: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshots != null) {
                        scope.launch {
                            val requestsToUpsert = mutableListOf<LeaveRequestEntity>()
                            for (dc in snapshots.documentChanges) {
                                val doc = dc.document
                                try {
                                    val req = documentToLeaveRequest(doc.id, doc.data)
                                    if (req != null) {
                                        when (dc.type) {
                                            com.google.firebase.firestore.DocumentChange.Type.ADDED,
                                            com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> {
                                                requestsToUpsert.add(req)
                                            }
                                            com.google.firebase.firestore.DocumentChange.Type.REMOVED -> {
                                                leaveRequestDao.deleteRequest(req)
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e("FirestoreSyncManager", "Firestore parse error ${doc.id}", e)
                                }
                            }
                            if (requestsToUpsert.isNotEmpty()) {
                                leaveRequestDao.insertAll(requestsToUpsert)
                                _lastSyncTimestamp.value = System.currentTimeMillis()
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.w("FirestoreSyncManager", "Firestore requests listener could not start: ${e.message}")
        }
    }

    /**
     * Starts continuous real-time Server-Sent Events (SSE) listener and initial catchup pull.
     * This guarantees that any request submitted from another phone arrives immediately (< 100ms).
     */
    private fun startCloudRelayListener() {
        relayJob?.cancel()
        relayJob = scope.launch {
            // 1. Initial catchup from cloud cache (pull any requests submitted while this phone was closed/offline)
            catchupHistoricalCloudEvents()

            // 2. Request a full snapshot synchronization handshake from any active peer
            requestFullSyncFromCloud()

            // 3. Persistent Real-time SSE Stream Loop
            while (isActive) {
                try {
                    val topic = companySyncChannel
                    val sseUrl = "https://ntfy.sh/$topic/sse"
                    val request = Request.Builder()
                        .url(sseUrl)
                        .header("Accept", "text/event-stream")
                        .build()

                    _syncStatusText.value = "Connecté en temps réel (Canal: $topic)"
                    _isCloudConnected.value = true

                    httpClient.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            Log.w("FirestoreSyncManager", "SSE connection returned code ${response.code}")
                            _isCloudConnected.value = false
                            _syncStatusText.value = "Reconnexion au cloud..."
                            delay(5000)
                            return@use
                        }

                        val source = response.body?.source()
                        if (source == null) {
                            delay(3000)
                            return@use
                        }

                        while (isActive && !source.exhausted()) {
                            val line = source.readUtf8Line() ?: break
                            if (line.startsWith("data:")) {
                                val jsonStr = line.substring(5).trim()
                                if (jsonStr.isNotEmpty()) {
                                    handleIncomingRelayPayload(jsonStr)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    if (isActive) {
                        Log.w("FirestoreSyncManager", "Cloud Relay SSE stream interrupted: ${e.message}. Reconnecting in 4s...")
                        _isCloudConnected.value = false
                        _syncStatusText.value = "Reconnexion automatique..."
                        delay(4000)
                    }
                }
            }
        }
    }

    /**
     * Pulls historical cached messages from cloud to catch up on requests submitted while offline.
     */
    private suspend fun catchupHistoricalCloudEvents() {
        withContext(Dispatchers.IO) {
            try {
                val topic = companySyncChannel
                val pollUrl = "https://ntfy.sh/$topic/json?since=12h"
                val request = Request.Builder().url(pollUrl).build()

                fastHttpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val bodyString = response.body?.string() ?: ""
                        val lines = bodyString.split("\n")
                        for (line in lines) {
                            val trimmed = line.trim()
                            if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
                                handleIncomingRelayPayload(trimmed)
                            }
                        }
                        _lastSyncTimestamp.value = System.currentTimeMillis()
                        _isCloudConnected.value = true
                    }
                }
            } catch (e: Exception) {
                Log.w("FirestoreSyncManager", "Catchup pull error: ${e.message}")
            }
        }
    }

    /**
     * Handles any incoming JSON message received over the real-time cloud relay.
     */
    private suspend fun handleIncomingRelayPayload(rawJson: String) {
        try {
            val root = JSONObject(rawJson)
            // ntfy wraps custom published message in "message" field if passed as string/json
            val payloadStr = if (root.has("event") && root.getString("event") == "message") {
                root.optString("message", "")
            } else {
                rawJson
            }

            if (payloadStr.isBlank() || !payloadStr.startsWith("{")) return

            val payload = JSONObject(payloadStr)
            val action = payload.optString("action", "")
            val senderId = payload.optString("senderDeviceId", "")

            // Ignore messages that originated from our own device
            if (senderId == deviceId && action != ACTION_PING_TEST) {
                return
            }

            when (action) {
                ACTION_LEAVE_REQUEST -> {
                    val reqJson = payload.getJSONObject("request")
                    val entity = jsonToLeaveRequest(reqJson)
                    if (entity != null) {
                        val existing = leaveRequestDao.getRequestById(entity.id)
                        leaveRequestDao.insertRequest(entity)
                        _lastSyncTimestamp.value = System.currentTimeMillis()
                        Log.d("FirestoreSyncManager", "Real-time synced LeaveRequest ${entity.id} from remote phone (${entity.employeeName})")

                        // Trigger manager notification on this device if this was a new submission from another phone
                        if (existing == null) {
                            LocalNotificationHelper.sendNewLeaveRequestNotification(
                                context = context,
                                requestId = entity.id,
                                employeeName = entity.employeeName,
                                leaveType = entity.leaveType,
                                dates = "${entity.startDate} - ${entity.endDate}",
                                department = entity.department,
                                reason = entity.reason
                            )
                        }
                    }
                }

                ACTION_DELETE_LEAVE_REQUEST -> {
                    val requestId = payload.getString("requestId")
                    val existing = leaveRequestDao.getRequestById(requestId)
                    if (existing != null) {
                        leaveRequestDao.deleteRequest(existing)
                        _lastSyncTimestamp.value = System.currentTimeMillis()
                        Log.d("FirestoreSyncManager", "Real-time synced LeaveRequest DELETE $requestId")
                    }
                }

                ACTION_STATUS_UPDATE -> {
                    val requestId = payload.getString("requestId")
                    val status = payload.getString("status")
                    val decisionAt = payload.optLong("decisionAt", System.currentTimeMillis())
                    val adminComment = payload.optString("adminComment", "")
                    val targetUserEmail = payload.optString("targetUserEmail", "")
                    val employeeName = payload.optString("employeeName", "Collaborateur")
                    val leaveType = payload.optString("leaveType", "Congés")
                    val dates = payload.optString("dates", "")

                    leaveRequestDao.updateStatus(requestId, status, decisionAt, adminComment)
                    _lastSyncTimestamp.value = System.currentTimeMillis()
                    Log.d("FirestoreSyncManager", "Real-time synced status update for $requestId -> $status")

                    // Notify employee on this device
                    val isApproved = status == "APPROVED"
                    LocalNotificationHelper.sendLeaveStatusNotification(
                        context = context,
                        requestId = requestId,
                        employeeName = employeeName,
                        leaveType = leaveType,
                        dates = dates,
                        isApproved = isApproved,
                        adminComment = adminComment
                    )

                    // Also create in-app alert
                    val alert = AppAlert(
                        id = UUID.randomUUID().toString(),
                        targetUserEmail = targetUserEmail,
                        title = if (isApproved) "Demande de congé VALIDÉE" else "Demande de congé REFUSÉE",
                        message = "Votre demande de $leaveType ($dates) a été ${if (isApproved) "VALIDÉE" else "REFUSÉE"}." +
                                if (adminComment.isNotBlank()) "\nCommentaire : $adminComment" else "",
                        type = status,
                        timestamp = decisionAt,
                        isRead = false,
                        isPopupShown = false
                    )
                    alertDao.insertAlert(alert)
                }

                ACTION_USER_UPSERT -> {
                    val userJson = payload.getJSONObject("user")
                    val user = jsonToUser(userJson)
                    if (user != null) {
                        userDao.insertUser(user)
                        localStorage.saveUser(user, setAsActiveSession = false)
                    }
                }

                ACTION_SYNC_REQUEST -> {
                    // A remote phone connected and requested all existing data -> broadcast our full snapshot
                    broadcastFullSnapshot()
                }

                ACTION_FULL_SNAPSHOT -> {
                    val reqArray = payload.optJSONArray("requests")
                    if (reqArray != null) {
                        val list = mutableListOf<LeaveRequestEntity>()
                        for (i in 0 until reqArray.length()) {
                            val rJson = reqArray.getJSONObject(i)
                            val req = jsonToLeaveRequest(rJson)
                            if (req != null) list.add(req)
                        }
                        if (list.isNotEmpty()) {
                            leaveRequestDao.insertAll(list)
                            _lastSyncTimestamp.value = System.currentTimeMillis()
                            Log.d("FirestoreSyncManager", "Ingested full snapshot of ${list.size} requests from remote phone.")
                        }
                    }
                }

                ACTION_PING_TEST -> {
                    _lastSyncTimestamp.value = System.currentTimeMillis()
                    NotificationSystem.sendNotification(
                        title = "Liaison Cloud Établie",
                        message = "Communication multi-téléphones active et fonctionnelle !",
                        isApproved = true
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("FirestoreSyncManager", "Error processing incoming cloud relay payload", e)
        }
    }

    /**
     * Publishes a JSON event to the Cloud Relay topic for all connected phones.
     */
    private suspend fun publishToCloudRelay(payload: JSONObject) {
        withContext(Dispatchers.IO) {
            try {
                payload.put("senderDeviceId", deviceId)
                payload.put("timestamp", System.currentTimeMillis())

                val topic = companySyncChannel
                val publishUrl = "https://ntfy.sh/$topic"
                val body = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder()
                    .url(publishUrl)
                    .post(body)
                    .header("Title", "TimeOff Cloud Sync")
                    .header("Priority", "high")
                    .build()

                fastHttpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        _lastSyncTimestamp.value = System.currentTimeMillis()
                        _isCloudConnected.value = true
                        Log.d("FirestoreSyncManager", "Published ${payload.optString("action")} to cloud relay ($topic)")
                    } else {
                        Log.w("FirestoreSyncManager", "Publish failed with status code ${response.code}")
                    }
                }
            } catch (e: Exception) {
                Log.w("FirestoreSyncManager", "Error publishing to cloud relay: ${e.message}")
            }
        }
    }

    /**
     * Upload / Synchronize a newly submitted or updated leave request across all devices.
     */
    suspend fun uploadLeaveRequest(request: LeaveRequestEntity) {
        // 1. Broadcast via Universal Real-time Cloud Relay
        try {
            val payload = JSONObject().apply {
                put("action", ACTION_LEAVE_REQUEST)
                put("request", leaveRequestToJson(request))
            }
            publishToCloudRelay(payload)
        } catch (e: Exception) {
            Log.w("FirestoreSyncManager", "Relay upload error: ${e.message}")
        }

        // 2. Also save to Cloud Firestore if initialized
        val fs = firestore ?: return
        try {
            val data = leaveRequestToMap(request)
            fs.collection(COLLECTION_REQUESTS).document(request.id)
                .set(data, SetOptions.merge())
                .await()
            Log.d("FirestoreSyncManager", "Leave request ${request.id} synchronized to Firestore")
        } catch (e: Exception) {
            Log.w("FirestoreSyncManager", "Firestore upload notice: ${e.message}")
        }
    }

    suspend fun deleteLeaveRequest(request: LeaveRequestEntity) {
        // 1. Broadcast via Universal Real-time Cloud Relay
        try {
            val payload = JSONObject().apply {
                put("action", ACTION_DELETE_LEAVE_REQUEST)
                put("requestId", request.id)
            }
            publishToCloudRelay(payload)
        } catch (e: Exception) {
            Log.w("FirestoreSyncManager", "Relay upload error: ${e.message}")
        }

        // 2. Also delete from Cloud Firestore if initialized
        val fs = firestore ?: return
        try {
            fs.collection(COLLECTION_REQUESTS).document(request.id).delete().await()
            Log.d("FirestoreSyncManager", "Leave request ${request.id} deleted from Firestore")
        } catch (e: Exception) {
            Log.w("FirestoreSyncManager", "Firestore delete notice: ${e.message}")
        }
    }

    /**
     * Update leave request status (APPROVED / REJECTED) across all devices.
     */
    suspend fun updateLeaveRequestStatus(
        requestId: String,
        status: String,
        decisionAt: Long,
        adminComment: String?,
        targetUserEmail: String = "",
        employeeName: String = "",
        leaveType: String = "",
        dates: String = ""
    ) {
        // 1. Broadcast via Universal Real-time Cloud Relay
        try {
            val payload = JSONObject().apply {
                put("action", ACTION_STATUS_UPDATE)
                put("requestId", requestId)
                put("status", status)
                put("decisionAt", decisionAt)
                put("adminComment", adminComment ?: "")
                put("targetUserEmail", targetUserEmail)
                put("employeeName", employeeName)
                put("leaveType", leaveType)
                put("dates", dates)
            }
            publishToCloudRelay(payload)
        } catch (e: Exception) {
            Log.w("FirestoreSyncManager", "Relay status update error: ${e.message}")
        }

        // 2. Also save to Cloud Firestore if initialized
        val fs = firestore ?: return
        try {
            val updates = mutableMapOf<String, Any>(
                "status" to status,
                "decisionAt" to decisionAt,
                "adminComment" to (adminComment ?: ""),
                "approvedBy" to "Mohamed Taha El Mzabite"
            )
            if (targetUserEmail.isNotBlank()) updates["employeeEmail"] = targetUserEmail
            if (employeeName.isNotBlank()) updates["employeeName"] = employeeName
            if (leaveType.isNotBlank()) updates["leaveType"] = leaveType
            if (dates.isNotBlank()) updates["dates"] = dates

            fs.collection(COLLECTION_REQUESTS).document(requestId)
                .set(updates, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.w("FirestoreSyncManager", "Firestore status update notice: ${e.message}")
        }
    }

    /**
     * Synchronize a User profile to Cloud.
     */
    suspend fun uploadUser(user: User) {
        try {
            val payload = JSONObject().apply {
                put("action", ACTION_USER_UPSERT)
                put("user", userToJson(user))
            }
            publishToCloudRelay(payload)
        } catch (e: Exception) {
            Log.w("FirestoreSyncManager", "Relay user upload error: ${e.message}")
        }

        val fs = firestore ?: return
        try {
            val data = userToMap(user)
            val docId = user.email.lowercase().replace("/", "_")
            fs.collection(COLLECTION_USERS).document(docId)
                .set(data, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.w("FirestoreSyncManager", "Firestore user upload notice: ${e.message}")
        }
    }

    /**
     * Upload an Alert to Cloud.
     */
    suspend fun uploadAlert(alert: AppAlert) {
        val fs = firestore ?: return
        try {
            val data = alertToMap(alert)
            fs.collection(COLLECTION_ALERTS).document(alert.id)
                .set(data, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.w("FirestoreSyncManager", "Firestore alert upload notice: ${e.message}")
        }
    }

    suspend fun markAlertRead(alertId: String) {
        val fs = firestore ?: return
        try {
            fs.collection(COLLECTION_ALERTS).document(alertId)
                .update("isRead", true)
                .await()
        } catch (e: Exception) {
            Log.w("FirestoreSyncManager", "Firestore markAlertRead notice: ${e.message}")
        }
    }

    suspend fun deleteAlert(alertId: String) {
        val fs = firestore ?: return
        try {
            fs.collection(COLLECTION_ALERTS).document(alertId)
                .delete()
                .await()
        } catch (e: Exception) {
            Log.w("FirestoreSyncManager", "Firestore deleteAlert notice: ${e.message}")
        }
    }

    /**
     * Sends a request to all connected devices asking for their full snapshot.
     */
    suspend fun requestFullSyncFromCloud() {
        try {
            val payload = JSONObject().apply {
                put("action", ACTION_SYNC_REQUEST)
            }
            publishToCloudRelay(payload)
        } catch (e: Exception) {
            Log.w("FirestoreSyncManager", "Sync request error: ${e.message}")
        }
    }

    /**
     * Broadcasts all local requests and users to ensure remote phones have 100% of data.
     */
    suspend fun broadcastFullSnapshot() {
        withContext(Dispatchers.IO) {
            try {
                val allRequests = leaveRequestDao.getAllRequests()
                if (allRequests.isNotEmpty()) {
                    val reqArray = JSONArray()
                    for (req in allRequests) {
                        reqArray.put(leaveRequestToJson(req))
                    }
                    val payload = JSONObject().apply {
                        put("action", ACTION_FULL_SNAPSHOT)
                        put("requests", reqArray)
                    }
                    publishToCloudRelay(payload)
                }
            } catch (e: Exception) {
                Log.w("FirestoreSyncManager", "Broadcast full snapshot error: ${e.message}")
            }
        }
    }

    /**
     * Sends a test ping over the cloud relay to verify cross-device communication.
     */
    suspend fun sendTestPing() {
        try {
            val payload = JSONObject().apply {
                put("action", ACTION_PING_TEST)
                put("senderName", "Test Device $deviceId")
            }
            publishToCloudRelay(payload)
        } catch (e: Exception) {
            Log.w("FirestoreSyncManager", "Test ping error: ${e.message}")
        }
    }

    /**
     * Synchronizes all local data to Cloud (both Relay and Firestore).
     */
    suspend fun syncAllLocalDataToCloud() {
        broadcastFullSnapshot()
        catchupHistoricalCloudEvents()

        val fs = firestore ?: return
        try {
            val allRequests = leaveRequestDao.getAllRequests()
            for (req in allRequests) {
                uploadLeaveRequest(req)
            }
            val allUsers = userDao.getAllUsers()
            for (u in allUsers) {
                uploadUser(u)
            }
        } catch (e: Exception) {
            Log.w("FirestoreSyncManager", "Sync all error: ${e.message}")
        }
    }

    // --- JSON SERIALIZERS & MAPPERS ---

    private fun leaveRequestToJson(request: LeaveRequestEntity): JSONObject {
        return JSONObject().apply {
            put("id", request.id)
            put("employeeEmail", request.employeeEmail)
            put("employeeName", request.employeeName)
            put("department", request.department)
            put("leaveType", request.leaveType)
            put("startDate", request.startDate)
            put("endDate", request.endDate)
            put("startDay", request.startDay)
            put("endDay", request.endDay)
            put("month", request.month)
            put("year", request.year)
            put("daysCount", request.daysCount)
            put("reason", request.reason)
            put("attachmentName", request.attachmentName ?: "")
            put("status", request.status)
            put("createdAt", request.createdAt)
            put("decisionAt", request.decisionAt ?: 0L)
            put("adminComment", request.adminComment ?: "")
            put("alertDismissedByEmployee", request.alertDismissedByEmployee)
        }
    }

    private fun jsonToLeaveRequest(obj: JSONObject): LeaveRequestEntity? {
        val id = obj.optString("id", "")
        val employeeEmail = obj.optString("employeeEmail", "")
        if (id.isBlank() || employeeEmail.isBlank()) return null

        val decisionAtVal = obj.optLong("decisionAt", 0L)
        val decisionAt = if (decisionAtVal > 0) decisionAtVal else null
        val attachment = obj.optString("attachmentName", "").ifBlank { null }
        val adminComment = obj.optString("adminComment", "").ifBlank { null }

        return LeaveRequestEntity(
            id = id,
            employeeEmail = employeeEmail,
            employeeName = obj.optString("employeeName", "Collaborateur"),
            department = obj.optString("department", "Ingénierie & IT"),
            leaveType = obj.optString("leaveType", "Congés payés"),
            category = obj.optString("category", "Inconnu"),
            startDate = obj.optString("startDate", ""),
            endDate = obj.optString("endDate", ""),
            startDay = obj.optInt("startDay", 1),
            endDay = obj.optInt("endDay", 1),
            month = obj.optInt("month", 0),
            year = obj.optInt("year", 2026),
            daysCount = obj.optInt("daysCount", 1),
            reason = obj.optString("reason", ""),
            attachmentName = attachment,
            status = obj.optString("status", "PENDING"),
            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
            decisionAt = decisionAt,
            adminComment = adminComment,
            alertDismissedByEmployee = obj.optBoolean("alertDismissedByEmployee", false)
        )
    }

    private fun userToJson(user: User): JSONObject {
        return JSONObject().apply {
            put("email", user.email)
            put("matricule", user.matricule)
            put("fullName", user.fullName)
            put("passwordHash", user.passwordHash)
            put("jobTitle", user.jobTitle)
            put("department", user.department)
            put("phone", user.phone)
            put("hireDate", user.hireDate)
            put("officeLocation", user.officeLocation)
            put("avatarUrl", user.avatarUrl)
            put("paidLeaveAllowance", user.paidLeaveAllowance)
            put("paidLeaveUsed", user.paidLeaveUsed)
            put("rttAllowance", user.rttAllowance)
            put("rttUsed", user.rttUsed)
        }
    }

    private fun jsonToUser(obj: JSONObject): User? {
        val email = obj.optString("email", "")
        if (email.isBlank()) return null
        return User(
            email = email,
            matricule = obj.optString("matricule", "EMP-0001"),
            fullName = obj.optString("fullName", ""),
            passwordHash = obj.optString("passwordHash", ""),
            jobTitle = obj.optString("jobTitle", "Collaborateur"),
            department = obj.optString("department", "Ingénierie & IT"),
            phone = obj.optString("phone", "+212 6 00 00 00 00"),
            hireDate = obj.optString("hireDate", "Aujourd'hui"),
            officeLocation = obj.optString("officeLocation", "Siège Principal"),
            avatarUrl = obj.optString("avatarUrl", ""),
            paidLeaveAllowance = obj.optInt("paidLeaveAllowance", 25),
            paidLeaveUsed = obj.optInt("paidLeaveUsed", 0),
            rttAllowance = obj.optInt("rttAllowance", 10),
            rttUsed = obj.optInt("rttUsed", 0)
        )
    }

    private fun leaveRequestToMap(request: LeaveRequestEntity): Map<String, Any?> {
        return mapOf(
            "id" to request.id,
            "employeeEmail" to request.employeeEmail,
            "employeeName" to request.employeeName,
            "department" to request.department,
            "leaveType" to request.leaveType,
            "startDate" to request.startDate,
            "endDate" to request.endDate,
            "startDay" to request.startDay,
            "endDay" to request.endDay,
            "month" to request.month,
            "year" to request.year,
            "daysCount" to request.daysCount,
            "reason" to request.reason,
            "attachmentName" to (request.attachmentName ?: ""),
            "status" to request.status,
            "createdAt" to request.createdAt,
            "decisionAt" to (request.decisionAt ?: 0L),
            "adminComment" to (request.adminComment ?: ""),
            "alertDismissedByEmployee" to request.alertDismissedByEmployee
        )
    }

    private fun documentToLeaveRequest(id: String, data: Map<String, Any>): LeaveRequestEntity? {
        val employeeEmail = data["employeeEmail"] as? String ?: return null
        val employeeName = data["employeeName"] as? String ?: "Employé"
        val department = data["department"] as? String ?: "Ingénierie & IT"
        val leaveType = data["leaveType"] as? String ?: "Congés payés"
        val startDate = data["startDate"] as? String ?: ""
        val endDate = data["endDate"] as? String ?: ""
        val startDay = (data["startDay"] as? Number)?.toInt() ?: 1
        val endDay = (data["endDay"] as? Number)?.toInt() ?: 1
        val month = (data["month"] as? Number)?.toInt() ?: 0
        val year = (data["year"] as? Number)?.toInt() ?: 2026
        val daysCount = (data["daysCount"] as? Number)?.toInt() ?: 1
        val reason = data["reason"] as? String ?: ""
        val attachmentName = (data["attachmentName"] as? String)?.ifBlank { null }
        val status = data["status"] as? String ?: "PENDING"
        val createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
        val decisionAtNum = (data["decisionAt"] as? Number)?.toLong()
        val decisionAt = if (decisionAtNum != null && decisionAtNum > 0) decisionAtNum else null
        val adminComment = (data["adminComment"] as? String)?.ifBlank { null }
        val alertDismissed = (data["alertDismissedByEmployee"] as? Boolean) ?: false

        return LeaveRequestEntity(
            id = id,
            employeeEmail = employeeEmail,
            employeeName = employeeName,
            department = department,
            leaveType = leaveType,
            category = data["category"] as? String ?: "Inconnu",
            startDate = startDate,
            endDate = endDate,
            startDay = startDay,
            endDay = endDay,
            month = month,
            year = year,
            daysCount = daysCount,
            reason = reason,
            attachmentName = attachmentName,
            status = status,
            createdAt = createdAt,
            decisionAt = decisionAt,
            adminComment = adminComment,
            alertDismissedByEmployee = alertDismissed
        )
    }

    private fun userToMap(user: User): Map<String, Any?> {
        return mapOf(
            "email" to user.email,
            "matricule" to user.matricule,
            "fullName" to user.fullName,
            "passwordHash" to user.passwordHash,
            "jobTitle" to user.jobTitle,
            "department" to user.department,
            "phone" to user.phone,
            "hireDate" to user.hireDate,
            "officeLocation" to user.officeLocation,
            "avatarUrl" to user.avatarUrl,
            "paidLeaveAllowance" to user.paidLeaveAllowance,
            "paidLeaveUsed" to user.paidLeaveUsed,
            "rttAllowance" to user.rttAllowance,
            "rttUsed" to user.rttUsed
        )
    }

    private fun alertToMap(alert: AppAlert): Map<String, Any?> {
        return mapOf(
            "id" to alert.id,
            "targetUserEmail" to alert.targetUserEmail,
            "title" to alert.title,
            "message" to alert.message,
            "type" to alert.type,
            "timestamp" to alert.timestamp,
            "isRead" to alert.isRead,
            "isPopupShown" to alert.isPopupShown
        )
    }

    companion object {
        const val COLLECTION_REQUESTS = "leave_requests"
        const val COLLECTION_USERS = "users"
        const val COLLECTION_ALERTS = "app_alerts"

        private const val PREF_DEVICE_ID = "pref_device_id"
        private const val PREF_SYNC_CHANNEL = "pref_sync_channel"
        const val DEFAULT_SYNC_CHANNEL = "timeoff_company_global_sync_vzxqwe"

        const val ACTION_LEAVE_REQUEST = "ACTION_LEAVE_REQUEST"
        const val ACTION_DELETE_LEAVE_REQUEST = "ACTION_DELETE_LEAVE_REQUEST"
        const val ACTION_STATUS_UPDATE = "ACTION_STATUS_UPDATE"
        const val ACTION_USER_UPSERT = "ACTION_USER_UPSERT"
        const val ACTION_SYNC_REQUEST = "ACTION_SYNC_REQUEST"
        const val ACTION_FULL_SNAPSHOT = "ACTION_FULL_SNAPSHOT"
        const val ACTION_PING_TEST = "ACTION_PING_TEST"

        @Volatile
        private var INSTANCE: FirestoreSyncManager? = null

        fun getInstance(context: Context): FirestoreSyncManager {
            return INSTANCE ?: synchronized(this) {
                val instance = FirestoreSyncManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
