package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TrendingUp
import kotlinx.coroutines.launch
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import com.example.data.LeaveRepository
import com.example.data.LeaveRequestEntity
import com.example.data.User
import com.example.ui.AuthViewModel
import com.example.ui.components.NotificationTopBarAction
import com.example.ui.components.UserAvatar
import com.example.ui.theme.ThemeToggleIconButton
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DashboardApprovedLeave(
    val id: String,
    val title: String,
    val type: String,
    val startDay: Int,
    val endDay: Int,
    val month: Int, // 0-indexed Calendar.MONTH
    val year: Int,
    val durationDays: Int,
    val datesFormatted: String,
    val category: LeaveCategory,
    val countdownText: String,
    val details: String
)

enum class LeaveCategory {
    CONGES_PAYES,
    RTT,
    TELETTRAVAIL,
    EXCEPTIONNEL
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    authViewModel: AuthViewModel? = null,
    onNavigateToRequest: (() -> Unit)? = null
) {
    val currentUser by authViewModel?.currentUser?.collectAsState() ?: remember { mutableStateOf(null) }

    val employeeName = currentUser?.fullName?.takeIf { it.isNotBlank() } ?: "Collaborateur"
    val employeeFirstName = employeeName.split(" ").firstOrNull()?.uppercase() ?: "COLLABORATEUR"
    val userAvatarUrl = currentUser?.avatarUrl ?: ""

    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    val cpAllowance = currentUser?.paidLeaveAllowance ?: 25
    val cpUsed = currentUser?.paidLeaveUsed ?: 13
    val cpRemaining = (cpAllowance - cpUsed).coerceAtLeast(0)

    val rttAllowance = currentUser?.rttAllowance ?: 10
    val rttUsed = currentUser?.rttUsed ?: 5
    val rttRemaining = (rttAllowance - rttUsed).coerceAtLeast(0)

    val supabaseSync = remember { com.example.data.SupabaseSyncManager.getInstance(context) }
    var notesCount by remember { mutableIntStateOf(0) }
    var isLoadingNotes by remember { mutableStateOf(false) }

    LaunchedEffect(currentUser?.email) {
        if (currentUser != null) {
            isLoadingNotes = true
            try {
                // 1. Récupérer l'utilisateur authentifié Supabase
                val authUser = supabaseSync.getCurrentAuthUser()
                val targetUserId = authUser?.optString("id")?.takeIf { it.isNotBlank() }
                    ?: currentUser?.email
                    ?: ""

                // 2. Compter le nombre de notes dans la table "notes" où user_id = targetUserId
                if (targetUserId.isNotBlank()) {
                    notesCount = supabaseSync.getNotesCount(targetUserId)
                }
            } catch (_: Exception) {
                // Keep default 0 on error
            } finally {
                isLoadingNotes = false
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
                            fullName = employeeName,
                            size = 40.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "TimeOff",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = employeeName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                },
                actions = {
                    com.example.ui.i18n.LanguageSelector()
                    ThemeToggleIconButton()
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                com.example.data.FirestoreSyncManager.getInstance(context).syncAllLocalDataToCloud()
                                com.example.data.SupabaseSyncManager.getInstance(context).syncAll()
                                com.example.ui.NotificationSystem.sendNotification(
                                    title = com.example.ui.i18n.I18nManager.getString("dash_sync_title"),
                                    message = com.example.ui.i18n.I18nManager.getString("dash_sync"),
                                    isApproved = true
                                )
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Synchroniser avec le Cloud",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    NotificationTopBarAction(userEmail = currentUser?.email)
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
                .padding(paddingValues),
            contentAlignment = Alignment.TopCenter
        ) {
            // Responsive orientation check: multi-column balance display in landscape or on wide screens
            val isWideScreen = maxWidth >= 840.dp || (maxWidth > maxHeight && maxWidth >= 580.dp)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 1400.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(if (isWideScreen) 24.dp else 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
            // Welcome Section with dynamic user name
            Column {
                Text(
                    text = "BONJOUR, $employeeFirstName",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Tableau de bord",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                if (currentUser != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = "Matricule: ${currentUser?.matricule}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = "${currentUser?.jobTitle} • ${currentUser?.department}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            // ANNUAL ALLOWANCE PROGRESS BAR COMPONENT (Total Leave Days Used vs Annual Allowance)
            LeaveAllowanceProgressBarCard(
                annualPaidLeaveAllowance = cpAllowance,
                usedPaidLeaveDays = cpUsed,
                annualRttAllowance = rttAllowance,
                usedRttDays = rttUsed
            )

            // Soldes de congés Cards
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Soldes restants",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "Exercice en cours",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                CurrentMonthUsageCard(userEmail = currentUser?.email, context = context)

                if (isWideScreen) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        BalanceCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Filled.BeachAccess,
                            iconBgColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                            iconColor = MaterialTheme.colorScheme.primary,
                            title = com.example.ui.i18n.tr("type_cp"),
                            days = "$cpRemaining",
                            totalAllowance = "sur ${cpAllowance}j alloués",
                            progress = if (cpAllowance > 0) cpRemaining.toFloat() / cpAllowance else 0f
                        )
                        BalanceCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Filled.MedicalServices,
                            iconBgColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                            iconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            title = "Arrêts Maladie",
                            days = "$rttRemaining",
                            totalAllowance = "sur ${rttAllowance}j alloués",
                            progress = if (rttAllowance > 0) rttRemaining.toFloat() / rttAllowance else 0f
                        )
                        BalanceCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Filled.EventNote,
                            iconBgColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.25f),
                            iconColor = MaterialTheme.colorScheme.tertiary,
                            title = "Mes Notes (Supabase)",
                            days = if (isLoadingNotes) "..." else "$notesCount",
                            totalAllowance = "synchronisées pour cet utilisateur",
                            progress = 1f
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        BalanceCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Filled.BeachAccess,
                            iconBgColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                            iconColor = MaterialTheme.colorScheme.primary,
                            title = com.example.ui.i18n.tr("type_cp"),
                            days = "$cpRemaining",
                            totalAllowance = "sur ${cpAllowance}j alloués",
                            progress = if (cpAllowance > 0) cpRemaining.toFloat() / cpAllowance else 0f
                        )
                        BalanceCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Filled.MedicalServices,
                            iconBgColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                            iconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            title = "Arrêts Maladie",
                            days = "$rttRemaining",
                            totalAllowance = "sur ${rttAllowance}j alloués",
                            progress = if (rttAllowance > 0) rttRemaining.toFloat() / rttAllowance else 0f
                        )
                    }

                    // Carte du nombre réel de notes depuis Supabase
                    BalanceCard(
                        modifier = Modifier.fillMaxWidth(),
                        icon = Icons.Filled.EventNote,
                        iconBgColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.25f),
                        iconColor = MaterialTheme.colorScheme.tertiary,
                        title = "Mes Notes (Supabase)",
                        days = if (isLoadingNotes) "..." else "$notesCount",
                        totalAllowance = "synchronisées pour cet utilisateur",
                        progress = 1f
                    )
                }
            }

            // MANAGER DASHBOARD SECTION (FOR LEADS / MANAGERS)
            val isManager = currentUser?.jobTitle?.let { title ->
                title.contains("Lead", ignoreCase = true) || 
                title.contains("Responsable", ignoreCase = true) || 
                title.contains("Manager", ignoreCase = true)
            } ?: false

            if (isManager && currentUser != null) {
                val managerRepo = remember { com.example.data.LeaveRepository.getInstance(context) }
                val allRequests by managerRepo.getAllRequestsFlow().collectAsState(initial = emptyList())
                val directReportsPendingRequests = allRequests.filter { 
                    it.status == "PENDING" && 
                    it.department == currentUser!!.department && 
                    it.employeeEmail != currentUser!!.email 
                }
                
                val departmentRequests = allRequests.filter {
                    it.department == currentUser!!.department
                }

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = com.example.ui.i18n.tr("team_stats"),
                        style = MaterialTheme.typography.headlineSmall
                    )
                    MonthlyLeaveRequestsChart(requests = departmentRequests)
                }

                if (directReportsPendingRequests.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Notifications,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = "À valider pour votre équipe",
                                    style = MaterialTheme.typography.headlineSmall
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.errorContainer
                            ) {
                                Text(
                                    text = "${directReportsPendingRequests.size}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        directReportsPendingRequests.forEach { req ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = req.employeeName,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "${req.leaveType} (${req.daysCount}j)",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                                        ) {
                                            Text(
                                                text = "EN ATTENTE",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Filled.EventNote,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "${req.startDate} au ${req.endDate}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // CALENDAR & RECENT REQUESTS SECTIONS (Adaptive 2-column on desktop, single column on mobile)
            val calendarSection = @Composable {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Planning de mes congés approuvés",
                                style = MaterialTheme.typography.headlineSmall
                            )
                        }
                    }

                    EmployeeLeaveCalendarCard(currentUserEmail = currentUser?.email)
                }
            }

            val recentRequestsSection = @Composable {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Demandes récentes",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        if (onNavigateToRequest != null) {
                            TextButton(onClick = onNavigateToRequest) {
                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Nouvelle", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                    val context = LocalContext.current
                    val leaveRepository = remember { LeaveRepository.getInstance(context) }
                    val userEmail = currentUser?.email ?: com.example.data.LocalStorageManager(context).getCurrentUser()?.email
                    val allDbRequests by remember(userEmail) {
                        if (!userEmail.isNullOrBlank()) {
                            leaveRepository.getEmployeeRequestsFlow(userEmail)
                        } else {
                            kotlinx.coroutines.flow.flowOf(emptyList<com.example.data.LeaveRequestEntity>())
                        }
                    }.collectAsState(initial = emptyList())
                    
                    if (allDbRequests.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.DateRange,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Text(
                                    text = "Aucune demande récente",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Vos demandes d'absences apparaîtront ici.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                if (onNavigateToRequest != null) {
                                    FilledTonalButton(
                                        onClick = onNavigateToRequest,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.padding(top = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Add,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Nouvelle demande", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                    } else {
                        allDbRequests.take(4).forEach { req ->
                            val (statusText, statusColor, statusBg) = when (req.status.uppercase(Locale.getDefault())) {
                                "APPROVED" -> Triple(
                                    "APPROUVÉ",
                                    Color(0xFF10B981), // Emerald Green
                                    Color(0xFFD1FAE5) // Light Emerald background
                                )
                                "REJECTED" -> Triple(
                                    "REFUSÉ",
                                    Color(0xFFEF4444), // Red
                                    Color(0xFFFEE2E2) // Light Red background
                                )
                                else -> Triple(
                                    "EN ATTENTE",
                                    Color(0xFFF59E0B), // Amber
                                    Color(0xFFFEF3C7) // Light Amber background
                                )
                            }

                            val icon = when {
                                req.leaveType.contains("Payé", ignoreCase = true) -> Icons.Filled.BeachAccess
                                req.leaveType.contains(com.example.ui.i18n.I18nManager.getString("type_rtt"), ignoreCase = true) -> Icons.Filled.Schedule
                                req.leaveType.contains(com.example.ui.i18n.I18nManager.getString("type_remote"), ignoreCase = true) -> Icons.Filled.HomeWork
                                else -> Icons.Filled.EventBusy
                            }

                            val formattedDate = if (req.startDate == req.endDate) req.startDate else "${req.startDate} - ${req.endDate}"

                            RequestCard(
                                icon = icon,
                                date = formattedDate,
                                type = "${req.leaveType} (${req.daysCount}j)",
                                status = statusText,
                                statusColor = statusColor,
                                statusBg = statusBg
                            )
                        }
                    }
                }
            }

            if (isWideScreen) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Box(modifier = Modifier.weight(1.15f)) {
                        calendarSection()
                    }
                    Box(modifier = Modifier.weight(0.85f)) {
                        recentRequestsSection()
                    }
                }
            } else {
                calendarSection()
                recentRequestsSection()
            }

            Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun LeaveAllowanceProgressBarCard(
    annualPaidLeaveAllowance: Int = 25,
    usedPaidLeaveDays: Int = 13,
    annualRttAllowance: Int = 10,
    usedRttDays: Int = 5
) {
    val totalAnnualAllowance = (annualPaidLeaveAllowance + annualRttAllowance).coerceAtLeast(1)
    val totalDaysUsed = usedPaidLeaveDays + usedRttDays
    val remainingDays = (totalAnnualAllowance - totalDaysUsed).coerceAtLeast(0)
    val usagePercentage = (totalDaysUsed.toFloat() / totalAnnualAllowance.toFloat()).coerceIn(0f, 1f)
    val cpUsagePercentage = (usedPaidLeaveDays.toFloat() / annualPaidLeaveAllowance.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
    val rttUsagePercentage = (usedRttDays.toFloat() / annualRttAllowance.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)

    val animatedOverallProgress by animateFloatAsState(
        targetValue = usagePercentage,
        animationSpec = tween(durationMillis = 900),
        label = "overallProgress"
    )

    val animatedCpProgress by animateFloatAsState(
        targetValue = cpUsagePercentage,
        animationSpec = tween(durationMillis = 900),
        label = "cpProgress"
    )

    val animatedRttProgress by animateFloatAsState(
        targetValue = rttUsagePercentage,
        animationSpec = tween(durationMillis = 900),
        label = "rttProgress"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Card Header with Icon & Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.TrendingUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Consommation annuelle",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Quota annuel : $totalAnnualAllowance jours",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                ) {
                    Text(
                        text = "${(usagePercentage * 100).toInt()}% consommés",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            // Main Global Stats Display
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "$totalDaysUsed",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "/ $totalAnnualAllowance jours pris",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondary)
                        )
                        Text(
                            text = "$remainingDays j restants",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Segmented Global Visual Progress Bar
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                ) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        val cpShare = usedPaidLeaveDays.toFloat() / totalAnnualAllowance.toFloat()
                        val rttShare = usedRttDays.toFloat() / totalAnnualAllowance.toFloat()

                        if (cpShare > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(cpShare * animatedOverallProgress / usagePercentage.coerceAtLeast(0.01f))
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                        if (rttShare > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(rttShare * animatedOverallProgress / usagePercentage.coerceAtLeast(0.01f))
                                    .background(MaterialTheme.colorScheme.secondary)
                            )
                        }
                        val remainingShare = (totalAnnualAllowance - totalDaysUsed).toFloat() / totalAnnualAllowance.toFloat()
                        if (remainingShare > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(remainingShare)
                                    .background(Color.Transparent)
                            )
                        }
                    }
                }

                // Global Bar Segment Legend
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "0j",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = "Mi-parcours (${totalAnnualAllowance / 2}j)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = "${totalAnnualAllowance}j",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)

            // Category Breakdown Progress Bars (Congés Payés & RTT)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Détail par type de congé",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Congés Payés Bar
                LeaveCategoryProgressRow(
                    title = com.example.ui.i18n.tr("type_cp"),
                    usedDays = usedPaidLeaveDays,
                    totalAllowance = annualPaidLeaveAllowance,
                    progress = animatedCpProgress,
                    barColor = MaterialTheme.colorScheme.primary,
                    icon = Icons.Filled.BeachAccess
                )

                // RTT Bar
                LeaveCategoryProgressRow(
                    title = com.example.ui.i18n.tr("type_rtt"),
                    usedDays = usedRttDays,
                    totalAllowance = annualRttAllowance,
                    progress = animatedRttProgress,
                    barColor = MaterialTheme.colorScheme.secondary,
                    icon = Icons.Filled.Schedule
                )
            }

            // Info note
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Validité des congés payés de l'exercice en cours jusqu'au 31 Mai.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun LeaveCategoryProgressRow(
    title: String,
    usedDays: Int,
    totalAllowance: Int,
    progress: Float,
    barColor: Color,
    icon: ImageVector
) {
    val remaining = (totalAllowance - usedDays).coerceAtLeast(0)
    val safeAllowance = totalAllowance.coerceAtLeast(1)
    val percent = ((usedDays.toFloat() / safeAllowance.toFloat()) * 100).toInt()

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = barColor,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$usedDays / ${totalAllowance}j",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "($percent%)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = barColor,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            strokeCap = StrokeCap.Round
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "$usedDays jours consommés",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
            Text(
                text = "$remaining jours restants",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = barColor
            )
        }
    }
}

