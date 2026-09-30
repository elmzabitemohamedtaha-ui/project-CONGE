import re

with open("app/src/main/java/com/example/ui/screens/AdminScreen.kt", "r") as f:
    text = f.read()

# Fix scale
text = text.replace('Modifier.then(androidx.compose.ui.draw.scale(0.7f))', 'Modifier.scale(0.7f)')

# Add imports
imports = """import androidx.compose.material3.Switch
import androidx.compose.ui.draw.scale
"""

if "import androidx.compose.material3.Switch" not in text:
    text = text.replace("import androidx.compose.material3.*", "import androidx.compose.material3.*\n" + imports)

with open("app/src/main/java/com/example/ui/screens/AdminScreen.kt", "w") as f:
    f.write(text)
