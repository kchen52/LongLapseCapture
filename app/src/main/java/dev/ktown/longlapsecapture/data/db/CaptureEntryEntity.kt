package dev.ktown.longlapsecapture.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "capture_entries",
    indices = [Index(value = ["projectId", "localDate"], unique = true)]
)
data class CaptureEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: String,
    val localDate: String,
    val filePath: String,
    val createdAt: Long
)
