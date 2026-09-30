import re

with open("app/src/main/java/com/example/ui/TimeOffApp.kt", "r") as f:
    text = f.read()

old_popup = """                    Text(
                        text = alert.message,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "Un email de confirmation officiel a également été envoyé sur votre boîte professionnelle.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center
                    )"""

new_popup = """                    Text(
                        text = alert.message,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    if (isApproved) {
                        Text(
                            text = "Acceptée par : Mohamed Taha El Mzabite (Administrateur)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF15803D),
                            textAlign = TextAlign.Center
                        )
                    }

                    Text(
                        text = "Un email de confirmation officiel a également été envoyé sur votre boîte professionnelle.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center
                    )"""

text = text.replace(old_popup, new_popup)

with open("app/src/main/java/com/example/ui/TimeOffApp.kt", "w") as f:
    f.write(text)
