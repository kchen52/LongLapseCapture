package dev.ktown.longlapsecapture.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY createdAt DESC")
    fun observeProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :projectId LIMIT 1")
    suspend fun getProject(projectId: String): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity)

    @Query("UPDATE projects SET referencePhotoPath = :path WHERE id = :projectId")
    suspend fun updateReferencePhoto(projectId: String, path: String)

    @Query("UPDATE projects SET lastCaptureDate = :date, lastCapturePath = :path WHERE id = :projectId")
    suspend fun updateLastCapture(projectId: String, date: String, path: String)

    @Query("UPDATE projects SET reminderHour = :hour, reminderMinute = :minute WHERE id = :projectId")
    suspend fun updateReminder(projectId: String, hour: Int?, minute: Int?)
}
