package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SupabaseSyncManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Enterprise Supabase Management and Configuration Dialog.
 *
 * Allows users and administrators to:
 * 1. Configure Supabase Project URL & Anon Public Key
 * 2. Test live connectivity with Supabase PostgREST backend
 * 3. Trigger 2-way real-time data sync with local Room database
 * 4. Generate & copy ready-to-run PostgreSQL schema script for Supabase SQL Editor
 * 5. Toggle background automatic periodic synchronization
 * 6. Launch CSV import for table leave_balance_history
 */
@Composable
fun SupabaseManagementDialog(
    onDismiss: () -> Unit,
    onOpenImportCsv: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val uriHandler = LocalUriHandler.current

    val supabaseManager = remember { SupabaseSyncManager.getInstance(context) }
    val isConfigured by supabaseManager.isConfigured.collectAsState()
    val isConnected by supabaseManager.isConnected.collectAsState()
    val syncStatusText by supabaseManager.syncStatusText.collectAsState()
    val lastSyncTime by supabaseManager.lastSyncTimestamp.collectAsState()
    val isSyncing by supabaseManager.isSyncing.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var inputUrl by remember { mutableStateOf(supabaseManager.supabaseUrl) }
    var inputKey by remember { mutableStateOf(supabaseManager.supabaseAnonKey) }
    var isKeyVisible by remember { mutableStateOf(false) }
    var isAutoSync by remember { mutableStateOf(supabaseManager.isAutoSyncEnabled) }

    var actionMessage by remember { mutableStateOf<String?>(null) }
    var isTestingConnection by remember { mutableStateOf(false) }
    var showInternalImportDialog by remember { mutableStateOf(false) }

    val greenSupabase = Color(0xFF3ECF8E) // Official Supabase Green

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("supabase_management_dialog"),
        icon = {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(greenSupabase.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Storage,
                    contentDescription = "Supabase Logo",
                    tint = greenSupabase,
                    modifier = Modifier.size(30.dp)
                )
            }
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Connexion Supabase Cloud",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Base de données PostgreSQL & Synchronisation",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Connection Status Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = when {
                        isConnected -> Color(0xFFDCFCE7)
                        isConfigured -> Color(0xFFFEF3C7)
                        else -> MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isConnected -> Color(0xFF16A34A)
                                        isConfigured -> Color(0xFFD97706)
                                        else -> Color.Gray
                                    }
                                )
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = when {
                                    isConnected -> "Supabase Connecté & Opérationnel"
                                    isConfigured -> "Configuration prête (En attente du test)"
                                    else -> "Configuration Supabase requise"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    isConnected -> Color(0xFF15803D)
                                    isConfigured -> Color(0xFFB45309)
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            )
                            Text(
                                text = syncStatusText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (lastSyncTime > 0) {
                                val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
                                Text(
                                    text = "Dernière synchro : ${sdf.format(Date(lastSyncTime))}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }

                // Tab Switcher: 0 = Synchronisation & Config, 1 = Script SQL Supabase
                TabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Paramètres", fontSize = 13.sp) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Script SQL", fontSize = 13.sp) }
                    )
                }

                when (selectedTab) {
                    0 -> {
                        // TAB 0: Configuration & Sync Controls
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = inputUrl,
                                onValueChange = {
                                    inputUrl = it
                                    actionMessage = null
                                },
                                label = { Text("URL du projet Supabase") },
                                placeholder = { Text("https://xyzcompany.supabase.co") },
                                leadingIcon = {
                                    Icon(Icons.Filled.Language, contentDescription = null, tint = greenSupabase)
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("supabase_url_input")
                            )

                            OutlinedTextField(
                                value = inputKey,
                                onValueChange = {
                                    inputKey = it
                                    actionMessage = null
                                },
                                label = { Text("Clé publique Anon (anon key)") },
                                placeholder = { Text("eyJhbGciOiJIUzI1NiIsInR...") },
                                leadingIcon = {
                                    Icon(Icons.Filled.Key, contentDescription = null, tint = greenSupabase)
                                },
                                trailingIcon = {
                                    IconButton(onClick = { isKeyVisible = !isKeyVisible }) {
                                        Icon(
                                            imageVector = if (isKeyVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                            contentDescription = "Afficher/Masquer la clé"
                                        )
                                    }
                                },
                                visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("supabase_anon_key_input")
                            )

                            // Save & Test Action buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        supabaseManager.supabaseUrl = inputUrl
                                        supabaseManager.supabaseAnonKey = inputKey
                                        Toast.makeText(context, "Identifiants enregistrés !", Toast.LENGTH_SHORT).show()
                                        actionMessage = "Identifiants enregistrés localement."
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Enregistrer")
                                }

                                Button(
                                    onClick = {
                                        supabaseManager.supabaseUrl = inputUrl
                                        supabaseManager.supabaseAnonKey = inputKey
                                        coroutineScope.launch {
                                            isTestingConnection = true
                                            actionMessage = "Test de liaison Supabase..."
                                            val res = supabaseManager.testConnection()
                                            isTestingConnection = false
                                            actionMessage = res.fold(
                                                onSuccess = { it },
                                                onFailure = { "Erreur : ${it.localizedMessage}" }
                                            )
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = greenSupabase),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    if (isTestingConnection) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Text("Tester", color = Color.Black, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            // Auto Sync Toggle
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Synchronisation automatique",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "Synchronise les demandes en arrière-plan toutes les 30s",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Switch(
                                        checked = isAutoSync,
                                        onCheckedChange = { checked ->
                                            isAutoSync = checked
                                            supabaseManager.isAutoSyncEnabled = checked
                                        }
                                    )
                                }
                            }

                            // Manual Bidirectional Sync Button
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        actionMessage = "Synchronisation en cours..."
                                        val res = supabaseManager.syncAll()
                                        actionMessage = res.fold(
                                            onSuccess = { it },
                                            onFailure = { "Erreur sync : ${it.localizedMessage}" }
                                        )
                                    }
                                },
                                enabled = !isSyncing,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("supabase_sync_now_button"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                if (isSyncing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Synchronisation...")
                                } else {
                                    Icon(
                                        imageVector = Icons.Filled.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Synchroniser maintenant (Bidirectionnel)")
                                }
                            }

                            // Seed & Verify 6 Supabase Tables Button
                            OutlinedButton(
                                onClick = {
                                    coroutineScope.launch {
                                        actionMessage = "Peuplement et vérification des 6 tables en cours..."
                                        val res = supabaseManager.seedAllSupabaseTables()
                                        actionMessage = res.fold(
                                            onSuccess = { it },
                                            onFailure = { "Erreur : ${it.localizedMessage}" }
                                        )
                                    }
                                },
                                enabled = !isSyncing,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("supabase_seed_tables_button"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CloudDone,
                                    contentDescription = null,
                                    tint = greenSupabase,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Vérifier & Peupler les 6 tables Supabase", fontWeight = FontWeight.SemiBold)
                            }

                            // Quick Link to Supabase Dashboard
                            OutlinedButton(
                                onClick = {
                                    try {
                                        uriHandler.openUri("https://supabase.com/dashboard")
                                    } catch (_: Exception) {
                                        Toast.makeText(context, "Visitez https://supabase.com/dashboard", Toast.LENGTH_LONG).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Ouvrir la console Supabase (Web)")
                            }

                            // Import CSV Leave Balance History
                            Button(
                                onClick = {
                                    if (onOpenImportCsv != null) {
                                        onOpenImportCsv()
                                    } else {
                                        showInternalImportDialog = true
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("supabase_import_leave_balance_history_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.FileUpload,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Importer l’historique des congés (CSV)", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    1 -> {
                        // TAB 1: SQL Setup Script Assistant
                        var scriptMode by remember { mutableIntStateOf(0) } // 0 = Isolation RLS Employé (profiles, leave_requests, balance), 1 = Global 5 tables
                        val employeeRlsScript = remember { supabaseManager.getEmployeeIsolatedSqlSchema() }
                        val global5TablesScript = remember { supabaseManager.getSupabaseSqlSchema() }
                        val currentScript = if (scriptMode == 0) employeeRlsScript else global5TablesScript

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Sub-tabs to choose script variant
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { scriptMode = 0 },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (scriptMode == 0) greenSupabase else MaterialTheme.colorScheme.surfaceContainerHigh,
                                        contentColor = if (scriptMode == 0) Color.Black else MaterialTheme.colorScheme.onSurface
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(vertical = 6.dp, horizontal = 8.dp)
                                ) {
                                    Text("RLS Isolation Employé", fontSize = 11.sp, fontWeight = if (scriptMode == 0) FontWeight.Bold else FontWeight.Normal)
                                }
                                Button(
                                    onClick = { scriptMode = 1 },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (scriptMode == 1) greenSupabase else MaterialTheme.colorScheme.surfaceContainerHigh,
                                        contentColor = if (scriptMode == 1) Color.Black else MaterialTheme.colorScheme.onSurface
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(vertical = 6.dp, horizontal = 8.dp)
                                ) {
                                    Text("Script Global (6 Tables)", fontSize = 11.sp, fontWeight = if (scriptMode == 1) FontWeight.Bold else FontWeight.Normal)
                                }
                            }

                            if (scriptMode == 0) {
                                Text(
                                    text = "Script d'isolation stricte par employé (profiles, leave_requests, leave_balance_history) avec clés étrangères et politiques RLS :",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Column(
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(8.dp))
                                        .padding(8.dp)
                                ) {
                                    listOf(
                                        "profiles" to "FK vers auth.users(id) ON DELETE CASCADE + RLS auth.uid()",
                                        "leave_requests" to "FK vers profiles(id) + RLS isolation stricte par employé",
                                        "leave_balance_history" to "FK vers profiles & leave_requests + audit lecture seule"
                                    ).forEach { (tbl, desc) ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(Icons.Filled.Check, contentDescription = null, tint = greenSupabase, modifier = Modifier.size(14.dp))
                                            Text(tbl, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                                            Text("• $desc", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            } else {
                                Text(
                                    text = "Ce script SQL complet configure, synchronise et débloque les 6 tables Supabase requises pour l'application avec politiques RLS et triggers :",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                // 6 Tables chips / badges
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(8.dp))
                                        .padding(8.dp)
                                ) {
                                    listOf(
                                        "1. users" to "Profils collaborateurs, rôles & soldes",
                                        "2. profiles" to "Comptes utilisateurs & correspondance d'équipe",
                                        "3. leave_requests" to "Demandes de congés & statuts d'approbation",
                                        "4. app_alerts" to "Notifications, rappels & popups",
                                        "5. leave_balance_history" to "Historique des mouvements de solde & CSV",
                                        "6. notes" to "Mémos internes & notes RH d'équipe"
                                    ).forEach { (tbl, desc) ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(Icons.Filled.Check, contentDescription = null, tint = greenSupabase, modifier = Modifier.size(14.dp))
                                            Text(tbl, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                                            Text("• $desc", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(currentScript))
                                    Toast.makeText(context, "Script SQL copié dans le presse-papier !", Toast.LENGTH_SHORT).show()
                                    actionMessage = "✓ Script SQL (${if (scriptMode == 0) "Isolation RLS Employé" else "6 tables"}) copié ! Collez-le dans l'éditeur SQL de Supabase et cliquez sur 'RUN'."
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = greenSupabase),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (scriptMode == 0) "Copier Script RLS (profiles, congés, soldes)" else "Copier le script SQL (5 tables)",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF1E293B),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 220.dp)
                            ) {
                                Text(
                                    text = currentScript,
                                    color = Color(0xFFE2E8F0),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier
                                        .padding(12.dp)
                                        .verticalScroll(rememberScrollState())
                                )
                            }
                        }
                    }
                }

                // Action Feedback Message
                AnimatedVisibility(visible = actionMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = actionMessage.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(10.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Fermer")
            }
        }
    )

    if (showInternalImportDialog) {
        LeaveBalanceHistoryImportDialog(
            callerEmail = "elmzabitemohamedtaha@gmail.com",
            onDismiss = { showInternalImportDialog = false }
        )
    }
}
