package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.LeaveRepository
import com.example.data.LeaveRequestEntity
import com.example.data.User
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Metadata descriptor for each leave type option.
 */
data class LeaveTypeOption(
    val name: String,
    val description: String,
    val icon: ImageVector,
    val color: Color,
    val requiresJustification: Boolean = false,
    val deductsFromAllowance: Boolean = true
)

val LEAVE_TYPE_OPTIONS = listOf(
    LeaveTypeOption(
        name = "Congés payés",
        description = "Décompté du solde CP annuel",
        icon = Icons.Filled.DateRange,
        color = Color(0xFF2563EB),
        deductsFromAllowance = true
    ),
    LeaveTypeOption(
        name = "RTT",
        description = "Récupération du temps de travail",
        icon = Icons.Filled.AccessTime,
        color = Color(0xFF0D9488),
        deductsFromAllowance = true
    ),
    LeaveTypeOption(
        name = "Télétravail",
        description = "Travail à distance / concentration",
        icon = Icons.Filled.Home,
        color = Color(0xFF7C3AED),
        deductsFromAllowance = false
    ),
    LeaveTypeOption(
        name = "Maladie",
        description = "Arrêt maladie (justificatif requis)",
        icon = Icons.Filled.LocalHospital,
        color = Color(0xFFE11D48),
        requiresJustification = true,
        deductsFromAllowance = false
    ),
    LeaveTypeOption(
        name = "Formation",
        description = "Développement des compétences",
        icon = Icons.Filled.School,
        color = Color(0xFF059669),
        deductsFromAllowance = false
    ),
    LeaveTypeOption(
        name = "Congé sans solde",
        description = "Absence autorisée non rémunérée",
        icon = Icons.Filled.Work,
        color = Color(0xFFD97706),
        deductsFromAllowance = false
    ),
    LeaveTypeOption(
        name = "Événement familial",
        description = "Mariage, naissance, déménagement...",
        icon = Icons.Filled.CalendarMonth,
        color = Color(0xFF4F46E5),
        requiresJustification = true,
        deductsFromAllowance = false
    )
)

