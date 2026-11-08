package com.jornaler.app.data

import com.jornaler.app.domain.ArchiveJson
import com.jornaler.app.domain.Journals
import com.jornaler.app.domain.MergeResult
import com.jornaler.app.domain.defaultStatus
import com.jornaler.app.model.Entry
import com.jornaler.app.model.EntryKind
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class EntryRepository(
    private val dao: EntryDao,
    private val clock: () -> Long = System::currentTimeMillis,
    private val newId: () -> String = { UUID.randomUUID().toString() },
    private val zone: ZoneId = ZoneId.systemDefault(),
) {
    private val changeSignals = MutableSharedFlow<Unit>(extraBufferCapacity = 64)
    val changes = changeSignals.asSharedFlow()

    fun observeAll(): Flow<List<Entry>> = dao.observeAll()

    suspend fun snapshot(): List<Entry> = dao.getAll()

    suspend fun get(id: String): Entry? = dao.getById(id)

    suspend fun save(entry: Entry) {
        dao.upsert(entry)
        changed()
    }

    suspend fun delete(id: String) {
        dao.delete(id)
        changed()
    }

    suspend fun create(
        kind: EntryKind,
        title: String,
        body: String,
        mood: String? = null,
        tags: List<String> = emptyList(),
    ): Entry {
        val now = clock()
        val entry = Entry(
            id = newId(),
            kind = kind,
            title = title,
            body = body,
            status = defaultStatus(kind),
            tags = tags,
            mood = if (kind == EntryKind.JOURNAL) mood else null,
            pinned = false,
            journalDate = null,
            createdAt = now,
            updatedAt = now,
        )
        dao.upsert(entry)
        changed()
        return entry
    }

    suspend fun openOrCreateJournal(
        date: LocalDate,
        mood: String? = null,
        body: String = "",
        title: String = "",
    ): Entry {
        val existing = dao.journalsOn(date.toString()).maxByOrNull { it.updatedAt }
        if (existing == null) {
            val page = Journals.newPage(newId(), date, clock(), mood, body, title, zone)
            dao.upsert(page)
            changed()
            return page
        }
        val next = Journals.applyQuickNote(existing, body, mood, title, clock())
        if (next != existing) {
            dao.upsert(next)
            changed()
        }
        return next
    }

    suspend fun exportJson(): String {
        val entries = dao.getAll().sortedBy { it.createdAt }
        return ArchiveJson.encode(entries, clock())
    }

    suspend fun importJson(text: String): MergeResult {
        val incoming = ArchiveJson.decode(text, zone)
        val local = dao.getAll()
        val merged = ArchiveJson.merge(local, incoming)
        if (merged.toWrite.isNotEmpty()) {
            dao.upsertAll(merged.toWrite)
            changed()
        }
        return merged
    }

    suspend fun seedIfEmpty(entries: List<Entry>) {
        if (dao.count() > 0) return
        if (entries.isEmpty()) return
        dao.upsertAll(entries)
        changed()
    }

    private fun changed() {
        changeSignals.tryEmit(Unit)
    }
}
