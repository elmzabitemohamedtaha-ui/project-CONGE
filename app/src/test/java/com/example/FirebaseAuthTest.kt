package com.example

import com.example.data.FirebaseAuthResult
import com.example.ui.AuthViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FirebaseAuthTest {

    @Test
    fun firebaseAuthResult_errorHierarchy() {
        val errorResult = FirebaseAuthResult.Error("Identifiants incorrects", "INVALID_CREDENTIALS")
        assertTrue(errorResult is FirebaseAuthResult.Error)
        assertEquals("Identifiants incorrects", errorResult.message)
        assertEquals("INVALID_CREDENTIALS", errorResult.errorCode)
    }

    @Test
    fun firebaseAuthResult_fallbackLocalHierarchy() {
        val fallback = FirebaseAuthResult.FallbackLocal("Réseau indisponible")
        assertTrue(fallback is FirebaseAuthResult.FallbackLocal)
        assertEquals("Réseau indisponible", fallback.reason)
    }

    @Test
    fun passwordHash_consistency() {
        val pass1 = "MOHAMEDTAHA123"
        val hash1 = AuthViewModel.hashPassword(pass1)
        val hash2 = AuthViewModel.hashPassword(pass1)
        assertEquals(hash1, hash2)
        assertTrue(hash1.isNotEmpty())
    }
}
