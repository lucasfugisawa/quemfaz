package com.fugisawa.quemfaz

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport

// TODO: Replace with production server URL before release.
private const val BASE_URL_WEB = "http://localhost:8080"

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport {
        App(baseUrl = BASE_URL_WEB)
    }
}