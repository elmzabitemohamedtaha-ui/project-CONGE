package com.example.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class EmailNotification(
    val id: String = UUID.randomUUID().toString(),
    val toEmail: String,
    val recipientName: String,
    val subject: String,
    val content: String,
    val sentAt: String,
    val isApproved: Boolean,
    val status: EmailStatus = EmailStatus.SENT
)

enum class EmailStatus {
    SENT,
    DELIVERED,
    FAILED
}

object EmailNotificationService {
    private val _sentEmails = MutableStateFlow<List<EmailNotification>>(emptyList())
    val sentEmails: StateFlow<List<EmailNotification>> = _sentEmails.asStateFlow()

    fun sendLeaveStatusEmail(
        employeeName: String,
        employeeEmail: String,
        leaveType: String,
        dates: String,
        isApproved: Boolean,
        adminComment: String = ""
    ): EmailNotification {
        val decisionText = if (isApproved) "APPROUVÉE" else "REFUSÉE"
        val subject = "[TimeOff] Votre demande de $leaveType a été $decisionText"
        val timestamp = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())

        val content = buildString {
            append("Bonjour $employeeName,\n\n")
            append("Votre demande d'absence a été traitée par l'administrateur.\n\n")
            append("• Type d'absence : $leaveType\n")
            append("• Période : $dates\n")
            append("• Statut : $decisionText\n")
            if (adminComment.isNotBlank()) {
                append("• Motif / Commentaire : $adminComment\n")
            }
            append("\nCe message a été généré automatiquement par le système TimeOff.\n")
            append("Service Ressources Humaines")
        }

        val notification = EmailNotification(
            toEmail = employeeEmail,
            recipientName = employeeName,
            subject = subject,
            content = content,
            sentAt = timestamp,
            isApproved = isApproved,
            status = EmailStatus.DELIVERED
        )

        _sentEmails.value = listOf(notification) + _sentEmails.value
        return notification
    }

    fun sendNewRequestEmailToManager(
        employeeName: String,
        leaveType: String,
        dates: String,
        reason: String
    ): EmailNotification {
        val managerEmail = "manager@entreprise.com"
        val subject = "[TimeOff - Action Requise] Nouvelle demande de $leaveType de $employeeName"
        val timestamp = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())

        val content = buildString {
            append("Bonjour,\n\n")
            append("Une nouvelle demande de congés a été soumise par $employeeName et requiert votre validation.\n\n")
            append("• Employé(e) : $employeeName\n")
            append("• Type d'absence : $leaveType\n")
            append("• Période : $dates\n")
            if (reason.isNotBlank()) {
                append("• Motif : $reason\n")
            }
            append("\nVeuillez vous connecter à l'application TimeOff pour approuver ou refuser cette demande.\n")
            append("\nCe message a été généré automatiquement par le système TimeOff.")
        }

        val notification = EmailNotification(
            toEmail = managerEmail,
            recipientName = "Manager / RH",
            subject = subject,
            content = content,
            sentAt = timestamp,
            isApproved = false, // Pending state
            status = EmailStatus.DELIVERED
        )

        _sentEmails.value = listOf(notification) + _sentEmails.value
        return notification
    }

    fun openEmailClient(context: Context, emailNotification: EmailNotification) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:${emailNotification.toEmail}")
                putExtra(Intent.EXTRA_SUBJECT, emailNotification.subject)
                putExtra(Intent.EXTRA_TEXT, emailNotification.content)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            // In case no email client is available
        }
    }
}
