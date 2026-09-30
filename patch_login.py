import re

file_path = "app/src/main/java/com/example/ui/screens/LoginScreen.kt"
with open(file_path, "r") as f:
    content = f.read()

replacements = {
    '"Adresse email professionnelle"': 'com.example.ui.i18n.tr("login_email")',
    '"nom.prenom@entreprise.com"': 'com.example.ui.i18n.tr("login_email_placeholder")',
    '"Mot de passe"': 'com.example.ui.i18n.tr("login_password")',
    '"Identifiants incorrects."': 'com.example.ui.i18n.tr("login_error")',
    '"Déverrouiller"': 'com.example.ui.i18n.tr("login_button")',
    '"Accès Protégé"': 'com.example.ui.i18n.tr("login_subtitle")',
    '"Administration RH"': 'com.example.ui.i18n.tr("login_admin")'
}

for old, new in replacements.items():
    content = content.replace(old, new)

with open(file_path, "w") as f:
    f.write(content)

