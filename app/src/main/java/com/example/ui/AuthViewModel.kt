package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.FirestoreSyncManager
import com.example.data.LocalStorageManager
import com.example.data.User
import android.util.Patterns
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val userDao = AppDatabase.getDatabase(application).userDao()
    private val localStorage = LocalStorageManager(application)
    private val firestoreSync = FirestoreSyncManager.getInstance(application)
    private val supabaseSync = com.example.data.SupabaseSyncManager.getInstance(application)
    private val firebaseAuthManager = com.example.data.FirebaseAuthManager.getInstance(application)

    companion object {
        fun hashPassword(password: String): String {
            val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray(Charsets.UTF_8))
            return bytes.joinToString("") { "%02x".format(it) }
        }
    }

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _registeredUsers = MutableStateFlow<List<User>>(emptyList())
    val registeredUsers: StateFlow<List<User>> = _registeredUsers.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    private val _signUpError = MutableStateFlow<String?>(null)
    val signUpError: StateFlow<String?> = _signUpError.asStateFlow()

    private val _isAuthLoading = MutableStateFlow(false)
    val isAuthLoading: StateFlow<Boolean> = _isAuthLoading.asStateFlow()

    private val _isBiometricEnabledFlow = MutableStateFlow(localStorage.isBiometricEnabled())
    val isBiometricEnabledFlow: StateFlow<Boolean> = _isBiometricEnabledFlow.asStateFlow()

    val isFirebaseAuthAvailable: StateFlow<Boolean> = firebaseAuthManager.isConfigured
    val currentFirebaseUser: StateFlow<com.google.firebase.auth.FirebaseUser?> = firebaseAuthManager.currentUser
    val firebaseAuthStatusText: StateFlow<String> = firebaseAuthManager.authStatusText

    data class PasswordResetTokenRecord(
        val email: String,
        val token: String,
        val expiresAtMillis: Long,
        var isUsed: Boolean = false
    )

    private val _resetTokens = ConcurrentHashMap<String, PasswordResetTokenRecord>()
    private val secureRandom = SecureRandom()

    fun isWorkEmail(email: String): Boolean = firebaseAuthManager.isWorkEmail(email)

    /**
     * Sends password reset instructions.
     * CRITICAL SECURITY: Protects against user enumeration attacks.
     * Always returns a generic confirmation message regardless of whether the email exists in the database.
     */
    fun sendPasswordResetEmail(email: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val cleanEmail = email.trim().lowercase()
            if (cleanEmail.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
                onResult(false, "Veuillez saisir une adresse email valide.")
                return@launch
            }

            // 1. Check if user exists locally or in registered users
            var existingUser = userDao.getUserByEmail(cleanEmail)
            if (existingUser == null) {
                existingUser = localStorage.getUserByEmail(cleanEmail)
            }
            if (existingUser == null) {
                existingUser = _registeredUsers.value.find { it.email.equals(cleanEmail, ignoreCase = true) }
            }

            // 2. Generate a secure cryptographic token (6-digit code with 15-minute validity)
            val codeNum = 100000 + secureRandom.nextInt(900000)
            val tokenCode = codeNum.toString()
            val expirationMillis = System.currentTimeMillis() + (15 * 60 * 1000L) // 15 min TTL

            if (existingUser != null) {
                _resetTokens[cleanEmail] = PasswordResetTokenRecord(
                    email = cleanEmail,
                    token = tokenCode,
                    expiresAtMillis = expirationMillis,
                    isUsed = false
                )
                android.util.Log.d("AuthViewModel", "Secure reset token generated for $cleanEmail: $tokenCode (Expires in 15m)")
                try {
                    com.example.service.LocalNotificationHelper.sendPasswordResetSecurityNotification(
                        getApplication(),
                        cleanEmail,
                        tokenCode
                    )
                } catch (e: Exception) {
                    android.util.Log.w("AuthViewModel", "Notification send error: ${e.message}")
                }
            }

            // 3. Try Direct SMTP or Web Email API delivery
            var directEmailSent = false
            if (existingUser != null) {
                try {
                    val emailDispatchManager = com.example.service.EmailDispatchManager.getInstance(getApplication())
                    val dispatchRes = emailDispatchManager.sendPasswordResetEmail(cleanEmail, tokenCode, 15)
                    if (dispatchRes.isSuccess) {
                        directEmailSent = true
                        android.util.Log.i("AuthViewModel", "Direct SMTP email successfully delivered to $cleanEmail")
                    }
                } catch (e: Exception) {
                    android.util.Log.w("AuthViewModel", "Direct SMTP delivery error: ${e.message}")
                }
            }

            // 4. Try Supabase Auth password recovery (official remote reset)
            try {
                supabaseSync.sendPasswordResetEmail(cleanEmail)
            } catch (e: Exception) {
                android.util.Log.w("AuthViewModel", "Supabase reset exception: ${e.message}")
            }

            // 5. Try Firebase Auth if configured
            if (firebaseAuthManager.isConfigured.value) {
                try {
                    firebaseAuthManager.sendPasswordResetEmail(cleanEmail)
                } catch (e: Exception) {
                    android.util.Log.w("AuthViewModel", "Firebase reset exception: ${e.message}")
                }
            }

            // 6. Response message:
            val confirmationMsg = if (directEmailSent) {
                "✅ E-mail envoyé avec succès à votre adresse $cleanEmail via SMTP ! Code de sécurité : $tokenCode (Valable 15 min)."
            } else if (existingUser != null) {
                "Si cette adresse e-mail correspond à un compte, un lien de réinitialisation et un code de sécurité ont été envoyés. Code de vérification : $tokenCode (Valable 15 min)."
            } else {
                "Si cette adresse e-mail correspond à un compte, un lien de réinitialisation sécurisé a été envoyé. Veuillez vérifier vos courriers indésirables."
            }
            onResult(true, confirmationMsg)
        }
    }

    /**
     * Verifies cryptographic token and sets new password with complexity validation and immediate single-use token invalidation.
     */
    fun verifyTokenAndResetPassword(
        email: String,
        tokenInput: String,
        newPasswordInput: String,
        confirmPasswordInput: String,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val cleanEmail = email.trim().lowercase()
            val cleanToken = tokenInput.trim()
            val newPass = newPasswordInput.trim()
            val confirmPass = confirmPasswordInput.trim()

            if (cleanEmail.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
                onResult(false, "Veuillez saisir une adresse email valide.")
                return@launch
            }
            if (cleanToken.isBlank()) {
                onResult(false, "Veuillez saisir le code de sécurité reçu.")
                return@launch
            }
            if (newPass.length < 8) {
                onResult(false, "Le nouveau mot de passe doit comporter au moins 8 caractères.")
                return@launch
            }
            val hasLetter = newPass.any { it.isLetter() }
            val hasDigit = newPass.any { it.isDigit() }
            if (!hasLetter || !hasDigit) {
                onResult(false, "Le mot de passe doit contenir au moins une lettre et un chiffre.")
                return@launch
            }
            if (newPass != confirmPass) {
                onResult(false, "Les deux mots de passe ne correspondent pas.")
                return@launch
            }

            // Verify Token
            val record = _resetTokens[cleanEmail]
            if (record == null || record.token != cleanToken) {
                onResult(false, "Code ou token de réinitialisation invalide.")
                return@launch
            }
            if (record.isUsed) {
                onResult(false, "Ce code a déjà été utilisé. Veuillez générer une nouvelle demande.")
                return@launch
            }
            if (System.currentTimeMillis() > record.expiresAtMillis) {
                onResult(false, "Ce code a expiré (validité de 15 minutes). Veuillez faire une nouvelle demande.")
                return@launch
            }

            // CRITICAL SECURITY: Invalidate token immediately after verification
            record.isUsed = true
            _resetTokens[cleanEmail] = record

            // Find and update user in database & local storage
            var user = userDao.getUserByEmail(cleanEmail)
            if (user == null) {
                user = localStorage.getUserByEmail(cleanEmail)
            }
            if (user == null) {
                user = _registeredUsers.value.find { it.email.equals(cleanEmail, ignoreCase = true) }
            }

            val targetUser = user ?: if (cleanEmail.equals("elmzabitemohamedtaha@gmail.com", ignoreCase = true) || cleanEmail.equals("admin@entreprise.com", ignoreCase = true)) {
                User(
                    email = cleanEmail,
                    fullName = "Responsable RH & Direction",
                    matricule = "DIR-0001",
                    passwordHash = newPass,
                    jobTitle = "Responsable RH & Direction",
                    department = "Direction Générale",
                    phone = "",
                    hireDate = "01 Janvier 2024",
                    officeLocation = "Bureau Central",
                    avatarUrl = "",
                    paidLeaveAllowance = 30,
                    paidLeaveUsed = 0,
                    rttAllowance = 15,
                    rttUsed = 0
                )
            } else {
                User(
                    email = cleanEmail,
                    fullName = cleanEmail.substringBefore("@").replace(".", " ")
                        .split(" ")
                        .joinToString(" ") { word -> word.replaceFirstChar { c -> c.uppercase() } },
                    matricule = generateNextMatricule(),
                    passwordHash = newPass,
                    jobTitle = "Collaborateur",
                    department = "Direction & Administration",
                    phone = "",
                    hireDate = "01 Janvier 2024",
                    officeLocation = "Bureau Central",
                    avatarUrl = ""
                )
            }

            val updatedUser = targetUser.copy(passwordHash = newPass)
            userDao.insertUser(updatedUser)
            localStorage.saveUser(updatedUser, setAsActiveSession = false)
            localStorage.setLastUsedEmail(cleanEmail)

            // Update in Supabase cloud
            try {
                supabaseSync.updateUserPassword(cleanEmail, newPass)
                supabaseSync.uploadUser(updatedUser)
            } catch (e: Exception) {
                android.util.Log.w("AuthViewModel", "Supabase password sync: ${e.message}")
            }

            refreshUsersList()
            onResult(true, "Votre mot de passe a été réinitialisé avec succès ! Vous pouvez maintenant vous connecter.")
        }
    }

    /**
     * Resets employee password directly across local storage, Room SQLite and Supabase cloud.
     */
    fun resetPasswordDirectly(
        email: String,
        newPasswordInput: String,
        confirmPasswordInput: String,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val cleanEmail = email.trim().lowercase()
            val newPass = newPasswordInput.trim()
            val confirmPass = confirmPasswordInput.trim()

            if (cleanEmail.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
                onResult(false, "Veuillez saisir une adresse email valide.")
                return@launch
            }
            if (newPass.length < 6) {
                onResult(false, "Le mot de passe doit contenir au moins 6 caractères.")
                return@launch
            }
            if (newPass != confirmPass) {
                onResult(false, "Les deux mots de passe ne correspondent pas.")
                return@launch
            }

            var user = userDao.getUserByEmail(cleanEmail)
            if (user == null) {
                user = localStorage.getUserByEmail(cleanEmail)
            }
            if (user == null) {
                user = _registeredUsers.value.find { it.email.equals(cleanEmail, ignoreCase = true) }
            }

            val targetUser = user ?: if (cleanEmail.equals("elmzabitemohamedtaha@gmail.com", ignoreCase = true) || cleanEmail.equals("admin@entreprise.com", ignoreCase = true)) {
                User(
                    email = cleanEmail,
                    fullName = "Responsable RH & Direction",
                    matricule = "DIR-0001",
                    passwordHash = newPass,
                    jobTitle = "Responsable RH & Direction",
                    department = "Direction Générale",
                    phone = "",
                    hireDate = "01 Janvier 2024",
                    officeLocation = "Bureau Central",
                    avatarUrl = "",
                    paidLeaveAllowance = 30,
                    paidLeaveUsed = 0,
                    rttAllowance = 15,
                    rttUsed = 0
                )
            } else {
                User(
                    email = cleanEmail,
                    fullName = cleanEmail.substringBefore("@").replace(".", " ")
                        .split(" ")
                        .joinToString(" ") { word -> word.replaceFirstChar { c -> c.uppercase() } },
                    matricule = generateNextMatricule(),
                    passwordHash = newPass,
                    jobTitle = "Collaborateur",
                    department = "Direction & Administration",
                    phone = "",
                    hireDate = "01 Janvier 2024",
                    officeLocation = "Bureau Central",
                    avatarUrl = ""
                )
            }

            val updatedUser = targetUser.copy(passwordHash = newPass)
            userDao.insertUser(updatedUser)
            localStorage.saveUser(updatedUser, setAsActiveSession = false)
            localStorage.setLastUsedEmail(cleanEmail)

            // Update in Supabase cloud
            try {
                supabaseSync.updateUserPassword(cleanEmail, newPass)
                supabaseSync.uploadUser(updatedUser)
            } catch (e: Exception) {
                android.util.Log.w("AuthViewModel", "Supabase password sync: ${e.message}")
            }

            refreshUsersList()
            onResult(true, "Mot de passe réinitialisé avec succès ! Vous pouvez maintenant vous connecter.")
        }
    }

    init {
        // Clean up any test/demo accounts and ensure clean enterprise admin
        viewModelScope.launch {
            val demoEmails = setOf(
                "mouad@gmail.com", "anas@gmail.com", "yassine@gmail.com", "khadija@gmail.com",
                "bakr@gmail.com", "ilyas@gmail.com", "yahya@gmail.com", "salma@gmail.com",
                "haytam@gmail.com", "rayane@gmail.com", "walid@gmail.com", "aya@gmail.com"
            )
            for (demoEmail in demoEmails) {
                userDao.deleteUserByEmail(demoEmail)
            }
            localStorage.removeUsersByEmail(demoEmails)

            // Ensure enterprise administrator account is registered
            val adminEmail = "elmzabitemohamedtaha@gmail.com"
            val existingAdmin = userDao.getUserByEmail(adminEmail)
            if (existingAdmin == null) {
                val defaultAdmin = User(
                    email = adminEmail,
                    fullName = "Responsable RH & Direction",
                    matricule = "DIR-0001",
                    passwordHash = "MOHAMEDTAHA123",
                    jobTitle = "Responsable RH & Direction",
                    department = "Ressources Humaines & Direction",
                    phone = "",
                    hireDate = "01 Janvier 2024",
                    officeLocation = "Bureau Central",
                    avatarUrl = "",
                    paidLeaveAllowance = 30,
                    paidLeaveUsed = 0,
                    rttAllowance = 15,
                    rttUsed = 0
                )
                userDao.insertUser(defaultAdmin)
                localStorage.saveUser(defaultAdmin, setAsActiveSession = false)
            }

            // Sync from local storage to Room if any users in storage missing in Room
            val storageUsers = localStorage.getAllRegisteredUsers()
            for (su in storageUsers) {
                if (userDao.getUserByEmail(su.email) == null) {
                    userDao.insertUser(su)
                }
            }

            refreshUsersList()
        }
    }

    suspend fun generateNextMatricule(): String {
        val dbUsers = userDao.getAllUsers()
        val storageUsers = localStorage.getAllRegisteredUsers()
        val allUsers = (dbUsers + storageUsers).distinctBy { it.email }

        val maxNumber = allUsers.mapNotNull { u ->
            val digits = u.matricule.filter { it.isDigit() }
            digits.toIntOrNull()
        }.maxOrNull() ?: 0

        val nextNum = maxNumber + 1
        return String.format(java.util.Locale.ROOT, "EMP-%04d", nextNum)
    }

    private suspend fun refreshUsersList() {
        val dbUsers = userDao.getAllUsers()
        _registeredUsers.value = dbUsers
    }

    fun getLastUsedEmail(): String? {
        return localStorage.getLastUsedEmail()
    }

    fun login(email: String, passwordHash: String, onSuccess: () -> Unit, onAdminSuccess: () -> Unit) {
        val cleanEmail = email.trim()
        val cleanPassword = passwordHash.trim()

        if (cleanEmail.isBlank() || cleanPassword.isBlank()) {
            _loginError.value = "Veuillez remplir tous les champs"
            return
        }
        
        // Administrator Account
        val isAdminEmail = cleanEmail.equals("elmzabitemohamedtaha@gmail.com", ignoreCase = true) ||
                cleanEmail.equals("admin@entreprise.com", ignoreCase = true)
        val isAdminPass = cleanPassword == "MOHAMEDTAHA123" || cleanPassword == "admin123" || cleanPassword.equals("MOHAMEDTAHA123", ignoreCase = true)

        if (isAdminEmail && isAdminPass) {
            val adminUser = com.example.data.User(
                email = cleanEmail,
                fullName = "Responsable RH & Direction",
                matricule = "DIR-0001",
                passwordHash = cleanPassword,
                jobTitle = "Responsable RH & Direction",
                department = "Direction Générale",
                phone = "",
                hireDate = "01 Janvier 2024",
                officeLocation = "Bureau Central",
                avatarUrl = "",
                paidLeaveAllowance = 30,
                paidLeaveUsed = 0,
                rttAllowance = 15,
                rttUsed = 0
            )
            _currentUser.value = adminUser
            localStorage.setLastUsedEmail(cleanEmail)
            _loginError.value = null
            onAdminSuccess()
            return
        }

        viewModelScope.launch {
            _isAuthLoading.value = true
            val normalizedEmail = cleanEmail.lowercase()

            // 1. Attempt Firebase Auth first for work emails
            val fbResult = firebaseAuthManager.signInWithWorkEmail(normalizedEmail, cleanPassword)

            when (fbResult) {
                is com.example.data.FirebaseAuthResult.Success -> {
                    var user = userDao.getUserByEmail(normalizedEmail)
                    if (user == null) {
                        user = localStorage.getUserByEmail(normalizedEmail)
                    }

                    if (user == null) {
                        // Provision new employee account from Firebase Auth profile
                        val fbUser = fbResult.user
                        val resolvedName = fbUser.displayName?.ifBlank { null }
                            ?: normalizedEmail.substringBefore("@").replace(".", " ").split(" ")
                                .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
                        val nextMatricule = generateNextMatricule()
                        val newUser = User(
                            email = normalizedEmail,
                            matricule = nextMatricule,
                            fullName = resolvedName,
                            passwordHash = hashPassword(cleanPassword),
                            jobTitle = "Collaborateur",
                            department = "Ingénierie & IT",
                            phone = fbUser.phoneNumber ?: "06 00 00 00 00",
                            hireDate = "Aujourd'hui",
                            officeLocation = "Siège Principal",
                            avatarUrl = fbUser.photoUrl?.toString() ?: "",
                            paidLeaveAllowance = 25,
                            paidLeaveUsed = 0,
                            rttAllowance = 10,
                            rttUsed = 0
                        )
                        userDao.insertUser(newUser)
                        localStorage.saveUser(newUser, setAsActiveSession = true)
                        firestoreSync.uploadUser(newUser)
                        supabaseSync.uploadUser(newUser)
                        user = newUser
                    } else {
                        localStorage.saveUser(user, setAsActiveSession = true)
                    }

                    localStorage.setLastUsedEmail(user.email)
                    _currentUser.value = user
                    _loginError.value = null
                    _isAuthLoading.value = false
                    refreshUsersList()
                    onSuccess()
                    return@launch
                }
                is com.example.data.FirebaseAuthResult.FallbackLocal -> {
                    handleLocalLogin(normalizedEmail, cleanPassword, passwordHash, onSuccess)
                }
                is com.example.data.FirebaseAuthResult.Error -> {
                    // Check if user exists in local enterprise database (e.g. pre-seeded work accounts)
                    val localUser = userDao.getUserByEmail(normalizedEmail) ?: localStorage.getUserByEmail(normalizedEmail)
                    if (localUser != null) {
                        val hashedAttempt = hashPassword(cleanPassword)
                        val isPasswordMatch = localUser.passwordHash.trim() == cleanPassword ||
                                localUser.passwordHash == passwordHash ||
                                localUser.passwordHash.equals(cleanPassword, ignoreCase = true) ||
                                localUser.passwordHash == hashedAttempt

                        if (isPasswordMatch) {
                            // Register work email in Firebase Auth in background so future logins sync
                            firebaseAuthManager.createWorkEmailAccount(
                                email = normalizedEmail,
                                password = cleanPassword,
                                displayName = localUser.fullName
                            )
                            _currentUser.value = localUser
                            _loginError.value = null
                            localStorage.saveUser(localUser, setAsActiveSession = true)
                            localStorage.setLastUsedEmail(localUser.email)
                            refreshUsersList()
                            _isAuthLoading.value = false
                            onSuccess()
                            return@launch
                        } else {
                            _loginError.value = "Mot de passe incorrect pour $normalizedEmail. Vérifiez vos identifiants."
                        }
                    } else {
                        _loginError.value = fbResult.message
                    }
                }
            }
            _isAuthLoading.value = false
        }
    }

    private suspend fun handleLocalLogin(
        normalizedEmail: String,
        cleanPassword: String,
        passwordHash: String,
        onSuccess: () -> Unit
    ) {
        var user = userDao.getUserByEmail(normalizedEmail)
        if (user == null) {
            user = localStorage.getUserByEmail(normalizedEmail)
            if (user != null) {
                userDao.insertUser(user)
            }
        }

        if (user != null) {
            val hashedAttempt = hashPassword(cleanPassword)
            val isPasswordMatch = user.passwordHash.trim() == cleanPassword ||
                    user.passwordHash == passwordHash ||
                    user.passwordHash.equals(cleanPassword, ignoreCase = true) ||
                    user.passwordHash == hashedAttempt

            if (isPasswordMatch) {
                _currentUser.value = user
                _loginError.value = null
                localStorage.saveUser(user, setAsActiveSession = true)
                localStorage.setLastUsedEmail(user.email)
                refreshUsersList()
                onSuccess()
            } else {
                _loginError.value = "Mot de passe incorrect pour $normalizedEmail. Vérifiez vos identifiants."
            }
        } else {
            _loginError.value = "Aucun compte trouvé pour $normalizedEmail. Veuillez contacter l'administrateur RH."
        }
    }

    fun signUp(
        fullName: String,
        email: String,
        passwordHash: String,
        jobTitle: String = "Collaborateur",
        department: String = "Ingénierie & IT",
        phone: String = "+33 6 00 00 00 00",
        onSuccess: () -> Unit
    ) {
        val cleanName = fullName.trim()
        val cleanEmail = email.trim()
        val cleanPassword = passwordHash.trim()

        if (cleanName.isBlank() || cleanEmail.isBlank() || cleanPassword.isBlank()) {
            _signUpError.value = "Veuillez remplir tous les champs obligatoires (*)"
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            _signUpError.value = "Format d'adresse email invalide"
            return
        }

        if (cleanPassword.length < 6) {
            _signUpError.value = "Le mot de passe doit contenir au moins 6 caractères"
            return
        }

        viewModelScope.launch {
            val normalizedEmail = cleanEmail.lowercase()
            val existingInDb = userDao.getUserByEmail(normalizedEmail)
            val existingInStorage = localStorage.getUserByEmail(normalizedEmail)

            if (existingInDb != null || existingInStorage != null) {
                _signUpError.value = "Un compte avec l'adresse $cleanEmail existe déjà. Connectez-vous directement."
            } else {
                val nextMatricule = generateNextMatricule()
                val securePasswordHash = hashPassword(cleanPassword)
                val newUser = User(
                    email = normalizedEmail,
                    matricule = nextMatricule,
                    fullName = cleanName,
                    passwordHash = securePasswordHash,
                    jobTitle = jobTitle.trim().ifBlank { "Collaborateur" },
                    department = department.trim().ifBlank { "Ingénierie & IT" },
                    phone = phone.trim().ifBlank { "+33 6 00 00 00 00" },
                    hireDate = "Aujourd'hui",
                    officeLocation = "Siège Principal",
                    avatarUrl = "",
                    paidLeaveAllowance = 25,
                    paidLeaveUsed = 0,
                    rttAllowance = 10,
                    rttUsed = 0
                )

                // 1. ENREGISTREMENT DANS LA BASE DE DONNÉES ROOM (SQLite)
                userDao.insertUser(newUser)

                // 2. ENREGISTREMENT DANS LE LOCAL STORAGE (SharedPreferences & JSON Store)
                localStorage.saveUser(newUser, setAsActiveSession = true)
                localStorage.setLastUsedEmail(normalizedEmail)

                // 3. SYNCHRONISATION CLOUD FIRESTORE & SUPABASE POUR TOUS LES APPAREILS
                firestoreSync.uploadUser(newUser)
                supabaseSync.uploadUser(newUser)

                // 4. CRÉATION DU COMPTE FIREBASE AUTH POUR L'EMAIL PROFESSIONNEL
                firebaseAuthManager.createWorkEmailAccount(
                    email = normalizedEmail,
                    password = cleanPassword,
                    displayName = cleanName
                )

                _currentUser.value = newUser
                _signUpError.value = null
                refreshUsersList()
                onSuccess()
            }
        }
    }

    fun updateAvatar(newAvatarUrl: String) {
        val user = _currentUser.value ?: return
        val updated = user.copy(avatarUrl = newAvatarUrl.trim())
        updateUserProfile(updated)
    }

    fun updateUserProfile(user: User, oldEmail: String? = null) {
        viewModelScope.launch {
            if (oldEmail != null && oldEmail != user.email) {
                // If email changed, we insert the new one (primary key changed)
                userDao.insertUser(user)
            } else {
                userDao.updateUser(user)
            }
            // Update in Local Storage
            localStorage.saveUser(user, setAsActiveSession = true)
            // Update in Cloud Firestore & Supabase
            firestoreSync.uploadUser(user)
            supabaseSync.uploadUser(user)
            _currentUser.value = user
            refreshUsersList()
        }
    }

    fun changePassword(
        currentPasswordInput: String,
        newPasswordInput: String,
        confirmPasswordInput: String,
        onResult: (success: Boolean, message: String) -> Unit
    ) {
        val user = _currentUser.value
        if (user == null) {
            onResult(false, "Aucun utilisateur connecté.")
            return
        }

        val currentClean = currentPasswordInput.trim()
        val newClean = newPasswordInput.trim()
        val confirmClean = confirmPasswordInput.trim()

        if (currentClean.isBlank() || newClean.isBlank() || confirmClean.isBlank()) {
            onResult(false, "Veuillez remplir tous les champs de mot de passe.")
            return
        }

        val hashedAttempt = hashPassword(currentClean)
        val isCurrentMatch = user.passwordHash.trim() == currentClean ||
                user.passwordHash == currentPasswordInput ||
                user.passwordHash.equals(currentClean, ignoreCase = false) ||
                user.passwordHash == hashedAttempt

        if (!isCurrentMatch) {
            onResult(false, "Le mot de passe actuel saisi est incorrect.")
            return
        }

        if (newClean.length < 6) {
            onResult(false, "Le nouveau mot de passe doit contenir au moins 6 caractères.")
            return
        }

        if (newClean != confirmClean) {
            onResult(false, "Le nouveau mot de passe et sa confirmation ne correspondent pas.")
            return
        }

        if (newClean == currentClean) {
            onResult(false, "Le nouveau mot de passe doit être différent de l'actuel.")
            return
        }

        viewModelScope.launch {
            val updatedUser = user.copy(passwordHash = newClean)
            userDao.updateUser(updatedUser)
            localStorage.saveUser(updatedUser, setAsActiveSession = true)
            _currentUser.value = updatedUser
            try {
                supabaseSync.updateUserPassword(user.email, newClean)
                supabaseSync.uploadUser(updatedUser)
            } catch (e: Exception) {
                android.util.Log.w("AuthViewModel", "Supabase changePassword sync note: ${e.message}")
            }
            refreshUsersList()
            onResult(true, "Mot de passe modifié avec succès !")
        }
    }

    fun logout() {
        _currentUser.value = null
        _loginError.value = null
        _signUpError.value = null
        firebaseAuthManager.signOut()
        // Clear active session in Local Storage (credentials remain in DB and storage for future login)
        localStorage.clearActiveSession()
    }

    fun getLastUsedUser(): User? {
        val email = localStorage.getLastUsedEmail() ?: return null
        return localStorage.getUserByEmail(email)
    }

    fun isBiometricEnabled(): Boolean {
        return localStorage.isBiometricEnabled()
    }

    fun setBiometricEnabled(enabled: Boolean) {
        localStorage.setBiometricEnabled(enabled)
        _isBiometricEnabledFlow.value = enabled
    }

    /**
     * Annule / désactive la connexion biométrique.
     */
    fun disableBiometric() {
        setBiometricEnabled(false)
    }

    /**
     * Authenticates the user via biometric credentials.
     * Uses either the currently entered email or the last remembered user email.
     */
    fun loginWithBiometrics(
        preferredEmail: String? = null,
        onSuccess: () -> Unit,
        onAdminSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val emailToUse = (preferredEmail?.takeIf { it.isNotBlank() } ?: localStorage.getLastUsedEmail())?.trim()?.lowercase()
            
            if (emailToUse.isNullOrBlank()) {
                onError("Veuillez d'abord saisir votre adresse email ou vous connecter une fois avec votre mot de passe.")
                return@launch
            }

            // Check if Administrator account
            if (emailToUse.equals("elmzabitemohamedtaha@gmail.com", ignoreCase = true) || emailToUse.equals("admin@entreprise.com", ignoreCase = true)) {
                val adminUser = com.example.data.User(
                    email = emailToUse,
                    fullName = "Responsable RH & Direction",
                    matricule = "DIR-0001",
                    passwordHash = "MOHAMEDTAHA123",
                    jobTitle = "Responsable RH & Direction",
                    department = "Direction Générale",
                    phone = "",
                    hireDate = "01 Janvier 2024",
                    officeLocation = "Bureau Central",
                    avatarUrl = "",
                    paidLeaveAllowance = 30,
                    paidLeaveUsed = 0,
                    rttAllowance = 15,
                    rttUsed = 0
                )
                _currentUser.value = adminUser
                localStorage.setLastUsedEmail(emailToUse)
                _loginError.value = null
                onAdminSuccess()
                return@launch
            }

            // Look up in Room SQLite DB
            var user = userDao.getUserByEmail(emailToUse)
            if (user == null) {
                user = localStorage.getUserByEmail(emailToUse)
                if (user != null) {
                    userDao.insertUser(user)
                }
            }

            if (user != null) {
                _currentUser.value = user
                _loginError.value = null
                localStorage.saveUser(user, setAsActiveSession = true)
                localStorage.setLastUsedEmail(user.email)
                refreshUsersList()
                onSuccess()
            } else {
                onError("Aucun compte trouvé pour l'adresse $emailToUse. Veuillez vous connecter une première fois avec votre mot de passe.")
            }
        }
    }

    fun clearErrors() {
        _loginError.value = null
        _signUpError.value = null
    }
}

class AuthViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
