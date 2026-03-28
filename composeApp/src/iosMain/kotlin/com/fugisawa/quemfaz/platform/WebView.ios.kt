package com.fugisawa.quemfaz.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.WebKit.WKWebView

@Composable
actual fun PlatformWebView(url: String, modifier: Modifier) {
    UIKitView(
        factory = {
            WKWebView().apply {
                val nsUrl = NSURL(string = url) ?: return@apply
                loadRequest(NSURLRequest(uRL = nsUrl))
            }
        },
        modifier = modifier,
    )
}
