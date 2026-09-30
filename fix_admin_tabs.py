import re

with open("app/src/main/java/com/example/ui/screens/AdminScreen.kt", "r") as f:
    text = f.read()

# Tab 1
text = text.replace('Text("Planning", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal)', 
                    'Text(com.example.ui.i18n.tr("admin_tab_calendar"), fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal)')

# Tab 2
text = text.replace('Text("Demandes (${pendingRequests.size})", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal)', 
                    'Text("${com.example.ui.i18n.tr("admin_tab_requests")} (${pendingRequests.size})", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal)')

with open("app/src/main/java/com/example/ui/screens/AdminScreen.kt", "w") as f:
    f.write(text)

