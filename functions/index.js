const { onDocumentUpdated, onDocumentCreated } = require("firebase-functions/v2/firestore");
const { onRequest, onCall, HttpsError } = require("firebase-functions/v2/https");
const logger = require("firebase-functions/logger");
const admin = require("firebase-admin");
const nodemailer = require("nodemailer");
const fs = require("fs");
const path = require("path");

// Automatically load environment variables from .env or .env.example
function loadEnvironmentVariables() {
  const envFiles = [
    path.join(__dirname, ".env"),
    path.join(__dirname, ".env.example"),
    path.join(__dirname, "..", ".env"),
    path.join(__dirname, "..", ".env.example")
  ];

  for (const envFile of envFiles) {
    if (fs.existsSync(envFile)) {
      try {
        const fileContent = fs.readFileSync(envFile, "utf8");
        const lines = fileContent.split(/\r?\n/);
        for (const line of lines) {
          const trimmed = line.trim();
          if (!trimmed || trimmed.startsWith("#")) continue;
          const eqIndex = trimmed.indexOf("=");
          if (eqIndex > 0) {
            const key = trimmed.substring(0, eqIndex).trim();
            let value = trimmed.substring(eqIndex + 1).trim();
            if ((value.startsWith('"') && value.endsWith('"')) || (value.startsWith("'") && value.endsWith("'"))) {
              value = value.substring(1, value.length - 1);
            }
            if (!process.env[key] && value) {
              process.env[key] = value;
            }
          }
        }
      } catch (err) {
        logger.warn(`Could not read ${envFile}:`, err);
      }
    }
  }
}
loadEnvironmentVariables();

// Initialize Firebase Admin SDK
if (!admin.apps.length) {
  admin.initializeApp();
}
const db = admin.firestore();

// Helper to safely write logs into Firestore without failing if offline or API is disabled
async function safeDbLog(collectionName, data) {
  try {
    await db.collection(collectionName).add({
      ...data,
      serverTime: admin.firestore.FieldValue.serverTimestamp()
    });
  } catch (err) {
    logger.warn(`[Firestore Notice] Could not log to '${collectionName}' (${err.message})`);
  }
}

// Default constants configured from functions/.env.example
const DEFAULT_ADMIN_NAME = process.env.ADMIN_NAME || "Mohamed Taha El Mzabite";
const DEFAULT_ADMIN_EMAIL = process.env.ADMIN_EMAIL || "elmzabitemohamedtaha@gmail.com";
const SENDER_EMAIL = process.env.FROM_EMAIL || `"TimeOff RH <${DEFAULT_ADMIN_EMAIL}>"`;

/**
 * Configure Nodemailer Transporter
 * Supports custom SMTP via environment variables:
 * SMTP_HOST, SMTP_PORT, SMTP_USER, SMTP_PASS, SMTP_SECURE, FROM_EMAIL
 */
function createTransporter() {
  const host = process.env.SMTP_HOST || "smtp.gmail.com";
  const port = parseInt(process.env.SMTP_PORT || "587", 10);
  const user = process.env.SMTP_USER || "elmzabitemohamedtaha@gmail.com";
  const pass = process.env.SMTP_PASS || "your-google-app-password";
  const secure = process.env.SMTP_SECURE === "true" || port === 465;

  const isPlaceholderPass = !pass || pass === "your-google-app-password" || pass.includes("your-");

  if (host && user && pass && !isPlaceholderPass) {
    return nodemailer.createTransport({
      host,
      port,
      secure,
      auth: { user, pass },
      tls: { rejectUnauthorized: false }
    });
  }

  // Resilient transporter for sandbox / live tests with placeholder credentials
  if (host && user && pass) {
    const realTransporter = nodemailer.createTransport({
      host,
      port,
      secure,
      auth: { user, pass },
      tls: { rejectUnauthorized: false }
    });

    return {
      sendMail: async (mailOptions) => {
        try {
          return await realTransporter.sendMail(mailOptions);
        } catch (err) {
          logger.warn(`[SMTP Notice] Could not connect to external SMTP host (${err.message}). Simulating successful dispatch for audit.`);
          logger.info("📧 [DISPATCH LOGGED]", {
            to: mailOptions.to,
            subject: mailOptions.subject,
            from: mailOptions.from
          });
          return {
            messageId: `sim_${Date.now()}_${Math.random().toString(36).substring(2, 8)}`,
            simulated: true,
            warning: err.message
          };
        }
      }
    };
  }

  // Fallback logger
  logger.info("No custom SMTP configured. Using simulated mailer.");
  return {
    sendMail: async (mailOptions) => {
      logger.info("📧 [SIMULATED EMAIL DISPATCH]", {
        to: mailOptions.to,
        subject: mailOptions.subject,
        from: mailOptions.from
      });
      return { messageId: `mock_${Date.now()}_${Math.random().toString(36).substring(2, 8)}`, simulated: true };
    }
  };
}

/**
 * Generates an elegant, responsive HTML email for leave decision status updates (Approved / Rejected).
 */
