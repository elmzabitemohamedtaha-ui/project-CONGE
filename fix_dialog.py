import re

with open("app/src/main/java/com/example/ui/screens/HistoryScreen.kt", "r") as f:
    text = f.read()

old_details = """                    Text(
                        text = "• Période : du ${req.startDate} au ${req.endDate}",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Text(
                        text = "• Motif renseigné : ${req.reason}",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    if (!req.adminComment.isNullOrBlank()) {"""

new_details = """                    Text(
                        text = "• Période : du ${req.startDate} au ${req.endDate}",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Text(
                        text = "• Motif renseigné : ${req.reason}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    val submissionDate = java.text.SimpleDateFormat("dd/MM/yyyy à HH:mm", java.util.Locale.getDefault()).format(java.util.Date(req.createdAt))
                    Text(
                        text = "• Fait le : $submissionDate",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    if (req.status == "APPROVED") {
                        Text(
                            text = "• Approuvé par : Mohamed Taha El Mzabite (Administrateur)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF15803D)
                        )
                    }

                    if (!req.adminComment.isNullOrBlank()) {"""

text = text.replace(old_details, new_details)

with open("app/src/main/java/com/example/ui/screens/HistoryScreen.kt", "w") as f:
    f.write(text)
