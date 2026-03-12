package com.fugisawa.quemfaz

import androidx.compose.ui.window.ComposeUIViewController

// TODO: Replace with production server URL before release.
// For simulator builds, localhost works because the host machine runs the server.
private const val BASE_URL_IOS = "http://localhost:8080"

fun MainViewController() = ComposeUIViewController { App(baseUrl = BASE_URL_IOS) }