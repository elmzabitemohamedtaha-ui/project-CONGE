import re

with open("app/src/main/java/com/example/ui/TimeOffApp.kt", "r") as f:
    text = f.read()

text = text.replace('AdminScreen(\n                onLogout = {', 'AdminScreen(\n                authViewModel = authViewModel,\n                onLogout = {')

with open("app/src/main/java/com/example/ui/TimeOffApp.kt", "w") as f:
    f.write(text)
