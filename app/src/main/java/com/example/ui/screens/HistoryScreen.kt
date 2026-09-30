package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import java.io.OutputStreamWriter
import kotlinx.coroutines.launch
import coil.compose.AsyncImage
import com.example.data.LeaveRepository
import com.example.data.LeaveRequestEntity
import com.example.ui.AuthViewModel
import com.example.ui.components.NotificationTopBarAction
import com.example.ui.components.UserAvatar
import com.example.ui.theme.ThemeToggleIconButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    authViewModel: AuthViewModel? = null,
    onNavigateToRequest: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val currentUser by authViewModel?.currentUser?.collectAsState() ?: remember { mutableStateOf(null) }
    val userAvatarUrl = currentUser?.avatarUrl ?: ""
    val userEmail = currentUser?.email ?: ""
    val userName = currentUser?.fullName ?: "Collaborateur"

    val leaveRepository = remember { LeaveRepository.getInstance(context) }
    val requests by leaveRepository.getEmployeeRequestsFlow(userEmail).collectAsState(initial = emptyList())

    // State preservation across screen rotation
    var selectedFilter by rememberSaveable { mutableStateOf("Tous") }
    var selectedRequestId by rememberSaveable { mutableStateOf<String?>(null) }
    var isCalendarExpandedInPortrait by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val selectedRequestForDetail = remember(requests, selectedRequestId) {
        requests.find { it.id == selectedRequestId }
    }

    val filteredRequests = remember(requests, selectedFilter) {
        when (selectedFilter) {
            "En attente" -> requests.filter { it.status == "PENDING" }
            "Validés" -> requests.filter { it.status == "APPROVED" }
            "Refusés" -> requests.filter { it.status == "REJECTED" }
            else -> requests
        }
    }

    val csvExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    OutputStreamWriter(outputStream, "UTF-8").use { writer ->
                        // BOM for Excel
                        writer.write("\uFEFF")
                        writer.write("ID,Type,Date de début,Date de fin,Jours,Statut,Créé le\n")
                        filteredRequests.forEach { req ->
                            val statusStr = when (req.status) {
                                "PENDING" -> "En attente"
                                "APPROVED" -> "Approuvé"
                                "REJECTED" -> "Refusé"
                                else -> req.status
                            }
                            val type = "\"${req.leaveType.replace("\"", "\"\"")}\""
                            val days = "${req.daysCount}"
                            val startDate = "\"${req.startDate}\""
                            val endDate = "\"${req.endDate}\""
                            val created = "\"${req.createdAt}\""
                            
                            writer.write("${req.id},$type,$startDate,$endDate,$days,$statusStr,$created\n")
                        }
                    }
                }
                Toast.makeText(context, "Export CSV réussi", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Erreur d'export: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        UserAvatar(
                            avatarUrl = userAvatarUrl,
                            fullName = userName,
                            size = 40.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "TimeOff",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val sdf = SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.getDefault())
                            val timestamp = sdf.format(Date())
                            csvExportLauncher.launch("demandes_conges_$timestamp.csv")
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Download,
                            contentDescription = "Exporter en CSV",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    ThemeToggleIconButton()
                    NotificationTopBarAction(userEmail = userEmail)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Détection réactive de l'orientation et de la largeur disponible :
            // Mode Paysage (smartphone pivoté, tablette, ordinateur) -> Disposition 2 colonnes côte-à-côte
            // Mode Portrait (smartphone vertical) -> Disposition colonne unique fluide avec calendrier repliable
            val isLandscapeLayout = maxWidth >= 760.dp || (maxWidth > maxHeight && maxWidth >= 480.dp)

            if (isLandscapeLayout) {
                // ==========================================
                // DISPOSITION PAYSAGE / GRANDS ÉCRANS (2 COLONNES)
                // ==========================================
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Colonne gauche (58% de largeur) : Titre, Export, Filtres et Liste des demandes
                    Column(
                        modifier = Modifier
                            .weight(1.15f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // En-tête adaptatif avec titre, compteur et bouton Export CSV
                        HistoryHeader(
                            requestCount = requests.size,
                            onExportCsv = {
                                val sdf = SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.getDefault())
                                val timestamp = sdf.format(Date())
                                csvExportLauncher.launch("demandes_conges_$timestamp.csv")
                            }
                        )

                        // Filtres avec badges de comptage
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val filterOptions = listOf("Tous", "En attente", "Validés", "Refusés")
                            items(filterOptions) { filter ->
                                val isSelected = selectedFilter == filter
                                val count = when (filter) {
                                    "Tous" -> requests.size
                                    "En attente" -> requests.count { it.status == "PENDING" }
                                    "Validés" -> requests.count { it.status == "APPROVED" }
                                    "Refusés" -> requests.count { it.status == "REJECTED" }
                                    else -> 0
                                }
                                Surface(
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    shape = CircleShape,
                                    modifier = Modifier.clickable { selectedFilter = filter }
                                ) {
                                    Text(
                                        text = "$filter ($count)",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                    )
                                }
                            }
                        }

                        // Liste des demandes ou écran d'état vide sous forme de LazyColumn fluide sans risque de conflit de scroll
                        LazyColumn(
                            state = listState,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            if (requests.isEmpty()) {
                                item(key = "empty_guide_landscape") {
                                    EmptyLeaveRequestsGuide(onNavigateToRequest = onNavigateToRequest)
                                }
                            } else if (filteredRequests.isEmpty()) {
                                item(key = "empty_filter_landscape") {
                                    EmptyFilterState(
                                        selectedFilter = selectedFilter,
                                        onResetFilter = { selectedFilter = "Tous" }
                                    )
                                }
                            } else {
                                items(filteredRequests, key = { it.id }) { item ->
                                    LeaveHistoryItemRow(
                                        item = item,
                                        onDelete = {
                                            scope.launch {
                                                leaveRepository.deleteLeaveRequest(item)
                                            }
                                        },
                                        onClick = { selectedRequestId = item.id }
                                    )
                                }
                            }
                        }
                    }

                    // Colonne droite (42% de largeur) : Calendrier interactif & Bilan des demandes
                    // IMPORTANT : verticalScroll garantit un affichage sans débordement même sur un smartphone en paysage à hauteur réduite
                    Column(
                        modifier = Modifier
                            .weight(0.85f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        EmployeeLeaveCalendarCard(currentUserEmail = userEmail)
                        LeaveSummaryStatsCard(requests = requests)
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            } else {
                // ==========================================
                // DISPOSITION PORTRAIT (COLONNE UNIQUE FLUIDE)
                // ==========================================
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 10.dp, bottom = 28.dp)
                ) {
                    // En-tête titre & Bouton Export CSV adaptatif
                    item(key = "header") {
                        HistoryHeader(
                            requestCount = requests.size,
                            onExportCsv = {
                                val sdf = SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.getDefault())
                                val timestamp = sdf.format(Date())
                                csvExportLauncher.launch("demandes_conges_$timestamp.csv")
                            }
                        )
                    }

                    // Section Calendrier avec repliage animé pour préserver l'espace visuel en mode portrait
                    item(key = "calendar_section") {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { isCalendarExpandedInPortrait = !isCalendarExpandedInPortrait }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            modifier = Modifier.size(38.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Filled.DateRange,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(20.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                        Column {
                                            Text(
                                                text = "Calendrier de mes congés",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = if (isCalendarExpandedInPortrait) "Appuyer pour réduire le calendrier" else "Appuyer pour voir les dates sur le calendrier",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Icon(
                                        imageVector = if (isCalendarExpandedInPortrait) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                        contentDescription = if (isCalendarExpandedInPortrait) "Réduire" else "Agrandir",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                AnimatedVisibility(
                                    visible = isCalendarExpandedInPortrait,
                                    enter = expandVertically() + fadeIn(),
                                    exit = shrinkVertically() + fadeOut()
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                        )
                                        EmployeeLeaveCalendarCard(currentUserEmail = userEmail)
                                    }
                                }
                            }
                        }
                    }

                    // Filtres par statut (Tous, En attente, Validés, Refusés)
                    item(key = "filters") {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val filterOptions = listOf("Tous", "En attente", "Validés", "Refusés")
                            items(filterOptions) { filter ->
                                val isSelected = selectedFilter == filter
                                val count = when (filter) {
                                    "Tous" -> requests.size
                                    "En attente" -> requests.count { it.status == "PENDING" }
                                    "Validés" -> requests.count { it.status == "APPROVED" }
                                    "Refusés" -> requests.count { it.status == "REJECTED" }
                                    else -> 0
                                }
                                Surface(
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    shape = CircleShape,
                                    modifier = Modifier.clickable { selectedFilter = filter }
                                ) {
                                    Text(
                                        text = "$filter ($count)",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Liste des demandes ou écrans d'assistance
                    if (requests.isEmpty()) {
                        item(key = "empty_guide") {
                            EmptyLeaveRequestsGuide(onNavigateToRequest = onNavigateToRequest)
                        }
                    } else if (filteredRequests.isEmpty()) {
                        item(key = "empty_filter") {
                            EmptyFilterState(
                                selectedFilter = selectedFilter,
                                onResetFilter = { selectedFilter = "Tous" }
                            )
                        }
                    } else {
                        items(filteredRequests, key = { it.id }) { item ->
                            LeaveHistoryItemRow(
                                item = item,
                                onDelete = {
                                    scope.launch {
                                        leaveRepository.deleteLeaveRequest(item)
                                    }
                                },
                                onClick = { selectedRequestId = item.id }
                            )
                        }

                        // Bilan statistique en fin de liste
                        item(key = "summary_stats") {
                            LeaveSummaryStatsCard(requests = requests)
                        }
                    }
                }
            }
        }
    }

    // Detail Dialog (avec conservation de l'état en rotation et scroll adaptatif pour écran paysage)
    selectedRequestForDetail?.let { req ->
        val statusDisplay = when (req.status) {
            "APPROVED" -> StatusConfig("Demande Validée", Color(0xFF15803D), Color(0xFFDCFCE7), Icons.Filled.CheckCircle)
            "REJECTED" -> StatusConfig("Demande Refusée", MaterialTheme.colorScheme.error, MaterialTheme.colorScheme.errorContainer, Icons.Filled.Close)
            else -> StatusConfig("En attente de validation", Color(0xFFC2410C), Color(0xFFFFEDD5), Icons.Filled.AccessTime)
        }

        AlertDialog(
            onDismissRequest = { selectedRequestId = null },
            modifier = Modifier.widthIn(max = 500.dp),
            icon = {
                Icon(
                    imageVector = statusDisplay.icon,
                    contentDescription = null,
                    tint = statusDisplay.textColor,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "${req.leaveType} (${req.daysCount} jour${if (req.daysCount > 1) "s" else ""})",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = statusDisplay.bgColor,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Statut : ${statusDisplay.label}",
                            color = statusDisplay.textColor,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(10.dp),
                            textAlign = TextAlign.Center
                        )
                    }

                    Text(
                        text = "• Période : du ${req.startDate} au ${req.endDate}",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Text(
                        text = "• Motif renseigné : ${req.reason}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    val submissionDate = java.text.SimpleDateFormat("dd/MM/yyyy à HH:mm", java.util.Locale.getDefault()).format(java.util.Date(req.createdAt))
                    Text(
                        text = "• Fait le : $submissionDate",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    if (req.status == "APPROVED") {
                        val approver = req.adminComment?.takeIf { it.isNotBlank() } ?: "Direction des Ressources Humaines"
                        Text(
                            text = "• Statut : Validé et approuvé par l'administration RH",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF15803D)
                        )
                    }

                    if (!req.adminComment.isNullOrBlank()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Text(
                            text = "Commentaire / Décision de l'administration :",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = req.adminComment,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    val dateCreated = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(req.createdAt))
                    Text(
                        text = "Soumise le : $dateCreated",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            },
            confirmButton = {
                Button(onClick = { selectedRequestId = null }) {
                    Text("Fermer")
                }
            }
        )
    }
}

/**
 * En-tête adaptatif pour l'écran Historique (titre, sous-titre dynamique et bouton Export CSV).
 * Conçu pour s'ajuster avec fluidité sur tous les formats de téléphones (petits écrans, grands écrans, mode paysage, polices agrandies).
 */
@Composable
private fun HistoryHeader(
    requestCount: Int,
    onExportCsv: () -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val screenWidth = maxWidth
        val isNarrow = screenWidth < 370.dp

        if (isNarrow) {
            // Disposition adaptative pour téléphones étroits ou polices agrandies :
            // Titre sur la première ligne (sans coupure ni troncation),
            // compteur et bouton d'action CSV sur la deuxième ligne.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Historique de mes demandes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 2,
                    softWrap = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (requestCount == 0) "Aucune demande enregistrée" else "$requestCount demande${if (requestCount > 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    ExportCsvButton(onClick = onExportCsv)
                }
            }
        } else {
            // Disposition en ligne pour smartphones standards, grands formats et mode paysage
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp)
                ) {
                    Text(
                        text = "Historique de mes demandes",
                        style = if (screenWidth >= 600.dp) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 2,
                        softWrap = true
                    )
                    Text(
                        text = if (requestCount == 0) "Suivi en temps réel de vos congés" else "$requestCount demande${if (requestCount > 1) "s" else ""} enregistrée${if (requestCount > 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                ExportCsvButton(onClick = onExportCsv)
            }
        }
    }
}

