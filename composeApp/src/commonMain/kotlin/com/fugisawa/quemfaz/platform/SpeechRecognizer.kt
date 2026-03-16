package com.fugisawa.quemfaz.platform

import androidx.compose.runtime.Composable

enum class SpeechRecognizerState {
    IDLE, LISTENING, ERROR, UNAVAILABLE
}

data class SpeechRecognizerResult(
    val text: String,
    val isFinal: Boolean
)

class SpeechRecognizerHandle(
    val startListening: () -> Unit,
    val stopListening: () -> Unit
)

@Composable
expect fun isSpeechRecognizerAvailable(): Boolean

@Composable
expect fun rememberSpeechRecognizer(
    onResult: (SpeechRecognizerResult) -> Unit,
    onError: (String) -> Unit,
    onStateChange: (SpeechRecognizerState) -> Unit
): SpeechRecognizerHandle
