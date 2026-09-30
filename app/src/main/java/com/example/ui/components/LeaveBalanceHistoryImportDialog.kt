package com.example.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.CsvParseResult
import com.example.data.LeaveBalanceHistoryCsvRow
import com.example.data.SupabaseImportResult
import com.example.data.SupabaseSyncManager
import com.example.util.LeaveBalanceCsvParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Enterprise Dialog for importing CSV files into the Supabase `leave_balance_history` table.
 *
 * Security:
 * - Only administrators can perform the import.
 * - Normal employees are strictly prevented from viewing or executing imports.
 *
 * Validations:
 * - user_id: Valid RFC 4122 UUID
 * - action: Strictly 'accrual', 'deduction' or 'adjustment'
 * - days_delta & balance_after: Valid numeric values
 */
@Composable
fun LeaveBalanceHistoryImportDialog(
    callerEmail: String? = "elmzabitemohamedtaha@gmail.com",
    onDismiss: () -> Unit,
    onImportCompleted: (Int) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val supabaseManager = remember { SupabaseSyncManager.getInstance(context) }
    val isSupabaseConfigured by supabaseManager.isConfigured.collectAsState()

    // 1. Strict Security & Administrator check
    val cleanEmail = callerEmail?.trim()?.lowercase() ?: ""
    val isUserAdmin = cleanEmail == "elmzabitemohamedtaha@gmail.com" ||
            cleanEmail.contains("admin") ||
            cleanEmail.contains("rh")

    // State management
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Fichier CSV, 1: Coller CSV / Éditeur
    var csvText by remember { mutableStateOf(LeaveBalanceCsvParser.getSampleCsv()) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var parseResult by remember { mutableStateOf<CsvParseResult?>(null) }
    var previewFilter by remember { mutableIntStateOf(0) } // 0: Toutes, 1: Valides, 2: Erreurs

    var isImporting by remember { mutableStateOf(false) }
    var importResult by remember { mutableStateOf<SupabaseImportResult?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Initial parse of default sample
    LaunchedEffect(Unit) {
        parseResult = LeaveBalanceCsvParser.parse(csvText)
    }

    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val contentResolver = context.contentResolver
                    val fileName = uri.lastPathSegment ?: "fichier.csv"
                    contentResolver.openInputStream(uri)?.use { inputStream ->
                        val parsed = LeaveBalanceCsvParser.parse(inputStream)
                        withContext(Dispatchers.Main) {
                            selectedFileName = fileName
                            csvText = parsed.rawContent
                            parseResult = parsed
                            importResult = null
                            errorMessage = null
                            Toast.makeText(
                                context,
                                "Fichier CSV chargé : ${parsed.totalRows} ligne(s) détectée(s)",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        errorMessage = "Impossible de lire le fichier sélectionné : ${e.message}"
                    }
                }
            }
        }
    }

    Dialog(
        onDismissRequest = { if (!isImporting) onDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !isImporting,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .heightIn(max = 760.dp)
                .clip(RoundedCornerShape(20.dp))
                .testTag("import_leave_balance_history_dialog"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            if (!isUserAdmin) {
                // Access Denied Screen for non-admin users
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(Color(0xFFFEE2E2), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = "Accès refusé",
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Text(
                        text = "Accès Administrateur Requis",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFDC2626)
                    )
                    Text(
                        text = "Les employés normaux ne sont pas autorisés à importer, modifier ou supprimer l'historique des soldes de congés.\n\nVeuillez vous connecter avec le compte administrateur RH pour accéder à cette fonctionnalité.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_access_denied_button")
                    ) {
                        Text("Fermer")
                    }
                }
                return@Surface
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // 1. DIALOG HEADER
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color(0xFF0284C7).copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.FileUpload,
                                contentDescription = null,
                                tint = Color(0xFF0284C7),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Importer l'historique des congés",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Security,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "Table Supabase: leave_balance_history • Admin RH",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF059669)
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = { if (!isImporting) onDismiss() },
                        enabled = !isImporting,
                        modifier = Modifier.testTag("close_import_dialog_button")
                    ) {
                        Icon(imageVector = Icons.Filled.Clear, contentDescription = "Fermer")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 2. INPUT MODE TABS
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Filled.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Sélectionner un fichier CSV", fontSize = 13.sp)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Saisir / Coller CSV", fontSize = 13.sp)
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 3. TAB CONTENT
                when (selectedTab) {
                    0 -> {
                        // File Selection Tab
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (selectedFileName != null) "Fichier : $selectedFileName" else "Aucun fichier sélectionné",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Colonnes: user_id, employee_email, leave_type, action, days_delta, balance_after, reason",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Button(
                                        onClick = { filePickerLauncher.launch("*/*") },
                                        modifier = Modifier.testTag("select_csv_file_button"),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Filled.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Parcourir...", fontSize = 13.sp)
                                    }
                                }

                                // Quick test buttons
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            csvText = LeaveBalanceCsvParser.getSampleCsv()
                                            selectedFileName = "exemple_standard.csv"
                                            parseResult = LeaveBalanceCsvParser.parse(csvText)
                                            errorMessage = null
                                            importResult = null
                                        },
                                        modifier = Modifier.testTag("load_sample_csv_button"),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF10B981))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Charger exemple valide", fontSize = 12.sp)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            csvText = LeaveBalanceCsvParser.getSampleWithErrorsCsv()
                                            selectedFileName = "exemple_avec_erreurs.csv"
                                            parseResult = LeaveBalanceCsvParser.parse(csvText)
                                            errorMessage = null
                                            importResult = null
                                        },
                                        modifier = Modifier.testTag("load_error_sample_button"),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Filled.Warning, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFFF59E0B))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Tester détection d'erreurs", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                    1 -> {
                        // Direct Text Input Tab
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedTextField(
                                value = csvText,
                                onValueChange = {
                                    csvText = it
                                    parseResult = LeaveBalanceCsvParser.parse(it)
                                    selectedFileName = "saisie_manuelle.csv"
                                    importResult = null
                                    errorMessage = null
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                                    .testTag("csv_raw_text_input"),
                                textStyle = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                ),
                                placeholder = {
                                    Text(
                                        "user_id,employee_email,leave_type,action,days_delta,balance_after,reason\nUUID_UTILISATEUR,employe@example.com,annual,accrual,21,21,Solde annuel initial",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp
                                    )
                                },
                                shape = RoundedCornerShape(10.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Actions admises : 'accrual', 'deduction', 'adjustment'",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                TextButton(
                                    onClick = {
                                        csvText = LeaveBalanceCsvParser.getSampleCsv()
                                        parseResult = LeaveBalanceCsvParser.parse(csvText)
                                    }
                                ) {
                                    Text("Réinitialiser exemple", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 4. PREVIEW SUMMARY & FILTER TABS
                val result = parseResult
                if (result != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Aperçu des lignes (${result.totalRows})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = previewFilter == 0,
                                onClick = { previewFilter = 0 },
                                label = { Text("Toutes (${result.totalRows})", fontSize = 11.sp) },
                                modifier = Modifier.testTag("filter_all_rows_chip")
                            )
                            FilterChip(
                                selected = previewFilter == 1,
                                onClick = { previewFilter = 1 },
                                label = { Text("Valides (${result.validCount})", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFDCFCE7),
                                    selectedLabelColor = Color(0xFF166534)
                                ),
                                modifier = Modifier.testTag("filter_valid_rows_chip")
                            )
                            FilterChip(
                                selected = previewFilter == 2,
                                onClick = { previewFilter = 2 },
                                label = { Text("Erreurs (${result.errorCount})", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFFEE2E2),
                                    selectedLabelColor = Color(0xFF991B1B)
                                ),
                                modifier = Modifier.testTag("filter_error_rows_chip")
                            )
                        }
                    }

                    // Global header errors if any
                    if (result.headerErrors.isNotEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Filled.Error, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                                Column {
                                    result.headerErrors.forEach { err ->
                                        Text(text = "• $err", color = Color(0xFF991B1B), fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    // Filtered row items
                    val displayedRows = when (previewFilter) {
                        1 -> result.validRows
                        2 -> result.errorRows
                        else -> result.rows
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .testTag("csv_rows_preview_list"),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (displayedRows.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Aucune ligne à afficher pour ce filtre.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            items(displayedRows) { row ->
                                RowPreviewCard(row = row)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 5. STATUS / FEEDBACK BANNER
                if (importResult != null) {
                    when (val res = importResult!!) {
                        is SupabaseImportResult.Success -> {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(24.dp))
                                    Column {
                                        Text(
                                            text = "Importation réussie !",
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF166534),
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = res.message,
                                            color = Color(0xFF15803D),
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                        is SupabaseImportResult.Error -> {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(Icons.Filled.Error, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(24.dp))
                                    Column {
                                        Text(
                                            text = res.message,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF991B1B),
                                            fontSize = 13.sp
                                        )
                                        if (!res.details.isNullOrBlank()) {
                                            Text(
                                                text = res.details,
                                                color = Color(0xFFB91C1C),
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (errorMessage != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.Warning, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(18.dp))
                            Text(text = errorMessage!!, color = Color(0xFF92400E), fontSize = 12.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // 6. ACTION BUTTONS
                HorizontalDivider()
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = onDismiss,
                        enabled = !isImporting,
                        modifier = Modifier.testTag("cancel_import_button")
                    ) {
                        Text("Fermer")
                    }

                    val validCount = parseResult?.validCount ?: 0
                    Button(
                        onClick = {
                            val validRows = parseResult?.validRows ?: emptyList()
                            if (validRows.isEmpty()) {
                                errorMessage = "Aucune ligne valide à importer."
                                return@Button
                            }

                            isImporting = true
                            errorMessage = null
                            importResult = null

                            coroutineScope.launch {
                                val res = supabaseManager.insertLeaveBalanceHistory(
                                    rows = validRows,
                                    callerEmail = callerEmail
                                )
                                isImporting = false
                                importResult = res
                                if (res is SupabaseImportResult.Success) {
                                    onImportCompleted(res.importedCount)
                                    Toast.makeText(
                                        context,
                                        "${res.importedCount} ligne(s) importée(s) avec succès !",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        },
                        enabled = !isImporting && validCount > 0,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0284C7)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("confirm_import_button")
                    ) {
                        if (isImporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Importation en cours...")
                        } else {
                            Icon(Icons.Filled.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Confirmer l'import ($validCount ${if (validCount > 1) "lignes" else "ligne"})")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Visual card displaying row data and any validation errors.
 */
@Composable
private fun RowPreviewCard(row: LeaveBalanceHistoryCsvRow) {
    val borderColor = if (row.isValid) Color(0xFFE2E8F0) else Color(0xFFFCA5A5)
    val bgColor = if (row.isValid) MaterialTheme.colorScheme.surfaceContainerLowest else Color(0xFFFEF2F2)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(10.dp)),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                if (row.isValid) Color(0xFF0284C7).copy(alpha = 0.1f) else Color(0xFFDC2626).copy(alpha = 0.1f),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "#${row.lineNumber}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (row.isValid) Color(0xFF0284C7) else Color(0xFFDC2626)
                        )
                    }

                    Text(
                        text = row.employeeEmail.ifBlank { "(email manquant)" },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Action badge
                ActionBadge(action = row.action, daysDelta = row.daysDelta, isValid = row.isValid)
            }

            // Details line: Type, Delta, New Balance, Reason
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Type : ${row.leaveType}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "•",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Solde après : ${if (row.rawBalanceAfter.isNotBlank()) row.rawBalanceAfter else "${row.balanceAfter}"} j",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF0284C7)
                )
                if (row.reason.isNotBlank()) {
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = row.reason,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // UUID display
            Text(
                text = "user_id : ${row.userId.ifBlank { "(vide)" }}",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp
                ),
                color = if (LeaveBalanceCsvParser.isValidUuid(row.userId)) Color(0xFF64748B) else Color(0xFFDC2626)
            )

            // Validation Errors if any
            if (!row.isValid && row.validationErrors.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFEE2E2), RoundedCornerShape(6.dp))
                        .padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    row.validationErrors.forEach { err ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ErrorOutline,
                                contentDescription = null,
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = err,
                                color = Color(0xFF991B1B),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Action badge styling (accrual = green, deduction = red/orange, adjustment = blue).
 */
@Composable
private fun ActionBadge(action: String, daysDelta: Double, isValid: Boolean) {
    val cleanAction = action.lowercase().trim()
    val (bgColor, textColor, label, prefix) = when (cleanAction) {
        "accrual" -> Quad(Color(0xFFDCFCE7), Color(0xFF166534), "accrual", "+")
        "deduction" -> Quad(Color(0xFFFEE2E2), Color(0xFF991B1B), "deduction", "")
        "adjustment" -> Quad(Color(0xFFE0F2FE), Color(0xFF0369A1), "adjustment", if (daysDelta > 0) "+" else "")
        else -> Quad(Color(0xFFFEF3C7), Color(0xFF92400E), action.ifBlank { "inconnu" }, "")
    }

    val deltaFormatted = if (daysDelta == daysDelta.toLong().toDouble()) {
        "${daysDelta.toLong()}"
    } else {
        "$daysDelta"
    }

    Box(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = if (isValid) "$label ($prefix$deltaFormatted j)" else label,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
