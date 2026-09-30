package com.example.ui.screens

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Surface

import androidx.compose.material3.TextButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import com.example.ui.i18n.I18nManager
import androidx.compose.runtime.collectAsState

import androidx.compose.material3.Text


import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.ui.AuthViewModel
import com.example.ui.components.NotificationTopBarAction
import com.example.ui.components.UserAvatar
import com.example.ui.theme.ThemeModeSelectorCard
import com.example.ui.theme.ThemeToggleIconButton
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel? = null,
    onLogout: () -> Unit = {}
) {
    var showSecurityScreen by remember { mutableStateOf(false) }

    if (showSecurityScreen) {
        SecurityScreen(
            authViewModel = authViewModel,
            onBack = { showSecurityScreen = false }
        )
        return
    }

    val context = LocalContext.current
    val currentUser by authViewModel?.currentUser?.collectAsState() ?: remember { mutableStateOf(null) }

    val fullName = currentUser?.fullName ?: "Collaborateur"
    val matricule = currentUser?.matricule ?: "EMP-0001"
    val jobTitle = currentUser?.jobTitle ?: "Collaborateur"
    val department = currentUser?.department ?: "Direction & Administration"
    val email = currentUser?.email ?: ""
    val phone = currentUser?.phone ?: ""
    val hireDate = currentUser?.hireDate ?: "01 Janvier 2024"
    val officeLocation = currentUser?.officeLocation ?: "Bureau Central"
    val avatarUrl = currentUser?.avatarUrl ?: ""

    var showEditProfileDialog by rememberSaveable { mutableStateOf(false) }
    var showPhotoOptionsDialog by rememberSaveable { mutableStateOf(false) }
    var showUrlInputDialog by rememberSaveable { mutableStateOf(false) }
    var inputCustomUrl by rememberSaveable { mutableStateOf("") }

    // Dialog state (Preserved across rotation)
    var editFullName by rememberSaveable { mutableStateOf(fullName) }
    var editJobTitle by rememberSaveable { mutableStateOf(jobTitle) }
    var editDepartment by rememberSaveable { mutableStateOf(department) }
    var editPhone by rememberSaveable { mutableStateOf(phone) }
    var editOffice by rememberSaveable { mutableStateOf(officeLocation) }
    var editEmail by rememberSaveable { mutableStateOf(email) }
    var showCloudSyncDialog by rememberSaveable { mutableStateOf(false) }
    var showSupabaseDialog by rememberSaveable { mutableStateOf(false) }
    var showLanguageDialog by rememberSaveable { mutableStateOf(false) }

    val syncManager = remember { com.example.data.FirestoreSyncManager.getInstance(context) }
    val isCloudConnected by syncManager.isCloudConnected.collectAsState()

    val supabaseManager = remember { com.example.data.SupabaseSyncManager.getInstance(context) }
    val isSupabaseConnected by supabaseManager.isConnected.collectAsState()

    // Image Picker Launcher from Gallery
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null && currentUser != null) {
            val savedPath = saveImageToInternalStorage(context, uri, currentUser!!.email)
            if (savedPath != null) {
                authViewModel?.updateAvatar(savedPath)
                Toast.makeText(context, "Photo de profil mise à jour avec succès !", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Impossible de charger l'image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        UserAvatar(
                            avatarUrl = avatarUrl,
                            fullName = fullName,
                            size = 40.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = com.example.ui.i18n.tr("prof_title"),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    NotificationTopBarAction(userEmail = currentUser?.email)
                    ThemeToggleIconButton()
                    IconButton(onClick = { showEditProfileDialog = true }) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Modifier",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
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
            // Orientation & width check: 2 columns in landscape mode or on wide screens
            val isWideScreen = maxWidth >= 840.dp || (maxWidth > maxHeight && maxWidth >= 580.dp)

            val profileInfoSection = @Composable {
                Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    // Profile Header
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier.padding(top = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    UserAvatar(
                        avatarUrl = avatarUrl,
                        fullName = fullName,
                        size = 112.dp,
                        border = BorderStroke(3.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                        onClick = { showPhotoOptionsDialog = true }
                    )

                    // Floating camera badge on bottom end
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                            .clickable { showPhotoOptionsDialog = true }
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = if (avatarUrl.isBlank()) Icons.Filled.AddAPhoto else Icons.Filled.CameraAlt,
                            contentDescription = "Ajouter ou modifier la photo",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Explicit button to add or change photo
                OutlinedButton(
                    onClick = { showPhotoOptionsDialog = true },
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = if (avatarUrl.isBlank()) Icons.Filled.AddAPhoto else Icons.Filled.PhotoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (avatarUrl.isBlank()) "Ajouter une photo" else "Modifier la photo",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = fullName,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = jobTitle,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Row(
                        modifier = Modifier.padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Badge,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Matricule : $matricule",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Apartment,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = department,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }
            }

            // Informations personnelles de l'employé connecté
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Informations personnelles",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    TextButton(onClick = { showEditProfileDialog = true }) {
                        Text("Modifier", fontWeight = FontWeight.Bold)
                    }
                }

                ProfileInfoCard(
                    icon = Icons.Filled.Badge,
                    label = "Matricule Employé (Attribué automatiquement)",
                    value = matricule
                )
                ProfileInfoCard(
                    icon = Icons.Filled.Mail,
                    label = com.example.ui.i18n.tr("prof_email"),
                    value = email
                )
                ProfileInfoCard(
                    icon = Icons.Filled.Phone,
                    label = com.example.ui.i18n.tr("prof_phone"),
                    value = phone
                )
                ProfileInfoCard(
                    icon = Icons.Filled.CalendarToday,
                    label = "Date d'embauche",
                    value = hireDate
                )
                ProfileInfoCard(
                    icon = Icons.Filled.LocationOn,
                    label = "Bureau / Emplacement",
                    value = officeLocation
                )
            }
        }
    }

    val profileSettingsSection = @Composable {
        // Paramètres de l'application
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Paramètres du compte & Apparence",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

                // Global Theme Mode Selector (Clair / Sombre / Système)
                ThemeModeSelectorCard()

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column {
                        SettingRow(
                            icon = Icons.Filled.CameraAlt,
                            title = "Photo de profil",
                            subtitle = if (avatarUrl.isBlank()) "Aucune photo définie (Ajouter)" else "Modifier ou supprimer",
                            onClick = { showPhotoOptionsDialog = true },
                            trailing = {
                                Icon(
                                    imageVector = Icons.Filled.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outlineVariant
                                )
                            }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainer)
                        var notificationsEnabled by remember { mutableStateOf(true) }
                        SettingRow(
                            icon = Icons.Filled.Notifications,
                            title = "Notifications & Alertes",
                            trailing = {
                                Switch(
                                    checked = notificationsEnabled,
                                    onCheckedChange = { notificationsEnabled = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = MaterialTheme.colorScheme.surface,
                                        checkedTrackColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainer)
                        SettingRow(
                            icon = Icons.Filled.Lock,
                            title = "Sécurité & Mot de passe",
                            subtitle = "Changer mon mot de passe",
                            onClick = { showSecurityScreen = true },
                            trailing = {
                                Icon(
                                    imageVector = Icons.Filled.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outlineVariant
                                )
                            }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainer)
                        SettingRow(
                            icon = if (isCloudConnected) Icons.Filled.CloudDone else Icons.Filled.CloudSync,
                            title = "Synchronisation Multi-Téléphones",
                            subtitle = if (isCloudConnected) "Connecté en temps réel" else "Connexion au réseau...",
                            onClick = { showCloudSyncDialog = true },
                            trailing = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (isCloudConnected) Color(0xFF10B981) else MaterialTheme.colorScheme.error)
                                    )
                                    Icon(
                                        imageVector = Icons.Filled.ChevronRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.outlineVariant
                                    )
                                }
                            }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainer)
                        SettingRow(
                            icon = Icons.Filled.Storage,
                            title = "Base de données Supabase",
                            subtitle = if (isSupabaseConnected) "Connecté (Synchronisation cloud)" else "Configurer la liaison Supabase",
                            onClick = { showSupabaseDialog = true },
                            trailing = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (isSupabaseConnected) Color(0xFF3ECF8E) else Color.Gray)
                                    )
                                    Icon(
                                        imageVector = Icons.Filled.ChevronRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.outlineVariant
                                    )
                                }
                            }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainer)
                        val currentLangCode by I18nManager.currentLang.collectAsState()
                        val currentLangName = when(currentLangCode) {
                            "fr" -> "Français"
                            "en" -> "English"
                            "ar" -> "العربية"
                            "de" -> "Deutsch"
                            else -> "Français"
                        }
                        
                        SettingRow(
                            icon = Icons.Filled.Language,
                            title = com.example.ui.i18n.tr("prof_lang"),
                            subtitle = currentLangName,
                            onClick = { showLanguageDialog = true },
                            trailing = {
                                Icon(
                                    imageVector = Icons.Filled.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outlineVariant
                                )
                            }
                        )
                    }
                }
            }
        }

    val profileLogoutSection = @Composable {
        // Logout Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Button(
                onClick = {
                    authViewModel?.logout()
                    onLogout()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                    Text(text = com.example.ui.i18n.tr("btn_logout"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
            Text(
                text = "TimeOff • Connecté en tant que $fullName",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 1200.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = if (isWideScreen) 32.dp else 16.dp, vertical = if (isWideScreen) 24.dp else 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        if (isWideScreen) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(32.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    profileInfoSection()
                    profileLogoutSection()
                }
                Column(
                    modifier = Modifier.weight(1.1f),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    profileSettingsSection()
                }
            }
        } else {
            profileInfoSection()
            profileSettingsSection()
            profileLogoutSection()
        }
    }
        }
    }

    // Language Selection Dialog
    if (showLanguageDialog) {
        val currentLangCode by I18nManager.currentLang.collectAsState()
        val languages = listOf(
            "fr" to "Français",
            "en" to "English",
            "ar" to "العربية",
            "de" to "Deutsch"
        )
        
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = {
                Text(com.example.ui.i18n.tr("select_language"))
            },
            text = {
                Column(
                    modifier = Modifier
                        .selectableGroup()
                        .verticalScroll(rememberScrollState())
                ) {
                    languages.forEach { (code, name) ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .selectable(
                                    selected = (code == currentLangCode),
                                    onClick = { 
                                        I18nManager.setLang(context, code)
                                        showLanguageDialog = false
                                    }
                                )
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (code == currentLangCode),
                                onClick = null,
                                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(text = name, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(com.example.ui.i18n.tr("btn_close"))
                }
            }
        )
    }

    // Photo Selection / Options Dialog
    if (showPhotoOptionsDialog) {
        AlertDialog(
            onDismissRequest = { showPhotoOptionsDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CameraAlt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Photo de profil",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Choisissez comment définir votre photo de profil :",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Option 1: Choose from Gallery
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showPhotoOptionsDialog = false
                                galleryLauncher.launch("image/*")
                            },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PhotoLibrary,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Column {
                                Text(
                                    text = "Choisir dans la galerie",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Importer depuis les photos de l'appareil",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }

                    // Option 2: Enter URL
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showPhotoOptionsDialog = false
                                inputCustomUrl = avatarUrl
                                showUrlInputDialog = true
                            },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Link,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            }
                            Column {
                                Text(
                                    text = "Entrer un lien URL",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Coller l'adresse web d'une image",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }

                    // Option 3: Remove photo (if exists)
                    if (avatarUrl.isNotBlank()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    authViewModel?.updateAvatar("")
                                    showPhotoOptionsDialog = false
                                    Toast.makeText(context, "Photo de profil supprimée", Toast.LENGTH_SHORT).show()
                                },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.errorContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.DeleteOutline,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Supprimer la photo",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        text = "Revenir aux initiales par défaut",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showPhotoOptionsDialog = false }) {
                    Text("Fermer")
                }
            }
        )
    }

    // Direct URL Input Dialog
    if (showUrlInputDialog) {
        AlertDialog(
            onDismissRequest = { showUrlInputDialog = false },
            title = {
                Text(
                    text = "Lien de la photo",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Collez l'URL de votre photo ou avatar (ex: https://...) :",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = inputCustomUrl,
                        onValueChange = { inputCustomUrl = it },
                        label = { Text("URL de l'image") },
                        placeholder = { Text("https://example.com/photo.jpg") },
                        leadingIcon = { Icon(Icons.Filled.Image, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = inputCustomUrl.trim()
                        authViewModel?.updateAvatar(trimmed)
                        showUrlInputDialog = false
                        Toast.makeText(context, "Photo de profil mise à jour !", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Appliquer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUrlInputDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    // Edit Profile Modal Dialog
    if (showEditProfileDialog) {
        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = {
                Text(
                    text = "Modifier mes informations",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = editFullName,
                        onValueChange = { editFullName = it },
                        label = { Text("Nom complet") },
                        leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = editJobTitle,
                        onValueChange = { editJobTitle = it },
                        label = { Text(com.example.ui.i18n.tr("prof_pos")) },
                        leadingIcon = { Icon(Icons.Filled.Badge, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        enabled = false,
                        readOnly = true
                    )
                    OutlinedTextField(
                        value = editDepartment,
                        onValueChange = { editDepartment = it },
                        label = { Text(com.example.ui.i18n.tr("prof_dept")) },
                        leadingIcon = { Icon(Icons.Filled.Apartment, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        enabled = false,
                        readOnly = true
                    )
                    OutlinedTextField(
                        value = editEmail,
                        onValueChange = { editEmail = it },
                        label = { Text(com.example.ui.i18n.tr("prof_email")) },
                        leadingIcon = { Icon(Icons.Filled.Mail, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = editPhone,
                        onValueChange = { newValue -> 
                            editPhone = newValue.filter { it.isDigit() }
                        },
                        label = { Text("Numéro de téléphone") },
                        leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = editOffice,
                        onValueChange = { editOffice = it },
                        label = { Text("Emplacement bureau") },
                        leadingIcon = { Icon(Icons.Filled.LocationOn, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (currentUser != null) {
                            val updated = currentUser!!.copy(
                                email = editEmail.trim(),
                                fullName = editFullName.trim(),
                                jobTitle = editJobTitle.trim(),
                                department = editDepartment.trim(),
                                phone = editPhone.trim(),
                                officeLocation = editOffice.trim()
                            )
                            authViewModel?.updateUserProfile(updated, currentUser!!.email)
                            Toast.makeText(context, "Profil mis à jour avec succès !", Toast.LENGTH_SHORT).show()
                        }
                        showEditProfileDialog = false
                    }
                ) {
                    Text("Enregistrer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    if (showCloudSyncDialog) {
        CloudSyncManagementDialog(
            onDismiss = { showCloudSyncDialog = false },
            onOpenSupabase = {
                showCloudSyncDialog = false
                showSupabaseDialog = true
            }
        )
    }

    if (showSupabaseDialog) {
        com.example.ui.components.SupabaseManagementDialog(
            onDismiss = { showSupabaseDialog = false }
        )
    }
}

private fun saveImageToInternalStorage(context: Context, uri: Uri, userEmail: String): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val safeName = userEmail.replace("@", "_").replace(".", "_")
        val file = File(context.filesDir, "avatar_${safeName}.jpg")
        FileOutputStream(file).use { outputStream ->
            inputStream.copyTo(outputStream)
        }
        file.absolutePath
    } catch (e: Exception) {
        uri.toString()
    }
}

@Composable
fun ProfileInfoCard(
    icon: ImageVector,
    label: String,
    value: String
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
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    .padding(12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick)
                else Modifier
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        trailing()
    }
}
