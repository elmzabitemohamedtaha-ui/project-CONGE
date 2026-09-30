import re

with open("app/src/main/java/com/example/ui/screens/DashboardScreen.kt", "r") as f:
    content = f.read()

new_imports = """
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.graphics.StrokeCap
import java.util.Calendar
import com.example.data.LeaveRepository
"""
# insert imports if they don't exist
if "import androidx.compose.material3.CircularProgressIndicator" not in content:
    content = content.replace("import androidx.compose.material3.Card", "import androidx.compose.material3.CircularProgressIndicator\nimport androidx.compose.material3.Card")
if "import androidx.compose.ui.graphics.StrokeCap" not in content:
    content = content.replace("import androidx.compose.ui.graphics.Color", "import androidx.compose.ui.graphics.StrokeCap\nimport androidx.compose.ui.graphics.Color")

new_component = """
@Composable
fun CurrentMonthUsageCard(userEmail: String?, context: android.content.Context) {
    if (userEmail == null) return
    val leaveRepository = remember { LeaveRepository.getInstance(context) }
    val requests by leaveRepository.getEmployeeRequestsFlow(userEmail).collectAsState(initial = emptyList())
    
    val currentMonth = remember { Calendar.getInstance().get(Calendar.MONTH) }
    val currentYear = remember { Calendar.getInstance().get(Calendar.YEAR) }
    val monthNames = listOf("Janvier", "Février", "Mars", "Avril", "Mai", "Juin", "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre")
    
    val monthName = monthNames[currentMonth]
    
    // Count days approved this month
    val daysThisMonth = remember(requests, currentMonth, currentYear) {
        requests.filter { 
            it.status.uppercase() == "APPROVED" && 
            it.year == currentYear && 
            it.month == currentMonth 
        }.sumOf { it.daysCount.toDouble() }.toFloat()
    }
    
    // Assuming 21 working days per month approx
    val totalWorkingDays = 21f
    val progress = (daysThisMonth / totalWorkingDays).coerceIn(0f, 1f)
    
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.size(64.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    strokeWidth = 6.dp
                )
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(64.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 6.dp,
                    strokeCap = StrokeCap.Round
                )
                Text(
                    text = "${if (daysThisMonth % 1.0f == 0.0f) daysThisMonth.toInt() else daysThisMonth}j",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Consommation du mois",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$daysThisMonth jours de congés utilisés en $monthName.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
"""

content = content + "\n" + new_component

# Insert the component invocation
target_to_replace = """                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {"""

replacement = """                CurrentMonthUsageCard(userEmail = currentUser?.email, context = context)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {"""

content = content.replace(target_to_replace, replacement)

with open("app/src/main/java/com/example/ui/screens/DashboardScreen.kt", "w") as f:
    f.write(content)
