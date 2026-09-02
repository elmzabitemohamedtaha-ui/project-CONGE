package com.example.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.LeaveRequestEntity
import com.example.data.User
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility service to generate and export leave request data in CSV format
 * formatted for seamless integration with Microsoft Excel, Google Sheets,
 * LibreOffice Calc, and HR/Payroll spreadsheet software.
 */
object CsvReportExporter {

    private val DATE_FORMAT = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE)
    private val FILE_DATE_FORMAT = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.FRANCE)

    /**
     * Escapes CSV field according to RFC 4180 standard.
     * Encloses strings with semicolons, commas, quotes, or newlines in double quotes.
     */
    private fun escapeCsv(value: String?): String {
        if (value == null) return ""
        var escaped = value.replace("\"", "\"\"")
        // If value contains semicolon, comma, quotes, newline, or carriage return, enclose in quotes
        if (escaped.contains(";") || escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n") || escaped.contains("\r")) {
            escaped = "\"$escaped\""
        }
        return escaped
    }

    /**
     * Translates status codes into friendly French terms for spreadsheet reporting.
     */
    private fun formatStatus(status: String): String {
        return when (status.uppercase()) {
            "APPROVED" -> "Approuvé"
            "REJECTED" -> "Refusé"
            "PENDING" -> "En attente"
            else -> status
        }
    }

    /**
     * Generates a CSV file from a list of leave requests.
     * Includes UTF-8 Byte Order Mark (BOM) to guarantee automatic encoding detection
     * and proper display of French accents (é, è, à, ç, etc.) in Excel.
     * Uses semicolon ';' as delimiter for optimal compatibility with European Excel locales.
     */
    fun exportLeaveRequestsToCsv(
        context: Context,
        requests: List<LeaveRequestEntity>,
        usersMap: Map<String, User> = emptyMap(),
        fileNamePrefix: String = "export_conges",
        periodLabel: String? = null
    ): File {
        val timestamp = FILE_DATE_FORMAT.format(Date())
        val fileName = "${fileNamePrefix}_$timestamp.csv"
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val csvFile = File(exportDir, fileName)

        val delimiter = ";"

        FileOutputStream(csvFile).use { fos ->
            // Write UTF-8 BOM so Excel opens accented characters seamlessly
            fos.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))

            OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->
                // Header row
                val headers = listOf(
                    "ID Demande",
                    "Matricule",
                    "Nom Collaborateur",
                    "Email Collaborateur",
                    "Département",
                    "Poste / Rôle",
                    "Type de Congé",
                    "Date Début",
                    "Date Fin",
                    "Nombre de Jours",
                    "Mois",
                    "Année",
                    "Statut",
                    "Motif de la Demande",
                    "Justificatif Joint",
                    "Date de Soumission",
                    "Date de Décision",
                    "Commentaire Direction / RH"
                )
                writer.write(headers.joinToString(delimiter) + "\r\n")

                val monthNames = arrayOf(
                    "Janvier", "Février", "Mars", "Avril", "Mai", "Juin",
                    "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre"
                )

                // Data rows
                for (req in requests) {
                    val user = usersMap[req.employeeEmail.lowercase().trim()]
                    val matricule = user?.matricule ?: "EMP-0000"
                    val jobTitle = user?.jobTitle ?: "Collaborateur"
                    val monthLabel = if (req.month in 0..11) monthNames[req.month] else "${req.month + 1}"
                    
                    val submissionDateStr = if (req.createdAt > 0) DATE_FORMAT.format(Date(req.createdAt)) else "-"
                    val decisionDateStr = if (req.decisionAt != null && req.decisionAt > 0) DATE_FORMAT.format(Date(req.decisionAt)) else "-"

                    val row = listOf(
                        escapeCsv(req.id),
                        escapeCsv(matricule),
                        escapeCsv(req.employeeName),
                        escapeCsv(req.employeeEmail),
                        escapeCsv(req.department),
                        escapeCsv(jobTitle),
                        escapeCsv(req.leaveType),
                        escapeCsv(req.startDate),
                        escapeCsv(req.endDate),
                        req.daysCount.toString(),
                        escapeCsv(monthLabel),
                        req.year.toString(),
                        escapeCsv(formatStatus(req.status)),
                        escapeCsv(req.reason),
                        escapeCsv(req.attachmentName ?: "Aucun"),
                        escapeCsv(submissionDateStr),
                        escapeCsv(decisionDateStr),
                        escapeCsv(req.adminComment ?: "")
                    )
                    writer.write(row.joinToString(delimiter) + "\r\n")
                }
                writer.flush()
            }
        }

        return csvFile
    }

    /**
     * Opens the CSV file with spreadsheet applications installed on the device
     * (Microsoft Excel, Google Sheets, WPS Office, LibreOffice, Files, etc.).
     */
    fun openCsv(context: Context, csvFile: File) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                csvFile
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "text/comma-separated-values")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            val chooser = Intent.createChooser(intent, "Ouvrir dans un tableur (Excel / Sheets)").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to text/csv or generic viewer
            try {
                val uri: Uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    csvFile
                )
                val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "text/csv")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                val chooser = Intent.createChooser(fallbackIntent, "Ouvrir le fichier CSV").apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(chooser)
            } catch (fallbackError: Exception) {
                Toast.makeText(context, "Impossible d'ouvrir le fichier CSV : ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Shares the CSV file via email, Slack, Teams, Google Drive, WhatsApp, etc.
     */
    fun shareCsv(context: Context, csvFile: File, subject: String = "Export des Données Congés (CSV)", messageText: String? = null) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                csvFile
            )

            val textContent = messageText ?: "Veuillez trouver ci-joint l'export CSV des demandes de congés pour traitement sur tableur (Excel, Google Sheets).\n\nFichier généré par l'application TimeOff."

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/comma-separated-values"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, textContent)
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }

            val chooser = Intent.createChooser(shareIntent, "Partager le fichier CSV").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Erreur lors du partage CSV : ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
}
