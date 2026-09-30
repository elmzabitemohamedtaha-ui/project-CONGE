package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Shield
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import android.content.Intent
import android.net.Uri
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Devices
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CorporateFare
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.example.ui.AuthViewModel
import com.example.ui.theme.ThemeToggleIconButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    onLoginSuccess: () -> Unit,
    onAdminLogin: () -> Unit
) {
    val context = LocalContext.current
    val lastUsedUser = remember { authViewModel.getLastUsedUser() }
    val initialEmail = remember { authViewModel.getLastUsedEmail() ?: "" }

    var email by rememberSaveable { mutableStateOf(initialEmail) }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    val loginError by authViewModel.loginError.collectAsState()
    val isAuthLoading by authViewModel.isAuthLoading.collectAsState()
    val isFirebaseConfigured by authViewModel.isFirebaseAuthAvailable.collectAsState()
    val signUpError by authViewModel.signUpError.collectAsState()

    var showForgotPasswordDialog by rememberSaveable { mutableStateOf(false) }
    var showRegisterDialog by rememberSaveable { mutableStateOf(false) }

    val isWorkEmail = authViewModel.isWorkEmail(email)

    Scaffold { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            // Orientation & size checks: 2-column showcase on large desktop or landscape tablet
            val isWideDesktop = maxWidth >= 960.dp || (maxWidth > maxHeight && maxWidth >= 760.dp && maxHeight >= 500.dp)
            val isDesktop = maxWidth >= 840.dp || (maxWidth > maxHeight && maxWidth >= 580.dp)

            val loginCardContent: @Composable (Boolean) -> Unit = { cardIsDesktop ->
                Card(
                    modifier = Modifier
                        .widthIn(max = 500.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 0.dp)
                        .padding(vertical = if (cardIsDesktop) 24.dp else 0.dp),
                    shape = RoundedCornerShape(if (cardIsDesktop) 28.dp else 0.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (cardIsDesktop) MaterialTheme.colorScheme.surfaceContainerLowest else Color.Transparent
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (cardIsDesktop) 6.dp else 0.dp),
                    border = if (cardIsDesktop) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)) else null
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .imePadding()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = if (cardIsDesktop) 32.dp else 24.dp, vertical = if (cardIsDesktop) 32.dp else 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                // Logo Section
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_app_logo),
                            contentDescription = "Logo TimeOff",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    }
                    Text(
                        text = "TimeOff",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Portail de Gestion des Congés & Absences",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                // Remembered user quick login (standard login shortcut)
                if (lastUsedUser != null) {
                    Card(
                        onClick = {
                            email = lastUsedUser.email
                        },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
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
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Person,
                                    contentDescription = "Utilisateur",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Dernier compte utilisé",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${lastUsedUser.fullName} (${lastUsedUser.email})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Form Section
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            authViewModel.clearErrors()
                        },
                        label = { Text(com.example.ui.i18n.tr("login_email")) },
                        placeholder = { Text(com.example.ui.i18n.tr("login_email_placeholder")) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Email,
                                contentDescription = null,
                                tint = if (isWorkEmail) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                        },
                        trailingIcon = {
                            if (email.isNotBlank() && isWorkEmail) {
                                Icon(
                                    imageVector = Icons.Filled.Verified,
                                    contentDescription = "Email professionnel valide",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                        )
                    )

                    if (email.isNotBlank() && isWorkEmail) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CorporateFare,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Email professionnel détecté • Authentification Firebase Auth",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            authViewModel.clearErrors()
                        },
                        label = { Text(com.example.ui.i18n.tr("login_password")) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline
                            )
                        },
                        trailingIcon = {
                            val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = image,
                                    contentDescription = if (passwordVisible) "Masquer mot de passe" else "Afficher mot de passe",
                                    tint = MaterialTheme.colorScheme.outline
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                authViewModel.clearErrors()
                                showRegisterDialog = true
                            },
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PersonAdd,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Créer un compte pro",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        TextButton(
                            onClick = {
                                authViewModel.clearErrors()
                                showForgotPasswordDialog = true
                            },
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Mot de passe oublié ?",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }

                if (loginError != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = loginError!!,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Action Section
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = {
                            authViewModel.login(
                                email = email,
                                passwordHash = password,
                                onSuccess = onLoginSuccess,
                                onAdminSuccess = onAdminLogin
                            )
                        },
                        enabled = !isAuthLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        if (isAuthLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.5.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Connexion en cours...",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Login,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isWorkEmail) "Se connecter avec mon email pro" else "Se connecter",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Firebase Auth Status Indicator
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isFirebaseConfigured) Color(0xFF10B981) else MaterialTheme.colorScheme.primary)
                        )
                        Text(
                            text = if (isFirebaseConfigured) "Firebase Auth : Prêt pour emails professionnels" else "Auth Hybride : Prêt",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Security & Privacy Enterprise Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Authentification sécurisée • Base de données chiffrée",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }

        // Top Right Theme Toggle (floating on all screen formats)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.TopEnd
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.8f),
                shadowElevation = 2.dp
            ) {
                ThemeToggleIconButton(
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        if (isWideDesktop) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 40.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Hero Showcase Panel for Computers / Desktops
                Card(
                    modifier = Modifier
                        .widthIn(max = 460.dp)
                        .fillMaxHeight(0.88f),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(68.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    androidx.compose.foundation.Image(
                                        painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_app_logo),
                                        contentDescription = "Logo TimeOff",
                                        modifier = Modifier.size(48.dp)
                                    )
                                }
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "TimeOff Enterprise",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Solution collaborative pour la gestion des congés et plannings d'équipes.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                            // Feature bullets
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                DesktopFeatureItem(
                                    icon = Icons.Filled.CloudDone,
                                    title = "Synchronisation Supabase Cloud",
                                    subtitle = "Données persistées sur Supabase et hors-ligne Room SQLite"
                                )
                                DesktopFeatureItem(
                                    icon = Icons.Filled.DateRange,
                                    title = "Planning d'Équipe Intelligent",
                                    subtitle = "Détection des chevauchements d'absences en temps réel"
                                )
                                DesktopFeatureItem(
                                    icon = Icons.Filled.Security,
                                    title = "Validation Hiérarchique & Sécurité",
                                    subtitle = "Workflows RH conformes, alertes directes et exports PDF"
                                )
                                DesktopFeatureItem(
                                    icon = Icons.Filled.Devices,
                                    title = "Multiplateforme Fluide",
                                    subtitle = "Expérience ergonomique sur ordinateurs, tablettes et mobiles"
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                        ) {
                            Text(
                                text = "Portail RH Sécurisé • Multi-terminaux",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(36.dp))

                // Right Login Card
                loginCardContent(true)
            }
        } else {
            loginCardContent(isDesktop)
        }
    }
}

    if (showForgotPasswordDialog) {
        ForgotPasswordDialog(
            initialEmail = email,
            onDismiss = { showForgotPasswordDialog = false },
            onSendReset = { targetEmail, onResult ->
                authViewModel.sendPasswordResetEmail(targetEmail, onResult)
            },
            onVerifyAndReset = { targetEmail, token, newPass, confirmPass, onResult ->
                authViewModel.verifyTokenAndResetPassword(targetEmail, token, newPass, confirmPass, onResult)
            },
            onPasswordResetSuccess = { updatedEmail ->
                email = updatedEmail
                password = ""
                showForgotPasswordDialog = false
            }
        )
    }

    if (showRegisterDialog) {
        RegisterWorkAccountDialog(
            initialEmail = email,
            signUpError = signUpError,
            onDismiss = {
                authViewModel.clearErrors()
                showRegisterDialog = false
            },
            onRegister = { fullName, regEmail, regPassword, jobTitle, department ->
                authViewModel.signUp(
                    fullName = fullName,
                    email = regEmail,
                    passwordHash = regPassword,
                    jobTitle = jobTitle,
                    department = department,
                    onSuccess = {
                        showRegisterDialog = false
                        onLoginSuccess()
                    }
                )
            }
        )
    }
}

