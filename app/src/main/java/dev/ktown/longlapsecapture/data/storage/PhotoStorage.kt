package dev.ktown.longlapsecapture.data.storage

import android.content.ContentValues
import android.content.Context
import android.media.MediaMuxer
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.Closeable
import java.io.File

class TimelapseExportHandle(
    val muxer: MediaMuxer,
    /** Human-readable location for UI (path or Downloads folder hint). */
    val savedLocationDescription: String,
    private val closeAfterMuxer: Closeable?
) {
    fun closeAfterMuxerFinished() {
        try {
            closeAfterMuxer?.close()
        } catch (_: Exception) {
        }
    }
}

class PhotoStorage(val context: Context) {
    private fun rootDirectory(): File {
        val base = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            ?: context.filesDir
        return File(base, "LonglapseCapture")
    }

    fun projectDirectory(projectId: String): File {
        val dir = File(rootDirectory(), projectId)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun photoFileForDate(projectId: String, localDate: String): File {
        val dir = projectDirectory(projectId)
        return File(dir, "$localDate.jpg")
    }

    fun referenceMaskFile(projectId: String, localDate: String): File {
        val dir = projectDirectory(projectId)
        return File(dir, "$localDate-mask.png")
    }

    /**
     * Creates a [MediaMuxer] that writes into the public **Downloads/LonglapseCapture** folder.
     * On API 29+ this uses [MediaStore.Downloads] (no storage permission). On older APIs the
     * caller must hold [android.Manifest.permission.WRITE_EXTERNAL_STORAGE].
     */
    fun openTimelapseExportToDownloads(fileName: String): TimelapseExportHandle {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            openTimelapseExportMediaStore(fileName)
        } else {
            openTimelapseExportLegacyDownloads(fileName)
        }
    }

    private fun openTimelapseExportMediaStore(fileName: String): TimelapseExportHandle {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                "${Environment.DIRECTORY_DOWNLOADS}/LonglapseCapture"
            )
        }
        val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: error("Could not create video in Downloads")
        val pfd = context.contentResolver.openFileDescriptor(uri, "w")
            ?: error("Could not open Downloads video for writing")
        val muxer = MediaMuxer(pfd.fileDescriptor, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        val relative = "${Environment.DIRECTORY_DOWNLOADS}/LonglapseCapture/$fileName"
        return TimelapseExportHandle(muxer, relative, pfd)
    }

    private fun openTimelapseExportLegacyDownloads(fileName: String): TimelapseExportHandle {
        val base = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val dir = File(base, "LonglapseCapture")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        val file = File(dir, fileName)
        val muxer = MediaMuxer(file.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        return TimelapseExportHandle(muxer, file.absolutePath, null)
    }
}
