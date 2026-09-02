package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.data.FirestoreSyncManager
import com.example.ui.TimeOffApp
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize local notification channels
        com.example.service.LocalNotificationHelper.createNotificationChannels(applicationContext)

        // Start real-time multi-device cloud synchronization immediately
        FirestoreSyncManager.getInstance(applicationContext).startRealtimeListeners()
        
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                TimeOffApp()
            }
        }
    }
}


