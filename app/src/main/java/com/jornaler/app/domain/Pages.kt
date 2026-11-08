package com.jornaler.app.domain

import com.jornaler.app.model.Entry
import com.jornaler.app.model.EntryKind
import com.jornaler.app.model.PieceStatus
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class Shelf {
    IN_PROGRESS,
    POEMS,
    JOURNAL,
    FINISHED,
}

object Library {
    private val order = compareByDescending<Entry> { it.pinned }.thenByDescending { it.updatedAt }

    fun belongs(entry: Entry, shelf: Shelf): Boolean = when (shelf) {
        Shelf.IN_PROGRESS ->
            entry.kind != EntryKind.JOURNAL &&
                (entry.status == PieceStatus.IDEA || entry.status == PieceStatus.DRAFTING)
        Shelf.POEMS -> entry.kind == EntryKind.POEM
        Shelf.JOURNAL -> entry.kind == EntryKind.JOURNAL
        Shelf.FINISHED -> entry.kind != EntryKind.JOURNAL && entry.status == PieceStatus.FINISHED
    }

    fun matches(entry: Entry, query: String): Boolean {
        val needle = query.trim()
        if (needle.isEmpty()) return true
        if (entry.title.contains(needle, ignoreCase = true)) return true
        if (entry.body.contains(needle, ignoreCase = true)) return true
        if (entry.tags.any { it.contains(needle, ignoreCase = true) }) return true
        if (entry.mood?.contains(needle) == true) return true
        return false
    }

    fun visible(entries: List<Entry>, shelf: Shelf, query: String): List<Entry> {
        val filtered = if (query.isBlank()) {
            entries.filter { belongs(it, shelf) }
        } else {
            entries.filter { matches(it, query) }
        }
        return filtered.sortedWith(order)
    }

    fun journalsInMonth(entries: List<Entry>, year: Int, month: Int): List<Entry> {
        val prefix = "%04d-%02d".format(year, month)
        return entries
            .filter { it.kind == EntryKind.JOURNAL && it.journalDate?.startsWith(prefix) == true }
            .sortedWith(compareByDescending<Entry> { it.journalDate }.thenByDescending { it.updatedAt })
    }
}

object WritingStats {
    fun words(body: String): Int = body.split(Regex("\\s+")).count { it.isNotBlank() }

    fun lines(body: String): Int {
        if (body.isEmpty()) return 0
        return body.replace("\r\n", "\n").replace('\r', '\n').split('\n').size
    }

    fun firstContentLine(body: String): String =
        body.lineSequence().map { it.trim() }.firstOrNull { it.isNotEmpty() }.orEmpty()
}

fun nextStatus(status: PieceStatus): PieceStatus = when (status) {
    PieceStatus.IDEA -> PieceStatus.DRAFTING
    PieceStatus.DRAFTING -> PieceStatus.FINISHED
    PieceStatus.FINISHED -> PieceStatus.IDEA
}

fun defaultStatus(kind: EntryKind): PieceStatus? = when (kind) {
    EntryKind.POEM -> PieceStatus.DRAFTING
    EntryKind.IDEA -> PieceStatus.IDEA
    EntryKind.JOURNAL -> null
}

fun normalizeTag(raw: String): String? {
    val cleaned = raw.trim().trimStart('#').trim()
    if (cleaned.isEmpty() || cleaned.length > 32) return null
    return cleaned
}

fun parseTags(raw: String): List<String> =
    raw.split(',', '\n')
        .mapNotNull { normalizeTag(it) }
        .distinctBy { it.lowercase() }

object Drafts {
    /**
     * Returns the entry to store, or null when nothing the writer changed needs saving.
     * Pinning alone does not bump the updated time, so a pin does not reorder the page.
     * Body text is kept exactly, including line breaks and indentation.
     */
    fun merge(
        base: Entry,
        title: String,
        body: String,
        status: PieceStatus?,
        tags: List<String>,
        mood: String?,
        pinned: Boolean,
        now: Long,
    ): Entry? {
        val normalizedStatus = if (base.kind == EntryKind.JOURNAL) null else status
        val normalizedMood = if (base.kind == EntryKind.JOURNAL) mood else null
        val contentChanged = title != base.title ||
            body != base.body ||
            normalizedStatus != base.status ||
            tags != base.tags ||
            normalizedMood != base.mood
        val pinChanged = pinned != base.pinned
        if (!contentChanged && !pinChanged) return null
        return base.copy(
            title = title,
            body = body,
            status = normalizedStatus,
            tags = tags,
            mood = normalizedMood,
            pinned = pinned,
            updatedAt = if (contentChanged) now else base.updatedAt,
        )
    }
}

