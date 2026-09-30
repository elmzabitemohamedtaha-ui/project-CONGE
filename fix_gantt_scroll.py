import re

with open("app/src/main/java/com/example/ui/screens/AdminScreen.kt", "r") as f:
    text = f.read()

old_gantt_header = """fun TeamTimelineGanttView(
    members: List<TeamMemberSchedule>,
    daysInMonth: Int,
    currentMonth: Int,
    currentYear: Int,
    detectedOverlaps: List<TeamOverlapConflict>,
    onSelectDay: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = com.example.ui.i18n.tr("gantt_title", members.size),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Faites défiler horizontalement →",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            val horizontalScrollState = rememberScrollState()"""

new_gantt_header = """fun TeamTimelineGanttView(
    members: List<TeamMemberSchedule>,
    daysInMonth: Int,
    currentMonth: Int,
    currentYear: Int,
    detectedOverlaps: List<TeamOverlapConflict>,
    onSelectDay: (Int) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            val horizontalScrollState = rememberScrollState()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = com.example.ui.i18n.tr("gantt_title", members.size),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            horizontalScrollState.animateScrollTo(horizontalScrollState.value + 500)
                        }
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = com.example.ui.i18n.tr("scroll_horizontally"),
                        style = MaterialTheme.typography.labelSmall
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))"""

text = text.replace(old_gantt_header, new_gantt_header)

with open("app/src/main/java/com/example/ui/screens/AdminScreen.kt", "w") as f:
    f.write(text)
