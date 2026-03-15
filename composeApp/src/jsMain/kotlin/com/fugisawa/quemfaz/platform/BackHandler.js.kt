package com.fugisawa.quemfaz.platform

import androidx.compose.runtime.Composable

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // Web uses browser back button; no-op here.
}
