package com.example.service

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.LeaveRequestEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Service dedicated to generating official, high-quality vector PDF reports
 * of monthly approved leave requests for managers and HR administrators.
 */
object PdfReportGenerator {

    private val MONTH_NAMES_FR = arrayOf(
        "Janvier", "Février", "Mars", "Avril", "Mai", "Juin",
        "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre"
    )

    data class ReportStatistics(
        val totalRequests: Int,
        val totalDays: Int,
        val uniqueEmployeesCount: Int,
        val byType: Map<String, Int>,
        val byDepartment: Map<String, Int>
    )

    /**
     * Calculates summary metrics for the given approved requests list.
     */
    fun calculateStatistics(requests: List<LeaveRequestEntity>): ReportStatistics {
        val totalRequests = requests.size
        val totalDays = requests.sumOf { it.daysCount }
        val uniqueEmployees = requests.map { it.employeeEmail.lowercase().trim() }.distinct().size

        val byType = requests.groupBy { it.leaveType }
            .mapValues { entry -> entry.value.sumOf { it.daysCount } }

        val byDept = requests.groupBy { it.department.ifBlank { "Général" } }
            .mapValues { entry -> entry.value.sumOf { it.daysCount } }

        return ReportStatistics(
            totalRequests = totalRequests,
            totalDays = totalDays,
            uniqueEmployeesCount = uniqueEmployees,
            byType = byType,
            byDepartment = byDept
        )
    }

    /**
     * Generates a multi-page PDF document for monthly approved leaves.
     */
    fun generateMonthlyLeaveReport(
        context: Context,
        month: Int, // 0-11
        year: Int,
        departmentFilter: String? = null,
        approvedRequests: List<LeaveRequestEntity>,
        managerName: String = "Responsable RH"
    ): File {
        val monthLabel = if (month in 0..11) MONTH_NAMES_FR[month] else "Mois $month"
        val filteredRequests = if (!departmentFilter.isNullOrBlank() && departmentFilter != "Tous" && departmentFilter != "Tous les départements") {
            approvedRequests.filter { it.department.equals(departmentFilter, ignoreCase = true) }
        } else {
            approvedRequests
        }.sortedWith(compareBy({ it.department }, { it.startDay }, { it.employeeName }))

        val stats = calculateStatistics(filteredRequests)

        // A4 page dimensions in points (72 points per inch)
        val pageWidth = 595
        val pageHeight = 842

        val document = PdfDocument()

        // Setup Paints
        val primaryColor = Color.rgb(15, 23, 42) // Slate 900
        val secondaryColor = Color.rgb(37, 99, 235) // Blue 600
        val emeraldColor = Color.rgb(16, 185, 129) // Emerald 500
        val darkTextColor = Color.rgb(30, 41, 59) // Slate 800
        val lightTextColor = Color.rgb(100, 116, 139) // Slate 500
        val borderLightColor = Color.rgb(226, 232, 240) // Slate 200
        val zebraRowColor = Color.rgb(248, 250, 252) // Slate 50

        val textPaint = Paint().apply {
            isAntiAlias = true
            color = darkTextColor
            textSize = 10f
        }

        val boldPaint = Paint().apply {
            isAntiAlias = true
            color = darkTextColor
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 10f
        }

        val fillPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
        }

