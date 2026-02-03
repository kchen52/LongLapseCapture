package dev.ktown.longlapsecapture.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ProjectEntity::class, CaptureEntryEntity::class],
    version = 1
)
abstract class LonglapseDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun captureEntryDao(): CaptureEntryDao
}