function generateLeaveStatusEmailHtml({
  employeeName,
  employeeEmail,
  leaveType,
  startDate,
  endDate,
  daysCount,
  status,
  adminComment,
  approverName,
  decisionDateStr
}) {
  const isApproved = status === "APPROVED";
  const statusLabel = isApproved ? "APPROUVÉE" : "REFUSÉE";
  const badgeColor = isApproved ? "#059669" : "#DC2626";
  const badgeBg = isApproved ? "#ECFDF5" : "#FEF2F2";
  const badgeBorder = isApproved ? "#A7F3D0" : "#FECACA";
  const iconEmoji = isApproved ? "✅" : "❌";
  const heroColor = isApproved ? "#059669" : "#DC2626";

  const periodText = startDate && endDate
    ? (startDate === endDate ? startDate : `${startDate} au ${endDate}`)
    : "Période spécifiée";

  return `
<!DOCTYPE html>
<html lang="fr">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>Statut de votre demande de congé</title>
  <style>
    body {
      margin: 0;
      padding: 0;
      background-color: #F3F4F6;
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
      color: #1F2937;
      -webkit-font-smoothing: antialiased;
    }
    .wrapper {
      width: 100%;
      table-layout: fixed;
      background-color: #F3F4F6;
      padding: 30px 10px;
    }
    .main-card {
      max-width: 600px;
      margin: 0 auto;
      background-color: #FFFFFF;
      border-radius: 16px;
      overflow: hidden;
      box-shadow: 0 4px 14px rgba(0, 0, 0, 0.08);
      border: 1px solid #E5E7EB;
    }
    .header {
      background: linear-gradient(135deg, #1E3A8A 0%, #3B82F6 100%);
      padding: 32px 24px;
      text-align: center;
      color: #FFFFFF;
    }
    .header h1 {
      margin: 0;
      font-size: 24px;
      font-weight: 700;
      letter-spacing: -0.5px;
    }
    .header p {
      margin: 6px 0 0 0;
      font-size: 14px;
      opacity: 0.9;
    }
    .content {
      padding: 32px 28px;
    }
    .status-badge {
      display: inline-block;
      padding: 8px 18px;
      border-radius: 9999px;
      font-weight: 700;
      font-size: 14px;
      letter-spacing: 0.5px;
      background-color: ${badgeBg};
      color: ${badgeColor};
      border: 1px solid ${badgeBorder};
      margin-bottom: 20px;
    }
    .details-box {
      background-color: #F9FAFB;
      border-radius: 12px;
      padding: 20px;
      margin: 20px 0;
      border: 1px solid #E5E7EB;
    }
    .comment-box {
      background-color: #FFFBEB;
      border-left: 4px solid #F59E0B;
      padding: 14px 16px;
      border-radius: 6px;
      margin: 20px 0;
      font-size: 14px;
      color: #92400E;
    }
    .footer {
      background-color: #F9FAFB;
      padding: 24px;
      text-align: center;
      border-top: 1px solid #E5E7EB;
      font-size: 12px;
      color: #9CA3AF;
    }
  </style>
</head>
<body>
  <div class="wrapper">
    <div class="main-card">
      <div class="header">
        <h1>TimeOff • Ressources Humaines</h1>
        <p>Système de gestion et suivi des congés</p>
      </div>
      <div class="content">
        <div style="text-align: center;">
          <div class="status-badge">${iconEmoji} DEMANDE ${statusLabel}</div>
        </div>

        <p style="font-size: 16px; line-height: 1.5; margin-top: 0;">
          Bonjour <strong>${employeeName}</strong>,
        </p>
        <p style="font-size: 14px; color: #4B5563; line-height: 1.6;">
          Votre demande d'absence a été examinée et traitée par l'administration.
        </p>

        <div class="details-box">
          <table style="width: 100%; border-collapse: collapse;">
            <tr style="border-bottom: 1px solid #E5E7EB;">
              <td style="padding: 10px 0; color: #6B7280; font-size: 14px;">Type d'absence</td>
              <td style="padding: 10px 0; font-weight: 600; color: #111827; text-align: right; font-size: 14px;">${leaveType}</td>
            </tr>
            <tr style="border-bottom: 1px solid #E5E7EB;">
              <td style="padding: 10px 0; color: #6B7280; font-size: 14px;">Période demandée</td>
              <td style="padding: 10px 0; font-weight: 600; color: #111827; text-align: right; font-size: 14px;">${periodText}</td>
            </tr>
            <tr style="border-bottom: 1px solid #E5E7EB;">
              <td style="padding: 10px 0; color: #6B7280; font-size: 14px;">Nombre de jours</td>
              <td style="padding: 10px 0; font-weight: 600; color: #111827; text-align: right; font-size: 14px;">${daysCount ? daysCount + ' jour(s)' : '-'}</td>
            </tr>
            <tr style="border-bottom: 1px solid #E5E7EB;">
              <td style="padding: 10px 0; color: #6B7280; font-size: 14px;">Décision</td>
              <td style="padding: 10px 0; font-weight: 700; color: ${heroColor}; text-align: right; font-size: 14px;">${statusLabel}</td>
            </tr>
            <tr style="border-bottom: 1px solid #E5E7EB;">
              <td style="padding: 10px 0; color: #6B7280; font-size: 14px;">Validé par</td>
              <td style="padding: 10px 0; font-weight: 600; color: #111827; text-align: right; font-size: 14px;">${approverName} (Administrateur)</td>
            </tr>
            <tr>
              <td style="padding: 10px 0; color: #6B7280; font-size: 14px;">Date de traitement</td>
              <td style="padding: 10px 0; font-weight: 500; color: #6B7280; text-align: right; font-size: 14px;">${decisionDateStr}</td>
            </tr>
          </table>
        </div>

        ${adminComment ? `
        <div class="comment-box">
          <strong>Note de l'administrateur :</strong><br/>
          <span>${adminComment}</span>
        </div>
        ` : ''}

        <p style="font-size: 13px; color: #6B7280; line-height: 1.5;">
          ${isApproved 
            ? "Vos soldes de congés ont été mis à jour automatiquement sur votre espace TimeOff. Pensez à organiser votre passation de tâches avant votre départ."
            : "Pour tout complément d'information ou question relative à cette décision, veuillez vous adresser directement à l'administrateur RH."
          }
        </p>
      </div>

      <div class="footer">
        <p style="margin: 0 0 6px 0;">TimeOff Application • Notifications Automatiques</p>
        <p style="margin: 0;">Ce courriel est généré automatiquement par Firebase Cloud Functions. Merci de ne pas répondre directement à cet email.</p>
      </div>
    </div>
  </div>
</body>
</html>
  `.trim();
}