/**
 * Reusable and feature-complete Leave Request Form Component.
 * Features:
 * - Dropdown selection for Leave Type with custom badges and icons
 * - Date Picker Dialogs for Start and End dates with instant duration calculation
 * - Quick period presets ("Demain", "1 semaine", "2 semaines")
 * - Balance awareness & real-time validation warnings
 * - Optional attachment support
 * - Clean Room Database integration
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaveRequestForm(
    currentUser: User?,
    modifier: Modifier = Modifier,
    onSubmittedSuccess: ((LeaveRequestEntity) -> Unit)? = null,
    onUserUpdate: ((User) -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Form Fields State
    var selectedTypeOption by remember { mutableStateOf(LEAVE_TYPE_OPTIONS[0]) }
    var dropdownExpanded by remember { mutableStateOf(false) }

    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var motif by remember { mutableStateOf("") }
    var attachedFileName by remember { mutableStateOf<String?>(null) }

    // Dialogs & UI Feedback
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }
    var submitSuccessMessage by remember { mutableStateOf<String?>(null) }

    // Date computation & validation helpers
    val tomorrowUtcMillis = remember { getTomorrowUtcMillis() }
    val tomorrowDate = remember { getTomorrowDate() }

    val parsedStart by remember(startDate) { derivedStateOf { parseFlexibleDate(startDate) } }
    val parsedEnd by remember(endDate) { derivedStateOf { parseFlexibleDate(endDate) } }

    val calculatedWorkingDays by remember(parsedStart, parsedEnd) {
        derivedStateOf {
            if (parsedStart != null && parsedEnd != null && !parsedEnd!!.before(parsedStart)) {
                calculateBusinessDays(parsedStart!!, parsedEnd!!)
            } else {
                0
            }
        }
    }

    val isStartDateInPast by remember(parsedStart, tomorrowDate) {
        derivedStateOf { parsedStart != null && parsedStart!!.before(tomorrowDate) }
    }

    val isEndDateInPast by remember(parsedEnd, tomorrowDate) {
        derivedStateOf { parsedEnd != null && parsedEnd!!.before(tomorrowDate) }
    }

    val isEndBeforeStart by remember(parsedStart, parsedEnd) {
        derivedStateOf { parsedStart != null && parsedEnd != null && parsedEnd!!.before(parsedStart) }
    }

    // Balances
    val cpRemaining = currentUser?.let { it.paidLeaveAllowance - it.paidLeaveUsed } ?: 12
    val rttRemaining = currentUser?.let { it.rttAllowance - it.rttUsed } ?: 7

    val exceedsBalance by remember(selectedTypeOption, calculatedWorkingDays, cpRemaining, rttRemaining) {
        derivedStateOf {
            when (selectedTypeOption.name) {
                "Congés payés" -> calculatedWorkingDays > cpRemaining
                "RTT" -> calculatedWorkingDays > rttRemaining
                else -> false
            }
        }
    }

    // Attachment file picker
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = uri.lastPathSegment?.substringAfterLast("/") ?: "justificatif.pdf"
            attachedFileName = fileName
        }
    }

    // START DATE PICKER DIALOG
    if (showStartDatePicker) {
        val startDatePickerState = rememberDatePickerState(
            initialSelectedDateMillis = tomorrowUtcMillis,
            selectableDates = remember(tomorrowUtcMillis) {
                object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                        return utcTimeMillis >= tomorrowUtcMillis
                    }
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    startDatePickerState.selectedDateMillis?.let { millis ->
                        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply {
                            timeZone = TimeZone.getTimeZone("UTC")
                        }
                        startDate = sdf.format(Date(millis))
                        validationError = null
                        submitSuccessMessage = null
                        // If end date is empty or earlier, auto-set to start date
                        if (endDate.isBlank() || (parsedEnd != null && parsedEnd!!.before(Date(millis)))) {
                            endDate = startDate
                        }
                    }
                    showStartDatePicker = false
                }) {
                    Text("Confirmer", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) {
                    Text("Annuler")
                }
            }
        ) {
            DatePicker(state = startDatePickerState)
        }
    }

    // END DATE PICKER DIALOG
    if (showEndDatePicker) {
        val minEndUtcMillis = remember(parsedStart, tomorrowUtcMillis) {
            if (parsedStart != null) {
                val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                val localCal = Calendar.getInstance().apply { time = parsedStart!! }
                cal.set(
                    localCal.get(Calendar.YEAR),
                    localCal.get(Calendar.MONTH),
                    localCal.get(Calendar.DAY_OF_MONTH),
                    0, 0, 0
                )
                cal.set(Calendar.MILLISECOND, 0)
                maxOf(tomorrowUtcMillis, cal.timeInMillis)
            } else {
                tomorrowUtcMillis
            }
        }
        val endDatePickerState = rememberDatePickerState(
            initialSelectedDateMillis = minEndUtcMillis,
            selectableDates = remember(minEndUtcMillis) {
                object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                        return utcTimeMillis >= minEndUtcMillis
                    }
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    endDatePickerState.selectedDateMillis?.let { millis ->
                        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply {
                            timeZone = TimeZone.getTimeZone("UTC")
                        }
                        endDate = sdf.format(Date(millis))
                        validationError = null
                        submitSuccessMessage = null
                    }
                    showEndDatePicker = false
                }) {
                    Text("Confirmer", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) {
                    Text("Annuler")
                }
            }
        ) {
            DatePicker(state = endDatePickerState)
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("leave_request_form_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header / Category Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(selectedTypeOption.color.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = selectedTypeOption.icon,
                            contentDescription = null,
                            tint = selectedTypeOption.color,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Formulaire de demande",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = selectedTypeOption.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 1. DROPDOWN FOR LEAVE TYPE
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Type d'absence ou congé",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                ExposedDropdownMenuBox(
                    expanded = dropdownExpanded,
                    onExpandedChange = { dropdownExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedTypeOption.name,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded)
                        },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(selectedTypeOption.color)
                            )
                        },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                            .testTag("leave_type_dropdown"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    ExposedDropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainer)
                    ) {
                        LEAVE_TYPE_OPTIONS.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(option.color)
                                        )
                                        Column {
                                            Text(
                                                text = option.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (option.name == selectedTypeOption.name) FontWeight.Bold else FontWeight.Normal
                                            )
                                            Text(
                                                text = option.description,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                    }
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = option.icon,
                                        contentDescription = null,
                                        tint = option.color,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                trailingIcon = if (option.name == selectedTypeOption.name) {
                                    {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = "Sélectionné",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                } else null,
                                onClick = {
                                    selectedTypeOption = option
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Balance preview badge for CP / RTT
                if (selectedTypeOption.name == "Congés payés") {
                    Row(
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Solde disponible : $cpRemaining jours de congés payés",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else if (selectedTypeOption.name == "RTT") {
                    Row(
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Solde disponible : $rttRemaining jours de RTT",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // 2. QUICK DATE PRESET SHORTCUTS
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Raccourcis rapides",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

                    // Tomorrow 1 day
                    SuggestionChip(
                        onClick = {
                            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
                            val dateStr = sdf.format(cal.time)
                            startDate = dateStr
                            endDate = dateStr
                            validationError = null
                        },
                        label = { Text("Demain (1j)", style = MaterialTheme.typography.labelSmall) },
                        shape = RoundedCornerShape(8.dp)
                    )

                    // Next Week (5 days)
                    SuggestionChip(
                        onClick = {
                            val cal = Calendar.getInstance().apply {
                                add(Calendar.DAY_OF_YEAR, 1)
                                while (get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
                                    add(Calendar.DAY_OF_YEAR, 1)
                                }
                            }
                            startDate = sdf.format(cal.time)
                            cal.add(Calendar.DAY_OF_YEAR, 4)
                            endDate = sdf.format(cal.time)
                            validationError = null
                        },
                        label = { Text("Semaine pro. (5j)", style = MaterialTheme.typography.labelSmall) },
                        shape = RoundedCornerShape(8.dp)
                    )

                    // 2 Weeks
                    SuggestionChip(
                        onClick = {
                            val cal = Calendar.getInstance().apply {
                                add(Calendar.DAY_OF_YEAR, 1)
                                while (get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
                                    add(Calendar.DAY_OF_YEAR, 1)
                                }
                            }
                            startDate = sdf.format(cal.time)
                            cal.add(Calendar.DAY_OF_YEAR, 11) // 2 full working weeks (Mon-Fri following week)
                            endDate = sdf.format(cal.time)
                            validationError = null
                        },
                        label = { Text("2 semaines", style = MaterialTheme.typography.labelSmall) },
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }

            // 3. DATE PICKERS FOR START AND END DATES
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Start Date Field
                    Box(modifier = Modifier.weight(1f)) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Date de début",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isStartDateInPast) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedTextField(
                                value = startDate,
                                onValueChange = {
                                    startDate = it
                                    validationError = null
                                    submitSuccessMessage = null
                                },
                                placeholder = { Text("jj/mm/aaaa", style = MaterialTheme.typography.bodySmall) },
                                isError = isStartDateInPast,
                                trailingIcon = {
                                    IconButton(
                                        onClick = { showStartDatePicker = true },
                                        modifier = Modifier.testTag("start_date_picker_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.CalendarToday,
                                            contentDescription = "Sélectionner la date de début",
                                            tint = if (isStartDateInPast) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().testTag("start_date_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                    focusedBorderColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }

                    // End Date Field
                    Box(modifier = Modifier.weight(1f)) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Date de fin",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isEndDateInPast || isEndBeforeStart) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedTextField(
                                value = endDate,
                                onValueChange = {
                                    endDate = it
                                    validationError = null
                                    submitSuccessMessage = null
                                },
                                placeholder = { Text("jj/mm/aaaa", style = MaterialTheme.typography.bodySmall) },
                                isError = isEndDateInPast || isEndBeforeStart,
                                trailingIcon = {
                                    IconButton(
                                        onClick = { showEndDatePicker = true },
                                        modifier = Modifier.testTag("end_date_picker_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.CalendarToday,
                                            contentDescription = "Sélectionner la date de fin",
                                            tint = if (isEndDateInPast || isEndBeforeStart) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().testTag("end_date_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                    focusedBorderColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }

                // Inline Real-time Date Warnings
                if (isStartDateInPast) {
                    Row(
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "La date de début doit être ultérieure à aujourd'hui.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                } else if (isEndBeforeStart) {
                    Row(
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "La date de fin ne peut pas précéder la date de début.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                // Calculated Duration Pill
                if (calculatedWorkingDays > 0 && !isEndBeforeStart) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (exceedsBalance) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                                else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp)
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
                                Icon(
                                    imageVector = if (exceedsBalance) Icons.Filled.ErrorOutline else Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = if (exceedsBalance) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Durée : $calculatedWorkingDays jour(s) ouvré(s)",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (exceedsBalance) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }

                            if (exceedsBalance) {
                                Text(
                                    text = "Dépasse votre solde disponible",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }

            // 4. MOTIF / REASON TEXT FIELD
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Motif ou précision",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${motif.length}/300",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                OutlinedTextField(
                    value = motif,
                    onValueChange = { if (it.length <= 300) motif = it },
                    placeholder = {
                        Text(
                            text = "Ex: Congés d'été, raison familiale, projet personnel...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("leave_motif_input"),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 3,
                    maxLines = 5,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            // 5. ATTACHMENT SECTION (JUSTIFICATIF)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = if (selectedTypeOption.requiresJustification) "Pièce justificative (Recommandée pour ${selectedTypeOption.name})" else "Pièce jointe (Optionnel)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selectedTypeOption.requiresJustification) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (attachedFileName == null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { filePickerLauncher.launch("*/*") }
                            .testTag("add_attachment_button"),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AttachFile,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Ajouter un justificatif ou document",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "PDF, PNG ou JPG (max 5 Mo)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AttachFile,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = attachedFileName!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            IconButton(
                                onClick = { attachedFileName = null },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Supprimer la pièce jointe",
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 6. VALIDATION & FEEDBACK MESSAGES
            AnimatedVisibility(
                visible = validationError != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = validationError ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = submitSuccessMessage != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = submitSuccessMessage ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            if (isSubmitting) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            // 7. SUBMIT BUTTON
            Button(
                onClick = {
                    submitSuccessMessage = null

                    // 1. Validation checks
                    if (startDate.isBlank() || endDate.isBlank()) {
                        validationError = "Veuillez renseigner les dates de début et de fin."
                        return@Button
                    }

                    val start = parseFlexibleDate(startDate)
                    val end = parseFlexibleDate(endDate)

                    if (start == null || end == null) {
                        validationError = "Format de date invalide. Utilisez le sélecteur ou le format jj/mm/aaaa."
                        return@Button
                    }

                    if (start.before(tomorrowDate)) {
                        validationError = "La date de début doit être ultérieure à aujourd'hui."
                        return@Button
                    }

                    if (end.before(tomorrowDate)) {
                        validationError = "La date de fin doit être ultérieure à aujourd'hui."
                        return@Button
                    }

                    if (end.before(start)) {
                        validationError = "La date de fin ne peut pas être antérieure à la date de début."
                        return@Button
                    }

                    validationError = null
                    isSubmitting = true

                    val user = currentUser ?: User(
                        email = "khadija@gmail.com",
                        fullName = "khadija el ferrouni",
                        passwordHash = "MOHAMEDTAHA123",
                        jobTitle = "Développeur Senior",
                        department = "Ingénierie & IT"
                    )

                    coroutineScope.launch {
                        try {
                            val repo = LeaveRepository.getInstance(context)
                            val createdRequest = repo.submitLeaveRequest(
                                user = user,
                                leaveType = selectedTypeOption.name,
                                startDateStr = startDate,
                                endDateStr = endDate,
                                reason = motif,
                                attachmentName = attachedFileName
                            )

                            // Deduct the requested days from the user's balance
                            val isPaidLeave = selectedTypeOption.name.contains("Payé", ignoreCase = true)
                            val count = createdRequest.daysCount
                            val updatedUser = if (isPaidLeave) {
                                user.copy(paidLeaveUsed = user.paidLeaveUsed + count)
                            } else {
                                user.copy(rttUsed = user.rttUsed + count)
                            }
                            onUserUpdate?.invoke(updatedUser)

                            submitSuccessMessage = "Demande de ${selectedTypeOption.name} ($startDate au $endDate) soumise avec succès !"
                            isSubmitting = false

                            // Reset form fields
                            startDate = ""
                            endDate = ""
                            motif = ""
                            attachedFileName = null

                            onSubmittedSuccess?.invoke(createdRequest)
                        } catch (e: Exception) {
                            isSubmitting = false
                            validationError = "Erreur lors de l'enregistrement : ${e.localizedMessage}"
                        }
                    }
                },
                enabled = !isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("submit_leave_request_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (isSubmitting) "Transmission en cours..." else "Soumettre la demande",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// ----------------------------------------------------
// UTILITY FUNCTIONS FOR DATE PARSING & CALCULATIONS
// ----------------------------------------------------

private fun getTomorrowUtcMillis(): Long {
    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        add(Calendar.DAY_OF_YEAR, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
}

private fun getTomorrowDate(): Date {
    val cal = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return cal.time
}

private fun parseFlexibleDate(dateStr: String): Date? {
    val trimmed = dateStr.trim()
    if (trimmed.isEmpty()) return null
    val patterns = listOf(
        "dd/MM/yyyy",
        "MM/dd/yyyy",
        "yyyy-MM-dd",
        "dd-MM-yyyy",
        "d/M/yyyy",
        "d-M-yyyy",
        "dd.MM.yyyy",
        "yyyy/MM/dd"
    )
    for (pattern in patterns) {
        try {
            val sdf = SimpleDateFormat(pattern, Locale.getDefault()).apply {
                isLenient = false
            }
            val parsed = sdf.parse(trimmed)
            if (parsed != null) return parsed
        } catch (_: Exception) {
            // try next pattern
        }
    }
    return null
}

/**
 * Calculates business days (excluding Saturdays and Sundays) between start and end date inclusive.
 */
private fun calculateBusinessDays(startDate: Date, endDate: Date): Int {
    val startCal = Calendar.getInstance().apply { time = startDate }
    val endCal = Calendar.getInstance().apply { time = endDate }

    if (startCal.after(endCal)) return 0

    var businessDays = 0
    while (!startCal.after(endCal)) {
        val dayOfWeek = startCal.get(Calendar.DAY_OF_WEEK)
        if (dayOfWeek != Calendar.SATURDAY && dayOfWeek != Calendar.SUNDAY) {
            businessDays++
        }
        startCal.add(Calendar.DAY_OF_YEAR, 1)
    }
    return maxOf(1, businessDays)
}
