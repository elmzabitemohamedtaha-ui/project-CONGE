import re

with open("app/src/main/java/com/example/ui/i18n/Translations.kt", "r") as f:
    text = f.read()

def inject_dict(lang_marker, block):
    global text
    # finds the end of the map definition for a language
    pattern = r'("admin_type".*?\))'
    # we need to be careful. Let's just find "admin_type" to "{0} ({1}d)")
    # or "admin_type" to "{0} ({1} يوم)") etc.
    # Actually, let's just do a manual string replace for each language.

text = text.replace('"admin_type" to "{0} ({1}d)")', '"admin_type" to "{0} ({1}d)",\n    "admin_tab_calendar" to "Planning",\n    "admin_tab_requests" to "Demandes",\n    "admin_tab_stats" to "Rapports",\n    "admin_tab_logs" to "Logs Emails",\n    "gantt_title" to "Vue Gantt ({0})",\n    "scroll_horizontally" to "Défiler horizontalement",\n    "admin_team_planning" to "Admin - Planning Équipe",\n    "admin_supervision" to "Supervision des congés",\n    "admin_timeline" to "Timeline",\n    "admin_grid" to "Grille"\n)')

text = text.replace('"admin_type" to "{0} ({1}d)")', '"admin_type" to "{0} ({1}d)",\n    "admin_tab_calendar" to "Planning",\n    "admin_tab_requests" to "Requests",\n    "admin_tab_stats" to "Reports",\n    "admin_tab_logs" to "Email Logs",\n    "gantt_title" to "Gantt View ({0})",\n    "scroll_horizontally" to "Scroll horizontally",\n    "admin_team_planning" to "Admin - Team Planning",\n    "admin_supervision" to "Leave Supervision",\n    "admin_timeline" to "Timeline",\n    "admin_grid" to "Grid"\n)')

text = text.replace('"admin_type" to "{0} ({1} يوم)")', '"admin_type" to "{0} ({1} يوم)",\n    "admin_tab_calendar" to "التخطيط",\n    "admin_tab_requests" to "الطلبات",\n    "admin_tab_stats" to "تقارير",\n    "admin_tab_logs" to "سجلات",\n    "gantt_title" to "عرض جانت ({0})",\n    "scroll_horizontally" to "التمرير أفقيا",\n    "admin_team_planning" to "تخطيط الفريق",\n    "admin_supervision" to "الإشراف على الإجازات",\n    "admin_timeline" to "الجدول الزمني",\n    "admin_grid" to "شبكة"\n)')

text = text.replace('"admin_type" to "{0} ({1} T)")', '"admin_type" to "{0} ({1} T)",\n    "admin_tab_calendar" to "Planung",\n    "admin_tab_requests" to "Anträge",\n    "admin_tab_stats" to "Berichte",\n    "admin_tab_logs" to "E-Mail-Protokolle",\n    "gantt_title" to "Gantt-Ansicht ({0})",\n    "scroll_horizontally" to "Horizontal scrollen",\n    "admin_team_planning" to "Admin - Teamplanung",\n    "admin_supervision" to "Urlaubsüberwachung",\n    "admin_timeline" to "Zeitachse",\n    "admin_grid" to "Raster"\n)')

with open("app/src/main/java/com/example/ui/i18n/Translations.kt", "w") as f:
    f.write(text)
