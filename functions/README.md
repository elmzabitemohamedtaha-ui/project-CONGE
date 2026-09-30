# Firebase Cloud Functions — TimeOff Notifications Automatiques

Ce module implémente les **Firebase Cloud Functions** pour l'application **TimeOff**, assurant l'envoi automatisé d'e-mails professionnels par protocole SMTP aux collaborateurs et à l'administrateur.

---

## ⚡ Fonctions Implémentées

### 1. `onLeaveRequestCreated` (Déclencheur Firestore `onDocumentCreated`)
- **Déclencheur** : `leave_requests/{requestId}` lors de la création d'un nouveau document de demande d'absence.
- **Action Déclenchée** :
  1. **Notification Alerte Administrateur** : Envoie un courriel au format HTML soigné et responsive à l'administrateur (**Mohamed Taha El Mzabite** - `elmzabitemohamedtaha@gmail.com`) contenant tous les détails de la demande soumise (Collaborateur, Email, Département, Type d'absence, Période, Nombre de jours, Motif, Date et heure de soumission).
  2. **Accusé de Réception Collaborateur** : Envoie un courriel de confirmation au collaborateur demandeur confirmant que sa demande a bien été enregistrée et transmise pour examen.
  3. **Audit & Traçabilité** : Enregistre les métadonnées de transmission dans la collection Firestore `email_logs` ainsi que dans la collection `mail` (compatible avec l'extension Firebase *Trigger Email*).

### 2. `onLeaveRequestStatusUpdated` (Déclencheur Firestore `onDocumentUpdated`)
- **Déclencheur** : `leave_requests/{requestId}` lors de la modification du statut vers `APPROVED` ou `REJECTED`.
- **Action Déclenchée** :
  - Envoie un courriel officiel avec badge de décision vert (✅ Validée) ou rouge (❌ Refusée), le motif/commentaire et le nom de l'administrateur approbateur.

### 3. Points de Terminaison HTTPS (Tests & Déclenchements Manuels)
- **`sendNewLeaveRequestEmail`** :
  - URL HTTPS pour tester ou déclencher manuellement une notification de nouvelle demande (supporte requêtes POST et GET).
- **`sendManualStatusEmail`** :
  - URL HTTPS pour tester ou déclencher un avis de décision (Approbation / Refus).

---

## 🚀 Configuration SMTP (`functions/.env`)

Le module lit automatiquement les identifiants configurés dans `functions/.env` ou `functions/.env.example` :

```env
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USER=elmzabitemohamedtaha@gmail.com
SMTP_PASS=votre_mot_de_passe_d_application
SMTP_SECURE=false

FROM_EMAIL="TimeOff RH <elmzabitemohamedtaha@gmail.com>"
ADMIN_NAME=Mohamed Taha El Mzabite
ADMIN_EMAIL=elmzabitemohamedtaha@gmail.com
```

> **Configuration pour Gmail** :
> 1. Accédez à votre compte Google > **Sécurité**.
> 2. Activez l'authentification à deux facteurs si ce n'est pas déjà fait.
> 3. Allez dans **Mots de passe des applications** et générez un mot de passe à 16 caractères pour *TimeOff*.
> 4. Renseignez ce mot de passe dans `SMTP_PASS` dans `functions/.env` ou dans la console Firebase.

---

## 📦 Déploiement

Pour déployer les fonctions sur votre projet Firebase :

```bash
# Se connecter à Firebase CLI
firebase login

# Sélectionner le projet
firebase use <votre-project-id>

# Déployer les fonctions
firebase deploy --only functions
```
