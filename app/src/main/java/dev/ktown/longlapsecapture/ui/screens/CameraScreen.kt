package dev.ktown.longlapsecapture.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.layout.ContentScale
import androidx.core.content.ContextCompat
import coil3.compose.AsyncImage
import dev.ktown.longlapsecapture.data.db.ProjectEntity
import dev.ktown.longlapsecapture.data.repository.LonglapseRepository
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate

@Composable
fun CameraScreen(
    repository: LonglapseRepository,
    projectId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    var project by remember { mutableStateOf<ProjectEntity?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    val previewView = remember { PreviewView(context) }
    var cameraSelector by remember { mutableStateOf(CameraSelector.DEFAULT_BACK_CAMERA) }
    var subjectOnlyOverlay by remember { mutableStateOf(false) }
    var hasTodayCapture by remember { mutableStateOf(false) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher =
        androidx.activity.compose.rememberLauncherForActivityResult(
            contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
        ) { granted ->
            hasCameraPermission = granted
        }

    BackHandler(onBack = onBack)

    LaunchedEffect(projectId) {
        val loaded = repository.getProject(projectId)
        project = loaded
        cameraSelector = if (loaded?.preferredCameraFacing == "front") {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
        hasTodayCapture = loaded?.let { repository.hasCaptureForToday(it.id) } ?: false
    }

    LaunchedEffect(hasCameraPermission, cameraSelector) {
        if (hasCameraPermission) {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            val executor = ContextCompat.getMainExecutor(context)
            cameraProviderFuture.addListener(
                {
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().apply {
                        setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val capture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build()
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        capture
                    )
                    imageCapture = capture
                },
                executor
            )
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = project?.name ?: "Capture",
                style = MaterialTheme.typography.titleMedium
            )
            if (project?.referencePhotoPath != null) {
                Button(onClick = { subjectOnlyOverlay = !subjectOnlyOverlay }) {
                    Text(
                        if (subjectOnlyOverlay) "Full reference" else "Subject only"
                    )
                }
            }
        }

        if (!hasCameraPermission) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Camera permission is required to capture photos.")
                Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                    Text("Grant permission")
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                AndroidView(
                    factory = { previewView },
                    modifier = Modifier.fillMaxSize()
                )
                project?.let { p ->
                    p.referencePhotoPath?.let { reference ->
                        if (!subjectOnlyOverlay) {
                            AsyncImage(
                                model = reference,
                                contentDescription = "Reference overlay",
                                modifier = Modifier.fillMaxSize(),
                                alpha = 0.4f
                            )
                        } else {
                            val maskPath = repository.referenceMaskPath(p)
                            if (maskPath != null) {
                                AsyncImage(
                                    model = maskPath,
                                    contentDescription = "Reference subject overlay",
                                    modifier = Modifier.fillMaxSize(),
                                    alpha = 0.6f
                                )
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    val captureEnabled = !hasTodayCapture && imageCapture != null && project != null
                    val shutterColor = if (captureEnabled) {
                        MaterialTheme.colorScheme.onBackground
                    } else {
                        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                    }
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .border(
                                width = 4.dp,
                                color = shutterColor,
                                shape = CircleShape
                            )
                            .padding(6.dp)
                            .clip(CircleShape)
                            .background(shutterColor)
                            .clickable(enabled = captureEnabled) {
                                val capture = imageCapture ?: return@clickable
                                val activeProject = project ?: return@clickable
                                val today = LocalDate.now()
                                val filePath = repository.photoFilePath(activeProject.id, today)
                                val outputFile = File(filePath)
                                val outputOptions =
                                    ImageCapture.OutputFileOptions.Builder(outputFile).build()
                                capture.takePicture(
                                    outputOptions,
                                    ContextCompat.getMainExecutor(context),
                                    object : ImageCapture.OnImageSavedCallback {
                                        override fun onImageSaved(
                                            outputFileResults: ImageCapture.OutputFileResults
                                        ) {
                                            scope.launch {
                                                val shouldSetReference =
                                                    activeProject.referencePhotoPath == null
                                                repository.saveCapture(
                                                    projectId = activeProject.id,
                                                    localDate = today,
                                                    filePath = filePath,
                                                    setAsReference = shouldSetReference
                                                )
                                                hasTodayCapture = true
                                                onBack()
                                            }
                                        }

                                        override fun onError(exception: ImageCaptureException) {
                                            // No-op for now; capture errors can be surfaced later.
                                        }
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(shutterColor)
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    androidx.compose.material3.IconButton(
                        onClick = {
                            val activeProject = project ?: return@IconButton
                            val newSelector =
                                if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                                    CameraSelector.DEFAULT_FRONT_CAMERA
                                } else {
                                    CameraSelector.DEFAULT_BACK_CAMERA
                                }
                            cameraSelector = newSelector
                            scope.launch {
                                val facing = if (newSelector == CameraSelector.DEFAULT_FRONT_CAMERA) {
                                    "front"
                                } else {
                                    "back"
                                }
                                repository.updatePreferredCameraFacing(activeProject.id, facing)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Cameraswitch,
                            contentDescription = "Flip camera"
                        )
                    }
                }
            }
        }
    }
}
