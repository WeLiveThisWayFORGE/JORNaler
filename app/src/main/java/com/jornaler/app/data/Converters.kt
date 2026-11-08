package com.jornaler.app.data

import androidx.room.TypeConverter
import com.jornaler.app.model.EntryKind
import com.jornaler.app.model.PieceStatus
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

class Converters {
    private val json = Json

    @TypeConverter
    fun fromKind(value: EntryKind): String = value.name

    @TypeConverter
    fun toKind(value: String): EntryKind = EntryKind.valueOf(value)

    @TypeConverter
    fun fromStatus(value: PieceStatus?): String? = value?.name

    @TypeConverter
    fun toStatus(value: String?): PieceStatus? = value?.let { PieceStatus.valueOf(it) }

    @TypeConverter
    fun fromTags(value: List<String>): String = json.encodeToString(ListSerializer(String.serializer()), value)

    @TypeConverter
    fun toTags(value: String): List<String> =
        if (value.isBlank()) emptyList() else json.decodeFromString(ListSerializer(String.serializer()), value)
}
