package com.jornaler.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.jornaler.app.model.Entry

@Database(entities = [Entry::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class JornalerDatabase : RoomDatabase() {
    abstract fun entries(): EntryDao

    companion object {
        @Volatile
        private var instance: JornalerDatabase? = null

        fun get(context: Context): JornalerDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    JornalerDatabase::class.java,
                    "jornaler.db",
                ).build().also { instance = it }
            }
    }
}
