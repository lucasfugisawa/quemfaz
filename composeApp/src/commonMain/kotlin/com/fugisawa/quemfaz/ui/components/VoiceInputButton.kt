package com.fugisawa.quemfaz.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.fugisawa.quemfaz.platform.SpeechRecognizerState
import com.fugisawa.quemfaz.platform.isSpeechRecognizerAvailable
import com.fugisawa.quemfaz.platform.rememberSpeechRecognizer
import com.fugisawa.quemfaz.ui.theme.Spacing

@Composable
fun VoiceInputButton(
    onTranscription: (String) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
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

    // Pulse animation when listening
    val pulseScale = if (state == SpeechRecognizerState.LISTENING) {
        val infiniteTransition = rememberInfiniteTransition(label = "voicePulse")
        val scale by infiniteTransition.animateFloat(
            initialValue = 1.0f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(
                animation = tween(600),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "pulseScale",
        )
        scale
    } else {
        1.0f
    }

    val isListening = state == SpeechRecognizerState.LISTENING
    val icon = if (isListening) Icons.Default.Stop else Icons.Default.Mic

    if (compact) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .size(Spacing.compactVoiceButtonSize)
                .graphicsLayer { scaleX = pulseScale; scaleY = pulseScale }
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .clickable {
                    errorMessage = null
                    if (isListening) recognizer.stopListening() else recognizer.startListening()
                }
        ) {
            Icon(
                imageVector = icon,
                contentDescription = if (isListening) "Parar" else "Falar",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onPrimary,
            )
        }
    } else {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier,
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(72.dp)
                    .graphicsLayer { scaleX = pulseScale; scaleY = pulseScale }
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary,
                            )
                        )
                    )
                    .clickable {
                        errorMessage = null
                        if (isListening) recognizer.stopListening() else recognizer.startListening()
                    }
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = if (isListening) "Parar" else "Falar",
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            Text(
                text = if (isListening) "Ouvindo..." else "Falar",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            errorMessage?.let { msg ->
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = Spacing.xs),
                )
            }
        }
    }
}
