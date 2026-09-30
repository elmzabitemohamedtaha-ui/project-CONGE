import re

with open("app/src/main/java/com/example/ui/screens/AdminScreen.kt", "r") as f:
    text = f.read()

text = text.replace('fun AdminScreen(onLogout: () -> Unit) {', 'fun AdminScreen(authViewModel: com.example.ui.AuthViewModel? = null, onLogout: () -> Unit) {')

text = text.replace('com.example.ui.screens.ProfileScreen(onLogout = onLogout)', 'com.example.ui.screens.ProfileScreen(authViewModel = authViewModel, onLogout = onLogout)')

with open("app/src/main/java/com/example/ui/screens/AdminScreen.kt", "w") as f:
    f.write(text)