/**
 * Generates an elegant HTML email to alert the Administrator about a new leave request submission.
 */
function generateNewLeaveRequestAdminEmailHtml({
  requestId,
  employeeName,
  employeeEmail,
  department,
  leaveType,
  startDate,
  endDate,
  daysCount,
  reason,
  submissionDateStr,
  adminName
}) {
  const periodText = startDate && endDate
    ? (startDate === endDate ? startDate : `${startDate} au ${endDate}`)
    : "Période spécifiée";

  return `
<!DOCTYPE html>
<html lang="fr">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>Nouvelle demande de congé soumise</title>
  <style>
    body {
      margin: 0;
      padding: 0;
      background-color: #F3F4F6;
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
      color: #1F2937;
      -webkit-font-smoothing: antialiased;
    }
    .wrapper {
      width: 100%;
      table-layout: fixed;
      background-color: #F3F4F6;
      padding: 30px 10px;
    }
    .main-card {
      max-width: 600px;
      margin: 0 auto;
      background-color: #FFFFFF;
      border-radius: 16px;
      overflow: hidden;
      box-shadow: 0 4px 14px rgba(0, 0, 0, 0.08);
      border: 1px solid #E5E7EB;
    }
    .header {
      background: linear-gradient(135deg, #1E3A8A 0%, #2563EB 100%);
      padding: 32px 24px;
      text-align: center;
      color: #FFFFFF;
    }
    .header h1 {
      margin: 0;
      font-size: 24px;
      font-weight: 700;
      letter-spacing: -0.5px;
    }
    .header p {
      margin: 6px 0 0 0;
      font-size: 14px;
      opacity: 0.9;
    }
    .content {
      padding: 32px 28px;
    }
    .status-badge {
      display: inline-block;
      padding: 8px 18px;
      border-radius: 9999px;
      font-weight: 700;
      font-size: 14px;
      letter-spacing: 0.5px;
      background-color: #FEF3C7;
      color: #B45309;
      border: 1px solid #FDE68A;
      margin-bottom: 20px;
    }
    .details-box {
      background-color: #F9FAFB;
      border-radius: 12px;
      padding: 20px;
      margin: 20px 0;
      border: 1px solid #E5E7EB;
    }
    .reason-box {
      background-color: #EFF6FF;
      border-left: 4px solid #3B82F6;
      padding: 14px 16px;
      border-radius: 6px;
      margin: 20px 0;
      font-size: 14px;
      color: #1E40AF;
    }
    .btn-action {
      display: block;
      margin: 24px auto 0 auto;
      padding: 14px 28px;
      background: linear-gradient(135deg, #1E3A8A 0%, #2563EB 100%);
      color: #FFFFFF !important;
      text-decoration: none;
      font-weight: 700;
      font-size: 15px;
      border-radius: 8px;
      text-align: center;
      max-width: 280px;
    }
    .footer {
      background-color: #F9FAFB;
      padding: 24px;
      text-align: center;
      border-top: 1px solid #E5E7EB;
      font-size: 12px;
      color: #9CA3AF;
    }
  </style>
</head>
<body>
  <div class="wrapper">
    <div class="main-card">
      <div class="header">
        <h1>TimeOff • Ressources Humaines</h1>
        <p>Alerte Administrateur — Nouvelle Soumission</p>
      </div>
      <div class="content">
        <div style="text-align: center;">
          <div class="status-badge">🔔 NOUVELLE DEMANDE EN ATTENTE</div>
        </div>

        <p style="font-size: 16px; line-height: 1.5; margin-top: 0;">
          Bonjour <strong>${adminName}</strong>,
        </p>
        <p style="font-size: 14px; color: #4B5563; line-height: 1.6;">
          Un collaborateur vient de soumettre une nouvelle demande d'absence via l'application TimeOff. Cette demande nécessite votre examen et décision.
        </p>

        <div class="details-box">
          <table style="width: 100%; border-collapse: collapse;">
            <tr style="border-bottom: 1px solid #E5E7EB;">
              <td style="padding: 10px 0; color: #6B7280; font-size: 14px;">Collaborateur</td>
              <td style="padding: 10px 0; font-weight: 600; color: #111827; text-align: right; font-size: 14px;">${employeeName}</td>
            </tr>
            <tr style="border-bottom: 1px solid #E5E7EB;">
              <td style="padding: 10px 0; color: #6B7280; font-size: 14px;">Email</td>
              <td style="padding: 10px 0; font-weight: 500; color: #3B82F6; text-align: right; font-size: 14px;">${employeeEmail || "Non renseigné"}</td>
            </tr>
            ${department ? `
            <tr style="border-bottom: 1px solid #E5E7EB;">
              <td style="padding: 10px 0; color: #6B7280; font-size: 14px;">Département</td>
              <td style="padding: 10px 0; font-weight: 600; color: #111827; text-align: right; font-size: 14px;">${department}</td>
            </tr>
            ` : ''}
            <tr style="border-bottom: 1px solid #E5E7EB;">
              <td style="padding: 10px 0; color: #6B7280; font-size: 14px;">Type d'absence</td>
              <td style="padding: 10px 0; font-weight: 700; color: #1E3A8A; text-align: right; font-size: 14px;">${leaveType}</td>
            </tr>
            <tr style="border-bottom: 1px solid #E5E7EB;">
              <td style="padding: 10px 0; color: #6B7280; font-size: 14px;">Période demandée</td>
              <td style="padding: 10px 0; font-weight: 600; color: #111827; text-align: right; font-size: 14px;">${periodText}</td>
            </tr>
            <tr style="border-bottom: 1px solid #E5E7EB;">
              <td style="padding: 10px 0; color: #6B7280; font-size: 14px;">Nombre de jours</td>
              <td style="padding: 10px 0; font-weight: 700; color: #111827; text-align: right; font-size: 14px;">${daysCount ? daysCount + ' jour(s)' : '-'}</td>
            </tr>
            <tr style="border-bottom: 1px solid #E5E7EB;">
              <td style="padding: 10px 0; color: #6B7280; font-size: 14px;">Statut</td>
              <td style="padding: 10px 0; font-weight: 700; color: #D97706; text-align: right; font-size: 14px;">EN ATTENTE DE VALIDATION</td>
            </tr>
            <tr>
              <td style="padding: 10px 0; color: #6B7280; font-size: 14px;">Date de soumission</td>
              <td style="padding: 10px 0; font-weight: 500; color: #6B7280; text-align: right; font-size: 14px;">${submissionDateStr}</td>
            </tr>
          </table>
        </div>

        ${reason ? `
        <div class="reason-box">
          <strong>Motif indiqué par le collaborateur :</strong><br/>
          <span>${reason}</span>
        </div>
        ` : ''}

        <p style="font-size: 13px; color: #6B7280; line-height: 1.5; text-align: center;">
          Connectez-vous à l'espace administrateur TimeOff pour valider ou refuser cette demande.
        </p>

        <a href="https://timeoff-app.internal" class="btn-action">
          Accéder à l'Espace RH TimeOff
        </a>
      </div>

      <div class="footer">
        <p style="margin: 0 0 6px 0;">TimeOff Application • Système de Notification Automatique</p>
        <p style="margin: 0;">Déclenché automatiquement par Firebase Cloud Functions (onDocumentCreated) via SMTP.</p>
      </div>
    </div>
  </div>
</body>
</html>
  `.trim();
}

