import re

file_path = "app/src/main/java/com/example/ui/screens/ProfileScreen.kt"
with open(file_path, "r") as f:
    content = f.read()

replacements = {
    '"Mon Profil"': 'com.example.ui.i18n.tr("prof_title")',
    '"Email Professionnel"': 'com.example.ui.i18n.tr("prof_email")',
    '"Téléphone"': 'com.example.ui.i18n.tr("prof_phone")',
    '"Département (Non modifiable)"': 'com.example.ui.i18n.tr("prof_dept")',
    '"Poste / Fonction (Non modifiable)"': 'com.example.ui.i18n.tr("prof_pos")',
    '"Langue"': 'com.example.ui.i18n.tr("prof_lang")',
    '"Se Déconnecter"': 'com.example.ui.i18n.tr("btn_logout")'
}

for old, new in replacements.items():
    content = content.replace(old, new)

with open(file_path, "w") as f:
    f.write(content)

