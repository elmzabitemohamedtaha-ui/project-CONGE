package com.example.util

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

sealed class BiometricStatus {
    object Ready : BiometricStatus()
    data class NotEnrolled(val message: String) : BiometricStatus()
    data class Unavailable(val message: String) : BiometricStatus()
}

object BiometricAuthHelper {

    /**
     * Checks if biometric authentication can be performed on this device.
     */
    fun checkBiometricStatus(context: Context): BiometricStatus {
        val biometricManager = BiometricManager.from(context)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
        return when (biometricManager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.Ready
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NotEnrolled(
                "Aucune empreinte digitale ou visage n'est enregistré sur cet appareil."
            )
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricStatus.Unavailable(
                "Votre appareil ne dispose pas de capteur biométrique."
            )
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricStatus.Unavailable(
                "Le capteur biométrique est temporairement indisponible."
            )
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> BiometricStatus.Unavailable(
                "Une mise à jour de sécurité est requise pour utiliser la biométrie."
            )
            else -> BiometricStatus.Unavailable(
                "L'authentification biométrique n'est pas disponible sur cet appareil."
            )
        }
    }

    /**
     * Returns true if biometric hardware is ready and enrolled.
     */
    fun isBiometricAvailable(context: Context): Boolean {
        return checkBiometricStatus(context) is BiometricStatus.Ready
    }

    /**
     * Launches the system BiometricPrompt modal.
     */
    fun authenticate(
        activity: FragmentActivity,
        title: String = "Connexion Biométrique",
        subtitle: String = "Accès sécurisé TimeOff",
        description: String = "Veuillez confirmer votre identité pour déverrouiller vos dossiers de congés confidentiels.",
        negativeButtonText: String = "Annuler",
        onSuccess: () -> Unit,
        onError: (errorCode: Int, errString: String) -> Unit,
        onFailed: () -> Unit = {}
    ) {
        val executor = ContextCompat.getMainExecutor(activity)

        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onError(errorCode, errString.toString())
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onFailed()
                }
            }
        )

        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setDescription(description)
            .setNegativeButtonText(negativeButtonText)
            .setAllowedAuthenticators(authenticators)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}
