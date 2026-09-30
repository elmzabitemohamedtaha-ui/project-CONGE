import re

file_path = "app/src/main/java/com/example/ui/screens/AdminScreen.kt"
with open(file_path, "r") as f:
    content = f.read()

replacements = {
    '"Administration RH"': 'com.example.ui.i18n.tr("login_admin")',
    '"Demandes en attente (${pendingRequests.size})"': 'com.example.ui.i18n.tr("admin_pending", pendingRequests.size)',
    '"Rechercher par employé ou date..."': 'com.example.ui.i18n.tr("admin_search")',
    '"Voulez-vous approuver cette demande ?"': 'com.example.ui.i18n.tr("admin_approve_confirm")',
    '"Voulez-vous refuser cette demande ?"': 'com.example.ui.i18n.tr("admin_reject_confirm")',
    '"Commentaire officiel"': 'com.example.ui.i18n.tr("admin_comment")',
    '"Accepter"': 'com.example.ui.i18n.tr("btn_accept")',
    '"Refuser"': 'com.example.ui.i18n.tr("btn_reject")',
    '"Département : ${req.department}"': 'com.example.ui.i18n.tr("admin_dept", req.department)',
    '"Annuler"': 'com.example.ui.i18n.tr("btn_cancel")'
}

for old, new in replacements.items():
    content = content.replace(old, new)

with open(file_path, "w") as f:
    f.write(content)

