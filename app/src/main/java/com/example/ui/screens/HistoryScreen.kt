package com.example.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.unit.dp
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
    val userEmail = currentUser?.email ?: "mouad@gmail.com"
    val userName = currentUser?.fullName ?: "Collaborateur"

    val leaveRepository = remember { LeaveRepository.getInstance(context) }
    val requests by leaveRepository.getEmployeeRequestsFlow(userEmail).collectAsState(initial = emptyList())

    var selectedFilter by remember { mutableStateOf("Tous") }
    var selectedRequestForDetail by remember { mutableStateOf<LeaveRequestEntity?>(null) }

    val filteredRequests = remember(requests, selectedFilter) {
        when (selectedFilter) {
            "En attente" -> requests.filter { it.status == "PENDING" }
            "Validés" -> requests.filter { it.status == "APPROVED" }
            "Refusés" -> requests.filter { it.status == "REJECTED" }
            else -> requests
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
                    ThemeToggleIconButton()
                    NotificationTopBarAction(userEmail = userEmail)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))
            // Header & Filters
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Historique de mes demandes",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            // History List or Informative Empty States
            if (requests.isEmpty()) {
                // User has never submitted any request yet
                EmptyLeaveRequestsGuide(
                    onNavigateToRequest = onNavigateToRequest
                )
            } else if (filteredRequests.isEmpty()) {
                // User has requests, but none match the current filter
                EmptyFilterState(
                    selectedFilter = selectedFilter,
                    onResetFilter = { selectedFilter = "Tous" }
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    items(filteredRequests, key = { it.id }) { item ->
                        LeaveHistoryCard(
                            request = item,
                            onClick = { selectedRequestForDetail = item }
                        )
                    }
                }
            }
        }
    }

    // Detail Dialog
    selectedRequestForDetail?.let { req ->
        val statusDisplay = when (req.status) {
            "APPROVED" -> StatusConfig("Demande Validée", Color(0xFF15803D), Color(0xFFDCFCE7), Icons.Filled.CheckCircle)
            "REJECTED" -> StatusConfig("Demande Refusée", MaterialTheme.colorScheme.error, MaterialTheme.colorScheme.errorContainer, Icons.Filled.Close)
            else -> StatusConfig("En attente de validation", Color(0xFFC2410C), Color(0xFFFFEDD5), Icons.Filled.AccessTime)
        }

        AlertDialog(
            onDismissRequest = { selectedRequestForDetail = null },
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
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                Button(onClick = { selectedRequestForDetail = null }) {
                    Text("Fermer")
                }
            }
        )
    }
}

/**
 * Informative Empty State displayed when the user has not submitted any leave requests yet.
 * Guides them step-by-step on how leave requests work and provides a direct CTA to submit one.
 */
@Composable
private fun EmptyLeaveRequestsGuide(
    onNavigateToRequest: (() -> Unit)? = null
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
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
                Column {
                    Text(
                        text = request.leaveType,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${request.startDate} - ${request.endDate}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                    if (request.reason.isNotBlank()) {
                        Text(
                            text = request.reason,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
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
