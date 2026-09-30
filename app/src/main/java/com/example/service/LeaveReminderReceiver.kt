package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class LeaveReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val leaveType = intent.getStringExtra("leaveType") ?: "Congé"
        val dates = intent.getStringExtra("dates") ?: ""
        val employeeName = intent.getStringExtra("employeeName") ?: "Collaborateur"
        val requestId = intent.getStringExtra("requestId") ?: System.currentTimeMillis().toString()

        Log.d("LeaveReminder", "Alarm triggered for request: $requestId")

        LocalNotificationHelper.sendLeaveReminderNotification(
            context = context,
            requestId = requestId,
            employeeName = employeeName,
            leaveType = leaveType,
            dates = dates
        )
    }
}
