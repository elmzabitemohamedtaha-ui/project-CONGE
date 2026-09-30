import re

with open("app/src/main/java/com/example/ui/screens/HistoryScreen.kt", "r") as f:
    text = f.read()

old_column = """                Column {
                    Text(
                        text = request.leaveType,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${request.startDate} - ${request.endDate}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                    if (request.reason.isNotBlank()) {
                        Text(
                            text = request.reason,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }"""

new_column = """                Column {
                    Text(
                        text = request.leaveType,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${request.startDate} - ${request.endDate}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                    
                    val submissionDate = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(request.createdAt))
                    Text(
                        text = "Déposée le: $submissionDate",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    
                    if (request.status == "APPROVED") {
                        Text(
                            text = "Validé par: Mohamed Taha El Mzabite",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF15803D),
                            modifier = Modifier.padding(top = 2.dp),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (request.reason.isNotBlank()) {
                        Text(
                            text = request.reason,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }"""

text = text.replace(old_column, new_column)

with open("app/src/main/java/com/example/ui/screens/HistoryScreen.kt", "w") as f:
    f.write(text)
