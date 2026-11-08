package com.jornaler.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.jornaler.app.ui.JornalerRoot
import com.jornaler.app.ui.SamplePages
import java.time.LocalDate
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var intentToken by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val graph = (application as JornalerApp).graph
        if (shouldSeedSamplePages(
                BuildConfig.DEBUG,
                intent?.getBooleanExtra(NotebookIntents.EXTRA_SEED, false) == true,
            )
        ) {
            intent.removeExtra(NotebookIntents.EXTRA_SEED)
            lifecycleScope.launch {
                graph.repository.seedIfEmpty(SamplePages.entries(LocalDate.now()))
            }
        }
        setContent {
            JornalerRoot(
                repository = graph.repository,
                themes = graph.themes,
                appScope = graph.appScope,
                intentToken = intentToken,
                readIntent = { this@MainActivity.intent },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (shouldSeedSamplePages(
                BuildConfig.DEBUG,
                intent.getBooleanExtra(NotebookIntents.EXTRA_SEED, false),
            )
        ) {
            intent.removeExtra(NotebookIntents.EXTRA_SEED)
            val graph = (application as JornalerApp).graph
            lifecycleScope.launch {
                graph.repository.seedIfEmpty(SamplePages.entries(LocalDate.now()))
            }
        }
        intentToken += 1
    }
}