        val strokePaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 1f
            color = borderLightColor
        }

        val rowsPerPage = 14
        val chunks = if (filteredRequests.isEmpty()) {
            listOf(emptyList())
        } else {
            filteredRequests.chunked(rowsPerPage)
        }
        val totalPages = chunks.size

        val genDateFormatted = SimpleDateFormat("dd/MM/yyyy à HH:mm", Locale.FRANCE).format(Date())

        chunks.forEachIndexed { pageIndex, pageRequests ->
            val pageNumber = pageIndex + 1
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            val margin = 36f
            val contentWidth = pageWidth - (margin * 2)

            // 1. TOP HEADER BANNER
            fillPaint.color = primaryColor
            val headerRect = RectF(margin, margin, margin + contentWidth, margin + 82f)
            canvas.drawRoundRect(headerRect, 8f, 8f, fillPaint)

            // Accent left bar
            fillPaint.color = secondaryColor
            canvas.drawRoundRect(RectF(margin, margin, margin + 6f, margin + 82f), 8f, 8f, fillPaint)

            // Header Title
            boldPaint.color = Color.WHITE
            boldPaint.textSize = 15f
            canvas.drawText("TIMEOFF • RAPPORT MENSUEL DES CONGÉS VALIDÉS", margin + 18f, margin + 28f, boldPaint)

            // Header Subtitle
            textPaint.color = Color.rgb(203, 213, 225) // Slate 300
            textPaint.textSize = 10.5f
            val deptLabel = if (departmentFilter.isNullOrBlank() || departmentFilter.startsWith("Tous")) "Tous départements" else "Département $departmentFilter"
            canvas.drawText("Période : $monthLabel $year  •  Périmètre : $deptLabel", margin + 18f, margin + 46f, textPaint)

            // Generation Meta
            textPaint.textSize = 8.5f
            textPaint.color = Color.rgb(148, 163, 184)
            canvas.drawText("Émis le $genDateFormatted par $managerName  •  Statut : Document Officiel RH", margin + 18f, margin + 66f, textPaint)

            var currentY = margin + 96f

            // 2. KEY METRICS CARDS (Only on Page 1)
            if (pageNumber == 1) {
                val cardSpacing = 8f
                val cardWidth = (contentWidth - (cardSpacing * 3)) / 4f
                val cardHeight = 52f

                // Card 1: Total Requests
                drawKpiCard(
                    canvas = canvas,
                    x = margin,
                    y = currentY,
                    width = cardWidth,
                    height = cardHeight,
                    title = "DEMANDES VALIDÉES",
                    value = "${stats.totalRequests}",
                    subtext = "Validations RH",
                    accentColor = secondaryColor,
                    fillPaint = fillPaint,
                    strokePaint = strokePaint,
                    textPaint = textPaint,
                    boldPaint = boldPaint
                )

                // Card 2: Total Days
                drawKpiCard(
                    canvas = canvas,
                    x = margin + cardWidth + cardSpacing,
                    y = currentY,
                    width = cardWidth,
                    height = cardHeight,
                    title = "TOTAL JOURS POSÉS",
                    value = "${stats.totalDays} j",
                    subtext = "Cumul du mois",
                    accentColor = emeraldColor,
                    fillPaint = fillPaint,
                    strokePaint = strokePaint,
                    textPaint = textPaint,
                    boldPaint = boldPaint
                )

                // Card 3: Employees
                drawKpiCard(
                    canvas = canvas,
                    x = margin + (cardWidth + cardSpacing) * 2,
                    y = currentY,
                    width = cardWidth,
                    height = cardHeight,
                    title = "COLLABORATEURS",
                    value = "${stats.uniqueEmployeesCount}",
                    subtext = "Effectif concerné",
                    accentColor = Color.rgb(139, 92, 246), // Violet
                    fillPaint = fillPaint,
                    strokePaint = strokePaint,
                    textPaint = textPaint,
                    boldPaint = boldPaint
                )

                // Card 4: Top Leave Type
                val topType = stats.byType.maxByOrNull { it.value }
                val topTypeText = if (topType != null) "${topType.key} (${topType.value}j)" else "N/A"
                drawKpiCard(
                    canvas = canvas,
                    x = margin + (cardWidth + cardSpacing) * 3,
                    y = currentY,
                    width = cardWidth,
                    height = cardHeight,
                    title = "TYPE PRINCIPAL",
                    value = topType?.key?.take(12) ?: "Aucun",
                    subtext = if (topType != null) "${topType.value} j posés" else "-",
                    accentColor = Color.rgb(245, 158, 11), // Amber
                    fillPaint = fillPaint,
                    strokePaint = strokePaint,
                    textPaint = textPaint,
                    boldPaint = boldPaint
                )

                currentY += cardHeight + 14f

                // Mini summary bar: Type distribution pills
                if (stats.byType.isNotEmpty()) {
                    fillPaint.color = Color.rgb(241, 245, 249) // Slate 100
                    canvas.drawRoundRect(RectF(margin, currentY, margin + contentWidth, currentY + 24f), 6f, 6f, fillPaint)

                    boldPaint.color = darkTextColor
                    boldPaint.textSize = 8.5f
                    canvas.drawText("RÉPARTITION PAR MOTIF :", margin + 8f, currentY + 15f, boldPaint)

                    var pillX = margin + 140f
                    textPaint.textSize = 8f
                    stats.byType.forEach { (type, days) ->
                        if (pillX < margin + contentWidth - 80f) {
                            fillPaint.color = Color.WHITE
                            val label = "$type: ${days}j"
                            val labelWidth = textPaint.measureText(label) + 12f
                            canvas.drawRoundRect(RectF(pillX, currentY + 4f, pillX + labelWidth, currentY + 20f), 4f, 4f, fillPaint)
                            strokePaint.color = borderLightColor
                            canvas.drawRoundRect(RectF(pillX, currentY + 4f, pillX + labelWidth, currentY + 20f), 4f, 4f, strokePaint)

                            textPaint.color = darkTextColor
                            canvas.drawText(label, pillX + 6f, currentY + 15f, textPaint)
                            pillX += labelWidth + 6f
                        }
                    }
                    currentY += 32f
                }
            }

            // 3. TABLE SECTION
            boldPaint.color = darkTextColor
            boldPaint.textSize = 11f
            val tableTitle = if (totalPages > 1) "DÉTAIL DES ABSENCES VALIDÉES (Page $pageNumber / $totalPages)" else "DÉTAIL DES ABSENCES VALIDÉES"
            canvas.drawText(tableTitle, margin, currentY + 2f, boldPaint)
            currentY += 10f

            // Table Columns Definition
            // Total content width = contentWidth (~523pt)
            val colNumW = 24f
            val colEmpW = 120f
            val colDeptW = 75f
            val colTypeW = 85f
            val colDatesW = 105f
            val colDaysW = 40f
            val colStatusW = contentWidth - (colNumW + colEmpW + colDeptW + colTypeW + colDatesW + colDaysW) // ~74pt

            val colX0 = margin
            val colX1 = colX0 + colNumW
            val colX2 = colX1 + colEmpW
            val colX3 = colX2 + colDeptW
            val colX4 = colX3 + colTypeW
            val colX5 = colX4 + colDatesW
            val colX6 = colX5 + colDaysW

            val tableHeaderHeight = 22f

            // Table Header Background
            fillPaint.color = Color.rgb(241, 245, 249) // Slate 100
            canvas.drawRect(margin, currentY, margin + contentWidth, currentY + tableHeaderHeight, fillPaint)

            // Table Header Border
            strokePaint.color = borderLightColor
            canvas.drawRect(margin, currentY, margin + contentWidth, currentY + tableHeaderHeight, strokePaint)

            // Table Header Labels
            boldPaint.color = Color.rgb(71, 85, 105) // Slate 600
            boldPaint.textSize = 8f

            canvas.drawText("N°", colX0 + 4f, currentY + 14f, boldPaint)
            canvas.drawText("COLLABORATEUR", colX1 + 4f, currentY + 14f, boldPaint)
            canvas.drawText("DÉPARTEMENT", colX2 + 4f, currentY + 14f, boldPaint)
            canvas.drawText("TYPE DE CONGÉ", colX3 + 4f, currentY + 14f, boldPaint)
            canvas.drawText("PÉRIODE ACCORDÉE", colX4 + 4f, currentY + 14f, boldPaint)
            canvas.drawText("DURÉE", colX5 + 4f, currentY + 14f, boldPaint)
            canvas.drawText("STATUT", colX6 + 4f, currentY + 14f, boldPaint)

            currentY += tableHeaderHeight

            // Table Data Rows
            val rowHeight = 26f

            if (pageRequests.isEmpty()) {
                // Empty state message in table
                fillPaint.color = Color.WHITE
                canvas.drawRect(margin, currentY, margin + contentWidth, currentY + 40f, fillPaint)
                strokePaint.color = borderLightColor
                canvas.drawRect(margin, currentY, margin + contentWidth, currentY + 40f, strokePaint)

                textPaint.color = lightTextColor
                textPaint.textSize = 9.5f
                canvas.drawText("Aucune demande de congé approuvée enregistrée pour ce mois / filtre.", margin + 20f, currentY + 24f, textPaint)
                currentY += 40f
            } else {
                pageRequests.forEachIndexed { rowIndex, req ->
                    val globalIndex = (pageIndex * rowsPerPage) + rowIndex + 1
                    val isEven = rowIndex % 2 == 0

                    // Row background
                    fillPaint.color = if (isEven) Color.WHITE else zebraRowColor
                    canvas.drawRect(margin, currentY, margin + contentWidth, currentY + rowHeight, fillPaint)

                    // Row bottom border
                    strokePaint.color = borderLightColor
                    canvas.drawLine(margin, currentY + rowHeight, margin + contentWidth, currentY + rowHeight, strokePaint)

                    // Index
                    textPaint.color = lightTextColor
                    textPaint.textSize = 8f
                    canvas.drawText("$globalIndex", colX0 + 4f, currentY + 16f, textPaint)

                    // Employee Name & Email
                    boldPaint.color = darkTextColor
                    boldPaint.textSize = 8.5f
                    val cleanName = req.employeeName.take(18)
                    canvas.drawText(cleanName, colX1 + 4f, currentY + 12f, boldPaint)

                    textPaint.color = lightTextColor
                    textPaint.textSize = 7f
                    val cleanEmail = req.employeeEmail.take(22)
                    canvas.drawText(cleanEmail, colX1 + 4f, currentY + 22f, textPaint)

                    // Department
                    textPaint.color = darkTextColor
                    textPaint.textSize = 8f
                    canvas.drawText(req.department.take(14), colX2 + 4f, currentY + 16f, textPaint)

                    // Leave Type
                    boldPaint.color = when (req.leaveType) {
                        "Congés payés" -> secondaryColor
                        "RTT" -> Color.rgb(139, 92, 246)
                        "Maladie" -> Color.rgb(225, 29, 72)
                        "Télétravail" -> Color.rgb(13, 148, 136)
                        else -> darkTextColor
                    }
                    boldPaint.textSize = 8f
                    canvas.drawText(req.leaveType.take(15), colX3 + 4f, currentY + 16f, boldPaint)

                    // Period (Dates)
                    textPaint.color = darkTextColor
                    textPaint.textSize = 8f
                    canvas.drawText("${req.startDate} → ${req.endDate}", colX4 + 4f, currentY + 16f, textPaint)

                    // Duration (Days)
                    boldPaint.color = darkTextColor
                    boldPaint.textSize = 8.5f
                    canvas.drawText("${req.daysCount} j", colX5 + 6f, currentY + 16f, boldPaint)

                    // Status Badge (Green pill)
                    val badgeW = colStatusW - 12f
                    val badgeH = 14f
                    val badgeX = colX6 + 4f
                    val badgeY = currentY + 6f

                    fillPaint.color = Color.rgb(220, 252, 231) // Green 100
                    canvas.drawRoundRect(RectF(badgeX, badgeY, badgeX + badgeW, badgeY + badgeH), 4f, 4f, fillPaint)

                    boldPaint.color = Color.rgb(22, 101, 52) // Green 800
                    boldPaint.textSize = 7.5f
                    canvas.drawText("VALIDÉ", badgeX + 8f, badgeY + 10.5f, boldPaint)

                    currentY += rowHeight
                }
            }

            // Table Outer Border
            strokePaint.color = borderLightColor
            canvas.drawRect(margin, margin + 96f + (if (pageNumber == 1) 52f + 14f + (if (stats.byType.isNotEmpty()) 32f else 0f) else 0f) + 10f, margin + contentWidth, currentY, strokePaint)

            // 4. SIGNATURE & VALIDATION BLOCK (On last page if space allows)
            if (pageNumber == totalPages) {
                currentY = pageHeight - margin - 80f

                val sigBoxW = (contentWidth - 20f) / 2f
                val sigBoxH = 54f

                // Left: HR / Manager Signature Box
                fillPaint.color = Color.rgb(248, 250, 252)
                canvas.drawRoundRect(RectF(margin, currentY, margin + sigBoxW, currentY + sigBoxH), 6f, 6f, fillPaint)
                strokePaint.color = borderLightColor
                canvas.drawRoundRect(RectF(margin, currentY, margin + sigBoxW, currentY + sigBoxH), 6f, 6f, strokePaint)

                boldPaint.color = darkTextColor
                boldPaint.textSize = 8f
                canvas.drawText("VISA DE LA DIRECTION / SERVICE RH :", margin + 8f, currentY + 14f, boldPaint)
                textPaint.color = lightTextColor
                textPaint.textSize = 7.5f
                canvas.drawText("Date : ___________________  Signature :", margin + 8f, currentY + 40f, textPaint)

                // Right: Legal / Archiving Notice
                val rightBoxX = margin + sigBoxW + 20f
                fillPaint.color = Color.rgb(248, 250, 252)
                canvas.drawRoundRect(RectF(rightBoxX, currentY, rightBoxX + sigBoxW, currentY + sigBoxH), 6f, 6f, fillPaint)
                strokePaint.color = borderLightColor
                canvas.drawRoundRect(RectF(rightBoxX, currentY, rightBoxX + sigBoxW, currentY + sigBoxH), 6f, 6f, strokePaint)

                boldPaint.color = darkTextColor
                boldPaint.textSize = 8f
                canvas.drawText("CONSERVATION & CONFORMITÉ :", rightBoxX + 8f, currentY + 14f, boldPaint)
                textPaint.color = lightTextColor
                textPaint.textSize = 7f
                canvas.drawText("Document à archiver conformément aux obligations légales.", rightBoxX + 8f, currentY + 28f, textPaint)
                canvas.drawText("Conforme aux dispositions du Code du Travail & Règlements RH.", rightBoxX + 8f, currentY + 40f, textPaint)
            }

            // 5. FOOTER (Every Page)
            val footerY = pageHeight - margin + 12f
            strokePaint.color = borderLightColor
            canvas.drawLine(margin, footerY - 14f, margin + contentWidth, footerY - 14f, strokePaint)

            textPaint.color = lightTextColor
            textPaint.textSize = 8f
            canvas.drawText("TimeOff Enterprise • Rapport confidentiel de gestion des absences", margin, footerY, textPaint)

            val pageStr = "Page $pageNumber / $totalPages"
            val pageStrW = textPaint.measureText(pageStr)
            canvas.drawText(pageStr, margin + contentWidth - pageStrW, footerY, textPaint)

            document.finishPage(page)
        }

        // Save PDF to App Documents or Cache
        val reportsDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Rapports_Conges").apply {
            if (!exists()) mkdirs()
        }

        val sanitizedDept = if (!departmentFilter.isNullOrBlank() && !departmentFilter.startsWith("Tous")) {
            "_${departmentFilter.replace(" ", "_")}"
        } else ""

        val fileName = "Rapport_Conges_${monthLabel}_${year}${sanitizedDept}_${System.currentTimeMillis()}.pdf"
        val outputFile = File(reportsDir, fileName)

        try {
            FileOutputStream(outputFile).use { outStream ->
                document.writeTo(outStream)
            }
        } finally {
            document.close()
        }

        return outputFile
    }

    private fun drawKpiCard(
        canvas: Canvas,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        title: String,
        value: String,
        subtext: String,
        accentColor: Int,
        fillPaint: Paint,
        strokePaint: Paint,
        textPaint: Paint,
        boldPaint: Paint
    ) {
        val rect = RectF(x, y, x + width, y + height)

        // Card background
        fillPaint.color = Color.rgb(248, 250, 252) // Slate 50
        canvas.drawRoundRect(rect, 6f, 6f, fillPaint)

        // Card border
        strokePaint.color = Color.rgb(226, 232, 240)
        canvas.drawRoundRect(rect, 6f, 6f, strokePaint)

        // Top accent bar
        fillPaint.color = accentColor
        canvas.drawRoundRect(RectF(x, y, x + width, y + 3f), 2f, 2f, fillPaint)

        // Title
        boldPaint.color = Color.rgb(100, 116, 139) // Slate 500
        boldPaint.textSize = 7f
        canvas.drawText(title, x + 8f, y + 14f, boldPaint)

        // Main Value
        boldPaint.color = Color.rgb(15, 23, 42)
        boldPaint.textSize = 13f
        canvas.drawText(value, x + 8f, y + 32f, boldPaint)

        // Subtext
        textPaint.color = Color.rgb(148, 163, 184)
        textPaint.textSize = 7f
        canvas.drawText(subtext, x + 8f, y + 45f, textPaint)
    }

    /**
     * Opens the generated PDF with any compatible PDF viewer installed on the device.
     */
    fun openPdf(context: Context, pdfFile: File) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            val chooser = Intent.createChooser(intent, "Ouvrir le rapport PDF").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Impossible d'ouvrir le PDF : ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Shares the generated PDF file via email, Slack, messaging or cloud storage.
     */
    fun sharePdf(context: Context, pdfFile: File, monthLabel: String, year: Int) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Rapport Mensuel des Congés - $monthLabel $year")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Veuillez trouver ci-joint le rapport officiel des congés et absences validés pour $monthLabel $year.\n\nDocument généré via TimeOff RH."
                )
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }

            val chooser = Intent.createChooser(shareIntent, "Partager le rapport PDF").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Erreur lors du partage : ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
}
