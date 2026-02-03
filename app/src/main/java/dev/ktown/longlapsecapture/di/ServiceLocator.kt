package dev.ktown.longlapsecapture.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dev.ktown.longlapsecapture.data.db.LonglapseDatabase
import dev.ktown.longlapsecapture.data.repository.LonglapseRepository
import dev.ktown.longlapsecapture.data.storage.PhotoStorage

object ServiceLocator {
    @Volatile
    private var database: LonglapseDatabase? = null

    @Volatile
    private var repository: LonglapseRepository? = null

    fun init(context: Context) {
        if (database == null) {
            database = Room.databaseBuilder(
                context.applicationContext,
                LonglapseDatabase::class.java,
                "longlapse.db"
            )
                .addMigrations(object : Migration(1, 2) {
                    override fun migrate(db: SupportSQLiteDatabase) {
                        db.execSQL("ALTER TABLE projects ADD COLUMN preferredCameraFacing TEXT")
                    }
                })
                .build()
        }
        if (repository == null) {
            val storage = PhotoStorage(context.applicationContext)
            repository = LonglapseRepository(
                projectDao = requireNotNull(database).projectDao(),
                captureEntryDao = requireNotNull(database).captureEntryDao(),
                storage = storage
            )
        }
    }

    fun repository(): LonglapseRepository = requireNotNull(repository) {
        "ServiceLocator not initialized"
    }
}
