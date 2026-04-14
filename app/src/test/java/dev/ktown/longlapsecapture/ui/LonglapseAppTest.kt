package dev.ktown.longlapsecapture.ui

import dev.ktown.longlapsecapture.data.db.ProjectEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LonglapseAppTest {

    @Test
    fun resolveLaunchTarget_opensCamera_whenNotificationTapAndNoCaptureToday() {
        val target = resolveLaunchTarget(
            startProjectId = "project-1",
            startInCamera = true,
            hasCaptureForToday = false
        )

        assertEquals(LaunchTarget.CAMERA, target)
    }

    @Test
    fun resolveLaunchTarget_opensHome_whenNotificationTapAndAlreadyCapturedToday() {
        val target = resolveLaunchTarget(
            startProjectId = "project-1",
            startInCamera = true,
            hasCaptureForToday = true
        )

        assertEquals(LaunchTarget.PROJECT_LIST, target)
    }

    @Test
    fun resolveLaunchTarget_opensProjectDetail_whenNotCameraLaunch() {
        val target = resolveLaunchTarget(
            startProjectId = "project-1",
            startInCamera = false,
            hasCaptureForToday = true
        )

        assertEquals(LaunchTarget.PROJECT_DETAIL, target)
    }

    @Test
    fun selectPromptProject_returnsNull_whenEveryProjectCapturedToday() {
        val today = "2026-04-13"
        val projects = listOf(
            project(id = "a", lastCaptureDate = today),
            project(id = "b", lastCaptureDate = today)
        )

        val selected = selectPromptProject(
            projects = projects,
            today = today,
            currentPromptId = null
        )

        assertNull(selected)
    }

    @Test
    fun selectPromptProject_switchesAwayFromCurrent_whenCurrentNowCaptured() {
        val today = "2026-04-13"
        val projects = listOf(
            project(id = "a", lastCaptureDate = today),
            project(id = "b", lastCaptureDate = null)
        )

        val selected = selectPromptProject(
            projects = projects,
            today = today,
            currentPromptId = "a"
        )

        assertEquals("b", selected?.id)
    }

    private fun project(id: String, lastCaptureDate: String?): ProjectEntity {
        return ProjectEntity(
            id = id,
            name = "Project $id",
            createdAt = 0L,
            referencePhotoPath = null,
            reminderHour = null,
            reminderMinute = null,
            lastCaptureDate = lastCaptureDate,
            lastCapturePath = null,
            preferredCameraFacing = null
        )
    }
}