@Composable
fun ForgotPasswordDialog(
    initialEmail: String,
    onDismiss: () -> Unit,
    onSendReset: (String, (Boolean, String) -> Unit) -> Unit,
    onVerifyAndReset: (email: String, token: String, newPass: String, confirmPass: String, (Boolean, String) -> Unit) -> Unit,
    onPasswordResetSuccess: (email: String) -> Unit
) {
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    var emailInput by rememberSaveable { mutableStateOf(initialEmail) }
    var isSending by rememberSaveable { mutableStateOf(false) }
    var requestStatusMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var isRequestSuccess by rememberSaveable { mutableStateOf(false) }

    // Step 2 state
    var tokenInput by rememberSaveable { mutableStateOf("") }
    var newPasswordInput by rememberSaveable { mutableStateOf("") }
    var confirmPasswordInput by rememberSaveable { mutableStateOf("") }
    var newPasswordVisible by rememberSaveable { mutableStateOf(false) }
    var confirmPasswordVisible by rememberSaveable { mutableStateOf(false) }
    var isResetting by rememberSaveable { mutableStateOf(false) }
    var resetStatusMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var isResetSuccess by rememberSaveable { mutableStateOf(false) }
    var showEmailSettingsDialog by remember { mutableStateOf(false) }

    // Password strength calculation
    val hasMinLength = newPasswordInput.length >= 8
    val hasLetter = newPasswordInput.any { it.isLetter() }
    val hasDigit = newPasswordInput.any { it.isDigit() }
    val passwordStrengthScore = listOf(hasMinLength, hasLetter, hasDigit).count { it }
    val passwordsMatch = newPasswordInput.isNotEmpty() && newPasswordInput == confirmPasswordInput

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LockReset,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Mot de passe oublié",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Envoi SMTP & Expiration 15 min",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = { showEmailSettingsDialog = true }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Paramètres SMTP",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Stepper TabRow
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                text = "1. Demande de lien",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                text = "2. Nouveau mot de passe",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }

                if (selectedTab == 0) {
                    // STEP 1 : REQUEST RESET
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Saisissez votre e-mail professionnel. Un lien et un code de réinitialisation sécurisé à usage unique vous seront transmis.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = {
                            emailInput = it
                            requestStatusMessage = null
                        },
                        label = { Text("Adresse email professionnelle *") },
                        placeholder = { Text("collaborateur@entreprise.com") },
                        leadingIcon = {
                            Icon(Icons.Filled.Email, contentDescription = null)
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { showEmailSettingsDialog = true },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Configurer SMTP / E-mail direct", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    if (requestStatusMessage != null) {
                        val context = LocalContext.current
                        val clipboardManager = LocalClipboardManager.current
                        var isCodeCopied by remember { mutableStateOf(false) }
                        val extractedCode = remember(requestStatusMessage) {
                            Regex("""\b(\d{6})\b""").find(requestStatusMessage ?: "")?.value
                        }

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isRequestSuccess) Color(0xFFECFDF5) else MaterialTheme.colorScheme.errorContainer
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isRequestSuccess) Icons.Filled.CheckCircle else Icons.Filled.Close,
                                        contentDescription = null,
                                        tint = if (isRequestSuccess) Color(0xFF065F46) else MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = requestStatusMessage!!,
                                        color = if (isRequestSuccess) Color(0xFF065F46) else MaterialTheme.colorScheme.onErrorContainer,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                if (isRequestSuccess && extractedCode != null) {
                                    // Security Code Highlight Box
                                    Surface(
                                        color = Color.White,
                                        shape = RoundedCornerShape(8.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA7F3D0)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = "Code temporaire (15 min) :",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color(0xFF065F46)
                                                )
                                                Text(
                                                    text = extractedCode,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF047857),
                                                    letterSpacing = androidx.compose.ui.unit.TextUnit(2f, androidx.compose.ui.unit.TextUnitType.Sp)
                                                )
                                            }

                                            TextButton(
                                                onClick = {
                                                    clipboardManager.setText(AnnotatedString(extractedCode))
                                                    isCodeCopied = true
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = if (isCodeCopied) Icons.Filled.Check else Icons.Filled.ContentCopy,
                                                    contentDescription = null,
                                                    tint = Color(0xFF047857),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = if (isCodeCopied) "Copié !" else "Copier",
                                                    color = Color(0xFF047857),
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }

                                    // Action to open Mail app with pre-filled reset message
                                    OutlinedButton(
                                        onClick = {
                                            val mailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                                data = Uri.parse("mailto:")
                                                putExtra(Intent.EXTRA_EMAIL, arrayOf(emailInput.trim()))
                                                putExtra(Intent.EXTRA_SUBJECT, "Code de réinitialisation de mot de passe")
                                                putExtra(
                                                    Intent.EXTRA_TEXT,
                                                    "Votre code de sécurité de réinitialisation est : $extractedCode (valable 15 minutes).\nSaisissez ce code dans l'application pour choisir votre nouveau mot de passe."
                                                )
                                            }
                                            try {
                                                context.startActivity(Intent.createChooser(mailIntent, "Envoyer ou consulter dans Mail"))
                                            } catch (_: Exception) {}
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Email,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Ouvrir dans l'application Mail / Gmail")
                                    }

                                    // Direct action to jump to Step 2 with code pre-filled
                                    Button(
                                        onClick = {
                                            tokenInput = extractedCode
                                            selectedTab = 1
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Remplir le code & continuer vers l'étape 2")
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    // Clear technical note explaining why it doesn't leave automatically
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "ℹ️ Note SMTP : Pour qu'un e-mail parte automatiquement vers une vraie boîte Gmail sans passer par le client mail, un serveur SMTP (SendGrid, Supabase ou Firebase) doit être configuré. En mode autonome, le code sécurisé ci-dessus est généré instantanément.",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF475569),
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    TextButton(
                        onClick = { selectedTab = 1 },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Vous avez déjà reçu un code ? Passer à l'étape 2",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    // STEP 2 : ENTER TOKEN AND SET NEW PASSWORD
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Key,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Saisissez le code de sécurité reçu ainsi que votre nouveau mot de passe.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("Email professionnel") },
                        leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = tokenInput,
                        onValueChange = {
                            tokenInput = it
                            resetStatusMessage = null
                        },
                        label = { Text("Code de sécurité ou Token *") },
                        placeholder = { Text("Ex: 482915") },
                        leadingIcon = { Icon(Icons.Filled.Key, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = newPasswordInput,
                        onValueChange = {
                            newPasswordInput = it
                            resetStatusMessage = null
                        },
                        label = { Text("Nouveau mot de passe *") },
                        placeholder = { Text("Min. 8 caractères avec lettres et chiffres") },
                        leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                                Icon(
                                    imageVector = if (newPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = if (newPasswordVisible) "Masquer" else "Afficher"
                                )
                            }
                        },
                        visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Strength indicator bar
                    if (newPasswordInput.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Force du mot de passe",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = when (passwordStrengthScore) {
                                        3 -> "Robuste"
                                        2 -> "Moyen"
                                        else -> "Trop faible"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = when (passwordStrengthScore) {
                                        3 -> Color(0xFF059669)
                                        2 -> Color(0xFFD97706)
                                        else -> MaterialTheme.colorScheme.error
                                    }
                                )
                            }
                            LinearProgressIndicator(
                                progress = { passwordStrengthScore / 3f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = when (passwordStrengthScore) {
                                    3 -> Color(0xFF059669)
                                    2 -> Color(0xFFD97706)
                                    else -> MaterialTheme.colorScheme.error
                                },
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = if (hasMinLength) "✓ 8+ car." else "• 8+ car.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (hasMinLength) Color(0xFF059669) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (hasLetter) "✓ Lettre" else "• Lettre",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (hasLetter) Color(0xFF059669) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (hasDigit) "✓ Chiffre" else "• Chiffre",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (hasDigit) Color(0xFF059669) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = confirmPasswordInput,
                        onValueChange = {
                            confirmPasswordInput = it
                            resetStatusMessage = null
                        },
                        label = { Text("Confirmer le mot de passe *") },
                        placeholder = { Text("Répétez le mot de passe") },
                        leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                Icon(
                                    imageVector = if (confirmPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = if (confirmPasswordVisible) "Masquer" else "Afficher"
                                )
                            }
                        },
                        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    if (confirmPasswordInput.isNotEmpty()) {
                        Text(
                            text = if (passwordsMatch) "✓ Les mots de passe correspondent" else "✗ Les mots de passe ne correspondent pas",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (passwordsMatch) Color(0xFF059669) else MaterialTheme.colorScheme.error
                        )
                    }

                    if (resetStatusMessage != null) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isResetSuccess) Color(0xFFECFDF5) else MaterialTheme.colorScheme.errorContainer
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isResetSuccess) Icons.Filled.CheckCircle else Icons.Filled.Close,
                                        contentDescription = null,
                                        tint = if (isResetSuccess) Color(0xFF065F46) else MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = resetStatusMessage!!,
                                        color = if (isResetSuccess) Color(0xFF065F46) else MaterialTheme.colorScheme.onErrorContainer,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }

                                if (isResetSuccess) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = {
                                            onPasswordResetSuccess(emailInput.trim())
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Se connecter avec le nouveau mot de passe")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (selectedTab == 0) {
                Button(
                    onClick = {
                        if (emailInput.isBlank()) {
                            requestStatusMessage = "Veuillez renseigner votre adresse email."
                            isRequestSuccess = false
                            return@Button
                        }
                        isSending = true
                        requestStatusMessage = null
                        onSendReset(emailInput.trim()) { success, msg ->
                            isSending = false
                            isRequestSuccess = success
                            requestStatusMessage = msg
                        }
                    },
                    enabled = !isSending,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isSending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Envoi en cours...")
                    } else {
                        Text("Envoyer le lien")
                    }
                }
            } else {
                Button(
                    onClick = {
                        if (emailInput.isBlank() || tokenInput.isBlank() || newPasswordInput.isBlank()) {
                            resetStatusMessage = "Veuillez renseigner tous les champs obligatoires."
                            isResetSuccess = false
                            return@Button
                        }
                        if (newPasswordInput.length < 8) {
                            resetStatusMessage = "Le mot de passe doit comporter au moins 8 caractères."
                            isResetSuccess = false
                            return@Button
                        }
                        if (!passwordsMatch) {
                            resetStatusMessage = "Les deux mots de passe ne correspondent pas."
                            isResetSuccess = false
                            return@Button
                        }
                        isResetting = true
                        resetStatusMessage = null
                        onVerifyAndReset(
                            emailInput.trim(),
                            tokenInput.trim(),
                            newPasswordInput.trim(),
                            confirmPasswordInput.trim()
                        ) { success, msg ->
                            isResetting = false
                            isResetSuccess = success
                            resetStatusMessage = msg
                        }
                    },
                    enabled = !isResetting && !isResetSuccess,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isResetting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Mise à jour...")
                    } else {
                        Text("Valider le mot de passe")
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Fermer")
            }
        }
    )

    if (showEmailSettingsDialog) {
        com.example.ui.components.EmailSettingsDialog(
            onDismiss = { showEmailSettingsDialog = false }
        )
    }
}

