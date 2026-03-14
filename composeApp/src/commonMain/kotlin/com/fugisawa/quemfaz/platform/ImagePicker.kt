package com.fugisawa.quemfaz.platform

import androidx.compose.runtime.Composable

/**
 * Platform-specific image picker.
 * Obtain via [rememberImagePickerLauncher].
 * Call [launch] to trigger native image selection.
 */
expect class ImagePickerLauncher

@Composable
expect fun rememberImagePickerLauncher(
    onImageSelected: (data: ByteArray, mimeType: String) -> Unit,
): ImagePickerLauncher

expect fun ImagePickerLauncher.launch()
