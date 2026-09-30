package com.example.ui.i18n

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object I18nManager {
    private const val PREFS_NAME = "i18n_prefs"
    private const val LANG_KEY = "app_lang"

    private val _currentLang = MutableStateFlow("fr")
    val currentLang: StateFlow<String> = _currentLang.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedLang = prefs.getString(LANG_KEY, "fr") ?: "fr"
        _currentLang.value = savedLang
    }

    fun setLang(context: Context, lang: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(LANG_KEY, lang).apply()
        _currentLang.value = lang
    }

    fun getString(key: String, vararg args: Any): String {
        val lang = _currentLang.value
        var template = allTranslations[lang]?.get(key) ?: allTranslations["fr"]?.get(key) ?: key
        args.forEachIndexed { index, arg ->
            template = template.replace("{$index}", arg.toString())
        }
        return template
    }
}

@Composable
fun tr(key: String, vararg args: Any): String {
    val lang by I18nManager.currentLang.collectAsState()
    var template = allTranslations[lang]?.get(key) ?: allTranslations["fr"]?.get(key) ?: key
    
    args.forEachIndexed { index, arg ->
        template = template.replace("{$index}", arg.toString())
    }
    return template
}
