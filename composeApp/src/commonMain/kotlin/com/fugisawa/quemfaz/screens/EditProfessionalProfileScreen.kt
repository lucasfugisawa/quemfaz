package com.fugisawa.quemfaz.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.fugisawa.quemfaz.contract.profile.ProfessionalProfileResponse
import com.fugisawa.quemfaz.ui.components.ProfileAvatar
import com.fugisawa.quemfaz.ui.preview.LightDarkScreenPreview
import com.fugisawa.quemfaz.ui.preview.PreviewSamples
import com.fugisawa.quemfaz.ui.theme.AppTheme
import com.fugisawa.quemfaz.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfessionalProfileScreen(
    uiState: EditProfileUiState,
    onSave: (description: String, city: String, neighborhoods: List<String>, contactPhone: String, whatsAppPhone: String) -> Unit,
    onNavigateBack: () -> Unit,
    onGoToOnboarding: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Professional Profile") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Text("←", style = MaterialTheme.typography.titleLarge)
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when (uiState) {
                is EditProfileUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is EditProfileUiState.NoProfile -> {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "You don't have a professional profile yet.",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = onGoToOnboarding,
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Text("Set up professional profile")
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = onNavigateBack) { Text("Go Back") }
                        }
                    }
                }
                is EditProfileUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                uiState.message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = onNavigateBack) { Text("Go Back") }
                        }
                    }
                }
                is EditProfileUiState.Ready -> EditProfileForm(
                    uiState.profile, isSaving = false, isSaved = false, onSave = onSave
                )
                is EditProfileUiState.Saving -> EditProfileForm(
                    uiState.profile, isSaving = true, isSaved = false, onSave = onSave
                )
                is EditProfileUiState.Saved -> EditProfileForm(
                    uiState.profile, isSaving = false, isSaved = true, onSave = onSave
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditProfileForm(
    profile: ProfessionalProfileResponse,
    isSaving: Boolean,
    isSaved: Boolean,
    onSave: (description: String, city: String, neighborhoods: List<String>, contactPhone: String, whatsAppPhone: String) -> Unit
) {
    var description by remember(profile.id) { mutableStateOf(profile.description) }
    var city by remember(profile.id) { mutableStateOf(profile.cityName) }
    var neighborhoodChips by remember(profile.id) { mutableStateOf(profile.neighborhoods) }
    var neighborhoodInput by remember(profile.id) { mutableStateOf("") }
    var contactPhone by remember(profile.id) { mutableStateOf(profile.contactPhone) }
    var whatsAppPhone by remember(profile.id) { mutableStateOf(profile.whatsAppPhone ?: "") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProfileAvatar(
                name = "${profile.firstName} ${profile.lastName}".trim().ifBlank { null },
                photoUrl = profile.photoUrl,
                size = 64.dp
            )
            Spacer(modifier = Modifier.width(16.dp))
            if (profile.services.isNotEmpty()) {
                Text(
                    "Services: ${profile.services.joinToString(", ") { it.displayName }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

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

        if (neighborhoodChips.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                neighborhoodChips.forEach { chip ->
                    InputChip(
                        selected = false,
                        onClick = {},
                        label = { Text(chip) },
                        trailingIcon = {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove $chip",
                                modifier = Modifier
                                    .size(InputChipDefaults.IconSize)
                                    .clickable { neighborhoodChips = neighborhoodChips - chip },
                            )
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(Spacing.xs))
        }

        OutlinedTextField(
            value = neighborhoodInput,
            onValueChange = { input ->
                if (input.endsWith(",")) {
                    val chip = input.dropLast(1).trim()
                    if (chip.isNotBlank()) neighborhoodChips = neighborhoodChips + chip
                    neighborhoodInput = ""
                } else {
                    neighborhoodInput = input
                }
            },
            label = { Text("Add neighborhood") },
            placeholder = { Text("Type and press comma to add") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                val chip = neighborhoodInput.trim()
                if (chip.isNotBlank()) neighborhoodChips = neighborhoodChips + chip
                neighborhoodInput = ""
            }),
            shape = MaterialTheme.shapes.medium,
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
                onSave(description, city, neighborhoodChips, contactPhone, whatsAppPhone)
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

// ── Previews ──

@LightDarkScreenPreview
@Composable
private fun EditProfileLoadingPreview() {
    AppTheme { EditProfessionalProfileScreen(uiState = EditProfileUiState.Loading, onSave = { _, _, _, _, _ -> }, onNavigateBack = {}, onGoToOnboarding = {}) }
}

@LightDarkScreenPreview
@Composable
private fun EditProfileNoProfilePreview() {
    AppTheme { EditProfessionalProfileScreen(uiState = EditProfileUiState.NoProfile, onSave = { _, _, _, _, _ -> }, onNavigateBack = {}, onGoToOnboarding = {}) }
}

@LightDarkScreenPreview
@Composable
private fun EditProfileReadyPreview() {
    AppTheme {
        EditProfessionalProfileScreen(
            uiState = EditProfileUiState.Ready(PreviewSamples.sampleProfile),
            onSave = { _, _, _, _, _ -> }, onNavigateBack = {}, onGoToOnboarding = {}
        )
    }
}

@LightDarkScreenPreview
@Composable
private fun EditProfileSavingPreview() {
    AppTheme {
        EditProfessionalProfileScreen(
            uiState = EditProfileUiState.Saving(PreviewSamples.sampleProfile),
            onSave = { _, _, _, _, _ -> }, onNavigateBack = {}, onGoToOnboarding = {}
        )
    }
}

@LightDarkScreenPreview
@Composable
private fun EditProfileSavedPreview() {
    AppTheme {
        EditProfessionalProfileScreen(
            uiState = EditProfileUiState.Saved(PreviewSamples.sampleProfile),
            onSave = { _, _, _, _, _ -> }, onNavigateBack = {}, onGoToOnboarding = {}
        )
    }
}

@LightDarkScreenPreview
@Composable
private fun EditProfileErrorPreview() {
    AppTheme {
        EditProfessionalProfileScreen(
            uiState = EditProfileUiState.Error("Failed to load profile. Please try again later."),
            onSave = { _, _, _, _, _ -> }, onNavigateBack = {}, onGoToOnboarding = {}
        )
    }
}
