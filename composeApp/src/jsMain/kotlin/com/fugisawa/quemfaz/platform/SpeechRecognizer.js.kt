package com.fugisawa.quemfaz.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun isSpeechRecognizerAvailable(): Boolean = false

@Composable
actual fun rememberSpeechRecognizer(
    onResult: (SpeechRecognizerResult) -> Unit,
    onError: (String) -> Unit,
    onStateChange: (SpeechRecognizerState) -> Unit
): SpeechRecognizerHandle = remember {
    SpeechRecognizerHandle(
        startListening = {},
        stopListening = {}
    )
}
