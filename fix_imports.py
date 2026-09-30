import re

def dedup_imports(file_path):
    with open(file_path, "r") as f:
        lines = f.readlines()
    
    seen = set()
    new_lines = []
    for line in lines:
        if line.startswith("import "):
            if line.strip() in seen:
                continue
            seen.add(line.strip())
        new_lines.append(line)
        
    with open(file_path, "w") as f:
        f.writelines(new_lines)

dedup_imports("app/src/main/java/com/example/ui/screens/AdminScreen.kt")
dedup_imports("app/src/main/java/com/example/ui/screens/ProfileScreen.kt")
