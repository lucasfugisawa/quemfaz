package com.fugisawa.quemfaz.platform

import androidx.compose.runtime.Composable

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // iOS uses swipe-to-go-back gesture handled natively; no-op here.
}
