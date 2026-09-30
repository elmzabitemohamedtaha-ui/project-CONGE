import re

file_path = "app/src/main/java/com/example/ui/screens/DashboardScreen.kt"
with open(file_path, "r") as f:
    content = f.read()

replacements = {
    'req.leaveType.contains(com.example.ui.i18n.tr("type_rtt"), ignoreCase = true)': 'req.leaveType.contains(com.example.ui.i18n.I18nManager.getString("type_rtt"), ignoreCase = true)',
    'req.leaveType.contains(com.example.ui.i18n.tr("type_remote"), ignoreCase = true)': 'req.leaveType.contains(com.example.ui.i18n.I18nManager.getString("type_remote"), ignoreCase = true)',
    'type = com.example.ui.i18n.tr("type_cp"),': 'type = com.example.ui.i18n.I18nManager.getString("type_cp"),'
}

for old, new in replacements.items():
    content = content.replace(old, new)

with open(file_path, "w") as f:
    f.write(content)

