import re

with open("app/src/main/java/com/example/ui/screens/AdminScreen.kt", "r") as f:
    text = f.read()

# Tab 3
text = text.replace('Text("Exports (PDF/CSV)", fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal)', 
                    'Text("${com.example.ui.i18n.tr("admin_tab_stats")} (PDF/CSV)", fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal)')

# Tab 4
text = text.replace('Text("Emails", fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Normal)', 
                    'Text(com.example.ui.i18n.tr("admin_tab_logs"), fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Normal)')

with open("app/src/main/java/com/example/ui/screens/AdminScreen.kt", "w") as f:
    f.write(text)