object Journals {
    fun titleFor(date: LocalDate, locale: Locale = Locale.getDefault()): String =
        date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d", locale))

    fun newPage(
        id: String,
        date: LocalDate,
        now: Long,
        mood: String?,
        body: String,
        title: String,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Entry {
        val created = date.atStartOfDay(zone).toInstant().toEpochMilli().coerceAtMost(now)
        return Entry(
            id = id,
            kind = EntryKind.JOURNAL,
            title = title.ifBlank { titleFor(date) },
            body = body,
            status = null,
            tags = emptyList(),
            mood = mood?.takeIf { it.isNotBlank() },
            pinned = false,
            journalDate = date.toString(),
            createdAt = created,
            updatedAt = now,
        )
    }

    fun applyQuickNote(existing: Entry, body: String, mood: String?, title: String, now: Long): Entry {
        var next = existing
        if (body.isNotBlank()) {
            val combined = if (next.body.isBlank()) body else next.body.trimEnd() + "\n\n" + body
            next = next.copy(body = combined)
        }
        if (!mood.isNullOrBlank()) {
            next = next.copy(mood = mood)
        }
        if (title.isNotBlank()) {
            val generated = existing.journalDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                ?.let { titleFor(it) }
            if (existing.title.isBlank() || existing.title == generated) {
                next = next.copy(title = title)
            }
        }
        if (next == existing) return existing
        return next.copy(updatedAt = now)
    }
}

object Sharing {
    fun plainText(entry: Entry): String = buildString {
        if (entry.title.isNotBlank()) {
            append(entry.title.trim())
            append("\n\n")
        }
        append(entry.body.trimEnd())
        if (isNotEmpty() && !endsWith("\n")) append('\n')
        append("\n— JORNaler\n")
    }
}

object Formatting {
    fun prettyDay(date: LocalDate, today: LocalDate): String = when (date) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> if (date.year == today.year) {
            date.format(DateTimeFormatter.ofPattern("MMM d"))
        } else {
            date.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
        }
    }

    fun dateFromEpoch(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): LocalDate =
        Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()

    fun cardTitle(entry: Entry): String {
        if (entry.title.isNotBlank()) return entry.title.trim()
        if (entry.kind == EntryKind.JOURNAL) {
            entry.journalDate?.let { key ->
                runCatching { LocalDate.parse(key) }.getOrNull()?.let { return Journals.titleFor(it) }
            }
        }
        return WritingStats.firstContentLine(entry.body).ifBlank { "Untitled" }
    }

    fun cardPreview(entry: Entry): String {
        val body = entry.body.trimEnd()
        if (body.isBlank()) return ""
        if (entry.title.isBlank()) {
            val lines = body.lines()
            return lines.drop(1).joinToString("\n").trim()
        }
        val first = WritingStats.firstContentLine(body)
        if (first.equals(entry.title.trim(), ignoreCase = true)) {
            return body.lines().drop(1).joinToString("\n").trim()
        }
        return body.trim()
    }

    fun dateLabel(entry: Entry, today: LocalDate, zone: ZoneId = ZoneId.systemDefault()): String {
        if (entry.kind == EntryKind.JOURNAL && entry.journalDate != null) {
            val date = runCatching { LocalDate.parse(entry.journalDate) }.getOrNull()
            if (date != null) return prettyDay(date, today)
        }
        return "Updated " + prettyDay(dateFromEpoch(entry.updatedAt, zone), today)
    }

    fun statusLabel(status: PieceStatus?): String = when (status) {
        PieceStatus.IDEA -> "Idea"
        PieceStatus.DRAFTING -> "Drafting"
        PieceStatus.FINISHED -> "Finished"
        null -> ""
    }

    fun kindLabel(kind: EntryKind): String = when (kind) {
        EntryKind.POEM -> "Poem"
        EntryKind.JOURNAL -> "Journal"
        EntryKind.IDEA -> "Idea"
    }

    fun indicator(entry: Entry): String {
        val kind = kindLabel(entry.kind)
        val status = statusLabel(entry.status)
        if (entry.kind == EntryKind.JOURNAL || status.isBlank()) return kind
        if (entry.kind == EntryKind.IDEA && entry.status == PieceStatus.IDEA) return "Idea"
        return "$kind · $status"
    }
}
