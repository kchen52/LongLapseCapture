package dev.ktown.longlapsecapture

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dev.ktown.longlapsecapture.ui.LonglapseApp
import dev.ktown.longlapsecapture.ui.theme.LonglapseTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LonglapseTheme {
                LonglapseApp(
                    startProjectId = intent.getStringExtra(EXTRA_PROJECT_ID),
                    startInCamera = intent.getBooleanExtra(EXTRA_OPEN_CAMERA, false)
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    companion object {
        const val EXTRA_PROJECT_ID = "extra_project_id"
        const val EXTRA_OPEN_CAMERA = "extra_open_camera"
    }
}
