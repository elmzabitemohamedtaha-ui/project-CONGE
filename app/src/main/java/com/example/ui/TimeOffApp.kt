package com.example.ui

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.LeaveRepository
import com.example.ui.screens.AdminScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.RequestScreen
import kotlinx.coroutines.launch

sealed class Screen(
    val route: String,
    val title: String,
    val filledIcon: ImageVector,
    val outlinedIcon: ImageVector
) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Filled.Dashboard, Icons.Outlined.Dashboard)
    object Request : Screen("request", "Request", Icons.Filled.AddCircle, Icons.Outlined.AddCircleOutline)
    object History : Screen("history", "History", Icons.Filled.History, Icons.Outlined.History)
    object Profile : Screen("profile", "Profile", Icons.Filled.Person, Icons.Outlined.Person)
}

val items = listOf(
    Screen.Dashboard,
    Screen.Request,
    Screen.History,
    Screen.Profile
)

@Composable
fun TimeOffApp() {
    val rootNavController = rememberNavController()
    val context = LocalContext.current
    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(context.applicationContext as Application)
    )

    NavHost(
        navController = rootNavController,
        startDestination = "login"
    ) {
        composable("login") {
            LoginScreen(
                authViewModel = authViewModel,
                onLoginSuccess = {
                    rootNavController.navigate("main") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onAdminLogin = {
                    rootNavController.navigate("admin") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
        composable("admin") {
            AdminScreen(
                onLogout = {
                    authViewModel.logout()
                    rootNavController.navigate("login") {
                        popUpTo(0)
                    }
                }
            )
        }
        composable("main") {
            MainAppScreen(
                authViewModel = authViewModel,
                onLogout = {
                    authViewModel.logout()
                    rootNavController.navigate("login") {
                        popUpTo(0)
                    }
                }
            )
        }
    }
}

@Composable
fun MainAppScreen(
    authViewModel: AuthViewModel,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    val leaveRepository = remember { LeaveRepository.getInstance(context) }
    val activePopupAlert by leaveRepository.activePopupAlert.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()

    // Check if there are any unshown alerts for this logged in employee
    LaunchedEffect(currentUser) {
        currentUser?.email?.let { email ->
            leaveRepository.checkForUnshownAlerts(email)
        }
    }

    LaunchedEffect(Unit) {
        NotificationSystem.notifications.collect { notification ->
            val statusStr = if (notification.isApproved) "Validé" else "Refusé"
            snackbarHostState.showSnackbar(
                message = "${notification.title}: ${notification.message}",
                duration = SnackbarDuration.Short
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                items.forEach { screen ->
                    val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = if (selected) screen.filledIcon else screen.outlinedIcon,
                                contentDescription = screen.title
                            )
                        },
                        label = { Text(screen.title) },
                        selected = selected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    authViewModel = authViewModel,
                    onNavigateToRequest = {
                        navController.navigate(Screen.Request.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(Screen.Request.route) { RequestScreen(authViewModel = authViewModel) }
            composable(Screen.History.route) {
                HistoryScreen(
                    authViewModel = authViewModel,
                    onNavigateToRequest = {
                        navController.navigate(Screen.Request.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(Screen.Profile.route) { ProfileScreen(authViewModel = authViewModel, onLogout = onLogout) }
        }
    }

    // Prominent Alert Popup for Employee when Admin Validates or Refuses
    activePopupAlert?.let { alert ->
        val isApproved = alert.type == "APPROVED"
        val dialogColor = if (isApproved) Color(0xFF10B981) else MaterialTheme.colorScheme.error

        AlertDialog(
            onDismissRequest = {
                coroutineScope.launch {
                    leaveRepository.dismissActiveAlert(alert)
                }
            },
            icon = {
                Icon(
                    imageVector = if (isApproved) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                    contentDescription = null,
                    tint = dialogColor,
                    modifier = Modifier.size(44.dp)
                )
            },
            title = {
                Text(
                    text = alert.title,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isApproved) Color(0xFFDCFCE7) else MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isApproved) "Décision : VALIDÉE" else "Décision : REFUSÉE",
                            color = if (isApproved) Color(0xFF15803D) else MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(10.dp),
                            textAlign = TextAlign.Center
                        )
                    }

                    Text(
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
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            leaveRepository.dismissActiveAlert(alert)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = dialogColor)
                ) {
                    Text("J'ai compris", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
