package dev.ktown.longlapsecapture.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.ktown.longlapsecapture.data.db.CaptureEntryEntity
import dev.ktown.longlapsecapture.data.db.ProjectEntity
import dev.ktown.longlapsecapture.data.repository.LonglapseRepository
import dev.ktown.longlapsecapture.export.TimelapseExporter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(
    repository: LonglapseRepository,
    projectId: String,
    onBack: () -> Unit,
    onOpenCamera: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var project by remember { mutableStateOf<ProjectEntity?>(null) }
    var entries by remember { mutableStateOf<List<CaptureEntryEntity>>(emptyList()) }
    var exportStatus by remember { mutableStateOf<String?>(null) }
    val exporter = remember { TimelapseExporter(repository) }

    LaunchedEffect(projectId) {
        project = repository.getProject(projectId)
    }

    LaunchedEffect(projectId) {
        repository.observeEntries(projectId).collectLatest { entries = it }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(project?.name ?: "Project") },
                navigationIcon = {
                    androidx.compose.material3.IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            project?.referencePhotoPath?.let { reference ->
                Text("Reference photo", style = MaterialTheme.typography.titleSmall)
                AsyncImage(
                    model = reference,
                    contentDescription = "Reference photo",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onOpenCamera) {
                    Text("Capture today")
                }
                Button(
                    onClick = {
                        exportStatus = "Exporting..."
                        scope.launch {
                            val output = exporter.exportTimelapse(
                                projectId = projectId,
                                entries = entries,
                                fps = 24
                            )
                            exportStatus = output.fold(
                                onSuccess = { "Saved to $it" },
                                onFailure = { "Export failed: ${it.message}" }
                            )
                        }
                    },
                    enabled = entries.isNotEmpty()
                ) {
                    Text("Export video")
                }
            }
            exportStatus?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Timeline", style = MaterialTheme.typography.titleSmall)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(entries, key = { it.id }) { entry ->
                    EntryRow(entry = entry)
                }
            }
        }
    }
}

@Composable
private fun EntryRow(entry: CaptureEntryEntity) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AsyncImage(
                model = entry.filePath,
                contentDescription = "Capture ${entry.localDate}",
                modifier = Modifier
                    .height(64.dp)
                    .fillMaxWidth(0.3f)
            )
            Column {
                Text(entry.localDate, style = MaterialTheme.typography.bodyLarge)
                Text(entry.filePath, style = MaterialTheme.typography.bodySmall, maxLines = 1)
            }
        }
    }
}
