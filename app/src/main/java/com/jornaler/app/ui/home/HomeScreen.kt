package com.jornaler.app.ui.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jornaler.app.data.EntryRepository
import com.jornaler.app.data.ThemeMode
import com.jornaler.app.domain.Formatting
import com.jornaler.app.domain.Library
import com.jornaler.app.domain.Shelf
import com.jornaler.app.domain.nextStatus
import com.jornaler.app.model.Entry
import com.jornaler.app.model.EntryKind
import com.jornaler.app.ui.SamplePages
import com.jornaler.app.ui.theme.FrauncesDisplay
import com.jornaler.app.ui.theme.JornalerTheme
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun HomeRoute(
    repository: EntryRepository,
    themeMode: ThemeMode,
    onTheme: (ThemeMode) -> Unit,
    entries: List<Entry>?,
    snackbar: SnackbarHostState,
    onOpen: (Entry) -> Unit,
    onQuickAdd: () -> Unit,
    onOpenJournal: (LocalDate) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val message = runCatching {
                val json = repository.exportJson()
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(json.toByteArray(Charsets.UTF_8))
                    } ?: error("Couldn't open the file.")
                }
                "Backup saved on this phone."
            }.getOrElse { "Couldn't export. ${it.message ?: "Try another folder."}" }
            snackbar.showSnackbar(message)
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val message = runCatching {
                val text = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
                        ?: error("Couldn't open the file.")
                }
                val outcome = repository.importJson(text)
                "Added ${outcome.added}, updated ${outcome.updated}, kept ${outcome.keptLocal} newer pages."
            }.getOrElse { it.message ?: "Couldn't import that file." }
            snackbar.showSnackbar(message)
        }
    }

    HomeScreen(
        entries = entries,
        today = LocalDate.now(),
        themeMode = themeMode,
        onTheme = onTheme,
        onOpen = onOpen,
        onPin = { entry ->
            scope.launch { repository.save(entry.copy(pinned = !entry.pinned)) }
        },
        onCycleStatus = { entry ->
            val status = entry.status ?: return@HomeScreen
            scope.launch {
                repository.save(entry.copy(status = nextStatus(status), updatedAt = System.currentTimeMillis()))
            }
        },
        onQuickAdd = onQuickAdd,
        onOpenJournal = onOpenJournal,
        onExport = { exportLauncher.launch("jornaler-backup.json") },
        onImport = {
            importLauncher.launch(arrayOf("application/json", "text/*", "application/octet-stream"))
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    entries: List<Entry>?,
    today: LocalDate,
    themeMode: ThemeMode,
    onTheme: (ThemeMode) -> Unit,
    onOpen: (Entry) -> Unit,
    onPin: (Entry) -> Unit,
    onCycleStatus: (Entry) -> Unit,
    onQuickAdd: () -> Unit,
    onOpenJournal: (LocalDate) -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    initialShelf: Shelf = Shelf.IN_PROGRESS,
) {
    var shelfName by rememberSaveable { mutableStateOf(initialShelf.name) }
    val shelf = runCatching { Shelf.valueOf(shelfName) }.getOrDefault(Shelf.IN_PROGRESS)
    var searching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var monthKey by rememberSaveable { mutableStateOf(YearMonth.from(today).toString()) }
    val month = runCatching { YearMonth.parse(monthKey) }.getOrDefault(YearMonth.from(today))
    var menu by remember { mutableStateOf(false) }
    val searchingNow = searching || query.isNotBlank()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
                title = {
                    if (searchingNow) {
                        androidx.compose.foundation.text.BasicTextField(
                            value = query,
                            onValueChange = { query = it },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onBackground,
                            ),
                            cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                            decorationBox = { inner ->
                                Box {
                                    if (query.isEmpty()) {
                                        Text(
                                            "Search pages, tags, moods",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    inner()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        Text("JORNaler", fontFamily = FrauncesDisplay)
                    }
                },
                navigationIcon = {
                    if (searchingNow) {
                        IconButton(onClick = {
                            query = ""
                            searching = false
                        }) {
                            Icon(Icons.Filled.Close, contentDescription = "Close search")
                        }
                    }
                },
                actions = {
                    if (!searchingNow) {
                        IconButton(onClick = { searching = true }) {
                            Icon(Icons.Filled.Search, contentDescription = "Search")
                        }
                    }
                    IconButton(onClick = { menu = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More")
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        ThemeMode.entries.forEach { mode ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        when (mode) {
                                            ThemeMode.SYSTEM -> "Match system theme"
                                            ThemeMode.LIGHT -> "Light paper"
                                            ThemeMode.DARK -> "Night paper"
                                        },
                                    )
                                },
                                leadingIcon = {
                                    if (themeMode == mode) {
                                        Icon(Icons.Filled.Check, contentDescription = null)
                                    }
                                },
                                onClick = {
                                    onTheme(mode)
                                    menu = false
                                },
                            )
                        }
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Export backup") },
                            onClick = {
                                menu = false
                                onExport()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Import backup") },
                            onClick = {
                                menu = false
                                onImport()
                            },
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onQuickAdd) {
                Icon(Icons.Filled.Add, contentDescription = "New page")
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 760.dp)
                    .fillMaxSize(),
            ) {
                if (!searchingNow) {
                    PrimaryTabRow(selectedTabIndex = shelf.ordinal) {
                        Shelf.entries.forEach { item ->
                            Tab(
                                selected = item == shelf,
                                onClick = { shelfName = item.name },
                                text = {
                                    Text(
                                        when (item) {
                                            Shelf.IN_PROGRESS -> "In progress"
                                            Shelf.POEMS -> "Poems"
                                            Shelf.JOURNAL -> "Journal"
                                            Shelf.FINISHED -> "Finished"
                                        },
                                        maxLines = 1,
                                    )
                                },
                            )
                        }
                    }
                } else {
                    Text(
                        "Searching every page",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    )
                }

                when {
                    entries == null -> LoadingNotebook(Modifier.fillMaxSize())
                    searchingNow -> PageList(
                        pages = Library.visible(entries, shelf, query),
                        today = today,
                        emptyTitle = "Nothing matches",
                        emptyBody = "Try a word from the page, a tag, or a mood.",
                        onOpen = onOpen,
                        onPin = onPin,
                        onCycleStatus = onCycleStatus,
                    )
                    shelf == Shelf.JOURNAL -> JournalBrowser(
                        entries = entries,
                        month = month,
                        today = today,
                        onMonth = { monthKey = it.toString() },
                        onOpen = onOpen,
                        onPin = onPin,
                        onOpenJournal = onOpenJournal,
                    )
                    else -> {
                        val copy = emptyCopy(shelf)
                        PageList(
                            pages = Library.visible(entries, shelf, ""),
                            today = today,
                            emptyTitle = copy.first,
                            emptyBody = copy.second,
                            onOpen = onOpen,
                            onPin = onPin,
                            onCycleStatus = onCycleStatus,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PageList(
    pages: List<Entry>,
    today: LocalDate,
    emptyTitle: String,
    emptyBody: String,
    onOpen: (Entry) -> Unit,
    onPin: (Entry) -> Unit,
    onCycleStatus: (Entry) -> Unit,
) {
    if (pages.isEmpty()) {
        EmptyCopy(emptyTitle, emptyBody, Modifier.fillMaxSize())
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(pages, key = { it.id }) { entry ->
            EntryCard(entry, today, onOpen, onPin, onCycleStatus)
        }
    }
}

@Composable
private fun JournalBrowser(
    entries: List<Entry>,
    month: YearMonth,
    today: LocalDate,
    onMonth: (YearMonth) -> Unit,
    onOpen: (Entry) -> Unit,
    onPin: (Entry) -> Unit,
    onOpenJournal: (LocalDate) -> Unit,
) {
    val monthPages = Library.journalsInMonth(entries, month.year, month.monthValue)
    val marked = monthPages.mapNotNull { page ->
        page.journalDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    }.toSet()
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            TextButton(onClick = { onOpenJournal(today) }, modifier = Modifier.fillMaxWidth()) {
                Text("Write today", style = MaterialTheme.typography.titleMedium)
            }
        }
        item {
            MonthCalendar(
                month = month,
                marked = marked,
                today = today,
                onPrev = { onMonth(month.minusMonths(1)) },
                onNext = { onMonth(month.plusMonths(1)) },
                onDay = onOpenJournal,
            )
        }
        if (monthPages.isEmpty()) {
            item {
                Text(
                    "No pages this month.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
                )
            }
        } else {
            items(monthPages, key = { it.id }) { entry ->
                EntryCard(entry, today, onOpen, onPin, onCycleStatus = {})
            }
        }
    }
}

@Composable
private fun MonthCalendar(
    month: YearMonth,
    marked: Set<LocalDate>,
    today: LocalDate,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onDay: (LocalDate) -> Unit,
) {
    val locale = Locale.getDefault()
    val weekFields = WeekFields.of(locale)
    val first = weekFields.firstDayOfWeek
    val labels = (0 until 7).map { offset ->
        first.plus(offset.toLong()).getDisplayName(TextStyle.NARROW_STANDALONE, locale)
    }
    val offset = (month.atDay(1).dayOfWeek.value - first.value + 7) % 7
    val days = month.lengthOfMonth()
    val rows = (offset + days + 6) / 7
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPrev) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous month")
                }
                Text(
                    month.month.getDisplayName(TextStyle.FULL, locale) + " " + month.year,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onNext) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next month")
                }
            }
            Row {
                labels.forEach { label ->
                    Text(
                        label,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            for (row in 0 until rows) {
                Row {
                    for (col in 0 until 7) {
                        val dayNumber = row * 7 + col - offset + 1
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (dayNumber in 1..days) {
                                val date = month.atDay(dayNumber)
                                DayCell(date, date == today, date in marked, onDay)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(date: LocalDate, isToday: Boolean, marked: Boolean, onDay: (LocalDate) -> Unit) {
    val background = when {
        marked -> MaterialTheme.colorScheme.secondaryContainer
        else -> Color.Transparent
    }
    val borderColor = if (isToday) MaterialTheme.colorScheme.primary else Color.Transparent
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(background)
            .border(1.5.dp, borderColor, CircleShape)
            .clickable { onDay(date) },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            date.dayOfMonth.toString(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
fun EntryCard(
    entry: Entry,
    today: LocalDate,
    onOpen: (Entry) -> Unit,
    onPin: (Entry) -> Unit,
    onCycleStatus: (Entry) -> Unit,
) {
    val preview = Formatting.cardPreview(entry)
    Surface(
        onClick = { onOpen(entry) },
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (entry.pinned) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
            else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Column(Modifier.padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    Formatting.indicator(entry),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (entry.kind == EntryKind.IDEA) {
                        MaterialTheme.colorScheme.secondary
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                )
                Spacer(Modifier.weight(1f))
                if (entry.kind != EntryKind.JOURNAL && entry.status != null) {
                    AssistChip(
                        onClick = { onCycleStatus(entry) },
                        label = { Text(Formatting.statusLabel(entry.status)) },
                    )
                    Spacer(Modifier.width(4.dp))
                }
                IconButton(onClick = { onPin(entry) }) {
                    Icon(
                        imageVector = if (entry.pinned) Icons.Filled.Star else Icons.Outlined.Star,
                        contentDescription = if (entry.pinned) "Unpin" else "Pin",
                        tint = if (entry.pinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                Formatting.cardTitle(entry),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(end = 8.dp),
            )
            if (preview.isNotBlank()) {
                Text(
                    preview,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp, end = 8.dp),
                )
            }
            Row(
                modifier = Modifier.padding(top = 8.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    Formatting.dateLabel(entry, today),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (entry.mood != null) {
                    Text(entry.mood, modifier = Modifier.padding(start = 8.dp))
                }
                if (entry.tags.isNotEmpty()) {
                    Text(
                        entry.tags.joinToString(" · ", prefix = "  "),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingNotebook(modifier: Modifier) {
    com.jornaler.app.ui.PortraitStartup(modifier)
}

@Composable
private fun EmptyCopy(title: String, body: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

private fun emptyCopy(shelf: Shelf): Pair<String, String> = when (shelf) {
    Shelf.IN_PROGRESS -> "Nothing in the middle of being written." to
        "A scrap, a first line, a maybe. Tap + when one shows up."
    Shelf.POEMS -> "No poems yet." to "When you start a poem, it will wait for you here."
    Shelf.JOURNAL -> "No journal pages yet." to "Today is a blank page. One tap starts it."
    Shelf.FINISHED -> "Nothing marked finished." to
        "When a piece feels done, set it to Finished and it lands here."
}

@Preview(showBackground = true, widthDp = 390, heightDp = 840)
@Composable
private fun HomePreview() {
    JornalerTheme(ThemeMode.LIGHT) {
        HomeScreen(
            entries = SamplePages.entries(LocalDate.of(2026, 11, 8)),
            today = LocalDate.of(2026, 11, 8),
            themeMode = ThemeMode.LIGHT,
            onTheme = {},
            onOpen = {},
            onPin = {},
            onCycleStatus = {},
            onQuickAdd = {},
            onOpenJournal = {},
            onExport = {},
            onImport = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 840, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HomeDarkPreview() {
    JornalerTheme(ThemeMode.DARK) {
        HomeScreen(
            entries = SamplePages.entries(LocalDate.of(2026, 11, 8)),
            today = LocalDate.of(2026, 11, 8),
            themeMode = ThemeMode.DARK,
            onTheme = {},
            onOpen = {},
            onPin = {},
            onCycleStatus = {},
            onQuickAdd = {},
            onOpenJournal = {},
            onExport = {},
            onImport = {},
            initialShelf = Shelf.POEMS,
        )
    }
}
