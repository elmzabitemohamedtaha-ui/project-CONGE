import re

file_path = "app/src/main/java/com/example/ui/screens/AdminScreen.kt"
with open(file_path, "r") as f:
    content = f.read()

imports = """
import com.example.ui.i18n.I18nManager
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
"""

if "import com.example.ui.i18n.I18nManager" not in content:
    content = content.replace("import androidx.compose.material3.Text", imports + "\nimport androidx.compose.material3.Text")

language_row = """
            // Language Selection Row (Horizontally Scrollable)
            val currentLangCode by I18nManager.currentLang.collectAsState()
            val languages = listOf(
                "fr" to "Français",
                "en" to "English",
                "ar" to "العربية",
                "de" to "Deutsch"
            )
            
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(languages) { (code, name) ->
                    FilterChip(
                        selected = currentLangCode == code,
                        onClick = { I18nManager.setLang(context, code) },
                        label = { Text(name, style = MaterialTheme.typography.labelMedium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
"""

# Insert right after the opening of the Column in the Scaffold
content = content.replace(
    """        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {""", 
    """        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {""" + language_row
)

with open(file_path, "w") as f:
    f.write(content)

