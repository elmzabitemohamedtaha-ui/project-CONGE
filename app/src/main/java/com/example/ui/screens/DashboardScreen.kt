package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TrendingUp
import kotlinx.coroutines.launch
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
                    ThemeToggleIconButton()
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                com.example.data.FirestoreSyncManager.getInstance(context).syncAllLocalDataToCloud()
                                com.example.ui.NotificationSystem.sendNotification(
                                    title = "Synchronisation Cloud",
                                    message = "Vos demandes de congé et profil sont synchronisés en temps réel.",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BalanceCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.BeachAccess,
                        iconBgColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                        iconColor = MaterialTheme.colorScheme.primary,
                        title = "Congés Payés",
                        days = "$cpRemaining",
                        totalAllowance = "sur ${cpAllowance}j alloués"
                    )
                    BalanceCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.Schedule,
                        iconBgColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                        iconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        title = "RTT",
                        days = "$rttRemaining",
                        totalAllowance = "sur ${rttAllowance}j alloués"
                    )
                }
            }

            // CALENDAR VISUALIZATION SECTION (UPCOMING APPROVED LEAVES)
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

            // Demandes récentes
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Demandes récentes",
                    style = MaterialTheme.typography.headlineSmall
                )
                val context = LocalContext.current
                val leaveRepository = remember { LeaveRepository.getInstance(context) }
                val userEmail = currentUser?.email
                val allDbRequests by if (userEmail != null) {
                    leaveRepository.getEmployeeRequestsFlow(userEmail).collectAsState(initial = emptyList())
                } else {
                    leaveRepository.getAllRequestsFlow().collectAsState(initial = emptyList())
                }
                
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
                            req.leaveType.contains("RTT", ignoreCase = true) -> Icons.Filled.Schedule
                            req.leaveType.contains("Télétravail", ignoreCase = true) -> Icons.Filled.HomeWork
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

            // Prochains jours fériés
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Prochains jours fériés",
                    style = MaterialTheme.typography.headlineSmall
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        HolidayCard(
                            isPrimary = true,
                            day = "MARDI",
                            name = "Toussaint",
                            date = "1 Novembre 2023"
                        )
                    }
                    item {
                        HolidayCard(
                            isPrimary = false,
                            day = "VENDREDI",
                            name = "Armistice",
                            date = "11 Novembre 2023"
                        )
                    }
                    item {
                        HolidayCard(
                            isPrimary = false,
                            day = "LUNDI",
                            name = "Noël",
                            date = "25 Décembre 2023"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
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
                    title = "Congés Payés",
                    usedDays = usedPaidLeaveDays,
                    totalAllowance = annualPaidLeaveAllowance,
                    progress = animatedCpProgress,
                    barColor = MaterialTheme.colorScheme.primary,
                    icon = Icons.Filled.BeachAccess
                )

                // RTT Bar
                LeaveCategoryProgressRow(
                    title = "RTT",
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
    val dbRequests by if (currentUserEmail != null) {
        leaveRepository.getEmployeeRequestsFlow(currentUserEmail).collectAsState(initial = emptyList())
    } else {
        leaveRepository.getAllRequestsFlow().collectAsState(initial = emptyList())
    }

    val nowCal = remember { Calendar.getInstance() }
    val currentRealMonth = remember { nowCal.get(Calendar.MONTH) }
    val currentRealYear = remember { nowCal.get(Calendar.YEAR) }
    val currentRealDay = remember { nowCal.get(Calendar.DAY_OF_MONTH) }

    var displayedMonth by remember { mutableIntStateOf(currentRealMonth) }
    var displayedYear by remember { mutableIntStateOf(currentRealYear) }
    var selectedDay by remember { mutableStateOf<Int?>(currentRealDay) }

    val approvedLeaves = remember(dbRequests, currentUserEmail) {
        val userApproved = dbRequests.filter { it.status.uppercase() == "APPROVED" }
        val mapped = userApproved.map { req ->
            val cat = when {
                req.leaveType.contains("Payé", ignoreCase = true) -> LeaveCategory.CONGES_PAYES
                req.leaveType.contains("RTT", ignoreCase = true) -> LeaveCategory.RTT
                req.leaveType.contains("Télétravail", ignoreCase = true) -> LeaveCategory.TELETTRAVAIL
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

        if (mapped.isNotEmpty()) {
            mapped
        } else {
            // Default enterprise calendar period if no records yet
            listOf(
                DashboardApprovedLeave(
                    id = "sample_1",
                    title = "Congés d'équipe",
                    type = "Congés Payés",
                    startDay = 24,
                    endDay = 26,
                    month = currentRealMonth,
                    year = currentRealYear,
                    durationDays = 3,
                    datesFormatted = "24 - 26 ${SimpleDateFormat("MMMM yyyy", Locale.FRENCH).format(nowCal.time)}",
                    category = LeaveCategory.CONGES_PAYES,
                    countdownText = "Validé RH",
                    details = "3 jours validés par la direction"
                )
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

    val currentMonthLeaves = remember(displayedMonth, displayedYear) {
        approvedLeaves.filter { it.month == displayedMonth && it.year == displayedYear }
    }

    val activeLeave = remember(selectedDay, displayedMonth, displayedYear) {
        if (selectedDay == null) null
        else currentMonthLeaves.find { selectedDay!! in it.startDay..it.endDay }
            ?: currentMonthLeaves.firstOrNull()
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
                    label = "Congés Payés"
                )
                LegendItem(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    dotColor = MaterialTheme.colorScheme.secondary,
                    label = "RTT"
                )
                LegendItem(
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                    dotColor = MaterialTheme.colorScheme.tertiary,
                    label = "Télétravail"
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
                text = "Tous mes prochains congés validés",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

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
    totalAllowance: String
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
fun HolidayCard(
    isPrimary: Boolean,
    day: String,
    name: String,
    date: String
) {
    val bgColor = if (isPrimary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerLowest
    val textColor = if (isPrimary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val subtextColor = if (isPrimary) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.outline
    val iconColor = if (isPrimary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary

    Card(
        modifier = Modifier.width(160.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isPrimary) 4.dp else 1.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = day,
                    style = MaterialTheme.typography.labelSmall,
                    color = subtextColor,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.Filled.Celebration,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(16.dp)
                )
            }

            Column {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Text(
                    text = date,
                    style = MaterialTheme.typography.bodySmall,
                    color = subtextColor
                )
            }
        }
    }
}
