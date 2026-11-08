package com.jornaler.app.ui.quick

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.dp
import com.jornaler.app.data.EntryRepository
import com.jornaler.app.model.EntryKind
import com.jornaler.app.ui.Moods
import com.jornaler.app.ui.theme.FrauncesDisplay
import com.jornaler.app.ui.theme.WritingBodyStyle
import java.time.LocalDate
import kotlinx.coroutines.launch

@Composable
fun QuickAddRoute(
    repository: EntryRepository,
    snackbar: SnackbarHostState,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    QuickAddScreen(
        onBack = onBack,
        onSave = { kind, title, body, mood ->
            scope.launch {
                val id = when (kind) {
                    EntryKind.JOURNAL -> repository.openOrCreateJournal(
                        date = LocalDate.now(),
                        mood = mood,
                        body = body,
                        title = title,
                    ).id
                    else -> {
                        if (title.isBlank() && body.isBlank()) {
                            snackbar.showSnackbar("Write a line first.")
                            return@launch
                        }
                        repository.create(kind = kind, title = title, body = body).id
                    }
                }
                onOpen(id)
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuickAddScreen(
    onBack: () -> Unit,
    onSave: (EntryKind, String, String, String?) -> Unit,
) {
    var kind by remember { mutableStateOf(EntryKind.IDEA) }
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var mood by remember { mutableStateOf<String?>(null) }
    val writing = WritingBodyStyle.copy(
        color = MaterialTheme.colorScheme.onBackground,
        lineBreak = LineBreak.Simple,
        hyphens = Hyphens.None,
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
                title = { Text("Catch a spark", fontFamily = FrauncesDisplay) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = 680.dp).fillMaxWidth()) {
                Text(
                    "Idea, poem, or today's journal. You can shape it after.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KindChoice(EntryKind.IDEA, "Idea", kind) { kind = EntryKind.IDEA }
                    KindChoice(EntryKind.POEM, "Poem", kind) { kind = EntryKind.POEM }
                    KindChoice(EntryKind.JOURNAL, "Journal", kind) { kind = EntryKind.JOURNAL }
                }
                Spacer(Modifier.height(18.dp))
                if (kind == EntryKind.JOURNAL) {
                    Text("Mood", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Moods.forEach { item ->
                            TextButton(onClick = { mood = if (mood == item) null else item }) {
                                Text(if (mood == item) "$item ·" else item)
                            }
                        }
                    }
                }
                BasicTextField(
                    value = title,
                    onValueChange = { title = it.replace("\n", " ") },
                    textStyle = MaterialTheme.typography.titleLarge.copy(color = MaterialTheme.colorScheme.onBackground),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    decorationBox = { inner ->
                        Box {
                            if (title.isEmpty()) {
                                Text(
                                    if (kind == EntryKind.JOURNAL) "Today's title, if you want one" else "Title, if it has one",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                                )
                            }
                            inner()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
                Spacer(Modifier.height(16.dp))
                BasicTextField(
                    value = body,
                    onValueChange = { body = it },
                    textStyle = writing,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    decorationBox = { inner ->
                        Box(Modifier.heightIn(min = 180.dp)) {
                            if (body.isEmpty()) {
                                Text(
                                    when (kind) {
                                        EntryKind.IDEA -> "The spark, before it cools."
                                        EntryKind.POEM -> "A first line is enough."
                                        EntryKind.JOURNAL -> "A few lines from today."
                                    },
                                    style = writing,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                                )
                            }
                            inner()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 180.dp),
                )
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = { onSave(kind, title.trim(), body, mood) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (kind == EntryKind.JOURNAL) "Open today's page" else "Keep this")
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun KindChoice(kind: EntryKind, label: String, selected: EntryKind, onClick: () -> Unit) {
    FilterChip(
        selected = selected == kind,
        onClick = onClick,
        label = { Text(label) },
    )
}
