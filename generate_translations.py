import json

dict_fr = {
    "nav_dashboard": "Tableau de bord",
    "nav_request": "Demande",
    "nav_history": "Historique",
    "nav_manager": "Manager",
    "nav_admin": "Admin",
    "nav_profile": "Profil",
    "app_name": "TimeOff",
    "logout": "Se Déconnecter",
    "accept": "Accepter",
    "reject": "Refuser",
    "send": "Envoyer",
    "cancel": "Annuler",
    "confirm": "Confirmer",
    "close": "Fermer",
    "dashboard_balances": "Soldes de congés",
    "dashboard_recent": "Mes dernières demandes",
    "paid_leaves": "Congés Payés",
    "rtt": "RTT",
    "days_remaining": "Jours restants",
    "team_stats": "Statistiques de l'équipe",
    "requests_per_month": "Demandes de congés par mois",
    "new_request": "Nouvelle demande",
    "leave_type": "Type de congé",
    "start_date": "Date de début",
    "end_date": "Date de fin",
    "reason": "Motif",
    "add_attachment": "Ajouter une pièce jointe",
    "view_attachment": "Voir pièce jointe",
    "status_pending": "En attente",
    "status_approved": "Approuvé",
    "status_rejected": "Refusé",
    "profile_title": "Mon Profil",
    "history_title": "Historique des demandes",
    "admin_title": "Administration RH",
    "manager_title": "Validation Équipe",
    "email": "Email Professionnel",
    "phone": "Téléphone",
    "department": "Département",
    "position": "Poste / Fonction",
    "language": "Langue",
    "select_language": "Sélectionner la langue"
}

dict_en = {
    "nav_dashboard": "Dashboard",
    "nav_request": "Request",
    "nav_history": "History",
    "nav_manager": "Manager",
    "nav_admin": "Admin",
    "nav_profile": "Profile",
    "app_name": "TimeOff",
    "logout": "Log Out",
    "accept": "Accept",
    "reject": "Reject",
    "send": "Submit",
    "cancel": "Cancel",
    "confirm": "Confirm",
    "close": "Close",
    "dashboard_balances": "Leave Balances",
    "dashboard_recent": "My Recent Requests",
    "paid_leaves": "Paid Leaves",
    "rtt": "Reduced Working Time",
    "days_remaining": "Days remaining",
    "team_stats": "Team Statistics",
    "requests_per_month": "Leave requests per month",
    "new_request": "New Request",
    "leave_type": "Leave Type",
    "start_date": "Start Date",
    "end_date": "End Date",
    "reason": "Reason",
    "add_attachment": "Add Attachment",
    "view_attachment": "View Attachment",
    "status_pending": "Pending",
    "status_approved": "Approved",
    "status_rejected": "Rejected",
    "profile_title": "My Profile",
    "history_title": "Request History",
    "admin_title": "HR Administration",
    "manager_title": "Team Validation",
    "email": "Professional Email",
    "phone": "Phone",
    "department": "Department",
    "position": "Position / Role",
    "language": "Language",
    "select_language": "Select Language"
}

dict_ar = {
    "nav_dashboard": "لوحة القيادة",
    "nav_request": "طلب",
    "nav_history": "السجل",
    "nav_manager": "المدير",
    "nav_admin": "الإدارة",
    "nav_profile": "الملف الشخصي",
    "app_name": "TimeOff",
    "logout": "تسجيل خروج",
    "accept": "قبول",
    "reject": "رفض",
    "send": "إرسال",
    "cancel": "إلغاء",
    "confirm": "تأكيد",
    "close": "إغلاق",
    "dashboard_balances": "رصيد الإجازات",
    "dashboard_recent": "طلباتي الأخيرة",
    "paid_leaves": "إجازات مدفوعة",
    "rtt": "تقليص وقت العمل",
    "days_remaining": "أيام متبقية",
    "team_stats": "إحصائيات الفريق",
    "requests_per_month": "طلبات الإجازة شهريا",
    "new_request": "طلب جديد",
    "leave_type": "نوع الإجازة",
    "start_date": "تاريخ البدء",
    "end_date": "تاريخ الانتهاء",
    "reason": "السبب",
    "add_attachment": "إضافة مرفق",
    "view_attachment": "عرض المرفق",
    "status_pending": "قيد الانتظار",
    "status_approved": "مقبول",
    "status_rejected": "مرفوض",
    "profile_title": "ملفي الشخصي",
    "history_title": "سجل الطلبات",
    "admin_title": "إدارة الموارد البشرية",
    "manager_title": "موافقة الفريق",
    "email": "البريد الإلكتروني المهني",
    "phone": "الهاتف",
    "department": "القسم",
    "position": "المنصب / الوظيفة",
    "language": "اللغة",
    "select_language": "اختر اللغة"
}

dict_de = {
    "nav_dashboard": "Dashboard",
    "nav_request": "Antrag",
    "nav_history": "Verlauf",
    "nav_manager": "Manager",
    "nav_admin": "Admin",
    "nav_profile": "Profil",
    "app_name": "TimeOff",
    "logout": "Abmelden",
    "accept": "Akzeptieren",
    "reject": "Ablehnen",
    "send": "Senden",
    "cancel": "Abbrechen",
    "confirm": "Bestätigen",
    "close": "Schließen",
    "dashboard_balances": "Urlaubssalden",
    "dashboard_recent": "Meine letzten Anträge",
    "paid_leaves": "Bezahlter Urlaub",
    "rtt": "Arbeitszeitverkürzung",
    "days_remaining": "Tage verbleibend",
    "team_stats": "Team-Statistiken",
    "requests_per_month": "Urlaubsanträge pro Monat",
    "new_request": "Neuer Antrag",
    "leave_type": "Urlaubsart",
    "start_date": "Startdatum",
    "end_date": "Enddatum",
    "reason": "Grund",
    "add_attachment": "Anhang hinzufügen",
    "view_attachment": "Anhang ansehen",
    "status_pending": "Ausstehend",
    "status_approved": "Genehmigt",
    "status_rejected": "Abgelehnt",
    "profile_title": "Mein Profil",
    "history_title": "Antragsverlauf",
    "admin_title": "HR-Administration",
    "manager_title": "Team-Validierung",
    "email": "Geschäftliche E-Mail",
    "phone": "Telefon",
    "department": "Abteilung",
    "position": "Position / Rolle",
    "language": "Sprache",
    "select_language": "Sprache auswählen"
}

output = f"""package com.example.ui.i18n

val translationsFr = mapOf(
{", ".join([f'    "{k}" to "{v}"' for k,v in dict_fr.items()])}
)

val translationsEn = mapOf(
{", ".join([f'    "{k}" to "{v}"' for k,v in dict_en.items()])}
)

val translationsAr = mapOf(
{", ".join([f'    "{k}" to "{v}"' for k,v in dict_ar.items()])}
)

val translationsDe = mapOf(
{", ".join([f'    "{k}" to "{v}"' for k,v in dict_de.items()])}
)

val allTranslations = mapOf(
    "fr" to translationsFr,
    "en" to translationsEn,
    "ar" to translationsAr,
    "de" to translationsDe
)
"""

with open("app/src/main/java/com/example/ui/i18n/Translations.kt", "w") as f:
    f.write(output)

