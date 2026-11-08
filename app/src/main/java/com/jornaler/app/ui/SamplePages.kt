package com.jornaler.app.ui

import com.jornaler.app.domain.Journals
import com.jornaler.app.model.Entry
import com.jornaler.app.model.EntryKind
import com.jornaler.app.model.PieceStatus
import java.time.LocalDate
import java.time.ZoneId

object SamplePages {
    fun entries(today: LocalDate, zone: ZoneId = ZoneId.systemDefault()): List<Entry> {
        fun at(date: LocalDate, hour: Int): Long =
            date.atTime(hour, 15).atZone(zone).toInstant().toEpochMilli()

        return listOf(
            Entry(
                id = "sample-dusk",
                kind = EntryKind.POEM,
                title = "The window at dusk",
                body = "The glass keeps a little of the day.\nI write my name in the fog\nand watch it leave first.",
                status = PieceStatus.DRAFTING,
                tags = listOf("evening"),
                pinned = true,
                createdAt = at(today.minusDays(3), 18),
                updatedAt = at(today, 16),
            ),
            Entry(
                id = "sample-fox",
                kind = EntryKind.IDEA,
                title = "",
                body = "A fox asleep between the encyclopedias.",
                status = PieceStatus.IDEA,
                tags = listOf("library"),
                createdAt = at(today.minusDays(1), 21),
                updatedAt = at(today, 11),
            ),
            Entry(
                id = "sample-rain",
                kind = EntryKind.POEM,
                title = "Library rain",
                body = "Every aisle sounds like turning a page.",
                status = PieceStatus.IDEA,
                createdAt = at(today.minusDays(4), 15),
                updatedAt = at(today.minusDays(1), 9),
            ),
            Entry(
                id = "sample-today",
                kind = EntryKind.JOURNAL,
                title = Journals.titleFor(today),
                body = "I sat on the rug and tried to describe the kettle. It sounds like a small train that is happy to wait.",
                mood = "😌",
                tags = listOf("home"),
                journalDate = today.toString(),
                createdAt = at(today, 8),
                updatedAt = at(today, 9),
            ),
            Entry(
                id = "sample-pocket",
                kind = EntryKind.POEM,
                title = "Pocket star",
                body = "I found a star in my coat pocket.\nIt was only a button.\nI sewed the night back on.",
                status = PieceStatus.FINISHED,
                tags = listOf("night"),
                createdAt = at(today.minusDays(12), 20),
                updatedAt = at(today.minusDays(6), 19),
            ),
        )
    }
}

val Moods = listOf("😊", "😌", "😔", "🥰", "😤", "😴", "🤔", "✨", "🌧️", "🌞", "💛", "🌿")