/**
 * Bouton d'export CSV tactile, accessible (zone tactile >= 48dp) et harmonieux
 */
@Composable
private fun ExportCsvButton(onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
        ),
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Download,
                contentDescription = "Export CSV",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "CSV",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * Rangée pour une demande avec Swipe-to-dismiss pour annuler/supprimer si le statut est PENDING
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LeaveHistoryItemRow(
    item: LeaveRequestEntity,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.EndToStart || dismissValue == SwipeToDismissBoxValue.StartToEnd) {
                if (item.status == "PENDING") {
                    onDelete()
                    true
                } else {
                    false
                }
            } else {
                false
            }
        }
    )

    if (item.status == "PENDING") {
        SwipeToDismissBox(
            state = dismissState,
            backgroundContent = {
                val color by animateColorAsState(
                    when (dismissState.targetValue) {
                        SwipeToDismissBoxValue.Settled -> Color.Transparent
                        else -> MaterialTheme.colorScheme.error
                    },
                    label = "bg color"
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                        .background(color)
                        .padding(horizontal = 20.dp),
                    contentAlignment = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Supprimer",
                        tint = Color.White
                    )
                }
            }
        ) {
            LeaveHistoryCard(
                request = item,
                onClick = onClick
            )
        }
    } else {
        LeaveHistoryCard(
            request = item,
            onClick = onClick
        )
    }
}

