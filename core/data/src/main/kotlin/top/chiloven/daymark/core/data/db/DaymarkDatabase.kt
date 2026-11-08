package top.chiloven.daymark.core.data.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val dateEpochDay: Long,
    val direction: String,
    val recurrenceFrequency: String,
    val recurrenceInterval: Int,
    val recurrenceEndEpochDay: Long?,
    val categoryId: Long?,
    val note: String,
    val isPinned: Boolean,
    val reminderOffsetDays: Int?,
    val reminderTimeMinutes: Int?,
    val createdAt: Long,
    val updatedAt: Long,
)

@Dao
interface EventDao {

    @Query("SELECT * FROM events ORDER BY isPinned DESC, dateEpochDay ASC")
    fun observeAll(): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE id = :id")
    fun observeById(id: Long): Flow<EventEntity?>

    @Query(
        "SELECT * FROM events " +
                "WHERE title LIKE '%' || :query || '%' OR note LIKE '%' || :query || '%' " +
                "ORDER BY isPinned DESC, dateEpochDay ASC",
    )
    fun search(query: String): Flow<List<EventEntity>>

    @Upsert
    suspend fun upsert(entity: EventEntity): Long

    @Delete
    suspend fun delete(entity: EventEntity)

    @Upsert
    suspend fun upsertAll(entities: List<EventEntity>)

    @Query("DELETE FROM events WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM events")
    suspend fun deleteAll()

    @Query("UPDATE events SET isPinned = :pinned, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setPinned(id: Long, pinned: Boolean, updatedAt: Long)

}

@Database(
    entities = [EventEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class DaymarkDatabase : RoomDatabase() {

    abstract fun eventDao(): EventDao

}
