package com.example.service

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.Socket
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

/**
 * Manages enterprise email delivery:
 * 1. Direct native SMTP over SSL (Port 465) or STARTTLS (Port 587) - Works with Gmail, Brevo, SendGrid, Outlook, etc.
 * 2. Web Email API via OkHttp (Brevo, Resend, SendGrid)
 * 3. Fallback to Local Push Notification & Android Mail Intent
 */
class EmailDispatchManager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val httpClient = OkHttpClient.Builder().build()

    companion object {
        private const val PREFS_NAME = "enterprise_email_dispatch_prefs"
        private const val TAG = "EmailDispatchManager"

        private const val KEY_SMTP_ENABLED = "smtp_enabled"
        private const val KEY_SMTP_HOST = "smtp_host"
        private const val KEY_SMTP_PORT = "smtp_port"
        private const val KEY_SMTP_USER = "smtp_user"
        private const val KEY_SMTP_PASS = "smtp_pass"
        private const val KEY_SMTP_FROM = "smtp_from"
        private const val KEY_SMTP_SENDER_NAME = "smtp_sender_name"
        private const val KEY_SMTP_USE_SSL = "smtp_use_ssl"
        private const val KEY_API_KEY = "email_api_key"
        private const val KEY_PROVIDER_TYPE = "email_provider_type" // "SMTP", "BREVO_API", "RESEND_API"

        @Volatile
        private var INSTANCE: EmailDispatchManager? = null

        fun getInstance(context: Context): EmailDispatchManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: EmailDispatchManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    var isSmtpEnabled: Boolean
        get() = prefs.getBoolean(KEY_SMTP_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_SMTP_ENABLED, value).apply()

    var smtpHost: String
        get() = prefs.getString(KEY_SMTP_HOST, "smtp.gmail.com") ?: "smtp.gmail.com"
        set(value) = prefs.edit().putString(KEY_SMTP_HOST, value.trim()).apply()

    var smtpPort: Int
        get() = prefs.getInt(KEY_SMTP_PORT, 465)
        set(value) = prefs.edit().putInt(KEY_SMTP_PORT, value).apply()

    var smtpUser: String
        get() = prefs.getString(KEY_SMTP_USER, "elmzabitemohamedtaha@gmail.com") ?: "elmzabitemohamedtaha@gmail.com"
        set(value) = prefs.edit().putString(KEY_SMTP_USER, value.trim()).apply()

    var smtpPass: String
        get() = prefs.getString(KEY_SMTP_PASS, "") ?: ""
        set(value) = prefs.edit().putString(KEY_SMTP_PASS, value.trim()).apply()

    var smtpFrom: String
        get() = prefs.getString(KEY_SMTP_FROM, "")?.takeIf { it.isNotBlank() } ?: smtpUser
        set(value) = prefs.edit().putString(KEY_SMTP_FROM, value.trim()).apply()

    var senderName: String
        get() = prefs.getString(KEY_SMTP_SENDER_NAME, "Portail RH TimeOff") ?: "Portail RH TimeOff"
        set(value) = prefs.edit().putString(KEY_SMTP_SENDER_NAME, value.trim()).apply()

    var isSsl: Boolean
        get() = prefs.getBoolean(KEY_SMTP_USE_SSL, smtpPort == 465)
        set(value) = prefs.edit().putBoolean(KEY_SMTP_USE_SSL, value).apply()

    var apiKey: String
        get() = prefs.getString(KEY_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_API_KEY, value.trim()).apply()

    var providerType: String
        get() = prefs.getString(KEY_PROVIDER_TYPE, "SMTP") ?: "SMTP"
        set(value) = prefs.edit().putString(KEY_PROVIDER_TYPE, value.trim()).apply()

    private val _lastDispatchStatus = MutableStateFlow<String?>(null)
    val lastDispatchStatus: StateFlow<String?> = _lastDispatchStatus.asStateFlow()

    fun isConfigured(): Boolean {
        return (providerType == "SMTP" && smtpPass.isNotBlank() && smtpUser.isNotBlank()) ||
                (providerType != "SMTP" && apiKey.isNotBlank())
    }

    /**
     * Sends password reset email with formatted HTML template and 6-digit code.
     */
    suspend fun sendPasswordResetEmail(
        toEmail: String,
        securityCode: String,
        expirationMinutes: Int = 15
    ): Result<String> = withContext(Dispatchers.IO) {
        val cleanTo = toEmail.trim().lowercase()
        val subject = "Code de sécurité : Réinitialisation de votre mot de passe - Portail RH"

        val htmlContent = buildHtmlResetEmail(cleanTo, securityCode, expirationMinutes)

        // 1. Try Brevo / Resend REST API if configured
        if (providerType == "BREVO_API" && apiKey.isNotBlank()) {
            return@withContext sendViaBrevoApi(cleanTo, subject, htmlContent)
        }
        if (providerType == "RESEND_API" && apiKey.isNotBlank()) {
            return@withContext sendViaResendApi(cleanTo, subject, htmlContent)
        }

        // 2. Try SMTP if credentials are provided
        if (smtpPass.isNotBlank() && smtpUser.isNotBlank()) {
            return@withContext sendViaSmtp(
                host = smtpHost,
                port = smtpPort,
                username = smtpUser,
                password = smtpPass,
                fromEmail = smtpFrom,
                fromName = senderName,
                toEmail = cleanTo,
                subject = subject,
                htmlBody = htmlContent,
                useSsl = isSsl
            )
        }

        // 3. Fallback when SMTP pass is not yet entered
        _lastDispatchStatus.value = "SMTP non configuré (mot de passe d'application manquant)"
        Result.failure(
            IllegalStateException("SMTP_NOT_CONFIGURED: Veuillez renseigner le mot de passe d'application SMTP dans les paramètres d'envoi.")
        )
    }

    /**
     * Native Pure Kotlin SMTP Client implementation (SSL 465 or STARTTLS 587)
     */
    private suspend fun sendViaSmtp(
        host: String,
        port: Int,
        username: String,
        password: String,
        fromEmail: String,
        fromName: String,
        toEmail: String,
        subject: String,
        htmlBody: String,
        useSsl: Boolean
    ): Result<String> = withContext(Dispatchers.IO) {
        var socket: Socket? = null
        try {
            Log.d(TAG, "Connecting to SMTP server $host:$port (SSL: $useSsl)...")

            socket = if (useSsl) {
                SSLSocketFactory.getDefault().createSocket(host, port) as SSLSocket
            } else {
                Socket(host, port)
            }
            socket.soTimeout = 12000

            var reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            var writer = PrintWriter(OutputStreamWriter(socket.getOutputStream(), Charsets.UTF_8), true)

            fun readResponse(): String {
                val sb = StringBuilder()
                var line: String?
                while (true) {
                    line = reader.readLine() ?: break
                    sb.append(line).append("\n")
                    if (line.length >= 4 && line[3] == ' ') break
                    if (line.length == 3) break
                }
                val resp = sb.toString().trim()
                Log.d(TAG, "SMTP << $resp")
                return resp
            }

            fun sendCommand(cmd: String, expectedCode: String = ""): String {
                Log.d(TAG, "SMTP >> ${if (cmd.startsWith("AUTH") || cmd.length > 30) cmd.take(15) + "..." else cmd}")
                writer.print(cmd + "\r\n")
                writer.flush()
                val resp = readResponse()
                if (expectedCode.isNotEmpty() && !resp.startsWith(expectedCode)) {
                    throw IllegalStateException("Erreur SMTP sur '$cmd': $resp")
                }
                return resp
            }

            // Banner 220
            val banner = readResponse()
            if (!banner.startsWith("220")) {
                throw IllegalStateException("Réponse inattendue du serveur SMTP: $banner")
            }

            sendCommand("EHLO localhost", "250")

            // STARTTLS for port 587 or non-SSL connections
            if (!useSsl && port == 587) {
                sendCommand("STARTTLS", "220")
                val sslSocket = (SSLSocketFactory.getDefault() as SSLSocketFactory)
                    .createSocket(socket, host, port, true) as SSLSocket
                sslSocket.soTimeout = 12000
                socket = sslSocket

                reader = BufferedReader(InputStreamReader(sslSocket.getInputStream()))
                writer = PrintWriter(OutputStreamWriter(sslSocket.getOutputStream(), Charsets.UTF_8), true)

                sendCommand("EHLO localhost", "250")
            }

            // AUTH LOGIN
            if (username.isNotEmpty() && password.isNotEmpty()) {
                sendCommand("AUTH LOGIN", "334")
                val userB64 = Base64.encodeToString(username.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
                sendCommand(userB64, "334")
                val passB64 = Base64.encodeToString(password.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
                sendCommand(passB64, "235")
            }

            sendCommand("MAIL FROM:<$fromEmail>", "250")
            sendCommand("RCPT TO:<$toEmail>", "250")
            sendCommand("DATA", "354")

            val subjectB64 = Base64.encodeToString(subject.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            val fromNameB64 = Base64.encodeToString(fromName.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)

            val mimeMessage = buildString {
                append("From: =?UTF-8?B?$fromNameB64?= <$fromEmail>\r\n")
                append("To: <$toEmail>\r\n")
                append("Subject: =?UTF-8?B?$subjectB64?=\r\n")
                append("MIME-Version: 1.0\r\n")
                append("Content-Type: text/html; charset=UTF-8\r\n")
                append("Content-Transfer-Encoding: 8bit\r\n")
                append("\r\n")
                append(htmlBody)
                append("\r\n.")
            }

            writer.print(mimeMessage + "\r\n")
            writer.flush()
            val dataResp = readResponse()
            if (!dataResp.startsWith("250")) {
                throw IllegalStateException("Échec lors de l'envoi des données SMTP: $dataResp")
            }

            try { sendCommand("QUIT") } catch (_: Exception) {}

            _lastDispatchStatus.value = "E-mail délivré avec succès via SMTP à $toEmail"
            Result.success("E-mail envoyé avec succès à $toEmail via le serveur SMTP ($host)")
        } catch (e: Exception) {
            Log.e(TAG, "Erreur SMTP: ${e.message}", e)
            _lastDispatchStatus.value = "Erreur SMTP: ${e.message}"
            Result.failure(e)
        } finally {
            try { socket?.close() } catch (_: Exception) {}
        }
    }

    private suspend fun sendViaBrevoApi(toEmail: String, subject: String, htmlContent: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("sender", JSONObject().apply {
                    put("name", senderName)
                    put("email", smtpFrom)
                })
                put("to", JSONArray().apply {
                    put(JSONObject().apply { put("email", toEmail) })
                })
                put("subject", subject)
                put("htmlContent", htmlContent)
            }

            val body = payload.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("https://api.brevo.com/v3/smtp/email")
                .addHeader("api-key", apiKey)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success("E-mail envoyé avec succès à $toEmail via Brevo API")
                } else {
                    val err = response.body?.string() ?: ""
                    Result.failure(Exception("Erreur API Brevo (${response.code}): $err"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun sendViaResendApi(toEmail: String, subject: String, htmlContent: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject().apply {
                put("from", "$senderName <$smtpFrom>")
                put("to", JSONArray().apply { put(toEmail) })
                put("subject", subject)
                put("html", htmlContent)
            }

            val body = payload.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("https://api.resend.com/emails")
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success("E-mail envoyé avec succès à $toEmail via Resend API")
                } else {
                    val err = response.body?.string() ?: ""
                    Result.failure(Exception("Erreur API Resend (${response.code}): $err"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildHtmlResetEmail(toEmail: String, code: String, expirationMinutes: Int): String {
        return """
            <!DOCTYPE html>
            <html lang="fr">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <title>Code de sécurité</title>
            </head>
            <body style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #f8fafc; margin: 0; padding: 24px; color: #1e293b;">
              <table width="100%" border="0" cellspacing="0" cellpadding="0" style="max-width: 580px; margin: 0 auto; background-color: #ffffff; border-radius: 16px; overflow: hidden; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);">
                <tr>
                  <td style="background-color: #4f46e5; padding: 28px 24px; text-align: center;">
                    <h1 style="color: #ffffff; margin: 0; font-size: 22px; font-weight: 700;">Portail RH & Congés Entreprise</h1>
                    <p style="color: #e0e7ff; margin: 6px 0 0 0; font-size: 14px;">Sécurité & Gestion des Accès Collaborateurs</p>
                  </td>
                </tr>
                <tr>
                  <td style="padding: 32px 28px;">
                    <p style="font-size: 16px; margin: 0 0 16px 0;">Bonjour,</p>
                    <p style="font-size: 15px; line-height: 1.6; color: #475569; margin: 0 0 24px 0;">
                      Une demande de réinitialisation de mot de passe a été initiée pour le compte associé à l'adresse <strong>$toEmail</strong>.
                    </p>
                    <div style="background-color: #eef2ff; border: 2px dashed #6366f1; border-radius: 12px; padding: 20px; text-align: center; margin: 24px 0;">
                      <p style="font-size: 13px; font-weight: 600; text-transform: uppercase; color: #4f46e5; letter-spacing: 1px; margin: 0 0 8px 0;">Votre code de sécurité temporaire</p>
                      <div style="font-size: 36px; font-weight: 800; letter-spacing: 8px; color: #312e81; font-family: monospace;">$code</div>
                      <p style="font-size: 12px; color: #64748b; margin: 8px 0 0 0;">Valable pendant <strong>$expirationMinutes minutes</strong> uniquement</p>
                    </div>
                    <p style="font-size: 14px; line-height: 1.6; color: #475569;">
                      Saisissez ce code à 6 chiffres dans l'écran <em>"Étape 2 : Nouveau mot de passe"</em> de votre application pour définir votre nouveau mot de passe.
                    </p>
                    <hr style="border: 0; border-top: 1px solid #e2e8f0; margin: 28px 0;" />
                    <p style="font-size: 12px; color: #94a3b8; line-height: 1.5; margin: 0;">
                      🔒 <strong>Sécurité :</strong> Si vous n'êtes pas à l'origine de cette demande, vous pouvez ignorer cet e-mail. Votre mot de passe actuel reste actif et inchangé.
                    </p>
                  </td>
                </tr>
                <tr>
                  <td style="background-color: #f1f5f9; padding: 16px; text-align: center; font-size: 12px; color: #64748b;">
                    © ${java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)} Direction des Ressources Humaines & SI
                  </td>
                </tr>
              </table>
            </body>
            </html>
        """.trimIndent()
    }
}
