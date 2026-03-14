package com.fugisawa.quemfaz.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.browser.document
import org.w3c.dom.HTMLInputElement
import org.w3c.files.FileReader
import org.w3c.files.get

actual class ImagePickerLauncher(
    internal val launchFn: () -> Unit,
)

@Composable
actual fun rememberImagePickerLauncher(
    onImageSelected: (data: ByteArray, mimeType: String) -> Unit,
): ImagePickerLauncher {
    return remember {
        ImagePickerLauncher {
            val input = document.createElement("input") as HTMLInputElement
            input.type = "file"
            input.accept = "image/jpeg,image/png,image/webp"
            input.onchange = {
                val file = input.files?.get(0) ?: return@onchange Unit
                val reader = FileReader()
                reader.onload = { _ ->
                    val arrayBuffer = reader.result
                    val jsArray = js("new Uint8Array(arrayBuffer)") as ByteArray
                    onImageSelected(jsArray, file.type)
                    Unit
                }
                reader.readAsArrayBuffer(file)
                Unit
            }
            input.click()
        }
    }
}

actual fun ImagePickerLauncher.launch() = launchFn()
