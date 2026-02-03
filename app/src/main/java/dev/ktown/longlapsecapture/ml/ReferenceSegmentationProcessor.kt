package dev.ktown.longlapsecapture.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import dev.ktown.longlapsecapture.data.storage.PhotoStorage
import java.io.File
import java.io.FileOutputStream

class ReferenceSegmentationProcessor(
    private val context: Context,
    private val storage: PhotoStorage
 ) {

    /**
     * Generate a subject-only PNG mask for the given project/date reference photo.
     * This runs on-device using ML Kit Subject Segmentation.
     */
    fun generateSubjectMask(referencePath: String, projectId: String, localDate: String) {
        val file = File(referencePath)
        if (!file.exists()) return

        val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return
        val options = SubjectSegmenterOptions.Builder()
            .enableForegroundBitmap()
            .build()
        val segmenter = SubjectSegmentation.getClient(options)

        val image = InputImage.fromBitmap(bitmap, 0)
        try {
            val result = Tasks.await(segmenter.process(image))
            val subjectBitmap = result.foregroundBitmap ?: return

            val maskFile = storage.referenceMaskFile(projectId, localDate)
            FileOutputStream(maskFile).use { out ->
                subjectBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            subjectBitmap.recycle()
        } catch (_: Exception) {
            // Fail silently for now; we can add logging later.
        } finally {
            segmenter.close()
        }
    }
}

