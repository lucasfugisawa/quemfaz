package com.fugisawa.quemfaz.platform

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

actual class ImagePickerLauncher(
    private val launchFn: () -> Unit,
)

@Composable
actual fun rememberImagePickerLauncher(
    onImageSelected: (data: ByteArray, mimeType: String) -> Unit,
): ImagePickerLauncher {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
        val bytes = context.contentResolver.openInputStream(uri)?.readBytes() ?: return@rememberLauncherForActivityResult
        onImageSelected(bytes, mimeType)
    }
    return remember {
        ImagePickerLauncher {
            launcher.launch(
                ActivityResultContracts.PickVisualMedia.ImageOnly
            )
        }
    }
}

actual fun ImagePickerLauncher.launch() = launchFn()
