package com.fugisawa.quemfaz.platform

import kotlinx.browser.window

actual fun openUrl(url: String) {
    window.open(url)
}
