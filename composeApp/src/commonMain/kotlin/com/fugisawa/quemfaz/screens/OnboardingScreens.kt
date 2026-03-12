package com.fugisawa.quemfaz.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
    var phoneNumber by remember { mutableStateOf("") }

    Scaffold { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(24.dp)) {
            when (uiState) {
                is OnboardingUiState.Idle -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Text("Become a professional", style = MaterialTheme.typography.headlineLarge)
                        Text("Describe your services in your own words. We'll help you organize them.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            modifier = Modifier.fillMaxWidth().height(200.dp),
                            placeholder = { Text("e.g. I am a residential painter with 10 years of experience. I work in Batatais and Ribeirão Preto. I also do small wall repairs.") },
                            shape = MaterialTheme.shapes.medium
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Button(
                            onClick = { onCreateDraft(inputText) },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            enabled = inputText.isNotBlank(),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text("Analyze my services", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
                is OnboardingUiState.Loading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Interpreting your description...", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                is OnboardingUiState.DraftReady -> {
                    val draft = uiState.draft
                    Column(modifier = Modifier.fillMaxSize()) {
                        Text("Review your profile", style = MaterialTheme.typography.headlineLarge)
                        Text("This is how customers will see your services.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Description:", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                                Text(draft.normalizedDescription, style = MaterialTheme.typography.bodyLarge)
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Text("Interpreted services:", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                                @OptIn(ExperimentalLayoutApi::class)
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    draft.interpretedServices.forEach { service ->
                                        SuggestionChip(
                                            onClick = {},
                                            label = { Text(service.displayName) }
                                        )
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = phoneNumber,
                            onValueChange = { phoneNumber = it },
                            label = { Text("Phone number") },
                            placeholder = { Text("e.g. +55 11 99999-9999") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = MaterialTheme.shapes.medium
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        Button(
                            onClick = {
                                onConfirm(
                                    draft.normalizedDescription,
                                    draft.interpretedServices.map { it.serviceId },
                                    draft.cityName,
                                    draft.neighborhoods,
                                    phoneNumber
                                )
                            },
                            enabled = phoneNumber.isNotBlank(),
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text("Confirm and Publish", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
                is OnboardingUiState.Published -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🎉", style = MaterialTheme.typography.displayLarge)
                        Text("Profile Published!", style = MaterialTheme.typography.headlineMedium)
                        Text("You're now visible to customers.", style = MaterialTheme.typography.bodyMedium)
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        Button(
                            onClick = onFinish,
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text("View my profile", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
                is OnboardingUiState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("❌", style = MaterialTheme.typography.displayLarge)
                        Text("Something went wrong", style = MaterialTheme.typography.headlineSmall)
                        Text(uiState.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Button(onClick = { onCreateDraft(inputText) }) {
                            Text("Retry")
                        }
                    }
                }
            }
        }
    }
}