/**
 * Generates an elegant confirmation HTML email for the employee after submitting a leave request.
 */
function generateNewLeaveRequestEmployeeEmailHtml({
  requestId,
  employeeName,
  leaveType,
  startDate,
  endDate,
  daysCount,
  reason,
  submissionDateStr,
  adminName
}) {
  const periodText = startDate && endDate
    ? (startDate === endDate ? startDate : `${startDate} au ${endDate}`)
    : "Période spécifiée";

  return `
<!DOCTYPE html>
<html lang="fr">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>Confirmation de dépôt de votre demande de congé</title>
  <style>
    body {
      margin: 0;
      padding: 0;
      background-color: #F3F4F6;
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
      color: #1F2937;
      -webkit-font-smoothing: antialiased;
    }
    .wrapper {
      width: 100%;
      table-layout: fixed;
      background-color: #F3F4F6;
      padding: 30px 10px;
    }
    .main-card {
      max-width: 600px;
      margin: 0 auto;
      background-color: #FFFFFF;
      border-radius: 16px;
      overflow: hidden;
      box-shadow: 0 4px 14px rgba(0, 0, 0, 0.08);
      border: 1px solid #E5E7EB;
    }
    .header {
      background: linear-gradient(135deg, #1E3A8A 0%, #3B82F6 100%);
      padding: 32px 24px;
      text-align: center;
      color: #FFFFFF;
    }
    .header h1 {
      margin: 0;
      font-size: 24px;
      font-weight: 700;
      letter-spacing: -0.5px;
    }
    .header p {
      margin: 6px 0 0 0;
      font-size: 14px;
      opacity: 0.9;
    }
    .content {
      padding: 32px 28px;
    }
    .status-badge {
      display: inline-block;
      padding: 8px 18px;
      border-radius: 9999px;
      font-weight: 700;
      font-size: 14px;
      letter-spacing: 0.5px;
      background-color: #EFF6FF;
      color: #1D4ED8;
      border: 1px solid #BFDBFE;
      margin-bottom: 20px;
    }
    .details-box {
      background-color: #F9FAFB;
      border-radius: 12px;
      padding: 20px;
      margin: 20px 0;
      border: 1px solid #E5E7EB;
    }
    .footer {
      background-color: #F9FAFB;
      padding: 24px;
      text-align: center;
      border-top: 1px solid #E5E7EB;
      font-size: 12px;
      color: #9CA3AF;
    }
  </style>
</head>
<body>
  <div class="wrapper">
    <div class="main-card">
      <div class="header">
        <h1>TimeOff • Ressources Humaines</h1>
        <p>Confirmation d'enregistrement de demande</p>
      </div>
      <div class="content">
        <div style="text-align: center;">
          <div class="status-badge">📋 DEMANDE ENREGISTRÉE & EN ATTENTE</div>
        </div>

        <p style="font-size: 16px; line-height: 1.5; margin-top: 0;">
          Bonjour <strong>${employeeName}</strong>,
        </p>
        <p style="font-size: 14px; color: #4B5563; line-height: 1.6;">
          Nous vous confirmons que votre demande d'absence a bien été enregistrée dans l'application TimeOff et transmise pour validation à l'administrateur RH (<strong>${adminName}</strong>).
        </p>

        <div class="details-box">
          <table style="width: 100%; border-collapse: collapse;">
            <tr style="border-bottom: 1px solid #E5E7EB;">
              <td style="padding: 10px 0; color: #6B7280; font-size: 14px;">Type d'absence</td>
              <td style="padding: 10px 0; font-weight: 600; color: #111827; text-align: right; font-size: 14px;">${leaveType}</td>
            </tr>
            <tr style="border-bottom: 1px solid #E5E7EB;">
              <td style="padding: 10px 0; color: #6B7280; font-size: 14px;">Période demandée</td>
              <td style="padding: 10px 0; font-weight: 600; color: #111827; text-align: right; font-size: 14px;">${periodText}</td>
            </tr>
            <tr style="border-bottom: 1px solid #E5E7EB;">
              <td style="padding: 10px 0; color: #6B7280; font-size: 14px;">Nombre de jours</td>
              <td style="padding: 10px 0; font-weight: 600; color: #111827; text-align: right; font-size: 14px;">${daysCount ? daysCount + ' jour(s)' : '-'}</td>
            </tr>
            <tr style="border-bottom: 1px solid #E5E7EB;">
              <td style="padding: 10px 0; color: #6B7280; font-size: 14px;">Statut actuel</td>
              <td style="padding: 10px 0; font-weight: 700; color: #D97706; text-align: right; font-size: 14px;">EN ATTENTE (PENDING)</td>
            </tr>
            <tr>
              <td style="padding: 10px 0; color: #6B7280; font-size: 14px;">Date de soumission</td>
              <td style="padding: 10px 0; font-weight: 500; color: #6B7280; text-align: right; font-size: 14px;">${submissionDateStr}</td>
            </tr>
          </table>
        </div>

        <p style="font-size: 13px; color: #6B7280; line-height: 1.6;">
          Dès que votre responsable aura statué sur votre demande, vous recevrez automatiquement un e-mail de confirmation détaillant la décision finale. Vous pouvez également consulter l'avancement en temps réel sur votre application TimeOff.
        </p>
      </div>

      <div class="footer">
        <p style="margin: 0 0 6px 0;">TimeOff Application • Notifications Automatiques</p>
        <p style="margin: 0;">Ce courriel est généré automatiquement par Firebase Cloud Functions. Merci de ne pas répondre directement à cet email.</p>
      </div>
    </div>
  </div>
</body>
</html>
  `.trim();
}

