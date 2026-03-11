package com.fugisawa.quemfaz.platform

import android.content.Context
import android.content.Intent
import android.net.Uri

var appContext: Context? = null

actual fun openUrl(url: String) {
    val ctx = appContext ?: return
    runCatching {
        ctx.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
