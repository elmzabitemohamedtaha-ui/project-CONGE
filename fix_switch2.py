import re

with open("app/src/main/java/com/example/ui/screens/AdminScreen.kt", "r") as f:
    text = f.read()

imports = """import androidx.compose.material3.Switch
import androidx.compose.ui.draw.scale
"""

text = text.replace("import androidx.compose.material3.AlertDialog", imports + "import androidx.compose.material3.AlertDialog")

with open("app/src/main/java/com/example/ui/screens/AdminScreen.kt", "w") as f:
    f.write(text)
