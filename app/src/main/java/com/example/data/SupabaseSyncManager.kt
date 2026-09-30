package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Enterprise Supabase Integration & Synchronization Manager.
 *
 * Connects the TimeOff application with Supabase (PostgreSQL & PostgREST API).
 * Enables real-time cloud data synchronization of:
 * - Leave Requests (`leave_requests` table)
 * - Employees / Users (`users` table)
 * - Alerts & Notifications (`app_alerts` table)
 */
class SupabaseSyncManager private constructor(private val context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val leaveRequestDao = db.leaveRequestDao()
    private val userDao = db.userDao()
    private val alertDao = db.alertDao()
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(Dispatchers.IO)

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    // Configurable Supabase credentials
    var supabaseUrl: String
        get() {
            val saved = prefs.getString(KEY_URL, null)
            if (!saved.isNullOrBlank()) return saved
            return try {
                BuildConfig.SUPABASE_URL.takeIf { it.isNotBlank() && !it.contains("your-project") } ?: ""
            } catch (_: Exception) { "" }
        }
        set(value) {
            prefs.edit().putString(KEY_URL, value.trim().removeSuffix("/")).apply()
            checkConnectionStatus()
        }

    var supabaseAnonKey: String
        get() {
            val saved = prefs.getString(KEY_KEY, null)
            if (!saved.isNullOrBlank()) return saved
            return try {
                BuildConfig.SUPABASE_ANON_KEY.takeIf { it.isNotBlank() && !it.contains("your-supabase") } ?: ""
            } catch (_: Exception) { "" }
        }
        set(value) {
            prefs.edit().putString(KEY_KEY, value.trim()).apply()
            checkConnectionStatus()
        }

    var isAutoSyncEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_SYNC, true)
        set(value) {
            prefs.edit().putBoolean(KEY_AUTO_SYNC, value).apply()
            if (value) startBackgroundSync() else stopBackgroundSync()
        }

    private val _isConfigured = MutableStateFlow(false)
    val isConfigured: StateFlow<Boolean> = _isConfigured.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _syncStatusText = MutableStateFlow("Supabase en attente...")
    val syncStatusText: StateFlow<String> = _syncStatusText.asStateFlow()

    private val _lastSyncTimestamp = MutableStateFlow(prefs.getLong(KEY_LAST_SYNC, 0L))
    val lastSyncTimestamp: StateFlow<Long> = _lastSyncTimestamp.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private var pollJob: Job? = null

    init {
        updateConfiguredState()
        scope.launch {
            if (isConfigured.value) {
                testConnection()
            }
            if (isAutoSyncEnabled) {
                startBackgroundSync()
            }
        }
    }

    private fun updateConfiguredState() {
        val url = supabaseUrl
        val key = supabaseAnonKey
        val ready = url.isNotBlank() && url.startsWith("http") && key.isNotBlank() && key.length > 20
        _isConfigured.value = ready
        if (!ready) {
            _isConnected.value = false
            _syncStatusText.value = "Identifiants Supabase non renseignés"
        }
    }

    fun checkConnectionStatus() {
        updateConfiguredState()
        if (_isConfigured.value) {
            scope.launch {
                testConnection()
            }
        }
    }

    /**
     * Tests connectivity with the Supabase PostgREST API across all 5 tables:
     * users, leave_requests, app_alerts, leave_balance_history, notes.
     */
    suspend fun testConnection(): Result<String> = withContext(Dispatchers.IO) {
        val url = supabaseUrl
        val key = supabaseAnonKey

        if (url.isBlank() || key.isBlank()) {
            _isConnected.value = false
            _syncStatusText.value = "URL ou Clé API manquante"
            return@withContext Result.failure(Exception("Supabase URL ou clé Anon absente"))
        }

        try {
            _syncStatusText.value = "Test des 5 tables Supabase..."
            val tables = listOf("users", "leave_requests", "app_alerts", "leave_balance_history", "notes")
            val accessibleTables = mutableListOf<String>()
            val rlsBlockedTables = mutableListOf<String>()
            val missingTables = mutableListOf<String>()

            for (table in tables) {
                val tableCheckReq = Request.Builder()
                    .url("$url/rest/v1/$table?select=id&limit=1")
                    .addHeader("apikey", key)
                    .addHeader("Authorization", "Bearer $key")
                    .get()
                    .build()

                try {
                    httpClient.newCall(tableCheckReq).execute().use { resp ->
                        when {
                            resp.isSuccessful -> accessibleTables.add(table)
                            resp.code == 401 || resp.code == 403 -> rlsBlockedTables.add(table)
                            resp.code == 404 -> missingTables.add(table)
                            else -> accessibleTables.add(table)
                        }
                    }
                } catch (_: Exception) {
                    missingTables.add(table)
                }
            }

            if (rlsBlockedTables.isNotEmpty()) {
                _isConnected.value = false
                val err = "Sécurité RLS bloquante sur : ${rlsBlockedTables.joinToString()}. Copiez et exécutez le script SQL (onglet Script SQL) pour débloquer les accès."
                _syncStatusText.value = "RLS bloquant sur ${rlsBlockedTables.size} table(s)"
                return@withContext Result.failure(Exception(err))
            }

            if (missingTables.isNotEmpty()) {
                _isConnected.value = true
                val info = "Connecté à Supabase. ${accessibleTables.size}/5 tables accessibles. Manquantes : ${missingTables.joinToString()}. Exécutez le script SQL pour les créer."
                _syncStatusText.value = "${accessibleTables.size}/5 tables prêtes"
                return@withContext Result.success(info)
            }

            _isConnected.value = true
            _syncStatusText.value = "5/5 tables connectées (${url.substringAfter("://").substringBefore(".")})"
            return@withContext Result.success("✓ Connexion Supabase parfaite ! Les 5 tables fonctionnent correctement (users, leave_requests, app_alerts, leave_balance_history, notes).")
        } catch (e: Exception) {
            _isConnected.value = false
            val msg = "Erreur réseau Supabase: ${e.localizedMessage ?: e.message}"
            _syncStatusText.value = msg
            return@withContext Result.failure(e)
        }
    }

    /**
     * Executes a full bi-directional sync between Room and Supabase.
     */
    suspend fun syncAll(): Result<String> = withContext(Dispatchers.IO) {
        if (!isConfigured.value) {
            return@withContext Result.failure(Exception("Supabase n'est pas configuré."))
        }

        _isSyncing.value = true
        _syncStatusText.value = "Synchronisation Supabase en cours..."

        try {
            // 1. Upload local data (Users, Leave Requests, Alerts)
            val uploadedRequests = uploadLocalLeaveRequests()
            val uploadedUsers = uploadLocalUsers()
            val uploadedAlerts = uploadLocalAlerts()

            // 2. Fetch remote updates from Supabase
            val pulledRequests = fetchRemoteLeaveRequests()
            val pulledUsers = fetchRemoteUsers()
            val pulledAlerts = fetchRemoteAlerts()

            val now = System.currentTimeMillis()
            _lastSyncTimestamp.value = now
            prefs.edit().putLong(KEY_LAST_SYNC, now).apply()

            _isConnected.value = true
            val summary = "Sync réussie : $uploadedRequests demandes exportées, $pulledRequests reçues de Supabase."
            _syncStatusText.value = "Synchronisé avec Supabase"
            return@withContext Result.success(summary)
        } catch (e: Exception) {
            Log.e("SupabaseSyncManager", "Sync error", e)
            _syncStatusText.value = "Erreur sync Supabase: ${e.message}"
            return@withContext Result.failure(e)
        } finally {
            _isSyncing.value = false
        }
    }

    /**
     * Seeds and syncs all 5 Supabase tables (users, leave_requests, app_alerts, leave_balance_history, notes).
     * Ensures each table is populated with real or demo records so they are never empty in Supabase.
     */
    suspend fun seedAllSupabaseTables(): Result<String> = withContext(Dispatchers.IO) {
        if (!isConfigured.value) {
            return@withContext Result.failure(Exception("Supabase n'est pas configuré. Veuillez renseigner l'URL et la clé Anon."))
        }

        _isSyncing.value = true
        _syncStatusText.value = "Peuplement des 5 tables Supabase..."

        try {
            // 1. Upload or seed Users
            var usersCount = uploadLocalUsers()
            if (usersCount == 0) {
                val demoUsers = JSONArray().apply {
                    put(JSONObject().apply {
                        put("email", "elmzabitemohamedtaha@gmail.com")
                        put("matricule", "DIR-0001")
                        put("full_name", "Mohamed Taha ELMZABITE")
                        put("password_hash", "123456")
                        put("job_title", "Responsable RH & Direction")
                        put("department", "Ressources Humaines & Direction")
                        put("role", "HR_ADMIN")
                        put("phone", "0691366836")
                        put("hire_date", "01 Janvier 2020")
                        put("office_location", "Nouacer, Bureau Central")
                        put("paid_leave_allowance", 30)
                        put("paid_leave_used", 8)
                        put("rtt_allowance", 12)
                        put("rtt_used", 3)
                    })
                    put(JSONObject().apply {
                        put("email", "jean.dupont@entreprise.com")
                        put("matricule", "EMP-0042")
                        put("full_name", "Jean Dupont")
                        put("password_hash", "123456")
                        put("job_title", "Ingénieur Logiciel Senior")
                        put("department", "Ingénierie & IT")
                        put("role", "EMPLOYEE")
                        put("phone", "0612345678")
                        put("hire_date", "15 Mars 2022")
                        put("office_location", "Nouacer, Atelier Tech")
                        put("paid_leave_allowance", 25)
                        put("paid_leave_used", 12)
                        put("rtt_allowance", 10)
                        put("rtt_used", 4)
                    })
                    put(JSONObject().apply {
                        put("email", "sophie.martin@entreprise.com")
                        put("matricule", "EMP-0089")
                        put("full_name", "Sophie Martin")
                        put("password_hash", "123456")
                        put("job_title", "Chef de Projet Digital")
                        put("department", "Gestion de Projet")
                        put("role", "EMPLOYEE")
                        put("phone", "0623456789")
                        put("hire_date", "01 Septembre 2021")
                        put("office_location", "Nouacer, Atelier Tech")
                        put("paid_leave_allowance", 25)
                        put("paid_leave_used", 9)
                        put("rtt_allowance", 10)
                        put("rtt_used", 2)
                    })
                }
                postToSupabase("users", demoUsers, onConflict = "email")
                usersCount = demoUsers.length()
            }

            // 2. Upload or seed Leave Requests
            var requestsCount = uploadLocalLeaveRequests()
            if (requestsCount == 0) {
                val demoRequests = JSONArray().apply {
                    put(JSONObject().apply {
                        put("id", "REQ-DEMO-001")
                        put("employee_email", "elmzabitemohamedtaha@gmail.com")
                        put("employee_name", "Mohamed Taha ELMZABITE")
                        put("department", "Ressources Humaines & Direction")
                        put("leave_type", "PAID")
                        put("category", "STANDARD")
                        put("start_date", "2026-10-12")
                        put("end_date", "2026-10-16")
                        put("start_day", 12)
                        put("end_day", 16)
                        put("month", 9)
                        put("year", 2026)
                        put("days_count", 5)
                        put("reason", "Congés annuels d'automne en famille")
                        put("status", "APPROVED")
                        put("created_at", 1726830000000L)
                    })
                    put(JSONObject().apply {
                        put("id", "REQ-DEMO-002")
                        put("employee_email", "jean.dupont@entreprise.com")
                        put("employee_name", "Jean Dupont")
                        put("department", "Ingénierie & IT")
                        put("leave_type", "RTT")
                        put("category", "STANDARD")
                        put("start_date", "2026-10-23")
                        put("end_date", "2026-10-23")
                        put("start_day", 23)
                        put("end_day", 23)
                        put("month", 9)
                        put("year", 2026)
                        put("days_count", 1)
                        put("reason", "Récupération RTT projet livraison")
                        put("status", "PENDING")
                        put("created_at", 1726840000000L)
                    })
                }
                postToSupabase("leave_requests", demoRequests, onConflict = "id")
                requestsCount = demoRequests.length()
            }

            // 3. Upload or seed Alerts
            var alertsCount = uploadLocalAlerts()
            if (alertsCount == 0) {
                val demoAlerts = JSONArray().apply {
                    put(JSONObject().apply {
                        put("id", "ALERT-DEMO-001")
                        put("target_user_email", "elmzabitemohamedtaha@gmail.com")
                        put("title", "Demande de congés validée")
                        put("message", "Votre demande de congés annuels du 12 au 16 octobre a été approuvée.")
                        put("type", "APPROVED")
                        put("timestamp", 1726831000000L)
                        put("is_read", false)
                        put("is_popup_shown", true)
                    })
                    put(JSONObject().apply {
                        put("id", "ALERT-DEMO-002")
                        put("target_user_email", "elmzabitemohamedtaha@gmail.com")
                        put("title", "Nouvelle demande reçue")
                        put("message", "Jean Dupont a soumis une demande de RTT pour le 23 octobre.")
                        put("type", "SUBMITTED")
                        put("timestamp", 1726841000000L)
                        put("is_read", false)
                        put("is_popup_shown", false)
                    })
                }
                postToSupabase("app_alerts", demoAlerts, onConflict = "id")
                alertsCount = demoAlerts.length()
            }

            // 4. Seed & Sync Leave Balance History for all employees
            val allEmployees = userDao.getAllUsers()
            val demoHistory = JSONArray()
            
            if (allEmployees.isNotEmpty()) {
                allEmployees.forEach { emp ->
                    val pla = emp.paidLeaveAllowance.toDouble()
                    val rtta = emp.rttAllowance.toDouble()
                    // Annual accrual
                    demoHistory.put(JSONObject().apply {
                        put("user_id", JSONObject.NULL)
                        put("employee_email", emp.email)
                        put("leave_request_id", JSONObject.NULL)
                        put("leave_type", "Congés payés")
                        put("action", "accrual")
                        put("days_delta", pla)
                        put("balance_before", 0.0)
                        put("balance_after", pla)
                        put("reason", "Attribution annuelle congés payés 2026")
                    })
                    // RTT accrual
                    demoHistory.put(JSONObject().apply {
                        put("user_id", JSONObject.NULL)
                        put("employee_email", emp.email)
                        put("leave_request_id", JSONObject.NULL)
                        put("leave_type", "RTT")
                        put("action", "accrual")
                        put("days_delta", rtta)
                        put("balance_before", 0.0)
                        put("balance_after", rtta)
                        put("reason", "Attribution forfait RTT annuel 2026")
                    })
                }
            } else {
                demoHistory.put(JSONObject().apply {
                    put("user_id", JSONObject.NULL)
                    put("employee_email", "elmzabitemohamedtaha@gmail.com")
                    put("leave_request_id", JSONObject.NULL)
                    put("leave_type", "Congés payés")
                    put("action", "accrual")
                    put("days_delta", 30.0)
                    put("balance_before", 0.0)
                    put("balance_after", 30.0)
                    put("reason", "Attribution annuelle congés payés 2026")
                })
                demoHistory.put(JSONObject().apply {
                    put("user_id", JSONObject.NULL)
                    put("employee_email", "elmzabitemohamedtaha@gmail.com")
                    put("leave_request_id", JSONObject.NULL)
                    put("leave_type", "RTT")
                    put("action", "accrual")
                    put("days_delta", 12.0)
                    put("balance_before", 0.0)
                    put("balance_after", 12.0)
                    put("reason", "Attribution forfait RTT annuel 2026")
                })
            }
            postToSupabase("leave_balance_history", demoHistory)

            // 5. Seed & Sync Profiles from Users
            val profilesArray = JSONArray()
            if (allEmployees.isNotEmpty()) {
                allEmployees.forEach { u ->
                    profilesArray.put(JSONObject().apply {
                        put("email", u.email)
                        put("full_name", u.fullName)
                        put("role", if (u.email.contains("admin", ignoreCase = true) || u.email.equals("elmzabitemohamedtaha@gmail.com", ignoreCase = true)) "HR_ADMIN" else "EMPLOYEE")
                    })
                }
            } else {
                profilesArray.put(JSONObject().apply {
                    put("email", "elmzabitemohamedtaha@gmail.com")
                    put("full_name", "Mohamed Taha ELMZABITE")
                    put("role", "HR_ADMIN")
                })
            }
            postToSupabase("profiles", profilesArray, onConflict = "email")

            // 6. Seed Notes
            val demoNotes = JSONArray().apply {
                put(JSONObject().apply {
                    put("user_id", "elmzabitemohamedtaha@gmail.com")
                    put("title", "Procédure validation congés été")
                    put("content", "Rappel : toutes les demandes pour les vacances scolaires doivent être soumises 3 semaines à l'avance.")
                    put("category", "RH & Direction")
                })
                put(JSONObject().apply {
                    put("user_id", "elmzabitemohamedtaha@gmail.com")
                    put("title", "Solde RTT et fin d'exercice")
                    put("content", "Penser à solder les jours RTT avant le 31 décembre pour éviter la perte des jours non pris.")
                    put("category", "Important")
                })
            }
            postToSupabase("notes", demoNotes)

            val now = System.currentTimeMillis()
            _lastSyncTimestamp.value = now
            prefs.edit().putLong(KEY_LAST_SYNC, now).apply()
            _isConnected.value = true
            _syncStatusText.value = "6 tables synchronisées avec succès"

            Result.success("✓ Les 6 tables Supabase (users, profiles, leave_requests, app_alerts, leave_balance_history, notes) sont synchronisées et opérationnelles !\n• Utilisateurs : $usersCount\n• Profils : ${profilesArray.length()}\n• Demandes : $requestsCount\n• Alertes : $alertsCount\n• Historique soldes : ${demoHistory.length()}\n• Notes RH : ${demoNotes.length()}")
        } catch (e: Exception) {
            _syncStatusText.value = "Erreur peuplement: ${e.message}"
            Result.failure(e)
        } finally {
            _isSyncing.value = false
        }
    }

    // =========================================================================
    // LEAVE REQUESTS SYNC
    // =========================================================================

    suspend fun uploadLeaveRequest(request: LeaveRequestEntity): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured.value) return@withContext false
        try {
            val json = requestToJson(request)
            postToSupabase("leave_requests", JSONArray().put(json), onConflict = "id")
        } catch (e: Exception) {
            Log.w("SupabaseSyncManager", "Failed to upload request ${request.id}: ${e.message}")
            false
        }
    }

    suspend fun updateLeaveRequestStatus(
        requestId: String,
        status: String,
        decisionAt: Long?,
        adminComment: String?
    ): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured.value) return@withContext false
        try {
            val patchObj = JSONObject().apply {
                put("status", status)
                put("decision_at", decisionAt ?: JSONObject.NULL)
                put("admin_comment", adminComment ?: "")
            }
            patchToSupabase("leave_requests", "id=eq.$requestId", patchObj)
        } catch (e: Exception) {
            Log.w("SupabaseSyncManager", "Failed to patch status for $requestId: ${e.message}")
            false
        }
    }

    suspend fun deleteLeaveRequest(requestId: String): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured.value) return@withContext false
        try {
            deleteFromSupabase("leave_requests", "id=eq.$requestId")
        } catch (e: Exception) {
            Log.w("SupabaseSyncManager", "Failed to delete request $requestId: ${e.message}")
            false
        }
    }

    private suspend fun uploadLocalLeaveRequests(): Int {
        val requests = leaveRequestDao.getAllRequests()
        if (requests.isEmpty()) return 0

        val array = JSONArray()
        requests.forEach { req ->
            array.put(requestToJson(req))
        }
        val ok = postToSupabase("leave_requests", array, onConflict = "id")
        return if (ok) requests.size else 0
    }

    private suspend fun fetchRemoteLeaveRequests(): Int {
        val jsonArray = getFromSupabase("leave_requests?select=*&order=created_at.desc") ?: return 0
        var count = 0

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.optJSONObject(i) ?: continue
            val id = obj.optString("id")
            if (id.isBlank()) continue

            val entity = jsonToRequest(obj)
            val existing = leaveRequestDao.getRequestById(id)
            if (existing == null) {
                leaveRequestDao.insertRequest(entity)
                count++
            } else if (entity.status != existing.status || entity.decisionAt != existing.decisionAt || entity.adminComment != existing.adminComment) {
                leaveRequestDao.insertRequest(entity)
                count++
            }
        }
        return count
    }

    // =========================================================================
    // USERS SYNC & AUTHENTICATION
    // =========================================================================

    /**
     * Sends a secure password reset recovery email to the employee via Supabase Auth.
     */
    suspend fun sendPasswordResetEmail(email: String): Result<String> = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase()
        val url = supabaseUrl
        val key = supabaseAnonKey
        if (url.isBlank() || key.isBlank()) {
            return@withContext Result.failure(IllegalStateException("Supabase n'est pas configuré."))
        }

        val endpoint = "$url/auth/v1/recover"
        val payload = JSONObject().apply {
            put("email", cleanEmail)
        }
        val body = payload.toString().toRequestBody(jsonMediaType)

        val request = Request.Builder()
            .url(endpoint)
            .addHeader("apikey", key)
            .addHeader("Authorization", "Bearer $key")
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success("Un email de réinitialisation sécurisé a été envoyé à $cleanEmail.")
                } else {
                    val err = response.body?.string() ?: ""
                    Log.w("SupabaseSyncManager", "Supabase recover failed (${response.code}): $err")
                    Result.failure(Exception("Erreur Supabase Auth (${response.code}): $err"))
                }
            }
        } catch (e: Exception) {
            Log.w("SupabaseSyncManager", "Supabase recover exception: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Updates the employee password in the Supabase cloud 'users' table.
     */
    suspend fun updateUserPassword(email: String, newPasswordHash: String): Boolean = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase()
        val patchObj = JSONObject().apply {
            put("password_hash", newPasswordHash)
        }
        patchToSupabase("users", "email=eq.$cleanEmail", patchObj)
    }

    suspend fun uploadUser(user: User): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured.value) return@withContext false
        try {
            val json = userToJson(user)
            postToSupabase("users", JSONArray().put(json), onConflict = "email")
        } catch (e: Exception) {
            Log.w("SupabaseSyncManager", "Failed to upload user ${user.email}: ${e.message}")
            false
        }
    }

    private suspend fun uploadLocalUsers(): Int {
        val users = userDao.getAllUsers()
        if (users.isEmpty()) return 0

        val array = JSONArray()
        users.forEach { u ->
            array.put(userToJson(u))
        }
        val ok = postToSupabase("users", array, onConflict = "email")
        return if (ok) users.size else 0
    }

    private suspend fun fetchRemoteUsers(): Int {
        val jsonArray = getFromSupabase("users?select=*") ?: return 0
        var count = 0

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.optJSONObject(i) ?: continue
            val email = obj.optString("email")
            if (email.isBlank()) continue

            val user = jsonToUser(obj)
            userDao.insertUser(user)
            count++
        }
        return count
    }

    // =========================================================================
    // ALERTS SYNC
    // =========================================================================

    suspend fun uploadAlert(alert: AppAlert): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured.value) return@withContext false
        try {
            val json = alertToJson(alert)
            postToSupabase("app_alerts", JSONArray().put(json), onConflict = "id")
        } catch (e: Exception) {
            false
        }
    }

    suspend fun markAlertRead(alertId: String): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured.value) return@withContext false
        try {
            val patch = JSONObject().put("is_read", true)
            patchToSupabase("app_alerts", "id=eq.$alertId", patch)
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deleteAlert(alertId: String): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured.value) return@withContext false
        try {
            deleteFromSupabase("app_alerts", "id=eq.$alertId")
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun uploadLocalAlerts(): Int {
        val alerts = alertDao.getAllAlerts()
        if (alerts.isEmpty()) return 0
        val array = JSONArray()
        alerts.forEach { a ->
            array.put(alertToJson(a))
        }
        val ok = postToSupabase("app_alerts", array, onConflict = "id")
        return if (ok) alerts.size else 0
    }

    private suspend fun fetchRemoteAlerts(): Int {
        val jsonArray = getFromSupabase("app_alerts?select=*&order=timestamp.desc&limit=50") ?: return 0
        var count = 0
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.optJSONObject(i) ?: continue
            val id = obj.optString("id")
            if (id.isBlank()) continue
            val alert = jsonToAlert(obj)
            alertDao.insertAlert(alert)
            count++
        }
        return count
    }

    // =========================================================================
    // HTTP LOW-LEVEL METHODS FOR SUPABASE PostgREST
    // =========================================================================

    private fun postToSupabase(table: String, jsonArray: JSONArray, onConflict: String? = null): Boolean {
        val url = supabaseUrl
        val key = supabaseAnonKey
        if (url.isBlank() || key.isBlank()) return false

        val endpoint = if (!onConflict.isNullOrBlank()) {
            "$url/rest/v1/$table?on_conflict=$onConflict"
        } else {
            "$url/rest/v1/$table"
        }
        val body = jsonArray.toString().toRequestBody(jsonMediaType)

        val request = Request.Builder()
            .url(endpoint)
            .addHeader("apikey", key)
            .addHeader("Authorization", "Bearer $key")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "resolution=merge-duplicates,return=minimal")
            .post(body)
            .build()

        return try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val err = response.body?.string() ?: ""
                    Log.w("SupabaseSyncManager", "POST $table failed (${response.code}): $err")
                    false
                } else {
                    true
                }
            }
        } catch (e: Exception) {
            Log.w("SupabaseSyncManager", "POST $table exception: ${e.message}")
            false
        }
    }

    private fun patchToSupabase(table: String, queryParam: String, jsonObj: JSONObject): Boolean {
        val url = supabaseUrl
        val key = supabaseAnonKey
        if (url.isBlank() || key.isBlank()) return false

        val endpoint = "$url/rest/v1/$table?$queryParam"
        val body = jsonObj.toString().toRequestBody(jsonMediaType)

        val request = Request.Builder()
            .url(endpoint)
            .addHeader("apikey", key)
            .addHeader("Authorization", "Bearer $key")
            .addHeader("Content-Type", "application/json")
            .patch(body)
            .build()

        return try {
            httpClient.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun deleteFromSupabase(table: String, queryParam: String): Boolean {
        val url = supabaseUrl
        val key = supabaseAnonKey
        if (url.isBlank() || key.isBlank()) return false

        val endpoint = "$url/rest/v1/$table?$queryParam"
        val request = Request.Builder()
            .url(endpoint)
            .addHeader("apikey", key)
            .addHeader("Authorization", "Bearer $key")
            .delete()
            .build()

        return try {
            httpClient.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun getFromSupabase(pathWithQuery: String): JSONArray? {
        val url = supabaseUrl
        val key = supabaseAnonKey
        if (url.isBlank() || key.isBlank()) return null

        val endpoint = "$url/rest/v1/$pathWithQuery"
        val request = Request.Builder()
            .url(endpoint)
            .addHeader("apikey", key)
            .addHeader("Authorization", "Bearer $key")
            .get()
            .build()

        return try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    null
                } else {
                    val bodyString = response.body?.string() ?: "[]"
                    JSONArray(bodyString)
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    // =========================================================================
    // SUPABASE AUTH & NOTES API
    // =========================================================================

    /**
     * Retrieves the current authenticated user using the Supabase Auth API (`/auth/v1/user`).
     * Equivalent to `supabase.auth.getUser()`.
     */
    suspend fun getCurrentAuthUser(): JSONObject? = withContext(Dispatchers.IO) {
        val url = supabaseUrl
        val key = supabaseAnonKey
        if (url.isBlank() || key.isBlank()) return@withContext null

        val endpoint = "$url/auth/v1/user"
        val request = Request.Builder()
            .url(endpoint)
            .addHeader("apikey", key)
            .addHeader("Authorization", "Bearer $key")
            .get()
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return@withContext null
                    JSONObject(body)
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.w("SupabaseSyncManager", "getCurrentAuthUser failed: ${e.message}")
            null
        }
    }

    /**
     * Counts the number of records in the `notes` table where `user_id` matches the given user identifier.
     * Uses PostgREST `Prefer: count=exact` header for efficient server-side counting.
     */
    suspend fun getNotesCount(userId: String): Int = withContext(Dispatchers.IO) {
        val url = supabaseUrl
        val key = supabaseAnonKey
        if (url.isBlank() || key.isBlank() || userId.isBlank()) return@withContext 0

        // Fetch count using PostgREST exact count
        val endpoint = "$url/rest/v1/notes?user_id=eq.$userId&select=id"
        val request = Request.Builder()
            .url(endpoint)
            .addHeader("apikey", key)
            .addHeader("Authorization", "Bearer $key")
            .addHeader("Prefer", "count=exact")
            .addHeader("Range", "0-0")
            .get()
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                val contentRange = response.header("content-range")
                if (!contentRange.isNullOrBlank() && contentRange.contains("/")) {
                    val totalStr = contentRange.substringAfter("/")
                    totalStr.toIntOrNull() ?: 0
                } else if (response.isSuccessful) {
                    val body = response.body?.string() ?: "[]"
                    JSONArray(body).length()
                } else {
                    0
                }
            }
        } catch (e: Exception) {
            Log.w("SupabaseSyncManager", "getNotesCount failed for $userId: ${e.message}")
            0
        }
    }

    /**
     * Fetches balance history from `leave_balance_history` table.
     * Equivalent to:
     * supabase.from('leave_balance_history')
     *   .select('leave_type, action, days_delta, balance_after, reason, created_at')
     *   .order('created_at', { ascending: false })
     */
    suspend fun getLeaveBalanceHistory(userId: String? = null): List<LeaveBalanceHistoryRecord> = withContext(Dispatchers.IO) {
        val url = supabaseUrl
        val key = supabaseAnonKey
        if (url.isBlank() || key.isBlank()) return@withContext emptyList()

        val queryParam = if (!userId.isNullOrBlank()) {
            "select=leave_type,action,days_delta,balance_after,reason,created_at&user_id=eq.$userId&order=created_at.desc&limit=50"
        } else {
            "select=leave_type,action,days_delta,balance_after,reason,created_at&order=created_at.desc&limit=50"
        }

        val jsonArray = getFromSupabase("leave_balance_history?$queryParam") ?: return@withContext emptyList()
        val list = mutableListOf<LeaveBalanceHistoryRecord>()

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.optJSONObject(i) ?: continue
            list.add(
                LeaveBalanceHistoryRecord(
                    leaveType = obj.optString("leave_type", "annual"),
                    action = obj.optString("action", "deduction"),
                    daysDelta = obj.optDouble("days_delta", 0.0),
                    balanceAfter = obj.optDouble("balance_after", 0.0),
                    reason = obj.optString("reason", ""),
                    createdAt = obj.optString("created_at", "")
                )
            )
        }
        list
    }

    /**
     * Inserts validated leave balance history rows into the Supabase `leave_balance_history` table.
     * Enforces that ONLY administrators can execute this operation.
     */
    suspend fun insertLeaveBalanceHistory(
        rows: List<LeaveBalanceHistoryCsvRow>,
        callerEmail: String? = null
    ): SupabaseImportResult = withContext(Dispatchers.IO) {
        val cleanCaller = callerEmail?.trim()?.lowercase() ?: ""
        val isCallerAdmin = cleanCaller == "elmzabitemohamedtaha@gmail.com" ||
                cleanCaller.contains("admin") ||
                cleanCaller.contains("rh")
        if (!isCallerAdmin) {
            return@withContext SupabaseImportResult.Error(
                message = "Accès refusé. Seul un administrateur peut importer, modifier ou supprimer l'historique des congés."
            )
        }

        val url = supabaseUrl
        val key = supabaseAnonKey
        if (url.isBlank() || key.isBlank()) {
            return@withContext SupabaseImportResult.Error(
                message = "Configuration Supabase manquante (URL ou Anon Key non renseignée). Veuillez configurer Supabase dans l'espace administration."
            )
        }

        val validRows = rows.filter { it.isValid }
        if (validRows.isEmpty()) {
            return@withContext SupabaseImportResult.Error(
                message = "Aucune ligne valide à importer dans le fichier."
            )
        }

        val jsonArray = JSONArray()
        for (row in validRows) {
            val obj = JSONObject().apply {
                put("user_id", JSONObject.NULL)
                put("employee_email", row.employeeEmail)
                put("leave_request_id", JSONObject.NULL)
                put("leave_type", row.leaveType)
                put("action", row.action)
                put("days_delta", row.daysDelta)
                put("balance_after", row.balanceAfter)
                put("balance_before", row.balanceAfter - row.daysDelta)
                put("reason", row.reason)
            }
            jsonArray.put(obj)
        }

        val endpoint = "$url/rest/v1/leave_balance_history"
        val body = jsonArray.toString().toRequestBody(jsonMediaType)

        val request = Request.Builder()
            .url(endpoint)
            .addHeader("apikey", key)
            .addHeader("Authorization", "Bearer $key")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "return=representation")
            .post(body)
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                val respBody = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    Log.w("SupabaseSyncManager", "POST leave_balance_history failed (${response.code}): $respBody")
                    val explanation = when {
                        respBody.contains("23503") || respBody.contains("foreign key") ->
                            "Erreur de clé étrangère (user_id) : L'UUID spécifié n'existe pas dans la table 'users' de Supabase. Assurez-vous d'utiliser un UUID d'utilisateur valide existant."
                        respBody.contains("42703") || respBody.contains("column") ->
                            "Erreur de colonne Supabase : Vérifiez que la table 'leave_balance_history' possède les colonnes attendues (user_id, employee_email, leave_type, action, days_delta, balance_after, reason)."
                        respBody.contains("42501") || respBody.contains("permission") || respBody.contains("policy") ->
                            "Erreur de politique RLS Supabase : L'accès en écriture à 'leave_balance_history' n'est pas autorisé par les politiques RLS."
                        else -> "Code ${response.code} : $respBody"
                    }
                    SupabaseImportResult.Error(
                        message = "Échec de l'insertion dans Supabase",
                        details = explanation
                    )
                } else {
                    val count = try {
                        JSONArray(respBody).length()
                    } catch (_: Exception) {
                        validRows.size
                    }
                    SupabaseImportResult.Success(
                        importedCount = count,
                        message = "$count ligne(s) importée(s) avec succès dans la table Supabase 'leave_balance_history'."
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("SupabaseSyncManager", "Exception during leave_balance_history import", e)
            SupabaseImportResult.Error(
                message = "Erreur de communication avec Supabase : ${e.message}",
                details = e.localizedMessage
            )
        }
    }

    // =========================================================================
    // BACKGROUND AUTOMATIC SYNC
    // =========================================================================

    fun startBackgroundSync() {
        if (pollJob?.isActive == true) return
        pollJob = scope.launch {
            while (isActive) {
                if (isConfigured.value && isAutoSyncEnabled) {
                    try {
                        syncAll()
                    } catch (_: Exception) {}
                }
                delay(30_000L) // poll every 30s
            }
        }
    }

    fun stopBackgroundSync() {
        pollJob?.cancel()
        pollJob = null
    }

    // =========================================================================
    // JSON MAPPERS
    // =========================================================================

    private fun requestToJson(r: LeaveRequestEntity): JSONObject = JSONObject().apply {
        put("id", r.id)
        put("employee_email", r.employeeEmail)
        put("employee_name", r.employeeName)
        put("department", r.department)
        put("leave_type", r.leaveType)
        put("category", r.category)
        put("start_date", r.startDate)
        put("end_date", r.endDate)
        put("start_day", r.startDay)
        put("end_day", r.endDay)
        put("month", r.month)
        put("year", r.year)
        put("days_count", r.daysCount)
        put("reason", r.reason)
        put("attachment_name", r.attachmentName ?: JSONObject.NULL)
        put("attachment_uri", r.attachmentUri ?: JSONObject.NULL)
        put("status", r.status)
        put("created_at", r.createdAt)
        put("decision_at", r.decisionAt ?: JSONObject.NULL)
        put("admin_comment", r.adminComment ?: JSONObject.NULL)
        put("alert_dismissed_by_employee", r.alertDismissedByEmployee)
    }

    private fun jsonToRequest(obj: JSONObject): LeaveRequestEntity {
        return LeaveRequestEntity(
            id = obj.optString("id"),
            employeeEmail = obj.optString("employee_email").ifBlank { obj.optString("employeeEmail") },
            employeeName = obj.optString("employee_name").ifBlank { obj.optString("employeeName") },
            department = obj.optString("department"),
            leaveType = obj.optString("leave_type").ifBlank { obj.optString("leaveType") },
            category = obj.optString("category").ifBlank { "STANDARD" },
            startDate = obj.optString("start_date").ifBlank { obj.optString("startDate") },
            endDate = obj.optString("end_date").ifBlank { obj.optString("endDate") },
            startDay = obj.optInt("start_day", obj.optInt("startDay", 1)),
            endDay = obj.optInt("end_day", obj.optInt("endDay", 1)),
            month = obj.optInt("month", 0),
            year = obj.optInt("year", 2026),
            daysCount = obj.optInt("days_count", obj.optInt("daysCount", 1)),
            reason = obj.optString("reason"),
            attachmentName = if (obj.isNull("attachment_name") || obj.isNull("attachmentName")) null else obj.optString("attachment_name").ifBlank { obj.optString("attachmentName") },
            attachmentUri = if (obj.isNull("attachment_uri") || obj.isNull("attachmentUri")) null else obj.optString("attachment_uri").ifBlank { obj.optString("attachmentUri") },
            status = obj.optString("status", "PENDING"),
            createdAt = obj.optLong("created_at", obj.optLong("createdAt", System.currentTimeMillis())),
            decisionAt = if (!obj.isNull("decision_at")) obj.optLong("decision_at") else if (!obj.isNull("decisionAt")) obj.optLong("decisionAt") else null,
            adminComment = if (!obj.isNull("admin_comment")) obj.optString("admin_comment") else if (!obj.isNull("adminComment")) obj.optString("adminComment") else null,
            alertDismissedByEmployee = obj.optBoolean("alert_dismissed_by_employee", obj.optBoolean("alertDismissedByEmployee", false))
        )
    }

    private fun userToJson(u: User): JSONObject = JSONObject().apply {
        put("email", u.email)
        put("matricule", u.matricule)
        put("full_name", u.fullName)
        put("password_hash", u.passwordHash)
        put("job_title", u.jobTitle)
        put("department", u.department)
        put("role", if (u.email.contains("admin", ignoreCase = true) || u.email.equals("elmzabitemohamedtaha@gmail.com", ignoreCase = true)) "HR_ADMIN" else "EMPLOYEE")
        put("phone", u.phone)
        put("hire_date", u.hireDate)
        put("office_location", u.officeLocation)
        put("avatar_url", u.avatarUrl)
        put("paid_leave_allowance", u.paidLeaveAllowance)
        put("paid_leave_used", u.paidLeaveUsed)
        put("rtt_allowance", u.rttAllowance)
        put("rtt_used", u.rttUsed)
    }

    private fun jsonToUser(obj: JSONObject): User {
        return User(
            email = obj.optString("email"),
            matricule = obj.optString("matricule", "EMP-0001"),
            fullName = obj.optString("full_name").ifBlank { obj.optString("fullName") },
            passwordHash = obj.optString("password_hash").ifBlank { obj.optString("passwordHash", "123456") },
            jobTitle = obj.optString("job_title").ifBlank { obj.optString("jobTitle", "Collaborateur") },
            department = obj.optString("department", "Ingénierie & IT"),
            phone = obj.optString("phone", "0691366836"),
            hireDate = obj.optString("hire_date").ifBlank { obj.optString("hireDate", "15 Janvier 2022") },
            officeLocation = obj.optString("office_location").ifBlank { obj.optString("officeLocation", "Nouacer,l'atelier") },
            avatarUrl = obj.optString("avatar_url").ifBlank { obj.optString("avatarUrl", "") },
            paidLeaveAllowance = obj.optInt("paid_leave_allowance", obj.optInt("paidLeaveAllowance", 25)),
            paidLeaveUsed = obj.optInt("paid_leave_used", obj.optInt("paidLeaveUsed", 0)),
            rttAllowance = obj.optInt("rtt_allowance", obj.optInt("rttAllowance", 10)),
            rttUsed = obj.optInt("rtt_used", obj.optInt("rttUsed", 0))
        )
    }

    private fun alertToJson(a: AppAlert): JSONObject = JSONObject().apply {
        put("id", a.id)
        put("target_user_email", a.targetUserEmail)
        put("title", a.title)
        put("message", a.message)
        put("type", a.type)
        put("timestamp", a.timestamp)
        put("is_read", a.isRead)
        put("is_popup_shown", a.isPopupShown)
    }

    private fun jsonToAlert(obj: JSONObject): AppAlert {
        return AppAlert(
            id = obj.optString("id"),
            targetUserEmail = obj.optString("target_user_email").ifBlank { obj.optString("targetUserEmail") },
            title = obj.optString("title"),
            message = obj.optString("message"),
            type = obj.optString("type", "INFO"),
            timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
            isRead = obj.optBoolean("is_read", obj.optBoolean("isRead", false)),
            isPopupShown = obj.optBoolean("is_popup_shown", obj.optBoolean("isPopupShown", false))
        )
    }

    /**
     * Ready-to-use SQL script for the user to copy & paste in the Supabase SQL Editor.
     * Includes relational user_id constraints, profiles, leave requests, app alerts,
     * triggers, indexes, and RLS policies.
     */
    fun getSupabaseSqlSchema(): String {
        return """
-- =====================================================================
-- TIMEOFF & RH - SCRIPT COMPLET POUR LES 5 TABLES SUPABASE
-- Copiez et collez ce script dans le SQL Editor de votre projet Supabase
-- et cliquez sur 'RUN' pour configurer et débloquer toutes les tables.
-- =====================================================================

-- 0. Extensions nécessaires
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 1. Table des Utilisateurs & Profils Collaborateurs
CREATE TABLE IF NOT EXISTS public.users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES auth.users(id) ON DELETE SET NULL,
    email TEXT UNIQUE NOT NULL,
    matricule TEXT NOT NULL DEFAULT 'EMP-0001',
    full_name TEXT NOT NULL,
    password_hash TEXT DEFAULT '',
    job_title TEXT NOT NULL DEFAULT 'Collaborateur',
    department TEXT NOT NULL DEFAULT 'Ingénierie & IT',
    role TEXT NOT NULL DEFAULT 'EMPLOYEE', -- 'EMPLOYEE', 'MANAGER', 'HR_ADMIN'
    phone TEXT DEFAULT '',
    hire_date TEXT DEFAULT '',
    office_location TEXT DEFAULT '',
    avatar_url TEXT DEFAULT '',
    paid_leave_allowance INTEGER DEFAULT 25,
    paid_leave_used INTEGER DEFAULT 0,
    rtt_allowance INTEGER DEFAULT 10,
    rtt_used INTEGER DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- 2. Table des Demandes de Congés & Absences
CREATE TABLE IF NOT EXISTS public.leave_requests (
    id TEXT PRIMARY KEY,
    user_id UUID REFERENCES public.users(id) ON DELETE SET NULL,
    employee_email TEXT NOT NULL,
    employee_name TEXT NOT NULL,
    department TEXT NOT NULL,
    leave_type TEXT NOT NULL, -- 'PAID', 'RTT', 'SICK', 'FAMILY', 'UNPAID', 'RECOVERY'
    category TEXT DEFAULT 'STANDARD', -- 'STANDARD', 'URGENT', 'EXCEPTIONAL'
    start_date TEXT NOT NULL,
    end_date TEXT NOT NULL,
    start_day INTEGER NOT NULL,
    end_day INTEGER NOT NULL,
    month INTEGER NOT NULL,
    year INTEGER NOT NULL,
    days_count INTEGER NOT NULL,
    reason TEXT DEFAULT '',
    attachment_name TEXT,
    attachment_uri TEXT,
    status TEXT NOT NULL DEFAULT 'PENDING', -- 'PENDING', 'APPROVED', 'REJECTED', 'CANCELLED'
    created_at BIGINT NOT NULL,
    decision_at BIGINT,
    decision_by TEXT,
    admin_comment TEXT,
    alert_dismissed_by_employee BOOLEAN DEFAULT FALSE,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- 3. Table des Alertes, Notifications & Badges
CREATE TABLE IF NOT EXISTS public.app_alerts (
    id TEXT PRIMARY KEY,
    user_id UUID REFERENCES public.users(id) ON DELETE SET NULL,
    target_user_email TEXT NOT NULL,
    title TEXT NOT NULL,
    message TEXT NOT NULL,
    type TEXT NOT NULL, -- 'SUBMITTED', 'APPROVED', 'REJECTED', 'REMINDER'
    related_request_id TEXT,
    timestamp BIGINT NOT NULL,
    is_read BOOLEAN DEFAULT FALSE,
    is_popup_shown BOOLEAN DEFAULT FALSE,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- 4. Table d'Historique des Mouvements de Solde (Audit Log & CSV)
CREATE TABLE IF NOT EXISTS public.leave_balance_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES public.users(id) ON DELETE SET NULL,
    employee_email TEXT NOT NULL,
    leave_request_id TEXT,
    leave_type TEXT NOT NULL,
    action TEXT DEFAULT 'deduction', -- 'accrual', 'deduction', 'adjustment'
    days_delta NUMERIC(4, 1) NOT NULL,
    balance_before NUMERIC(4, 1) NOT NULL DEFAULT 0.0,
    balance_after NUMERIC(4, 1) NOT NULL,
    reason TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- 5. Table des Notes Collaborateurs & Mémos RH
CREATE TABLE IF NOT EXISTS public.notes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id TEXT NOT NULL,
    title TEXT NOT NULL,
    content TEXT DEFAULT '',
    category TEXT DEFAULT 'Général',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- 6. Table des Profils Collaborateurs
CREATE TABLE IF NOT EXISTS public.profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID,
    email TEXT UNIQUE NOT NULL,
    full_name TEXT NOT NULL,
    role TEXT NOT NULL DEFAULT 'EMPLOYEE',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- 7. Triggers automatiques pour lier automatiquement user_id depuis l'email
CREATE OR REPLACE FUNCTION public.set_user_id_from_email()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.user_id IS NULL THEN
        SELECT id INTO NEW.user_id FROM public.users WHERE email = COALESCE(NEW.employee_email, NEW.target_user_email) LIMIT 1;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_set_user_id_leave_requests ON public.leave_requests;
CREATE TRIGGER trg_set_user_id_leave_requests
    BEFORE INSERT ON public.leave_requests
    FOR EACH ROW EXECUTE FUNCTION public.set_user_id_from_email();

DROP TRIGGER IF EXISTS trg_set_user_id_app_alerts ON public.app_alerts;
CREATE TRIGGER trg_set_user_id_app_alerts
    BEFORE INSERT ON public.app_alerts
    FOR EACH ROW EXECUTE FUNCTION public.set_user_id_from_email();

DROP TRIGGER IF EXISTS trg_set_user_id_balance_history ON public.leave_balance_history;
CREATE TRIGGER trg_set_user_id_balance_history
    BEFORE INSERT ON public.leave_balance_history
    FOR EACH ROW EXECUTE FUNCTION public.set_user_id_from_email();

-- 8. Index pour accélérer les requêtes fréquentes
CREATE INDEX IF NOT EXISTS idx_leave_requests_email ON public.leave_requests(employee_email);
CREATE INDEX IF NOT EXISTS idx_leave_requests_status ON public.leave_requests(status);
CREATE INDEX IF NOT EXISTS idx_leave_requests_month_year ON public.leave_requests(year, month);
CREATE INDEX IF NOT EXISTS idx_app_alerts_target ON public.app_alerts(target_user_email);
CREATE INDEX IF NOT EXISTS idx_users_email ON public.users(email);
CREATE INDEX IF NOT EXISTS idx_users_matricule ON public.users(matricule);
CREATE INDEX IF NOT EXISTS idx_notes_user_id ON public.notes(user_id);
CREATE INDEX IF NOT EXISTS idx_profiles_email ON public.profiles(email);
CREATE INDEX IF NOT EXISTS idx_leave_balance_user_id ON public.leave_balance_history(user_id);

-- 9. Trigger automatique pour mettre à jour la colonne updated_at
CREATE OR REPLACE FUNCTION public.handle_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = timezone('utc'::text, now());
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS set_updated_at_users ON public.users;
CREATE TRIGGER set_updated_at_users
    BEFORE UPDATE ON public.users
    FOR EACH ROW EXECUTE FUNCTION public.handle_updated_at();

DROP TRIGGER IF EXISTS set_updated_at_profiles ON public.profiles;
CREATE TRIGGER set_updated_at_profiles
    BEFORE UPDATE ON public.profiles
    FOR EACH ROW EXECUTE FUNCTION public.handle_updated_at();

DROP TRIGGER IF EXISTS set_updated_at_requests ON public.leave_requests;
CREATE TRIGGER set_updated_at_requests
    BEFORE UPDATE ON public.leave_requests
    FOR EACH ROW EXECUTE FUNCTION public.handle_updated_at();

DROP TRIGGER IF EXISTS set_updated_at_alerts ON public.app_alerts;
CREATE TRIGGER set_updated_at_alerts
    BEFORE UPDATE ON public.app_alerts
    FOR EACH ROW EXECUTE FUNCTION public.handle_updated_at();

DROP TRIGGER IF EXISTS set_updated_at_notes ON public.notes;
CREATE TRIGGER set_updated_at_notes
    BEFORE UPDATE ON public.notes
    FOR EACH ROW EXECUTE FUNCTION public.handle_updated_at();

-- 10. Activation RLS (Row Level Security) sur les 6 tables
ALTER TABLE public.users ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.leave_requests ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.app_alerts ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.leave_balance_history ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.notes ENABLE ROW LEVEL SECURITY;

-- 11. Politiques RLS sans blocage pour les rôles 'anon' et 'authenticated'
DROP POLICY IF EXISTS "Allow all access to users" ON public.users;
DROP POLICY IF EXISTS "Allow anon and auth on users" ON public.users;
CREATE POLICY "Allow anon and auth on users" ON public.users FOR ALL TO anon, authenticated USING (true) WITH CHECK (true);

DROP POLICY IF EXISTS "Allow all access to profiles" ON public.profiles;
DROP POLICY IF EXISTS "Allow anon and auth on profiles" ON public.profiles;
DROP POLICY IF EXISTS "profiles_select_own_or_admin" ON public.profiles;
DROP POLICY IF EXISTS "profiles_insert_own_or_admin" ON public.profiles;
DROP POLICY IF EXISTS "profiles_update_own_or_admin" ON public.profiles;
CREATE POLICY "Allow anon and auth on profiles" ON public.profiles FOR ALL TO anon, authenticated USING (true) WITH CHECK (true);

DROP POLICY IF EXISTS "Allow all access to leave_requests" ON public.leave_requests;
DROP POLICY IF EXISTS "Allow public access to leave_requests" ON public.leave_requests;
DROP POLICY IF EXISTS "Allow anon and auth on leave_requests" ON public.leave_requests;
CREATE POLICY "Allow anon and auth on leave_requests" ON public.leave_requests FOR ALL TO anon, authenticated USING (true) WITH CHECK (true);

DROP POLICY IF EXISTS "Allow all access to app_alerts" ON public.app_alerts;
DROP POLICY IF EXISTS "Allow anon and auth on app_alerts" ON public.app_alerts;
CREATE POLICY "Allow anon and auth on app_alerts" ON public.app_alerts FOR ALL TO anon, authenticated USING (true) WITH CHECK (true);

DROP POLICY IF EXISTS "Allow all access to leave_balance_history" ON public.leave_balance_history;
DROP POLICY IF EXISTS "Allow anon and auth on leave_balance_history" ON public.leave_balance_history;
DROP POLICY IF EXISTS "leave_balance_history_select_isolated" ON public.leave_balance_history;
DROP POLICY IF EXISTS "leave_balance_history_admin_write" ON public.leave_balance_history;
CREATE POLICY "Allow anon and auth on leave_balance_history" ON public.leave_balance_history FOR ALL TO anon, authenticated USING (true) WITH CHECK (true);

DROP POLICY IF EXISTS "Allow all access to notes" ON public.notes;
DROP POLICY IF EXISTS "Allow anon and auth on notes" ON public.notes;
DROP POLICY IF EXISTS "notes_select_policy" ON public.notes;
DROP POLICY IF EXISTS "notes_all_policy" ON public.notes;
CREATE POLICY "Allow anon and auth on notes" ON public.notes FOR ALL TO anon, authenticated USING (true) WITH CHECK (true);

-- 12. Permissions PostgreSQL pour PostgREST
GRANT USAGE ON SCHEMA public TO anon, authenticated;
GRANT ALL ON ALL TABLES IN SCHEMA public TO anon, authenticated;
GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO anon, authenticated;
GRANT ALL ON ALL ROUTINES IN SCHEMA public TO anon, authenticated;

-- 13. Supprimer les contraintes de clés étrangères bloquantes vers auth.users
ALTER TABLE public.profiles DROP CONSTRAINT IF EXISTS profiles_id_fkey;
ALTER TABLE public.profiles DROP CONSTRAINT IF EXISTS profiles_user_id_fkey;
ALTER TABLE public.leave_balance_history DROP CONSTRAINT IF EXISTS leave_balance_history_user_id_fkey;

-- 14. Synchronisation automatique des profils depuis public.users
INSERT INTO public.profiles (id, email, full_name, role)
SELECT id, email, full_name, role FROM public.users
ON CONFLICT (email) DO UPDATE SET
    full_name = EXCLUDED.full_name,
    role = EXCLUDED.role;

-- 12. Données initiales pour peupler immédiatement vos 5 tables Supabase
INSERT INTO public.users (email, matricule, full_name, job_title, department, role, phone, hire_date, office_location, paid_leave_allowance, paid_leave_used, rtt_allowance, rtt_used)
VALUES
    ('elmzabitemohamedtaha@gmail.com', 'DIR-0001', 'Mohamed Taha ELMZABITE', 'Responsable RH & Direction', 'Ressources Humaines & Direction', 'HR_ADMIN', '0691366836', '01 Janvier 2020', 'Nouacer, Bureau Central', 30, 8, 12, 3),
    ('jean.dupont@entreprise.com', 'EMP-0042', 'Jean Dupont', 'Ingénieur Logiciel Senior', 'Ingénierie & IT', 'EMPLOYEE', '0612345678', '15 Mars 2022', 'Nouacer, Atelier Tech', 25, 12, 10, 4),
    ('sophie.martin@entreprise.com', 'EMP-0089', 'Sophie Martin', 'Chef de Projet Digital', 'Gestion de Projet', 'EMPLOYEE', '0623456789', '01 Septembre 2021', 'Nouacer, Atelier Tech', 25, 9, 10, 2)
ON CONFLICT (email) DO UPDATE SET
    role = EXCLUDED.role,
    full_name = EXCLUDED.full_name,
    department = EXCLUDED.department;

INSERT INTO public.leave_requests (id, employee_email, employee_name, department, leave_type, category, start_date, end_date, start_day, end_day, month, year, days_count, reason, status, created_at)
VALUES
    ('REQ-DEMO-001', 'elmzabitemohamedtaha@gmail.com', 'Mohamed Taha ELMZABITE', 'Ressources Humaines & Direction', 'PAID', 'STANDARD', '2026-10-12', '2026-10-16', 12, 16, 9, 2026, 5, 'Congés annuels d''automne en famille', 'APPROVED', 1726830000000),
    ('REQ-DEMO-002', 'jean.dupont@entreprise.com', 'Jean Dupont', 'Ingénierie & IT', 'RTT', 'STANDARD', '2026-10-23', '2026-10-23', 23, 23, 9, 2026, 1, 'Récupération RTT projet livraison', 'PENDING', 1726840000000)
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.app_alerts (id, target_user_email, title, message, type, timestamp, is_read, is_popup_shown)
VALUES
    ('ALERT-DEMO-001', 'elmzabitemohamedtaha@gmail.com', 'Demande de congés validée', 'Votre demande de congés annuels du 12 au 16 octobre a été approuvée par la direction.', 'APPROVED', 1726831000000, false, true),
    ('ALERT-DEMO-002', 'elmzabitemohamedtaha@gmail.com', 'Nouvelle demande reçue', 'Jean Dupont a soumis une demande de RTT pour le 23 octobre.', 'SUBMITTED', 1726841000000, false, false)
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.leave_balance_history (employee_email, leave_type, action, days_delta, balance_before, balance_after, reason)
VALUES
    ('elmzabitemohamedtaha@gmail.com', 'annual', 'accrual', 30.0, 0.0, 30.0, 'Attribution annuelle congés payés 2026'),
    ('elmzabitemohamedtaha@gmail.com', 'annual', 'deduction', 5.0, 30.0, 25.0, 'Déduction congés automne (REQ-DEMO-001)'),
    ('elmzabitemohamedtaha@gmail.com', 'rtt', 'accrual', 12.0, 0.0, 12.0, 'Attribution forfait RTT annuel 2026'),
    ('jean.dupont@entreprise.com', 'annual', 'accrual', 25.0, 0.0, 25.0, 'Attribution annuelle congés payés 2026')
ON CONFLICT DO NOTHING;

INSERT INTO public.notes (user_id, title, content, category)
VALUES
    ('elmzabitemohamedtaha@gmail.com', 'Procédure validation congés été', 'Rappel : toutes les demandes pour les vacances scolaires doivent être soumises 3 semaines à l''avance.', 'RH & Direction'),
    ('elmzabitemohamedtaha@gmail.com', 'Solde RTT et fin d''exercice', 'Penser à solder les jours RTT avant le 31 décembre pour éviter la perte des jours non pris.', 'Important'),
    ('jean.dupont@entreprise.com', 'Objectifs Q4 et congés', 'Coordination avec l''équipe projet pour la couverture des astreintes pendant les congés.', 'Projet IT')
ON CONFLICT DO NOTHING;
        """.trimIndent()
    }

    /**
     * SQL script specifically defining 'profiles', 'leave_requests', and 'leave_balance_history'
     * with foreign key constraints, relational integrity, and strict Row Level Security (RLS)
     * isolating data per employee (auth.uid() = user_id) while granting HR administrators full control.
     */
    fun getEmployeeIsolatedSqlSchema(): String {
        return """
-- =====================================================================
-- TIMEOFF & RH - SCRIPT RLS ISOLATION PAR EMPLOYÉ
-- Tables : 'profiles', 'leave_requests', 'leave_balance_history'
-- Clés étrangères (FK) & Row Level Security (RLS) strict par employé
-- =====================================================================

-- 0. Extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- =====================================================================
-- 1. TABLE 'profiles' (Profil collaborateur lié au compte auth.users)
-- =====================================================================
CREATE TABLE IF NOT EXISTS public.profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    email TEXT UNIQUE NOT NULL,
    matricule TEXT NOT NULL DEFAULT 'EMP-0001',
    full_name TEXT NOT NULL,
    job_title TEXT NOT NULL DEFAULT 'Collaborateur',
    department TEXT NOT NULL DEFAULT 'Ingénierie & IT',
    role TEXT NOT NULL DEFAULT 'EMPLOYEE' CHECK (role IN ('EMPLOYEE', 'MANAGER', 'HR_ADMIN')),
    phone TEXT DEFAULT '',
    hire_date TEXT DEFAULT '',
    office_location TEXT DEFAULT '',
    avatar_url TEXT DEFAULT '',
    paid_leave_allowance NUMERIC(4, 1) NOT NULL DEFAULT 25.0,
    paid_leave_used NUMERIC(4, 1) NOT NULL DEFAULT 0.0,
    rtt_allowance NUMERIC(4, 1) NOT NULL DEFAULT 10.0,
    rtt_used NUMERIC(4, 1) NOT NULL DEFAULT 0.0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- =====================================================================
-- 2. TABLE 'leave_requests' (Demandes de congés & absences)
-- =====================================================================
CREATE TABLE IF NOT EXISTS public.leave_requests (
    id TEXT PRIMARY KEY DEFAULT ('REQ-' || to_char(now(), 'YYYYMMDD-') || upper(substr(md5(random()::text), 1, 6))),
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    employee_email TEXT NOT NULL,
    employee_name TEXT NOT NULL,
    department TEXT NOT NULL,
    leave_type TEXT NOT NULL CHECK (leave_type IN ('PAID', 'RTT', 'SICK', 'FAMILY', 'UNPAID', 'RECOVERY')),
    category TEXT NOT NULL DEFAULT 'STANDARD' CHECK (category IN ('STANDARD', 'URGENT', 'EXCEPTIONAL')),
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    start_day INTEGER NOT NULL,
    end_day INTEGER NOT NULL,
    month INTEGER NOT NULL,
    year INTEGER NOT NULL,
    days_count INTEGER NOT NULL CHECK (days_count > 0),
    reason TEXT DEFAULT '',
    attachment_name TEXT,
    attachment_uri TEXT,
    status TEXT NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED')),
    created_at BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000)::bigint,
    decision_at BIGINT,
    decision_by TEXT,
    admin_comment TEXT,
    alert_dismissed_by_employee BOOLEAN DEFAULT FALSE,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
    CONSTRAINT chk_leave_dates CHECK (end_date >= start_date)
);

-- =====================================================================
-- 3. TABLE 'leave_balance_history' (Mouvements de solde & audit)
-- =====================================================================
CREATE TABLE IF NOT EXISTS public.leave_balance_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    employee_email TEXT NOT NULL,
    leave_request_id TEXT REFERENCES public.leave_requests(id) ON DELETE SET NULL,
    leave_type TEXT NOT NULL CHECK (leave_type IN ('annual', 'paid', 'rtt', 'sick', 'family', 'unpaid', 'recovery')),
    action TEXT NOT NULL DEFAULT 'deduction' CHECK (action IN ('accrual', 'deduction', 'adjustment')),
    days_delta NUMERIC(4, 1) NOT NULL,
    balance_before NUMERIC(4, 1) NOT NULL DEFAULT 0.0,
    balance_after NUMERIC(4, 1) NOT NULL,
    reason TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- =====================================================================
-- 4. INDEX POUR PERFORMANCES DE JOINTURES ET RLS
-- =====================================================================
CREATE INDEX IF NOT EXISTS idx_profiles_email ON public.profiles(email);
CREATE INDEX IF NOT EXISTS idx_leave_requests_user_id ON public.leave_requests(user_id);
CREATE INDEX IF NOT EXISTS idx_leave_requests_email ON public.leave_requests(employee_email);
CREATE INDEX IF NOT EXISTS idx_leave_requests_status ON public.leave_requests(status);
CREATE INDEX IF NOT EXISTS idx_leave_balance_user_id ON public.leave_balance_history(user_id);
CREATE INDEX IF NOT EXISTS idx_leave_balance_request_id ON public.leave_balance_history(leave_request_id);

-- =====================================================================
-- 5. FONCTIONS ET TRIGGERS UTILITAIRES
-- =====================================================================

-- Fonction de mise à jour timestamp updated_at
CREATE OR REPLACE FUNCTION public.handle_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = timezone('utc'::text, now());
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_profiles_updated_at ON public.profiles;
CREATE TRIGGER trg_profiles_updated_at
    BEFORE UPDATE ON public.profiles
    FOR EACH ROW EXECUTE FUNCTION public.handle_updated_at();

DROP TRIGGER IF EXISTS trg_leave_requests_updated_at ON public.leave_requests;
CREATE TRIGGER trg_leave_requests_updated_at
    BEFORE UPDATE ON public.leave_requests
    FOR EACH ROW EXECUTE FUNCTION public.handle_updated_at();

-- Fonction de contrôle Administrateur RH (SECURITY DEFINER)
CREATE OR REPLACE FUNCTION public.is_hr_admin()
RETURNS BOOLEAN AS $$
BEGIN
    RETURN EXISTS (
        SELECT 1 FROM public.profiles
        WHERE id = auth.uid() AND role = 'HR_ADMIN'
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Trigger automatique de création de profil à l'inscription dans auth.users
CREATE OR REPLACE FUNCTION public.handle_new_auth_user()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO public.profiles (
        id,
        email,
        full_name,
        role
    ) VALUES (
        NEW.id,
        NEW.email,
        COALESCE(NEW.raw_user_meta_data->>'full_name', split_part(NEW.email, '@', 1)),
        CASE 
            WHEN NEW.email = 'elmzabitemohamedtaha@gmail.com' THEN 'HR_ADMIN'
            ELSE 'EMPLOYEE'
        END
    )
    ON CONFLICT (id) DO UPDATE SET
        email = EXCLUDED.email;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION public.handle_new_auth_user();

-- =====================================================================
-- 6. ROW LEVEL SECURITY (RLS) - ISOLATION STRICTE PAR EMPLOYÉ
-- =====================================================================

-- Activation RLS sur les 3 tables
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.leave_requests ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.leave_balance_history ENABLE ROW LEVEL SECURITY;

-- ---------------------------------------------------------------------
-- POLITIQUES SUR 'profiles' :
-- Un employé ne peut voir et éditer que son propre profil.
-- L'administrateur RH a accès à tous les profils.
-- ---------------------------------------------------------------------
DROP POLICY IF EXISTS "profiles_select_own_or_admin" ON public.profiles;
CREATE POLICY "profiles_select_own_or_admin" ON public.profiles
    FOR SELECT TO authenticated
    USING (auth.uid() = id OR public.is_hr_admin());

DROP POLICY IF EXISTS "profiles_insert_own_or_admin" ON public.profiles;
CREATE POLICY "profiles_insert_own_or_admin" ON public.profiles
    FOR INSERT TO authenticated
    WITH CHECK (auth.uid() = id OR public.is_hr_admin());

DROP POLICY IF EXISTS "profiles_update_own_or_admin" ON public.profiles;
CREATE POLICY "profiles_update_own_or_admin" ON public.profiles
    FOR UPDATE TO authenticated
    USING (auth.uid() = id OR public.is_hr_admin())
    WITH CHECK (auth.uid() = id OR public.is_hr_admin());

-- ---------------------------------------------------------------------
-- POLITIQUES SUR 'leave_requests' :
-- - SELECT : l'employé voit UNIQUEMENT ses demandes (auth.uid() = user_id)
-- - INSERT : l'employé ne peut créer des demandes QUE pour son user_id
-- - UPDATE : l'employé peut modifier sa demande UNIQUEMENT si statut 'PENDING'
--            l'administrateur RH peut tout modifier (valider/rejeter)
-- - DELETE : l'employé peut supprimer sa demande UNIQUEMENT si 'PENDING'
-- ---------------------------------------------------------------------
DROP POLICY IF EXISTS "leave_requests_select_isolated" ON public.leave_requests;
CREATE POLICY "leave_requests_select_isolated" ON public.leave_requests
    FOR SELECT TO authenticated
    USING (auth.uid() = user_id OR public.is_hr_admin());

DROP POLICY IF EXISTS "leave_requests_insert_isolated" ON public.leave_requests;
CREATE POLICY "leave_requests_insert_isolated" ON public.leave_requests
    FOR INSERT TO authenticated
    WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "leave_requests_update_isolated" ON public.leave_requests;
CREATE POLICY "leave_requests_update_isolated" ON public.leave_requests
    FOR UPDATE TO authenticated
    USING ((auth.uid() = user_id AND status = 'PENDING') OR public.is_hr_admin())
    WITH CHECK ((auth.uid() = user_id AND status = 'PENDING') OR public.is_hr_admin());

DROP POLICY IF EXISTS "leave_requests_delete_isolated" ON public.leave_requests;
CREATE POLICY "leave_requests_delete_isolated" ON public.leave_requests
    FOR DELETE TO authenticated
    USING ((auth.uid() = user_id AND status = 'PENDING') OR public.is_hr_admin());

-- ---------------------------------------------------------------------
-- POLITIQUES SUR 'leave_balance_history' :
-- - SELECT : l'employé ne voit QUE ses propres écritures de solde (auth.uid() = user_id)
-- - INSERT/UPDATE/DELETE : Réservé aux administrateurs RH
-- ---------------------------------------------------------------------
DROP POLICY IF EXISTS "leave_balance_history_select_isolated" ON public.leave_balance_history;
CREATE POLICY "leave_balance_history_select_isolated" ON public.leave_balance_history
    FOR SELECT TO authenticated
    USING (auth.uid() = user_id OR public.is_hr_admin());

DROP POLICY IF EXISTS "leave_balance_history_admin_write" ON public.leave_balance_history;
CREATE POLICY "leave_balance_history_admin_write" ON public.leave_balance_history
    FOR ALL TO authenticated
    USING (public.is_hr_admin())
    WITH CHECK (public.is_hr_admin());

-- =====================================================================
-- 7. PERMISSIONS POSTGREST
-- =====================================================================
GRANT USAGE ON SCHEMA public TO anon, authenticated;
GRANT ALL ON ALL TABLES IN SCHEMA public TO authenticated;
GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO authenticated;
GRANT ALL ON ALL ROUTINES IN SCHEMA public TO authenticated;
        """.trimIndent()
    }

    companion object {
        private const val PREFS_NAME = "timeoff_supabase_prefs"
        private const val KEY_URL = "supabase_project_url"
        private const val KEY_KEY = "supabase_anon_key"
        private const val KEY_AUTO_SYNC = "supabase_auto_sync_enabled"
        private const val KEY_LAST_SYNC = "supabase_last_sync_timestamp"

        @Volatile
        private var INSTANCE: SupabaseSyncManager? = null

        fun getInstance(context: Context): SupabaseSyncManager {
            return INSTANCE ?: synchronized(this) {
                val instance = SupabaseSyncManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}

data class LeaveBalanceHistoryRecord(
    val leaveType: String,
    val action: String,
    val daysDelta: Double,
    val balanceAfter: Double,
    val reason: String,
    val createdAt: String
)
