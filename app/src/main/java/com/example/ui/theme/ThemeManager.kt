package com.example.ui.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}

object ThemeManager {
    private const val PREFS_NAME = "timeoff_theme_preferences"
    private const val KEY_THEME_MODE = "app_theme_mode"

    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private var sharedPreferences: SharedPreferences? = null

    fun initialize(context: Context) {
        if (sharedPreferences == null) {
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            sharedPreferences = prefs
            val savedName = prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name)
            val initialMode = try {
                ThemeMode.valueOf(savedName ?: ThemeMode.SYSTEM.name)
            } catch (e: Exception) {
                ThemeMode.SYSTEM
            }
            _themeMode.value = initialMode
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        sharedPreferences?.edit()?.putString(KEY_THEME_MODE, mode.name)?.apply()
    }

    fun toggleTheme() {
        val current = _themeMode.value
        val next = when (current) {
            ThemeMode.LIGHT -> ThemeMode.DARK
            ThemeMode.DARK -> ThemeMode.LIGHT
            ThemeMode.SYSTEM -> ThemeMode.DARK
        }
        setThemeMode(next)
    }

    @Composable
    fun isDarkTheme(): Boolean {
        val mode by themeMode.collectAsState()
        return when (mode) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
            ThemeMode.SYSTEM -> isSystemInDarkTheme()
        }
    }
}

/**
 * Reusable icon button for AppBars that smoothly toggles between Light and Dark mode.
 */
@Composable
fun ThemeToggleIconButton(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface
) {
    val context = LocalContext.current
    val currentMode by ThemeManager.themeMode.collectAsState()
    val isDark = ThemeManager.isDarkTheme()

    IconButton(
        onClick = {
            ThemeManager.initialize(context)
            ThemeManager.toggleTheme()
        },
        modifier = modifier
    ) {
        AnimatedContent(
            targetState = isDark,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "ThemeToggleIcon"
        ) { dark ->
            if (dark) {
                Icon(
                    imageVector = Icons.Filled.LightMode,
                    contentDescription = "Passer au mode clair",
                    tint = tint,
                    modifier = Modifier.size(22.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.DarkMode,
                    contentDescription = "Passer au mode sombre",
                    tint = tint,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

/**
 * Reusable Theme Setting Control for Profile & Settings screens.
 */
@Composable
fun ThemeModeSelectorCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentMode by ThemeManager.themeMode.collectAsState()
    val isDark = ThemeManager.isDarkTheme()

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = if (isDark) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "Thème de l'application",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = when (currentMode) {
                                ThemeMode.LIGHT -> "Mode Clair activé"
                                ThemeMode.DARK -> "Mode Sombre activé"
                                ThemeMode.SYSTEM -> "Automatique (suit le système)"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = isDark,
                    onCheckedChange = { checked ->
                        ThemeManager.initialize(context)
                        ThemeManager.setThemeMode(if (checked) ThemeMode.DARK else ThemeMode.LIGHT)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.surface,
                        checkedTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            // Segmented mode picker: Clair / Sombre / Système
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                val modes = listOf(
                    ThemeMode.LIGHT to "Clair",
                    ThemeMode.DARK to "Sombre",
                    ThemeMode.SYSTEM to "Auto"
                )

                modes.forEachIndexed { index, (mode, label) ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size),
                        onClick = {
                            ThemeManager.initialize(context)
                            ThemeManager.setThemeMode(mode)
                        },
                        selected = currentMode == mode,
                        label = {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (currentMode == mode) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }
        }
    }
}
