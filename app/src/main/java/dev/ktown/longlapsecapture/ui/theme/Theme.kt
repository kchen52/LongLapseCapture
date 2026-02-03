package dev.ktown.longlapsecapture.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalInspectionMode

@Composable
fun LonglapseTheme(content: @Composable () -> Unit) {
    val isPreview = LocalInspectionMode.current
    val colorScheme = if (isPreview) {
        lightColorScheme()
    } else {
        if (androidx.compose.foundation.isSystemInDarkTheme()) {
            darkColorScheme()
        } else {
            lightColorScheme()
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
