package com.example.dosimetrov2

import android.content.Context
import androidx.room.*

@Entity(tableName = "sessions")
data class DosimetrySession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long,
    val dose: Float,
    val criterion: String,
    val dataJson: String
)

@Dao
interface DosimetryDao {
    @Insert
    suspend fun insert(session: DosimetrySession)

    @Query("SELECT * FROM sessions ORDER BY timestamp DESC")
    suspend fun getAllSessions(): List<DosimetrySession>

    @Query("DELETE FROM sessions")
    suspend fun deleteAll()
}

@Database(entities = [DosimetrySession::class], version = 1)
abstract class DosimetryDatabase : RoomDatabase() {
    abstract fun dosimetryDao(): DosimetryDao

    companion object {
        @Volatile
        private var INSTANCE: DosimetryDatabase? = null

        fun getDatabase(context: Context): DosimetryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DosimetryDatabase::class.java,
                    "dosimetry_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
