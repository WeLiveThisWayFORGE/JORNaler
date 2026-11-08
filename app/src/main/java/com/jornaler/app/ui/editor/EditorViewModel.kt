package com.jornaler.app.ui.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jornaler.app.data.EntryRepository
import com.jornaler.app.domain.Drafts
import com.jornaler.app.domain.parseTags
import com.jornaler.app.model.Entry
import com.jornaler.app.model.EntryKind
import com.jornaler.app.model.PieceStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

enum class SaveState {
    SAVED,
    SAVING,
}

@OptIn(FlowPreview::class)
class EditorViewModel(
    private val repository: EntryRepository,
    private val appScope: CoroutineScope,
    private val entryId: String,
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {
    var title by mutableStateOf("")
    var body by mutableStateOf("")
    var status by mutableStateOf<PieceStatus?>(null)
    var tags by mutableStateOf<List<String>>(emptyList())
    var mood by mutableStateOf<String?>(null)
    var pinned by mutableStateOf(false)
    var kind by mutableStateOf(EntryKind.IDEA)
    var journalDate by mutableStateOf<String?>(null)
    var loading by mutableStateOf(true)
        private set
    var missing by mutableStateOf(false)
        private set
    var saveState by mutableStateOf(SaveState.SAVED)
        private set

    private var baseline: Entry? = null
    private val gate = Mutex()
    private var ticket = 0
    private var appliedTicket = 0

    init {
        viewModelScope.launch {
            val entry = repository.get(entryId)
            if (entry == null) {
                missing = true
                loading = false
                return@launch
            }
            title = entry.title
            body = entry.body
            status = entry.status
            tags = entry.tags
            mood = entry.mood
            pinned = entry.pinned
            kind = entry.kind
            journalDate = entry.journalDate
            baseline = entry
            loading = false
            snapshotFlow { captureFields() }
                .debounce(450)
                .collect { fields ->
                    val wrote = write(issue(fields))
                    if (wrote) saveState = SaveState.SAVED
                }
        }
    }

    fun flush() {
        if (baseline == null) return
        appScope.launch { write(issue(captureFields())) }
    }

    fun chooseStatus(next: PieceStatus) {
        status = next
    }

    fun togglePin() {
        pinned = !pinned
    }

    fun chooseMood(value: String?) {
        mood = if (mood == value) null else value
    }

    fun addTags(raw: String) {
        val incoming = parseTags(raw)
        if (incoming.isEmpty()) return
        tags = (tags + incoming).distinctBy { it.lowercase() }.take(12)
    }

    fun removeTag(tag: String) {
        tags = tags.filterNot { it.equals(tag, ignoreCase = true) }
    }

    fun currentDraft(): Entry? {
        val base = baseline ?: return null
        return base.copy(
            title = title,
            body = body,
            status = if (kind == EntryKind.JOURNAL) null else status,
            tags = tags,
            mood = if (kind == EntryKind.JOURNAL) mood else null,
            pinned = pinned,
        )
    }

    suspend fun delete() {
        repository.delete(entryId)
    }

    private fun captureFields() = DraftFields(title, body, status, tags, mood, pinned)

    private fun issue(fields: DraftFields): Held {
        ticket += 1
        return Held(ticket, fields)
    }

    private suspend fun write(held: Held): Boolean = gate.withLock {
        if (held.ticket < appliedTicket) return false
        val base = baseline ?: return false
        val next = Drafts.merge(
            base = base,
            title = held.fields.title,
            body = held.fields.body,
            status = held.fields.status,
            tags = held.fields.tags,
            mood = held.fields.mood,
            pinned = held.fields.pinned,
            now = clock(),
        )
        if (next == null) {
            appliedTicket = held.ticket
            return false
        }
        withContext(Dispatchers.Main.immediate) { saveState = SaveState.SAVING }
        repository.save(next)
        baseline = next
        appliedTicket = held.ticket
        withContext(Dispatchers.Main.immediate) { saveState = SaveState.SAVED }
        true
    }

    private data class DraftFields(
        val title: String,
        val body: String,
        val status: PieceStatus?,
        val tags: List<String>,
        val mood: String?,
        val pinned: Boolean,
    )

    private data class Held(val ticket: Int, val fields: DraftFields)

    companion object {
        fun factory(
            repository: EntryRepository,
            appScope: CoroutineScope,
            id: String,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                EditorViewModel(repository, appScope, id) as T
        }
    }
}
