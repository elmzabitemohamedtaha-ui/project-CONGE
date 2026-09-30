import re
with open("app/src/main/java/com/example/ui/i18n/Translations.kt", "r") as f:
    text = f.read()

text = text.replace('"admin_tab_logs" to "Logs Emails",', '"admin_tab_logs" to "Logs Emails",\n    "admin_tab_employees" to "Employés",')
text = text.replace('"admin_tab_logs" to "Email Logs",', '"admin_tab_logs" to "Email Logs",\n    "admin_tab_employees" to "Employees",')
text = text.replace('"admin_tab_logs" to "سجلات",', '"admin_tab_logs" to "سجلات",\n    "admin_tab_employees" to "الموظفون",')
text = text.replace('"admin_tab_logs" to "E-Mail-Protokolle",', '"admin_tab_logs" to "E-Mail-Protokolle",\n    "admin_tab_employees" to "Mitarbeiter",')

with open("app/src/main/java/com/example/ui/i18n/Translations.kt", "w") as f:
    f.write(text)
