package com.jornaler.app.ui.editor

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jornaler.app.data.EntryRepository
import com.jornaler.app.data.ThemeMode
import com.jornaler.app.domain.Formatting
import com.jornaler.app.domain.Sharing
import com.jornaler.app.domain.WritingStats
import com.jornaler.app.model.EntryKind
import com.jornaler.app.model.PieceStatus
import com.jornaler.app.ui.Moods
import com.jornaler.app.ui.theme.JornalerTheme
import com.jornaler.app.ui.theme.WritingBodyStyle
import com.jornaler.app.ui.theme.WritingTitleStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun EditorRoute(
    repository: EntryRepository,
    appScope: CoroutineScope,
    entryId: String,
    onBack: () -> Unit,
) {
    val viewModel: EditorViewModel = viewModel(
        key = entryId,
        factory = EditorViewModel.factory(repository, appScope, entryId),
    )
    DisposableEffect(viewModel) {
        onDispose { viewModel.flush() }
    }
    val scope = rememberCoroutineScope()
    EditorScreen(
        viewModel = viewModel,
        onBack = onBack,
        onDelete = {
            scope.launch {
                viewModel.delete()
                onBack()
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    onBack: () -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var tagDraft by remember { mutableStateOf("") }
    val bodyFocus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val writing = WritingBodyStyle.copy(
        color = MaterialTheme.colorScheme.onBackground,
        lineBreak = LineBreak.Simple,
        hyphens = Hyphens.None,
    )
    val titleStyle = WritingTitleStyle.copy(color = MaterialTheme.colorScheme.onBackground)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
                title = {
                    Text(
                        if (viewModel.loading) "" else Formatting.kindLabel(viewModel.kind),
                        style = MaterialTheme.typography.labelLarge,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!viewModel.loading && !viewModel.missing) {
                        IconButton(onClick = { viewModel.togglePin() }) {
                            Icon(
                                imageVector = if (viewModel.pinned) Icons.Filled.Star else Icons.Outlined.Star,
                                contentDescription = if (viewModel.pinned) "Unpin" else "Pin",
                                tint = if (viewModel.pinned) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                        if (viewModel.kind == EntryKind.POEM) {
                            IconButton(onClick = {
                                val draft = viewModel.currentDraft() ?: return@IconButton
                                val send = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, draft.title.ifBlank { "Poem" })
                                    putExtra(Intent.EXTRA_TEXT, Sharing.plainText(draft))
                                }
                                context.startActivity(Intent.createChooser(send, "Share poem"))
                            }) {
                                Icon(Icons.Filled.Share, contentDescription = "Share poem")
                            }
                        }
                        IconButton(onClick = { menu = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(
                                text = { Text("Delete page") },
                                leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                                onClick = {
                                    menu = false
                                    confirmDelete = true
                                },
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (!viewModel.loading && !viewModel.missing) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        "${WritingStats.words(viewModel.body)} words · ${WritingStats.lines(viewModel.body)} lines",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        if (viewModel.saveState == SaveState.SAVING) "Saving…" else "Saved",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        },
    ) { padding ->
        when {
            viewModel.loading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            viewModel.missing -> {
                Column(
                    Modifier.fillMaxSize().padding(padding).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text("This page is gone.", style = MaterialTheme.typography.titleLarge)
                    TextButton(onClick = onBack) { Text("Back to Jornaler") }
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Column(Modifier.widthIn(max = 680.dp).fillMaxWidth()) {
                        if (viewModel.kind != EntryKind.JOURNAL && viewModel.status != null) {
                            StatusPicker(viewModel.status!!) { viewModel.chooseStatus(it) }
                            Spacer(Modifier.height(12.dp))
                        }
                        if (viewModel.kind == EntryKind.JOURNAL) {
                            Text("Mood", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Moods.forEach { mood ->
                                    val selected = viewModel.mood == mood
                                    TextButton(
                                        onClick = { viewModel.chooseMood(mood) },
                                        modifier = if (selected) {
                                            Modifier.background(
                                                MaterialTheme.colorScheme.primaryContainer,
                                                androidx.compose.foundation.shape.CircleShape,
                                            )
                                        } else {
                                            Modifier
                                        },
                                    ) {
                                        Text(mood)
                                    }
                                }
                            }
                            if (viewModel.mood != null) {
                                Text(
                                    "Feeling ${viewModel.mood}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                        BasicTextField(
                            value = viewModel.title,
                            onValueChange = { viewModel.title = it.replace("\n", " ") },
                            textStyle = titleStyle,
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                                imeAction = ImeAction.Next,
                            ),
                            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                onNext = { bodyFocus.requestFocus() },
                            ),
                            decorationBox = { inner ->
                                Box {
                                    if (viewModel.title.isEmpty()) {
                                        Text(
                                            if (viewModel.kind == EntryKind.JOURNAL) "Give today a title" else "Title",
                                            style = titleStyle,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                                        )
                                    }
                                    inner()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(8.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            viewModel.tags.forEach { tag ->
                                InputChip(
                                    selected = true,
                                    onClick = { viewModel.removeTag(tag) },
                                    label = { Text(tag) },
                                )
                            }
                        }
                        BasicTextField(
                            value = tagDraft,
                            onValueChange = { tagDraft = it },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                onDone = {
                                    viewModel.addTags(tagDraft)
                                    tagDraft = ""
                                    keyboard?.hide()
                                },
                            ),
                            decorationBox = { inner ->
                                Box {
                                    if (tagDraft.isEmpty() && viewModel.tags.isEmpty()) {
                                        Text(
                                            "Add a tag",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        )
                                    }
                                    inner()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp, bottom = 12.dp),
                        )
                        Row(Modifier.fillMaxWidth()) {
                            if (viewModel.kind == EntryKind.POEM) {
                                Box(
                                    Modifier
                                        .padding(top = 8.dp, end = 14.dp)
                                        .width(2.dp)
                                        .heightIn(min = 280.dp)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                                )
                            }
                            BasicTextField(
                                value = viewModel.body,
                                onValueChange = { viewModel.body = it },
                                textStyle = writing,
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Sentences,
                                ),
                                decorationBox = { inner ->
                                    Box(Modifier.fillMaxWidth()) {
                                        if (viewModel.body.isEmpty()) {
                                            Text(
                                                when (viewModel.kind) {
                                                    EntryKind.POEM -> "Line by line."
                                                    EntryKind.JOURNAL -> "What do you want to keep from today?"
                                                    EntryKind.IDEA -> "The spark, before it cools."
                                                },
                                                style = writing,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                                            )
                                        }
                                        inner()
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 280.dp)
                                    .focusRequester(bodyFocus),
                            )
                        }
                        Spacer(Modifier.height(48.dp))
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this page?") },
            text = { Text("It's removed from this phone. A backup file you already exported is left as it was.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Keep") }
            },
        )
    }
}

@Composable
private fun StatusPicker(selected: PieceStatus, onSelect: (PieceStatus) -> Unit) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        PieceStatus.entries.forEachIndexed { index, status ->
            SegmentedButton(
                selected = selected == status,
                onClick = { onSelect(status) },
                shape = SegmentedButtonDefaults.itemShape(index, PieceStatus.entries.size),
            ) {
                Text(Formatting.statusLabel(status))
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 800)
@Composable
private fun EditorPreview() {
    JornalerTheme(ThemeMode.LIGHT) {
        Text(
            "The window at dusk",
            style = WritingTitleStyle,
            modifier = Modifier.padding(24.dp),
        )
    }
}
