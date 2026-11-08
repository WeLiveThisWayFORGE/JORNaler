package com.jornaler.app.ui

import android.content.Intent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jornaler.app.NotebookIntents
import com.jornaler.app.data.EntryRepository
import com.jornaler.app.data.ThemeMode
import com.jornaler.app.data.ThemeStore
import com.jornaler.app.ui.editor.EditorRoute
import com.jornaler.app.ui.home.HomeRoute
import com.jornaler.app.ui.quick.QuickAddRoute
import com.jornaler.app.ui.theme.JornalerTheme
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

sealed interface NavRequest {
    data class Open(val id: String) : NavRequest
    data object Quick : NavRequest
    data object Today : NavRequest

    companion object {
        fun from(intent: Intent?): NavRequest? {
            if (intent == null) return null
            return when (intent.getStringExtra(NotebookIntents.EXTRA_NAV)) {
                NotebookIntents.NAV_OPEN ->
                    intent.getStringExtra(NotebookIntents.EXTRA_ENTRY_ID)?.let { Open(it) }
                NotebookIntents.NAV_QUICK -> Quick
                NotebookIntents.NAV_TODAY -> Today
                else -> null
            }
        }
    }
}

@Composable
fun JornalerRoot(
    repository: EntryRepository,
    themes: ThemeStore,
    appScope: CoroutineScope,
    intentToken: Int,
    readIntent: () -> Intent,
) {
    val mode by themes.observe().collectAsState()
    val entries by repository.observeAll().collectAsStateWithLifecycle(initialValue = null)
    val nav = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    SystemBars(mode)
    JornalerTheme(mode) {
        Scaffold(
            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(snackbar) },
        ) { _ ->
            NavHost(
                navController = nav,
                startDestination = "home",
            ) {
                composable("home") {
                    HomeRoute(
                        repository = repository,
                        themeMode = mode,
                        onTheme = themes::set,
                        entries = entries,
                        snackbar = snackbar,
                        onOpen = { entry -> nav.navigate("editor/${entry.id}") },
                        onQuickAdd = { nav.navigate("quick") },
                        onOpenJournal = { date ->
                            scope.launch {
                                val page = repository.openOrCreateJournal(date)
                                nav.navigate("editor/${page.id}")
                            }
                        },
                    )
                }
                composable("quick") {
                    QuickAddRoute(
                        repository = repository,
                        snackbar = snackbar,
                        onBack = { nav.popBackStack() },
                        onOpen = { id ->
                            nav.navigate("editor/$id") {
                                popUpTo("quick") { inclusive = true }
                            }
                        },
                    )
                }
                composable("editor/{id}") { entry ->
                    val id = entry.arguments?.getString("id").orEmpty()
                    EditorRoute(
                        repository = repository,
                        appScope = appScope,
                        entryId = id,
                        onBack = { nav.popBackStack() },
                    )
                }
            }
            IntentBridge(intentToken, readIntent, nav, repository)
        }
    }
}

@Composable
private fun IntentBridge(
    intentToken: Int,
    readIntent: () -> Intent,
    nav: NavHostController,
    repository: EntryRepository,
) {
    var handled by rememberSaveable { mutableIntStateOf(-1) }
    LaunchedEffect(intentToken) {
        if (handled == intentToken) return@LaunchedEffect
        val intent = readIntent()
        when (val request = NavRequest.from(intent)) {
            is NavRequest.Open -> nav.navigate("editor/${request.id}") {
                popUpTo("home") { inclusive = false }
                launchSingleTop = true
            }
            NavRequest.Quick -> nav.navigate("quick") {
                popUpTo("home") { inclusive = false }
                launchSingleTop = true
            }
            NavRequest.Today -> {
                val page = repository.openOrCreateJournal(LocalDate.now())
                nav.navigate("editor/${page.id}") {
                    popUpTo("home") { inclusive = false }
                    launchSingleTop = true
                }
            }
            null -> Unit
        }
        intent.removeExtra(NotebookIntents.EXTRA_NAV)
        intent.removeExtra(NotebookIntents.EXTRA_ENTRY_ID)
        handled = intentToken
    }
}

@Composable
private fun SystemBars(mode: ThemeMode) {
    val view = LocalView.current
    val dark = when (mode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val background = if (dark) {
        com.jornaler.app.ui.theme.DarkColors.background
    } else {
        com.jornaler.app.ui.theme.LightColors.background
    }
    androidx.compose.runtime.SideEffect {
        val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
        window.statusBarColor = background.toArgb()
        window.navigationBarColor = background.toArgb()
        val controller = WindowCompat.getInsetsController(window, view)
        controller.isAppearanceLightStatusBars = !dark
        controller.isAppearanceLightNavigationBars = !dark
    }
}
