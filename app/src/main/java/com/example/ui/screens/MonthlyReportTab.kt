package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LeaveRepository
import com.example.data.LeaveRequestEntity
import com.example.data.User
import com.example.service.CsvReportExporter
import com.example.service.PdfReportGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyReportTab(
    registeredUsers: List<User> = emptyList()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val leaveRepository = remember { LeaveRepository.getInstance(context) }

    // Month & Year state (Defaults to Octobre 2026 or current calendar)
    var selectedMonth by remember { mutableIntStateOf(9) } // 9 = Octobre (0-indexed)
    var selectedYear by remember { mutableIntStateOf(2026) }
    var selectedDepartment by remember { mutableStateOf("Tous") }

    val monthNames = remember {
        arrayOf(
            "Janvier", "Février", "Mars", "Avril", "Mai", "Juin",
            "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre"
        )
    }

    val departments = remember(registeredUsers) {
        val userDepts = registeredUsers.map { it.department }.filter { it.isNotBlank() }
        (listOf("Tous", "Tech / IT", "Ressources Humaines", "Marketing", "Finance", "Opérations") + userDepts).distinct()
    }

    // Map for fast user lookup
    val usersMap = remember(registeredUsers) {
        registeredUsers.associateBy { it.email.lowercase().trim() }
    }

    // Reactive leave requests
    val allRequests by leaveRepository.getAllRequestsFlow().collectAsState(initial = emptyList())

    // Filter approved requests for the selected month and year
    val approvedRequests = remember(allRequests, selectedMonth, selectedYear, selectedDepartment) {
        allRequests.filter { req ->
            req.status.equals("APPROVED", ignoreCase = true) &&
                    req.month == selectedMonth &&
                    req.year == selectedYear &&
                    (selectedDepartment == "Tous" || req.department.equals(selectedDepartment, ignoreCase = true))
        }.sortedWith(compareBy({ it.startDay }, { it.employeeName }))
    }

    // Statistics
    val stats = remember(approvedRequests) {
        PdfReportGenerator.calculateStatistics(approvedRequests)
    }

    // PDF Export states
    var isGeneratingPdf by remember { mutableStateOf(false) }
    var lastGeneratedPdfFile by remember { mutableStateOf<File?>(null) }
    var showPdfSuccessBanner by remember { mutableStateOf(false) }

    // CSV Export states
    var isGeneratingCsv by remember { mutableStateOf(false) }
    var lastGeneratedCsvFile by remember { mutableStateOf<File?>(null) }
    var showCsvSuccessBanner by remember { mutableStateOf(false) }
    var csvExportScopeLabel by remember { mutableStateOf("") }

    // Detail dialog for clicked request
    var selectedRequestDetail by remember { mutableStateOf<LeaveRequestEntity?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
    ) {
        // 1. HERO HEADER CARD & PERIOD SELECTOR
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Dual Badge Icons for PDF & CSV
                            Row(horizontalArrangement = Arrangement.spacedBy((-10).dp)) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFEF4444).copy(alpha = 0.15f),
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.PictureAsPdf,
                                            contentDescription = "PDF",
                                            tint = Color(0xFFDC2626),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF10B981).copy(alpha = 0.15f),
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.TableChart,
                                            contentDescription = "CSV / Excel",
                                            tint = Color(0xFF059669),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                            Column {
                                Text(
                                    text = "Rapports & Exports (PDF & CSV)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Exportez en PDF officiel ou CSV compatible Excel / Google Sheets",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Month & Year Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Month Dropdown
                        var monthMenuExpanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(1.2f)) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { monthMenuExpanded = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.CalendarMonth,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = monthNames[selectedMonth],
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp
                                        )
                                    }
                                    Text("▼", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                }
                            }

                            DropdownMenu(
                                expanded = monthMenuExpanded,
                                onDismissRequest = { monthMenuExpanded = false }
                            ) {
                                monthNames.forEachIndexed { index, name ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = name,
                                                fontWeight = if (selectedMonth == index) FontWeight.Bold else FontWeight.Normal,
                                                color = if (selectedMonth == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                        },
                                        onClick = {
                                            selectedMonth = index
                                            monthMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Year Selector
                        var yearMenuExpanded by remember { mutableStateOf(false) }
                        val years = listOf(2025, 2026, 2027)
                        Box(modifier = Modifier.weight(0.8f)) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { yearMenuExpanded = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "$selectedYear",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                    Text("▼", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                }
                            }

                            DropdownMenu(
                                expanded = yearMenuExpanded,
                                onDismissRequest = { yearMenuExpanded = false }
                            ) {
                                years.forEach { yr ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "$yr",
                                                fontWeight = if (selectedYear == yr) FontWeight.Bold else FontWeight.Normal,
                                                color = if (selectedYear == yr) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                        },
                                        onClick = {
                                            selectedYear = yr
                                            yearMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Department Filter Chips
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Filtrer par département :",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(departments) { dept ->
                                FilterChip(
                                    selected = selectedDepartment == dept,
                                    onClick = { selectedDepartment = dept },
                                    label = { Text(dept, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. SUCCESS EXPORT NOTIFICATION BANNERS
        // CSV Success Banner
        if (showCsvSuccessBanner && lastGeneratedCsvFile != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFECFDF5) // Emerald 50
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF34D399))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF059669),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.TableChart,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Fichier CSV Tableur Prêt !",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF065F46),
                                fontSize = 14.sp
                            )
                            Text(
                                text = "${lastGeneratedCsvFile!!.name} ($csvExportScopeLabel)",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF047857),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(
                                onClick = {
                                    CsvReportExporter.openCsv(context, lastGeneratedCsvFile!!)
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.OpenInNew,
                                    contentDescription = "Ouvrir dans Excel / Sheets",
                                    tint = Color(0xFF059669)
                                )
                            }
                            IconButton(
                                onClick = {
                                    CsvReportExporter.shareCsv(
                                        context = context,
                                        csvFile = lastGeneratedCsvFile!!,
                                        subject = "Export Congés CSV - ${monthNames[selectedMonth]} $selectedYear"
                                    )
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Share,
                                    contentDescription = "Partager CSV",
                                    tint = Color(0xFF059669)
                                )
                            }
                        }
                    }
                }
            }
        }

        // PDF Success Banner
        if (showPdfSuccessBanner && lastGeneratedPdfFile != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFEFF6FF) // Blue 50
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF93C5FD))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF2563EB),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.PictureAsPdf,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Rapport PDF prêt !",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E3A8A),
                                fontSize = 14.sp
                            )
                            Text(
                                text = lastGeneratedPdfFile!!.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF1D4ED8),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(
                                onClick = {
                                    PdfReportGenerator.openPdf(context, lastGeneratedPdfFile!!)
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Visibility,
                                    contentDescription = "Ouvrir",
                                    tint = Color(0xFF2563EB)
                                )
                            }
                            IconButton(
                                onClick = {
                                    PdfReportGenerator.sharePdf(
                                        context = context,
                                        pdfFile = lastGeneratedPdfFile!!,
                                        monthLabel = monthNames[selectedMonth],
                                        year = selectedYear
                                    )
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Share,
                                    contentDescription = "Partager",
                                    tint = Color(0xFF2563EB)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. ACTION CARDS: CSV EXPORT & PDF EXPORT
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Options d'Exportation pour Gestionnaires",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // PRIMARY OPTION 1: EXPORT CSV FOR SPREADSHEETS (Excel / Google Sheets)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF047857).copy(alpha = 0.06f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF059669),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.TableChart,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Export CSV (Excel & Tableurs)",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF065F46)
                                    )
                                    Text(
                                        text = "Format UTF-8 BOM avec séparateurs standard pour intégration immédiate dans Excel, Google Sheets, RH et paie.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Export current filtered month to CSV
                                Button(
                                    onClick = {
                                        isGeneratingCsv = true
                                        coroutineScope.launch {
                                            val generatedCsv = withContext(Dispatchers.IO) {
                                                CsvReportExporter.exportLeaveRequestsToCsv(
                                                    context = context,
                                                    requests = approvedRequests,
                                                    usersMap = usersMap,
                                                    fileNamePrefix = "conges_${monthNames[selectedMonth].lowercase()}_$selectedYear",
                                                    periodLabel = "${monthNames[selectedMonth]} $selectedYear"
                                                )
                                            }
                                            lastGeneratedCsvFile = generatedCsv
                                            csvExportScopeLabel = "${monthNames[selectedMonth]} $selectedYear (${approvedRequests.size} congés)"
                                            isGeneratingCsv = false
                                            showCsvSuccessBanner = true

                                            Toast.makeText(
                                                context,
                                                "Fichier CSV généré (${approvedRequests.size} lignes) !",
                                                Toast.LENGTH_SHORT
                                            ).show()

                                            CsvReportExporter.openCsv(context, generatedCsv)
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(46.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF059669) // Emerald 600
                                    ),
                                    enabled = !isGeneratingCsv
                                ) {
                                    if (isGeneratingCsv) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Export CSV...", color = Color.White, fontSize = 13.sp)
                                    } else {
                                        Icon(
                                            imageVector = Icons.Filled.FileDownload,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "CSV Mois (${approvedRequests.size})",
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 13.sp
                                        )
                                    }
                                }

                                // Export all leave requests in system to master CSV
                                OutlinedButton(
                                    onClick = {
                                        isGeneratingCsv = true
                                        coroutineScope.launch {
                                            val generatedCsv = withContext(Dispatchers.IO) {
                                                CsvReportExporter.exportLeaveRequestsToCsv(
                                                    context = context,
                                                    requests = allRequests,
                                                    usersMap = usersMap,
                                                    fileNamePrefix = "export_global_conges_tous",
                                                    periodLabel = "Historique Complet"
                                                )
                                            }
                                            lastGeneratedCsvFile = generatedCsv
                                            csvExportScopeLabel = "Historique complet (${allRequests.size} demandes)"
                                            isGeneratingCsv = false
                                            showCsvSuccessBanner = true

                                            Toast.makeText(
                                                context,
                                                "Fichier CSV global généré (${allRequests.size} demandes) !",
                                                Toast.LENGTH_SHORT
                                            ).show()

                                            CsvReportExporter.openCsv(context, generatedCsv)
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(46.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    enabled = !isGeneratingCsv
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Download,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = Color(0xFF059669)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "CSV Global (${allRequests.size})",
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF059669),
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }

                    // PRIMARY OPTION 2: EXPORT PDF
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF1E3A8A).copy(alpha = 0.05f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF1E3A8A),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.PictureAsPdf,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Rapport PDF Officiel A4",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1E3A8A)
                                    )
                                    Text(
                                        text = "Document vectoriel paginé avec en-tête d'entreprise, signature RH et indicateurs pour archivage.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        isGeneratingPdf = true
                                        coroutineScope.launch {
                                            val generatedFile = withContext(Dispatchers.IO) {
                                                PdfReportGenerator.generateMonthlyLeaveReport(
                                                    context = context,
                                                    month = selectedMonth,
                                                    year = selectedYear,
                                                    departmentFilter = if (selectedDepartment == "Tous") null else selectedDepartment,
                                                    approvedRequests = approvedRequests,
                                                    managerName = "Directeur RH"
                                                )
                                            }
                                            lastGeneratedPdfFile = generatedFile
                                            isGeneratingPdf = false
                                            showPdfSuccessBanner = true

                                            Toast.makeText(
                                                context,
                                                "Rapport PDF généré (${approvedRequests.size} congés) !",
                                                Toast.LENGTH_SHORT
                                            ).show()

                                            PdfReportGenerator.openPdf(context, generatedFile)
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .height(46.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF1E3A8A)
                                    ),
                                    enabled = !isGeneratingPdf
                                ) {
                                    if (isGeneratingPdf) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Génération PDF...", color = Color.White, fontSize = 13.sp)
                                    } else {
                                        Icon(
                                            imageVector = Icons.Filled.PictureAsPdf,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Générer PDF (${approvedRequests.size})",
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 13.sp
                                        )
                                    }
                                }

                                OutlinedButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            val file = lastGeneratedPdfFile ?: withContext(Dispatchers.IO) {
                                                PdfReportGenerator.generateMonthlyLeaveReport(
                                                    context = context,
                                                    month = selectedMonth,
                                                    year = selectedYear,
                                                    departmentFilter = if (selectedDepartment == "Tous") null else selectedDepartment,
                                                    approvedRequests = approvedRequests,
                                                    managerName = "Directeur RH"
                                                )
                                            }
                                            lastGeneratedPdfFile = file
                                            showPdfSuccessBanner = true
                                            PdfReportGenerator.sharePdf(
                                                context = context,
                                                pdfFile = file,
                                                monthLabel = monthNames[selectedMonth],
                                                year = selectedYear
                                            )
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(0.8f)
                                        .height(46.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Share,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Partager", fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. EXECUTIVE SUMMARY / KPI CARDS
        item {
            Text(
                text = "Indicateurs Clés — ${monthNames[selectedMonth]} $selectedYear",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Card 1: Total Approved
                KpiBadgeCard(
                    modifier = Modifier.weight(1f),
                    title = "Demandes",
                    value = "${stats.totalRequests}",
                    subtext = "Approuvées",
                    icon = Icons.Filled.CheckCircle,
                    accentColor = Color(0xFF10B981)
                )

                // Card 2: Total Days
                KpiBadgeCard(
                    modifier = Modifier.weight(1f),
                    title = "Jours Posés",
                    value = "${stats.totalDays} j",
                    subtext = "Cumul validé",
                    icon = Icons.Filled.DateRange,
                    accentColor = Color(0xFF2563EB)
                )

                // Card 3: Employees
                KpiBadgeCard(
                    modifier = Modifier.weight(1f),
                    title = "Salariés",
                    value = "${stats.uniqueEmployeesCount}",
                    subtext = "En congé",
                    icon = Icons.Filled.Groups,
                    accentColor = Color(0xFF8B5CF6)
                )
            }
        }

        // 5. BREAKDOWN BY TYPE & DEPARTMENT
        if (stats.byType.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Répartition par Type de Congé",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        stats.byType.forEach { (type, days) ->
                            val percent = if (stats.totalDays > 0) days.toFloat() / stats.totalDays else 0f
                            val typeColor = when (type) {
                                "Congés payés" -> Color(0xFF2563EB)
                                "RTT" -> Color(0xFF8B5CF6)
                                "Maladie" -> Color(0xFFEF4444)
                                "Télétravail" -> Color(0xFF0D9488)
                                else -> Color(0xFFF59E0B)
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(type, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                                    Text(
                                        "$days jours (${(percent * 100).toInt()}%)",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = typeColor
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { percent },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = typeColor,
                                    trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                )
                            }
                        }
                    }
                }
            }
        }

        // 6. APPROVED REQUESTS LIST (FOR SPREADSHEET / PDF PREVIEW)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Congés Inclus dans les Exports (${approvedRequests.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                if (approvedRequests.isEmpty()) {
                    // Seed button to easily generate test approved requests if empty
                    TextButton(
                        onClick = {
                            coroutineScope.launch {
                                // Add 2 test approved leaves for this month
                                val currentCal = Calendar.getInstance()
                                currentCal.set(Calendar.MONTH, selectedMonth)
                                currentCal.set(Calendar.YEAR, selectedYear)

                                val sample1 = LeaveRequestEntity(
                                    id = "sample_app_${System.currentTimeMillis()}_1",
                                    employeeEmail = "mouad@gmail.com",
                                    employeeName = "mouad elmzabite",
                                    department = "Tech / IT",
                                    leaveType = "Congés payés",
                                    startDate = "05/${String.format("%02d", selectedMonth + 1)}/$selectedYear",
                                    endDate = "12/${String.format("%02d", selectedMonth + 1)}/$selectedYear",
                                    startDay = 5,
                                    endDay = 12,
                                    month = selectedMonth,
                                    year = selectedYear,
                                    daysCount = 6,
                                    reason = "Vacances d'automne en famille",
                                    status = "APPROVED",
                                    createdAt = System.currentTimeMillis() - 86400000L * 5,
                                    decisionAt = System.currentTimeMillis() - 86400000L * 4,
                                    adminComment = "Validé sans réserve."
                                )

                                val sample2 = LeaveRequestEntity(
                                    id = "sample_app_${System.currentTimeMillis()}_2",
                                    employeeEmail = "khadija@gmail.com",
                                    employeeName = "khadija el ferrouni",
                                    department = "Ressources Humaines",
                                    leaveType = "RTT",
                                    startDate = "15/${String.format("%02d", selectedMonth + 1)}/$selectedYear",
                                    endDate = "18/${String.format("%02d", selectedMonth + 1)}/$selectedYear",
                                    startDay = 15,
                                    endDay = 18,
                                    month = selectedMonth,
                                    year = selectedYear,
                                    daysCount = 3,
                                    reason = "Pont et récupération horaire",
                                    status = "APPROVED",
                                    createdAt = System.currentTimeMillis() - 86400000L * 3,
                                    decisionAt = System.currentTimeMillis() - 86400000L * 2,
                                    adminComment = "Accordé par la direction RH."
                                )

                                val db = com.example.data.AppDatabase.getDatabase(context)
                                db.leaveRequestDao().insertAll(listOf(sample1, sample2))

                                Toast.makeText(context, "Exemples de congés validés ajoutés !", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Icon(imageVector = Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ajouter exemples", fontSize = 12.sp)
                    }
                }
            }
        }

        if (approvedRequests.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.size(54.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Assessment,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Text(
                            text = "Aucun congé approuvé pour ce mois",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Aucune demande avec le statut 'APPROVED' n'a été trouvée pour ${monthNames[selectedMonth]} $selectedYear.\nVous pouvez exporter l'historique complet en CSV ci-dessus ou ajouter des exemples.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(approvedRequests, key = { it.id }) { req ->
                ApprovedLeaveReportItem(
                    request = req,
                    onClick = { selectedRequestDetail = req }
                )
            }
        }
    }

    // Detail Dialog
    if (selectedRequestDetail != null) {
        val req = selectedRequestDetail!!
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { selectedRequestDetail = null },
            icon = {
                Icon(
                    imageVector = Icons.Filled.VerifiedUser,
                    contentDescription = null,
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Détail du Congé Validé",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Collaborateur : ${req.employeeName} (${req.employeeEmail})",
                        fontWeight = FontWeight.SemiBold
                    )
                    Text("Département : ${req.department}")
                    Text("Type : ${req.leaveType} (${req.daysCount} jours)")
                    Text("Période : Du ${req.startDate} au ${req.endDate}")
                    if (req.reason.isNotBlank()) {
                        Text("Motif : ${req.reason}")
                    }
                    if (!req.adminComment.isNullOrBlank()) {
                        Text(
                            text = "Avis RH : ${req.adminComment}",
                            color = Color(0xFF047857),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = { selectedRequestDetail = null }) {
                    Text("Fermer")
                }
            }
        )
    }
}

@Composable
fun KpiBadgeCard(
    title: String,
    value: String,
    subtext: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
            }

            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = subtext,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun ApprovedLeaveReportItem(
    request: LeaveRequestEntity,
    onClick: () -> Unit
) {
    val leaveTypeColor = when (request.leaveType) {
        "Congés payés" -> Color(0xFF2563EB)
        "RTT" -> Color(0xFF8B5CF6)
        "Maladie" -> Color(0xFFEF4444)
        "Télétravail" -> Color(0xFF0D9488)
        else -> Color(0xFFF59E0B)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Leave Type Indicator Bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(44.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(leaveTypeColor)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = request.employeeName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFDCFCE7)
                    ) {
                        Text(
                            text = "APPROUVÉ",
                            color = Color(0xFF15803D),
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = request.department,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text("•", color = MaterialTheme.colorScheme.outline)
                    Text(
                        text = "${request.leaveType} (${request.daysCount}j)",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = leaveTypeColor
                    )
                }

                Text(
                    text = "Période : ${request.startDate} au ${request.endDate}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
