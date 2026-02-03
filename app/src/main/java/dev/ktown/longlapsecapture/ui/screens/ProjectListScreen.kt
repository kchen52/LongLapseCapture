package dev.ktown.longlapsecapture.ui.screens

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.ktown.longlapsecapture.data.repository.LonglapseRepository
import dev.ktown.longlapsecapture.data.repository.ProjectWithStats
import dev.ktown.longlapsecapture.reminder.ReminderScheduler
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun ProjectListScreen(
    repository: LonglapseRepository,
    onOpenProject: (String) -> Unit,
    onOpenCamera: (String) -> Unit,
    onRequestNotifications: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var projectsWithStats by remember { mutableStateOf<List<ProjectWithStats>>(emptyList()) }
    var showCreateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        repository.observeProjectsWithStats().collectLatest { projectsWithStats = it }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Create project"
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Your timelapse projects",
                style = MaterialTheme.typography.titleLarge
            )
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(projectsWithStats, key = { it.project.id }) { item ->
                    ProjectCard(
                        item = item,
                        onOpen = { onOpenProject(item.project.id) },
                        onCapture = { onOpenCamera(item.project.id) }
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateProjectDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, hour, minute ->
                showCreateDialog = false
                onRequestNotifications()
                scope.launch {
                    val project = repository.createProject(name, hour, minute)
                    if (hour != null && minute != null) {
                        ReminderScheduler.scheduleDailyReminder(
                            context = context,
                            projectId = project.id,
                            projectName = project.name,
                            hour = hour,
                            minute = minute
                        )
                    }
                }
            }
        )
    }
}

private const val EXPORT_FPS = 24

@Composable
private fun ProjectCard(
    item: ProjectWithStats,
    onOpen: () -> Unit,
    onCapture: () -> Unit
) {
    val project = item.project
    val firstDateFormatted = item.firstCaptureDate?.let { formatDisplayDate(it) } ?: "—"
    val videoDurationFormatted = formatVideoDuration(item.captureCount)

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = project.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "First picture: $firstDateFormatted",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Pictures: ${item.captureCount}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Export length (at ${EXPORT_FPS} fps): $videoDurationFormatted",
                    style = MaterialTheme.typography.bodyMedium
                )
                val lastCapture = project.lastCaptureDate ?: "No captures yet"
                Text(text = "Last capture: $lastCapture", style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onOpen) {
                        Text("Details")
                    }
                    TextButton(onClick = onCapture) {
                        Text("Capture")
                    }
                }
            }
            item.firstCapturePath?.let { path ->
                AsyncImage(
                    model = path,
                    contentDescription = "First capture",
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

private fun formatDisplayDate(isoDate: String): String {
    return try {
        val date = java.time.LocalDate.parse(isoDate)
        date.format(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy"))
    } catch (_: Exception) {
        isoDate
    }
}

private fun formatVideoDuration(captureCount: Int): String {
    if (captureCount == 0) return "—"
    val totalSeconds = captureCount / EXPORT_FPS
    return when {
        totalSeconds < 60 -> "${totalSeconds}s"
        totalSeconds < 3600 -> "${totalSeconds / 60}m ${totalSeconds % 60}s"
        else -> "${totalSeconds / 3600}h ${(totalSeconds % 3600) / 60}m"
    }
}

@Composable
private fun CreateProjectDialog(
    onDismiss: () -> Unit,
    onCreate: (String, Int?, Int?) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var reminderTime by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New timelapse project") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Project name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(onClick = {
                        val now = java.time.LocalTime.now()
                        TimePickerDialog(
                            context,
                            { _, hour, minute -> reminderTime = hour to minute },
                            reminderTime?.first ?: now.hour,
                            reminderTime?.second ?: now.minute,
                            false
                        ).show()
                    }) {
                        Text("Reminder time")
                    }
                    Text(
                        text = reminderTime?.let { "${it.first.toString().padStart(2, '0')}:${it.second.toString().padStart(2, '0')}" }
                            ?: "Optional"
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(name.trim(), reminderTime?.first, reminderTime?.second) },
                enabled = name.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
