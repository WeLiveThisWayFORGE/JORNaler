package com.jornaler.app.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

enum class EntryKind {
    POEM,
    JOURNAL,
    IDEA,
}

enum class PieceStatus {
    IDEA,
    DRAFTING,
    FINISHED,
}

@Serializable
@Entity(
    tableName = "entries",
    indices = [
        Index("journalDate"),
        Index("updatedAt"),
    ],
)
data class Entry(
    @PrimaryKey val id: String,
    val kind: EntryKind,
    val title: String = "",
    val body: String = "",
    val status: PieceStatus? = null,
    val tags: List<String> = emptyList(),
    val mood: String? = null,
    val pinned: Boolean = false,
    val journalDate: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
)
