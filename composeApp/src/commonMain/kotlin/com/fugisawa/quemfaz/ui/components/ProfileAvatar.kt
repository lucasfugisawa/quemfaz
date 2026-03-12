package com.fugisawa.quemfaz.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.dp
import com.fugisawa.quemfaz.ui.preview.LightDarkPreview
import com.fugisawa.quemfaz.ui.theme.AppTheme

@Composable
fun ProfileAvatar(
    name: String?,
    size: Dp = 56.dp,
    textStyle: TextStyle = MaterialTheme.typography.titleLarge
) {
    Surface(
        modifier = Modifier.size(size),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(name?.take(1)?.uppercase() ?: "?", style = textStyle)
        }
    }
}

// ── Previews ──

@LightDarkPreview
@Composable
private fun ProfileAvatarPreview() {
    AppTheme {
        Surface {
            Row(modifier = Modifier.padding(16.dp)) {
                ProfileAvatar(name = "Carlos Silva")
                Spacer(modifier = Modifier.width(8.dp))
                ProfileAvatar(name = null)
                Spacer(modifier = Modifier.width(8.dp))
                ProfileAvatar(name = "Ana", size = 80.dp)
            }
        }
    }
}