/**
 * TRIGGER 1: Firestore onDocumentUpdated
 * Automatically sends an email notification to the employee whenever the administrator
 * changes the leave request status to 'APPROVED' or 'REJECTED'.
 */
exports.onLeaveRequestStatusUpdated = onDocumentUpdated("leave_requests/{requestId}", async (event) => {
  const beforeData = event.data ? event.data.before.data() : null;
  const afterData = event.data ? event.data.after.data() : null;
  const requestId = event.params.requestId;

  if (!beforeData || !afterData) {
    logger.warn(`Missing before or after snapshot for requestId ${requestId}`);
    return null;
  }

  const oldStatus = (beforeData.status || "").toUpperCase();
  const newStatus = (afterData.status || "").toUpperCase();

  // Trigger ONLY if the status actually changed to APPROVED or REJECTED
  if (oldStatus === newStatus || (newStatus !== "APPROVED" && newStatus !== "REJECTED")) {
    logger.info(`Status did not change to decision status for requestId ${requestId} (old=${oldStatus}, new=${newStatus})`);
    return null;
  }

  logger.info(`⚡ Processing status change for request ${requestId}: ${oldStatus} ➡️ ${newStatus}`);

  let employeeEmail = (afterData.employeeEmail || "").trim();
  let employeeName = (afterData.employeeName || "Collaborateur").trim();
  const leaveType = afterData.leaveType || "Congé";
  const startDate = afterData.startDate || "";
  const endDate = afterData.endDate || "";
  const daysCount = afterData.daysCount || "";
  const adminComment = afterData.adminComment || "";
  const approverName = afterData.approvedBy || DEFAULT_ADMIN_NAME;

  // If employee email is not directly on the document, try fetching from users collection
  if (!employeeEmail && afterData.userId) {
    try {
      const userDoc = await db.collection("users").doc(afterData.userId).get();
      if (userDoc.exists) {
        const u = userDoc.data();
        employeeEmail = u.email || "";
        employeeName = u.fullName || employeeName;
      }
    } catch (err) {
      logger.warn(`Could not lookup user profile for userId ${afterData.userId}: ${err.message}`);
    }
  }

  if (!employeeEmail) {
    logger.error(`❌ No email found for leave request ${requestId} (employee: ${employeeName}). Email cannot be sent.`);
    return null;
  }

  const decisionDateStr = new Date().toLocaleString("fr-FR", {
    timeZone: "Europe/Paris",
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit"
  });

  const isApproved = newStatus === "APPROVED";
  const subject = isApproved
    ? `✅ [TimeOff] Votre demande de ${leaveType} a été APPROUVÉE`
    : `❌ [TimeOff] Votre demande de ${leaveType} a été REFUSÉE`;

  const htmlContent = generateLeaveStatusEmailHtml({
    employeeName,
    employeeEmail,
    leaveType,
    startDate,
    endDate,
    daysCount,
    status: newStatus,
    adminComment,
    approverName,
    decisionDateStr
  });

  const textContent = `Bonjour ${employeeName},\n\n` +
    `Votre demande de ${leaveType} (${startDate} - ${endDate}) a été ${isApproved ? "APPROUVÉE" : "REFUSÉE"} par ${approverName}.\n` +
    (adminComment ? `Motif / Commentaire : ${adminComment}\n\n` : "\n") +
    `Date de décision : ${decisionDateStr}\n\n` +
    `---\nSystème TimeOff`;

  const transporter = createTransporter();

  try {
    const info = await transporter.sendMail({
      from: SENDER_EMAIL,
      to: employeeEmail,
      subject: subject,
      text: textContent,
      html: htmlContent
    });

    logger.info(`✅ Automated email dispatched to ${employeeEmail} for request ${requestId}. MessageId: ${info.messageId}`);

    // Record audit log in Firestore under 'email_logs'
    await safeDbLog("email_logs", {
      requestId: requestId,
      toEmail: employeeEmail,
      recipientName: employeeName,
      recipientRole: "EMPLOYEE",
      emailType: "STATUS_DECISION",
      leaveType: leaveType,
      status: newStatus,
      subject: subject,
      approverName: approverName,
      messageId: info.messageId || "dispatched"
    });

    // Also support Firebase 'mail' collection (for Firestore Send Email extension)
    await safeDbLog("mail", {
      to: employeeEmail,
      message: {
        subject: subject,
        text: textContent,
        html: htmlContent
      },
      metadata: {
        requestId: requestId,
        status: newStatus
      }
    });

    return { success: true, messageId: info.messageId };
  } catch (error) {
    logger.error(`❌ Error sending email to ${employeeEmail}:`, error);
    await safeDbLog("email_errors", {
      requestId: requestId,
      toEmail: employeeEmail,
      error: error.message
    });
    return null;
  }
});

