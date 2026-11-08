package com.jornaler.app.ui

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowCompat
import com.jornaler.app.R

/** Gold of the supplied painting's frame, used to letterbox the full portrait. */
val PortraitFrame = Color(0xFFB28A4F)

/**
 * Full supplied portrait, aspect ratio kept. Shown only while the notebook is
 * actually loading. Nothing here waits on a timer.
 */
@Composable
fun PortraitStartup(modifier: Modifier = Modifier) {
    val view = LocalView.current
    val description = stringResource(R.string.startup_portrait_description)
    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        window.statusBarColor = PortraitFrame.toArgb()
        window.navigationBarColor = PortraitFrame.toArgb()
        val controller = WindowCompat.getInsetsController(window, view)
        controller.isAppearanceLightStatusBars = true
        controller.isAppearanceLightNavigationBars = true
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PortraitFrame),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.startup_portrait),
            contentDescription = description,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
