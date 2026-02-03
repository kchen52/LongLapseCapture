package dev.ktown.longlapsecapture.data.repository

import dev.ktown.longlapsecapture.data.db.CaptureEntryDao
import dev.ktown.longlapsecapture.data.db.CaptureEntryEntity
import dev.ktown.longlapsecapture.data.db.ProjectDao
import dev.ktown.longlapsecapture.data.db.ProjectEntity
import dev.ktown.longlapsecapture.data.storage.PhotoStorage
import dev.ktown.longlapsecapture.ml.ReferenceSegmentationProcessor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

data class ProjectWithStats(
    val project: ProjectEntity,
    val captureCount: Int,
    val firstCaptureDate: String?,
    val firstCapturePath: String?
)

class LonglapseRepository(
    private val projectDao: ProjectDao,
    private val captureEntryDao: CaptureEntryDao,
    private val storage: PhotoStorage
) {
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val segmentationProcessor by lazy {
        ReferenceSegmentationProcessor(
            context = storage.context,
            storage = storage
        )
    }

    fun observeProjects(): Flow<List<ProjectEntity>> = projectDao.observeProjects()

    fun observeProjectsWithStats(): Flow<List<ProjectWithStats>> = flow {
        projectDao.observeProjects().collect { projects ->
            val withStats = projects.map { p ->
                val count = captureEntryDao.getCaptureCount(p.id)
                val firstDate = captureEntryDao.getFirstCaptureDate(p.id)
                val firstPath = captureEntryDao.getFirstCapturePath(p.id)
                ProjectWithStats(project = p, captureCount = count, firstCaptureDate = firstDate, firstCapturePath = firstPath)
            }
            emit(withStats)
        }
    }

    fun observeEntries(projectId: String): Flow<List<CaptureEntryEntity>> =
        captureEntryDao.observeEntries(projectId)

    suspend fun getProject(projectId: String): ProjectEntity? = projectDao.getProject(projectId)

    suspend fun createProject(
        name: String,
        reminderHour: Int?,
        reminderMinute: Int?
    ): ProjectEntity {
        val now = System.currentTimeMillis()
        val project = ProjectEntity(
            id = UUID.randomUUID().toString(),
            name = name,
            createdAt = now,
            referencePhotoPath = null,
            reminderHour = reminderHour,
            reminderMinute = reminderMinute,
            lastCaptureDate = null,
            lastCapturePath = null,
            preferredCameraFacing = null
        )
        projectDao.insertProject(project)
        return project
    }

    suspend fun saveCapture(
        projectId: String,
        localDate: LocalDate,
        filePath: String,
        setAsReference: Boolean
    ) {
        val dateString = localDate.format(dateFormatter)
        // Prevent duplicate captures for the same day to avoid constraint crashes.
        val existing = captureEntryDao.getEntryForDate(projectId, dateString)
        if (existing == null) {
            val entry = CaptureEntryEntity(
                projectId = projectId,
                localDate = dateString,
                filePath = filePath,
                createdAt = System.currentTimeMillis()
            )
            captureEntryDao.insertEntry(entry)
            projectDao.updateLastCapture(projectId, dateString, filePath)
            if (setAsReference) {
                projectDao.updateReferencePhoto(projectId, filePath)
                segmentationProcessor.generateSubjectMask(
                    referencePath = filePath,
                    projectId = projectId,
                    localDate = dateString
                )
            }
        }
    }

    suspend fun hasCaptureForToday(projectId: String): Boolean {
        val today = todayString()
        return captureEntryDao.getEntryForDate(projectId, today) != null
    }

    suspend fun updateReferencePhoto(projectId: String, filePath: String) {
        projectDao.updateReferencePhoto(projectId, filePath)
        // Also (re)generate subject mask when user changes the reference.
        val project = projectDao.getProject(projectId)
        val date = project?.lastCaptureDate ?: return
        segmentationProcessor.generateSubjectMask(
            referencePath = filePath,
            projectId = projectId,
            localDate = date
        )
    }

    suspend fun updatePreferredCameraFacing(projectId: String, facing: String) {
        projectDao.updatePreferredCameraFacing(projectId, facing)
    }

    suspend fun updateReminder(projectId: String, hour: Int?, minute: Int?) {
        projectDao.updateReminder(projectId, hour, minute)
    }

    fun todayString(): String = LocalDate.now().format(dateFormatter)

    fun photoFilePath(projectId: String, localDate: LocalDate): String {
        return storage.photoFileForDate(projectId, localDate.format(dateFormatter)).absolutePath
    }

    fun referenceMaskPath(project: ProjectEntity): String? {
        val date = project.lastCaptureDate ?: return null
        return storage.referenceMaskFile(project.id, date).absolutePath
    }

    fun exportFilePath(projectId: String, suffix: String = "timelapse"): String {
        val fileName = "${projectId}_$suffix.mp4"
        return storage.exportDirectory().resolve(fileName).absolutePath
    }
}
