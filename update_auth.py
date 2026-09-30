import re

with open("app/src/main/java/com/example/ui/AuthViewModel.kt", "r") as f:
    text = f.read()

old_admin_block = """        // Administrator Account
        if (cleanEmail.equals("elmzabitemohamedtaha@gmail.com", ignoreCase = true) &&
            (cleanPassword == "MOHAMEDTAHA123" || cleanPassword.equals("MOHAMEDTAHA123", ignoreCase = true))) {
            localStorage.setLastUsedEmail(cleanEmail)
            _loginError.value = null
            onAdminSuccess()
            return
        }"""

new_admin_block = """        // Administrator Account
        if (cleanEmail.equals("elmzabitemohamedtaha@gmail.com", ignoreCase = true) &&
            (cleanPassword == "MOHAMEDTAHA123" || cleanPassword.equals("MOHAMEDTAHA123", ignoreCase = true))) {
            val adminUser = com.example.data.User(
                email = cleanEmail,
                fullName = "Mohamed Taha El Mzabite",
                matricule = "ADMIN-0001",
                passwordHash = cleanPassword,
                jobTitle = "Administrateur RH",
                department = "Direction Générale",
                phone = "06 00 00 00 00",
                hireDate = "01 Janvier 2020",
                officeLocation = "Siège Social",
                avatarUrl = "https://api.dicebear.com/7.x/avataaars/png?seed=Admin",
                paidLeaveAllowance = 30,
                paidLeaveUsed = 5,
                rttAllowance = 15,
                rttUsed = 2
            )
            _currentUser.value = adminUser
            localStorage.setLastUsedEmail(cleanEmail)
            _loginError.value = null
            onAdminSuccess()
            return
        }"""

text = text.replace(old_admin_block, new_admin_block)

with open("app/src/main/java/com/example/ui/AuthViewModel.kt", "w") as f:
    f.write(text)
