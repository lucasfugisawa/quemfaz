package com.fugisawa.quemfaz.platform

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun isSpeechRecognizerAvailable(): Boolean {
    val context = LocalContext.current
    return remember { SpeechRecognizer.isRecognitionAvailable(context) }
}

@Composable
actual fun rememberSpeechRecognizer(
    onResult: (SpeechRecognizerResult) -> Unit,
    onError: (String) -> Unit,
    onStateChange: (SpeechRecognizerState) -> Unit
): SpeechRecognizerHandle {
    val context = LocalContext.current
    val currentOnResult by rememberUpdatedState(onResult)
    val currentOnError by rememberUpdatedState(onError)
    val currentOnStateChange by rememberUpdatedState(onStateChange)

    var pendingStart by remember { mutableStateOf(false) }

    val recognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }

    val intent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
    }

    DisposableEffect(recognizer) {
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                currentOnStateChange(SpeechRecognizerState.LISTENING)
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}

            override fun onError(error: Int) {
                currentOnStateChange(SpeechRecognizerState.ERROR)
                val message = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "Nenhuma fala detectada"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Tempo esgotado"
                    SpeechRecognizer.ERROR_AUDIO -> "Erro de áudio"
                    SpeechRecognizer.ERROR_CLIENT -> "Erro interno"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permissão de microfone necessária"
                    SpeechRecognizer.ERROR_NETWORK,
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Erro de rede"
                    SpeechRecognizer.ERROR_SERVER -> "Erro no servidor de reconhecimento"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Reconhecimento ocupado"
                    else -> "Erro desconhecido"
                }
                currentOnError(message)
                currentOnStateChange(SpeechRecognizerState.IDLE)
            }

            override fun onResults(results: Bundle?) {
                val matches = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull().orEmpty()
                if (text.isNotEmpty()) {
                    currentOnResult(SpeechRecognizerResult(text, isFinal = true))
                }
                currentOnStateChange(SpeechRecognizerState.IDLE)
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull().orEmpty()
                if (text.isNotEmpty()) {
                    currentOnResult(SpeechRecognizerResult(text, isFinal = false))
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        onDispose {
            recognizer.destroy()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            recognizer.startListening(intent)
        } else {
            currentOnError("Permissão de microfone necessária")
            currentOnStateChange(SpeechRecognizerState.IDLE)
        }
        pendingStart = false
    }

    return remember {
        SpeechRecognizerHandle(
            startListening = {
                pendingStart = true
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            },
            stopListening = {
                recognizer.stopListening()
                currentOnStateChange(SpeechRecognizerState.IDLE)
            }
        )
    }
}