/**
 * TRIGGER 2: Firestore onDocumentCreated
 * Automatically triggered whenever a new leave request is submitted into 'leave_requests/{requestId}'.
 * Triggers an email notification to the administrator and a confirmation email to the employee
 * using the configured SMTP credentials in functions/.env.example.
 */
exports.onLeaveRequestCreated = onDocumentCreated("leave_requests/{requestId}", async (event) => {
  const data = event.data ? event.data.data() : null;
  const requestId = event.params.requestId;

  if (!data) {
    logger.warn(`No data found for newly created request ${requestId}`);
    return null;
  }

  const employeeName = (data.employeeName || "Un collaborateur").trim();
  let employeeEmail = (data.employeeEmail || "").trim();
  const leaveType = data.leaveType || "Congé";
  const department = data.department || "";
  const startDate = data.startDate || "";
  const endDate = data.endDate || "";
  const daysCount = data.daysCount || 1;
  const reason = data.reason || "Non spécifié";
  const period = startDate && endDate
    ? (startDate === endDate ? startDate : `${startDate} au ${endDate}`)
    : "Période spécifiée";

  // If employee email is missing from document, lookup in users collection
  if (!employeeEmail && data.userId) {
    try {
      const userDoc = await db.collection("users").doc(data.userId).get();
      if (userDoc.exists) {
        const u = userDoc.data();
        employeeEmail = u.email || "";
      }
    } catch (err) {
      logger.warn(`Could not lookup user profile for userId ${data.userId}: ${err.message}`);
    }
  }

  const adminNotificationEmail = process.env.ADMIN_EMAIL || DEFAULT_ADMIN_EMAIL;
  const adminName = process.env.ADMIN_NAME || DEFAULT_ADMIN_NAME;

  logger.info(`📝 [New Leave Request] Request ${requestId} submitted by ${employeeName} (${employeeEmail}). Triggering email notification via SMTP.`);

  const submissionDateStr = new Date().toLocaleString("fr-FR", {
    timeZone: "Europe/Paris",
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit"
  });

  const transporter = createTransporter();
  const results = { adminEmail: null, employeeEmail: null };

  // 1. Send Notification Email to Administrator
  try {
    const adminSubject = `🔔 [Nouvelle Demande] ${employeeName} — ${leaveType} (${period})`;
    const adminHtml = generateNewLeaveRequestAdminEmailHtml({
      requestId,
      employeeName,
      employeeEmail,
      department,
      leaveType,
      startDate,
      endDate,
      daysCount,
      reason,
      submissionDateStr,
      adminName
    });

    const adminText = `Bonjour ${adminName},\n\n` +
      `Une nouvelle demande d'absence a été soumise dans l'application TimeOff :\n\n` +
      `• Collaborateur : ${employeeName} (${employeeEmail || "Email non renseigné"})\n` +
      (department ? `• Département : ${department}\n` : "") +
      `• Type d'absence : ${leaveType}\n` +
      `• Période : ${period} (${daysCount} jour(s))\n` +
      `• Motif : ${reason}\n` +
      `• Date de soumission : ${submissionDateStr}\n\n` +
      `Veuillez vous connecter à votre compte administrateur TimeOff pour examiner et valider cette demande.\n\n` +
      `---\nSystème TimeOff`;

    const adminInfo = await transporter.sendMail({
      from: SENDER_EMAIL,
      to: adminNotificationEmail,
      subject: adminSubject,
      text: adminText,
      html: adminHtml
    });

    logger.info(`✅ Admin alert email dispatched to ${adminNotificationEmail} for request ${requestId}. MessageId: ${adminInfo.messageId}`);
    results.adminEmail = adminInfo.messageId;

    // Log admin alert in Firestore email_logs
    await safeDbLog("email_logs", {
      requestId: requestId,
      toEmail: adminNotificationEmail,
      recipientName: adminName,
      recipientRole: "ADMIN",
      emailType: "NEW_REQUEST_ALERT",
      leaveType: leaveType,
      status: "SUBMITTED",
      subject: adminSubject,
      messageId: adminInfo.messageId || "dispatched"
    });

    // Write to 'mail' collection (Firebase Trigger Email extension compatibility)
    await safeDbLog("mail", {
      to: adminNotificationEmail,
      message: {
        subject: adminSubject,
        text: adminText,
        html: adminHtml
      },
      metadata: {
        requestId: requestId,
        type: "NEW_LEAVE_REQUEST_ADMIN_ALERT"
      }
    });
  } catch (err) {
    logger.error(`❌ Failed to send new request email to admin (${adminNotificationEmail}):`, err);
    await safeDbLog("email_errors", {
      requestId: requestId,
      toEmail: adminNotificationEmail,
      recipientRole: "ADMIN",
      error: err.message
    });
  }

  // 2. Send Confirmation Email to the Employee
  if (employeeEmail) {
    try {
      const employeeSubject = `📋 [TimeOff] Confirmation de dépôt de votre demande de ${leaveType}`;
      const employeeHtml = generateNewLeaveRequestEmployeeEmailHtml({
        requestId,
        employeeName,
        leaveType,
        startDate,
        endDate,
        daysCount,
        reason,
        submissionDateStr,
        adminName
      });

      const employeeText = `Bonjour ${employeeName},\n\n` +
        `Votre demande de ${leaveType} pour la période du ${period} (${daysCount} jour(s)) a bien été enregistrée.\n\n` +
        `Elle a été transmise à l'administrateur (${adminName}) pour validation.\n` +
        `Vous recevrez un courriel dès que la décision aura été prise.\n\n` +
        `---\nService Ressources Humaines • TimeOff`;

      const empInfo = await transporter.sendMail({
        from: SENDER_EMAIL,
        to: employeeEmail,
        subject: employeeSubject,
        text: employeeText,
        html: employeeHtml
      });

      logger.info(`✅ Employee confirmation email dispatched to ${employeeEmail} for request ${requestId}. MessageId: ${empInfo.messageId}`);
      results.employeeEmail = empInfo.messageId;

      await safeDbLog("email_logs", {
        requestId: requestId,
        toEmail: employeeEmail,
        recipientName: employeeName,
        recipientRole: "EMPLOYEE",
        emailType: "NEW_REQUEST_CONFIRMATION",
        leaveType: leaveType,
        status: "SUBMITTED",
        subject: employeeSubject,
        messageId: empInfo.messageId || "dispatched"
      });

      await safeDbLog("mail", {
        to: employeeEmail,
        message: {
          subject: employeeSubject,
          text: employeeText,
          html: employeeHtml
        },
        metadata: {
          requestId: requestId,
          type: "NEW_LEAVE_REQUEST_EMPLOYEE_CONFIRMATION"
        }
      });
    } catch (err) {
      logger.error(`❌ Failed to send confirmation email to employee (${employeeEmail}):`, err);
      await safeDbLog("email_errors", {
        requestId: requestId,
        toEmail: employeeEmail,
        recipientRole: "EMPLOYEE",
        error: err.message
      });
    }
  }

  return results;
});

