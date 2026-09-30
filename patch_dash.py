import re

file_path = "app/src/main/java/com/example/ui/screens/DashboardScreen.kt"
with open(file_path, "r") as f:
    content = f.read()

replacements = {
    '"Soldes de congés"': 'com.example.ui.i18n.tr("dash_balances")',
    '"Aucune demande de congé enregistrée."': 'com.example.ui.i18n.tr("hist_empty")',
    '"Aucune demande récente."': 'com.example.ui.i18n.tr("dash_no_requests")',
    '"En attente"': 'com.example.ui.i18n.tr("status_pending")',
    '"Approuvé"': 'com.example.ui.i18n.tr("status_approved")',
    '"Refusé"': 'com.example.ui.i18n.tr("status_rejected")',
    '"Congés Payés"': 'com.example.ui.i18n.tr("type_cp")',
    '"RTT"': 'com.example.ui.i18n.tr("type_rtt")',
    '"Maladie"': 'com.example.ui.i18n.tr("type_sick")',
    '"Télétravail"': 'com.example.ui.i18n.tr("type_remote")',
    '"${days} jours restants"': 'com.example.ui.i18n.tr("days_remaining", days)',
    '"Vos demandes de congé et profil sont synchronisés en temps réel."': 'com.example.ui.i18n.tr("dash_sync")',
    '"Synchronisation Cloud"': 'com.example.ui.i18n.tr("dash_sync_title")'
}

for old, new in replacements.items():
    content = content.replace(old, new)

with open(file_path, "w") as f:
    f.write(content)

