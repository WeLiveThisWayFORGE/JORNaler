package com.jornaler.app

import com.jornaler.app.data.Converters
import com.jornaler.app.domain.ArchiveJson
import com.jornaler.app.domain.Drafts
import com.jornaler.app.domain.Formatting
import com.jornaler.app.domain.Journals
import com.jornaler.app.domain.Library
import com.jornaler.app.domain.Shelf
import com.jornaler.app.domain.Sharing
import com.jornaler.app.domain.WritingStats
import com.jornaler.app.domain.nextStatus
import com.jornaler.app.domain.normalizeTag
import com.jornaler.app.domain.parseTags
import com.jornaler.app.model.Entry
import com.jornaler.app.model.EntryKind
import com.jornaler.app.model.PieceStatus
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WritingStatsTest {
    @Test
    fun countsWordsAcrossBreaksAndIndentation() {
        assertEquals(0, WritingStats.words(""))
        assertEquals(0, WritingStats.words(" \n\t "))
        assertEquals(2, WritingStats.words("hello  world"))
        assertEquals(4, WritingStats.words("  one\n\ttwo  three\nfour"))
    }

    @Test
    fun countsEveryEditorLineIncludingBlanks() {
        assertEquals(0, WritingStats.lines(""))
        assertEquals(1, WritingStats.lines("a"))
        assertEquals(2, WritingStats.lines("a\nb"))
        assertEquals(3, WritingStats.lines("a\n\nb"))
        assertEquals(3, WritingStats.lines("a\nb\n"))
        assertEquals(2, WritingStats.lines("  indented\nsecond"))
    }

    @Test
    fun firstContentLineSkipsLeadingBlanks() {
        assertEquals("rose", WritingStats.firstContentLine("\n  rose\nmore"))
        assertEquals("", WritingStats.firstContentLine("\n  \n"))
    }
}

class LibraryTest {
    private val poem = entry("p", EntryKind.POEM, status = PieceStatus.DRAFTING, updatedAt = 10, pinned = false)
    private val idea = entry("i", EntryKind.IDEA, status = PieceStatus.IDEA, updatedAt = 30)
    private val finished = entry("f", EntryKind.POEM, status = PieceStatus.FINISHED, updatedAt = 40, pinned = true)
    private val journal = entry(
        "j",
        EntryKind.JOURNAL,
        status = null,
        journalDate = "2026-11-08",
        updatedAt = 50,
        tags = listOf("kettle"),
        mood = "😌",
    )

    @Test
    fun inProgressIsIdeasAndDraftsNewestFirstWithPinsOnTop() {
        val pinnedDraft = poem.copy(pinned = true, updatedAt = 1)
        val visible = Library.visible(listOf(idea, pinnedDraft, finished, journal), Shelf.IN_PROGRESS, "")
        assertEquals(listOf("p", "i"), visible.map { it.id })
    }

    @Test
    fun shelvesKeepJournalsOutOfPoemWorkflow() {
        val all = listOf(poem, idea, finished, journal)
        assertTrue(Library.visible(all, Shelf.POEMS, "").map { it.id }.containsAll(listOf("p", "f")))
        assertEquals(listOf("j"), Library.visible(all, Shelf.JOURNAL, "").map { it.id })
        assertEquals(listOf("f"), Library.visible(all, Shelf.FINISHED, "").map { it.id })
        assertFalse(Library.belongs(journal, Shelf.IN_PROGRESS))
        assertFalse(Library.belongs(journal, Shelf.FINISHED))
    }

    @Test
    fun searchLooksThroughTitleBodyTagsAndMood() {
        val pages = listOf(
            poem.copy(title = "Window", body = "fog on the glass"),
            idea.copy(body = "A fox in the stacks", tags = listOf("library")),
            journal,
        )
        assertEquals(setOf("p"), Library.visible(pages, Shelf.POEMS, "FOG").map { it.id }.toSet())
        assertEquals(setOf("i"), Library.visible(pages, Shelf.IN_PROGRESS, "Library").map { it.id }.toSet())
        assertEquals(setOf("j"), Library.visible(pages, Shelf.JOURNAL, "😌").map { it.id }.toSet())
        assertEquals(setOf("j"), Library.visible(pages, Shelf.JOURNAL, "kettle").map { it.id }.toSet())
    }

