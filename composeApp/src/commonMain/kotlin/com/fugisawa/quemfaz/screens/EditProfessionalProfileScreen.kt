package com.fugisawa.quemfaz.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fugisawa.quemfaz.contract.profile.ProfessionalProfileResponse

@Composable
fun EditProfessionalProfileScreen(
    uiState: EditProfileUiState,
    onSave: (description: String, city: String, neighborhoods: List<String>, contactPhone: String, whatsAppPhone: String) -> Unit,
    onNavigateBack: () -> Unit
) {
    when (uiState) {
        is EditProfileUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is EditProfileUiState.NoProfile -> {
            Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No professional profile found.", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onNavigateBack) { Text("Go Back") }
                }
            }
        }
        is EditProfileUiState.Error -> {
            Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(uiState.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onNavigateBack) { Text("Go Back") }
                }
            }
        }
        is EditProfileUiState.Ready -> EditProfileForm(uiState.profile, isSaving = false, isSaved = false, onSave = onSave)
        is EditProfileUiState.Saving -> EditProfileForm(uiState.profile, isSaving = true, isSaved = false, onSave = onSave)
        is EditProfileUiState.Saved -> EditProfileForm(uiState.profile, isSaving = false, isSaved = true, onSave = onSave)
    }
}

@Composable
private fun EditProfileForm(
    profile: ProfessionalProfileResponse,
    isSaving: Boolean,
    isSaved: Boolean,
    onSave: (description: String, city: String, neighborhoods: List<String>, contactPhone: String, whatsAppPhone: String) -> Unit
) {
    var description by remember(profile.id) { mutableStateOf(profile.description) }
    var city by remember(profile.id) { mutableStateOf(profile.cityName) }
    var neighborhoodsText by remember(profile.id) { mutableStateOf(profile.neighborhoods.joinToString(", ")) }
    var contactPhone by remember(profile.id) { mutableStateOf(profile.contactPhone) }
    var whatsAppPhone by remember(profile.id) { mutableStateOf(profile.whatsAppPhone ?: "") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Edit Professional Profile", style = MaterialTheme.typography.headlineSmall)

        if (profile.services.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Services: ${profile.services.joinToString(", ") { it.displayName }}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth().height(120.dp),
            shape = MaterialTheme.shapes.medium
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = city,
            onValueChange = { city = it },
            label = { Text("City") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = MaterialTheme.shapes.medium
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = neighborhoodsText,
            onValueChange = { neighborhoodsText = it },
            label = { Text("Neighborhoods (comma-separated)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = MaterialTheme.shapes.medium
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = contactPhone,
            onValueChange = { contactPhone = it },
            label = { Text("Contact Phone") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = MaterialTheme.shapes.medium
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = whatsAppPhone,
            onValueChange = { whatsAppPhone = it },
            label = { Text("WhatsApp Phone") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = MaterialTheme.shapes.medium
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (isSaved) {
            Text(
                "Profile saved successfully.",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(
            onClick = {
                val neighborhoods = neighborhoodsText
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                onSave(description, city, neighborhoods, contactPhone, whatsAppPhone)
            },
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            if (isSaving) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("Save")
            }
        }
    }
}
