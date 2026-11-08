package com.jornaler.app.domain

import com.jornaler.app.model.Entry
import com.jornaler.app.model.EntryKind
import com.jornaler.app.model.PieceStatus
import java.time.Instant
import java.time.ZoneId
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class ArchiveFile(
    val version: Int = ArchiveJson.VERSION,
    val app: String = "JORNaler",
    val exportedAt: Long,
    val entries: List<Entry> = emptyList(),
)

data class MergeResult(
    val toWrite: List<Entry>,
    val added: Int,
    val updated: Int,
    val keptLocal: Int,
)

object ArchiveJson {
    const val VERSION = 1

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(entries: List<Entry>, exportedAt: Long): String =
        json.encodeToString(ArchiveFile(version = VERSION, app = "JORNaler", exportedAt = exportedAt, entries = entries))

    fun decode(text: String, zone: ZoneId = ZoneId.systemDefault()): List<Entry> {
        val file = try {
            json.decodeFromString<ArchiveFile>(text)
        } catch (_: Exception) {
            throw IllegalArgumentException("This file isn't a JORNaler backup.")
        }
        if (file.version > VERSION) {
            throw IllegalArgumentException("This file is from a newer JORNaler.")
        }
        if (file.version < 1) {
            throw IllegalArgumentException("This file isn't a JORNaler backup.")
        }
        return file.entries.map { normalize(it, zone) }
    }

    fun merge(local: List<Entry>, incoming: List<Entry>): MergeResult {
        val collapsed = LinkedHashMap<String, Entry>()
        for (item in incoming) collapsed[item.id] = item
        val localById = local.associateBy { it.id }
        val toWrite = mutableListOf<Entry>()
        var added = 0
        var updated = 0
        var kept = 0
        for (item in collapsed.values) {
            val existing = localById[item.id]
            when {
                existing == null -> {
                    toWrite += item
                    added++
                }
                item.updatedAt > existing.updatedAt -> {
                    toWrite += item
                    updated++
                }
                else -> kept++
            }
        }
        return MergeResult(toWrite, added, updated, kept)
    }

    private fun normalize(entry: Entry, zone: ZoneId): Entry {
        if (entry.id.isBlank()) {
            throw IllegalArgumentException("A page in this file is missing an id.")
        }
        val tags = entry.tags.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        return when (entry.kind) {
            EntryKind.JOURNAL -> entry.copy(
                status = null,
                tags = tags,
                journalDate = entry.journalDate ?: Instant.ofEpochMilli(entry.createdAt).atZone(zone).toLocalDate().toString(),
            )
            EntryKind.POEM -> entry.copy(
                status = entry.status ?: PieceStatus.DRAFTING,
                mood = null,
                journalDate = null,
                tags = tags,
            )
            EntryKind.IDEA -> entry.copy(
                status = entry.status ?: PieceStatus.IDEA,
                mood = null,
                journalDate = null,
                tags = tags,
            )
        }
    }
}
