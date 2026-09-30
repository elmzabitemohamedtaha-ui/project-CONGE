package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Switch
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.testTag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow

import com.example.ui.i18n.I18nManager

import androidx.compose.material3.Text


import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.AppDatabase
import com.example.data.FirestoreSyncManager
import com.example.data.LeaveRepository
import com.example.data.LeaveRequestEntity
import com.example.data.LocalStorageManager
import com.example.data.User
import com.example.service.EmailNotification
import com.example.service.EmailNotificationService
import com.example.service.PdfReportGenerator
import com.example.ui.NotificationSystem
import com.example.ui.components.UserAvatar
import com.example.ui.theme.ThemeToggleIconButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class PendingRequest(
    val id: String,
    val employeeName: String,
    val employeeEmail: String,
    val leaveType: String,
    val dates: String,
    val department: String = "Ingénierie & IT",
    val startDay: Int = 12,
    val endDay: Int = 14,
    val month: Int = Calendar.OCTOBER,
    val year: Int = 2026,
    val isPending: Boolean = true,
    val attachmentName: String? = null,
    val attachmentUri: String? = null
)

data class TeamMemberSchedule(
    val email: String,
    val matricule: String = "EMP-0001",
    val name: String,
    val role: String,
    val department: String,
    val avatarUrl: String,
    val leaves: List<LeavePeriod>
)

