package dev.ktown.longlapsecapture.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.ktown.longlapsecapture.data.db.ProjectEntity
import dev.ktown.longlapsecapture.di.ServiceLocator
import dev.ktown.longlapsecapture.ui.screens.CameraScreen
import dev.ktown.longlapsecapture.ui.screens.ProjectDetailScreen
import dev.ktown.longlapsecapture.ui.screens.ProjectListScreen
import kotlinx.coroutines.flow.collectLatest

private sealed class Screen {
    data object ProjectList : Screen()
    data class ProjectDetail(val projectId: String) : Screen()
    data class Camera(val projectId: String) : Screen()
}

@Composable
fun LonglapseApp(
    startProjectId: String?,
    startInCamera: Boolean
) {
    val repository = remember { ServiceLocator.repository() }
    var screen by remember { mutableStateOf<Screen>(Screen.ProjectList) }
    var promptProject by remember { mutableStateOf<ProjectEntity?>(null) }
    var reminderDismissedThisLaunch by remember { mutableStateOf(false) }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(startProjectId, startInCamera) {
        if (startProjectId != null) {
            screen = if (startInCamera) {
                Screen.Camera(startProjectId)
            } else {
                Screen.ProjectDetail(startProjectId)
            }
        }
    }

    LaunchedEffect(Unit) {
        repository.observeProjects().collectLatest { projects ->
            if (reminderDismissedThisLaunch) return@collectLatest
            val today = repository.todayString()
            if (promptProject == null) {
                promptProject = projects.firstOrNull { it.lastCaptureDate != today }
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        when (val current = screen) {
            is Screen.ProjectList -> ProjectListScreen(
                repository = repository,
                onOpenProject = { screen = Screen.ProjectDetail(it) },
                onOpenCamera = { screen = Screen.Camera(it) },
                onRequestNotifications = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            )
            is Screen.ProjectDetail -> ProjectDetailScreen(
                repository = repository,
                projectId = current.projectId,
                onBack = { screen = Screen.ProjectList },
                onOpenCamera = { screen = Screen.Camera(current.projectId) }
            )
            is Screen.Camera -> CameraScreen(
                repository = repository,
                projectId = current.projectId,
                onBack = { screen = Screen.ProjectDetail(current.projectId) }
            )
        }
    }

    if (promptProject != null && screen is Screen.ProjectList) {
        AlertDialog(
            onDismissRequest = {
                reminderDismissedThisLaunch = true
                promptProject = null
            },
            title = { Text("Take today's photo?") },
            text = { Text("Capture a new photo for ${promptProject?.name}.") },
            confirmButton = {
                TextButton(onClick = {
                    reminderDismissedThisLaunch = true
                    val projectId = promptProject?.id
                    promptProject = null
                    if (projectId != null) {
                        screen = Screen.Camera(projectId)
                    }
                }) {
                    Text("Take photo")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    reminderDismissedThisLaunch = true
                    promptProject = null
                }) {
                    Text("Later")
                }
            }
        )
    }
}
