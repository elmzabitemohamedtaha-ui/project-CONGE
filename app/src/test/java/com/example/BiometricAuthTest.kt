package com.example

import com.example.util.BiometricStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BiometricAuthTest {

    @Test
    fun biometricStatus_readyBehavior() {
        val status = BiometricStatus.Ready
        assertTrue(status is BiometricStatus.Ready)
    }

    @Test
    fun biometricStatus_notEnrolledContainsMessage() {
        val msg = "Aucune empreinte digitale enregistrée."
        val status = BiometricStatus.NotEnrolled(msg)
        assertEquals(msg, status.message)
    }

    @Test
    fun biometricStatus_unavailableContainsMessage() {
        val msg = "Matériel biométrique non disponible."
        val status = BiometricStatus.Unavailable(msg)
        assertEquals(msg, status.message)
    }
}