@Composable
fun EmployeeLeaveCalendarCard(currentUserEmail: String? = null) {
    val context = LocalContext.current
    val leaveRepository = remember { LeaveRepository.getInstance(context) }
    val effectiveEmail = remember(currentUserEmail) {
        currentUserEmail?.takeIf { it.isNotBlank() }
            ?: com.example.data.LocalStorageManager(context).getCurrentUser()?.email
    }

    val dbRequests by remember(effectiveEmail) {
        if (!effectiveEmail.isNullOrBlank()) {
            leaveRepository.getEmployeeRequestsFlow(effectiveEmail)
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList<com.example.data.LeaveRequestEntity>())
        }
    }.collectAsState(initial = emptyList())

    val nowCal = remember { Calendar.getInstance() }
    val currentRealMonth = remember { nowCal.get(Calendar.MONTH) }
    val currentRealYear = remember { nowCal.get(Calendar.YEAR) }
    val currentRealDay = remember { nowCal.get(Calendar.DAY_OF_MONTH) }

    var displayedMonth by rememberSaveable { mutableIntStateOf(currentRealMonth) }
    var displayedYear by rememberSaveable { mutableIntStateOf(currentRealYear) }
    var selectedDay by rememberSaveable { mutableStateOf<Int?>(currentRealDay) }

    val approvedLeaves = remember(dbRequests, effectiveEmail) {
        val userApproved = dbRequests.filter { req ->
            req.status.equals("APPROVED", ignoreCase = true) &&
            (effectiveEmail.isNullOrBlank() || req.employeeEmail.trim().equals(effectiveEmail.trim(), ignoreCase = true))
        }
        userApproved.map { req ->
            val cat = when {
                req.leaveType.contains("Payé", ignoreCase = true) -> LeaveCategory.CONGES_PAYES
                req.leaveType.contains(com.example.ui.i18n.I18nManager.getString("type_rtt"), ignoreCase = true) -> LeaveCategory.RTT
                req.leaveType.contains(com.example.ui.i18n.I18nManager.getString("type_remote"), ignoreCase = true) -> LeaveCategory.TELETTRAVAIL
                else -> LeaveCategory.EXCEPTIONNEL
            }

            val formattedDates = if (req.startDate == req.endDate) req.startDate else "${req.startDate} - ${req.endDate}"

            val detailsText = if (req.adminComment?.isNotBlank() == true) {
                "${req.daysCount}j validé(s) - Note: ${req.adminComment}"
            } else {
                "${req.daysCount}j validé(s) par la direction"
            }

            DashboardApprovedLeave(
                id = req.id,
                title = if (req.reason.isNotBlank()) req.reason else req.leaveType,
                type = req.leaveType,
                startDay = req.startDay,
                endDay = req.endDay,
                month = req.month,
                year = req.year,
                durationDays = req.daysCount,
                datesFormatted = formattedDates,
                category = cat,
                countdownText = "Validé RH",
                details = detailsText
            )
        }
    }

    val cal = remember(displayedMonth, displayedYear) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, displayedYear)
            set(Calendar.MONTH, displayedMonth)
            set(Calendar.DAY_OF_MONTH, 1)
        }
    }

    val monthName = remember(displayedMonth, displayedYear) {
        val sdf = SimpleDateFormat("MMMM yyyy", Locale.FRENCH)
        sdf.format(cal.time).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.FRENCH) else it.toString() }
    }

    val maxDaysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val rawFirstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
    val firstDayOffset = (rawFirstDayOfWeek + 5) % 7 // 0 for Monday

    val currentMonthLeaves = remember(displayedMonth, displayedYear, approvedLeaves) {
        approvedLeaves.filter { it.month == displayedMonth && it.year == displayedYear }
    }

    val activeLeave = remember(selectedDay, currentMonthLeaves) {
        if (selectedDay == null) null
        else currentMonthLeaves.find { selectedDay!! in it.startDay..it.endDay }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Month Header & Navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = monthName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (currentMonthLeaves.isNotEmpty()) "${currentMonthLeaves.size} période(s) approuvée(s)" else "Aucun congé ce mois-ci",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (currentMonthLeaves.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            if (displayedMonth == Calendar.JANUARY) {
                                displayedMonth = Calendar.DECEMBER
                                displayedYear -= 1
                            } else {
                                displayedMonth -= 1
                            }
                            selectedDay = null
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Mois précédent",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    TextButton(
                        onClick = {
                            displayedMonth = currentRealMonth
                            displayedYear = currentRealYear
                            selectedDay = currentRealDay
                        }
                    ) {
                        Text(
                            text = "Aujourd'hui",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    IconButton(
                        onClick = {
                            if (displayedMonth == Calendar.DECEMBER) {
                                displayedMonth = Calendar.JANUARY
                                displayedYear += 1
                            } else {
                                displayedMonth += 1
                            }
                            selectedDay = null
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Mois suivant",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Days of week header (LUN - DIM)
            val daysOfWeek = listOf("Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                daysOfWeek.forEachIndexed { index, day ->
                    val isWeekend = index >= 5
                    Text(
                        text = day.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isWeekend) MaterialTheme.colorScheme.outline.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Calendar Days Grid
            val totalCells = firstDayOffset + maxDaysInMonth
            val rows = Math.ceil(totalCells / 7.0).toInt()

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                for (r in 0 until rows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        for (c in 0 until 7) {
                            val index = r * 7 + c
                            val dayNumber = index - firstDayOffset + 1

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (dayNumber in 1..maxDaysInMonth) {
                                    val matchingLeave = currentMonthLeaves.find { dayNumber in it.startDay..it.endDay }
                                    val isSelected = selectedDay == dayNumber
                                    val isWeekend = c >= 5

                                    val cellBgColor = when {
                                        matchingLeave != null -> when (matchingLeave.category) {
                                            LeaveCategory.CONGES_PAYES -> MaterialTheme.colorScheme.primaryContainer
                                            LeaveCategory.RTT -> MaterialTheme.colorScheme.secondaryContainer
                                            LeaveCategory.TELETTRAVAIL -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
                                            LeaveCategory.EXCEPTIONNEL -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                                        }
                                        isSelected -> MaterialTheme.colorScheme.surfaceContainerHigh
                                        else -> Color.Transparent
                                    }

                                    val textColor = when {
                                        matchingLeave != null -> when (matchingLeave.category) {
                                            LeaveCategory.CONGES_PAYES -> MaterialTheme.colorScheme.onPrimaryContainer
                                            LeaveCategory.RTT -> MaterialTheme.colorScheme.onSecondaryContainer
                                            LeaveCategory.TELETTRAVAIL -> MaterialTheme.colorScheme.onTertiaryContainer
                                            LeaveCategory.EXCEPTIONNEL -> MaterialTheme.colorScheme.onErrorContainer
                                        }
                                        isWeekend -> MaterialTheme.colorScheme.outline
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }

                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(cellBgColor)
                                            .then(
                                                if (isSelected) {
                                                    Modifier.border(
                                                        1.5.dp,
                                                        MaterialTheme.colorScheme.primary,
                                                        RoundedCornerShape(8.dp)
                                                    )
                                                } else Modifier
                                            )
                                            .clickable {
                                                selectedDay = dayNumber
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = dayNumber.toString(),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (matchingLeave != null || isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = textColor
                                            )
                                            if (matchingLeave != null) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(4.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            when (matchingLeave.category) {
                                                                LeaveCategory.CONGES_PAYES -> MaterialTheme.colorScheme.primary
                                                                LeaveCategory.RTT -> MaterialTheme.colorScheme.secondary
                                                                LeaveCategory.TELETTRAVAIL -> MaterialTheme.colorScheme.tertiary
                                                                LeaveCategory.EXCEPTIONNEL -> MaterialTheme.colorScheme.error
                                                            }
                                                        )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Color-Coded Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendItem(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    dotColor = MaterialTheme.colorScheme.primary,
                    label = com.example.ui.i18n.tr("type_cp")
                )
                LegendItem(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    dotColor = MaterialTheme.colorScheme.secondary,
                    label = com.example.ui.i18n.tr("type_rtt")
                )
                LegendItem(
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                    dotColor = MaterialTheme.colorScheme.tertiary,
                    label = com.example.ui.i18n.tr("type_remote")
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)
            Spacer(modifier = Modifier.height(14.dp))

            // Selected or Upcoming Leave Detail Card
            if (activeLeave != null) {
                LeaveDetailBanner(leave = activeLeave)
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Sélectionnez un jour surligné pour afficher les détails du congé.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Quick list of all upcoming approved leaves
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Mes prochains congés validés",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (approvedLeaves.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    approvedLeaves.forEach { leave ->
                        UpcomingLeaveMiniRow(
                            leave = leave,
                            isSelected = displayedMonth == leave.month && displayedYear == leave.year && selectedDay in leave.startDay..leave.endDay,
                            onClick = {
                                displayedMonth = leave.month
                                displayedYear = leave.year
                                selectedDay = leave.startDay
                            }
                        )
                    }
                }
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.EventAvailable,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Aucun congé approuvé pour le moment. Vos congés personnels validés s'afficheront ici.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LegendItem(
    color: Color,
    dotColor: Color,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color)
                .border(1.dp, dotColor.copy(alpha = 0.5f), RoundedCornerShape(3.dp))
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun LeaveDetailBanner(leave: DashboardApprovedLeave) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = when (leave.category) {
            LeaveCategory.CONGES_PAYES -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            LeaveCategory.RTT -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
            LeaveCategory.TELETTRAVAIL -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
            LeaveCategory.EXCEPTIONNEL -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        },
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            when (leave.category) {
                LeaveCategory.CONGES_PAYES -> MaterialTheme.colorScheme.primaryContainer
                LeaveCategory.RTT -> MaterialTheme.colorScheme.secondaryContainer
                LeaveCategory.TELETTRAVAIL -> MaterialTheme.colorScheme.tertiaryContainer
                LeaveCategory.EXCEPTIONNEL -> MaterialTheme.colorScheme.errorContainer
            }
        )
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = when (leave.category) {
                            LeaveCategory.CONGES_PAYES -> Icons.Filled.BeachAccess
                            LeaveCategory.RTT -> Icons.Filled.Schedule
                            LeaveCategory.TELETTRAVAIL -> Icons.Filled.HomeWork
                            LeaveCategory.EXCEPTIONNEL -> Icons.Filled.EventBusy
                        },
                        contentDescription = null,
                        tint = when (leave.category) {
                            LeaveCategory.CONGES_PAYES -> MaterialTheme.colorScheme.primary
                            LeaveCategory.RTT -> MaterialTheme.colorScheme.secondary
                            LeaveCategory.TELETTRAVAIL -> MaterialTheme.colorScheme.tertiary
                            LeaveCategory.EXCEPTIONNEL -> MaterialTheme.colorScheme.error
                        },
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = leave.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        text = "APPROUVÉ",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Text(
                text = "${leave.datesFormatted} • ${leave.durationDays} jour(s)",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = leave.details,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = leave.countdownText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun UpcomingLeaveMiniRow(
    leave: DashboardApprovedLeave,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainerLowest,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            when (leave.category) {
                                LeaveCategory.CONGES_PAYES -> MaterialTheme.colorScheme.primaryContainer
                                LeaveCategory.RTT -> MaterialTheme.colorScheme.secondaryContainer
                                LeaveCategory.TELETTRAVAIL -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                                LeaveCategory.EXCEPTIONNEL -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (leave.category) {
                            LeaveCategory.CONGES_PAYES -> Icons.Filled.BeachAccess
                            LeaveCategory.RTT -> Icons.Filled.Schedule
                            LeaveCategory.TELETTRAVAIL -> Icons.Filled.HomeWork
                            LeaveCategory.EXCEPTIONNEL -> Icons.Filled.EventBusy
                        },
                        contentDescription = null,
                        tint = when (leave.category) {
                            LeaveCategory.CONGES_PAYES -> MaterialTheme.colorScheme.primary
                            LeaveCategory.RTT -> MaterialTheme.colorScheme.secondary
                            LeaveCategory.TELETTRAVAIL -> MaterialTheme.colorScheme.tertiary
                            LeaveCategory.EXCEPTIONNEL -> MaterialTheme.colorScheme.error
                        },
                        modifier = Modifier.size(16.dp)
                    )
                }

                Column {
                    Text(
                        text = leave.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = leave.datesFormatted,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Text(
                    text = leave.countdownText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun BalanceCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconBgColor: Color,
    iconColor: Color,
    title: String,
    days: String,
    totalAllowance: String,
    progress: Float
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline
                )
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = days,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "jours",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                Text(
                    text = totalAllowance,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = iconColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}

@Composable
fun RequestCard(
    icon: ImageVector,
    date: String,
    type: String,
    status: String,
    statusColor: Color,
    statusBg: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = date,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = type,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(4.dp),
                color = statusBg
            ) {
                Text(
                    text = status,
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
@Composable
fun MonthlyLeaveRequestsChart(requests: List<LeaveRequestEntity>) {
    val currentYear = remember { Calendar.getInstance().get(Calendar.YEAR) }
    val monthNames = listOf("Jan", "Fév", "Mar", "Avr", "Mai", "Juin", "Juil", "Août", "Sep", "Oct", "Nov", "Déc")
    
    // Calculate counts per month for the current year
    val counts = remember(requests) {
        val yearRequests = requests.filter { it.year == currentYear }
        val monthCounts = IntArray(12) { 0 }
        yearRequests.forEach { req ->
            if (req.month in 0..11) {
                monthCounts[req.month]++
            }
        }
        monthCounts.toList()
    }
    
    val maxCount = counts.maxOrNull()?.coerceAtLeast(1) ?: 1

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Demandes de congés par mois ($currentYear)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                counts.forEachIndexed { index, count ->
                    val heightFraction = count.toFloat() / maxCount.toFloat()
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (count > 0) {
                            Text(
                                text = count.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.6f)
                                .fillMaxHeight(heightFraction.coerceAtLeast(0.01f))
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(
                                    if (count > 0) MaterialTheme.colorScheme.primary 
                                    else MaterialTheme.colorScheme.surfaceContainerHigh
                                )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = monthNames[index],
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun CurrentMonthUsageCard(userEmail: String?, context: android.content.Context) {
    if (userEmail == null) return
    val leaveRepository = remember { LeaveRepository.getInstance(context) }
    val requests by leaveRepository.getEmployeeRequestsFlow(userEmail).collectAsState(initial = emptyList())
    
    val currentMonth = remember { Calendar.getInstance().get(Calendar.MONTH) }
    val currentYear = remember { Calendar.getInstance().get(Calendar.YEAR) }
    val monthNames = listOf("Janvier", "Février", "Mars", "Avril", "Mai", "Juin", "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre")
    
    val monthName = monthNames[currentMonth]
    
    // Count days approved this month
    val daysThisMonth = remember(requests, currentMonth, currentYear) {
        requests.filter { 
            it.status.uppercase() == "APPROVED" && 
            it.year == currentYear && 
            it.month == currentMonth 
        }.sumOf { it.daysCount.toDouble() }.toFloat()
    }
    
    // Assuming 21 working days per month approx
    val totalWorkingDays = 21f
    val progress = (daysThisMonth / totalWorkingDays).coerceIn(0f, 1f)
    
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.size(64.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    strokeWidth = 6.dp
                )
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(64.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 6.dp,
                    strokeCap = StrokeCap.Round
                )
                Text(
                    text = "${if (daysThisMonth % 1.0f == 0.0f) daysThisMonth.toInt() else daysThisMonth}j",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Consommation du mois",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$daysThisMonth jours de congés utilisés en $monthName.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
