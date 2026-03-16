package com.fugisawa.quemfaz.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.ui.unit.dp
import com.fugisawa.quemfaz.ui.preview.LightDarkPreview
import com.fugisawa.quemfaz.ui.strings.Strings
import com.fugisawa.quemfaz.ui.theme.AppTheme
import com.fugisawa.quemfaz.ui.theme.Spacing

@Composable
fun ErrorMessage(
    message: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(message, color = MaterialTheme.colorScheme.error)
        if (onRetry != null) {
            Spacer(modifier = Modifier.height(Spacing.sm))
            Button(onClick = onRetry) { Text(Strings.Common.RETRY) }
        }
    }
}

// ── Previews ──

@LightDarkPreview
@Composable
private fun ErrorMessageWithRetryPreview() {
    AppTheme { Surface(modifier = Modifier.padding(16.dp)) { ErrorMessage(message = "Network error. Please try again.", onRetry = {}) } }
}

@LightDarkPreview
@Composable
private fun ErrorMessageNoRetryPreview() {
    AppTheme { Surface(modifier = Modifier.padding(16.dp)) { ErrorMessage(message = "Something went wrong.") } }
}