@Composable
fun RegisterWorkAccountDialog(
    initialEmail: String,
    signUpError: String?,
    onDismiss: () -> Unit,
    onRegister: (fullName: String, email: String, password: String, jobTitle: String, department: String) -> Unit
) {
    var fullName by rememberSaveable { mutableStateOf("") }
    var regEmail by rememberSaveable { mutableStateOf(initialEmail) }
    var regPassword by rememberSaveable { mutableStateOf("") }
    var jobTitle by rememberSaveable { mutableStateOf("Collaborateur") }
    var department by rememberSaveable { mutableStateOf("Ingénierie & IT") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.PersonAdd,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Nouveau compte collaborateur",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Créez votre compte professionnel pour vous connecter via Firebase Auth et gérer vos congés.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Nom et Prénom *") },
                    leadingIcon = { Icon(Icons.Filled.Badge, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = regEmail,
                    onValueChange = { regEmail = it },
                    label = { Text("Email professionnel *") },
                    leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = regPassword,
                    onValueChange = { regPassword = it },
                    label = { Text("Mot de passe (min 6 car.) *") },
                    leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                    trailingIcon = {
                        val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = image, contentDescription = null)
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = jobTitle,
                    onValueChange = { jobTitle = it },
                    label = { Text("Poste / Fonction") },
                    leadingIcon = { Icon(Icons.Filled.Work, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = department,
                    onValueChange = { department = it },
                    label = { Text("Département") },
                    leadingIcon = { Icon(Icons.Filled.CorporateFare, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                if (signUpError != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = signUpError,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onRegister(fullName, regEmail, regPassword, jobTitle, department)
                },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Créer mon compte")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

@Composable
private fun DesktopFeatureItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            modifier = Modifier.size(38.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

