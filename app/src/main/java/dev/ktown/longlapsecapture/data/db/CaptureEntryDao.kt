package dev.ktown.longlapsecapture.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CaptureEntryDao {
    @Query("SELECT * FROM capture_entries WHERE projectId = :projectId ORDER BY localDate ASC")
    fun observeEntries(projectId: String): Flow<List<CaptureEntryEntity>>

    @Query("SELECT * FROM capture_entries WHERE projectId = :projectId AND localDate = :localDate LIMIT 1")
    suspend fun getEntryForDate(projectId: String, localDate: String): CaptureEntryEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertEntry(entry: CaptureEntryEntity)

    @Query("SELECT COUNT(*) FROM capture_entries WHERE projectId = :projectId")
    suspend fun getCaptureCount(projectId: String): Int

    @Query("SELECT localDate FROM capture_entries WHERE projectId = :projectId ORDER BY localDate ASC LIMIT 1")
    suspend fun getFirstCaptureDate(projectId: String): String?

    @Query("SELECT filePath FROM capture_entries WHERE projectId = :projectId ORDER BY localDate ASC LIMIT 1")
    suspend fun getFirstCapturePath(projectId: String): String?
}
