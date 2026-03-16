package com.fugisawa.quemfaz.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fugisawa.quemfaz.platform.SpeechRecognizerState
import com.fugisawa.quemfaz.platform.isSpeechRecognizerAvailable
import com.fugisawa.quemfaz.platform.rememberSpeechRecognizer
import com.fugisawa.quemfaz.ui.theme.Spacing

@Composable
fun VoiceInputButton(
    onTranscription: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isAvailable = isSpeechRecognizerAvailable()
    if (!isAvailable) return

    var state by remember { mutableStateOf(SpeechRecognizerState.IDLE) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val recognizer = rememberSpeechRecognizer(
        onResult = { result -> onTranscription(result.text) },
        onError = { error -> errorMessage = error },
        onStateChange = { newState -> state = newState }
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary
                        )
                    )
                )
                .clickable {
                    errorMessage = null
                    if (state == SpeechRecognizerState.LISTENING) {
                        recognizer.stopListening()
                    } else {
                        recognizer.startListening()
                    }
                }
        ) {
            Text(
                text = if (state == SpeechRecognizerState.LISTENING) "\u23F9" else "\uD83C\uDFA4",
                fontSize = 28.sp
            )
        }

        Spacer(modifier = Modifier.height(Spacing.sm))

        Text(
            text = when (state) {
                SpeechRecognizerState.LISTENING -> "Ouvindo..."
                else -> "Toque para falar"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        errorMessage?.let { msg ->
            Text(
                text = msg,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = Spacing.xs)
            )
        }
    }
}
