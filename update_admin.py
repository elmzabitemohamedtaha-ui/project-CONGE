import re

with open("app/src/main/java/com/example/ui/screens/AdminScreen.kt", "r") as f:
    text = f.read()

# Add Tabs 4 and 5
old_tab_3 = """                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Email,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(com.example.ui.i18n.tr("admin_tab_logs"), fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                )"""

new_tab_3_4_5 = """                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Email,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(com.example.ui.i18n.tr("admin_tab_logs"), fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Groups,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(com.example.ui.i18n.tr("admin_tab_employees"), fontWeight = if (selectedTab == 4) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 5,
                    onClick = { selectedTab = 5 },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(com.example.ui.i18n.tr("prof_title"), fontWeight = if (selectedTab == 5) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                )"""

text = text.replace(old_tab_3, new_tab_3_4_5)

# Add Content for 4 and 5
old_tab_content = """                3 -> {
                    EmailLogsTabView(
                        sentEmails = sentEmails,
                        onSelectEmail = { selectedEmailForDetail = it },
                        onOpenClient = { email ->
                            EmailNotificationService.openEmailClient(context, email)
                        }
                    )
                }
            }
        }
    }"""

new_tab_content = """                3 -> {
                    EmailLogsTabView(
                        sentEmails = sentEmails,
                        onSelectEmail = { selectedEmailForDetail = it },
                        onOpenClient = { email ->
                            EmailNotificationService.openEmailClient(context, email)
                        }
                    )
                }
                4 -> {
                    EmployeeDirectoryTab(registeredUsers = registeredUsers)
                }
                5 -> {
                    com.example.ui.screens.ProfileScreen(onLogout = onLogout)
                }
            }
        }
    }"""

text = text.replace(old_tab_content, new_tab_content)

with open("app/src/main/java/com/example/ui/screens/AdminScreen.kt", "w") as f:
    f.write(text)
