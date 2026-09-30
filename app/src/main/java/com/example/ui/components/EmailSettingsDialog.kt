package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.service.EmailDispatchManager
import kotlinx.coroutines.launch

@Composable
fun EmailSettingsDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val emailManager = remember { EmailDispatchManager.getInstance(context) }
    val scope = rememberCoroutineScope()

    var host by remember { mutableStateOf(emailManager.smtpHost) }
    var portText by remember { mutableStateOf(emailManager.smtpPort.toString()) }
    var user by remember { mutableStateOf(emailManager.smtpUser) }
    var pass by remember { mutableStateOf(emailManager.smtpPass) }
    var fromEmail by remember { mutableStateOf(emailManager.smtpFrom) }
    var senderName by remember { mutableStateOf(emailManager.senderName) }
    var useSsl by remember { mutableStateOf(emailManager.isSsl) }
    var passVisible by remember { mutableStateOf(false) }

    var isTesting by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }
    var testSuccess by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Email,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Text(
                        text = "Configuration Serveur E-mail",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Envoi direct des codes de sécurité par SMTP",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Presets chips
                Text(
                    text = "Fournisseurs préconfigurés :",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = host.contains("gmail"),
                        onClick = {
                            host = "smtp.gmail.com"
                            portText = "465"
                            useSsl = true
                            if (fromEmail.isBlank()) fromEmail = user
                        },
                        label = { Text("Gmail", style = MaterialTheme.typography.labelSmall) }
                    )
                    FilterChip(
                        selected = host.contains("brevo"),
                        onClick = {
                            host = "smtp-relay.brevo.com"
                            portText = "587"
                            useSsl = false
                        },
                        label = { Text("Brevo", style = MaterialTheme.typography.labelSmall) }
                    )
                    FilterChip(
                        selected = host.contains("sendgrid"),
                        onClick = {
                            host = "smtp.sendgrid.net"
                            portText = "465"
                            useSsl = true
                            user = "apikey"
                        },
                        label = { Text("SendGrid", style = MaterialTheme.typography.labelSmall) }
                    )
                }

                // Help box
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "💡 Astuce Gmail : Activez la validation en 2 étapes sur Google, puis créez un 'Mot de passe d'application' à 16 lettres dans myaccount.google.com/apppasswords et collez-le ci-dessous.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it },
                    label = { Text("Hôte SMTP *") },
                    placeholder = { Text("smtp.gmail.com") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = portText,
                        onValueChange = { portText = it },
                        label = { Text("Port *") },
                        placeholder = { Text("465") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Row(
                        modifier = Modifier
                            .weight(1.2f)
                            .padding(top = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = useSsl,
                            onCheckedChange = { useSsl = it }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (useSsl) "SSL direct" else "STARTTLS",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                OutlinedTextField(
                    value = user,
                    onValueChange = { user = it },
                    label = { Text("Nom d'utilisateur / E-mail expéditeur *") },
                    placeholder = { Text("votre-email@gmail.com") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = pass,
                    onValueChange = { pass = it },
                    label = { Text("Mot de passe / Clé d'application *") },
                    placeholder = { Text("Mot de passe d'application ou clé API") },
                    trailingIcon = {
                        IconButton(onClick = { passVisible = !passVisible }) {
                            Icon(
                                imageVector = if (passVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = null
                            )
                        }
                    },
                    visualTransformation = if (passVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = senderName,
                    onValueChange = { senderName = it },
                    label = { Text("Nom d'affichage expéditeur") },
                    placeholder = { Text("Portail RH Entreprise") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                if (testResult != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (testSuccess) Color(0xFFECFDF5) else MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (testSuccess) Icons.Filled.CheckCircle else Icons.Filled.Close,
                                contentDescription = null,
                                tint = if (testSuccess) Color(0xFF065F46) else MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = testResult!!,
                                color = if (testSuccess) Color(0xFF065F46) else MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                // Test button
                OutlinedButton(
                    onClick = {
                        isTesting = true
                        testResult = null
                        scope.launch {
                            // Temporary save for testing
                            emailManager.smtpHost = host
                            emailManager.smtpPort = portText.toIntOrNull() ?: 465
                            emailManager.smtpUser = user
                            emailManager.smtpPass = pass
                            emailManager.smtpFrom = if (fromEmail.isBlank()) user else fromEmail
                            emailManager.senderName = senderName
                            emailManager.isSsl = useSsl

                            val res = emailManager.sendPasswordResetEmail(user, "123456", 15)
                            isTesting = false
                            if (res.isSuccess) {
                                testSuccess = true
                                testResult = "✅ Connexion réussie ! Un e-mail test a été délivré à $user."
                            } else {
                                testSuccess = false
                                testResult = "❌ Échec SMTP : ${res.exceptionOrNull()?.localizedMessage ?: "Vérifiez vos identifiants"}"
                            }
                        }
                    },
                    enabled = !isTesting && user.isNotBlank() && pass.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Test de connexion...")
                    } else {
                        Icon(imageVector = Icons.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Tester l'envoi d'un e-mail test")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    emailManager.smtpHost = host
                    emailManager.smtpPort = portText.toIntOrNull() ?: 465
                    emailManager.smtpUser = user
                    emailManager.smtpPass = pass
                    emailManager.smtpFrom = if (fromEmail.isBlank()) user else fromEmail
                    emailManager.senderName = senderName
                    emailManager.isSsl = useSsl
                    onDismiss()
                },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Enregistrer les paramètres")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}
