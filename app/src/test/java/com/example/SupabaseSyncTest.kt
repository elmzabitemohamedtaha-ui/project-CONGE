package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.SupabaseSyncManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SupabaseSyncTest {

    @Test
    fun supabaseSqlSchema_containsRequiredTablesAndPolicies() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = SupabaseSyncManager.getInstance(context)
        val sql = manager.getSupabaseSqlSchema()

        assertNotNull(sql)
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS public.leave_requests"))
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS public.users"))
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS public.app_alerts"))
        assertTrue(sql.contains("ENABLE ROW LEVEL SECURITY"))
        assertTrue(sql.contains("Allow public access to leave_requests"))
    }

    @Test
    fun supabaseEmployeeIsolatedSqlSchema_containsProfilesAndIsolatedRls() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = SupabaseSyncManager.getInstance(context)
        val sql = manager.getEmployeeIsolatedSqlSchema()

        assertNotNull(sql)
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS public.profiles"))
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS public.leave_requests"))
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS public.leave_balance_history"))
        assertTrue(sql.contains("REFERENCES auth.users(id)"))
        assertTrue(sql.contains("REFERENCES public.profiles(id)"))
        assertTrue(sql.contains("ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY"))
        assertTrue(sql.contains("ALTER TABLE public.leave_requests ENABLE ROW LEVEL SECURITY"))
        assertTrue(sql.contains("ALTER TABLE public.leave_balance_history ENABLE ROW LEVEL SECURITY"))
        assertTrue(sql.contains("auth.uid() = user_id"))
        assertTrue(sql.contains("is_hr_admin()"))
    }

    @Test
    fun supabaseManager_configurationProperties() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = SupabaseSyncManager.getInstance(context)

        manager.supabaseUrl = "https://example-project.supabase.co"
        manager.supabaseAnonKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.dummykey12345678901234567890"

        assertTrue(manager.isConfigured.value)
        assertTrue(manager.supabaseUrl.contains("example-project.supabase.co"))

        // Reset
        manager.supabaseUrl = ""
        manager.supabaseAnonKey = ""
        assertFalse(manager.isConfigured.value)
    }

    @Test
    fun supabaseManager_autoSyncToggle() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = SupabaseSyncManager.getInstance(context)

        manager.isAutoSyncEnabled = false
        assertFalse(manager.isAutoSyncEnabled)

        manager.isAutoSyncEnabled = true
        assertTrue(manager.isAutoSyncEnabled)
    }
}
