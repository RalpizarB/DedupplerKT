package com.deduppler

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.unit.dp
import com.deduppler.ui.DedupplerApp

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "DedupplerKT - Duplicate File Finder",
        state = rememberWindowState(width = 1000.dp, height = 800.dp)
    ) {
        DedupplerApp()
    }
}