/**
 * Carte récapitulative des statistiques de demandes (Total, Validées, En attente, Refusées)
 */
@Composable
private fun LeaveSummaryStatsCard(requests: List<LeaveRequestEntity>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Bilan de vos demandes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total soumises :", style = MaterialTheme.typography.bodyMedium)
                Text("${requests.size}", fontWeight = FontWeight.Bold)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Validées par la direction :", style = MaterialTheme.typography.bodyMedium)
                Text("${requests.count { it.status == "APPROVED" }}", fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("En attente d'approbation :", style = MaterialTheme.typography.bodyMedium)
                Text("${requests.count { it.status == "PENDING" }}", fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Refusées :", style = MaterialTheme.typography.bodyMedium)
                Text("${requests.count { it.status == "REJECTED" }}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

/**
 * Informative Empty State displayed when the user has not submitted any leave requests yet.
 * Guides them step-by-step on how leave requests work and provides a direct CTA to submit one.
 */
@Composable
private fun EmptyLeaveRequestsGuide(
    onNavigateToRequest: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Card with Illustration and Welcoming Guidance
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Layered Decorative Icon
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(76.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                    )
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DateRange,
                            contentDescription = null,
                            modifier = Modifier.size(30.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Text(
                    text = "Aucune demande déposée",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Vous n'avez pas encore soumis de demande d'absence ou de congé. Tout votre historique apparaîtra ici avec son statut en temps réel.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
                )

                if (onNavigateToRequest != null) {
                    Button(
                        onClick = onNavigateToRequest,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Déposer ma première demande",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Informative Step-by-Step Guide Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Comment fonctionne une demande ?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Step 1
                GuideStepItem(
                    stepNumber = "1",
                    title = "Sélectionnez le type de congé",
                    description = "Congés payés, RTT, Télétravail, Maladie ou Événement familial selon vos droits disponibles.",
                    icon = Icons.Filled.BeachAccess,
                    iconBg = MaterialTheme.colorScheme.primaryContainer,
                    iconColor = MaterialTheme.colorScheme.primary
                )

                // Step 2
                GuideStepItem(
                    stepNumber = "2",
                    title = "Définissez vos dates",
                    description = "Choisissez la période souhaitée. Le nombre de jours ouvrés est décompté automatiquement.",
                    icon = Icons.Filled.DateRange,
                    iconBg = MaterialTheme.colorScheme.secondaryContainer,
                    iconColor = MaterialTheme.colorScheme.secondary
                )

                // Step 3
                GuideStepItem(
                    stepNumber = "3",
                    title = "Suivez la validation par votre manager",
                    description = "Recevez une notification dès que la demande est examinée (Approuvée 🟢 ou Refusée 🔴).",
                    icon = Icons.Filled.CheckCircle,
                    iconBg = MaterialTheme.colorScheme.tertiaryContainer,
                    iconColor = MaterialTheme.colorScheme.tertiary
                )
            }
        }

        // Helpful Tip Banner
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "💡",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "Vos soldes de congés disponibles et pris sont toujours consultables en direct depuis l'onglet Accueil.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun GuideStepItem(
    stepNumber: String,
    title: String,
    description: String,
    icon: ImageVector,
    iconBg: Color,
    iconColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = MaterialTheme.typography.bodySmall.lineHeight
            )
        }
    }
}

/**
 * Informative Empty State displayed when requests exist, but none match the current filter.
 */
@Composable
private fun EmptyFilterState(
    selectedFilter: String,
    onResetFilter: () -> Unit
) {
    val (icon, title, description, iconColor) = when (selectedFilter) {
        "En attente" -> Quadruple(
            Icons.Filled.AccessTime,
            "Aucune demande en attente",
            "Toutes vos demandes soumises ont déjà été traitées par votre responsable.",
            Color(0xFFC2410C)
        )
        "Validés" -> Quadruple(
            Icons.Filled.CheckCircle,
            "Aucune demande validée",
            "Vos demandes approuvées par l'administration apparaîtront sous cet onglet.",
            Color(0xFF15803D)
        )
        "Refusés" -> Quadruple(
            Icons.Filled.Close,
            "Aucune demande refusée",
            "Bonne nouvelle ! Aucune de vos demandes de congé n'a fait l'objet d'un refus.",
            MaterialTheme.colorScheme.error
        )
        else -> Quadruple(
            Icons.Filled.FilterList,
            "Aucun résultat",
            "Aucune demande ne correspond au filtre actuellement sélectionné.",
            MaterialTheme.colorScheme.outline
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = iconColor
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            FilledTonalButton(
                onClick = onResetFilter,
                modifier = Modifier.padding(top = 4.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Afficher toutes les demandes")
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

private data class StatusConfig(
    val label: String,
    val textColor: Color,
    val bgColor: Color,
    val icon: ImageVector
)

@Composable
fun LeaveHistoryCard(
    request: LeaveRequestEntity,
    onClick: () -> Unit
) {
    val (icon, iconBg, iconColor) = when {
        request.leaveType.contains("RTT", ignoreCase = true) ->
            Triple(Icons.Filled.EventNote, MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.tertiary)
        request.leaveType.contains("Télétravail", ignoreCase = true) ->
            Triple(Icons.Filled.HomeWork, MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.secondary)
        request.leaveType.contains("Maladie", ignoreCase = true) ->
            Triple(Icons.Filled.MedicalServices, MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.error)
        request.leaveType.contains("Formation", ignoreCase = true) ->
            Triple(Icons.Filled.School, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.primary)
        else ->
            Triple(Icons.Filled.BeachAccess, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.primary)
    }

    val (statusLabel, statusTextCol, statusBgCol, statusIndicatorCol) = when (request.status) {
        "APPROVED" -> listOf("Validé", Color(0xFF15803D), Color(0xFFDCFCE7), Color(0xFF22C55E))
        "REJECTED" -> listOf("Refusé", Color(0xFFB91C1C), Color(0xFFFEE2E2), Color(0xFFEF4444))
        else -> listOf("En attente", Color(0xFFC2410C), Color(0xFFFFEDD5), Color(0xFFF97316))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = request.leaveType,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${request.startDate} - ${request.endDate}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    val submissionDate = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(request.createdAt))
                    Text(
                        text = "Déposée le: $submissionDate",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    if (request.status == "APPROVED") {
                        Text(
                            text = "Validé par la direction RH",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF15803D),
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (request.reason.isNotBlank()) {
                        Text(
                            text = request.reason,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(statusBgCol as Color)
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(statusIndicatorCol as Color)
                    )
                    Text(
                        text = statusLabel as String,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = statusTextCol as Color
                    )
                }
                Text(
                    text = "${request.daysCount} jour${if (request.daysCount > 1) "s" else ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}
