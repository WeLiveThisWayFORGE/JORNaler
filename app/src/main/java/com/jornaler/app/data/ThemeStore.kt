package com.jornaler.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

class ThemeStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val state = MutableStateFlow(read())

    fun observe(): StateFlow<ThemeMode> = state.asStateFlow()

    fun set(mode: ThemeMode) {
        prefs.edit().putString(KEY, mode.name).apply()
        state.value = mode
    }

    private fun read(): ThemeMode {
        val stored = prefs.getString(KEY, ThemeMode.SYSTEM.name)
        return runCatching { ThemeMode.valueOf(stored ?: ThemeMode.SYSTEM.name) }
            .getOrDefault(ThemeMode.SYSTEM)
    }

    private companion object {
        const val PREFS = "jornaler"
        const val KEY = "theme"
    }
}
