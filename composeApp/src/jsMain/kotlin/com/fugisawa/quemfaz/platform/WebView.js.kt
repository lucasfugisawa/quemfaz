package com.fugisawa.quemfaz.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun PlatformWebView(url: String, modifier: Modifier) {
    // Web platform: fall back to opening in browser since iframe embedding
    // of external URLs is often blocked by Content-Security-Policy.
    openUrl(url)
}
