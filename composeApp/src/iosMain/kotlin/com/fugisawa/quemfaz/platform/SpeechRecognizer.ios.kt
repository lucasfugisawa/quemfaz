package com.fugisawa.quemfaz.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFAudio.AVAudioEngine
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryRecord
import platform.AVFAudio.AVAudioSessionSetActiveOptionNotifyOthersOnDeactivation
import platform.AVFAudio.setActive
import platform.Speech.SFSpeechAudioBufferRecognitionRequest
import platform.Speech.SFSpeechRecognizer
import platform.Speech.SFSpeechRecognitionTask
import platform.Speech.SFSpeechRecognizerAuthorizationStatus
import platform.Foundation.NSLocale

@Composable
actual fun isSpeechRecognizerAvailable(): Boolean {
    return remember {
        val recognizer = SFSpeechRecognizer(locale = NSLocale("pt-BR"))
        recognizer.isAvailable()
    }
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberSpeechRecognizer(
    onResult: (SpeechRecognizerResult) -> Unit,
    onError: (String) -> Unit,
    onStateChange: (SpeechRecognizerState) -> Unit
): SpeechRecognizerHandle {
    val currentOnResult by rememberUpdatedState(onResult)
    val currentOnError by rememberUpdatedState(onError)
    val currentOnStateChange by rememberUpdatedState(onStateChange)

    val audioEngine = remember { AVAudioEngine() }
    var recognitionTask: SFSpeechRecognitionTask? = null

    DisposableEffect(Unit) {
        onDispose {
            if (audioEngine.isRunning()) {
                audioEngine.stop()
                audioEngine.inputNode.removeTapOnBus(0u)
            }
            recognitionTask?.cancel()
        }
    }

    return remember {
        SpeechRecognizerHandle(
            startListening = {
                SFSpeechRecognizer.requestAuthorization { authStatus ->
                    when (authStatus) {
                        SFSpeechRecognizerAuthorizationStatus.SFSpeechRecognizerAuthorizationStatusAuthorized -> {
                            val audioSession = AVAudioSession.sharedInstance()
                            try {
                                audioSession.setCategory(AVAudioSessionCategoryRecord, null)
                                audioSession.setActive(true, null)
                            } catch (_: Exception) {
                                currentOnError("Erro ao configurar áudio")
                                currentOnStateChange(SpeechRecognizerState.ERROR)
                                return@requestAuthorization
                            }

                            val speechRecognizer = SFSpeechRecognizer(locale = NSLocale("pt-BR"))
                            if (!speechRecognizer.isAvailable()) {
                                currentOnError("Reconhecimento de fala indisponível")
                                currentOnStateChange(SpeechRecognizerState.UNAVAILABLE)
                                return@requestAuthorization
                            }

                            val request = SFSpeechAudioBufferRecognitionRequest().apply {
                                shouldReportPartialResults = true
                            }

                            val inputNode = audioEngine.inputNode
                            val recordingFormat = inputNode.outputFormatForBus(0u)
                            inputNode.installTapOnBus(
                                0u,
                                bufferSize = 1024u,
                                format = recordingFormat
                            ) { buffer, _ ->
                                if (buffer != null) {
                                    request.appendAudioPCMBuffer(buffer)
                                }
                            }

                            audioEngine.prepare()
                            try {
                                audioEngine.startAndReturnError(null)
                            } catch (_: Exception) {
                                currentOnError("Erro ao iniciar gravação")
                                currentOnStateChange(SpeechRecognizerState.ERROR)
                                return@requestAuthorization
                            }

                            recognitionTask = speechRecognizer.recognitionTaskWithRequest(request) { result, error ->
                                if (error != null) {
                                    audioEngine.stop()
                                    inputNode.removeTapOnBus(0u)
                                    currentOnError("Erro no reconhecimento de fala")
                                    currentOnStateChange(SpeechRecognizerState.IDLE)
                                    return@recognitionTaskWithRequest
                                }
                                if (result != null) {
                                    val text = result.bestTranscription.formattedString
                                    val isFinal = result.isFinal()
                                    currentOnResult(SpeechRecognizerResult(text, isFinal))
                                    if (isFinal) {
                                        audioEngine.stop()
                                        inputNode.removeTapOnBus(0u)
                                        currentOnStateChange(SpeechRecognizerState.IDLE)
                                    }
                                }
                            }

                            currentOnStateChange(SpeechRecognizerState.LISTENING)
                        }
                        SFSpeechRecognizerAuthorizationStatus.SFSpeechRecognizerAuthorizationStatusDenied,
                        SFSpeechRecognizerAuthorizationStatus.SFSpeechRecognizerAuthorizationStatusRestricted -> {
                            currentOnError("Permissão de reconhecimento de fala necessária")
                            currentOnStateChange(SpeechRecognizerState.IDLE)
                        }
                        else -> {
                            currentOnError("Permissão de reconhecimento de fala necessária")
                            currentOnStateChange(SpeechRecognizerState.IDLE)
                        }
                    }
                }
            },
            stopListening = {
                if (audioEngine.isRunning()) {
                    audioEngine.stop()
                    audioEngine.inputNode.removeTapOnBus(0u)
                }
                recognitionTask?.cancel()
                recognitionTask = null
                currentOnStateChange(SpeechRecognizerState.IDLE)
            }
        )
    }
}
