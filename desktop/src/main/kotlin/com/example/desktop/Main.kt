package com.example.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.example.desktop.core.AppState
import com.example.desktop.ui.DashboardScreen
import com.example.desktop.ui.MeelanoColors

/**
 * Windows entry point.
 *
 * The window is sized to the phone dashboard's proportions rather than a wide
 * desktop layout, because the design is a single vertical column and stretching
 * it sideways would leave the orb marooned in empty space.
 */
fun main() = application {
    val scope = rememberCoroutineScope()
    val state = remember { AppState(scope) }

    val windowState = rememberWindowState(
        size = DpSize(460.dp, 820.dp)
    )

    // A tunnel left running after the window closes would keep the system proxy
    // pointed at a dead port, so teardown is tied to the composition's life.
    DisposableEffect(Unit) {
        onDispose { state.shutdown() }
    }

    Window(
        onCloseRequest = {
            state.shutdown()
            exitApplication()
        },
        state = windowState,
        title = "MeeLano Tunnel",
        icon = painterResource("icon.png")
    ) {
        Box(Modifier.fillMaxSize()) {
            DashboardScreen(state)
        }
    }
}
