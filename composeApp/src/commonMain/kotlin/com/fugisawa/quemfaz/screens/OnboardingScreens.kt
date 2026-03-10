package com.fugisawa.quemfaz.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun OnboardingScreens(
    uiState: OnboardingUiState,
    onCreateDraft: (String) -> Unit,
    onConfirm: (String, List<String>, String?, List<String>, String) -> Unit,
    onFinish: () -> Unit
) {
    var inputText by remember { mutableStateOf("") }

    when (uiState) {
        is OnboardingUiState.Idle -> {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text("Tell us what services you provide", style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    placeholder = { Text("e.g. I do residential painting in Batatais...") }
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { onCreateDraft(inputText) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = inputText.isNotBlank()
                ) {
                    Text("Analyze services")
                }
            }
        }
        is OnboardingUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is OnboardingUiState.DraftReady -> {
            val draft = uiState.draft
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text("Review your services", style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Description:", style = MaterialTheme.typography.titleMedium)
                Text(draft.normalizedDescription)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Interpreted services:", style = MaterialTheme.typography.titleMedium)
                draft.interpretedServices.forEach { service ->
                    Text("• ${service.displayName}")
                }
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = {
                        onConfirm(
                            draft.normalizedDescription,
                            draft.interpretedServices.map { it.serviceId },
                            draft.cityName,
                            draft.neighborhoods,
                            "" // Placeholder for contact phone
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Confirm and Publish")
                }
            }
        }
        is OnboardingUiState.Published -> {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                Text("Profile Published! 🎉", style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onFinish) {
                    Text("View my profile")
                }
            }
        }
        is OnboardingUiState.Error -> {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text("Error: ${uiState.message}", color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { onCreateDraft(inputText) }) {
                    Text("Retry")
                }
            }
        }
    }
}