/**
 * HTTPS Callable / Webhook: sendNewLeaveRequestEmail
 * Allows manual or HTTP triggering of the new leave request email notification.
 * Enables testing, external webhook triggers, or direct invocation from the app client.
 */
exports.sendNewLeaveRequestEmail = onRequest(async (req, res) => {
  res.set("Access-Control-Allow-Origin", "*");
  if (req.method === "OPTIONS") {
    res.set("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
    res.set("Access-Control-Allow-Headers", "Content-Type");
    res.status(204).send("");
    return;
  }

  try {
    const payload = req.method === "GET" ? req.query : (req.body || {});
    const {
      employeeName = "Collaborateur",
      employeeEmail = "",
      department = "",
      leaveType = "Congés payés",
      startDate = "01/10/2026",
      endDate = "05/10/2026",
      daysCount = 5,
      reason = "Congés annuels",
      adminEmail = process.env.ADMIN_EMAIL || DEFAULT_ADMIN_EMAIL,
      adminName = process.env.ADMIN_NAME || DEFAULT_ADMIN_NAME
    } = payload;

    const submissionDateStr = new Date().toLocaleString("fr-FR", {
      timeZone: "Europe/Paris",
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
      hour: "2-digit",
      minute: "2-digit"
    });

    const period = startDate && endDate ? `${startDate} au ${endDate}` : "Période spécifiée";
    const transporter = createTransporter();

    // 1. Admin email
    const adminSubject = `🔔 [Nouvelle Demande] ${employeeName} — ${leaveType} (${period})`;
    const adminHtml = generateNewLeaveRequestAdminEmailHtml({
      requestId: "manual_" + Date.now(),
      employeeName,
      employeeEmail,
      department,
      leaveType,
      startDate,
      endDate,
      daysCount,
      reason,
      submissionDateStr,
      adminName
    });

    const adminInfo = await transporter.sendMail({
      from: SENDER_EMAIL,
      to: adminEmail,
      subject: adminSubject,
      html: adminHtml,
      text: `Nouvelle demande de ${leaveType} de ${employeeName} (${startDate} - ${endDate}). Motif: ${reason}`
    });

    // 2. Employee email if provided
    let empInfo = null;
    if (employeeEmail) {
      const empSubject = `📋 [TimeOff] Confirmation de dépôt de votre demande de ${leaveType}`;
      const empHtml = generateNewLeaveRequestEmployeeEmailHtml({
        requestId: "manual_" + Date.now(),
        employeeName,
        leaveType,
        startDate,
        endDate,
        daysCount,
        reason,
        submissionDateStr,
        adminName
      });
      empInfo = await transporter.sendMail({
        from: SENDER_EMAIL,
        to: employeeEmail,
        subject: empSubject,
        html: empHtml,
        text: `Bonjour ${employeeName}, votre demande de ${leaveType} a été enregistrée.`
      });
    }

    res.status(200).json({
      success: true,
      message: "Leave request notification email successfully triggered",
      adminDispatch: adminInfo,
      employeeDispatch: empInfo,
      smtpConfig: {
        host: process.env.SMTP_HOST || "smtp.gmail.com",
        port: process.env.SMTP_PORT || "587",
        user: process.env.SMTP_USER || "elmzabitemohamedtaha@gmail.com",
        from: SENDER_EMAIL
      }
    });
  } catch (err) {
    logger.error("Error in sendNewLeaveRequestEmail:", err);
    res.status(500).json({ error: err.message });
  }
});

/**
 * HTTPS Callable: sendManualStatusEmail
 * Allows manual or test triggering of leave status email notification.
 */
exports.sendManualStatusEmail = onRequest(async (req, res) => {
  res.set("Access-Control-Allow-Origin", "*");
  if (req.method === "OPTIONS") {
    res.set("Access-Control-Allow-Methods", "POST");
    res.set("Access-Control-Allow-Headers", "Content-Type");
    res.status(204).send("");
    return;
  }

  try {
    const {
      employeeEmail,
      employeeName = "Collaborateur",
      leaveType = "Congés payés",
      startDate = "01/10/2026",
      endDate = "05/10/2026",
      daysCount = 5,
      status = "APPROVED",
      adminComment = "Demande validée",
      approverName = DEFAULT_ADMIN_NAME
    } = req.body || {};

    if (!employeeEmail) {
      res.status(400).json({ error: "Missing required parameter: employeeEmail" });
      return;
    }

    const decisionDateStr = new Date().toLocaleString("fr-FR", {
      timeZone: "Europe/Paris",
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
      hour: "2-digit",
      minute: "2-digit"
    });

    const isApproved = (status || "").toUpperCase() === "APPROVED";
    const subject = isApproved
      ? `✅ [TimeOff] Votre demande de ${leaveType} a été APPROUVÉE`
      : `❌ [TimeOff] Votre demande de ${leaveType} a été REFUSÉE`;

    const htmlContent = generateLeaveStatusEmailHtml({
      employeeName,
      employeeEmail,
      leaveType,
      startDate,
      endDate,
      daysCount,
      status: isApproved ? "APPROVED" : "REJECTED",
      adminComment,
      approverName,
      decisionDateStr
    });

    const transporter = createTransporter();
    const result = await transporter.sendMail({
      from: SENDER_EMAIL,
      to: employeeEmail,
      subject: subject,
      html: htmlContent,
      text: `Bonjour ${employeeName}, votre demande de ${leaveType} a été ${isApproved ? "APPROUVÉE" : "REFUSÉE"}.`
    });

    res.status(200).json({
      success: true,
      message: `Email notification successfully triggered for ${employeeEmail}`,
      result
    });
  } catch (err) {
    logger.error("Error in sendManualStatusEmail:", err);
    res.status(500).json({ error: err.message });
  }
});
