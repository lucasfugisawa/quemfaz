package com.fugisawa.quemfaz.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fugisawa.quemfaz.contract.catalog.CatalogResponse
import com.fugisawa.quemfaz.contract.profile.ProfessionalProfileResponse
import com.fugisawa.quemfaz.ui.components.ProfileAvatar
import com.fugisawa.quemfaz.ui.components.ServiceCategoryPicker
import com.fugisawa.quemfaz.ui.preview.LightDarkScreenPreview
import com.fugisawa.quemfaz.ui.preview.PreviewSamples
import com.fugisawa.quemfaz.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfessionalProfileScreen(
    uiState: EditProfileUiState,
    editedServiceIds: List<String>,
    catalog: CatalogResponse?,
    onAddService: (String) -> Unit,
    onRemoveService: (String) -> Unit,
    onSave: (description: String, city: String, contactPhone: String, whatsAppPhone: String) -> Unit,
    onNavigateBack: () -> Unit,
    onGoToOnboarding: () -> Unit,
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
                    uiState.profile, isSaving = false, isSaved = false, editedServiceIds, catalog, onAddService, onRemoveService, onSave
                )
                is EditProfileUiState.Saving -> EditProfileForm(
                    uiState.profile, isSaving = true, isSaved = false, editedServiceIds, catalog, onAddService, onRemoveService, onSave
                )
                is EditProfileUiState.Saved -> EditProfileForm(
                    uiState.profile, isSaving = false, isSaved = true, editedServiceIds, catalog, onAddService, onRemoveService, onSave
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
    editedServiceIds: List<String>,
    catalog: CatalogResponse?,
    onAddService: (String) -> Unit,
    onRemoveService: (String) -> Unit,
    onSave: (description: String, city: String, contactPhone: String, whatsAppPhone: String) -> Unit,
) {
    var description by remember(profile.id) { mutableStateOf(profile.description) }
    var city by remember(profile.id) { mutableStateOf(profile.cityName) }
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
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Services section
        Text("Serviços", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            editedServiceIds.forEach { serviceId ->
                val displayName = catalog?.services?.find { it.id == serviceId }?.displayName ?: serviceId
                InputChip(
                    selected = true,
                    onClick = { onRemoveService(serviceId) },
                    label = { Text(displayName) },
                    trailingIcon = {
                        Icon(Icons.Default.Close, contentDescription = "Remover", modifier = Modifier.size(16.dp))
                    },
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Add service button + dialog
        var showServicePicker by remember { mutableStateOf(false) }
        OutlinedButton(onClick = { showServicePicker = true }) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Adicionar serviço")
        }

        if (showServicePicker && catalog != null) {
            val alreadySelected = editedServiceIds.toSet()
            var pickerSelection by remember { mutableStateOf(emptySet<String>()) }

            AlertDialog(
                onDismissRequest = { showServicePicker = false },
                title = { Text("Adicionar serviços") },
                text = {
                    // Constrain height to avoid layout issues — ServiceCategoryPicker uses LazyColumn internally
                    Box(modifier = Modifier.heightIn(max = 400.dp)) {
                        ServiceCategoryPicker(
                            categories = catalog.categories,
                            services = catalog.services.filter { it.id !in alreadySelected },
                            selectedServiceIds = pickerSelection,
                            onSelectionChanged = { pickerSelection = it },
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        pickerSelection.forEach { onAddService(it) }
                        showServicePicker = false
                    }) { Text("Adicionar") }
                },
                dismissButton = {
                    TextButton(onClick = { showServicePicker = false }) { Text("Cancelar") }
                },
            )
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
                onSave(description, city, contactPhone, whatsAppPhone)
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
    AppTheme {
        EditProfessionalProfileScreen(
            uiState = EditProfileUiState.Loading,
            editedServiceIds = emptyList(), catalog = null, onAddService = {}, onRemoveService = {},
            onSave = { _, _, _, _ -> }, onNavigateBack = {}, onGoToOnboarding = {}
        )
    }
}

@LightDarkScreenPreview
@Composable
private fun EditProfileNoProfilePreview() {
    AppTheme {
        EditProfessionalProfileScreen(
            uiState = EditProfileUiState.NoProfile,
            editedServiceIds = emptyList(), catalog = null, onAddService = {}, onRemoveService = {},
            onSave = { _, _, _, _ -> }, onNavigateBack = {}, onGoToOnboarding = {}
        )
    }
}

@LightDarkScreenPreview
@Composable
private fun EditProfileReadyPreview() {
    AppTheme {
        EditProfessionalProfileScreen(
            uiState = EditProfileUiState.Ready(PreviewSamples.sampleProfile),
            editedServiceIds = PreviewSamples.sampleProfile.services.map { it.serviceId },
            catalog = null, onAddService = {}, onRemoveService = {},
            onSave = { _, _, _, _ -> }, onNavigateBack = {}, onGoToOnboarding = {}
        )
    }
}

@LightDarkScreenPreview
@Composable
private fun EditProfileSavingPreview() {
    AppTheme {
        EditProfessionalProfileScreen(
            uiState = EditProfileUiState.Saving(PreviewSamples.sampleProfile),
            editedServiceIds = PreviewSamples.sampleProfile.services.map { it.serviceId },
            catalog = null, onAddService = {}, onRemoveService = {},
            onSave = { _, _, _, _ -> }, onNavigateBack = {}, onGoToOnboarding = {}
        )
    }
}

@LightDarkScreenPreview
@Composable
private fun EditProfileSavedPreview() {
    AppTheme {
        EditProfessionalProfileScreen(
            uiState = EditProfileUiState.Saved(PreviewSamples.sampleProfile),
            editedServiceIds = PreviewSamples.sampleProfile.services.map { it.serviceId },
            catalog = null, onAddService = {}, onRemoveService = {},
            onSave = { _, _, _, _ -> }, onNavigateBack = {}, onGoToOnboarding = {}
        )
    }
}

@LightDarkScreenPreview
@Composable
private fun EditProfileErrorPreview() {
    AppTheme {
        EditProfessionalProfileScreen(
            uiState = EditProfileUiState.Error("Failed to load profile. Please try again later."),
            editedServiceIds = emptyList(), catalog = null, onAddService = {}, onRemoveService = {},
            onSave = { _, _, _, _ -> }, onNavigateBack = {}, onGoToOnboarding = {}
        )
    }
}
