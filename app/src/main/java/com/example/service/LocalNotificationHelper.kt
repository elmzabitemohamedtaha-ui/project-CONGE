package com.example.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R

/**
 * Helper object managing Android Local System Notifications for Leave Request status updates.
 */
object LocalNotificationHelper {

    const val CHANNEL_ID_LEAVE_STATUS = "timeoff_leave_status_channel"
    const val CHANNEL_NAME_LEAVE_STATUS = "Statut des Congés & Absences"
    const val CHANNEL_DESC_LEAVE_STATUS = "Notifications instantanées lors de l'approbation ou du refus de vos demandes de congés."

    const val CHANNEL_ID_NEW_REQUESTS = "timeoff_new_requests_channel"
    const val CHANNEL_NAME_NEW_REQUESTS = "Nouvelles Demandes de Congés (Admin)"
    const val CHANNEL_DESC_NEW_REQUESTS = "Alertes instantanées reçues par le manager lorsqu'un employé soumet une demande depuis un autre téléphone."

    const val EXTRA_NOTIFICATION_CLICKED = "EXTRA_NOTIFICATION_CLICKED"
    const val EXTRA_REQUEST_ID = "EXTRA_REQUEST_ID"
    const val EXTRA_STATUS = "EXTRA_STATUS"

    /**
     * Initializes the notification channels on Android 8.0+ (API 26+).
     */
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            val channelStatus = NotificationChannel(
                CHANNEL_ID_LEAVE_STATUS,
                CHANNEL_NAME_LEAVE_STATUS,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC_LEAVE_STATUS
                enableLights(true)
                lightColor = Color.BLUE
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 150, 300)
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }

            val channelNewRequests = NotificationChannel(
                CHANNEL_ID_NEW_REQUESTS,
                CHANNEL_NAME_NEW_REQUESTS,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC_NEW_REQUESTS
                enableLights(true)
                lightColor = Color.GREEN
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 400, 200, 400)
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }

            notificationManager.createNotificationChannel(channelStatus)
            notificationManager.createNotificationChannel(channelNewRequests)
        }
    }

    /**
     * Sends a rich notification to the manager when a new request is received from another phone.
     */
    fun sendNewLeaveRequestNotification(
        context: Context,
        requestId: String,
        employeeName: String,
        leaveType: String,
        dates: String,
        department: String,
        reason: String
    ) {
        createNotificationChannels(context)

        val title = "📩 Nouvelle demande de $leaveType"
        val shortMessage = "$employeeName ($department) a soumis une demande pour $dates."
        val expandedMessage = buildString {
            append("Nouvelle demande de congé reçue :\n\n")
            append("• Collaborateur : $employeeName\n")
            append("• Département : $department\n")
            append("• Type : $leaveType\n")
            append("• Période : $dates\n")
            if (reason.isNotBlank()) {
                append("• Motif : $reason\n")
            }
            append("\nAppuyez pour examiner et valider dans l'espace Administrateur.")
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_NOTIFICATION_CLICKED, true)
            putExtra(EXTRA_REQUEST_ID, requestId)
            putExtra(EXTRA_STATUS, "NEW_REQUEST")
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            requestId.hashCode() + 1000,
            intent,
            flags
        )

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_NEW_REQUESTS)
            .setSmallIcon(R.drawable.ic_notification_leave)
            .setContentTitle(title)
            .setContentText(shortMessage)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(expandedMessage)
                    .setBigContentTitle(title)
                    .setSummaryText("Demande en attente")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setColor(0xFF2563EB.toInt())
            .setColorized(true)
            .setSound(defaultSoundUri)
            .setVibrate(longArrayOf(0, 400, 200, 400))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationId = requestId.hashCode() + 1000
        val notificationManager = NotificationManagerCompat.from(context)

        try {
            if (hasNotificationPermission(context)) {
                notificationManager.notify(notificationId, builder.build())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Checks if notification permission is granted.
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    /**
     * Sends a rich local Android system notification when a leave request is approved or rejected.
     */
    fun sendLeaveStatusNotification(
        context: Context,
        requestId: String,
        employeeName: String,
        leaveType: String,
        dates: String,
        isApproved: Boolean,
        adminComment: String = ""
    ) {
        createNotificationChannels(context)

        val statusTitle = if (isApproved) {
            "✅ Demande de $leaveType APPROUVÉE"
        } else {
            "❌ Demande de $leaveType REFUSÉE"
        }

        val shortMessage = if (isApproved) {
            "Votre demande pour la période $dates a été acceptée par l'administrateur."
        } else {
            "Votre demande pour la période $dates n'a pas été retenue."
        }

        val expandedMessage = buildString {
            append("Bonjour $employeeName,\n\n")
            if (isApproved) {
                append("Bonne nouvelle ! Votre responsable a validé votre demande de $leaveType.")
            } else {
                append("Votre demande de $leaveType pour la période $dates a été refusée.")
            }
            append("\n• Période : $dates")
            if (adminComment.isNotBlank()) {
                append("\n• Motif / Commentaire : $adminComment")
            }
            append("\n\nConsultez l'application TimeOff pour voir vos soldes à jour.")
        }

        // Tap intent to launch the app
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_NOTIFICATION_CLICKED, true)
            putExtra(EXTRA_REQUEST_ID, requestId)
            putExtra(EXTRA_STATUS, if (isApproved) "APPROVED" else "REJECTED")
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            requestId.hashCode(),
            intent,
            flags
        )

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val accentColor = if (isApproved) 0xFF10B981.toInt() else 0xFFEF4444.toInt()

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_LEAVE_STATUS)
            .setSmallIcon(R.drawable.ic_notification_leave)
            .setContentTitle(statusTitle)
            .setContentText(shortMessage)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(expandedMessage)
                    .setBigContentTitle(statusTitle)
                    .setSummaryText(if (isApproved) "Validation RH" else "Refus RH")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setColor(accentColor)
            .setColorized(true)
            .setSound(defaultSoundUri)
            .setVibrate(longArrayOf(0, 300, 150, 300))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationId = requestId.hashCode()
        val notificationManager = NotificationManagerCompat.from(context)

        try {
            if (hasNotificationPermission(context)) {
                notificationManager.notify(notificationId, builder.build())
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Sends a test system notification to verify alerts functionality.
     */
    fun sendTestNotification(context: Context, isApproved: Boolean = true) {
        val testId = "test_${System.currentTimeMillis()}"
        sendLeaveStatusNotification(
            context = context,
            requestId = testId,
            employeeName = "Collaborateur",
            leaveType = if (isApproved) "Congés payés" else "RTT",
            dates = "15/10/2026 - 22/10/2026",
            isApproved = isApproved,
            adminComment = if (isApproved) "Test de notification locale réussi !" else "Test de refus pour vérification des alertes."
        )
    }
}
