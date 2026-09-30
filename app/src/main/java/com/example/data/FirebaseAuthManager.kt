package com.example.data

import android.content.Context
import android.util.Log
import android.util.Patterns
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

sealed class FirebaseAuthResult {
    data class Success(val user: FirebaseUser, val isNewUser: Boolean = false) : FirebaseAuthResult()
    data class FallbackLocal(val reason: String) : FirebaseAuthResult()
    data class Error(val message: String, val errorCode: String? = null) : FirebaseAuthResult()
}

/**
 * Enterprise Firebase Authentication Manager.
 * 
 * Handles employee authentication with corporate and work emails via Firebase Auth,
 * providing password reset, account registration, session synchronization, and
 * resilient fallback to enterprise local storage.
 */
class FirebaseAuthManager private constructor(private val context: Context) {

    private var firebaseAuth: FirebaseAuth? = null

    private val _isConfigured = MutableStateFlow(false)
    val isConfigured: StateFlow<Boolean> = _isConfigured.asStateFlow()

    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _authStatusText = MutableStateFlow("Initialisation Firebase Auth...")
    val authStatusText: StateFlow<String> = _authStatusText.asStateFlow()

    init {
        initializeFirebaseAuth()
    }

    private fun initializeFirebaseAuth() {
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
                firebaseAuth = FirebaseAuth.getInstance()
                _isConfigured.value = true
                _currentUser.value = firebaseAuth?.currentUser
                _authStatusText.value = if (firebaseAuth?.currentUser != null) {
                    "Connecté: ${firebaseAuth?.currentUser?.email}"
                } else {
                    "Firebase Auth prêt pour connexion email professionnel"
                }

                firebaseAuth?.addAuthStateListener { auth ->
                    _currentUser.value = auth.currentUser
                }
                Log.d("FirebaseAuthManager", "Firebase Auth initialized successfully.")
            } else {
                _isConfigured.value = false
                _authStatusText.value = "Mode Hybride (Local & Cloud Sync)"
                Log.i("FirebaseAuthManager", "FirebaseApp not pre-configured; running in hybrid auth mode.")
            }
        } catch (e: Exception) {
            _isConfigured.value = false
            _authStatusText.value = "Mode Hybride: ${e.message}"
            Log.w("FirebaseAuthManager", "Firebase Auth init note: ${e.message}")
        }
    }

    /**
     * Checks whether an email matches work/business email criteria.
     */
    fun isWorkEmail(email: String): Boolean {
        val trimmed = email.trim()
        if (!Patterns.EMAIL_ADDRESS.matcher(trimmed).matches()) return false
        val parts = trimmed.split("@")
        if (parts.size != 2) return false
        val domain = parts[1].lowercase()
        return domain.contains(".") && domain.length >= 4
    }

    /**
     * Authenticate an employee using their work email and password.
     */
    suspend fun signInWithWorkEmail(email: String, password: String): FirebaseAuthResult {
        val cleanEmail = email.trim().lowercase()
        val cleanPassword = password.trim()

        if (cleanEmail.isBlank() || cleanPassword.isBlank()) {
            return FirebaseAuthResult.Error("Veuillez saisir votre adresse email professionnelle et votre mot de passe.")
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            return FirebaseAuthResult.Error("Format d'adresse email professionnelle invalide.")
        }

        val auth = firebaseAuth
        if (auth == null) {
            // Firebase Auth is not initialized in cloud project; proceed with local enterprise database
            return FirebaseAuthResult.FallbackLocal("Service Firebase Auth non connecté au cloud. Authentification locale activée.")
        }

        return try {
            val result = auth.signInWithEmailAndPassword(cleanEmail, cleanPassword).await()
            val user = result.user
            if (user != null) {
                _currentUser.value = user
                _authStatusText.value = "Connecté via Firebase Auth: ${user.email}"
                FirebaseAuthResult.Success(user = user, isNewUser = false)
            } else {
                FirebaseAuthResult.Error("Impossible de récupérer les informations de l'utilisateur.")
            }
        } catch (e: FirebaseAuthInvalidUserException) {
            // User does not exist yet in Firebase Auth
            FirebaseAuthResult.Error(
                message = "Aucun compte Firebase trouvé pour $cleanEmail. Vous pouvez créer votre compte professionnel ou vérifier votre email.",
                errorCode = "USER_NOT_FOUND"
            )
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            FirebaseAuthResult.Error(
                message = "Mot de passe incorrect pour le compte professionnel $cleanEmail.",
                errorCode = "INVALID_CREDENTIALS"
            )
        } catch (e: Exception) {
            Log.w("FirebaseAuthManager", "signInWithWorkEmail exception: ${e.message}", e)
            val msg = e.message ?: "Erreur d'authentification"
            // If offline or network unavailable, signal fallback to local enterprise database
            if (msg.contains("network", ignoreCase = true) ||
                msg.contains("timeout", ignoreCase = true) ||
                msg.contains("unreachable", ignoreCase = true) ||
                msg.contains("failed to connect", ignoreCase = true) ||
                msg.contains("SERVICE_NOT_AVAILABLE", ignoreCase = true)
            ) {
                FirebaseAuthResult.FallbackLocal("Réseau indisponible. Connexion via la base locale d'entreprise.")
            } else {
                FirebaseAuthResult.Error(
                    message = "Erreur Firebase Auth: ${e.localizedMessage ?: msg}",
                    errorCode = e.javaClass.simpleName
                )
            }
        }
    }

    /**
     * Registers a new employee with their work email and password.
     */
    suspend fun createWorkEmailAccount(
        email: String,
        password: String,
        displayName: String
    ): FirebaseAuthResult {
        val cleanEmail = email.trim().lowercase()
        val cleanPassword = password.trim()
        val cleanName = displayName.trim()

        if (!isWorkEmail(cleanEmail)) {
            return FirebaseAuthResult.Error("Veuillez renseigner une adresse email professionnelle valide.")
        }

        if (cleanPassword.length < 6) {
            return FirebaseAuthResult.Error("Le mot de passe doit contenir au moins 6 caractères pour des raisons de sécurité.")
        }

        val auth = firebaseAuth
        if (auth == null) {
            return FirebaseAuthResult.FallbackLocal("Firebase Auth non initialisé. Création locale uniquement.")
        }

        return try {
            val result = auth.createUserWithEmailAndPassword(cleanEmail, cleanPassword).await()
            val user = result.user
            if (user != null) {
                if (cleanName.isNotBlank()) {
                    try {
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName(cleanName)
                            .build()
                        user.updateProfile(profileUpdates).await()
                    } catch (pe: Exception) {
                        Log.w("FirebaseAuthManager", "Failed to update displayName: ${pe.message}")
                    }
                }
                _currentUser.value = user
                FirebaseAuthResult.Success(user = user, isNewUser = true)
            } else {
                FirebaseAuthResult.Error("Échec de la création du compte.")
            }
        } catch (e: FirebaseAuthUserCollisionException) {
            FirebaseAuthResult.Error("Un compte avec cette adresse email existe déjà. Connectez-vous directement.")
        } catch (e: FirebaseAuthWeakPasswordException) {
            FirebaseAuthResult.Error("Le mot de passe est trop faible. Utilisez des lettres et des chiffres.")
        } catch (e: Exception) {
            FirebaseAuthResult.Error("Erreur d'inscription: ${e.localizedMessage ?: e.message}")
        }
    }

    /**
     * Sends a password reset email to the employee's work email address.
     */
    suspend fun sendPasswordResetEmail(email: String): Result<String> {
        val cleanEmail = email.trim().lowercase()
        if (!Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            return Result.failure(IllegalArgumentException("Format d'adresse email invalide."))
        }

        val auth = firebaseAuth
        if (auth == null) {
            return Result.failure(IllegalStateException("FIREBASE_NOT_CONFIGURED"))
        }

        return try {
            auth.sendPasswordResetEmail(cleanEmail).await()
            Result.success("Un email de réinitialisation a été envoyé avec succès à $cleanEmail.")
        } catch (e: Exception) {
            Log.w("FirebaseAuthManager", "Password reset failed: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Signs out the currently authenticated user from Firebase Auth.
     */
    fun signOut() {
        try {
            firebaseAuth?.signOut()
            _currentUser.value = null
            _authStatusText.value = "Déconnecté de Firebase Auth"
        } catch (e: Exception) {
            Log.w("FirebaseAuthManager", "SignOut warning: ${e.message}")
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: FirebaseAuthManager? = null

        fun getInstance(context: Context): FirebaseAuthManager {
            return INSTANCE ?: synchronized(this) {
                val instance = FirebaseAuthManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
