import re

with open("app/src/main/java/com/example/ui/screens/AdminScreen.kt", "r") as f:
    text = f.read()

# Change default to month view and add showOnlyApproved state
old_state = """    var selectedDayForDetail by remember { mutableStateOf<Int?>(13) }
    var calendarDisplayMode by remember { mutableIntStateOf(0) } // 0: Timeline Gantt, 1: Grille Mensuelle

    // Build complete team members and schedules with actual leave requests
    val teamMembers = remember(registeredUsers, allRequests) {"""

new_state = """    var selectedDayForDetail by remember { mutableStateOf<Int?>(13) }
    var calendarDisplayMode by remember { mutableIntStateOf(1) } // 1: Grille Mensuelle, 0: Timeline Gantt
    var showOnlyApproved by remember { mutableStateOf(true) }

    // Build complete team members and schedules with actual leave requests
    val teamMembers = remember(registeredUsers, allRequests, showOnlyApproved) {"""

text = text.replace(old_state, new_state)

# Apply showOnlyApproved filter inside teamMembers loop 1
old_filter_1 = """        for (u in registeredUsers) {
            val userLeaves = allRequests.filter { req ->
                (req.employeeEmail.trim().equals(u.email.trim(), ignoreCase = true) ||
                 req.employeeName.trim().equals(u.fullName.trim(), ignoreCase = true)) &&
                !req.status.equals("REJECTED", ignoreCase = true)
            }.map { it.toLeavePeriod() }"""

new_filter_1 = """        for (u in registeredUsers) {
            val userLeaves = allRequests.filter { req ->
                (req.employeeEmail.trim().equals(u.email.trim(), ignoreCase = true) ||
                 req.employeeName.trim().equals(u.fullName.trim(), ignoreCase = true)) &&
                if (showOnlyApproved) req.status.equals("APPROVED", ignoreCase = true) else !req.status.equals("REJECTED", ignoreCase = true)
            }.map { it.toLeavePeriod() }"""
            
text = text.replace(old_filter_1, new_filter_1)

# Apply showOnlyApproved filter inside teamMembers loop 2
old_filter_2 = """        for (req in allRequests) {
            if (membersList.none { it.email.equals(req.employeeEmail.trim(), ignoreCase = true) || it.name.equals(req.employeeName.trim(), ignoreCase = true) }) {
                val leaves = allRequests.filter {
                    (it.employeeEmail.trim().equals(req.employeeEmail.trim(), ignoreCase = true) ||
                     it.employeeName.trim().equals(req.employeeName.trim(), ignoreCase = true)) &&
                    !it.status.equals("REJECTED", ignoreCase = true)
                }.map { it.toLeavePeriod() }"""

new_filter_2 = """        for (req in allRequests) {
            if (membersList.none { it.email.equals(req.employeeEmail.trim(), ignoreCase = true) || it.name.equals(req.employeeName.trim(), ignoreCase = true) }) {
                val leaves = allRequests.filter {
                    (it.employeeEmail.trim().equals(req.employeeEmail.trim(), ignoreCase = true) ||
                     it.employeeName.trim().equals(req.employeeName.trim(), ignoreCase = true)) &&
                    if (showOnlyApproved) it.status.equals("APPROVED", ignoreCase = true) else !it.status.equals("REJECTED", ignoreCase = true)
                }.map { it.toLeavePeriod() }"""

text = text.replace(old_filter_2, new_filter_2)

with open("app/src/main/java/com/example/ui/screens/AdminScreen.kt", "w") as f:
    f.write(text)