data class LeavePeriod(
    val id: String,
    val type: String, // Congés payés, RTT, Télétravail, Maladie, Formation
    val startDay: Int,
    val endDay: Int,
    val month: Int,
    val year: Int,
    val isPending: Boolean = false,
    val status: String = if (isPending) "PENDING" else "APPROVED", // "APPROVED", "PENDING", "REJECTED"
    val color: Color = Color(0xFF2563EB),
    val startMillis: Long = 0L,
    val endMillis: Long = 0L
) {
    fun coversDay(day: Int, checkMonth: Int, checkYear: Int): Boolean {
        if (month == checkMonth && year == checkYear) {
            if (startDay <= endDay && day in startDay..endDay) return true
        }
        if (startMillis > 0L && endMillis >= startMillis) {
            val cal = Calendar.getInstance().apply {
                set(checkYear, checkMonth, day, 12, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val time = cal.timeInMillis
            return time in startMillis..endMillis
        }
        return false
    }
}

fun getLeaveTypeColor(type: String): Color {
    val clean = type.lowercase(Locale.getDefault())
    return when {
        clean.contains("payé") || clean.contains("conge") || clean.contains("congé") -> Color(0xFF2563EB) // Blue
        clean.contains("rtt") -> Color(0xFF0D9488) // Teal
        clean.contains("télé") || clean.contains("tele") -> Color(0xFF7C3AED) // Purple
        clean.contains("malad") -> Color(0xFFE11D48) // Rose/Red
        clean.contains("format") -> Color(0xFF059669) // Emerald
        else -> Color(0xFF6366F1)
    }
}

fun parseAdminDateToMillis(dateStr: String, isEndOfDay: Boolean = false): Long {
    val patterns = listOf("dd/MM/yyyy", "yyyy-MM-dd", "d/M/yyyy", "dd-MM-yyyy")
    for (p in patterns) {
        try {
            val sdf = SimpleDateFormat(p, Locale.getDefault()).apply { isLenient = false }
            val d = sdf.parse(dateStr.trim())
            if (d != null) {
                val cal = Calendar.getInstance().apply {
                    time = d
                    if (isEndOfDay) {
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                    } else {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                    }
                    set(Calendar.MILLISECOND, 0)
                }
                return cal.timeInMillis
            }
        } catch (_: Exception) {}
    }
    return 0L
}

fun com.example.data.LeaveRequestEntity.toLeavePeriod(): LeavePeriod {
    val sMillis = parseAdminDateToMillis(startDate, false)
    val eMillis = parseAdminDateToMillis(endDate, true)

    var sDay = startDay
    var eDay = endDay
    var mMonth = month
    var yYear = year

    if (sMillis > 0L) {
        val cal = Calendar.getInstance().apply { timeInMillis = sMillis }
        sDay = cal.get(Calendar.DAY_OF_MONTH)
        mMonth = cal.get(Calendar.MONTH)
        yYear = cal.get(Calendar.YEAR)
    }
    if (eMillis > 0L) {
        val cal = Calendar.getInstance().apply { timeInMillis = eMillis }
        eDay = cal.get(Calendar.DAY_OF_MONTH)
    }

    return LeavePeriod(
        id = id,
        type = leaveType,
        startDay = sDay,
        endDay = maxOf(sDay, eDay),
        month = mMonth,
        year = yYear,
        isPending = status.equals("PENDING", ignoreCase = true),
        status = status.uppercase(Locale.getDefault()),
        color = getLeaveTypeColor(leaveType),
        startMillis = sMillis,
        endMillis = maxOf(sMillis, eMillis)
    )
}

data class TeamOverlapConflict(
    val day: Int,
    val month: Int,
    val year: Int,
    val department: String,
    val affectedEmployees: List<String>,
    val leaveTypes: List<String>,
    val severity: OverlapSeverity
)

enum class OverlapSeverity {
    WARNING, CRITICAL
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(authViewModel: com.example.ui.AuthViewModel? = null, onLogout: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val leaveRepository = remember { LeaveRepository.getInstance(context) }

    var selectedTab by rememberSaveable { mutableIntStateOf(0) } // 0: Team Calendar View, 1: Pending Requests, 2: Email Logs

    val dbRequests by leaveRepository.getAllRequestsFlow().collectAsState(initial = emptyList())
    val currentUser by if (authViewModel != null) {
        authViewModel.currentUser.collectAsState()
    } else {
        remember { kotlinx.coroutines.flow.MutableStateFlow<com.example.data.User?>(null) }.collectAsState()
    }

    val pendingRequests = remember(dbRequests) {
        dbRequests.filter { it.status == "PENDING" }.map {
            PendingRequest(
                id = it.id,
                employeeName = it.employeeName,
                employeeEmail = it.employeeEmail,
                leaveType = "${it.leaveType} (${it.daysCount}j)",
                dates = "${it.startDate} - ${it.endDate}",
                department = it.department,
                startDay = it.startDay,
                endDay = it.endDay,
                month = it.month,
                year = it.year,
                isPending = true,
                attachmentName = it.attachmentName,
                attachmentUri = it.attachmentUri
            )
        }
    }

    var requestToDecide by remember { mutableStateOf<PendingRequest?>(null) }
    var isApproving by rememberSaveable { mutableStateOf(true) }
    var adminCommentInput by rememberSaveable { mutableStateOf("") }
    var showDecisionDialog by rememberSaveable { mutableStateOf(false) }

    // Dynamic Team Members list from DB and defaults
    var registeredUsers by remember { mutableStateOf<List<User>>(emptyList()) }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val db = AppDatabase.getDatabase(context)
                val users = db.userDao().getAllUsers()
                val localStorage = LocalStorageManager(context)
                val localUsers = localStorage.getAllRegisteredUsers()
                val combined = (users + localUsers).distinctBy { it.email }
                withContext(Dispatchers.Main) {
                    registeredUsers = combined
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val sentEmails by EmailNotificationService.sentEmails.collectAsState()
    var selectedEmailForDetail by remember { mutableStateOf<EmailNotification?>(null) }
    var showEmailLogs by remember { mutableStateOf(false) }
    var showCloudSyncDialog by remember { mutableStateOf(false) }
    var showSupabaseDialog by remember { mutableStateOf(false) }
    var showImportCsvDialog by remember { mutableStateOf(false) }

    val syncManager = remember { FirestoreSyncManager.getInstance(context) }
    val isCloudConnected by syncManager.isCloudConnected.collectAsState()
    val syncStatusText by syncManager.syncStatusText.collectAsState()

    val supabaseManager = remember { com.example.data.SupabaseSyncManager.getInstance(context) }
    val isSupabaseConnected by supabaseManager.isConnected.collectAsState()

    val currentLangCode by I18nManager.currentLang.collectAsState()
    val languages = listOf(
        "fr" to "Français",
        "en" to "English",
        "ar" to "العربية",
        "de" to "Deutsch"
    )

    val adminTabContent: @Composable () -> Unit = {
        when (selectedTab) {
            0 -> {
                TeamCalendarView(
                    registeredUsers = registeredUsers,
                    pendingRequests = pendingRequests,
                    allRequests = dbRequests,
                    currentUserEmail = currentUser?.email,
                    onApproveRequest = { request ->
                        requestToDecide = request
                        isApproving = true
                        adminCommentInput = "Demande approuvée par la direction."
                        showDecisionDialog = true
                    },
                    onRejectRequest = { request ->
                        requestToDecide = request
                        isApproving = false
                        adminCommentInput = "Effectif minimum non garanti sur cette période."
                        showDecisionDialog = true
                    }
                )
            }
            1 -> {
                PendingRequestsTabView(
                    pendingRequests = pendingRequests,
                    onAccept = { request ->
                        requestToDecide = request
                        isApproving = true
                        adminCommentInput = "Demande validée."
                        showDecisionDialog = true
                    },
                    onReject = { request ->
                        requestToDecide = request
                        isApproving = false
                        adminCommentInput = "Période non disponible / impératifs de service."
                        showDecisionDialog = true
                    }
                )
            }
            2 -> {
                MonthlyReportTab(
                    registeredUsers = registeredUsers
                )
            }
            3 -> {
                EmailLogsTabView(
                    sentEmails = sentEmails,
                    onSelectEmail = { selectedEmailForDetail = it },
                    onOpenClient = { email ->
                        EmailNotificationService.openEmailClient(context, email)
                    }
                )
            }
            4 -> {
                EmployeeDirectoryTab(
                    registeredUsers = registeredUsers,
                    onUpdateUser = { updatedUser ->
                        coroutineScope.launch {
                            try {
                                val db = com.example.data.AppDatabase.getDatabase(context)
                                db.userDao().insertUser(updatedUser) // upsert user
                                val localStorage = com.example.data.LocalStorageManager(context)
                                localStorage.saveUser(updatedUser, setAsActiveSession = false)
                                
                                // Update local state
                                registeredUsers = registeredUsers.map { 
                                    if (it.email == updatedUser.email) updatedUser else it 
                                }
                                
                                snackbarHostState.showSnackbar("Poste mis à jour avec succès.")
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Erreur lors de la mise à jour.")
                            }
                        }
                    }
                )
            }
            5 -> {
                com.example.ui.screens.ProfileScreen(authViewModel = authViewModel, onLogout = onLogout)
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        // Switch to NavigationRail on computers, tablets or in landscape mode on phones
        val isWideScreen = maxWidth >= 840.dp || (maxWidth > maxHeight && maxWidth >= 580.dp)

        if (isWideScreen) {
            // Adaptive Desktop / Computer layout with NavigationRail
            Row(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                NavigationRail(
                    modifier = Modifier.fillMaxHeight(),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    header = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(top = 16.dp, bottom = 12.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.Groups,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Admin RH",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                ) {
                    Column(
                        modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        NavigationRailItem(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            icon = { Icon(Icons.Filled.CalendarMonth, contentDescription = null) },
                            label = { Text("Planning", style = MaterialTheme.typography.labelSmall) }
                        )
                        NavigationRailItem(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            icon = {
                                BadgedBox(
                                    badge = {
                                        if (pendingRequests.isNotEmpty()) {
                                            Badge(
                                                containerColor = MaterialTheme.colorScheme.error,
                                                contentColor = MaterialTheme.colorScheme.onError
                                            ) {
                                                Text(pendingRequests.size.toString())
                                            }
                                        }
                                    }
                                ) {
                                    Icon(Icons.Filled.PendingActions, contentDescription = null)
                                }
                            },
                            label = { Text("Demandes", style = MaterialTheme.typography.labelSmall) }
                        )
                        NavigationRailItem(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            icon = { Icon(Icons.Filled.TableChart, contentDescription = null) },
                            label = { Text("Exports", style = MaterialTheme.typography.labelSmall) }
                        )
                        NavigationRailItem(
                            selected = selectedTab == 3,
                            onClick = { selectedTab = 3 },
                            icon = { Icon(Icons.Filled.Email, contentDescription = null) },
                            label = { Text("Emails", style = MaterialTheme.typography.labelSmall) }
                        )
                        NavigationRailItem(
                            selected = selectedTab == 4,
                            onClick = { selectedTab = 4 },
                            icon = { Icon(Icons.Filled.Groups, contentDescription = null) },
                            label = { Text("Équipe", style = MaterialTheme.typography.labelSmall) }
                        )
                        NavigationRailItem(
                            selected = selectedTab == 5,
                            onClick = { selectedTab = 5 },
                            icon = { Icon(Icons.Filled.Person, contentDescription = null) },
                            label = { Text("Profil", style = MaterialTheme.typography.labelSmall) }
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(
                            onClick = { showSupabaseDialog = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.TopEnd) {
                                Icon(
                                    imageVector = Icons.Filled.Storage,
                                    contentDescription = "Supabase Cloud Database & Sync",
                                    tint = if (isSupabaseConnected) Color(0xFF3ECF8E) else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                if (isSupabaseConnected) {
                                    Box(
                                        modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF3ECF8E))
                                    )
                                }
                            }
                        }
                        IconButton(
                            onClick = { showCloudSyncDialog = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.TopEnd) {
                                Icon(
                                    imageVector = if (isCloudConnected) Icons.Filled.CloudDone else Icons.Filled.CloudSync,
                                    contentDescription = "Cloud Multi-Phone Sync",
                                    tint = if (isCloudConnected) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                if (isCloudConnected) {
                                    Box(
                                        modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF10B981))
                                    )
                                }
                            }
                        }
                        IconButton(
                            onClick = { showImportCsvDialog = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.UploadFile,
                                contentDescription = "Importer CSV",
                                tint = Color(0xFF0284C7),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        ThemeToggleIconButton()
                        IconButton(
                            onClick = onLogout,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = "Déconnexion",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Content Area on Desktop
                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    topBar = {
                        TopAppBar(
                            title = {
                                val currentTitle = when (selectedTab) {
                                    0 -> com.example.ui.i18n.tr("admin_team_planning")
                                    1 -> "${com.example.ui.i18n.tr("admin_tab_requests")} (${pendingRequests.size})"
                                    2 -> "${com.example.ui.i18n.tr("admin_tab_stats")} (PDF / CSV / Excel)"
                                    3 -> com.example.ui.i18n.tr("admin_tab_logs")
                                    4 -> com.example.ui.i18n.tr("admin_tab_employees")
                                    else -> com.example.ui.i18n.tr("prof_title")
                                }
                                Text(
                                    text = currentTitle,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            actions = {
                                // Language selection chips on desktop top bar
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(end = 12.dp)
                                ) {
                                    languages.forEach { (code, name) ->
                                        FilterChip(
                                            selected = currentLangCode == code,
                                            onClick = { I18nManager.setLang(context, code) },
                                            label = { Text(name, style = MaterialTheme.typography.labelSmall) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        )
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                        adminTabContent()
                    }
                }
            }
        } else {
            // Mobile Layout with TopAppBar & ScrollableTabRow
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    TopAppBar(
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.Groups,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = com.example.ui.i18n.tr("admin_team_planning"),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = com.example.ui.i18n.tr("admin_supervision"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        actions = {
                            ThemeToggleIconButton()
                            IconButton(
                                onClick = { showSupabaseDialog = true }
                            ) {
                                Box(contentAlignment = Alignment.TopEnd) {
                                    Icon(
                                        imageVector = Icons.Filled.Storage,
                                        contentDescription = "Supabase Cloud Database & Sync",
                                        tint = if (isSupabaseConnected) Color(0xFF3ECF8E) else MaterialTheme.colorScheme.primary
                                    )
                                    if (isSupabaseConnected) {
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF3ECF8E))
                                        )
                                    }
                                }
                            }
                            IconButton(
                                onClick = { showCloudSyncDialog = true }
                            ) {
                                Box(contentAlignment = Alignment.TopEnd) {
                                    Icon(
                                        imageVector = if (isCloudConnected) Icons.Filled.CloudDone else Icons.Filled.CloudSync,
                                        contentDescription = "État de Synchronisation Cloud Multi-Téléphones",
                                        tint = if (isCloudConnected) Color(0xFF10B981) else MaterialTheme.colorScheme.primary
                                    )
                                    if (isCloudConnected) {
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF10B981))
                                        )
                                    }
                                }
                            }
                            IconButton(
                                onClick = { showImportCsvDialog = true },
                                modifier = Modifier.testTag("admin_topbar_import_csv_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.UploadFile,
                                    contentDescription = "Importer l'historique des congés (CSV)",
                                    tint = Color(0xFF0284C7)
                                )
                            }
                            IconButton(onClick = { selectedTab = 2 }) {
                                Icon(
                                    imageVector = Icons.Filled.TableChart,
                                    contentDescription = "Exports & Rapports (PDF/CSV)",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(onClick = onLogout) {
                                Icon(imageVector = Icons.AutoMirrored.Filled.Logout, contentDescription = "Déconnexion")
                            }
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
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    // Language Selection Row (Horizontally Scrollable)
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(languages) { (code, name) ->
                            FilterChip(
                                selected = currentLangCode == code,
                                onClick = { I18nManager.setLang(context, code) },
                                label = { Text(name, style = MaterialTheme.typography.labelMedium) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }

                    // Navigation Tabs
                    ScrollableTabRow(
                        selectedTabIndex = selectedTab,
                        edgePadding = 12.dp,
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
                                    Icon(
                                        imageVector = Icons.Filled.CalendarMonth,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(com.example.ui.i18n.tr("admin_tab_calendar"), fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal)
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
                                    BadgedBox(
                                        badge = {
                                            if (pendingRequests.isNotEmpty()) {
                                                Badge(
                                                    containerColor = MaterialTheme.colorScheme.error,
                                                    contentColor = MaterialTheme.colorScheme.onError
                                                ) {
                                                    Text(pendingRequests.size.toString())
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.PendingActions,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Text("${com.example.ui.i18n.tr("admin_tab_requests")} (${pendingRequests.size})", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.TableChart,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = if (selectedTab == 2) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text("${com.example.ui.i18n.tr("admin_tab_stats")} (PDF/CSV)", fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        )
                        Tab(
                            selected = selectedTab == 3,
                            onClick = { selectedTab = 3 },
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Email,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(com.example.ui.i18n.tr("admin_tab_logs"), fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        )
                        Tab(
                            selected = selectedTab == 4,
                            onClick = { selectedTab = 4 },
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Groups,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(com.example.ui.i18n.tr("admin_tab_employees"), fontWeight = if (selectedTab == 4) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        )
                        Tab(
                            selected = selectedTab == 5,
                            onClick = { selectedTab = 5 },
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(com.example.ui.i18n.tr("prof_title"), fontWeight = if (selectedTab == 5) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        )
                    }

                    // Tab Content
                    adminTabContent()
                }
            }
        }
    }

    // Decision Confirmation Dialog
    if (showDecisionDialog && requestToDecide != null) {
        val req = requestToDecide!!
        AlertDialog(
            onDismissRequest = { showDecisionDialog = false },
            icon = {
                Icon(
                    imageVector = if (isApproving) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                    contentDescription = null,
                    tint = if (isApproving) Color(0xFF10B981) else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = if (isApproving) "Valider la demande ?" else "Refuser la demande ?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Employé : ${req.employeeName} (${req.employeeEmail})\n" +
                               "Type : ${req.leaveType}\n" +
                               "Période : ${req.dates}",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Text(
                        text = "Message / Commentaire pour l'employé :",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    androidx.compose.material3.OutlinedTextField(
                        value = adminCommentInput,
                        onValueChange = { adminCommentInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        maxLines = 3,
                        label = { Text(com.example.ui.i18n.tr("admin_comment")) }
                    )

                    Text(
                        text = "Une alerte instantanée et un email officiel seront envoyés à l'employé.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val reqId = req.id
                        val comment = adminCommentInput
                        val empName = req.employeeName
                        val empEmail = req.employeeEmail
                        val isApp = isApproving
                        showDecisionDialog = false

                        coroutineScope.launch {
                            try {
                                if (isApp) {
                                    leaveRepository.approveRequest(reqId, comment)
                                    snackbarHostState.showSnackbar("Demande validée pour $empName. Alerte & email envoyés.")
                                } else {
                                    leaveRepository.rejectRequest(reqId, comment)
                                    // Refund the user balance
                                    val userToRefund = registeredUsers.find { it.email == empEmail }
                                    if (userToRefund != null) {
                                        val originalRequest = dbRequests.find { it.id == reqId }
                                        val count = originalRequest?.daysCount ?: 0
                                        val isPaidLeave = req.leaveType.contains("Payé", ignoreCase = true)
                                        val updatedUser = if (isPaidLeave) {
                                            userToRefund.copy(paidLeaveUsed = (userToRefund.paidLeaveUsed - count).coerceAtLeast(0))
                                        } else {
                                            userToRefund.copy(rttUsed = (userToRefund.rttUsed - count).coerceAtLeast(0))
                                        }
                                        val appDb = com.example.data.AppDatabase.getDatabase(context)
                                        appDb.userDao().updateUser(updatedUser)
                                        val localStorage = com.example.data.LocalStorageManager(context)
                                        localStorage.saveUser(updatedUser, setAsActiveSession = false)
                                        registeredUsers = registeredUsers.map { if (it.email == updatedUser.email) updatedUser else it }
                                    }
                                    snackbarHostState.showSnackbar("Demande refusée pour $empName. Alerte & email envoyés.")
                                }
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Erreur: ${e.localizedMessage}")
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isApproving) Color(0xFF10B981) else MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(if (isApproving) "Confirmer la Validation" else "Confirmer le Refus")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDecisionDialog = false }) {
                    Text(com.example.ui.i18n.tr("btn_cancel"))
                }
            }
        )
    }

    // Detail Dialog for Email
    selectedEmailForDetail?.let { email ->
        AlertDialog(
            onDismissRequest = { selectedEmailForDetail = null },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Email,
                    contentDescription = null,
                    tint = if (email.isApproved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(
                    text = email.subject,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Destinataire: ${email.toEmail}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = email.sentAt,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        text = email.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        EmailNotificationService.openEmailClient(context, email)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Ouvrir dans l'app Mail")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedEmailForDetail = null }) {
                    Text("Fermer")
                }
            }
        )
    }

    // Dialog for Multi-Device Cloud Synchronization Management
    if (showCloudSyncDialog) {
        CloudSyncManagementDialog(
            onDismiss = { showCloudSyncDialog = false },
            onOpenSupabase = {
                showCloudSyncDialog = false
                showSupabaseDialog = true
            }
        )
    }

    // Dialog for Supabase Cloud Database & PostgreSQL Management
    if (showSupabaseDialog) {
        com.example.ui.components.SupabaseManagementDialog(
            onDismiss = { showSupabaseDialog = false },
            onOpenImportCsv = {
                showSupabaseDialog = false
                showImportCsvDialog = true
            }
        )
    }

    if (showImportCsvDialog) {
        com.example.ui.components.LeaveBalanceHistoryImportDialog(
            callerEmail = "elmzabitemohamedtaha@gmail.com",
            onDismiss = { showImportCsvDialog = false }
        )
    }
}

/* =========================================================================================
   TEAM VIEW & CALENDAR VISUALIZATION COMPONENT (PREVENT OVERLAP)
   ========================================================================================= */

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TeamCalendarView(
    registeredUsers: List<User>,
    pendingRequests: List<PendingRequest>,
    allRequests: List<com.example.data.LeaveRequestEntity> = emptyList(),
    currentUserEmail: String? = null,
    onApproveRequest: (PendingRequest) -> Unit,
    onRejectRequest: (PendingRequest) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val currentCal = remember { Calendar.getInstance() }
    var currentMonth by remember { mutableIntStateOf(currentCal.get(Calendar.MONTH)) }
    var currentYear by remember { mutableIntStateOf(currentCal.get(Calendar.YEAR)) }
    var selectedDepartment by remember { mutableStateOf("Tous") }
    var selectedDayForDetail by remember { mutableStateOf<Int?>(currentCal.get(Calendar.DAY_OF_MONTH)) }
    var calendarDisplayMode by remember { mutableIntStateOf(1) } // 1: Grille Mensuelle, 0: Timeline Gantt
    var showOnlyApproved by remember { mutableStateOf(true) }
    var showOnlyMyLeaves by remember { mutableStateOf(false) }

    // Build complete team members and schedules with actual leave requests
    val teamMembers = remember(registeredUsers, allRequests, showOnlyApproved) {
        val membersList = mutableListOf<TeamMemberSchedule>()

        // 1. Process registered users and match their leaves
        for (u in registeredUsers) {
            val userLeaves = allRequests.filter { req ->
                (req.employeeEmail.trim().equals(u.email.trim(), ignoreCase = true) ||
                 req.employeeName.trim().equals(u.fullName.trim(), ignoreCase = true)) &&
                if (showOnlyApproved) req.status.equals("APPROVED", ignoreCase = true) else !req.status.equals("REJECTED", ignoreCase = true)
            }.map { it.toLeavePeriod() }

            membersList.add(
                TeamMemberSchedule(
                    email = u.email,
                    matricule = u.matricule,
                    name = u.fullName,
                    role = u.jobTitle,
                    department = u.department,
                    avatarUrl = u.avatarUrl,
                    leaves = userLeaves
                )
            )
        }

        // 2. Also incorporate any users who have requests in allRequests but aren't in registeredUsers
        for (req in allRequests) {
            if (membersList.none { it.email.equals(req.employeeEmail.trim(), ignoreCase = true) || it.name.equals(req.employeeName.trim(), ignoreCase = true) }) {
                val leaves = allRequests.filter {
                    (it.employeeEmail.trim().equals(req.employeeEmail.trim(), ignoreCase = true) ||
                     it.employeeName.trim().equals(req.employeeName.trim(), ignoreCase = true)) &&
                    if (showOnlyApproved) it.status.equals("APPROVED", ignoreCase = true) else !it.status.equals("REJECTED", ignoreCase = true)
                }.map { it.toLeavePeriod() }

                membersList.add(
                    TeamMemberSchedule(
                        email = req.employeeEmail,
                        matricule = "EMP-${1000 + membersList.size}",
                        name = req.employeeName,
                        role = "Collaborateur",
                        department = req.department,
                        avatarUrl = "",
                        leaves = leaves
                    )
                )
            }
        }

        membersList
    }

    // Filtered members by department and user isolation
    val filteredMembers = remember(teamMembers, selectedDepartment, showOnlyMyLeaves, currentUserEmail) {
        var list: List<TeamMemberSchedule> = teamMembers
        if (showOnlyMyLeaves && !currentUserEmail.isNullOrBlank()) {
            list = list.filter { it.email.trim().equals(currentUserEmail.trim(), ignoreCase = true) }
        } else if (selectedDepartment != "Tous") {
            list = list.filter { it.department.equals(selectedDepartment, ignoreCase = true) }
        }
        list
    }

    val departments = remember(teamMembers) {
        listOf("Tous") + teamMembers.map { it.department }.distinct()
    }

    // Days in current month
    val daysInMonth = remember(currentMonth, currentYear) {
        val cal = Calendar.getInstance()
        cal.set(currentYear, currentMonth, 1)
        cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    // Month Name
    val monthName = remember(currentMonth, currentYear) {
        val cal = Calendar.getInstance()
        cal.set(currentYear, currentMonth, 1)
        SimpleDateFormat("MMMM yyyy", Locale.FRENCH).format(cal.time).replaceFirstChar { it.uppercase() }
    }

    // OVERLAP DETECTION LOGIC
    // Detect days where 2 or more employees in the same department are on leave simultaneously
    val detectedOverlaps = remember(teamMembers, currentMonth, currentYear, daysInMonth) {
        val overlaps = mutableListOf<TeamOverlapConflict>()
        for (day in 1..daysInMonth) {
            val leavesOnDay = mutableListOf<Pair<TeamMemberSchedule, LeavePeriod>>()
            for (member in teamMembers) {
                for (leave in member.leaves) {
                    if (leave.coversDay(day, currentMonth, currentYear)) {
                        leavesOnDay.add(Pair(member, leave))
                    }
                }
            }
            // Group by department
            val byDept = leavesOnDay.groupBy { it.first.department }
            for ((dept, list) in byDept) {
                if (list.size >= 2) {
                    val severity = if (list.size >= 3) OverlapSeverity.CRITICAL else OverlapSeverity.WARNING
                    overlaps.add(
                        TeamOverlapConflict(
                            day = day,
                            month = currentMonth,
                            year = currentYear,
                            department = dept,
                            affectedEmployees = list.map { it.first.name },
                            leaveTypes = list.map { it.second.type },
                            severity = severity
                        )
                    )
                }
            }
        }
        overlaps
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
    ) {
        // OVERLAP ALERT BANNER (If overlaps exist in this view)
        item {
            if (detectedOverlaps.isNotEmpty()) {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.Warning,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onError,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Alerte Chevauchement d'équipe (${detectedOverlaps.size} jour(s) critique(s))",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = "Plusieurs collaborateurs du même département sont absents simultanément.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // List of overlapping conflict days
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            detectedOverlaps.take(3).forEach { conflict ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.padding(end = 4.dp)
                                            ) {
                                                Text(
                                                    text = "${conflict.day} Oct",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onError,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                            Text(
                                                text = "${conflict.department} : ${conflict.affectedEmployees.joinToString(", ")}",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Text(
                                            text = "Conflit",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.error,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Aucun chevauchement critique détecté sur la période sélectionnée.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        // MONTH NAVIGATOR & DISPLAY MODE SELECTOR
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Month selector
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    if (currentMonth == 0) {
                                        currentMonth = 11
                                        currentYear -= 1
                                    } else {
                                        currentMonth -= 1
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Mois précédent")
                            }

                            Text(
                                text = monthName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            IconButton(
                                onClick = {
                                    if (currentMonth == 11) {
                                        currentMonth = 0
                                        currentYear += 1
                                    } else {
                                        currentMonth += 1
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Mois suivant")
                            }
                        }

                        // Toggle to show only approved
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Text(
                                text = "Uniquement validés",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Switch(
                                checked = showOnlyApproved,
                                onCheckedChange = { showOnlyApproved = it },
                                modifier = Modifier.scale(0.7f)
                            )
                        }

                        // Toggle Mode: Timeline Gantt vs Grid
                        Row(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(8.dp))
                                .padding(2.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (calendarDisplayMode == 0) MaterialTheme.colorScheme.primary else Color.Transparent,
                                modifier = Modifier.clickable { calendarDisplayMode = 0 }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.ViewAgenda,
                                        contentDescription = null,
                                        tint = if (calendarDisplayMode == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = com.example.ui.i18n.tr("admin_timeline"),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (calendarDisplayMode == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (calendarDisplayMode == 1) MaterialTheme.colorScheme.primary else Color.Transparent,
                                modifier = Modifier.clickable { calendarDisplayMode = 1 }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CalendarMonth,
                                        contentDescription = null,
                                        tint = if (calendarDisplayMode == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = com.example.ui.i18n.tr("admin_grid"),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (calendarDisplayMode == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Department Filter Chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (!currentUserEmail.isNullOrBlank()) {
                            item {
                                FilterChip(
                                    selected = showOnlyMyLeaves,
                                    onClick = { showOnlyMyLeaves = !showOnlyMyLeaves },
                                    label = { Text("Mon planning uniquement", style = MaterialTheme.typography.labelSmall) },
                                    leadingIcon = if (showOnlyMyLeaves) {
                                        {
                                            Icon(
                                                imageVector = Icons.Filled.Person,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    } else null,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                )
                            }
                        }
                        items(departments) { dept ->
                            FilterChip(
                                selected = selectedDepartment == dept,
                                onClick = { selectedDepartment = dept },
                                label = { Text(dept, style = MaterialTheme.typography.labelSmall) },
                                leadingIcon = if (selectedDepartment == dept) {
                                    {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Quick Export PDF button for this month
                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch {
                                val db = AppDatabase.getDatabase(context)
                                val monthRequests = db.leaveRequestDao().getApprovedRequestsByMonth(currentMonth, currentYear)
                                val file = withContext(Dispatchers.IO) {
                                    PdfReportGenerator.generateMonthlyLeaveReport(
                                        context = context,
                                        month = currentMonth,
                                        year = currentYear,
                                        departmentFilter = if (selectedDepartment == "Tous") null else selectedDepartment,
                                        approvedRequests = monthRequests,
                                        managerName = "Responsable RH"
                                    )
                                }
                                Toast.makeText(context, "Rapport PDF de $monthName généré !", Toast.LENGTH_SHORT).show()
                                PdfReportGenerator.openPdf(context, file)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PictureAsPdf,
                            contentDescription = null,
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Exporter le rapport PDF de $monthName",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // VISUALIZATION VIEW (Timeline or Grid)
        item {
            if (calendarDisplayMode == 0) {
                // TIMELINE GANTT VIEW
                TeamTimelineGanttView(
                    members = filteredMembers,
                    daysInMonth = daysInMonth,
                    currentMonth = currentMonth,
                    currentYear = currentYear,
                    detectedOverlaps = detectedOverlaps,
                    onSelectDay = { selectedDayForDetail = it }
                )
            } else {
                // MONTH GRID VIEW
                TeamMonthGridView(
                    members = filteredMembers,
                    daysInMonth = daysInMonth,
                    currentMonth = currentMonth,
                    currentYear = currentYear,
                    detectedOverlaps = detectedOverlaps,
                    selectedDay = selectedDayForDetail,
                    onSelectDay = { selectedDayForDetail = it }
                )
            }
        }

        // SELECTED DAY DETAIL SHEET / INSPECTOR
        selectedDayForDetail?.let { day ->
            item {
                DayDetailInspectorCard(
                    day = day,
                    month = currentMonth,
                    year = currentYear,
                    members = teamMembers,
                    pendingRequests = pendingRequests,
                    onClose = { selectedDayForDetail = null },
                    onApprove = onApproveRequest,
                    onReject = onRejectRequest
                )
            }
        }

        // LEAVE TYPE COLOR LEGEND
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Légende des types d'absences :",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        LegendBadge("Congés Payés", Color(0xFF2563EB))
                        LegendBadge("RTT", Color(0xFF0D9488))
                        LegendBadge("Télétravail", Color(0xFF7C3AED))
                        LegendBadge("Maladie", Color(0xFFE11D48))
                        LegendBadge("Formation", Color(0xFF059669))
                    }
                }
            }
        }
    }
}

@Composable
fun LegendBadge(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/* =========================================================================================
   TIMELINE / GANTT VIEW FOR SIMULTANEOUS TEAM SCHEDULE
   ========================================================================================= */

@Composable
fun TeamTimelineGanttView(
    members: List<TeamMemberSchedule>,
    daysInMonth: Int,
    currentMonth: Int,
    currentYear: Int,
    detectedOverlaps: List<TeamOverlapConflict>,
    onSelectDay: (Int) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            val horizontalScrollState = rememberScrollState()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = com.example.ui.i18n.tr("gantt_title", members.size),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            horizontalScrollState.animateScrollTo(horizontalScrollState.value + 500)
                        }
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = com.example.ui.i18n.tr("scroll_horizontally"),
                        style = MaterialTheme.typography.labelSmall
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(horizontalScrollState)
            ) {
                Column {
                    // Header row with day numbers (1..daysInMonth)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Fixed Employee Header Column width
                        Box(
                            modifier = Modifier
                                .width(130.dp)
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = "Employé",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }

                        // Day headers
                        for (d in 1..daysInMonth) {
                            val isConflict = detectedOverlaps.any { it.day == d }
                            Box(
                                modifier = Modifier
                                    .width(36.dp)
                                    .height(34.dp)
                                    .clickable { onSelectDay(d) }
                                    .background(
                                        if (isConflict) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                                        else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.4f),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .border(
                                        width = if (isConflict) 1.dp else 0.5.dp,
                                        color = if (isConflict) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(4.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "$d",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isConflict) FontWeight.ExtraBold else FontWeight.Medium,
                                        color = if (isConflict) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (isConflict) {
                                        Box(
                                            modifier = Modifier
                                                .size(4.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.error)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(2.dp))
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

                    // Member rows
                    members.forEach { member ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Member info column
                            Row(
                                modifier = Modifier
                                    .width(130.dp)
                                    .padding(end = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                UserAvatar(
                                    avatarUrl = member.avatarUrl,
                                    fullName = member.name,
                                    size = 26.dp
                                )
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = member.name,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(
                                        text = "${member.matricule} • ${member.department}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.outline,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // Day slots for this member
                            for (d in 1..daysInMonth) {
                                val leave = member.leaves.find {
                                    it.coversDay(d, currentMonth, currentYear)
                                }
                                val isConflictDay = detectedOverlaps.any { it.day == d && it.affectedEmployees.contains(member.name) }

                                Box(
                                    modifier = Modifier
                                        .width(36.dp)
                                        .height(32.dp)
                                        .clickable { onSelectDay(d) }
                                        .background(
                                            when {
                                                leave != null -> leave.color.copy(alpha = if (leave.isPending) 0.55f else 0.95f)
                                                else -> Color.Transparent
                                            },
                                            RoundedCornerShape(4.dp)
                                        )
                                        .border(
                                            width = if (isConflictDay) 1.5.dp else if (leave != null) 1.dp else 0.5.dp,
                                            color = if (isConflictDay) MaterialTheme.colorScheme.error else if (leave != null) leave.color else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                            shape = RoundedCornerShape(4.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (leave != null) {
                                        Text(
                                            text = when {
                                                leave.type.contains("payé", ignoreCase = true) || leave.type.contains("conge", ignoreCase = true) -> "CP"
                                                leave.type.contains("rtt", ignoreCase = true) -> "RTT"
                                                leave.type.contains("télé", ignoreCase = true) || leave.type.contains("tele", ignoreCase = true) -> "TT"
                                                leave.type.contains("malad", ignoreCase = true) -> "MAL"
                                                leave.type.contains("format", ignoreCase = true) -> "FOR"
                                                else -> leave.type.take(2).uppercase()
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 9.sp,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(2.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

/* =========================================================================================
   MONTH GRID VIEW FOR TEAM VISUALIZATION
   ========================================================================================= */

@Composable
fun TeamMonthGridView(
    members: List<TeamMemberSchedule>,
    daysInMonth: Int,
    currentMonth: Int,
    currentYear: Int,
    detectedOverlaps: List<TeamOverlapConflict>,
    selectedDay: Int?,
    onSelectDay: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "Grille Mensuelle des Congés",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Cliquez sur une date pour afficher les collaborateurs absents et les alertes.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Day of week headers (Lun, Mar, Mer, Jeu, Ven, Sam, Dim)
            val weekDays = listOf("Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                weekDays.forEach { wd ->
                    Text(
                        text = wd,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Calculate starting day offset
            val cal = Calendar.getInstance().apply {
                set(currentYear, currentMonth, 1)
            }
            val startDayOfWeek = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7 // Monday = 0

            val totalSlots = ((startDayOfWeek + daysInMonth + 6) / 7) * 7

            for (week in 0 until (totalSlots / 7)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    for (dayOfWeek in 0..6) {
                        val slotIndex = week * 7 + dayOfWeek
                        val dayNumber = slotIndex - startDayOfWeek + 1

                        if (dayNumber in 1..daysInMonth) {
                            // Find all leaves on this day
                            val absentMembers = members.filter { m ->
                                m.leaves.any { l ->
                                    l.coversDay(dayNumber, currentMonth, currentYear)
                                }
                            }
                            val conflict = detectedOverlaps.find { it.day == dayNumber }
                            val isSelected = selectedDay == dayNumber

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = when {
                                    isSelected -> MaterialTheme.colorScheme.primaryContainer
                                    conflict != null -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                                    absentMembers.isNotEmpty() -> MaterialTheme.colorScheme.surfaceContainerHigh
                                    else -> MaterialTheme.colorScheme.surfaceContainerLowest
                                },
                                border = if (conflict != null) {
                                    androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.error)
                                } else if (isSelected) {
                                    androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                                } else {
                                    androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(58.dp)
                                    .padding(1.dp)
                                    .clickable { onSelectDay(dayNumber) }
                            ) {
                                Column(
                                    modifier = Modifier.padding(3.dp),
                                    verticalArrangement = Arrangement.SpaceBetween,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "$dayNumber",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (absentMembers.isNotEmpty()) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 11.sp
                                        )
                                        if (conflict != null) {
                                            Icon(
                                                imageVector = Icons.Filled.Warning,
                                                contentDescription = "Chevauchement",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(10.dp)
                                            )
                                        }
                                    }

                                    // Small indicators for absent members
                                    if (absentMembers.isNotEmpty()) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(1.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            absentMembers.take(2).forEach { m ->
                                                val leave = m.leaves.firstOrNull { it.coversDay(dayNumber, currentMonth, currentYear) }
                                                if (leave != null) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(8.dp)
                                                            .clip(CircleShape)
                                                            .background(leave.color)
                                                    )
                                                }
                                            }
                                            if (absentMembers.size > 2) {
                                                Text(
                                                    text = "+${absentMembers.size - 2}",
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                        Text(
                                            text = "${absentMembers.size} abs.",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 8.sp,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.height(2.dp))
                                    }
                                }
                            }
                        } else {
                            // Empty slot
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(58.dp)
                                    .padding(1.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/* =========================================================================================
   DAY DETAIL INSPECTOR CARD
   ========================================================================================= */

@Composable
fun DayDetailInspectorCard(
    day: Int,
    month: Int,
    year: Int,
    members: List<TeamMemberSchedule>,
    pendingRequests: List<PendingRequest>,
    onClose: () -> Unit,
    onApprove: (PendingRequest) -> Unit,
    onReject: (PendingRequest) -> Unit
) {
    val cal = Calendar.getInstance().apply { set(year, month, day) }
    val fullDateStr = SimpleDateFormat("EEEE d MMMM yyyy", Locale.FRENCH).format(cal.time).replaceFirstChar { it.uppercase() }

    // Find all absences for this day
    val absences = members.flatMap { m ->
        m.leaves.filter { l -> l.coversDay(day, month, year) }.map { Pair(m, it) }
    }

    // Check if overlap exists on this day
    val groupedByDept = absences.groupBy { it.first.department }
    val overlapDepartments = groupedByDept.filter { it.value.size >= 2 }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                        imageVector = Icons.Filled.CalendarToday,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = fullDateStr,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                    Icon(imageVector = Icons.Filled.Close, contentDescription = "Fermer")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // OVERLAP WARNING BADGE FOR THIS SPECIFIC DAY
            if (overlapDepartments.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "Risque de sous-effectif détecté",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            overlapDepartments.forEach { (dept, list) ->
                                Text(
                                    text = "• $dept : ${list.size} collaborateurs absents (${list.joinToString(", ") { it.first.name }})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.9f)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // LIST OF ABSENCES
            if (absences.isEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Effectif complet ! Aucun collaborateur absent ce jour.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Text(
                    text = "Collaborateurs en congé / télétravail (${absences.size}) :",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    absences.forEach { (member, leave) ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    UserAvatar(
                                        avatarUrl = member.avatarUrl,
                                        fullName = member.name,
                                        size = 36.dp
                                    )
                                    Column {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = member.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                            ) {
                                                Text(
                                                    text = member.matricule,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = "${member.role} • ${member.department}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // Leave Type Badge
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = leave.color.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = leave.type,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = leave.color,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }

                                    // Color-Coded Status Badge (Green for Approved, Yellow/Amber for Pending, Red for Rejected)
                                    val (statusBgColor, statusTextColor, statusLabel) = when (leave.status.uppercase()) {
                                        "APPROVED", "APPROUVÉ", "VALIDE" -> Triple(
                                            Color(0xFFDCFCE7),
                                            Color(0xFF15803D),
                                            "Approuvé"
                                        )
                                        "REJECTED", "REFUSÉ", "REJETÉ" -> Triple(
                                            Color(0xFFFEE2E2),
                                            Color(0xFFB91C1C),
                                            "Refusé"
                                        )
                                        else -> Triple(
                                            Color(0xFFFEF3C7),
                                            Color(0xFFB45309),
                                            "En attente"
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = statusBgColor
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(statusTextColor)
                                            )
                                            Text(
                                                text = statusLabel,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = statusTextColor,
                                                fontWeight = FontWeight.Bold
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
    }
}

/* =========================================================================================
   PENDING REQUESTS TAB VIEW
   ========================================================================================= */

@Composable
fun PendingRequestsTabView(
    pendingRequests: List<PendingRequest>,
    onAccept: (PendingRequest) -> Unit,
    onReject: (PendingRequest) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredRequests = remember(pendingRequests, searchQuery) {
        if (searchQuery.isBlank()) pendingRequests
        else pendingRequests.filter { 
            it.employeeName.contains(searchQuery, ignoreCase = true) ||
            it.dates.contains(searchQuery, ignoreCase = true)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.MarkEmailRead,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Column {
                        Text(
                            text = "Validation & Notifications Automatiques",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "La validation ou le refus envoie instantanément un email et met à jour le planning d'équipe.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Demandes en attente (${filteredRequests.size})",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                androidx.compose.material3.OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(com.example.ui.i18n.tr("admin_search")) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Rechercher",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Filled.Clear,
                                    contentDescription = "Effacer la recherche",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        if (filteredRequests.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = if (pendingRequests.isEmpty()) "Toutes les demandes ont été traitées !" else "Aucune demande ne correspond à la recherche.",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(filteredRequests, key = { it.id }) { request ->
                AdminRequestCard(
                    request = request,
                    onAccept = { onAccept(request) },
                    onReject = { onReject(request) }
                )
            }
        }
    }
}

/* =========================================================================================
   EMAIL LOGS TAB VIEW
   ========================================================================================= */

@Composable
fun EmailLogsTabView(
    sentEmails: List<EmailNotification>,
    onSelectEmail: (EmailNotification) -> Unit,
    onOpenClient: (EmailNotification) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Historique des Notifications Emails (${sentEmails.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (sentEmails.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Email,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "Aucun email envoyé pour le moment.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(sentEmails, key = { it.id }) { email ->
                SentEmailCard(
                    email = email,
                    onClick = { onSelectEmail(email) },
                    onOpenClient = { onOpenClient(email) }
                )
            }
        }
    }
}

@Composable
fun AdminRequestCard(
    request: PendingRequest,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = request.employeeName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Email,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = request.employeeEmail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Text(
                        text = "Département : ${request.department}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = "En attente",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = request.leaveType,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Période : ${request.dates}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (request.attachmentName != null) {
                Spacer(modifier = Modifier.height(8.dp))
                var showAttachmentDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
                androidx.compose.material3.TextButton(onClick = { showAttachmentDialog = true }) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Filled.AttachFile,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Voir pièce jointe: ${request.attachmentName}")
                }
                
                if (showAttachmentDialog && request.attachmentUri != null) {
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { showAttachmentDialog = false },
                        title = { Text(request.attachmentName) },
                        text = {
                            coil.compose.AsyncImage(
                                model = java.io.File(request.attachmentUri),
                                contentDescription = null,
                                modifier = Modifier.fillMaxWidth().height(300.dp)
                            )
                        },
                        confirmButton = {
                            androidx.compose.material3.TextButton(onClick = { showAttachmentDialog = false }) {
                                Text("Fermer")
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = onReject,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = com.example.ui.i18n.tr("btn_reject"),
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text(com.example.ui.i18n.tr("btn_reject"))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    onClick = onAccept,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = com.example.ui.i18n.tr("btn_accept"),
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text(com.example.ui.i18n.tr("btn_accept"))
                }
            }
        }
    }
}

@Composable
fun SentEmailCard(
    email: EmailNotification,
    onClick: () -> Unit,
    onOpenClient: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
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
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            if (email.isApproved) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.errorContainer
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (email.isApproved) Icons.Filled.Check else Icons.Filled.Close,
                        contentDescription = null,
                        tint = if (email.isApproved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column {
                    Text(
                        text = "À : ${email.recipientName} (${email.toEmail})",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = email.subject,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                    Text(
                        text = "Envoyé le ${email.sentAt}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            IconButton(onClick = onOpenClient) {
                Icon(
                    imageVector = Icons.Filled.OpenInNew,
                    contentDescription = "Ouvrir dans l'application email",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/* =========================================================================================
   MULTI-DEVICE CLOUD SYNCHRONIZATION MANAGEMENT DIALOG
   ========================================================================================= */

@Composable
fun CloudSyncManagementDialog(
    onDismiss: () -> Unit,
    onOpenSupabase: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val syncManager = remember { FirestoreSyncManager.getInstance(context) }
    val supabaseManager = remember { com.example.data.SupabaseSyncManager.getInstance(context) }
    val isConnected by syncManager.isCloudConnected.collectAsState()
    val isSupabaseConnected by supabaseManager.isConnected.collectAsState()
    val syncStatusText by syncManager.syncStatusText.collectAsState()
    val lastSyncTime by syncManager.lastSyncTimestamp.collectAsState()

    var customChannelInput by remember { mutableStateOf(syncManager.companySyncChannel) }
    var isEditingChannel by remember { mutableStateOf(false) }
    var actionFeedback by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = if (isConnected) Icons.Filled.CloudDone else Icons.Filled.CloudSync,
                contentDescription = null,
                tint = if (isConnected) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(
                text = "Synchronisation Multi-Téléphones",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Live Status Badge
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isConnected) Color(0xFFDCFCE7) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (isConnected) Color(0xFF15803D) else MaterialTheme.colorScheme.error)
                        )
                        Column {
                            Text(
                                text = if (isConnected) "Liaison temps réel active" else "Connexion en cours...",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isConnected) Color(0xFF15803D) else MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = syncStatusText,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Explanation
                Text(
                    text = "Toute demande soumise par un collaborateur depuis son téléphone arrive automatiquement ici en direct sans configuration supplémentaire.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Sync Channel ID section
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Canal de synchronisation d'entreprise :",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (!isEditingChannel) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = syncManager.companySyncChannel,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                OutlinedButton(
                                    onClick = { isEditingChannel = true },
                                    modifier = Modifier.height(32.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                ) {
                                    Text("Modifier", fontSize = 12.sp)
                                }
                            }
                        } else {
                            androidx.compose.material3.OutlinedTextField(
                                value = customChannelInput,
                                onValueChange = { customChannelInput = it },
                                singleLine = true,
                                label = { Text("Code Canal (ex: societe_rh_2026)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(onClick = { isEditingChannel = false }) {
                                    Text(com.example.ui.i18n.tr("btn_cancel"))
                                }
                                Button(
                                    onClick = {
                                        syncManager.companySyncChannel = customChannelInput
                                        isEditingChannel = false
                                        actionFeedback = "Canal mis à jour. Reconnexion..."
                                    }
                                ) {
                                    Text("Enregistrer")
                                }
                            }
                        }
                    }
                }

                // Action Buttons
                Button(
                    onClick = {
                        coroutineScope.launch {
                            actionFeedback = "Synchronisation forcée en cours..."
                            syncManager.syncAllLocalDataToCloud()
                            actionFeedback = "Synchronisation terminée ! Toutes les demandes sont à jour."
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Forcer la synchronisation complète")
                }

                OutlinedButton(
                    onClick = {
                        coroutineScope.launch {
                            actionFeedback = "Signal test envoyé aux autres téléphones..."
                            syncManager.sendTestPing()
                            actionFeedback = "Signal test émis avec succès !"
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Filled.PhoneAndroid, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Tester la liaison entre téléphones")
                }

                if (onOpenSupabase != null) {
                    Button(
                        onClick = onOpenSupabase,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3ECF8E)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.Storage, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isSupabaseConnected) "Gérer la liaison Supabase (Connecté)" else "Configurer la liaison Supabase Cloud",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                actionFeedback?.let { msg ->
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Fermer")
            }
        }
    )
}


