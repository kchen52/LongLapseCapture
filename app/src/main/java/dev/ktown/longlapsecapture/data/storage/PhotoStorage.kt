package dev.ktown.longlapsecapture.data.storage

import android.content.Context
import android.os.Environment
import java.io.File

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

    fun exportDirectory(): File {
        val base = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
            ?: context.filesDir
        val dir = File(base, "LonglapseCapture")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }
}
