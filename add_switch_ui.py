import re

with open("app/src/main/java/com/example/ui/screens/AdminScreen.kt", "r") as f:
    text = f.read()

old_ui = """                        // Toggle Mode: Timeline Gantt vs Grid
                        Row(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(8.dp))
                                .padding(2.dp)
                        ) {"""

new_ui = """                        // Toggle to show only approved
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Text(
                                text = "Uniquement validés",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Switch(
                                checked = showOnlyApproved,
                                onCheckedChange = { showOnlyApproved = it },
                                modifier = Modifier.scale(0.7f)
                            )
                        }

                        // Toggle Mode: Timeline Gantt vs Grid
                        Row(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(8.dp))
                                .padding(2.dp)
                        ) {"""
                        
# Add scale import if necessary (though usually we can just use Modifier.scale directly if it's imported, let's just write androidx.compose.ui.draw.scale(0.7f) to be safe).

new_ui = new_ui.replace("Modifier.scale(0.7f)", "Modifier.then(androidx.compose.ui.draw.scale(0.7f))")

text = text.replace(old_ui, new_ui)

with open("app/src/main/java/com/example/ui/screens/AdminScreen.kt", "w") as f:
    f.write(text)
