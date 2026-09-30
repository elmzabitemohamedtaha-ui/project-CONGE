import re

with open("app/src/main/java/com/example/ui/screens/AdminScreen.kt", "r") as f:
    text = f.read()

text = text.replace('localStorage.saveRegisteredUser(updatedUser)', 'localStorage.saveUser(updatedUser, setAsActiveSession = false)')

with open("app/src/main/java/com/example/ui/screens/AdminScreen.kt", "w") as f:
    f.write(text)