    @Test
    fun journalsInMonthUseTheJournalDate() {
        val older = journal.copy(id = "old", journalDate = "2026-10-02")
        val found = Library.journalsInMonth(listOf(journal, older, poem), 2026, 11)
        assertEquals(listOf("j"), found.map { it.id })
    }
}

class DraftAndJournalTest {
    @Test
    fun statusCyclesAndDefaults() {
        assertEquals(PieceStatus.DRAFTING, nextStatus(PieceStatus.IDEA))
        assertEquals(PieceStatus.FINISHED, nextStatus(PieceStatus.DRAFTING))
        assertEquals(PieceStatus.IDEA, nextStatus(PieceStatus.FINISHED))
    }

    @Test
    fun editingBodyBumpsTimeButPinningDoesNot() {
        val base = entry("1", body = "line", updatedAt = 10)
        val edited = Drafts.merge(base, base.title, "  line\n\tnext", base.status, base.tags, null, false, 99)
        assertEquals("  line\n\tnext", edited?.body)
        assertEquals(99L, edited?.updatedAt)
        val pinned = Drafts.merge(base, base.title, base.body, base.status, base.tags, null, true, 99)
        assertEquals(true, pinned?.pinned)
        assertEquals(10L, pinned?.updatedAt)
        assertNull(Drafts.merge(base, base.title, base.body, base.status, base.tags, null, false, 99))
    }

    @Test
    fun journalEditsDropStatus() {
        val base = entry("j", EntryKind.JOURNAL, status = PieceStatus.DRAFTING, body = "a")
        val saved = Drafts.merge(base, "Hello", "a\nb", PieceStatus.FINISHED, listOf("day"), "🌞", false, 5)
        assertNull(saved?.status)
        assertEquals("🌞", saved?.mood)
        assertEquals("a\nb", saved?.body)
    }

    @Test
    fun journalTitleUsesTheDate() {
        val title = Journals.titleFor(LocalDate.of(2026, 11, 8), Locale.US)
        assertEquals("Sunday, November 8", title)
    }

    @Test
    fun quickNoteAppendsAndKeepsACustomTitle() {
        val previous = Locale.getDefault()
        Locale.setDefault(Locale.US)
        try {
            val existing = Journals.newPage("id", LocalDate.of(2026, 11, 8), 20, null, "Morning.", "", ZoneOffset.UTC)
            assertEquals("Sunday, November 8", existing.title)
            val named = Journals.applyQuickNote(existing, "Evening.", "😌", "Kettle", 30)
            assertEquals("Morning.\n\nEvening.", named.body)
            assertEquals("Kettle", named.title)
            assertEquals("😌", named.mood)
            assertEquals(30L, named.updatedAt)
            val kept = Journals.applyQuickNote(named, "", null, "Other", 40)
            assertEquals(named, kept)
        } finally {
            Locale.setDefault(previous)
        }
    }

    @Test
    fun tagsAreTrimmedAndSplit() {
        assertEquals(listOf("river", "night"), parseTags(" #river, night\nRiver "))
        assertNull(normalizeTag("   "))
    }
}

class ArchiveAndSharingTest {
    @Test
    fun roundTripKeepsIndentationBreaksAndKinds() {
        val poem = entry(
            id = "poem",
            kind = EntryKind.POEM,
            title = "The window at dusk",
            body = "  fog\n\n\tname",
            status = PieceStatus.DRAFTING,
            tags = listOf("evening"),
            pinned = true,
        )
        val journal = entry(
            id = "day",
            kind = EntryKind.JOURNAL,
            title = "Sunday, November 8",
            body = "kettle",
            status = null,
            mood = "😌",
            journalDate = "2026-11-08",
        )
        val text = ArchiveJson.encode(listOf(poem, journal), 1_700_000_000_000)
        assertTrue(text.contains("\"app\": \"JORNaler\""))
        val decoded = ArchiveJson.decode(text, ZoneOffset.UTC)
        assertEquals(poem.body, decoded.first { it.id == "poem" }.body)
        assertEquals(PieceStatus.DRAFTING, decoded.first { it.id == "poem" }.status)
        assertEquals("😌", decoded.first { it.id == "day" }.mood)
        assertNull(decoded.first { it.id == "day" }.status)
    }

