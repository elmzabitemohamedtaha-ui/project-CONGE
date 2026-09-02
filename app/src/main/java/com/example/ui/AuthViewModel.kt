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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val userDao = AppDatabase.getDatabase(application).userDao()
    private val localStorage = LocalStorageManager(application)
    private val firestoreSync = FirestoreSyncManager.getInstance(application)

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

    init {
        // Synchronize & pre-populate seed users and load all saved users
        viewModelScope.launch {
            val seedUsers = listOf(
                
                User(
                    email = "mouad@gmail.com",
                    matricule = "EMP-0002",
                    fullName = "Mouad elmzabite",
                    passwordHash = "MOHAMEDTAHA123",
                    jobTitle = "Développeur Senior",
                    department = "Ingénierie & IT",
                    phone = "0691366836",
                    hireDate = "12 Mars 2018",
                    officeLocation = "Nouacer,l'atelier",
                    avatarUrl = "",
                    paidLeaveAllowance = 25,
                    paidLeaveUsed = 13,
                    rttAllowance = 10,
                    rttUsed = 5
                ),
                User(
                    email = "walid@gmail.com",
                    matricule = "EMP-0003",
                    fullName = "walid elmzabite",
                    passwordHash = "MOHAMEDTAHA123",
                    jobTitle = "Lead Développeur Mobile",
                    department = "Ingénierie & IT",
                    phone = "0691366836",
                    hireDate = "15 Septembre 2019",
                    officeLocation = "Nouacer,l'atelier",
                    avatarUrl = "",
                    paidLeaveAllowance = 25,
                    paidLeaveUsed = 10,
                    rttAllowance = 10,
                    rttUsed = 4
                ),
                User(
                    email = "yassine@gmail.com",
                    matricule = "EMP-0004",
                    fullName = "Mohamed yasinne elmzabite",
                    passwordHash = "MOHAMEDTAHA123",
                    jobTitle = "Product Designer UI/UX",
                    department = "Design & Produit",
                    phone = "0691366836",
                    hireDate = "01 Juin 2020",
                    officeLocation = "Nouacer,l'atelier",
                    avatarUrl = "",
                    paidLeaveAllowance = 25,
                    paidLeaveUsed = 12,
                    rttAllowance = 10,
                    rttUsed = 6
                ),
                User(
                    email = "khadija@gmail.com",
                    matricule = "EMP-0005",
                    fullName = "khadija el ferrouni",
                    passwordHash = "MOHAMEDTAHA123",
                    jobTitle = "Responsable Marketing",
                    department = "Marketing & Com",
                    phone = "0691366836",
                    hireDate = "10 Janvier 2021",
                    officeLocation = "Nouacer,l'atelier",
                    avatarUrl = "",
                    paidLeaveAllowance = 25,
                    paidLeaveUsed = 7,
                    rttAllowance = 10,
                    rttUsed = 2
                ),
                User(
                    email = "bakr@gmail.com",
                    matricule = "EMP-0006",
                    fullName = "Bakr elmzabite",
                    passwordHash = "MOHAMEDTAHA123",
                    jobTitle = "Développeur Frontend",
                    department = "Ingénierie & IT",
                    phone = "0691366836",
                    hireDate = "05 Mai 2021",
                    officeLocation = "Nouacer,l'atelier",
                    avatarUrl = "",
                    paidLeaveAllowance = 25,
                    paidLeaveUsed = 5,
                    rttAllowance = 10,
                    rttUsed = 1
                ),
                User(
                    email = "ilyas@gmail.com",
                    matricule = "EMP-0007",
                    fullName = "ilyas elnnaqui",
                    passwordHash = "MOHAMEDTAHA123",
                    jobTitle = "Tech Lead Backend",
                    department = "Ingénierie & IT",
                    phone = "0691366836",
                    hireDate = "20 Novembre 2019",
                    officeLocation = "Nouacer,l'atelier",
                    avatarUrl = "",
                    paidLeaveAllowance = 25,
                    paidLeaveUsed = 15,
                    rttAllowance = 10,
                    rttUsed = 8
                )
            )

            for (seed in seedUsers) {
                val existing = userDao.getUserByEmail(seed.email)
                if (existing == null) {
                    userDao.insertUser(seed)
                    localStorage.saveUser(seed, setAsActiveSession = false)
                } else if (existing.avatarUrl.contains("pravatar.cc") || existing.avatarUrl.contains("unsplash.com")) {
                    // Reset placeholder avatar to empty
                    val cleaned = existing.copy(avatarUrl = "")
                    userDao.updateUser(cleaned)
                    localStorage.saveUser(cleaned, setAsActiveSession = false)
                }
            }

            // Clean active session if it has a placeholder avatar
            _currentUser.value?.let { current ->
                if (current.avatarUrl.contains("pravatar.cc") || current.avatarUrl.contains("unsplash.com")) {
                    val cleaned = current.copy(avatarUrl = "")
                    userDao.updateUser(cleaned)
                    localStorage.saveUser(cleaned, setAsActiveSession = true)
                    _currentUser.value = cleaned
                }
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
        if (cleanEmail.equals("elmzabitemohamedtaha@gmail.com", ignoreCase = true) &&
            (cleanPassword == "MOHAMEDTAHA123" || cleanPassword.equals("MOHAMEDTAHA123", ignoreCase = true))) {
            localStorage.setLastUsedEmail(cleanEmail)
            _loginError.value = null
            onAdminSuccess()
            return
        }

        viewModelScope.launch {
            val normalizedEmail = cleanEmail.lowercase()
            // 1. Search in Room SQLite Database
            var user = userDao.getUserByEmail(normalizedEmail)
            
            // 2. Fallback to Local Storage (SharedPreferences) if needed
            if (user == null) {
                user = localStorage.getUserByEmail(normalizedEmail)
                if (user != null) {
                    // Sync back to Room Database
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
                    // Save active session in Local Storage
                    localStorage.saveUser(user, setAsActiveSession = true)
                    localStorage.setLastUsedEmail(user.email)
                    refreshUsersList()
                    onSuccess()
                } else {
                    _loginError.value = "Mot de passe incorrect pour $cleanEmail. Vérifiez vos identifiants."
                }
            } else {
                _loginError.value = "Aucun compte trouvé pour $cleanEmail. Veuillez contacter l'administrateur RH."
            }
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

                // 3. SYNCHRONISATION CLOUD FIRESTORE POUR TOUS LES APPAREILS
                firestoreSync.uploadUser(newUser)

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

    fun updateUserProfile(user: User) {
        viewModelScope.launch {
            // Update in Room Database
            userDao.updateUser(user)
            // Update in Local Storage
            localStorage.saveUser(user, setAsActiveSession = true)
            // Update in Cloud Firestore
            firestoreSync.uploadUser(user)
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
            refreshUsersList()
            onResult(true, "Mot de passe modifié avec succès !")
        }
    }

    fun logout() {
        _currentUser.value = null
        _loginError.value = null
        _signUpError.value = null
        // Clear active session in Local Storage (credentials remain in DB and storage for future login)
        localStorage.clearActiveSession()
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
