import re

with open("app/src/main/java/com/example/ui/screens/AdminScreen.kt", "r") as f:
    text = f.read()

# 1. Update Tabs
text = text.replace('text = "Planning Équipe",', 'text = com.example.ui.i18n.tr("admin_tab_calendar"),')
text = text.replace('text = "Demandes en attente",', 'text = com.example.ui.i18n.tr("admin_tab_requests"),')
text = text.replace('text = "Rapports & Stats",', 'text = com.example.ui.i18n.tr("admin_tab_stats"),')
text = text.replace('text = "Logs Emails",', 'text = com.example.ui.i18n.tr("admin_tab_logs"),')

# 2. Update Headers
text = text.replace('text = "Admin - Planning Équipe",', 'text = com.example.ui.i18n.tr("admin_team_planning"),')
text = text.replace('text = "Supervision des congés & Prévention chevauchements",', 'text = com.example.ui.i18n.tr("admin_supervision"),')
text = text.replace('text = "Timeline",', 'text = com.example.ui.i18n.tr("admin_timeline"),')
text = text.replace('text = "Grille",', 'text = com.example.ui.i18n.tr("admin_grid"),')

# 3. Gantt title
# Original: text = "Vue Gantt de l'équipe (${members.size} collaborateurs)",
# Make it: text = com.example.ui.i18n.tr("gantt_title", members.size),
text = re.sub(r'text\s*=\s*"Vue Gantt de l\'équipe \(\$\{members\.size\} collaborateurs\)",', 'text = com.example.ui.i18n.tr("gantt_title", members.size),', text)


with open("app/src/main/java/com/example/ui/screens/AdminScreen.kt", "w") as f:
    f.write(text)

