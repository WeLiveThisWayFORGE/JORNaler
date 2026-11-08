package com.jornaler.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jornaler.app.model.Entry
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryDao {
    @Query("SELECT * FROM entries")
    fun observeAll(): Flow<List<Entry>>

    @Query("SELECT * FROM entries")
    suspend fun getAll(): List<Entry>

    @Query("SELECT * FROM entries WHERE id = :id")
    suspend fun getById(id: String): Entry?

    @Query("SELECT * FROM entries WHERE kind = 'JOURNAL' AND journalDate = :date")
    suspend fun journalsOn(date: String): List<Entry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: Entry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entries: List<Entry>)

    @Query("DELETE FROM entries WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT COUNT(*) FROM entries")
    suspend fun count(): Int
}
