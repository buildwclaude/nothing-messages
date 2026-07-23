package com.buildwclaude.messages.data.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

object ScheduledStatus {
    const val PENDING = 0
    const val SENT = 1
    const val FAILED = 2
    const val CANCELLED = 3
}

@Entity(tableName = "scheduled_messages")
data class ScheduledMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val threadId: Long?,             // may be null before first message in a thread
    val recipients: String,          // addresses joined with '|'
    val body: String,
    val sendAt: Long,                // epoch millis (absolute instant → DST/timezone safe)
    val subId: Int,
    val status: Int = ScheduledStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val attempts: Int = 0,
    val lastError: String? = null,
    val attachmentPaths: String? = null,  // local file paths joined with '|'
    val attachmentTypes: String? = null,  // mime types joined with '|'
)

@Entity(tableName = "thread_settings")
data class ThreadSettingEntity(
    @PrimaryKey val threadId: Long,
    val pinned: Boolean = false,
    val archived: Boolean = false,
    val muted: Boolean = false,
    val draftText: String? = null,
)

@Entity(tableName = "blocked_numbers")
data class BlockedNumberEntity(
    @PrimaryKey val normalizedAddress: String,
    val label: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

@Dao
interface ScheduledMessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ScheduledMessageEntity): Long

    @Update
    suspend fun update(entity: ScheduledMessageEntity)

    @Query("SELECT * FROM scheduled_messages WHERE id = :id")
    suspend fun byId(id: Long): ScheduledMessageEntity?

    @Query("SELECT * FROM scheduled_messages WHERE status = 0 ORDER BY sendAt ASC")
    suspend fun pending(): List<ScheduledMessageEntity>

    @Query("SELECT * FROM scheduled_messages WHERE status = 0 AND sendAt <= :now ORDER BY sendAt ASC")
    suspend fun overdue(now: Long): List<ScheduledMessageEntity>

    @Query("SELECT * FROM scheduled_messages WHERE status IN (0, 2) ORDER BY sendAt ASC")
    fun observeActive(): Flow<List<ScheduledMessageEntity>>

    @Query("SELECT * FROM scheduled_messages WHERE threadId = :threadId AND status = 0 ORDER BY sendAt ASC")
    fun observePendingForThread(threadId: Long): Flow<List<ScheduledMessageEntity>>

    @Query("UPDATE scheduled_messages SET status = :status WHERE id = :id")
    suspend fun setStatus(id: Long, status: Int)

    @Query("DELETE FROM scheduled_messages WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface ThreadSettingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ThreadSettingEntity)

    @Query("SELECT * FROM thread_settings WHERE threadId = :threadId")
    suspend fun byThread(threadId: Long): ThreadSettingEntity?

    @Query("SELECT * FROM thread_settings")
    fun observeAll(): Flow<List<ThreadSettingEntity>>

    @Query("SELECT * FROM thread_settings")
    suspend fun all(): List<ThreadSettingEntity>

    @Query("DELETE FROM thread_settings WHERE threadId = :threadId")
    suspend fun delete(threadId: Long)
}

@Dao
interface BlockedNumberDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: BlockedNumberEntity)

    @Query("SELECT * FROM blocked_numbers ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<BlockedNumberEntity>>

    @Query("SELECT * FROM blocked_numbers")
    suspend fun all(): List<BlockedNumberEntity>

    @Query("DELETE FROM blocked_numbers WHERE normalizedAddress = :address")
    suspend fun delete(address: String)

    @Query("SELECT COUNT(*) FROM blocked_numbers WHERE normalizedAddress = :address")
    suspend fun count(address: String): Int
}

@Database(
    entities = [ScheduledMessageEntity::class, ThreadSettingEntity::class, BlockedNumberEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scheduledMessageDao(): ScheduledMessageDao
    abstract fun threadSettingDao(): ThreadSettingDao
    abstract fun blockedNumberDao(): BlockedNumberDao
}
