package dev.ktown.longlapsecapture.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdAt: Long,
    val referencePhotoPath: String?,
    val reminderHour: Int?,
    val reminderMinute: Int?,
    val lastCaptureDate: String?,
    val lastCapturePath: String?,
    val preferredCameraFacing: String?
)
