package com.example.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

class LocalStorageManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "timeoff_local_storage"
        
        // Keys for active session user
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_CURRENT_USER_EMAIL = "current_user_email"
        private const val KEY_CURRENT_USER_MATRICULE = "current_user_matricule"
        private const val KEY_CURRENT_USER_FULL_NAME = "current_user_full_name"
        private const val KEY_CURRENT_USER_PASSWORD = "current_user_password"
        private const val KEY_CURRENT_USER_JOB_TITLE = "current_user_job_title"
        private const val KEY_CURRENT_USER_DEPARTMENT = "current_user_department"
        private const val KEY_CURRENT_USER_PHONE = "current_user_phone"
        private const val KEY_CURRENT_USER_HIRE_DATE = "current_user_hire_date"
        private const val KEY_CURRENT_USER_OFFICE = "current_user_office"
        private const val KEY_CURRENT_USER_AVATAR = "current_user_avatar"
        private const val KEY_CURRENT_USER_CP_ALLOWANCE = "current_user_cp_allowance"
        private const val KEY_CURRENT_USER_CP_USED = "current_user_cp_used"
        private const val KEY_CURRENT_USER_RTT_ALLOWANCE = "current_user_rtt_allowance"
        private const val KEY_CURRENT_USER_RTT_USED = "current_user_rtt_used"
        private const val KEY_CURRENT_USER_JSON = "current_user_json"
        
        // Key for all registered users in local storage
        private const val KEY_REGISTERED_USERS_JSON = "all_registered_users_json"
        private const val KEY_LAST_USED_EMAIL = "last_used_email"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_auth_enabled"

        @Volatile
        private var INSTANCE: LocalStorageManager? = null

        fun getInstance(context: Context): LocalStorageManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LocalStorageManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    /**
     * Active ou désactive l'authentification biométrique.
     */
    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }

    /**
     * Annule / désactive l'authentification biométrique.
     */
    fun disableBiometric() {
        setBiometricEnabled(false)
    }

    /**
     * Vérifie si l'authentification biométrique est activée dans les préférences.
     * Désactivé par défaut (false) afin de ne pas forcer la biométrie.
     */
    fun isBiometricEnabled(): Boolean {
        return prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
    }

    /**
     * Enregistre le dernier email utilisé pour la connexion ou l'inscription.
     */
    fun setLastUsedEmail(email: String) {
        prefs.edit().putString(KEY_LAST_USED_EMAIL, email.trim()).apply()
    }

    /**
     * Récupère le dernier email utilisé.
     */
    fun getLastUsedEmail(): String? {
        return prefs.getString(KEY_LAST_USED_EMAIL, null)
    }

    /**
     * Sauvegarde complète d'un utilisateur nouvellement inscrit ou connecté dans le Local Storage (SharedPreferences).
     */
    fun saveUser(user: User, setAsActiveSession: Boolean = true) {
        val editor = prefs.edit()

        if (setAsActiveSession) {
            editor.putBoolean(KEY_IS_LOGGED_IN, true)
            editor.putString(KEY_CURRENT_USER_EMAIL, user.email)
            editor.putString(KEY_CURRENT_USER_MATRICULE, user.matricule)
            editor.putString(KEY_CURRENT_USER_FULL_NAME, user.fullName)
            editor.putString(KEY_CURRENT_USER_PASSWORD, user.passwordHash)
            editor.putString(KEY_CURRENT_USER_JOB_TITLE, user.jobTitle)
            editor.putString(KEY_CURRENT_USER_DEPARTMENT, user.department)
            editor.putString(KEY_CURRENT_USER_PHONE, user.phone)
            editor.putString(KEY_CURRENT_USER_HIRE_DATE, user.hireDate)
            editor.putString(KEY_CURRENT_USER_OFFICE, user.officeLocation)
            editor.putString(KEY_CURRENT_USER_AVATAR, user.avatarUrl)
            editor.putInt(KEY_CURRENT_USER_CP_ALLOWANCE, user.paidLeaveAllowance)
            editor.putInt(KEY_CURRENT_USER_CP_USED, user.paidLeaveUsed)
            editor.putInt(KEY_CURRENT_USER_RTT_ALLOWANCE, user.rttAllowance)
            editor.putInt(KEY_CURRENT_USER_RTT_USED, user.rttUsed)
            editor.putString(KEY_CURRENT_USER_JSON, userToJson(user).toString())
        }

        // Ajouter l'utilisateur à la liste des utilisateurs enregistrés localement
        val existingUsers = getAllRegisteredUsers().toMutableList()
        val index = existingUsers.indexOfFirst { it.email.equals(user.email, ignoreCase = true) }
        if (index >= 0) {
            existingUsers[index] = user
        } else {
            existingUsers.add(user)
        }

        val jsonArray = JSONArray()
        for (u in existingUsers) {
            jsonArray.put(userToJson(u))
        }
        editor.putString(KEY_REGISTERED_USERS_JSON, jsonArray.toString())

        editor.apply()
    }

    /**
     * Récupère l'utilisateur connecté actuellement depuis le Local Storage.
     */
    fun getCurrentUser(): User? {
        val email = prefs.getString(KEY_CURRENT_USER_EMAIL, null) ?: return null
        val matricule = prefs.getString(KEY_CURRENT_USER_MATRICULE, "EMP-0001") ?: "EMP-0001"
        val fullName = prefs.getString(KEY_CURRENT_USER_FULL_NAME, "") ?: ""
        val password = prefs.getString(KEY_CURRENT_USER_PASSWORD, "") ?: ""
        val jobTitle = prefs.getString(KEY_CURRENT_USER_JOB_TITLE, "Collaborateur") ?: "Collaborateur"
        val department = prefs.getString(KEY_CURRENT_USER_DEPARTMENT, "Ingénierie & IT") ?: "Ingénierie & IT"
        val phone = prefs.getString(KEY_CURRENT_USER_PHONE, "+212 6 00 00 00 00") ?: "+212 6 00 00 00 00"
        val hireDate = prefs.getString(KEY_CURRENT_USER_HIRE_DATE, "Aujourd'hui") ?: "Aujourd'hui"
        val office = prefs.getString(KEY_CURRENT_USER_OFFICE, "Siège Principal") ?: "Siège Principal"
        val avatar = prefs.getString(KEY_CURRENT_USER_AVATAR, "") ?: ""
        val cpAllowance = prefs.getInt(KEY_CURRENT_USER_CP_ALLOWANCE, 25)
        val cpUsed = prefs.getInt(KEY_CURRENT_USER_CP_USED, 0)
        val rttAllowance = prefs.getInt(KEY_CURRENT_USER_RTT_ALLOWANCE, 10)
        val rttUsed = prefs.getInt(KEY_CURRENT_USER_RTT_USED, 0)

        return User(
            email = email,
            matricule = matricule,
            fullName = fullName,
            passwordHash = password,
            jobTitle = jobTitle,
            department = department,
            phone = phone,
            hireDate = hireDate,
            officeLocation = office,
            avatarUrl = avatar,
            paidLeaveAllowance = cpAllowance,
            paidLeaveUsed = cpUsed,
            rttAllowance = rttAllowance,
            rttUsed = rttUsed
        )
    }

    /**
     * Supprime des utilisateurs par email de la liste enregistrée dans le Local Storage.
     */
    fun removeUsersByEmail(emailsToRemove: Set<String>) {
        val existingUsers = getAllRegisteredUsers().toMutableList()
        val filtered = existingUsers.filterNot { u -> emailsToRemove.any { it.equals(u.email.trim(), ignoreCase = true) } }
        val jsonArray = JSONArray()
        for (u in filtered) {
            jsonArray.put(userToJson(u))
        }
        val editor = prefs.edit()
        editor.putString(KEY_REGISTERED_USERS_JSON, jsonArray.toString())
        val currentEmail = prefs.getString(KEY_CURRENT_USER_EMAIL, null)
        if (currentEmail != null && emailsToRemove.any { it.equals(currentEmail.trim(), ignoreCase = true) }) {
            clearActiveSession()
        }
        editor.apply()
    }

    /**
     * Récupère la liste de tous les utilisateurs inscrits stockés dans le Local Storage.
     */
    fun getAllRegisteredUsers(): List<User> {
        val jsonStr = prefs.getString(KEY_REGISTERED_USERS_JSON, null) ?: return emptyList()
        val list = mutableListOf<User>()
        try {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(jsonToUser(obj))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    /**
     * Recherche un utilisateur par son email dans le Local Storage.
     */
    fun getUserByEmail(email: String): User? {
        return getAllRegisteredUsers().find { it.email.equals(email.trim(), ignoreCase = true) }
    }

    /**
     * Supprime la session active lors de la déconnexion.
     */
    fun clearActiveSession() {
        prefs.edit()
            .remove(KEY_IS_LOGGED_IN)
            .remove(KEY_CURRENT_USER_EMAIL)
            .remove(KEY_CURRENT_USER_MATRICULE)
            .remove(KEY_CURRENT_USER_FULL_NAME)
            .remove(KEY_CURRENT_USER_PASSWORD)
            .remove(KEY_CURRENT_USER_JOB_TITLE)
            .remove(KEY_CURRENT_USER_DEPARTMENT)
            .remove(KEY_CURRENT_USER_PHONE)
            .remove(KEY_CURRENT_USER_HIRE_DATE)
            .remove(KEY_CURRENT_USER_OFFICE)
            .remove(KEY_CURRENT_USER_AVATAR)
            .remove(KEY_CURRENT_USER_CP_ALLOWANCE)
            .remove(KEY_CURRENT_USER_CP_USED)
            .remove(KEY_CURRENT_USER_RTT_ALLOWANCE)
            .remove(KEY_CURRENT_USER_RTT_USED)
            .remove(KEY_CURRENT_USER_JSON)
            .apply()
    }

    private fun userToJson(user: User): JSONObject {
        val obj = JSONObject()
        obj.put("email", user.email)
        obj.put("matricule", user.matricule)
        obj.put("fullName", user.fullName)
        obj.put("passwordHash", user.passwordHash)
        obj.put("jobTitle", user.jobTitle)
        obj.put("department", user.department)
        obj.put("phone", user.phone)
        obj.put("hireDate", user.hireDate)
        obj.put("officeLocation", user.officeLocation)
        obj.put("avatarUrl", user.avatarUrl)
        obj.put("paidLeaveAllowance", user.paidLeaveAllowance)
        obj.put("paidLeaveUsed", user.paidLeaveUsed)
        obj.put("rttAllowance", user.rttAllowance)
        obj.put("rttUsed", user.rttUsed)
        return obj
    }

    private fun jsonToUser(obj: JSONObject): User {
        return User(
            email = obj.optString("email", ""),
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
}