    @Test
    fun mergeKeepsTheNewerCopyAndCollapsesDuplicateIds() {
        val local = entry("1", body = "local newer", updatedAt = 50)
        val older = entry("1", body = "old", updatedAt = 10)
        val newerAgain = entry("1", body = "file wins", updatedAt = 80)
        val fresh = entry("2", body = "added", updatedAt = 5)
        val merged = ArchiveJson.merge(listOf(local), listOf(older, newerAgain, fresh, fresh.copy(body = "added last")))
        assertEquals(1, merged.added)
        assertEquals(1, merged.updated)
        assertEquals(0, merged.keptLocal)
        assertEquals("file wins", merged.toWrite.first { it.id == "1" }.body)
        assertEquals("added last", merged.toWrite.first { it.id == "2" }.body)
    }

    @Test
    fun equalTimestampsKeepThePhoneCopy() {
        val local = entry("1", body = "phone", updatedAt = 10)
        val incoming = entry("1", body = "file", updatedAt = 10)
        val merged = ArchiveJson.merge(listOf(local), listOf(incoming))
        assertEquals(1, merged.keptLocal)
        assertTrue(merged.toWrite.isEmpty())
    }

    @Test
    fun importNormalizesMissingJournalDateAndRejectsBadFiles() {
        val raw = """
            {"version":1,"app":"JORNaler","exportedAt":1,"entries":[
              {"id":"j","kind":"JOURNAL","title":"","body":"hi","status":"DRAFTING","createdAt":0,"updatedAt":1}
            ]}
        """.trimIndent()
        val decoded = ArchiveJson.decode(raw, ZoneOffset.UTC)
        assertEquals("1970-01-01", decoded.single().journalDate)
        assertNull(decoded.single().status)
        assertEquals(
            "This file is from a newer JORNaler.",
            runCatching { ArchiveJson.decode("""{"version":2,"exportedAt":1,"entries":[]}""") }.exceptionOrNull()?.message,
        )
        assertEquals(
            "A page in this file is missing an id.",
            runCatching {
                ArchiveJson.decode("""{"version":1,"exportedAt":1,"entries":[{"id":" ","kind":"IDEA","createdAt":1,"updatedAt":1}]}""")
            }.exceptionOrNull()?.message,
        )
    }

    @Test
    fun poemShareIsPlainTextWithTheAppName() {
        val text = Sharing.plainText(
            entry(title = "Pocket star", body = "I found a star.\nIt was a button.\n"),
        )
        assertEquals("Pocket star\n\nI found a star.\nIt was a button.\n\n— JORNaler\n", text)
    }

    @Test
    fun cardTitleFallsBackToTheFirstLine() {
        val idea = entry(title = "", body = "\nA fox asleep\nbetween books")
        assertEquals("A fox asleep", Formatting.cardTitle(idea))
        assertEquals("Today", Formatting.prettyDay(LocalDate.of(2026, 11, 8), LocalDate.of(2026, 11, 8)))
        assertEquals("Yesterday", Formatting.prettyDay(LocalDate.of(2026, 11, 7), LocalDate.of(2026, 11, 8)))
    }

    @Test
    fun tagConverterRoundTrips() {
        val converters = Converters()
        assertEquals(listOf("river", "night"), converters.toTags(converters.fromTags(listOf("river", "night"))))
        assertEquals(emptyList<String>(), converters.toTags(""))
        assertEquals(EntryKind.JOURNAL, converters.toKind(converters.fromKind(EntryKind.JOURNAL)))
        assertNull(converters.toStatus(converters.fromStatus(null)))
    }
}

private fun entry(
    id: String = "1",
    kind: EntryKind = EntryKind.POEM,
    title: String = "Title",
    body: String = "Body",
    status: PieceStatus? = PieceStatus.DRAFTING,
    tags: List<String> = emptyList(),
    mood: String? = null,
    pinned: Boolean = false,
    journalDate: String? = null,
    createdAt: Long = 1,
    updatedAt: Long = 1,
) = Entry(
    id = id,
    kind = kind,
    title = title,
    body = body,
    status = status,
    tags = tags,
    mood = mood,
    pinned = pinned,
    journalDate = journalDate,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
