package com.fugisawa.quemfaz.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.fugisawa.quemfaz.platform.PlatformWebView
import com.fugisawa.quemfaz.ui.components.AppScreen

@Composable
fun LegalDocumentScreen(
    title: String,
    url: String,
    onNavigateBack: () -> Unit,
) {
    AppScreen(title = title, onNavigateBack = onNavigateBack) {
        PlatformWebView(
            url = url,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
