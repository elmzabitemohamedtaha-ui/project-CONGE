import re

file_path = "app/src/main/java/com/example/ui/components/LeaveRequestForm.kt"
with open(file_path, "r") as f:
    content = f.read()

replacements = {
    '"Nouvelle demande"': 'com.example.ui.i18n.tr("req_new_title")',
    '"Type de congé"': 'com.example.ui.i18n.tr("req_type")',
    '"Date de début"': 'com.example.ui.i18n.tr("req_start")',
    '"Date de fin"': 'com.example.ui.i18n.tr("req_end")',
    '"Motif"': 'com.example.ui.i18n.tr("req_reason")',
    '"Raison de la demande..."': 'com.example.ui.i18n.tr("req_reason_placeholder")',
    '"Demain (1j)"': 'com.example.ui.i18n.tr("req_tomorrow")',
    '"Semaine pro. (5j)"': 'com.example.ui.i18n.tr("req_next_week")',
    '"2 semaines"': 'com.example.ui.i18n.tr("req_2_weeks")',
    '"Veuillez renseigner les dates de début et de fin."': 'com.example.ui.i18n.tr("req_dates_error")',
    '"Format de date invalide. Utilisez le sélecteur ou le format jj/mm/aaaa."': 'com.example.ui.i18n.tr("req_dates_invalid")',
    '"La date de début doit être ultérieure à aujourd\'hui."': 'com.example.ui.i18n.tr("req_dates_past")',
    '"La date de fin doit être ultérieure à aujourd\'hui."': 'com.example.ui.i18n.tr("req_dates_past")',
    '"La date de fin ne peut pas être antérieure à la date de début."': 'com.example.ui.i18n.tr("req_dates_order")',
    '"La date de début et la date de fin ne peuvent pas être égales."': 'com.example.ui.i18n.tr("req_dates_equal")',
    '"Solde insuffisant pour cette demande de congés payés."': 'com.example.ui.i18n.tr("req_balance_error")',
    '"Solde insuffisant pour cette demande de RTT."': 'com.example.ui.i18n.tr("req_balance_error")',
    '"Ajouter une pièce jointe"': 'com.example.ui.i18n.tr("btn_add_attachment")',
    '"Pièce jointe ajoutée :"': 'com.example.ui.i18n.tr("req_attachment") + " :"',
    '"Enregistrer"': 'com.example.ui.i18n.tr("btn_submit")',
    '"Fermer"': 'com.example.ui.i18n.tr("btn_close")',
    '"Annuler"': 'com.example.ui.i18n.tr("btn_cancel")',
    '"Confirmer"': 'com.example.ui.i18n.tr("btn_confirm")',
    'Text("Demande de ${selectedTypeOption.name} ($startDate au $endDate) soumise avec succès !")': 'Text(com.example.ui.i18n.tr("req_success"))'
}

for old, new in replacements.items():
    content = content.replace(old, new)

# One more manual replace for the success message state assignment
content = content.replace('"Demande de ${selectedTypeOption.name} ($startDate au $endDate) soumise avec succès !"', 'com.example.ui.i18n.tr("req_success")')

with open(file_path, "w") as f:
    f.write(content)

