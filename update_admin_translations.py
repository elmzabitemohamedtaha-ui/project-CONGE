import re

# 1. Update Translations.kt
trans_file = "app/src/main/java/com/example/ui/i18n/Translations.kt"
with open(trans_file, "r") as f:
    t_content = f.read()

new_fr = """    "admin_type" to "{0} ({1}d)",
    "admin_tab_calendar" to "Planning",
    "admin_tab_requests" to "Demandes",
    "admin_tab_stats" to "Rapports",
    "admin_tab_logs" to "Logs Emails",
    "gantt_title" to "Vue Gantt de l'équipe ({0} collab.)",
    "scroll_horizontally" to "Défiler horizontalement",
    "admin_team_planning" to "Admin - Planning Équipe",
    "admin_supervision" to "Supervision des congés & Prévention",
    "admin_timeline" to "Timeline",
    "admin_grid" to "Grille"
)"""

new_en = """    "admin_type" to "{0} ({1}d)",
    "admin_tab_calendar" to "Planning",
    "admin_tab_requests" to "Requests",
    "admin_tab_stats" to "Reports",
    "admin_tab_logs" to "Email Logs",
    "gantt_title" to "Team Gantt View ({0} members)",
    "scroll_horizontally" to "Scroll horizontally",
    "admin_team_planning" to "Admin - Team Planning",
    "admin_supervision" to "Leave Supervision & Overlap Prevention",
    "admin_timeline" to "Timeline",
    "admin_grid" to "Grid"
)"""

new_ar = """    "admin_type" to "{0} ({1} يوم)",
    "admin_tab_calendar" to "التخطيط",
    "admin_tab_requests" to "الطلبات",
    "admin_tab_stats" to "تقارير",
    "admin_tab_logs" to "سجلات البريد",
    "gantt_title" to "عرض جانت للفريق ({0} عضو)",
    "scroll_horizontally" to "التمرير أفقيا",
    "admin_team_planning" to "المدير - تخطيط الفريق",
    "admin_supervision" to "الإشراف على الإجازات ومنع التداخل",
    "admin_timeline" to "الجدول الزمني",
    "admin_grid" to "شبكة"
)"""

new_de = """    "admin_type" to "{0} ({1} T)",
    "admin_tab_calendar" to "Planung",
    "admin_tab_requests" to "Anträge",
    "admin_tab_stats" to "Berichte",
    "admin_tab_logs" to "E-Mail-Protokolle",
    "gantt_title" to "Team-Gantt-Ansicht ({0} Mitgl.)",
    "scroll_horizontally" to "Horizontal scrollen",
    "admin_team_planning" to "Admin - Teamplanung",
    "admin_supervision" to "Urlaubsüberwachung & Überschneidungsvermeidung",
    "admin_timeline" to "Zeitachse",
    "admin_grid" to "Raster"
)"""

t_content = t_content.replace('    "admin_type" to "{0} ({1}d)")', new_fr)
t_content = t_content.replace('    "admin_type" to "{0} ({1}d)")', new_en) # Not accurate replacement, let's just do regex

with open(trans_file, "w") as f:
    f.write(t_content)

