import re

file_path = "app/src/main/java/com/example/ui/screens/ProfileScreen.kt"
with open(file_path, "r") as f:
    content = f.read()

# 1. Add imports if missing
imports = """
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import com.example.ui.i18n.I18nManager
import androidx.compose.runtime.collectAsState
"""

if "import androidx.compose.material3.RadioButton" not in content:
    content = content.replace("import androidx.compose.material3.Text", imports + "\nimport androidx.compose.material3.Text")

# 2. Add state for dialog
if "var showLanguageDialog by remember { mutableStateOf(false) }" not in content:
    content = content.replace("var showCloudSyncDialog by remember { mutableStateOf(false) }", 
                              "var showCloudSyncDialog by remember { mutableStateOf(false) }\n    var showLanguageDialog by remember { mutableStateOf(false) }")

# 3. Modify SettingRow to be clickable and show current language
lang_replace_old = """                        SettingRow(
                            icon = Icons.Filled.Language,
                            title = "Langue de l'interface",
                            subtitle = "Français",
                            trailing = {
                                Icon(
                                    imageVector = Icons.Filled.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outlineVariant
                                )
                            }
                        )"""

lang_replace_new = """                        val currentLangCode by I18nManager.currentLang.collectAsState()
                        val currentLangName = when(currentLangCode) {
                            "fr" -> "Français"
                            "en" -> "English"
                            "ar" -> "العربية"
                            "de" -> "Deutsch"
                            else -> "Français"
                        }
                        
                        SettingRow(
                            icon = Icons.Filled.Language,
                            title = com.example.ui.i18n.tr("prof_lang"),
                            subtitle = currentLangName,
                            onClick = { showLanguageDialog = true },
                            trailing = {
                                Icon(
                                    imageVector = Icons.Filled.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outlineVariant
                                )
                            }
                        )"""

content = content.replace(lang_replace_old, lang_replace_new)

# 4. Add Dialog UI
dialog_ui = """
    // Language Selection Dialog
    if (showLanguageDialog) {
        val currentLangCode by I18nManager.currentLang.collectAsState()
        val languages = listOf(
            "fr" to "Français",
            "en" to "English",
            "ar" to "العربية",
            "de" to "Deutsch"
        )
        
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = {
                Text(com.example.ui.i18n.tr("select_language"))
            },
            text = {
                Column(modifier = Modifier.selectableGroup()) {
                    languages.forEach { (code, name) ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .selectable(
                                    selected = (code == currentLangCode),
                                    onClick = { 
                                        I18nManager.setLang(context, code)
                                        showLanguageDialog = false
                                    }
                                )
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (code == currentLangCode),
                                onClick = null, // null recommended for accessibility with screenreaders
                                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(text = name, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(com.example.ui.i18n.tr("btn_close"))
                }
            }
        )
    }
"""

content = content.replace("// Logout Section", dialog_ui + "\n    // Logout Section")

with open(file_path, "w") as f:
    f.write(content)

