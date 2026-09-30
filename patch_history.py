import re

file_path = "app/src/main/java/com/example/ui/screens/HistoryScreen.kt"
with open(file_path, "r") as f:
    content = f.read()

replacements = {
    '"Historique des demandes"': 'com.example.ui.i18n.tr("hist_title")',
    '"Aucune demande de congé enregistrée."': 'com.example.ui.i18n.tr("hist_empty")',
    '"Période : Du ${req.startDate} au ${req.endDate}"': 'com.example.ui.i18n.tr("hist_period", req.startDate, req.endDate)',
    '"Émis le $genDateFormatted par $managerName  •  Statut : Document Officiel RH"': 'com.example.ui.i18n.tr("hist_status_doc", genDateFormatted, managerName)',
    '"Type : ${req.leaveType} (${req.daysCount} jours)"': 'com.example.ui.i18n.tr("admin_type", req.leaveType, req.daysCount)',
    '"Motif : ${req.reason}"': 'com.example.ui.i18n.tr("req_reason") + " : " + req.reason',
    '"Voir pièce jointe: ${request.attachmentName}"': 'com.example.ui.i18n.tr("btn_view_attachment", request.attachmentName ?: "")'
}

for old, new in replacements.items():
    content = content.replace(old, new)

with open(file_path, "w") as f:
    f.write(content)

