package com.fugisawa.quemfaz.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIApplication
import platform.darwin.NSObject
import platform.Foundation.NSData
import platform.UniformTypeIdentifiers.UTTypeImage

actual class ImagePickerLauncher(
    internal val launchFn: () -> Unit,
)

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberImagePickerLauncher(
    onImageSelected: (data: ByteArray, mimeType: String) -> Unit,
): ImagePickerLauncher {
    return remember {
        ImagePickerLauncher {
            val config = PHPickerConfiguration().apply {
                filter = PHPickerFilter.imagesFilter
                selectionLimit = 1
            }
            val picker = PHPickerViewController(configuration = config)
            val delegate = object : NSObject(), PHPickerViewControllerDelegateProtocol {
                override fun picker(
                    picker: PHPickerViewController,
                    didFinishPicking: List<*>,
                ) {
                    picker.dismissViewControllerAnimated(true, null)
                    val result = didFinishPicking.firstOrNull() as? PHPickerResult ?: return
                    result.itemProvider.loadDataRepresentationForTypeIdentifier(
                        UTTypeImage.identifier,
                    ) { data, _ ->
                        if (data != null) {
                            val bytes = data.toByteArray()
                            onImageSelected(bytes, "image/jpeg")
                        }
                    }
                }
            }
            picker.delegate = delegate
            UIApplication.sharedApplication.keyWindow?.rootViewController
                ?.presentViewController(picker, true, null)
        }
    }
}

actual fun ImagePickerLauncher.launch() = launchFn()

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val size = this.length.toInt()
    return ByteArray(size) { i -> bytes?.get(i)?.toByte() ?: 0 }
}
