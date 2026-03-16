package com.fugisawa.quemfaz.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.fugisawa.quemfaz.LocalBaseUrl
import com.fugisawa.quemfaz.ui.preview.LightDarkPreview
import com.fugisawa.quemfaz.ui.theme.AppTheme

@Composable
fun ProfileAvatar(
    name: String?,
    photoUrl: String? = null,
    size: Dp = 56.dp,
    textStyle: TextStyle = MaterialTheme.typography.titleLarge,
    modifier: Modifier = Modifier
) {
    val baseUrl = LocalBaseUrl.current
    val resolvedUrl = remember(photoUrl, baseUrl) {
        when {
            photoUrl.isNullOrBlank() -> null
            photoUrl.startsWith("http://") || photoUrl.startsWith("https://") -> photoUrl
            else -> "$baseUrl$photoUrl"
        }
    }

    Surface(
        modifier = modifier.size(size),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (resolvedUrl != null) {
                var imageLoadFailed by remember(resolvedUrl) { mutableStateOf(false) }
                if (!imageLoadFailed) {
                    AsyncImage(
                        model = resolvedUrl,
                        contentDescription = name,
                        modifier = Modifier.fillMaxSize().clip(MaterialTheme.shapes.medium),
                        contentScale = ContentScale.Crop,
                        onError = { imageLoadFailed = true }
                    )
                } else {
                    Text(name?.take(1)?.uppercase() ?: "?", style = textStyle)
                }
            } else {
                Text(name?.take(1)?.uppercase() ?: "?", style = textStyle)
            }
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
                ProfileAvatar(
                    name = "Com Foto",
                    photoUrl = "https://example.com/photo.jpg"
                )
                Spacer(modifier = Modifier.width(8.dp))
                ProfileAvatar(name = "Ana", size = 80.dp)
            }
        }
    }
}
